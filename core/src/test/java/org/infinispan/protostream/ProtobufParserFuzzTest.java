package org.infinispan.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Label;

import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Coverage-guided fuzzing of the event-based {@link ProtobufParser}.
 * <p>
 * The invariant under fuzz: parsing arbitrary bytes either completes or reports the malformed
 * input as an {@link IOException} (typically {@link MalformedProtobufException}); no unchecked
 * exception may escape.
 * <p>
 * In regression mode (a plain {@code mvn test}) the seed corpus under
 * {@code src/test/resources/org/infinispan/protostream/ProtobufParserFuzzTestInputs} is replayed.
 * Enable fuzzing mode with {@code JAZZER_FUZZ=1} (or the {@code fuzz} profile); Jazzer runs a
 * single fuzz test per test run, so select it explicitly, e.g.
 * {@code mvn test -Pfuzz -Dtest=ProtobufParserFuzzTest}.
 *
 * @since 6.0.14
 * @see <a href="https://github.com/infinispan/protostream/issues/755">#755</a>
 */
public class ProtobufParserFuzzTest {

   private static final Descriptor DESCRIPTOR = new Descriptor.Builder()
         .withName("FuzzMsg").withFullName("FuzzMsg")
         .addField(new FieldDescriptor.Builder().withNumber(1).withName("f_string").withTypeName("string"))
         .addField(new FieldDescriptor.Builder().withNumber(2).withName("f_int32").withTypeName("int32"))
         .addField(new FieldDescriptor.Builder().withNumber(3).withName("f_double").withTypeName("double"))
         .addField(new FieldDescriptor.Builder().withNumber(4).withName("f_bool").withTypeName("bool"))
         .addField(new FieldDescriptor.Builder().withNumber(5).withName("f_bytes").withTypeName("bytes"))
         .addField(new FieldDescriptor.Builder().withNumber(6).withName("f_uint64").withTypeName("uint64"))
         .addField(new FieldDescriptor.Builder().withNumber(16).withName("r_int32").withTypeName("int32").withLabel(Label.REPEATED))
         .addField(new FieldDescriptor.Builder().withNumber(17).withName("r_string").withTypeName("string").withLabel(Label.REPEATED))
         .build();

   @FuzzTest(maxDuration = "5m")
   void fuzzProtobufParser(byte[] data) {
      try {
         ProtobufParser.INSTANCE.parse(new TagHandler() {
         }, DESCRIPTOR, data);
      } catch (IOException e) {
         // malformed input reported as an IOException: acceptable
      }
   }
}
