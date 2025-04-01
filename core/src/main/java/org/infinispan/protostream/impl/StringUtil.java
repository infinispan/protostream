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
import java.util.function.ToIntFunction;

import sun.misc.Unsafe;

class StringUtil {

   interface Utf8Helper {
      boolean providesLatin1Bytes();
      byte[] getBytes(String s);
   }

   private static final Log log = Log.LogFactory.getLog(ProtoStreamReaderImpl.class);
   private static final Byte LATIN1 = (byte) 0;

   static volatile Utf8Helper INSTANCE;

   static Utf8Helper getUtf8Helper() {
      if (INSTANCE == null) {
         // Attempt to use IMPL_LOOKUP so that we utilise a Lambda - Most efficient
         try {
            INSTANCE = createLambdaHelper();
            return INSTANCE;
         } catch (Throwable t) {
            log.warnf(t, "Unable to retrieve MethodHandles required for Lambda based String optimisations");
         }

         // Fallback to MethodHandler - Slightly less efficient and requires  --add-opens java.base/java.lang=ALL-UNNAMED
         try {
            INSTANCE = createFieldHelper();
            return INSTANCE;
         } catch (Throwable t) {
            log.warnf(t, "Unable to retrieve MethodHandles required for field based String optimisations");
         }

         // Coder optimisations not possible
         INSTANCE = new Utf8Helper() {
            @Override
            public boolean providesLatin1Bytes() {
               return false;
            }

            @Override
            public byte[] getBytes(String s) {
               return s.getBytes(StandardCharsets.UTF_8);
            }
         };
      }
      return INSTANCE;
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
      MethodHandle coder = lookup.findSpecial(
            String.class,
            "coder",
            methodType(byte.class),
            String.class
      );
      CallSite applyAsInt = LambdaMetafactory.metafactory(
            lookup,
            "applyAsInt",
            methodType(ToIntFunction.class),
            methodType(int.class, Object.class),
            coder,
            MethodType.methodType(byte.class, String.class)
      );
      ToIntFunction<String> coderFn = (ToIntFunction<String>) applyAsInt.getTarget().invokeExact();

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
            if (coderFn.applyAsInt(s) != LATIN1)
               return s.getBytes(StandardCharsets.UTF_8);

            return valueFn.apply(s);
         }
      };
   }

   private static Utf8Helper createFieldHelper() throws Throwable {
      Method privateMethod = String.class.getDeclaredMethod("coder");
      privateMethod.setAccessible(true);
      MethodHandle STRING_CODER_HANDLER = MethodHandles.lookup().unreflect(privateMethod);

      privateMethod = String.class.getDeclaredMethod("value");
      privateMethod.setAccessible(true);
      MethodHandle STRING_VALUE_HANDLER = MethodHandles.lookup().unreflect(privateMethod);

      return new Utf8Helper() {
         @Override
         public boolean providesLatin1Bytes() {
            return true;
         }

         @Override
         public byte[] getBytes(String s) {
            try {
               if (STRING_CODER_HANDLER.invoke(s) != LATIN1)
                  return s.getBytes(StandardCharsets.UTF_8);

               return (byte[]) STRING_VALUE_HANDLER.invoke(s);
            } catch (Throwable t) {
               throw new RuntimeException(t);
            }
         }
      };
   }
}
