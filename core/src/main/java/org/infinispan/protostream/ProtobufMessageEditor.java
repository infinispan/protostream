package org.infinispan.protostream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.descriptors.WireType;

/**
 * A lazy, field-addressable editor over the encoded bytes of a protobuf message.
 * <p>
 * Construction performs a single linear scan that records the byte span of each top-level field
 * occurrence, without decoding any values and without recursing into nested messages. Fields can then
 * be read and modified selectively:
 * <ul>
 * <li><strong>Reads</strong> decode only the requested field (or the leaf of a dotted path).</li>
 * <li><strong>Writes</strong> mark the field dirty and encode the new value lazily.</li>
 * <li><strong>Emission</strong> produces a spliced byte stream in which untouched field spans are
 * copied verbatim (bit-for-bit), unknown fields are always preserved, and the original field order
 * is maintained. Only the modified fields are re-encoded.</li>
 * </ul>
 * Nested messages are addressed through {@link #nestedEditor(FieldDescriptor, int)}, which is a new
 * editor over the raw nested span — created (and indexed) only when a path actually descends into it.
 * <p>
 * An editor is single-threaded and intended to be used on one entry at a time (e.g. inside a cache
 * {@code compute}). It holds a reference to the input array and never mutates it; it is not
 * marshallable.
 * <p>
 * Reads always reflect the original bytes: reading a field after editing it returns the value
 * before the edit. The pending edits are materialized only by emission
 * ({@link #toByteArray()} / {@link #writeTo(RandomAccessOutputStream)}).
 * <p>
 * Map fields are not supported: read and write operations that target a map field, or a property
 * path that traverses one, are rejected with {@link IllegalArgumentException}. Span-level access
 * ({@link #hasField(FieldDescriptor)} and {@link #getCount(FieldDescriptor)}) still works, and
 * unmodified map occurrences are passed through verbatim on emission.
 * <p>
 * Occurrences whose wire type is incompatible with the field's declared type are treated as
 * unknown data, exactly as protobuf readers treat them: they are invisible to reads and edits,
 * and are always passed through verbatim on emission.
 *
 * @since 6.0.14
 */
public final class ProtobufMessageEditor {

   /**
    * Maximum number of segments in a property path applied via {@link #apply}. Paths are caller
    * supplied and traversed recursively, so a bound is required to prevent {@code StackOverflowError}
    * on hostile input; real schemas nest far less deeply than this.
    */
   private static final int MAX_PATH_DEPTH = 256;

   /**
    * The largest valid protobuf field number, 2^29 - 1.
    */
   private static final int MAX_FIELD_NUMBER = (1 << 29) - 1;

   private final ImmutableSerializationContext serCtx;
   private final Descriptor descriptor;
   private final byte[] bytes;

   // Field span index: one entry per top-level field occurrence, in wire order.
   private int[] fieldNumbers;
   private int[] wireTypes;
   private int[] starts;
   private int[] ends;
   private int fieldCount;

   // Per-field edit state, keyed by field number (insertion order).
   private final Map<Integer, FieldEdit> edits = new LinkedHashMap<>();

   // Cached nested editors created by {@link #apply}, keyed by field number, so that successive
   // operations on the same nested path accumulate on the same nested state. The parallel map
   // records the original-occurrence index each cached editor writes back to.
   private final Map<Integer, ProtobufMessageEditor> nestedEditors = new LinkedHashMap<>();
   private final Map<Integer, Integer> nestedWritebackOccurrences = new LinkedHashMap<>();

   private ProtobufMessageEditor(ImmutableSerializationContext serCtx, Descriptor descriptor, byte[] bytes) throws IOException {
      this.serCtx = serCtx;
      this.descriptor = descriptor;
      this.bytes = bytes;
      int capacity = Math.max(8, bytes.length / 16);
      this.fieldNumbers = new int[capacity];
      this.wireTypes = new int[capacity];
      this.starts = new int[capacity];
      this.ends = new int[capacity];
      indexTopLevelFields(bytes);
   }

   /**
    * Creates an editor over the encoded bytes of a message.
    * <p>
    * Created without a serialization context: values can be read and scalar/bytes fields written, but
    * message-typed values cannot be marshalled/unmarshalled (see {@link #readPath(String[])},
    * {@link #setField(FieldDescriptor, Object)}).
    *
    * @param descriptor   the message descriptor
    * @param messageBytes the raw (unwrapped) protobuf bytes of the message
    * @throws IOException if the message bytes are malformed
    */
   public static ProtobufMessageEditor of(Descriptor descriptor, byte[] messageBytes) throws IOException {
      return of(null, descriptor, messageBytes);
   }

   /**
    * Creates an editor over the encoded bytes of a message.
    *
    * @param serCtx       the serialization context, used to marshall/unmarshall message-typed values
    * @param descriptor   the message descriptor
    * @param messageBytes the raw (unwrapped) protobuf bytes of the message
    * @throws IOException if the message bytes are malformed
    */
   public static ProtobufMessageEditor of(ImmutableSerializationContext serCtx, Descriptor descriptor, byte[] messageBytes) throws IOException {
      Objects.requireNonNull(descriptor, "descriptor cannot be null");
      Objects.requireNonNull(messageBytes, "messageBytes cannot be null");
      return new ProtobufMessageEditor(serCtx, descriptor, messageBytes);
   }

   public Descriptor getDescriptor() {
      return descriptor;
   }

   /**
    * @return true if at least one wire-compatible occurrence of the given field is present
    */
   public boolean hasField(FieldDescriptor fd) {
      return findFirstCompatibleOccurrence(fd) >= 0;
   }

