package org.infinispan.protostream.impl;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.config.AnnotationAttributeConfig;
import org.infinispan.protostream.config.AnnotationConfig;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.impl.parser.AnnotationParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public abstract class AnnotatedDescriptorImpl implements AnnotatedDescriptor {

   protected final String name;
   protected String fullName;
   protected final String documentation;
   protected Map<String, AnnotationElement.Annotation> annotations = null;
   protected Map<String, Object> parsedAnnotations = null;

   protected AnnotatedDescriptorImpl(String name, String fullName, String documentation) {
      this.name = name;
      this.fullName = fullName;
      this.documentation = documentation;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getFullName() {
      return fullName;
   }

   @Override
   public String getDocumentation() {
      return documentation;
   }

   private void initAnnotations() throws AnnotationParserException {
      if (annotations == null) {
         if (documentation != null) {
            AnnotationParser parser = new AnnotationParser(documentation);
            Map<String, AnnotationElement.Annotation> _annotations = parser.parse();
            Map<String, Object> _parsedAnnotations = new LinkedHashMap<>();
            for (AnnotationElement.Annotation annotation : _annotations.values()) {
               AnnotationConfig annotationConfig = getAnnotationConfig(annotation.name);
               if (annotationConfig == null) {
                  throw new AnnotationParserException("Unexpected annotation '" + annotation.name + "' on " + getFullName());
               }
               validateAttributes(annotation, annotationConfig);
               if (annotationConfig.annotationMetadataCreator() != null) {
                  _parsedAnnotations.put(annotation.name, annotationConfig.annotationMetadataCreator().create(this, annotation));
               }
            }
            annotations = _annotations;
            parsedAnnotations = _parsedAnnotations;
         } else {
            annotations = Collections.emptyMap();
            parsedAnnotations = Collections.emptyMap();
         }
      }
   }

   //todo [anistor] validate multiplicty (unused for now)
   private void validateAttributes(AnnotationElement.Annotation annotation, AnnotationConfig<AnnotatedDescriptor> annotationConfig) {
      for (AnnotationElement.Attribute attribute : annotation.attributes.values()) {
         AnnotationAttributeConfig attributeConfig = annotationConfig.attributes().get(attribute.name);
         if (attributeConfig == null) {
            throw new AnnotationParserException("Unexpected annotation attribute '" + attribute.name
                                                      + "' in annotation '" + annotation.name + "' on " + getFullName());
         }
         switch (attributeConfig.type()) {
            case IDENTIFIER:
               if (!(attribute.value instanceof AnnotationElement.Identifier)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be an identifier");
               }
               if (attributeConfig.allowedValues() != null && !attributeConfig.allowedValues().contains(attribute.value.getValue())) {
                  throw new AnnotationParserException("Annotation attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " should have one of the values : "
                                                            + attributeConfig.allowedValues());
               }
               break;
            case STRING:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof String)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be a String");
               }
               if (attributeConfig.allowedValues() != null && !attributeConfig.allowedValues().contains(attribute.value.getValue())) {
                  throw new AnnotationParserException("Annotation attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " should have one of the values : "
                                                            + attributeConfig.allowedValues());
               }
               break;
            case CHARACTER:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof Character)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be a char");
               }
               break;
            case BOOLEAN:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof Boolean)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be a boolean");
               }
               break;
            case INT:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof Integer)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be an int");
               }
               break;
            case LONG:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof Long)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be a long");
               }
               break;
            case FLOAT:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof Float)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be a float");
               }
               break;
            case DOUBLE:
               if (!(attribute.value instanceof AnnotationElement.Literal) || !(attribute.value.getValue() instanceof Double)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be a double");
               }
               break;
            case ANNOTATION:
               if (!(attribute.value instanceof AnnotationElement.Annotation)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.name
                                                            + "' of annotation '" + annotation.name
                                                            + "' on " + getFullName() + " must be an annotation");
               }
         }
      }
   }

   protected abstract AnnotationConfig<? extends AnnotatedDescriptor> getAnnotationConfig(String annotationName);

   @Override
   public Map<String, AnnotationElement.Annotation> getAnnotations() throws AnnotationParserException {
      initAnnotations();
      return annotations;
   }

   @Override
   public <T> T getParsedAnnotation(String annotationName) throws AnnotationParserException {
      initAnnotations();
      return (T) parsedAnnotations.get(annotationName);
   }
}
