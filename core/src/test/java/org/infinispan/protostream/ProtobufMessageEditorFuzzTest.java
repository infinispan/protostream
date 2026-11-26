package org.infinispan.protostream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Label;

import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Coverage-guided fuzzing of the wire-format scanner and field decoder of
 * {@link ProtobufMessageEditor}.
 * <p>
 * The invariant under fuzz: any input is either indexed successfully — in which case the verbatim
 * pass-through guarantee requires an unedited editor to emit byte-identical output, and decoding
 * the individual fields must not escape unchecked exceptions — or rejected loudly with
 * {@link MalformedProtobufException}.
 * <p>
 * In regression mode (a plain {@code mvn test}) the seed corpus under
 * {@code src/test/resources/org/infinispan/protostream/ProtobufMessageEditorFuzzTestInputs} is
 * replayed. Enable fuzzing mode with {@code JAZZER_FUZZ=1} (or the {@code fuzz} profile); Jazzer
 * runs a single fuzz test per test run, so select it explicitly, e.g.
 * {@code mvn test -Pfuzz -Dtest=ProtobufMessageEditorFuzzTest}.
 *
 * @since 6.0.14
 * @see <a href="https://github.com/infinispan/protostream/issues/755">#755</a>
 */
public class ProtobufMessageEditorFuzzTest {

   private static final Descriptor DESCRIPTOR = new Descriptor.Builder()
         .withName("FuzzMsg").withFullName("FuzzMsg")
         // one field of every packable/length-delimited declaration, to exercise each decoder
         .addField(new FieldDescriptor.Builder().withNumber(1).withName("f_string").withTypeName("string"))
         .addField(new FieldDescriptor.Builder().withNumber(2).withName("f_int32").withTypeName("int32"))
         .addField(new FieldDescriptor.Builder().withNumber(3).withName("f_sint64").withTypeName("sint64"))
         .addField(new FieldDescriptor.Builder().withNumber(4).withName("f_fixed32").withTypeName("fixed32"))
         .addField(new FieldDescriptor.Builder().withNumber(5).withName("f_double").withTypeName("double"))
         .addField(new FieldDescriptor.Builder().withNumber(6).withName("f_bool").withTypeName("bool"))
         .addField(new FieldDescriptor.Builder().withNumber(7).withName("f_bytes").withTypeName("bytes"))
         .addField(new FieldDescriptor.Builder().withNumber(8).withName("f_uint64").withTypeName("uint64"))
         .addField(new FieldDescriptor.Builder().withNumber(9).withName("f_float").withTypeName("float"))
         // repeated packable fields, to exercise the packed-decoding path
         .addField(new FieldDescriptor.Builder().withNumber(16).withName("r_int32").withTypeName("int32").withLabel(Label.REPEATED))
         .addField(new FieldDescriptor.Builder().withNumber(17).withName("r_string").withTypeName("string").withLabel(Label.REPEATED))
         .addField(new FieldDescriptor.Builder().withNumber(18).withName("r_fixed64").withTypeName("fixed64").withLabel(Label.REPEATED))
         .addField(new FieldDescriptor.Builder().withNumber(19).withName("r_bool").withTypeName("bool").withLabel(Label.REPEATED))
         .build();

   @FuzzTest(maxDuration = "5m")
   void fuzzEditor(byte[] data) throws IOException {
      ProtobufMessageEditor editor;
      try {
         editor = ProtobufMessageEditor.of(DESCRIPTOR, data);
      } catch (MalformedProtobufException e) {
         return; // malformed input rejected loudly: acceptable
      }
      // verbatim pass-through guarantee: an unedited editor emits byte-identical output
      assertArrayEquals(data, editor.toByteArray());
      // decoding individual fields of successfully indexed data must not escape unchecked exceptions
      for (FieldDescriptor fd : DESCRIPTOR.getFields()) {
         try {
            editor.readRepeated(fd);
         } catch (MalformedProtobufException e) {
            // an occurrence whose wire type is incompatible with the declared type: acceptable
         }
      }
   }
}
