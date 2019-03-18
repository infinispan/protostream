package org.infinispan.protostream.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * Generates compile-time auto-implementations of {@link SerializationContextInitializer}. Annotate a class or interface
 * extending from {@link SerializationContextInitializer} with this annotation and a new concrete public class named
 * XyzImpl having a default no-arguments public constructor will be generated at compile time. The implementations of
 * the methods from {@link SerializationContextInitializer} will be generated based on the information provided in the
 * attributes of this annotation.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoProtoSchemaBuilder {

   /**
    * The name of the generated implementation class (optional). If missing, the name of the current class plus the
    * "Impl" suffix is assumed by default.
    */
   String className() default "";

   /**
    * Generated Protobuf schema file name (required). Must end with ".proto" suffix.
    */
   String fileName();

   /**
    * Generated Protobuf schema resource path (optional). If this is present then a resource file is generated in the
    * designated path, with the given file name, and will be available to the ClassLoader at runtime, otherwise the
    * generated schema is directly baked as a String constant into the generated class.
    */
   String filePath() default "";

   /**
    * Package of the generated Protobuf schema. This is optional. If the package name is not specified then the
    * generated Protobuf types will end up in the unnamed/default package.
    */
   String packageName() default "";

   /**
    * Annotated classes to process (optional).  If missing, all @ProtoXyz annotated classes that belong to the packages
    * listed in {@link #packages} will be scanned.
    */
   Class[] classes() default {};

   /**
    * The packages to scan if {@link #classes} is not specified (the cannot be both present). The packages are scanned
    * recursively. If neither {@link #classes} nor {@code packages} was specified then all source files will be
    * scanned.
    */
   String[] packages() default {};

   /**
    * Do we accept classes not explicitly added in {@link #classes} to be auto-detected by reference from the already
    * added classes and be included automatically or should we fail if such a case is encountered? This option can only
    * be set to {@code false} if {@link #classes} is non-empty.
    */
   boolean autoImportClasses() default true;

   /**
    * Do we also generate a META-INF/services file for the generated implementation of the
    * SerializationContextInitializer implementation to be loadable by the java.util.ServiceLoader.
    */
   boolean service() default false;

   /**
    * Schema resource paths to import in the {@link org.infinispan.protostream.SerializationContext} <em>before</em>
    * starting the generation (optional). These resource files are used at compile time only and they do not need to be
    * loadable at runtime.
    */
   String[] schemaResources() default {};  //TODO implement!

   /**
    * Marshallers to register in the {@link org.infinispan.protostream.SerializationContext} <em>before</em> starting
    * the generation (optional).
    */
   Class<? extends BaseMarshaller<?>>[] marshallers() default {}; //TODO implement!
}
