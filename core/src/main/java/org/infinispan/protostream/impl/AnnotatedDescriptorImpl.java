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

   private void processAnnotations() throws AnnotationParserException {
      if (annotations == null) {
         if (documentation != null) {
            AnnotationParser parser = new AnnotationParser(documentation);
            Map<String, AnnotationElement.Annotation> _annotations = parser.parse();
            Map<String, Object> _parsedAnnotations = new LinkedHashMap<>();
            for (AnnotationElement.Annotation annotation : _annotations.values()) {
               AnnotationConfig annotationConfig = getAnnotationConfig(annotation.getName());
               if (annotationConfig == null) {
                  throw new AnnotationParserException("Unexpected annotation '" + annotation.getName() + "' on " + getFullName());
               }
               validateAttributes(annotation, annotationConfig);
               if (annotationConfig.annotationMetadataCreator() != null) {
                  _parsedAnnotations.put(annotation.getName(), annotationConfig.annotationMetadataCreator().create(this, annotation));
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
      for (AnnotationElement.Attribute attribute : annotation.getAttributes().values()) {
         AnnotationAttributeConfig attributeConfig = annotationConfig.attributes().get(attribute.getName());
         if (attributeConfig == null) {
            throw new AnnotationParserException("Unexpected annotation attribute '" + attribute.getName()
                                                      + "' in annotation '" + annotation.getName() + "' on " + getFullName());
         }
         switch (attributeConfig.type()) {
            case IDENTIFIER:
               if (!(attribute.getValue() instanceof AnnotationElement.Identifier)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be an identifier");
               }
               if (attributeConfig.allowedValues() != null && !attributeConfig.allowedValues().contains(attribute.getValue().getValue())) {
                  throw new AnnotationParserException("Annotation attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " should have one of the values : "
                                                            + attributeConfig.allowedValues());
               }
               break;
            case STRING:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof String)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be a String");
               }
               if (attributeConfig.allowedValues() != null && !attributeConfig.allowedValues().contains(attribute.getValue().getValue())) {
                  throw new AnnotationParserException("Annotation attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " should have one of the values : "
                                                            + attributeConfig.allowedValues());
               }
               break;
            case CHARACTER:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof Character)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be a char");
               }
               break;
            case BOOLEAN:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof Boolean)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be a boolean");
               }
               break;
            case INT:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof Integer)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be an int");
               }
               break;
            case LONG:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof Long)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be a long");
               }
               break;
            case FLOAT:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof Float)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be a float");
               }
               break;
            case DOUBLE:
               if (!(attribute.getValue() instanceof AnnotationElement.Literal) || !(attribute.getValue().getValue() instanceof Double)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be a double");
               }
               break;
            case ANNOTATION:
               if (!(attribute.getValue() instanceof AnnotationElement.Annotation)) {
                  throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                            + "' of annotation '" + annotation.getName()
                                                            + "' on " + getFullName() + " must be an annotation");
               }
         }
      }

      // set the default values for missing attributes
      for (AnnotationAttributeConfig attributeConfig : annotationConfig.attributes().values()) {
         if (!annotation.getAttributes().containsKey(attributeConfig.name()) && attributeConfig.defaultValue() != null) {
            AnnotationElement.Value value = attributeConfig.type() == AnnotationAttributeConfig.AttributeType.IDENTIFIER ?
                  new AnnotationElement.Identifier(AnnotationElement.UNKNOWN_POSITION, (String) attributeConfig.defaultValue()) :
                  new AnnotationElement.Literal(AnnotationElement.UNKNOWN_POSITION, attributeConfig.defaultValue());
            annotation.getAttributes().put(attributeConfig.name(), new AnnotationElement.Attribute(AnnotationElement.UNKNOWN_POSITION, attributeConfig.name(), value));
         }
      }
   }

   protected abstract AnnotationConfig<? extends AnnotatedDescriptor> getAnnotationConfig(String annotationName);

   @Override
   public Map<String, AnnotationElement.Annotation> getAnnotations() throws AnnotationParserException {
      processAnnotations();
      return annotations;
   }

   @Override
   public <T> T getProcessedAnnotation(String annotationName) throws AnnotationParserException {
      processAnnotations();
      return (T) parsedAnnotations.get(annotationName);
   }
}
