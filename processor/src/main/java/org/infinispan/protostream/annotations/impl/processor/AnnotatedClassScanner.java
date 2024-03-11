package org.infinispan.protostream.annotations.impl.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.annotations.impl.processor.types.MirrorTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.impl.Log;

/**
 * Discovers the classes to process based on the package and class filter specified in the {@link AutoProtoSchemaBuilder}
 * annotation.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class AnnotatedClassScanner {

   private static final Log log = Log.LogFactory.getLog(AnnotatedClassScanner.class);

   /**
    * Keep them sorted by FQN for predictable and repeatable order of processing.
    */
   private TreeMap<String, TypeMirror> classes;

   private final Messager messager;
   private final Elements elements;
   private final Types types;
   private final MirrorTypeFactory typeFactory;

   private final Element builderElement;
   private final ProtoSchemaAnnotation builderAnnotation;

   private final Set<String> basePackages;
   private final Set<TypeMirror> includedClasses;
   private final Set<TypeMirror> excludedClasses;
   private final PackageElement packageOfInitializer;

   private final String initializerClassName;
   private final String initializerFQClassName;

   AnnotatedClassScanner(Messager messager, Elements elements, Types types, MirrorTypeFactory typeFactory,
                         Element builderElement, ProtoSchemaAnnotation builderAnnotation) {
      this.messager = messager;
      this.elements = elements;
      this.types = types;
      this.typeFactory = typeFactory;
      this.builderElement = builderElement;
      this.builderAnnotation = builderAnnotation;

      includedClasses = new LinkedHashSet<>(builderAnnotation.includeClasses());
      excludedClasses = new LinkedHashSet<>(builderAnnotation.excludeClasses());
      basePackages = getBasePackages();

      if (!includedClasses.isEmpty()) {
         if (!excludedClasses.isEmpty()) {
            throw new AnnotationProcessingException(builderElement, "@AutoProtoSchemaBuilder.includeClasses and @AutoProtoSchemaBuilder.excludeClasses are mutually exclusive");
         }
         if (!basePackages.isEmpty()) {
            throw new AnnotationProcessingException(builderElement, "@AutoProtoSchemaBuilder.includeClasses and @AutoProtoSchemaBuilder.value/basePackages are mutually exclusive");
         }
      }

      for (TypeMirror c : excludedClasses) {
         TypeElement typeElement = (TypeElement) ((DeclaredType) c).asElement();
         PackageElement packageOfClass = elements.getPackageOf(typeElement);
         if (!isPackageIncluded(packageOfClass)) {
            String errMsg = String.format("@AutoProtoSchemaBuilder.excludeClasses and @AutoProtoSchemaBuilder.value/basePackages are conflicting. Class '%s' must belong to a base package.",
                  typeElement.getQualifiedName());
            throw new AnnotationProcessingException(builderElement, errMsg);
         }
      }

      packageOfInitializer = elements.getPackageOf(builderElement);

      initializerClassName = builderAnnotation.className().isEmpty() ? builderElement.getSimpleName() + "Impl" : builderAnnotation.className();
      initializerFQClassName = packageOfInitializer.isUnnamed() ? initializerClassName : packageOfInitializer.getQualifiedName().toString() + '.' + initializerClassName;
   }

   String getInitializerClassName() {
      return initializerClassName;
   }

   String getInitializerFQClassName() {
      return initializerFQClassName;
   }

   Collection<? extends TypeMirror> getClasses() {
      return classes.values();
   }

   Set<XClass> getXClasses() {
      return getClasses().stream().map(typeFactory::fromTypeMirror).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   /**
    * Gathers the message/enum classes to process and generate marshallers for.
    */
   void discoverClasses(RoundEnvironment roundEnv) throws AnnotationProcessingException {
      classes = new TreeMap<>();

      if (includedClasses.isEmpty()) {
         // No explicit list of classes is specified so we gather all relevant @ProtoXyz annotated classes from source
         // path and filter them based on the specified packages and also exclude the explicitly excluded classes.

         // Scan the elements in RoundEnv first.
         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoField.class)) {
            visitProtoField(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoFactory.class)) {
            visitProtoFactory(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoEnumValue.class)) {
            visitProtoEnumValue(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoName.class)) {
            visitProtoName(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(Proto.class)) {
            visitProto(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoAdapter.class)) {
            visitProtoAdapter(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoTypeId.class)) {
            visitProtoTypeId(e);
         }

         // Scan the elements from a previous compilation/generation (if any). This helps incremental compilation.
         for (TypeElement e : getOriginatingClasses()) {
            visitTypeElement(e);
         }
      } else {
         // We have a given list of classes, no scanning, just filter them by package and remove the excluded ones
         for (TypeMirror c : includedClasses) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) c).asElement();
            collectClasses(typeElement);
         }
      }
   }

   private void visitTypeElement(TypeElement e) {
      if (e.getAnnotation(ProtoTypeId.class) != null) {
         visitProtoTypeId(e);
      }
      if (e.getAnnotation(ProtoName.class) != null) {
         visitProtoName(e);
      }
      if (e.getAnnotation(Proto.class) != null) {
         visitProto(e);
      }

      for (Element member : e.getEnclosedElements()) {
         if (member.getAnnotation(ProtoField.class) != null) {
            visitProtoField(member);
         }
         if (member.getAnnotation(ProtoEnumValue.class) != null) {
            visitProtoEnumValue(member);
         }

         if (member.getKind() == ElementKind.CLASS || member.getKind() == ElementKind.INTERFACE) {
            visitTypeElement((TypeElement) member);
         }
      }
   }

   private void visitProtoFactory(Element e) {
      Element enclosingElement = e.getEnclosingElement();
      if (e.getKind() != ElementKind.METHOD && e.getKind() != ElementKind.CONSTRUCTOR
            || enclosingElement.getKind() != ElementKind.CLASS && enclosingElement.getKind() != ElementKind.INTERFACE) {
         throw new AnnotationProcessingException(e, "@ProtoFactory can only be applied to constructors and methods.");
      }
      collectClasses((TypeElement) enclosingElement);
   }

   private void visitProtoField(Element e) {
      Element enclosingElement = e.getEnclosingElement();
      if (e.getKind() != ElementKind.METHOD && e.getKind() != ElementKind.FIELD
            || enclosingElement.getKind() != ElementKind.CLASS && enclosingElement.getKind() != ElementKind.INTERFACE) {
         throw new AnnotationProcessingException(e, "@ProtoField can only be applied to fields and methods.");
      }
      collectClasses((TypeElement) enclosingElement);
   }

   private void visitProtoEnumValue(Element e) {
      Element enclosingElement = e.getEnclosingElement();
      if (e.getKind() != ElementKind.ENUM_CONSTANT || enclosingElement.getKind() != ElementKind.ENUM) {
         throw new AnnotationProcessingException(e, "@ProtoEnumValue can only be applied to enum constants.");
      }
      collectClasses((TypeElement) enclosingElement);
   }

   private void visitProtoAdapter(Element e) {
      if (e.getKind() != ElementKind.CLASS) {
         throw new AnnotationProcessingException(e, "@ProtoAdapter can only be applied to classes.");
      }
      collectClasses((TypeElement) e);
   }

   private void visitProtoTypeId(Element e) {
      switch (e.getKind()) {
         case CLASS, INTERFACE, RECORD, ENUM -> collectClasses((TypeElement) e);
         default ->
               throw new AnnotationProcessingException(e, "@ProtoTypeId can only be applied to classes, records, interfaces and enums.");
      }
   }

   private void visitProtoName(Element e) {
      switch (e.getKind()) {
         case CLASS, INTERFACE, RECORD, ENUM -> collectClasses((TypeElement) e);
         default ->
               throw new AnnotationProcessingException(e, "@ProtoName can only be applied to classes, interfaces, records and enums.");
      }
      collectClasses((TypeElement) e);
   }

   private void visitProto(Element e) {
      switch (e.getKind()) {
         case CLASS, INTERFACE, RECORD, ENUM -> collectClasses((TypeElement) e);
         default ->
               throw new AnnotationProcessingException(e, "@Proto can only be applied to classes, interfaces, records and enums.");
      }
      collectClasses((TypeElement) e);
   }

   /**
    * Extracts the old root elements from the previously generated initializer source code.
    */
   private Set<TypeElement> getOriginatingClasses() {
      Set<TypeElement> typeElements = Collections.emptySet();

      TypeElement initializer = elements.getTypeElement(initializerFQClassName);
      if (initializer != null) {
         TypeMirror annotationType = elements.getTypeElement(OriginatingClasses.class.getName()).asType();
         AnnotationMirror originatingClasses = initializer.getAnnotationMirrors().stream()
               .filter(annotation -> types.isSameType(annotation.getAnnotationType(), annotationType))
               .findAny().orElse(null);

         if (originatingClasses != null) {
            List<String> classes = ((List<AnnotationValue>) getAnnotationValue(originatingClasses, "value").getValue()).stream()
                  .map(av -> ((TypeElement) ((DeclaredType) av.getValue()).asElement()).getQualifiedName().toString())
                  .collect(Collectors.toList());

            typeElements = new HashSet<>(classes.size());
            for (String name : classes) {
               TypeElement typeElement = elements.getTypeElement(name);
               if (typeElement != null) {
                  typeElements.add(typeElement);
               }
            }
         }
      }

      log.debugf("Originating classes %s", typeElements);
      return typeElements;
   }

   /**
    * Gets the value of the {@code name} element of the annotation.
    *
    * @throws NoSuchElementException if the annotation has no element named {@code name}
    */
   private static AnnotationValue getAnnotationValue(AnnotationMirror annotation, String name) {
      ExecutableElement valueMethod = null;
      for (ExecutableElement method : ElementFilter.methodsIn(annotation.getAnnotationType().asElement().getEnclosedElements())) {
         if (method.getSimpleName().toString().equals(name)) {
            valueMethod = method;
            break;
         }
      }
      if (valueMethod == null) {
         return null;
      }
      AnnotationValue value = annotation.getElementValues().get(valueMethod);
      return value == null ? valueMethod.getDefaultValue() : value;
   }

   /**
    * Tests if a given class (which may have not been specifically included) is acceptable for inclusion.
    */
   boolean isClassAcceptable(XClass c) {
      String canonicalName = c.getCanonicalName();

      if (classes.containsKey(canonicalName)) {
         return true;
      }

      TypeElement typeElement = elements.getTypeElement(canonicalName);
      TypeMirror type = typeElement.asType();

      if (excludedClasses.contains(type)) {
         return false;
      }

      if (includedClasses.contains(type)) {
         return true;
      }

      PackageElement packageOfElement = elements.getPackageOf(typeElement);
      if (!isPackageIncluded(packageOfElement)) {
         return false;
      }

      // we're including based on packages only, or we have autoImportClasses enabled
      return includedClasses.isEmpty() || builderAnnotation.autoImportClasses();
   }

   private Set<String> getBasePackages() {
      if (builderAnnotation.value().length == 0 && builderAnnotation.basePackages().length == 0) {
         return Collections.emptySet();
      }
      if (builderAnnotation.value().length != 0 && builderAnnotation.basePackages().length != 0) {
         throw new AnnotationProcessingException(builderElement, "@AutoProtoSchemaBuilder.value and @AutoProtoSchemaBuilder.basePackages are mutually exclusive");
      }
      String[] basePackages = builderAnnotation.value();
      String annotationMember = "value";
      if (basePackages.length == 0) {
         annotationMember = "basePackages";
         basePackages = builderAnnotation.basePackages();
      }
      Set<String> packages = new HashSet<>(basePackages.length);
      for (String p : basePackages) {
         if (!SourceVersion.isName(p)) {
            throw new AnnotationProcessingException(builderElement, "@AutoProtoSchemaBuilder.%s contains an invalid package name : \"%s\"", annotationMember, p);
         }
         packages.add(p);
      }
      return packages;
   }

   /**
    * Checks if the type is included by given set of packages and is visible to the initializer and adds it to collected
    * classes Map if it satisfies all conditions.
    */
   private void collectClasses(TypeElement typeElement) {
      TypeMirror type = typeElement.asType();
      if (excludedClasses.contains(type)) {
         return;
      }

      PackageElement packageOfElement = elements.getPackageOf(typeElement);
      if (!isPackageIncluded(packageOfElement)) {
         return;
      }

      // when no explicit list of classes is given we will scan packages, and when scanning we have some additional rules
      if (includedClasses.isEmpty()) {
         // Skip interfaces and abstract classes when scanning packages. We only generate for instantiable classes.
         if (typeElement.getKind() == ElementKind.INTERFACE || typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            return;
         }

         // classes from default/unnamed package cannot be imported so are ignored unless the initializer is itself located in the default package
         // classes with non-public visibility are ignored unless the initializer is in the same package
         if (packageOfElement.isUnnamed() && !packageOfInitializer.isUnnamed()
               || !packageOfElement.equals(packageOfInitializer) && !isPublicElement(typeElement)) {
            messager.printMessage(Diagnostic.Kind.WARNING, String.format("Type %s is not visible to %s so it is ignored!",
                  typeElement.getQualifiedName(), ((QualifiedNameable) builderElement).getQualifiedName()), builderElement);
            return;
         }
      }

      classes.putIfAbsent(typeElement.getQualifiedName().toString(), type);
   }

   /**
    * Checks if the given type and all its outer types up to top level type have the PUBLIC modifier which ensures their
    * visibility to a different package.
    */
   private boolean isPublicElement(TypeElement typeElement) {
      Element e = typeElement;
      while (true) {
         if (!e.getModifiers().contains(Modifier.PUBLIC)) {
            return false;
         }
         e = e.getEnclosingElement();
         if (e == null || e.getKind() == ElementKind.PACKAGE) {
            break;
         }
         if (e.getKind() != ElementKind.CLASS && e.getKind() != ElementKind.INTERFACE && e.getKind() != ElementKind.ENUM) {
            return false;
         }
      }
      return true;
   }

   /**
    * Checks if a package is included in the given set of packages, considering also their subpackages recursively.
    */
   private boolean isPackageIncluded(PackageElement packageElement) {
      if (basePackages.isEmpty()) {
         return true;
      }

      String p = packageElement.getQualifiedName().toString();

      while (true) {
         if (basePackages.contains(p)) {
            return true;
         }

         int pos = p.lastIndexOf('.');
         if (pos == -1) {
            break;
         }
         p = p.substring(0, pos);
      }

      return false;
   }
}