   /**
    * @return the number of wire-compatible occurrences of the given field (occurrences whose wire
    * type is incompatible with the declared type count as unknown data)
    */
   public int getCount(FieldDescriptor fd) {
      int count = 0;
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) count++;
      }
      return count;
   }

   /**
    * Decodes the first occurrence of the given field.
    *
    * @return the decoded value ({@code Integer}/{@code Long}/{@code Float}/{@code Double}/{@code
    * Boolean}/{@code String}/{@code byte[]} depending on the field type), or {@code null} if the field
    * is absent
    */
   public Object readField(FieldDescriptor fd) throws IOException {
      requireNotMap(fd);
      int i = findFirstCompatibleOccurrence(fd);
      if (i < 0) return null;
      List<Object> values = decodeValues(fd, bytes, starts[i], ends[i]);
      // a zero-length packed occurrence carries no values
      return values.isEmpty() ? null : values.get(0);
   }

   /**
    * Decodes all occurrences of the given field, in wire order.
    *
    * @return an (possibly empty) list of decoded values
    */
   public List<Object> readRepeated(FieldDescriptor fd) throws IOException {
      requireNotMap(fd);
      List<Object> values = new ArrayList<>();
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) {
            values.addAll(decodeValues(fd, bytes, starts[i], ends[i]));
         }
      }
      return values;
   }

   /**
    * Returns the raw (undecoded) bytes of a nested message field occurrence.
    *
    * @param fd         a message-typed field descriptor
    * @param occurrence the zero-based occurrence index
    * @return the nested message bytes, or {@code null} if the occurrence is absent
    */
   public byte[] getMessageBytes(FieldDescriptor fd, int occurrence) throws IOException {
      requireMessageType(fd);
      int i = compatibleSpanIndex(fd, occurrence);
      if (i < 0) return null;
      return readDelimitedBytes(bytes, starts[i], ends[i]);
   }

   /**
    * Creates a new editor over the raw bytes of a nested message field occurrence, lazily. The nested
    * span is indexed only when this method is called; siblings of the nested message are never
    * indexed by this call.
    *
    * @param fd         a message-typed field descriptor
    * @param occurrence the zero-based occurrence index
    * @return an editor over the nested message (over an empty message if the occurrence is absent)
    */
   public ProtobufMessageEditor nestedEditor(FieldDescriptor fd, int occurrence) throws IOException {
      requireMessageType(fd);
      byte[] nested = getMessageBytes(fd, occurrence);
      if (nested == null) nested = new byte[0];
      return new ProtobufMessageEditor(serCtx, fd.getMessageType(), nested);
   }

   /**
    * Reads a dotted property path, decoding only the fields along the path. Intermediate messages are
    * traversed through lazily-created nested editors; the leaf is decoded on demand.
    * <p>
    * For a message-typed leaf the value is materialized into a Java object using the serialization
    * context (requiring {@link #of(ImmutableSerializationContext, Descriptor, byte[])}).
    * <p>
    * Mirrors the conventional read semantics: an absent field yields {@code null}; a repeated leaf
    * yields a list of values.
    *
    * @param path the property path (field names, no indices)
    * @return the decoded leaf value
    */
   public Object readPath(String[] path) throws IOException {
      Objects.requireNonNull(path, "path cannot be null");
      if (path.length == 0) throw new IllegalArgumentException("path must not be empty");
      Descriptor currentDescriptor = descriptor;
      ProtobufMessageEditor current = this;
      for (int i = 0; i < path.length; i++) {
         FieldDescriptor fd = currentDescriptor.findFieldByName(path[i]);
         if (fd == null) {
            throw new IllegalArgumentException("Unknown field '" + path[i] + "' in message " + currentDescriptor.getFullName());
         }
         if (i < path.length - 1) {
            if (!isMessage(fd)) {
               throw new IllegalArgumentException("Field '" + path[i] + "' is not a message type, cannot traverse into it");
            }
            byte[] nested = current.getMessageBytes(fd, 0);
            if (nested == null) return null;
            current = new ProtobufMessageEditor(serCtx, fd.getMessageType(), nested);
            currentDescriptor = fd.getMessageType();
         } else {
            if (isMessage(fd)) {
               // single pass over the span index: linear in the number of occurrences
               List<Object> values = new ArrayList<>();
               for (int j = 0; j < current.fieldCount; j++) {
                  if (current.fieldNumbers[j] == fd.getNumber() && isWireCompatible(fd, current.wireTypes[j])) {
                     values.add(materialize(fd.getMessageType().getFullName(),
                           readDelimitedBytes(current.bytes, current.starts[j], current.ends[j])));
                  }
               }
               if (values.isEmpty()) return null;
               return fd.isRepeated() ? values : values.get(0);
            }
            List<Object> values = new ArrayList<>();
            for (int j = 0; j < current.fieldCount; j++) {
               if (current.fieldNumbers[j] == fd.getNumber() && isWireCompatible(fd, current.wireTypes[j])) {
                  values.addAll(decodeValues(fd, current.bytes, current.starts[j], current.ends[j]));
               }
            }
            if (values.isEmpty()) return null;
            return fd.isRepeated() ? values : values.get(0);
         }
      }
      return null;
   }

   /**
    * Replaces all occurrences of the given field with a single value. A {@code null} value removes the
    * field.
    *
    * @throws IOException if a message-typed value cannot be marshalled
    */
   public void setField(FieldDescriptor fd, Object value) throws IOException {
      setValues(fd, value == null ? Collections.emptyList() : List.of(value));
   }

   /**
    * Replaces all occurrences of the given field with the given values (in order). An empty collection
    * removes the field.
    *
    * @throws IOException if a message-typed value cannot be marshalled
    */
   public void setValues(FieldDescriptor fd, Collection<? extends Object> values) throws IOException {
      Objects.requireNonNull(values, "values cannot be null");
      FieldEdit edit = editFor(fd);
      invalidateNested(fd);
      List<Object> converted = new ArrayList<>();
      for (Object v : values) {
         if (v != null) converted.add(convert(fd, v));
      }
      edit.replaced = true;
      edit.deleted = converted.isEmpty();
      edit.remove.clear();
      edit.add.clear();
      edit.occurrenceOverrides.clear();
      edit.created.clear();
      edit.add.addAll(converted);
   }

   /**
    * Appends one value as a new occurrence of the given field (typically used on repeated fields;
    * on a singular field it appends a duplicate occurrence, which protobuf readers resolve
    * last-wins). A {@code null} value is ignored: unlike SET, where {@code null} removes the
    * field, adding null is meaningless.
    *
    * @throws IOException if a message-typed value cannot be marshalled. The call converts the
    *                     value before touching any state, so a failed call leaves the editor
    *                     exactly as it was.
    */
   public void addField(FieldDescriptor fd, Object value) throws IOException {
      Object converted = convert(fd, value);
      if (converted == null) return;
      FieldEdit edit = editFor(fd);
      invalidateNested(fd);
      edit.add.add(converted);
   }

   /**
    * Removes all occurrences of the given field.
    */
   public void removeField(FieldDescriptor fd) {
      FieldEdit edit = editFor(fd);
      invalidateNested(fd);
      edit.replaced = false;
      edit.deleted = true;
      edit.remove.clear();
      edit.add.clear();
      edit.occurrenceOverrides.clear();
      edit.created.clear();
   }

   /**
    * Removes one specific value from the given field: matching original occurrences are skipped during
    * emission, and an equal pending appended value is dropped. If the field has no occurrences and no
    * pending values, this is a no-op.
    *
    * @param value the value to remove (compared in converted form; message values are compared by
    *              encoded content)
    */
   /**
    * The effective bytes of a message-field occurrence, honoring a pending per-occurrence
    * override recorded by an earlier dotted-path edit.
    */
   private byte[] effectiveMessageBytes(FieldDescriptor fd, FieldEdit edit, int occurrence, int span) throws IOException {
      if (edit != null && edit.occurrenceOverrides.get(occurrence) instanceof byte[] b) {
         return b;
      }
      return readDelimitedBytes(bytes, starts[span], ends[span]);
   }

   /**
    * Does any wire-compatible occurrence of the field (or pending addition) match the converted
    * value? Used to keep a non-matching REMOVE a true no-op.
    */
   private boolean matchesAnyOccurrence(FieldDescriptor fd, FieldEdit edit, Object converted) throws IOException {
      if (edit != null && indexOfMatch(edit.add, converted) >= 0) {
         return true;
      }
      int compatible = 0;
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) {
            if (isMessage(fd)) {
               if (matches(effectiveMessageBytes(fd, edit, compatible, i), converted)) {
                  return true;
               }
            } else {
               for (Object v : decodeValues(fd, bytes, starts[i], ends[i])) {
                  if (matches(v, converted)) {
                     return true;
                  }
               }
            }
            compatible++;
         }
      }
      return false;
   }

   public void removeValue(FieldDescriptor fd, Object value) throws IOException {
      Object converted = convert(fd, value);
      FieldEdit edit = edits.get(fd.getNumber());
      // a removal that matches nothing, original or pending, must be a true no-op: no edit state,
      // no nested-editor invalidation, hasChanges() stays false and the output stays bit-identical
      if (!matchesAnyOccurrence(fd, edit, converted)) {
         return;
      }
      if (edit == null) {
         edit = editFor(fd);
      }
      invalidateNested(fd);
      // scrub pending additions even when the field is deleted: values may have been appended
      // after the field was cleared
      removeFirstMatching(edit.add, converted);
      if (edit.deleted) return;
      if (!edit.replaced) {
         edit.remove.add(converted);
      }
   }

   /**
    * Removes all original occurrences of the given field for which the given test returns
    * {@code true}, applied to the decoded value of each occurrence. The predicate sees the
    * original occurrences, not pending edits. Pending appended values are dropped as well: those
    * are tested in their converted form (message values as encoded bytes).
    */
   public void removeMatching(FieldDescriptor fd, Predicate<Object> test) throws IOException {
      Objects.requireNonNull(test, "test cannot be null");
      requireNotMap(fd);
      List<Object> matching = new ArrayList<>();
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) {
            for (Object decoded : decodeValues(fd, bytes, starts[i], ends[i])) {
               if (test.test(decoded)) {
                  matching.add(decoded);
               }
            }
         }
      }
      FieldEdit edit = edits.get(fd.getNumber());
      if (edit == null) {
         if (matching.isEmpty()) return; // nothing to remove, original or pending
         edit = editFor(fd);
      }
      invalidateNested(fd);
      if (edit.deleted) return;
      if (edit.replaced) {
         edit.add.removeIf(test::test);
      } else {
         matching.forEach(edit.remove::add);
         edit.add.removeIf(test::test);
      }
   }

   /**
    * Applies a {@link ProtobufFieldUpdater.UpdateOperation} (SET/ADD/REMOVE on a dotted path) to this
    * editor. A dotted path edits the first occurrence of each repeated intermediate field; sibling
    * occurrences are preserved. A SET on an absent intermediate chain creates the chain, while ADD
    * and REMOVE on an absent chain are no-ops. Paths longer than {@value #MAX_PATH_DEPTH} segments
    * are rejected, to prevent unbounded recursion on hostile input.
    *
    * @throws IOException if the operation cannot be applied
    */
   public void apply(ProtobufFieldUpdater.UpdateOperation op) throws IOException {
      Objects.requireNonNull(op, "op cannot be null");
      if (op.propertyPath().length > MAX_PATH_DEPTH) {
         throw new IllegalArgumentException("Property path exceeds the maximum depth of " + MAX_PATH_DEPTH);
      }
      applyPath(op, 0);
   }

   /**
    * @return true if at least one field of this message was edited
    */
   public boolean hasChanges() {
      for (FieldEdit edit : edits.values()) {
         if (!edit.neutral()) return true;
      }
      return false;
   }

   /**
    * Emits the spliced message bytes: untouched field spans copied verbatim, modified fields
    * re-encoded, unknown fields preserved, original field order maintained.
    */
   public byte[] toByteArray() throws IOException {
      RandomAccessOutputStream out = RandomAccessOutputStream.withCapacity(bytes.length + 32);
      emitTo(out);
      return out.toByteArray();
   }

   /**
    * Emits the spliced message bytes into the given output stream.
    */
   public void writeTo(RandomAccessOutputStream out) throws IOException {
      emitTo(out);
   }

   // ------------------------------------------------------------------

   private void applyPath(ProtobufFieldUpdater.UpdateOperation op, int idx) throws IOException {
      String[] path = op.propertyPath();
      FieldDescriptor fd = descriptor.findFieldByName(path[idx]);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field '" + path[idx] + "' in message " + descriptor.getFullName());
      }
      if (idx < path.length - 1) {
         if (!isMessage(fd)) {
            throw new IllegalArgumentException("Field '" + path[idx] + "' is not a message type, cannot traverse into it");
         }
         ProtobufMessageEditor nested = nestedEditors.get(fd.getNumber());
         int writebackOccurrence = nestedWritebackOccurrences.getOrDefault(fd.getNumber(), 0);
         if (nested == null) {
            byte[] nestedBytes = null;
            FieldEdit edit = edits.get(fd.getNumber());
            if (edit != null && edit.deleted) {
               // a pending deletion: a later dotted SET starts fresh, it must not resurrect old bytes
               nestedBytes = new byte[0];
            } else if (edit != null && edit.replaced && !edit.add.isEmpty() && edit.add.get(0) instanceof byte[] b) {
               // source the nested view from the pending replacement, if the field was directly set
               nestedBytes = b;
            } else {
               // target the first occurrence that survives pending removals, so a dotted edit
               // never resurrects a value the caller removed
               writebackOccurrence = firstSurvivingOccurrence(fd);
               if (writebackOccurrence >= 0) {
                  // prefer the pending per-occurrence state (the result of an earlier dotted
                  // edit) over the original bytes, so earlier edits are never silently discarded
                  int span = compatibleSpanIndex(fd, writebackOccurrence);
                  nestedBytes = effectiveMessageBytes(fd, edit, writebackOccurrence, span);
               } else if (edit != null && !edit.created.isEmpty() && edit.created.get(0) instanceof byte[] b) {
                  // a synthetic occurrence created by an earlier dotted edit of a missing message
                  nestedBytes = b;
                  writebackOccurrence = 0;
               }
            }
            if (nestedBytes == null) {
               // absent chain: only SET creates the intermediate message; ADD and REMOVE are no-ops
               if (op.type() != ProtobufFieldUpdater.OperationType.SET) {
                  return;
               }
               nestedBytes = new byte[0];
               writebackOccurrence = 0;
            }
            nested = new ProtobufMessageEditor(serCtx, fd.getMessageType(), nestedBytes);
            nestedEditors.put(fd.getNumber(), nested);
            nestedWritebackOccurrences.put(fd.getNumber(), writebackOccurrence);
         }
         nested.applyPath(op, idx + 1);
         if (nested.hasChanges()) {
            setOccurrence(fd, writebackOccurrence, nested.toByteArray());
         }
      } else {
         // convert all values up front, so a value that cannot be converted leaves the editor untouched
         List<Object> converted = new ArrayList<>();
         for (Object v : op.values()) {
            converted.add(convert(fd, v));
         }
         switch (op.type()) {
            case SET -> setValues(fd, converted);
            case ADD -> {
               for (Object v : converted) {
                  addField(fd, v);
               }
            }
            case REMOVE -> {
               for (Object v : converted) {
                  removeValue(fd, v);
               }
            }
         }
      }
   }

   private void indexTopLevelFields(byte[] buf) throws IOException {
      SpanScanner s = new SpanScanner(buf);
      while (s.pos < s.end) {
         int start = s.pos;
         long tag = s.readTag();
         if (WireType.getTagWireType((int) tag) == WireType.WIRETYPE_START_GROUP) {
            s.scanGroupSpan((int) (tag >>> 3));
         } else {
            s.skipValue(tag);
         }
         addOccurrence((int) (tag >>> 3), (int) (tag & 7), start, s.pos);
      }
   }

   private void addOccurrence(int fieldNumber, int wireType, int start, int end) {
      if (fieldCount == fieldNumbers.length) {
         int newCap = fieldNumbers.length * 2;
         fieldNumbers = Arrays.copyOf(fieldNumbers, newCap);
         wireTypes = Arrays.copyOf(wireTypes, newCap);
         starts = Arrays.copyOf(starts, newCap);
         ends = Arrays.copyOf(ends, newCap);
      }
      fieldNumbers[fieldCount] = fieldNumber;
      wireTypes[fieldCount] = wireType;
      starts[fieldCount] = start;
      ends[fieldCount] = end;
      fieldCount++;
   }

   /**
    * An occurrence whose wire type is incompatible with the field's declared type is not valid
    * protobuf for that descriptor: readers treat it as unknown data. The editor mirrors that —
    * such occurrences are invisible to reads and edits, and are always passed through verbatim.
    */
   private static boolean isWireCompatible(FieldDescriptor fd, int wireType) {
      if (fd.getType().getWireType().value == wireType) {
         return true;
      }
      // a repeated packable scalar may be encoded packed, as one length-delimited occurrence
      return fd.isRepeated() && isPackable(fd.getType()) && wireType == WireType.WIRETYPE_LENGTH_DELIMITED;
   }

   private int findFirstCompatibleOccurrence(FieldDescriptor fd) {
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) return i;
      }
      return -1;
   }

   /**
    * The span index of the n-th wire-compatible occurrence of the field, or -1 if absent.
    */
   private int compatibleSpanIndex(FieldDescriptor fd, int n) {
      int seen = 0;
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) {
            if (seen++ == n) return i;
         }
      }
      return -1;
   }

   /**
    * The compatible-occurrence index of the first occurrence of the field that is not pending
    * removal, or -1 if none. A dotted-path edit targets the first surviving occurrence, so it
    * never resurrects a value the caller removed. An occurrence counts as removed when either its
    * original bytes or its pending override (the result of an earlier dotted edit) matches a
    * pending removal.
    */
   private int firstSurvivingOccurrence(FieldDescriptor fd) throws IOException {
      FieldEdit edit = edits.get(fd.getNumber());
      if (edit == null || edit.remove.isEmpty()) {
         return findFirstCompatibleOccurrence(fd) >= 0 ? 0 : -1;
      }
      int compatible = 0;
      for (int i = 0; i < fieldCount; i++) {
         if (fieldNumbers[i] == fd.getNumber() && isWireCompatible(fd, wireTypes[i])) {
            if (indexOfMatch(edit.remove, effectiveMessageBytes(fd, edit, compatible, i)) < 0) {
               return compatible;
            }
            compatible++;
         }
      }
      return -1;
   }

   private void emitTo(RandomAccessOutputStream out) throws IOException {
      TagWriter writer = TagWriter.newInstance(serCtx, out);
      Set<Integer> replacementEmitted = new HashSet<>();
      Map<Integer, Integer> occurrenceIndex = new HashMap<>();
      for (int i = 0; i < fieldCount; i++) {
         int fn = fieldNumbers[i];
         FieldEdit edit = edits.get(fn);
         if (edit == null || edit.neutral()) {
            out.write(bytes, starts[i], ends[i] - starts[i]);
            continue;
         }
         // an occurrence whose wire type is incompatible with the declared type is unknown data:
         // preserve it verbatim and do not let it consume an occurrence index
         if (!isWireCompatible(edit.fd, wireTypes[i])) {
            out.write(bytes, starts[i], ends[i] - starts[i]);
            continue;
         }
         if (edit.deleted) {
            continue; // suppress the occurrence
         }
         if (edit.replaced) {
            if (!replacementEmitted.contains(fn)) {
               for (Object v : edit.add) {
                  writeValue(writer, edit.fd, v);
               }
               replacementEmitted.add(fn);
            }
            continue; // suppress this (and remaining) original occurrence(s)
         }
         int occurrence = occurrenceIndex.merge(fn, 1, Integer::sum) - 1;
         Object override = edit.occurrenceOverrides.get(occurrence);
         if (override != null) {
            // a per-occurrence replacement (dotted-path edit): re-encode, siblings pass through.
            // A pending removal targeting the edited value removes it, per the operation order
            if (!edit.remove.isEmpty() && indexOfMatch(edit.remove, override) >= 0) {
               continue; // the edited occurrence is itself removed
            }
            writeValue(writer, edit.fd, override);
            continue;
         }
         // remove/add state: compare this occurrence against the pending removals (all matching
         // occurrences are skipped; the pending list is not consumed). A packed occurrence holds
         // several values: if any of them matches, the surviving values are re-encoded (a packed
         // span cannot be partially copied verbatim); otherwise the span is preserved as-is.
         if (!edit.remove.isEmpty()) {
            List<Object> decoded = decodeValues(edit.fd, bytes, starts[i], ends[i]);
            List<Object> kept = new ArrayList<>();
            boolean anyMatch = false;
            for (Object v : decoded) {
               if (indexOfMatch(edit.remove, v) >= 0) {
                  anyMatch = true;
               } else {
                  kept.add(v);
               }
            }
            if (anyMatch) {
               for (Object v : kept) {
                  writeValue(writer, edit.fd, v);
               }
               continue; // skip the (partially) matching occurrence
            }
         }
         out.write(bytes, starts[i], ends[i] - starts[i]);
      }
      // Tail: pending appended values (also after a remove-all), synthetic occurrences created by
      // path edits of absent messages, and replacement values for fields that had no original occurrence.
      for (FieldEdit edit : edits.values()) {
         if (edit.neutral() || (edit.replaced && replacementEmitted.contains(edit.fd.getNumber()))) continue;
         for (Object v : edit.created) {
            writeValue(writer, edit.fd, v);
         }
         for (Object v : edit.add) {
            writeValue(writer, edit.fd, v);
         }
      }
      writer.flush();
   }

   /**
    * Replaces a single occurrence of the given field, leaving sibling occurrences untouched. Used
    * by the dotted-path apply logic to write back a edited nested message without dropping the
    * other occurrences of a repeated field. If the field has no original occurrence with the given
    * index, the value is appended (creating the field). When the field was wholesale replaced, the
    * occurrence indexes into the pending replacement values. A dotted-path SET of a removed field
    * recreates it with just the edited content: the later operation wins.
    */
   private void setOccurrence(FieldDescriptor fd, int occurrence, Object value) throws IOException {
      FieldEdit edit = editFor(fd);
      Object converted = convert(fd, value);
      if (edit.deleted && !edit.replaced) {
         // the field was removed; the dotted-path SET recreates it with the edited content only
         edit.replaced = true;
         edit.deleted = false;
         edit.remove.clear();
         edit.occurrenceOverrides.clear();
         edit.created.clear();
         edit.add.clear();
         edit.add.add(converted);
         return;
      }
      if (edit.replaced) {
         edit.deleted = false;
         if (occurrence < edit.add.size()) {
            edit.add.set(occurrence, converted);
         } else {
            edit.add.add(converted);
         }
      } else if (occurrence >= 0 && occurrence < getCount(fd)) {
         edit.occurrenceOverrides.put(occurrence, converted);
      } else {
         // no original occurrence with this index: a synthetic occurrence, created by an earlier
         // path edit of a missing message; successive edits replace it in place
         int virtualIndex = occurrence - getCount(fd);
         if (virtualIndex < edit.created.size()) {
            edit.created.set(virtualIndex, converted);
         } else {
            edit.created.add(converted);
         }
      }
   }

   private FieldEdit editFor(FieldDescriptor fd) {
      Objects.requireNonNull(fd, "fd cannot be null");
      requireNotMap(fd);
      return edits.computeIfAbsent(fd.getNumber(), k -> new FieldEdit(fd));
   }

   /**
    * A direct edit of the field invalidates any cached nested editor: a later dotted-path
    * operation must source from the new pending state, not from the stale original bytes.
    */
   private void invalidateNested(FieldDescriptor fd) {
      nestedEditors.remove(fd.getNumber());
      nestedWritebackOccurrences.remove(fd.getNumber());
   }

   /**
    * Map fields are not supported by the read/write paths: their entries have a different wire
    * layout than the field's declared value type, so decoding or replacing them wholesale would be
    * incorrect. Reject loudly instead.
    */
   private static void requireNotMap(FieldDescriptor fd) {
      if (fd.isMap() || fd.getType() == Type.MAP) {
         throw new IllegalArgumentException("Map fields are not supported: " + fd.getName());
      }
   }

   private static void requireMessageType(FieldDescriptor fd) {
      Objects.requireNonNull(fd, "fd cannot be null");
      if (!isMessage(fd)) {
         throw new IllegalArgumentException("Field '" + fd.getName() + "' is not a message type");
      }
   }

   private static boolean isMessage(FieldDescriptor fd) {
      return fd.getType() == Type.MESSAGE;
   }

   private static boolean matches(Object a, Object b) {
      if (a instanceof byte[] && b instanceof byte[]) {
         return Arrays.equals((byte[]) a, (byte[]) b);
      }
      return Objects.equals(a, b);
   }

   private static int indexOfMatch(List<Object> list, Object value) {
      for (int i = 0; i < list.size(); i++) {
         if (matches(list.get(i), value)) return i;
      }
      return -1;
   }

   private static boolean removeFirstMatching(List<Object> list, Object value) {
      int i = indexOfMatch(list, value);
      if (i >= 0) {
         list.remove(i);
         return true;
      }
      return false;
   }

   private Object convert(FieldDescriptor fd, Object value) throws IOException {
      if (value == null) return null;
      return switch (fd.getType()) {
         case STRING -> value instanceof String s ? s : value.toString();
         case INT32, SINT32, UINT32, FIXED32, SFIXED32 ->
               value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
         case INT64, SINT64, UINT64, FIXED64, SFIXED64 ->
               value instanceof Number n ? n.longValue() : Long.parseLong(value.toString());
         case FLOAT -> value instanceof Number n ? n.floatValue() : Float.parseFloat(value.toString());
         case DOUBLE -> value instanceof Number n ? n.doubleValue() : Double.parseDouble(value.toString());
         case BOOL -> value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
         case ENUM -> {
            if (value instanceof Number n) yield n.intValue();
            EnumValueDescriptor enumValue = fd.getEnumType().findValueByName(value.toString());
            if (enumValue == null) {
               throw new IllegalArgumentException("Unknown enum value '" + value + "' for enum " + fd.getEnumType().getFullName());
            }
            yield enumValue.getNumber();
         }
         case BYTES -> {
            if (value instanceof byte[] b) yield b;
            throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to bytes");
         }
         case MESSAGE -> {
            if (value instanceof byte[] b) yield b;
            if (serCtx == null) {
               throw new IllegalStateException("Marshalling a message value requires a serialization context");
            }
            yield ProtobufUtil.toByteArray(serCtx, value);
         }
         case MAP -> throw new IllegalArgumentException("Map fields are not supported: " + fd.getName());
         default -> throw new IllegalArgumentException("Unsupported field type: " + fd.getType());
      };
   }

   private static void writeValue(TagWriter writer, FieldDescriptor fd, Object value) throws IOException {
      if (value == null) return;
      int number = fd.getNumber();
      switch (fd.getType()) {
         case STRING -> writer.writeString(number, (String) value);
         case INT32 -> writer.writeInt32(number, (Integer) value);
         case SINT32 -> writer.writeSInt32(number, (Integer) value);
         case UINT32 -> writer.writeUInt32(number, (Integer) value);
         case FIXED32 -> writer.writeFixed32(number, (Integer) value);
         case SFIXED32 -> writer.writeSFixed32(number, (Integer) value);
         case INT64 -> writer.writeInt64(number, (Long) value);
         case SINT64 -> writer.writeSInt64(number, (Long) value);
         case UINT64 -> writer.writeUInt64(number, (Long) value);
         case FIXED64 -> writer.writeFixed64(number, (Long) value);
         case SFIXED64 -> writer.writeSFixed64(number, (Long) value);
         case FLOAT -> writer.writeFloat(number, (Float) value);
         case DOUBLE -> writer.writeDouble(number, (Double) value);
         case BOOL -> writer.writeBool(number, (Boolean) value);
         case ENUM -> writer.writeEnum(number, (Integer) value);
         case BYTES -> writer.writeBytes(number, (byte[]) value);
         case MESSAGE -> writer.writeBytes(number, (byte[]) value);
         case MAP -> throw new IllegalArgumentException("Map fields are not supported: " + fd.getName());
         default -> throw new IOException("Unsupported field type for writing: " + fd.getType());
      }
   }

   // Decoding of field occurrences from their recorded spans.

   /**
    * Decodes the values of one recorded field span. A span normally holds a single value; a
    * repeated packable field may however be encoded in packed form, as one length-delimited
    * occurrence holding several scalars, in which case all of them are returned.
    */
   private static List<Object> decodeValues(FieldDescriptor fd, byte[] buf, int start, int end) throws IOException {
      SpanScanner s = new SpanScanner(buf, start, end);
      long tag = s.readTag();
      if ((int) (tag >>> 3) != fd.getNumber()) {
         throw new MalformedProtobufException("Field number mismatch while decoding field " + fd.getNumber());
      }
      if ((int) (tag & 7) == WireType.WIRETYPE_LENGTH_DELIMITED && fd.isRepeated() && isPackable(fd.getType())) {
         int length = readDelimitedLength(s);
         int limit = s.pos + length;
         List<Object> values = new ArrayList<>();
         while (s.pos < limit) {
            values.add(decodeScalar(fd, s));
         }
         if (s.pos != limit) {
            throw new MalformedProtobufException("Trailing bytes after decoding field " + fd.getNumber());
         }
         return values;
      }
      Object value = decodeScalar(fd, s);
      if (s.pos != s.end) {
         throw new MalformedProtobufException("Trailing bytes after decoding field " + fd.getNumber());
      }
      return List.of(value);
   }

   private static boolean isPackable(Type type) {
      return switch (type) {
         case INT32, SINT32, UINT32, INT64, SINT64, UINT64, FIXED32, SFIXED32, FIXED64, SFIXED64, FLOAT, DOUBLE, BOOL, ENUM -> true;
         default -> false;
      };
   }

   /**
    * Decodes one scalar/message value of the given type, without its tag.
    */
   private static Object decodeScalar(FieldDescriptor fd, SpanScanner s) throws IOException {
      return switch (fd.getType()) {
         case INT32 -> (int) s.readVarint64();
         case SINT32 -> decodeZigzag32(s.readVarint64());
         case UINT32 -> (int) s.readVarint64();
         case INT64 -> s.readVarint64();
         case SINT64 -> decodeZigzag64(s.readVarint64());
         case UINT64 -> s.readVarint64();
         case BOOL -> s.readVarint64() != 0;
         case ENUM -> (int) s.readVarint64();
         case FIXED32 -> s.readFixed32();
         case SFIXED32 -> s.readFixed32();
         case FLOAT -> Float.intBitsToFloat(s.readFixed32());
         case FIXED64 -> s.readFixed64();
         case SFIXED64 -> s.readFixed64();
         case DOUBLE -> Double.longBitsToDouble(s.readFixed64());
         case STRING -> readDelimitedString(s);
         case BYTES -> readDelimitedBytes(s);
         case MESSAGE -> readDelimitedBytes(s);
         case MAP -> throw new IllegalArgumentException("Map fields are not supported: " + fd.getName());
         default -> throw new MalformedProtobufException("Unsupported field type " + fd.getType());
      };
   }

   private static int decodeZigzag32(long v) {
      return (int) ((v >>> 1) ^ -(v & 1));
   }

   private static long decodeZigzag64(long v) {
      return (v >>> 1) ^ -(v & 1);
   }

   private static int readDelimitedLength(SpanScanner s) throws IOException {
      long length = s.readVarint64();
      if (length < 0 || length > s.end - s.pos) {
         throw new MalformedProtobufException("Truncated length-delimited value");
      }
      return (int) length;
   }

   private static String readDelimitedString(SpanScanner s) throws IOException {
      int length = readDelimitedLength(s);
      String value = new String(s.buf, s.pos, length, StandardCharsets.UTF_8);
      s.pos += length;
      return value;
   }

   private static byte[] readDelimitedBytes(SpanScanner s) throws IOException {
      int length = readDelimitedLength(s);
      byte[] value = Arrays.copyOfRange(s.buf, s.pos, s.pos + length);
      s.pos += length;
      return value;
   }

   private static byte[] readDelimitedBytes(byte[] buf, int start, int end) throws IOException {
      SpanScanner s = new SpanScanner(buf, start, end);
      s.readTag();
      return readDelimitedBytes(s);
   }

   @SuppressWarnings("unchecked")
   private Object materialize(String typeName, byte[] nestedBytes) throws IOException {
      if (serCtx == null) {
         throw new IllegalStateException("Reading a message value requires a serialization context");
      }
      BaseMarshaller<?> marshaller = serCtx.getMarshaller(typeName);
      if (marshaller == null) {
         throw new IllegalStateException("No marshaller registered for type " + typeName);
      }
      return ProtobufUtil.fromByteArray(serCtx, nestedBytes, (Class<Object>) marshaller.getJavaClass());
   }

   // Per-field edit state.
   // - replaced: the output consists exactly of `add` (original occurrences are suppressed);
   // - deleted: the field is removed entirely;
   // - remove: values to exclude from the original occurrences (compared per occurrence at emission);
   // - add: values to append at the end of the message (or the whole output when replaced);
   // - occurrenceOverrides: per-occurrence replacements (dotted-path edits of nested messages),
   //   keyed by the zero-based index of the original occurrence in wire order; sibling occurrences
   //   are passed through verbatim;
   // - created: synthetic occurrences for path edits of absent messages, keyed by the occurrence
   //   index beyond the original count.
   // Emission is pure with respect to this state, so the editor can be emitted repeatedly.
   private static final class FieldEdit {
      final FieldDescriptor fd;
      boolean replaced;
      boolean deleted;
      final List<Object> remove = new ArrayList<>();
      final List<Object> add = new ArrayList<>();
      final Map<Integer, Object> occurrenceOverrides = new LinkedHashMap<>();
      final List<Object> created = new ArrayList<>();

      FieldEdit(FieldDescriptor fd) {
         this.fd = fd;
      }

      boolean neutral() {
         return !replaced && !deleted && remove.isEmpty() && add.isEmpty() && occurrenceOverrides.isEmpty() && created.isEmpty();
      }
   }

   /**
    * Minimal protobuf wire-format scanner used to record field spans and decode single field values.
    * Deliberately self-contained (no {@link TagReader} dependency) so the editor does not require a
    * serialization context for indexing and reads.
    */
   private static final class SpanScanner {
      final byte[] buf;
      int pos;
      final int end;

      SpanScanner(byte[] buf) {
         this(buf, 0, buf.length);
      }

      SpanScanner(byte[] buf, int pos, int end) {
         this.buf = buf;
         this.pos = pos;
         this.end = end;
      }

      /**
       * Reads a tag varint, validating the field number and wire type.
       */
      long readTag() throws IOException {
         long tag = readVarint64();
         int fieldNumber = (int) (tag >>> 3);
         int wireType = (int) (tag & 7);
         if (fieldNumber < 1 || fieldNumber > MAX_FIELD_NUMBER) {
            throw new MalformedProtobufException("Found an invalid protobuf tag (" + tag + ") having an out-of-range field number");
         }
         if (wireType > 5) {
            throw new MalformedProtobufException("Found a protobuf tag with invalid wire type : " + tag);
         }
         return tag;
      }

      long readVarint64() throws IOException {
         long value = 0;
         for (int i = 0; i < 64; i += 7) {
            if (pos >= end) {
               throw new MalformedProtobufException("Truncated varint");
            }
            byte b = buf[pos++];
            value |= (long) (b & 0x7F) << i;
            if (b >= 0) {
               return value;
            }
         }
         throw new MalformedProtobufException("Malformed varint");
      }

      int readFixed32() throws IOException {
         if ((long) pos + 4 > end) {
            throw new MalformedProtobufException("Truncated fixed32 value");
         }
         int value = (buf[pos] & 0xFF)
               | ((buf[pos + 1] & 0xFF) << 8)
               | ((buf[pos + 2] & 0xFF) << 16)
               | ((buf[pos + 3] & 0xFF) << 24);
         pos += 4;
         return value;
      }

      long readFixed64() throws IOException {
         if ((long) pos + 8 > end) {
            throw new MalformedProtobufException("Truncated fixed64 value");
         }
         long value = 0;
         for (int i = 0; i < 8; i++) {
            value |= (long) (buf[pos + i] & 0xFF) << (8 * i);
         }
         pos += 8;
         return value;
      }

      /**
       * Skips the value portion of a tag whose header has already been consumed.
       */
      void skipValue(long tag) throws IOException {
         switch ((int) (tag & 7)) {
            case 0 -> readVarint64();
            case 1 -> skipRaw(8);
            case 5 -> skipRaw(4);
            case 2 -> skipRaw(readVarint64());
            default -> throw new MalformedProtobufException("Invalid wire type in tag " + tag);
         }
      }

      void skipRaw(long n) throws IOException {
         if (n < 0 || n > end - pos) {
            throw new MalformedProtobufException("Truncated message");
         }
         pos += (int) n;
      }

      /**
       * Skips the body of a group whose opening tag has already been consumed; stops after the
       * matching closing END_GROUP tag.
       */
      void scanGroupSpan(int openFieldNumber) throws IOException {
         int depth = 1;
         int[] openGroups = new int[8];
         openGroups[0] = openFieldNumber;
         while (depth > 0) {
            if (pos >= end) {
               throw new MalformedProtobufException("Unterminated group");
            }
            long tag = readTag();
            int wireType = (int) (tag & 7);
            if (wireType == 3) {
               if (depth == openGroups.length) {
                  openGroups = Arrays.copyOf(openGroups, openGroups.length * 2);
               }
               openGroups[depth] = (int) (tag >>> 3);
               depth++;
            } else if (wireType == 4) {
               depth--;
               if ((int) (tag >>> 3) != openGroups[depth]) {
                  throw new MalformedProtobufException("Mismatched END_GROUP tag: field " + (tag >>> 3)
                        + " does not match the open group field " + openGroups[depth]);
               }
            } else {
               skipValue(tag);
            }
         }
      }
   }
}
