package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;

/**
 * {@code UnknownFieldSet} keeps track of fields seen during parsing of a protocol message but whose field numbers are
 * not recognized by the user provided marshallers (are never requested by them). This usually occurs when new fields
 * are added to a message type and then messages containing those fields are read by old versions of software that was
 * built before the new types were added.
 * <p/>
 * This is also used to handle the case when fields are requested in a different order than they were written to the
 * stream (lowers performance but still works). In this case all fields that are encountered while parsing the stream up
 * to the point where the requested field is finally encountered are cached in this data structure.
 * <p/>
 * This interface is loosely based on com.google.protobuf.UnknownFieldSet.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface UnknownFieldSet {

   /**
    * Checks if there are any fields in this set.
    */
   boolean isEmpty();

   /**
    * Parse an entire message from {@code input} and merge its fields into this set.
    */
   void readAllFields(CodedInputStream input) throws IOException;

   /**
    * Parse a single field from {@code input} and merge it into this set.
    *
    * @param tag The field's tag number, which was already parsed (tag contains both field id and wire type).
    * @return {@code false} if the tag is an end group tag.
    */
   boolean readSingleField(int tag, CodedInputStream input) throws IOException;

   /**
    * Convenience method for merging a new field containing a single varint value. This is used in particular when an
    * unknown enum value is encountered.
    *
    * @param tag the field tag (containing both field id and wire type).
    */
   void putVarintField(int tag, int value);

   /**
    * Writes all fields from this set to the {@code output} stream.
    */
   void writeTo(CodedOutputStream output) throws IOException;

   /**
    * Reads and removes a field value from the set. The field is specified as a tag value composed of the numeric id of
    * the field and the wire type. It's possible that the tag has repeated values; in that case the first one is
    * returned.
    *
    * @param tag the field tag (containing both field id and wire type).
    * @param <A> The expected type of the tag value.
    * @return the first seen value or null if the tag was not found.
    */
   <A> A consumeTag(int tag);

   /**
    * Checks if a tag is present.
    *
    * @param tag the field tag (containing both field id and wire type).
    * @return true if present, false otherwise
    */
   boolean hasTag(int tag);
}
