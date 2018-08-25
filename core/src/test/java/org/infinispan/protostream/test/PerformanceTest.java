package org.infinispan.protostream.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.impl.Log;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Compare performance (time, space) versus Java Serialization and JBoss Marshalling.
 * <p>
 * This is a performance test so it is ignored during normal run of the test suite.
 *
 * @author anistor@redhat.com
 */
@Ignore
public class PerformanceTest extends AbstractProtoStreamTest {

   private static final Log log = Log.LogFactory.getLog(PerformanceTest.class);

   public static final int NUM_OUTER_LOOPS = 1000;
   public static final int NUM_INNER_LOOPS = 100000;

   @Test
   public void testProtoStreamWrite() throws Exception {
      User user = createTestObject();

      long[] results = new long[1];
      for (int i = 0; i < NUM_OUTER_LOOPS; i++) {
         log.infof("----------------------- # %d ------------------------", i);

         byte[] bytes = writeWithProtoStream(user, results);
         log.infof("ProtoStream payload length          = %d bytes", bytes.length);
      }
   }

   @Test
   public void testProtoStreamRead() throws Exception {
      User user = createTestObject();

      long[] results = new long[1];
      byte[] bytes = writeWithProtoStream(user, results);

      for (int i = 0; i < NUM_OUTER_LOOPS; i++) {
         log.infof("---------------------- # %d -------------------------", i);

         readWithProtoStream(bytes, results);
      }
   }

   /**
    * We run a total of 100 million writes and reads and expect better performance form ProtoStream than the other
    * marshallers, allowing 5 cases out of 1000 to have bad performance due to unfavorable thread scheduling, time
    * measurement inaccuracies, garbage collection pauses or whatever bad karma.
    */
   @Test
   public void testComparativeReadWrite() throws Exception {
      User user = createTestObject();

      int jinx = 0;
      byte[][] bytes = new byte[3][];
      long[][] results = new long[3][1];
      for (int i = 0; i < NUM_OUTER_LOOPS; i++) {
         log.infof("----------------------- # %d ------------------------", i);

         bytes[0] = writeWithProtoStream(user, results[0]);
         log.infof("ProtoStream payload length          = %d bytes", bytes[0].length);

         bytes[1] = writeWithJavaSerialization(user, results[1]);
         log.infof("Java serialization length           = %d bytes", bytes[1].length);

         bytes[2] = writeWithJBossMarshalling(user, results[2]);
         log.infof("JBoss Marshalling payload length    = %d bytes", bytes[2].length);

         // assert smaller size for Protobuf encoding
         assertTrue(bytes[0].length < bytes[1].length);
         assertTrue(bytes[0].length < bytes[2].length);

         // check better time
         if (/*results[0][0] > results[1][0] || */results[0][0] > results[2][0]) {
            jinx++;
         }
      }

      // allow 5 cases where performance is worse than others
      assertTrue(jinx <= 5);

      jinx = 0;
      for (int i = 0; i < NUM_OUTER_LOOPS; i++) {
         log.infof("---------------------- # %d -------------------------", i);

         readWithProtoStream(bytes[0], results[0]);

         readWithJavaSerialization(bytes[1], results[1]);

         readWithJBossMarshalling(bytes[2], results[2]);

         // check smaller time
         if (results[0][0] > results[1][0] || results[0][0] > results[2][0]) {
            jinx++;
         }
      }

      // allow 5 cases where performance is worse than others
      assertTrue(jinx <= 5);
   }

   private User createTestObject() {
      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      List<Address> addresses = new ArrayList<>();
      addresses.add(new Address("Old Street", "XYZ42", -12));
      addresses.add(new Address("Bond Street", "QQ42", 312));
      user.setAddresses(addresses);
      return user;
   }

   private void readWithProtoStream(byte[] bytes, long[] result) throws IOException, DescriptorParserException {
      Configuration.Builder cfgBuilder = Configuration.builder().setLogOutOfSequenceReads(false);
      SerializationContext ctx = createContext(cfgBuilder);
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_INNER_LOOPS; i++) {
         ProtobufUtil.readFrom(ctx, bais, User.class);
         bais.close();
         bais.reset();
      }

      result[0] = System.nanoTime() - tStart;
      log.infof("ProtoStream read duration           = %d ns", result[0]);
   }

   private void readWithJBossMarshalling(byte[] bytes, long[] result) throws IOException, ClassNotFoundException {
      MarshallingConfiguration configuration = new MarshallingConfiguration();
      configuration.setVersion(3);
      MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");

      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_INNER_LOOPS; i++) {
         Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
         unmarshaller.start(Marshalling.createByteInput(bais));
         unmarshaller.readObject();
         unmarshaller.finish();
         bais.close();
         bais.reset();
      }
      result[0] = System.nanoTime() - tStart;
      log.infof("JBoss marshalling read duration     = %d ns", result[0]);
   }

   private void readWithJavaSerialization(byte[] bytes, long[] result) throws IOException, ClassNotFoundException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_INNER_LOOPS; i++) {
         ObjectInputStream ois = new ObjectInputStream(bais);
         ois.readObject();
         ois.close();
         bais.reset();
      }

      result[0] = System.nanoTime() - tStart;
      log.infof("Java serialization read duration    = %d ns", result[0]);
   }

   private byte[] writeWithProtoStream(User user, long[] result) throws IOException, DescriptorParserException {
      Configuration.Builder cfgBuilder = Configuration.builder().setLogOutOfSequenceWrites(false);
      SerializationContext ctx = createContext(cfgBuilder);
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_INNER_LOOPS; i++) {
         ProtobufUtil.writeTo(ctx, out, user);
         if (i != NUM_INNER_LOOPS - 1) {
            out.reset();
         }
      }
      result[0] = System.nanoTime() - tStart;
      log.infof("ProtoStream write duration          = %d ns", result[0]);
      return out.toByteArray();
   }

   private byte[] writeWithJBossMarshalling(User user, long[] result) throws IOException {
      MarshallingConfiguration configuration = new MarshallingConfiguration();
      configuration.setVersion(3);
      MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");

      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_INNER_LOOPS; i++) {
         Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
         marshaller.start(Marshalling.createByteOutput(out));
         marshaller.writeObject(user);
         marshaller.finish();
         out.close();
         if (i != NUM_INNER_LOOPS - 1) {
            out.reset();
         }
      }
      result[0] = System.nanoTime() - tStart;
      log.infof("JBoss Marshalling write duration    = %d ns", result[0]);
      return out.toByteArray();
   }

   private byte[] writeWithJavaSerialization(User user, long[] result) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_INNER_LOOPS; i++) {
         ObjectOutputStream oos = new ObjectOutputStream(out);
         oos.writeObject(user);
         oos.flush();
         oos.close();
         if (i != NUM_INNER_LOOPS - 1) {
            out.reset();
         }
      }

      result[0] = System.nanoTime() - tStart;
      log.infof("Java Serialization write duration   = %d ns", result[0]);
      return out.toByteArray();
   }
}
