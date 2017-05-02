package org.infinispan.protostream.impl;

import java.io.ByteArrayOutputStream;

import org.infinispan.protostream.descriptors.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author anistor@redhat.com
 */
public class EncoderTest {

   @Test
   public void testWriteBool() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Encoder encoder = Encoder.newInstance(baos, 100);
      encoder.writeBoolField(5, true);
      encoder.flush();

      byte[] bytes = baos.toByteArray();
      assertEquals(2, bytes.length);
      assertEquals(WireType.makeTag(5, WireType.VARINT), bytes[0]);
      assertEquals(1, bytes[1]);
   }
}
