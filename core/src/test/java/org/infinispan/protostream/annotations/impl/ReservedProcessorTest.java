package org.infinispan.protostream.annotations.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.types.ReflectionTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.junit.jupiter.api.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class ReservedProcessorTest {

   // TODO [anistor] things to also test:
   //   empty field name
   //   duplicate field name
   //   illegal field name eg. "--?77fff"
   //   45 to 45
   //   45 to 5
   // messages from protoc:
   //   test.proto:3:9: Field name "field2222" is reserved multiple times
   //   test.proto: Reserved range 13 to 13 overlaps with already defined range 13 to 536870911.

   @Test
   public void testEmpty() {
      XClass classToTest = new ReflectionTypeFactory().fromClass(Integer.class);

      ReservedProcessor rp = new ReservedProcessor();
      rp.scan(classToTest);

      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      rp.generate(iw);

      assertEquals("", sw.toString());
   }

   @ProtoReserved(numbers = {1, 2}, ranges = @ProtoReserved.Range(from = 3, to = 7), names = {"a", "b"})
   private static class TestingReserved {
   }

   @Test
   public void testReservedNumbers() {
      XClass classToTest = new ReflectionTypeFactory().fromClass(TestingReserved.class);

      ReservedProcessor rp = new ReservedProcessor();
      rp.scan(classToTest);

      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      rp.generate(iw);

      assertEquals("reserved 1 to 7;\nreserved \"a\", \"b\";\n", sw.toString());
   }

   @ProtoReserved({1, 2, 1})
   private static class DuplicateNumber {
   }

   @Test
   public void testDuplicateReservedNumber() {
      var ex = assertThrows(ProtoSchemaBuilderException.class, () -> {
         XClass classToTest = new ReflectionTypeFactory().fromClass(DuplicateNumber.class);

         new ReservedProcessor().scan(classToTest);
      });
      assertTrue(ex.getMessage().contains("Found duplicate @ProtoReserved number 1 in org.infinispan.protostream.annotations.impl.ReservedProcessorTest.DuplicateNumber"));
   }

   @ProtoReserved(names = {"triceratops", "valociraptor", "triceratops"})
   private static class DuplicateName {
   }

   @Test
   public void testDuplicateReservedName() {
      var ex = assertThrows(ProtoSchemaBuilderException.class, () -> {
         XClass classToTest = new ReflectionTypeFactory().fromClass(DuplicateName.class);

         new ReservedProcessor().scan(classToTest);
      });
      assertTrue(ex.getMessage().contains("Found duplicate @ProtoReserved name \"triceratops\" in org.infinispan.protostream.annotations.impl.ReservedProcessorTest.DuplicateName"));
   }
}
