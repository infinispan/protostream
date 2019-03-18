package org.infinispan.protostream.annotations.impl.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.IndentWriter;
import org.infinispan.protostream.annotations.impl.processor.types.HasModelElement;
import org.infinispan.protostream.annotations.impl.processor.types.MirrorClassFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XMethod;

import com.google.auto.service.AutoService;

@SupportedOptions(AutoProtoSchemaBuilderAnnotationProcessor.DEBUG_OPTION)
@SupportedAnnotationTypes(AutoProtoSchemaBuilderAnnotationProcessor.PROTO_SCHEMA_TOOL_ANNOTATION_NAME)
@AutoService(Processor.class)
public final class AutoProtoSchemaBuilderAnnotationProcessor extends AbstractProcessor {

   /**
    * The FQN of the one and only annotation we claim.
    */
   public static final String PROTO_SCHEMA_TOOL_ANNOTATION_NAME = "org.infinispan.protostream.annotations.AutoProtoSchemaBuilder";

   public static final String DEBUG_OPTION = "debug";

   private final ServiceLoaderFileGenerator serviceLoaderFileGenerator = new ServiceLoaderFileGenerator(SerializationContextInitializer.class);

   @Override
   public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported(); //TODO [anistor] or maybe return processingEnv.getSourceVersion(), or hardcoded @SupportedSourceVersion(SourceVersion.RELEASE_8) ?
   }

   /**
    * Issue an error.
    */
   private void reportError(Element e, String message, Object... msgParams) {
      String formatted = String.format(message, msgParams);
      if (e != null) {
         processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, formatted, e);
      } else {
         processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, formatted);
      }
   }

   /**
    * Issue a warning.
    */
   private void reportWarning(Element e, String message, Object... msgParams) {
      String formatted = String.format(message, msgParams);
      if (e != null) {
         processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, formatted, e);
      } else {
         processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, formatted);
      }
   }

   /**
    * Log a message, only if debug option is enabled.
    */
   private void log(String message) {
      if (processingEnv.getOptions().containsKey(DEBUG_OPTION)) {
         processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
      }
   }

   //TODO [anistor] this is called multiple times (for multiple rounds). Do we need to ensure we do not generate same files multiple times? see roundEnv.processingOver()
   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      try {
         boolean claimed = annotations.size() == 1 && annotations.iterator().next().getQualifiedName().contentEquals(PROTO_SCHEMA_TOOL_ANNOTATION_NAME);
         if (!claimed && !roundEnv.processingOver()) {
            return false;
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(AutoProtoSchemaBuilder.class)) {
            if (annotatedElement.getKind() != ElementKind.INTERFACE && annotatedElement.getKind() != ElementKind.CLASS) {
               reportError(annotatedElement, "@AutoProtoSchemaBuilder can only be applied to classes and interfaces.");
               return true;
            }

            TypeElement annotatedTypeElement = (TypeElement) annotatedElement;
            AutoProtoSchemaBuilder annotation = annotatedTypeElement.getAnnotation(AutoProtoSchemaBuilder.class);

            Collection<? extends TypeMirror> classes = getClassesToProcess(roundEnv, annotatedTypeElement, annotation);

            try {
               process(annotatedTypeElement, annotation, classes);
            } catch (ProtoSchemaBuilderException e) {
               reportError(annotatedElement, "%s", e.getMessage());
               return true;
            }
         }

         if (roundEnv.processingOver()) {
            serviceLoaderFileGenerator.generate(processingEnv.getFiler());
         }
      } catch (Exception e) {
         reportError(null, "@AutoProtoSchemaBuilder processor threw an exception: %s", getStackTraceAsString(e));
      }

      return true;
   }

   private Collection<? extends TypeMirror> getClassesToProcess(RoundEnvironment roundEnv, TypeElement e, AutoProtoSchemaBuilder annotation) {
      Collection<? extends TypeMirror> specifiedClasses = Collections.emptyList();
      try {
         annotation.classes(); // this is expected to fail
      } catch (MirroredTypesException mte) {
         specifiedClasses = mte.getTypeMirrors();
      }

      Map<String, TypeMirror> classes = new TreeMap<>();  // keep them sorted by FQN for predictable and repeatable order of processing

      if (specifiedClasses.isEmpty()) {
         Set<String> packages = annotation.packages().length != 0 ? new HashSet<>(Arrays.asList(annotation.packages())) : null;

         // no explicit class set specified so we gather all @ProtoXyz annotated classes from source path

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoField.class)) {
            Element enclosingElement = annotatedElement.getEnclosingElement();
            if (enclosingElement.getKind() == ElementKind.CLASS || enclosingElement.getKind() == ElementKind.INTERFACE) {
               TypeElement typeElement = (TypeElement) enclosingElement;
               filterByPackage(classes, typeElement, packages);
            }
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoEnumValue.class)) {
            Element enclosingElement = annotatedElement.getEnclosingElement();
            if (annotatedElement.getKind() != ElementKind.ENUM_CONSTANT || enclosingElement.getKind() != ElementKind.ENUM) {
               reportError(annotatedElement, "@ProtoEnumValue can only be applied to enum constants.");
            }
            TypeElement typeElement = (TypeElement) enclosingElement;
            filterByPackage(classes, typeElement, packages);
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoEnum.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE) {
               reportError(annotatedElement, "@ProtoEnum can only be applied to enums.");
            }
            TypeElement typeElement = (TypeElement) annotatedElement;
            filterByPackage(classes, typeElement, packages);
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoMessage.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE) {
               reportError(annotatedElement, "@ProtoMessage can only be applied to classes and interfaces.");
            }
            TypeElement typeElement = (TypeElement) annotatedElement;
            filterByPackage(classes, typeElement, packages);
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoName.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE && annotatedElement.getKind() != ElementKind.ENUM) {
               reportError(annotatedElement, "@ProtoName can only be applied to classes, interfaces and enums.");
            }
            TypeElement typeElement = (TypeElement) annotatedElement.getEnclosingElement();
            filterByPackage(classes, typeElement, packages);
         }
      } else {
         if (annotation.packages().length != 0) {
            reportError(e, "@AutoProtoSchemaBuilder.packages cannot be specified if @AutoProtoSchemaBuilder.classes is not empty.");
         }

         // deduplicate the specified classes
         for (TypeMirror c : specifiedClasses) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) c).asElement();
            String fqn = typeElement.getQualifiedName().toString();
            classes.putIfAbsent(fqn, c);
         }
      }

      return classes.values();
   }

   private void filterByPackage(Map<String, TypeMirror> classes, TypeElement typeElement, Set<String> packages) {
      // skip interfaces and abstract classes when scanning packages
      if (typeElement.getKind() == ElementKind.INTERFACE || typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
         return;
      }

      String fqn = typeElement.getQualifiedName().toString();

      if (packages != null) {
         PackageElement packageOfElement = processingEnv.getElementUtils().getPackageOf(typeElement);
         String packageName = packageOfElement.getQualifiedName().toString();
         if (!packages.contains(packageName)) {  //todo [anistor] also implement prefix matching to enable recursive package scan
            return;
         }
      }

      classes.putIfAbsent(fqn, typeElement.asType());
   }

   private static String getStackTraceAsString(Throwable throwable) {
      StringWriter stringWriter = new StringWriter();
      throwable.printStackTrace(new PrintWriter(stringWriter));
      return stringWriter.toString();
   }

   private void process(TypeElement typeElement, AutoProtoSchemaBuilder annotation, Collection<? extends TypeMirror> classes) throws IOException {
      if (typeElement.getNestingKind() == NestingKind.LOCAL || typeElement.getNestingKind() == NestingKind.ANONYMOUS) {
         reportError(typeElement, "Classes annotated with @AutoProtoSchemaBuilder must not be local or anonymous.");
         return;
      }
      if (typeElement.getNestingKind() == NestingKind.MEMBER && !typeElement.getModifiers().contains(Modifier.STATIC)) {
         reportError(typeElement, "Nested classes annotated with @AutoProtoSchemaBuilder must be static.");
         return;
      }
      if (typeElement.getModifiers().contains(Modifier.FINAL)) {
         reportError(typeElement, "Classes annotated with @AutoProtoSchemaBuilder must not be final.");
         return;
      }
      if (!annotation.className().isEmpty() && !SourceVersion.isIdentifier(annotation.className())) {
         reportError(typeElement, "Invalid 'AutoProtoSchemaBuilder.className' annotation attribute. Should be a valid Java class name and must not be fully qualified.");
         return;
      }

      PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);
      String packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
      String initializerClassName = annotation.className().isEmpty() ? typeElement.getSimpleName() + "Impl" : annotation.className();
      String protobufPackageName = annotation.packageName().isEmpty() ? null : annotation.packageName();

      MirrorClassFactory typeFactory = new MirrorClassFactory(processingEnv);

      XClass annotatedType = typeFactory.fromTypeMirror(typeElement.asType());
      warnOverrideExistingMethod(annotatedType, "getProtoFileName");
      warnOverrideExistingMethod(annotatedType, "getProtoFile");
      XClass serializationContextClass = typeFactory.fromClass(SerializationContext.class);
      warnOverrideExistingMethod(annotatedType, "registerSchema", serializationContextClass);
      warnOverrideExistingMethod(annotatedType, "registerMarshallers", serializationContextClass);

      Set<XClass> xclasses = classes.stream().map(typeFactory::fromTypeMirror).collect(Collectors.toCollection(LinkedHashSet::new));

      SerializationContext serCtx = ProtobufUtil.newSerializationContext();

      Set<String> generatedClasses = new LinkedHashSet<>();

      CompileTimeProtoSchemaGenerator protoSchemaGenerator = new CompileTimeProtoSchemaGenerator(typeFactory, processingEnv, serCtx,
            annotation.fileName(), protobufPackageName, xclasses, annotation.autoImportClasses(), generatedClasses);
      String schemaSrc = protoSchemaGenerator.generateAndRegister();

      writeSerializationContextInitializerSourceFile(annotation, typeElement, classes,
            packageName, initializerClassName, annotation.fileName(), schemaSrc, generatedClasses);
   }

   private void warnOverrideExistingMethod(XClass xclass, String methodName, XClass... argTypes) {
      XMethod method = xclass.getMethod(methodName, argTypes);
      if (method != null && !java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
         reportWarning(((HasModelElement) method).getElement(), "@AutoProtoSchemaBuilder will inadvertently override your %s.%s method.",
               method.getDeclaringClass().getName(), method.getName());
      }
   }

   private void writeSerializationContextInitializerSourceFile(AutoProtoSchemaBuilder annotation, TypeElement annotatedElement,
                                                               Collection<? extends TypeMirror> classes,
                                                               String packageName, String initializerClassName,
                                                               String fileName, String schemaSrc,
                                                               Set<String> generatedClasses) throws IOException {
      Filer filer = processingEnv.getFiler();
      Types types = processingEnv.getTypeUtils();

      Element[] originatingElements = new Element[classes.size() + 1];
      int i = 0;
      for (TypeMirror tm : classes) {
         originatingElements[i++] = types.asElement(tm);
      }
      originatingElements[i] = annotatedElement;

      // write Protobuf schema as a resource file if we were asked to
      if (!annotation.filePath().isEmpty()) {
         writeSchema(annotation.filePath(), fileName, schemaSrc, originatingElements);
      }

      String initializerFqn = packageName != null ? packageName + '.' + initializerClassName : initializerClassName;

      JavaFileObject initializerFile;
      try {
         initializerFile = filer.createSourceFile(initializerFqn, originatingElements);
      } catch (FilerException fe) {
         // duplicated class name maybe?
         reportError(annotatedElement, fe.getMessage());
         return;
      }

      IndentWriter iw = new IndentWriter();
      iw.append("/*\n");
      iw.append(" Generated by ").append(getClass().getName()).append("\n");
      iw.append(" at ").append(String.valueOf(Calendar.getInstance().getTime())).append("\n");
      iw.append(" from class ").append(annotatedElement.getQualifiedName()).append("\n");
      iw.append(" annotated with ").append(String.valueOf(annotation)).append("\n");
      iw.append(" */\n\n");

      if (packageName != null) {
         iw.append("package ").append(packageName).append(";\n\n");
      }

      addGeneratedBy(iw);
      iw.append("public class ").append(initializerClassName);
      iw.append(annotatedElement.getKind() == ElementKind.INTERFACE ? " implements " : " extends ").append(annotatedElement.getQualifiedName()).append(" {\n\n");
      iw.inc();

      if (annotation.filePath().isEmpty()) {
         iw.append("private static final String PROTO_SCHEMA = ").append(makeStringLiteral(schemaSrc)).append(";\n\n");
      }

      iw.append("@Override\npublic String getProtoFileName() { return \"").append(fileName).append("\"; }\n\n");
      iw.append("@Override\npublic String getProtoFile() { return ");
      if (annotation.filePath().isEmpty()) {
         iw.append("PROTO_SCHEMA");
      } else {
         iw.append("org.infinispan.protostream.FileDescriptorSource.getResourceAsString(getClass(), getProtoFileName())");
      }
      iw.append("; }\n\n");

      iw.append("@Override\n");
      iw.append("public void registerSchema(org.infinispan.protostream.SerializationContext serCtx) throws java.io.IOException {\n");
      iw.inc();
      if (annotation.filePath().isEmpty()) {
         iw.append("serCtx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString(getProtoFileName(), PROTO_SCHEMA));\n");
      } else {
         String resourceFile = annotation.filePath().isEmpty() ? fileName : annotation.filePath().replace('.', '/') + '/' + fileName;
         iw.append("serCtx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromResources(\"").append(resourceFile).append("\"));\n");
      }
      iw.dec();
      iw.append("}\n\n");

      iw.append("@Override\n");
      iw.append("public void registerMarshallers(org.infinispan.protostream.SerializationContext serCtx) {\n");
      iw.inc();
      for (String name : generatedClasses) {
         iw.append("serCtx.registerMarshaller(new ").append(name).append("());\n");
      }
      iw.dec();
      iw.append("}\n");

      iw.dec();
      iw.append("}\n");

      try (PrintWriter out = new PrintWriter(initializerFile.openWriter())) {
         out.print(iw.toString());
      }

      if (annotation.service()) {
         // generate META-INF/services entry for this initializer implementation
         serviceLoaderFileGenerator.addProvider(initializerFqn, annotatedElement);
      }
   }

   private void writeSchema(String filePath, String fileName, String schemaSrc, Element[] originatingElements) throws IOException {
      // reinterpret the path as a package
      String pkg = filePath;
      if (pkg.startsWith("/")) {
         pkg = pkg.substring(1);
      }
      pkg = pkg.replace('/', '.');

      FileObject schemaFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, pkg, fileName, originatingElements);
      try (PrintWriter out = new PrintWriter(schemaFile.openWriter())) {
         out.print(schemaSrc);
      }
   }

   static void addGeneratedBy(IndentWriter iw) {
      String ISO8601Date = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
      iw.append("@javax.annotation.Generated(value=\"").append(AutoProtoSchemaBuilderAnnotationProcessor.class.getName())
            .append("\", date=\"").append(ISO8601Date)
            .append("\",\n      comments=\"Please do not edit this file\")\n");
   }

   private String makeStringLiteral(String s) {
      StringBuilder sb = new StringBuilder(s.length() + 2);
      sb.append('\"');
      for (int i = 0; i < s.length(); i++) {
         char ch = s.charAt(i);
         switch (ch) {
            case '\n':
               sb.append("\\n\" +\n\"");
               break;
            case '\"':
               sb.append("\\\"");
               break;
            case '\\':
               sb.append("\\\\");
               break;
            default:
               sb.append(ch);
         }
      }
      sb.append('\"');
      return sb.toString();
   }

   private String makeClassName(TypeElement e) {
      return makeNestedClassName(e, e.getSimpleName().toString());
   }

   private String makeNestedClassName(TypeElement e, String className) {
      Element enclosingElement = e.getEnclosingElement();
      if (enclosingElement instanceof PackageElement) {
         PackageElement packageElement = (PackageElement) enclosingElement;
         return packageElement.isUnnamed() ? className : packageElement.getQualifiedName() + "." + className;
      } else {
         TypeElement typeElement = (TypeElement) enclosingElement;
         return makeNestedClassName(typeElement, typeElement.getSimpleName() + "$" + className);
      }
   }
}
