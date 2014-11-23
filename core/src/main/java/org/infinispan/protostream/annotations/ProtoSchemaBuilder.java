package org.infinispan.protostream.annotations;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.ProtoSchemaGenerator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates a Protocol Buffers schema definition file based on a set of @Proto* annotated classes.
 * <p/>
 * See {@link ProtoMessage}, {@link ProtoField}, {@link ProtoEnum}, {@link ProtoEnumValue} and {@link
 * ProtoUnknownFieldSet}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoSchemaBuilder {

   private String fileName;

   private String packageName;

   private final Set<Class<?>> classes = new HashSet<Class<?>>();

   public ProtoSchemaBuilder() {
   }

   public ProtoSchemaBuilder fileName(String fileName) {
      this.fileName = fileName;
      return this;
   }

   public ProtoSchemaBuilder packageName(String packageName) {
      if (packageName.trim().isEmpty()) {
         throw new IllegalArgumentException("packageName cannot be empty");
      }
      this.packageName = packageName;
      return this;
   }

   public ProtoSchemaBuilder addClass(Class<?> clazz) {
      classes.add(clazz);
      return this;
   }

   /**
    * Builds the Protocol Buffers schema file and marshallers and registers them with  the given {@link
    * SerializationContext}.
    *
    * @param serializationContext
    * @return the generated Protocol Buffers schema file
    * @throws ProtoSchemaBuilderException
    * @throws IOException
    */
   public String build(SerializationContext serializationContext) throws ProtoSchemaBuilderException, IOException {
      if (fileName == null) {
         throw new ProtoSchemaBuilderException("fileName cannot be null");
      }
      if (classes.isEmpty()) {
         throw new ProtoSchemaBuilderException("At least one class must be specified");
      }
      return new ProtoSchemaGenerator(serializationContext, fileName, packageName, classes).generateAndRegister();
   }
}
