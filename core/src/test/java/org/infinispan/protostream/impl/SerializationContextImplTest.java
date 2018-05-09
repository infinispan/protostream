package org.infinispan.protostream.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author anistor@redhat.com
 */
public class SerializationContextImplTest {

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   private SerializationContextImpl createContext() {
      return (SerializationContextImpl) ProtobufUtil.newSerializationContext(Configuration.builder().build());
   }

   @Test
   public void testRegisterProtoFiles() {
      SerializationContextImpl ctx = createContext();

      String file1 = "syntax = \"proto3\";\n" +
            "package p;\n" +
            "message A {\n" +
            "   optional int32 f1 = 1;\n" +
            "}";

      String file2 = "package org.infinispan;\n" +
            "import \"file1.proto\";\n" +
            "message B {\n" +
            "   required b.A ma = 1;\n" +
            "}";

      Map<String, DescriptorParserException> failed = new HashMap<>();
      List<String> successful = new ArrayList<>();
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource()
            .addProtoFile("file1.proto", file1)
            .addProtoFile("file2.proto", file2)
            .withProgressCallback(new FileDescriptorSource.ProgressCallback() {
               @Override
               public void handleError(String fileName, DescriptorParserException exception) {
                  failed.put(fileName, exception);
               }

               @Override
               public void handleSuccess(String fileName) {
                  successful.add(fileName);
               }
            });
      ctx.registerProtoFiles(fileDescriptorSource);

      assertEquals(1, failed.size());
      assertEquals(1, successful.size());
      DescriptorParserException exception = failed.get("file2.proto");
      assertNotNull(exception);
      assertEquals("Field type b.A not found", exception.getMessage());
      assertTrue(successful.contains("file1.proto"));

      Map<String, FileDescriptor> fileDescriptors = ctx.getFileDescriptors();

      assertEquals(3, fileDescriptors.size());

      FileDescriptor fd1 = fileDescriptors.get("file1.proto");
      assertNotNull(fd1);
      assertEquals(1, fd1.getTypes().size());
      assertTrue(fd1.getTypes().containsKey("p.A"));

      FileDescriptor fd2 = fileDescriptors.get("file2.proto");
      assertNotNull(fd2);
      try {
         fd2.getTypes();
         fail("IllegalStateException expected");
      } catch (IllegalStateException e) {
         assertEquals("File file2.proto is not resolved yet", e.getMessage());
      }
   }

   // ColorType1 should not be an Enum
   enum ColorType1 {
      GREEN, RED;
   }

   @Test
   public void testRegisterImproperMarshaller1() {
      exception.expect(IllegalArgumentException.class);
      exception.expectMessage("Invalid marshaller (the produced class is a Java Enum but the marshaller is not an EnumMarshaller)");

      SerializationContextImpl ctx = createContext();

      String file = "package test;\n" +
            "message Color {\n" +
            "   optional int32 color = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource().addProtoFile("file.proto", file);
      ctx.registerProtoFiles(fileDescriptorSource);

      ctx.registerMarshaller(new MessageMarshaller<ColorType1>() {
         @Override
         public Class<ColorType1> getJavaClass() {
            return ColorType1.class;
         }

         @Override
         public String getTypeName() {
            return "test.Color";
         }

         @Override
         public ColorType1 readFrom(ProtoStreamReader reader) {
            // never invoked
            return null;
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, ColorType1 color) {
            // never invoked
         }
      });
   }

   // ColorType2 should be an Enum
   class ColorType2 {
      public int rgb;
   }

   @Test
   public void testRegisterImproperMarshaller2() {
      exception.expect(IllegalArgumentException.class);
      exception.expectMessage("test.Color is not a message type");

      SerializationContextImpl ctx = createContext();

      String file = "package test;\n" +
            "enum Color {\n" +
            "   GREEN = 1;\n" +
            "   RED = 2;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource().addProtoFile("file.proto", file);
      ctx.registerProtoFiles(fileDescriptorSource);

      ctx.registerMarshaller(new MessageMarshaller<ColorType2>() {
         @Override
         public Class<ColorType2> getJavaClass() {
            return ColorType2.class;
         }

         @Override
         public String getTypeName() {
            return "test.Color";
         }

         @Override
         public ColorType2 readFrom(ProtoStreamReader reader) {
            // never invoked
            return null;
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, ColorType2 color) {
            // never invoked
         }
      });
   }

