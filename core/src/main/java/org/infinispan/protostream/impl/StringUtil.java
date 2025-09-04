package org.infinispan.protostream.impl;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;

import org.infinispan.protostream.impl.jfr.JfrEventPublisher;
import sun.misc.Unsafe;

class StringUtil {

   interface Utf8Helper {
      boolean providesLatin1Bytes();
      byte[] getBytes(String s);
   }

   private static final Log log = Log.LogFactory.getLog(StringUtil.class);

   static final Utf8Helper UTF8_HELPER = getUtf8Helper();

   private static Utf8Helper getUtf8Helper() {
      // Attempt to use IMPL_LOOKUP so that we utilise a Lambda - Most efficient
      try {
         var helper = createLambdaHelper();
         if (log.isDebugEnabled())
            log.debug("Utilising Lambda based UNSAFE String optimisations");
         return helper;
      } catch (Throwable ignore) {
      }

      // Fallback to MethodHandler - Slightly less efficient and requires  --add-opens java.base/java.lang=ALL-UNNAMED
      try {
         return createFieldHelper();
      } catch (Throwable t) {
         log.unableToRetrieveMethodHandles(t);
      }

      // Coder optimisations not possible
      return new Utf8Helper() {
         @Override
         public boolean providesLatin1Bytes() {
            return false;
         }

         @Override
         public byte[] getBytes(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            JfrEventPublisher.bufferAllocateEvent(b.length);
            return b;
         }
      };
   }

   private static Utf8Helper createLambdaHelper() throws Throwable {
      Class<MethodHandles.Lookup> lookupClass = MethodHandles.Lookup.class;
      Field implLookup = lookupClass.getDeclaredField("IMPL_LOOKUP");

      Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
      long fieldOffset = unsafe.staticFieldOffset(implLookup);
      MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) unsafe.getObject(lookupClass, fieldOffset);
      MethodHandle constructor = trustedLookup.findConstructor(
            MethodHandles.Lookup.class,
            methodType(void.class, Class.class, Class.class, int.class)
      );

      MethodHandles.Lookup lookup = (MethodHandles.Lookup) constructor.invoke(String.class, null, -1);
      MethodHandle isLatin1 = lookup.findSpecial(
            String.class,
            "isLatin1",
            methodType(boolean.class),
            String.class
      );
      CallSite predicate = LambdaMetafactory.metafactory(
            lookup,
            "test",
            methodType(Predicate.class),
            methodType(boolean.class, Object.class),
            isLatin1,
            MethodType.methodType(boolean.class, String.class)
      );
      Predicate<String> isLatin1Predicate = (Predicate<String>) predicate.getTarget().invokeExact();

      MethodHandle value = lookup.findSpecial(
            String.class,
            "value",
            methodType(byte[].class),
            String.class
      );
      CallSite apply = LambdaMetafactory.metafactory(
            lookup,
            "apply",
            methodType(Function.class),
            methodType(Object.class, Object.class),
            value,
            methodType(byte[].class, String.class)
      );
      Function<String, byte[]> valueFn = (Function<String, byte[]>) apply.getTarget().invokeExact();

      return new Utf8Helper() {
         @Override
         public boolean providesLatin1Bytes() {
            return true;
         }

         @Override
         public byte[] getBytes(String s) {
            if (!isLatin1Predicate.test(s))
               return s.getBytes(StandardCharsets.UTF_8);

            return valueFn.apply(s);
         }
      };
   }

   private static Utf8Helper createFieldHelper() throws Throwable {
      Method privateMethod = String.class.getDeclaredMethod("isLatin1");
      privateMethod.setAccessible(true);
      MethodHandle isLatin1Handler = MethodHandles.lookup().unreflect(privateMethod);

      privateMethod = String.class.getDeclaredMethod("value");
      privateMethod.setAccessible(true);
      MethodHandle stringValueHandler = MethodHandles.lookup().unreflect(privateMethod);

      return new Utf8Helper() {
         @Override
         public boolean providesLatin1Bytes() {
            return true;
         }

         @Override
         public byte[] getBytes(String s) {
            try {
               if (!(boolean) isLatin1Handler.invokeExact(s))
                  return s.getBytes(StandardCharsets.UTF_8);

               return (byte[]) stringValueHandler.invokeExact(s);
            } catch (Throwable t) {
               throw new RuntimeException(t);
            }
         }
      };
   }

   public static void main(String[] args) {
      var str = "test";
      System.out.println(StringUtil.UTF8_HELPER.getBytes(str).length);
      System.out.println(str.getBytes(StandardCharsets.UTF_8).length);
   }
}
