package org.infinispan.protostream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Coverage-guided fuzzing of the {@link ProtobufUtil} wrapped-message envelope helpers.
 * <p>
 * The invariant under fuzz: any input is either rejected loudly with
 * {@link MalformedProtobufException}, or splits into parts that round-trip through
 * {@link ProtobufUtil#joinWrappedMessage} back to the same parts.
 * <p>
 * In regression mode (a plain {@code mvn test}) the seed corpus under
 * {@code src/test/resources/org/infinispan/protostream/ProtobufUtilFuzzTestInputs} is replayed.
 * Enable fuzzing mode with {@code JAZZER_FUZZ=1} (or the {@code fuzz} profile); Jazzer runs a
 * single fuzz test per test run, so select it explicitly, e.g.
 * {@code mvn test -Pfuzz -Dtest=ProtobufUtilFuzzTest}.
 *
 * @since 6.0.14
 * @see <a href="https://github.com/infinispan/protostream/issues/755">#755</a>
 */
public class ProtobufUtilFuzzTest {

   @FuzzTest(maxDuration = "5m")
   void fuzzSplitJoinWrappedMessage(byte[] data) throws IOException {
      ProtobufUtil.WrappedMessageParts parts;
      try {
         parts = ProtobufUtil.splitWrappedMessage(data);
      } catch (MalformedProtobufException e) {
         return; // malformed input rejected loudly: acceptable
      }
      if (parts.typeName() != null && parts.typeId() != null) {
         // mutually exclusive identifiers: join rejects this combination by design
         return;
      }
      byte[] joined;
      try {
         joined = ProtobufUtil.joinWrappedMessage(parts.typeName(), parts.typeId(), parts.message());
      } catch (IllegalArgumentException e) {
         throw new AssertionError("join rejected parts produced by split", e);
      }
      // the joined envelope must split back to the same parts
      ProtobufUtil.WrappedMessageParts parts2;
      try {
         parts2 = ProtobufUtil.splitWrappedMessage(joined);
      } catch (MalformedProtobufException e) {
         throw new AssertionError("split(join(split(data))) failed on split-valid input", e);
      }
      assertEquals(parts.typeName(), parts2.typeName());
      assertEquals(parts.typeId(), parts2.typeId());
      assertArrayEquals(parts.message(), parts2.message());
   }
}