   @Test
   public void testMarshallerProvider() throws Exception {
      SerializationContextImpl ctx = createContext();

      String file = "package test;\n" +
            "message X {\n" +
            "   optional int32 f = 1;\n" +
            "}";

      class X {

         Integer f;

         private X(Integer f) {
            this.f = f;
         }
      }

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource().addProtoFile("file.proto", file);
      ctx.registerProtoFiles(fileDescriptorSource);

      ctx.registerMarshallerProvider(new SerializationContext.MarshallerProvider() {
         @Override
         public BaseMarshaller<?> getMarshaller(String typeName) {
            if (typeName.equals("test.X")) {
               return makeMarshaller();
            }
            return null;
         }

         @Override
         public BaseMarshaller<?> getMarshaller(Class<?> javaClass) {
            if (javaClass == X.class) {
               return makeMarshaller();
            }
            return null;
         }

         private BaseMarshaller<?> makeMarshaller() {
            return new RawProtobufMarshaller<X>() {

               @Override
               public X readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in) throws IOException {
                  Integer f = null;
                  if (in.readTag() == WireFormat.makeTag(1, WireFormat.WIRETYPE_VARINT)) {
                     f = in.readInt32();
                  }
                  return new X(f);
               }

               @Override
               public void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out, X x) throws IOException {
                  out.writeInt32(1, x.f);
               }

               @Override
               public Class<X> getJavaClass() {
                  return X.class;
               }

               @Override
               public String getTypeName() {
                  return "test.X";
               }
            };
         }
      });

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new X(1234));
      Object out = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(out instanceof X);
      assertNotNull(((X) out).f);
      assertEquals(1234, ((X) out).f.intValue());
   }

   @Test
   public void testTwoFilesWithErrorsAtOnce() {
      SerializationContextImpl ctx = createContext();
      Map<String, Throwable> errors = new HashMap<>();
      List<String> successful = new ArrayList<>();
      FileDescriptorSource source = FileDescriptorSource.fromString("test1.proto", "kabooom1")
            .addProtoFile("test2.proto", "kabooom2")
            .withProgressCallback(new FileDescriptorSource.ProgressCallback() {
               @Override
               public void handleError(String fileName, DescriptorParserException ex) {
                  errors.put(fileName, ex);
               }

               @Override
               public void handleSuccess(String fileName) {
                  successful.add(fileName);
               }
            });
      ctx.registerProtoFiles(source);

      assertTrue(successful.isEmpty());
      assertEquals(2, errors.size());
      assertTrue(errors.containsKey("test1.proto"));
      assertTrue(errors.containsKey("test2.proto"));
      assertEquals("java.lang.IllegalStateException: Syntax error in test1.proto at 1:9: unexpected label: kabooom1", errors.get("test1.proto").getMessage());
      assertEquals("java.lang.IllegalStateException: Syntax error in test2.proto at 1:9: unexpected label: kabooom2", errors.get("test2.proto").getMessage());
      assertTrue(ctx.getFileDescriptors().containsKey("test1.proto"));
      assertTrue(ctx.getFileDescriptors().containsKey("test2.proto"));
      assertFalse(ctx.getFileDescriptors().get("test1.proto").isResolved());
      assertFalse(ctx.getFileDescriptors().get("test2.proto").isResolved());
   }

   @Test
   public void testTwoFilesWithErrorsSeparately() {
      SerializationContextImpl ctx = createContext();

      Map<String, Throwable> errors1 = new HashMap<>();
      List<String> successful1 = new ArrayList<>();
      FileDescriptorSource source1 = FileDescriptorSource.fromString("test1.proto", "kabooom1")
            .withProgressCallback(new FileDescriptorSource.ProgressCallback() {
               @Override
               public void handleError(String fileName, DescriptorParserException ex) {
                  errors1.put(fileName, ex);
               }

               @Override
               public void handleSuccess(String fileName) {
                  successful1.add(fileName);
               }
            });
      Map<String, Throwable> errors2 = new HashMap<>();
      List<String> successful2 = new ArrayList<>();
      FileDescriptorSource source2 = FileDescriptorSource.fromString("test2.proto", "kabooom2")
            .withProgressCallback(new FileDescriptorSource.ProgressCallback() {
               @Override
               public void handleError(String fileName, DescriptorParserException ex) {
                  errors2.put(fileName, ex);
               }

               @Override
               public void handleSuccess(String fileName) {
                  successful2.add(fileName);
               }
            });

      ctx.registerProtoFiles(source1);
      ctx.registerProtoFiles(source2);

      assertTrue(successful1.isEmpty());
      assertTrue(successful2.isEmpty());
      assertEquals(1, errors1.size());
      assertEquals(2, errors2.size());
      assertTrue(errors1.containsKey("test1.proto"));
      assertTrue(errors2.containsKey("test1.proto"));
      assertTrue(errors2.containsKey("test2.proto"));
      assertEquals("java.lang.IllegalStateException: Syntax error in test1.proto at 1:9: unexpected label: kabooom1", errors1.get("test1.proto").getMessage());
      assertEquals("java.lang.IllegalStateException: Syntax error in test1.proto at 1:9: unexpected label: kabooom1", errors2.get("test1.proto").getMessage());
      assertEquals("java.lang.IllegalStateException: Syntax error in test2.proto at 1:9: unexpected label: kabooom2", errors2.get("test2.proto").getMessage());
      assertTrue(ctx.getFileDescriptors().containsKey("test1.proto"));
      assertTrue(ctx.getFileDescriptors().containsKey("test2.proto"));
      assertFalse(ctx.getFileDescriptors().get("test1.proto").isResolved());
      assertFalse(ctx.getFileDescriptors().get("test2.proto").isResolved());
   }

   @Test
   public void testUnregisterMissingFiles() {
      exception.expect(IllegalArgumentException.class);
      exception.expectMessage("File test.proto does not exist");

      SerializationContextImpl ctx = createContext();
      ctx.unregisterProtoFile("test.proto");
   }

   @Test
   public void testUnregisterFileWithErrors() {
      SerializationContextImpl ctx = createContext();
      Map<String, Throwable> errors = new HashMap<>();
      List<String> successful = new ArrayList<>();
      FileDescriptorSource source = FileDescriptorSource.fromString("test.proto", "kabooom")
            .withProgressCallback(new FileDescriptorSource.ProgressCallback() {
               @Override
               public void handleError(String fileName, DescriptorParserException ex) {
                  errors.put(fileName, ex);
               }

               @Override
               public void handleSuccess(String fileName) {
                  successful.add(fileName);
               }
            });
      ctx.registerProtoFiles(source);

      assertTrue(successful.isEmpty());
      assertEquals(1, errors.size());
      assertTrue(errors.containsKey("test.proto"));
      assertEquals("java.lang.IllegalStateException: Syntax error in test.proto at 1:8: unexpected label: kabooom", errors.get("test.proto").getMessage());
      assertTrue(ctx.getFileDescriptors().containsKey("test.proto"));
      assertFalse(ctx.getFileDescriptors().get("test.proto").isResolved());
      ctx.unregisterProtoFile("test.proto");

      assertFalse(ctx.getFileDescriptors().containsKey("test.proto"));
   }

   @Test
   public void testFileCanExistWithSemanticErrors() {
      SerializationContextImpl ctx = createContext();
      FileDescriptorSource source = FileDescriptorSource.fromString("file1.proto", "import \"no_such_file.proto\";");

      try {
         ctx.registerProtoFiles(source);
         fail("DescriptorParserException expected");
      } catch (DescriptorParserException e) {
         assertEquals("Import 'no_such_file.proto' not found", e.getMessage());
      }

      FileDescriptor fileDescriptor = ctx.getFileDescriptors().get("file1.proto");
      assertNotNull(fileDescriptor);
      assertFalse(fileDescriptor.isResolved());
   }

   /**
    * Test that files with syntax errors DO NOT get registered if there is no progress callback present.
    */
   @Test
   public void testFileCannotExistWithParsingErrors() {
      SerializationContextImpl ctx = createContext();
      FileDescriptorSource source = FileDescriptorSource.fromString("file1.proto", "this is bogus");

      try {
         ctx.registerProtoFiles(source);
         fail("DescriptorParserException expected");
      } catch (DescriptorParserException e) {
         assertEquals("java.lang.IllegalStateException: Syntax error in file1.proto at 1:5: unexpected label: this", e.getMessage());
      }

      FileDescriptor fileDescriptor = ctx.getFileDescriptors().get("file1.proto");
      assertNull(fileDescriptor);
   }

   /**
    * Test that files with syntax errors DO get registered if there is a progress callback present.
    */
   @Test
   public void testFileCanExistWithParsingErrors() {
      SerializationContextImpl ctx = createContext();
      FileDescriptorSource source = FileDescriptorSource.fromString("file1.proto", "this is bogus")
            .withProgressCallback(new FileDescriptorSource.ProgressCallback() {
            });
      ctx.registerProtoFiles(source);
      FileDescriptor fileDescriptor = ctx.getFileDescriptors().get("file1.proto");
      assertNotNull(fileDescriptor);
      assertFalse(fileDescriptor.isResolved());
   }

   @Test
   public void testDuplicateTypeIdInDifferentFiles() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate type id 10 for type test2.M2. Already used by test1.M1");

      String file1 = "package test1;\n" +
            "/**@TypeId(10)*/\n" +
            "message M1 {\n" +
            "   optional string a = 1;\n" +
            "}";
      String file2 = "package test2;\n" +
            "/**@TypeId(10)*/\n" +
            "message M2 {\n" +
            "   optional string b = 1;\n" +
            "}";

      FileDescriptorSource source = new FileDescriptorSource()
            .addProtoFile("test1.proto", file1)
            .addProtoFile("test2.proto", file2);

      SerializationContextImpl ctx = createContext();
      ctx.registerProtoFiles(source);
   }

   @Test
   public void testDuplicateTypeIdInSameFile() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate type id 10 for type test1.M2. Already used by test1.M1");

      String file1 = "package test1;\n" +
            "/**@TypeId(10)*/\n" +
            "message M1 {\n" +
            "   optional string a = 1;\n" +
            "}" +
            "/**@TypeId(10)*/\n" +
            "message M2 {\n" +
            "   optional string b = 1;\n" +
            "}";

      SerializationContextImpl ctx = createContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromString("test1.proto", file1));
   }

   @Test
   public void testEnumConstantNameClashesWithOtherType1() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum value test1.E1.M1 clashes with message definition test1.M1");

      String file1 = "package test1;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}\n" +
            "enum E1 {\n" +
            "  M1 = 1;\n" +
            "}";

      SerializationContextImpl ctx = createContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromString("test1.proto", file1));
   }

   @Test
   public void testEnumConstantNameClashesWithOtherType2() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum value test1.E1.M1 clashes with message definition test1.M1");

      String file1 = "package test1;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "package test1;\n" +
            "enum E1 {\n" +
            "  M1 = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource()
            .addProtoFile("test_proto_path/file1.proto", file1)
            .addProtoFile("test_proto_path/file2.proto", file2);

      SerializationContextImpl ctx = createContext();
      ctx.registerProtoFiles(fileDescriptorSource);
   }

   @Test
   public void testEnumConstantNameClashesWithOtherType3() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum value test1.E1.M1 clashes with message definition test1.M1");

      String file1 = "package test1;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "package test1;\n" +
            "enum E1 {\n" +
            "  M1 = 1;\n" +
            "}";

      SerializationContextImpl ctx = createContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromString("test_proto_path/file1.proto", file1));
      ctx.registerProtoFiles(FileDescriptorSource.fromString("test_proto_path/file2.proto", file2));
   }
}
