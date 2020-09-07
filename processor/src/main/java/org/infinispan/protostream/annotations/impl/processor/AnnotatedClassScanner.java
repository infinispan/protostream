package org.infinispan.protostream.annotations.impl.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.annotations.impl.OriginatingClasses;
import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * Discovers the classes to process based on the package and class filter specified in the AutoProtoSchemaBuilder
 * annotation.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class AnnotatedClassScanner {

   /**
    * Keep them sorted by FQN for predictable and repeatable order of processing.
    */
   private TreeMap<String, TypeMirror> classes;

   private final Messager messager;
   private final Elements elements;

   private final Element builderElement;
   private final AutoProtoSchemaBuilder builderAnnotation;

   private final Set<String> basePackages;
   private final Set<TypeMirror> includedClasses = new LinkedHashSet<>();
   private final Set<TypeMirror> excludedClasses = new LinkedHashSet<>();
   private final PackageElement packageOfInitializer;

   private final String initializerClassName;
   private final String initializerFQClassName;

   AnnotatedClassScanner(Messager messager, Elements elements, Element builderElement, AutoProtoSchemaBuilder builderAnnotation) {
      this.messager = messager;
      this.elements = elements;
      this.builderElement = builderElement;
      this.builderAnnotation = builderAnnotation;

      try {
         builderAnnotation.includeClasses();
      } catch (MirroredTypesException mte) {
         includedClasses.addAll(mte.getTypeMirrors());
      }
      try {
         builderAnnotation.excludeClasses();
      } catch (MirroredTypesException mte) {
         excludedClasses.addAll(mte.getTypeMirrors());
      }
      Set<TypeMirror> overlap = new HashSet<>(includedClasses);
      overlap.retainAll(excludedClasses);
      if (!overlap.isEmpty()) {
         throw new AnnotationProcessingException(builderElement, "@AutoProtoSchemaBuilder.includedClasses/excludedClasses are conflicting: " + overlap);
      }

      basePackages = getBasePackages();
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

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoEnum.class)) {
            visitProtoEnum(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoMessage.class)) {
            visitProtoMessage(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoName.class)) {
            visitProtoName(e);
         }

         for (Element e : roundEnv.getElementsAnnotatedWith(ProtoTypeId.class)) {
            visitProtoTypeId(e);
         }

         // Scan the elements from a previous compilation/generation (if any). This helps incremental compilation.
         for (TypeElement e : getPreviouslyProcessedElements()) {
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
      if (e.getAnnotation(ProtoMessage.class) != null) {
         visitProtoMessage(e);
      }
      if (e.getAnnotation(ProtoEnum.class) != null) {
         visitProtoEnum(e);
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

   private void visitProtoTypeId(Element e) {
      if (e.getKind() != ElementKind.CLASS && e.getKind() != ElementKind.INTERFACE && e.getKind() != ElementKind.ENUM) {
         throw new AnnotationProcessingException(e, "@ProtoTypeId can only be applied to classes, interfaces and enums.");
      }
      collectClasses((TypeElement) e);
   }

   private void visitProtoName(Element e) {
      if (e.getKind() != ElementKind.CLASS && e.getKind() != ElementKind.INTERFACE && e.getKind() != ElementKind.ENUM) {
         throw new AnnotationProcessingException(e, "@ProtoName can only be applied to classes, interfaces and enums.");
      }
      collectClasses((TypeElement) e);
   }

   private void visitProtoMessage(Element e) {
      if (e.getKind() != ElementKind.CLASS && e.getKind() != ElementKind.INTERFACE) {
         throw new AnnotationProcessingException(e, "@ProtoMessage can only be applied to classes and interfaces.");
      }
      collectClasses((TypeElement) e);
   }

   private void visitProtoEnum(Element e) {
      if (e.getKind() != ElementKind.CLASS && e.getKind() != ElementKind.INTERFACE) {
         throw new AnnotationProcessingException(e, "@ProtoEnum can only be applied to enums.");
      }
      collectClasses((TypeElement) e);
   }

   /**
    * Extracts the old root elements from the previously generated initializer code.
    */
   private Set<TypeElement> getPreviouslyProcessedElements() {
      Set<TypeElement> typeElements = Collections.emptySet();

      TypeElement initializer = elements.getTypeElement(initializerFQClassName);
      if (initializer != null) {
         OriginatingClasses originatingClasses = initializer.getAnnotation(OriginatingClasses.class);
         if (originatingClasses != null) {
            typeElements = new HashSet<>(originatingClasses.value().length);
            for (String name : originatingClasses.value()) {
               TypeElement typeElement = elements.getTypeElement(name);
               if (typeElement != null) {
                  typeElements.add(typeElement);
               }
            }
         }
      }

      return typeElements;
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
      boolean usingAlias = true;
      String[] basePackages = builderAnnotation.value();
      if (basePackages.length == 0) {
         usingAlias = false;
         basePackages = builderAnnotation.basePackages();
      }
      Set<String> packages = new HashSet<>(basePackages.length);
      for (String p : basePackages) {
         if (!SourceVersion.isName(p)) {
            throw new AnnotationProcessingException(builderElement, "@AutoProtoSchemaBuilder.%s contains an invalid package name : \"%s\"", usingAlias ? "value" : "basePackages", p);
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
