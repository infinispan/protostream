package org.infinispan.protostream.annotations.impl.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;

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
   private final Set<TypeMirror> includedClasses;
   private final Set<TypeMirror> excludedClasses;
   private final PackageElement packageOfInitializer;

   AnnotatedClassScanner(Messager messager, Elements elements, Element builderElement, AutoProtoSchemaBuilder builderAnnotation) {
      this.messager = messager;
      this.elements = elements;
      this.builderElement = builderElement;
      this.builderAnnotation = builderAnnotation;

      includedClasses = getIncludedClasses();
      excludedClasses = getExcludedClasses();
      basePackages = getBasePackages();
      packageOfInitializer = elements.getPackageOf(builderElement);
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
         // no explicit list of classes is specified so we gather all @ProtoXyz annotated classes from source path
         // and filter them based on the specified packages and also exclude the explicitly excluded classes

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoField.class)) {
            Element enclosingElement = annotatedElement.getEnclosingElement();
            if (enclosingElement.getKind() == ElementKind.CLASS || enclosingElement.getKind() == ElementKind.INTERFACE) {
               TypeElement typeElement = (TypeElement) enclosingElement;
               collectClasses(typeElement);
            }
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoEnumValue.class)) {
            Element enclosingElement = annotatedElement.getEnclosingElement();
            if (annotatedElement.getKind() != ElementKind.ENUM_CONSTANT || enclosingElement.getKind() != ElementKind.ENUM) {
               throw new AnnotationProcessingException(annotatedElement, "@ProtoEnumValue can only be applied to enum constants.");
            }
            TypeElement typeElement = (TypeElement) enclosingElement;
            collectClasses(typeElement);
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoEnum.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE) {
               throw new AnnotationProcessingException(annotatedElement, "@ProtoEnum can only be applied to enums.");
            }
            TypeElement typeElement = (TypeElement) annotatedElement;
            collectClasses(typeElement);
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoMessage.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE) {
               throw new AnnotationProcessingException(annotatedElement, "@ProtoMessage can only be applied to classes and interfaces.");
            }
            TypeElement typeElement = (TypeElement) annotatedElement;
            collectClasses(typeElement);
         }

         for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ProtoName.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS && annotatedElement.getKind() != ElementKind.INTERFACE && annotatedElement.getKind() != ElementKind.ENUM) {
               throw new AnnotationProcessingException(annotatedElement, "@ProtoName can only be applied to classes, interfaces and enums.");
            }
            TypeElement typeElement = (TypeElement) annotatedElement;
            collectClasses(typeElement);
         }
      } else {
         // filter the included classes by package and exclude the explicitly excluded ones
         for (TypeMirror c : includedClasses) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) c).asElement();
            collectClasses(typeElement);
         }
      }
   }

   boolean isIncluded(String classFQN) {
      TypeElement typeElement = elements.getTypeElement(classFQN);
      TypeMirror type = typeElement.asType();

      if (excludedClasses.contains(type)) {
         return false;
      }

      PackageElement packageOfElement = elements.getPackageOf(typeElement);
      if (!isPackageIncluded(packageOfElement)) {
         return false;
      }

      if (!includedClasses.isEmpty() && includedClasses.contains(type)) {
         return true;
      }

      return builderAnnotation.autoImportClasses() || includedClasses.isEmpty();
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

   private Set<TypeMirror> getIncludedClasses() {
      List<? extends TypeMirror> classes = Collections.emptyList();
      try {
         builderAnnotation.includeClasses();  // this is guaranteed to fail, see MirroredTypesException javadoc
      } catch (MirroredTypesException mte) {
         classes = mte.getTypeMirrors();
      }
      return new LinkedHashSet<>(classes);
   }

   private Set<TypeMirror> getExcludedClasses() {
      List<? extends TypeMirror> classes = Collections.emptyList();
      try {
         builderAnnotation.excludeClasses();  // this is guaranteed to fail, see MirroredTypesException javadoc
      } catch (MirroredTypesException mte) {
         classes = mte.getTypeMirrors();
      }
      return new LinkedHashSet<>(classes);
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
         if (packageOfElement.isUnnamed() && !packageOfInitializer.isUnnamed() || !packageOfElement.equals(packageOfInitializer) && !isPublicElement(typeElement)) {
            messager.printMessage(Diagnostic.Kind.WARNING, String.format("Type %s is not visible to %s so it is ignored!",
                  typeElement.getQualifiedName(), packageOfInitializer.getQualifiedName()), builderElement);
            return;
         }
      }

      classes.putIfAbsent(typeElement.getQualifiedName().toString(), type);
   }

   /**
    * Checks if the type and all its outer types up to top level have the PUBLIC modifier which ensures their visibility
    * to a different package.
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
