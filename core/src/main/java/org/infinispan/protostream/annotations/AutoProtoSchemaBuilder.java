package org.infinispan.protostream.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContext;
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
 * <p>
 * <em>NOTE:</em> This annotation can also be used on packages but this kind of usage has been deprecated in 4.3.4 and
 * will be removed in version 5. See <a href="https://issues.redhat.com/browse/IPROTO-157">IPROTO-157</a>.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.CLASS)
public @interface AutoProtoSchemaBuilder {

   /**
    * The name of the generated Java implementation class (optional). If missing, the name of the current class plus the
    * "Impl" suffix is assumed by default.
    */
   String className() default "";

   /**
    * The generated Protobuf schema file name (optional). It can contain {@code '/'} characters, so it might appear like
    * a relative or absolute file name. Must end with ".proto" suffix. The schema will be registered under this name in
    * the {@link SerializationContext} by {@link SerializationContextInitializer#registerSchema(SerializationContext)}.
    * If missing, the simple name of the annotated class plus the ".proto" suffix will be used by default.
    */
   String schemaFileName() default "";

   /**
    * Generated Protobuf schema resource file path (optional). If this is present then a resource file is generated in
    * the designated path, with the given file name, and will be available to the ClassLoader at runtime, otherwise the
    * generated schema file is directly baked as a String constant into the generated class and no resource file is
    * generated.
    */
   String schemaFilePath() default "";

   /**
    * Package of the generated Protobuf schema. This is optional. If the package name is not specified then the
    * unnamed/default package is assumed.
    */
   String schemaPackageName() default "";

   /**
    * A handy alias for {@link #basePackages}. {@code value} and {@link #basePackages} are mutually exclusive.
    * See {@link #basePackages} for usage.
    */
   String[] value() default {};

   /**
    * The list of packages to scan (optional). {@code basePackages} and {@link #includeClasses} are mutually exclusive.
    * The packages are scanned for annotated classes recursively. If {@code basePackages} is empty then all packages are
    * considered, starting from root and including the default (unnamed) package. The packages are filtered based on the
    * {@link #excludeClasses} filter. If neither {@link #includeClasses} nor {@code basePackages} is specified then the
    * entire source path is scanned. Be wary of using this last option in anything but very simple demo projects.
    */
   String[] basePackages() default {};

   /**
    * Annotated classes to process (optional). If {@code includeClasses} is empty, all {@code ProtoXyz} annotated
    * classes that belong to the packages listed in {@link #basePackages} will be scanned. If neither
    * {@code includeClasses} nor {@link #basePackages} was specified then the entire source path will be
    * scanned. Be wary of using this last option in anything but very simple demo projects.
    */
   Class<?>[] includeClasses() default {};

   /**
    * Classes to be explicitly excluded. {@code excludeClasses} and {@link #includeClasses} are mutually exclusive. This
    * can be used together with {@link #basePackages}.
    */
   Class<?>[] excludeClasses() default {};

   /**
    * Indicates if we accept classes not explicitly included by the {@link #includeClasses} or {@link #basePackages} to
    * be auto-detected by reference from the already included classes and to be added automatically. If this is set to
    * {@code false} (which is the default) it results in a compilation error when such a case is encountered.
    *
    * @deprecated since 4.3.4. This will be removed in version 5. See <a href="https://issues.redhat.com/browse/IPROTO-158">IPROTO-158</a>.
    */
   @Deprecated
   boolean autoImportClasses() default false;

   /**
    * Enable generation of a {@code META-INF/services} file for the generated implementation class of the {@link
    * SerializationContextInitializer} to be loadable by the {@link java.util.ServiceLoader}. This defaults to {@code true}.
    * The ProtoStream library does not make any use of the {@link java.util.ServiceLoader} to benefit from this
    * mechanism but the user's application is free to use it.
    */
   boolean service() default true;

   /**
    * Generate only the marshallers and skip the schema file.
    * <p>
    * The schema is actually always generated at compile time, in memory, so that various validations can be performed
    * at compile time, but with this flag you effectively ensure it finally gets excluded from both the generated source
    * code and the generated resource files and it does not get registered at runtime by this
    * {@link SerializationContextInitializer} implementation. This flag is useful in cases where you want to register
    * the schema manually for whatever reason or the schema is already provided/registered by other parts of your
    * application.
    * <p>
    * This option conflicts with {@link #schemaFilePath()} and they cannot be used together. Also, this option cannot be
    * set to {@code true} if the annotated element is a subtype of {@link GeneratedSchema},
    * which is expected to always provide a generated schema, as the name implies.
    */
   boolean marshallersOnly() default false;

   /**
    * The {@link SerializationContextInitializer}s that must be executed before this one. Classes or interfaces listed
    * here must implement {@link SerializationContextInitializer} and must also be annotated with
    * {@code AutoProtoSchemaBuilder}. Classes not annotated with {@link AutoProtoSchemaBuilder} will result in a
    * compilation error.
    */
   Class<? extends SerializationContextInitializer>[] dependsOn() default {};
}
