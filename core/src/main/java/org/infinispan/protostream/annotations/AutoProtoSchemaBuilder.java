package org.infinispan.protostream.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.infinispan.protostream.SerializationContextInitializer;

/**
 * Generates compile-time auto-implementations of {@link SerializationContextInitializer}. Annotate a class or interface
 * extending from {@link SerializationContextInitializer} with this annotation and a new concrete public class named
 * based on {@link #className}, having a default no-arguments public constructor will be generated at compile time in
 * the same package. The implementations of the methods from {@link SerializationContextInitializer} will be generated
 * based on the information provided in the attributes of this annotation.
 * <p>
 * This annotation is used at compile time annotation processing only and should not be relied upon at runtime, so its
 * retention is set to {@link RetentionPolicy#CLASS}.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.CLASS)
public @interface AutoProtoSchemaBuilder {

   /**
    * The name of the generated implementation class (optional). If missing, the name of the current class plus the
    * "Impl" suffix is assumed by default.
    */
   String className() default "";

   /**
    * The generated Protobuf schema file name (optional). Must end with ".proto" suffix. The schema will be registered
    * under this name in the {@link org.infinispan.protostream.SerializationContext}. If missing, the simple name of the
    * annotated element will be used plus the ".proto" suffix.
    */
   String schemaFileName() default "";

   /**
    * Generated Protobuf schema resource file path (optional). If this is present then a resource file is generated in
    * the designated path, with the given file name, and will be available to the ClassLoader at runtime, otherwise the
    * generated schema file is directly baked as a String constant into the generated class.
    */
   String schemaFilePath() default "";

   /**
    * Package of the generated Protobuf schema. This is optional. If the package name is not specified then the
    * generated Protobuf types will end up in the unnamed/default package.
    */
   String schemaPackageName() default "";

   /**
    * Alias for {@link #basePackages}. {@code value} and {@link #basePackages} are mutually exclusive.
    */
   String[] value() default {};

   /**
    * The list of packages to scan (optional). The packages are scanned for annotated classes recursively. If {@code
    * basePackages} is empty then all packages are considered. The packages are filtered based on the {@link
    * #includeClasses}/{@link #excludeClasses} filter. If neither {@link #includeClasses} nor {@code basePackages} was
    * specified then the entire source path will be scanned. This last option should only be used in very simple demo
    * projects.
    */
   String[] basePackages() default {};

   /**
    * Annotated classes to process (optional). These classes must be located in the packages (or the subpackages) listed
    * under {@link #basePackages} (if specified) or they will be skipped. If {@code includeClasses} is empty, all {@code
    * ProtoXyz} annotated classes that belong to the packages listed in {@link #basePackages} will be scanned. If
    * neither {@code includeClasses} nor {@link #basePackages} was specified then the entire source path will be
    * scanned. This last option should only be used in very simple demo projects.
    */
   Class<?>[] includeClasses() default {};

   /**
    * Classes to be explicitly excluded.
    */
   Class<?>[] excludeClasses() default {};

   /**
    * Indicates if we accept classes not explicitly included by {@link #includeClasses} and {@link #basePackages} to be
    * auto-detected by reference from the already included classes and to be added automatically. If this is set to
    * {@code false} it fails if such a case is encountered.
    */
   boolean autoImportClasses() default false;

   /**
    * Enable generation of a {@code META-INF/services} file for the generated class of the {@link
    * SerializationContextInitializer} implementation to be loadable by the {@link java.util.ServiceLoader}. This is
    * optional and provided for convenience.
    */
   boolean service() default false;

   /**
    * The initializers to execute before this one. List here classes or interfaces annotated with {@code
    * AutoProtoSchemaBuilder} from which a {@link SerializationContextInitializer} is being generated at compile-time
    * annotation processing.
    */
   Class<? extends SerializationContextInitializer>[] dependsOn() default {};
}
