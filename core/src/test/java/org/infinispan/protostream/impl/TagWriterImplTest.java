package org.infinispan.protostream.impl;

import java.io.ByteArrayOutputStream;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author anistor@redhat.com
 */
public class TagWriterImplTest {

   @Test
   public void testWriteBool() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      TagWriterImpl tagWriter = TagWriterImpl.newInstance(ctx, baos, 100);
      tagWriter.writeBool(5, true);
      tagWriter.flush();

      byte[] bytes = baos.toByteArray();
      assertEquals(2, bytes.length);
      assertEquals(WireType.makeTag(5, WireType.VARINT), bytes[0]);
      assertEquals(1, bytes[1]);
   }
}
