package org.infinispan.protostream.impl;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.DescriptorParserException;
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
      if (name.indexOf('.') != -1) {
         throw new DescriptorParserException("Definition names should not be qualified : " + name);
      }
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
            Map<String, Object> _parsedAnnotations = new LinkedHashMap<String, Object>();
            for (AnnotationElement.Annotation annotation : _annotations.values()) {
               AnnotationConfig annotationConfig = getAnnotationConfig(annotation.getName());
               // unknown annotations are ignored
               if (annotationConfig != null) {
                  //todo [anistor] all annotation fields should be required unless they have defaults
                  validateAttributes(annotation, annotationConfig);

                  // convert single values to arrays if needed and set the default values for missing attributes
                  normalizeValues(annotation, annotationConfig);

                  if (annotationConfig.annotationMetadataCreator() != null) {
                     _parsedAnnotations.put(annotation.getName(), annotationConfig.annotationMetadataCreator().create(this, annotation));
                  }
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

   private void validateAttributes(AnnotationElement.Annotation annotation, AnnotationConfig<?> annotationConfig) {
      for (Map.Entry<String, AnnotationElement.Attribute> entry : annotation.getAttributes().entrySet()) {
         AnnotationElement.Attribute attribute = entry.getValue();

         final AnnotationAttributeConfig attributeConfig = annotationConfig.attributes().get(attribute.getName());
         if (attributeConfig == null) {
            throw new AnnotationParserException("Unexpected attribute '" + attribute.getName()
                                                      + "' in annotation '" + annotation.getName() + "' on " + getFullName());
         }

         AnnotationElement.Value value = attribute.getValue();
         if (!attributeConfig.multiple() && value instanceof AnnotationElement.Array) {
            throw new AnnotationParserException("Annotation attribute '" + attribute.getName()
                                                      + "' in annotation '" + annotation.getName() + "' on " + getFullName()
                                                      + " does not accept array values");
         }

         if (value instanceof AnnotationElement.Array) {
            for (AnnotationElement.Value v : ((AnnotationElement.Array) value).getValues()) {
               validateAttribute(annotation, attribute, attributeConfig, v);
            }
         } else {
            validateAttribute(annotation, attribute, attributeConfig, value);
         }
      }
   }

   private void validateAttribute(AnnotationElement.Annotation annotation, AnnotationElement.Attribute attribute, AnnotationAttributeConfig attributeConfig, AnnotationElement.Value value) {
      // validate declared type vs parsed type
      switch (attributeConfig.type()) {
         case IDENTIFIER:
            if (!(value instanceof AnnotationElement.Identifier)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be an identifier");
            }
            if (attributeConfig.allowedValues() != null && !attributeConfig.allowedValues().contains(value.getValue())) {
               throw new AnnotationParserException("Annotation attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " should have one of the values : "
                                                         + attributeConfig.allowedValues());
            }
            break;
         case STRING:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof String)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be a String");
            }
            if (attributeConfig.allowedValues() != null && !attributeConfig.allowedValues().contains(value.getValue())) {
               throw new AnnotationParserException("Annotation attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " should have one of the values : "
                                                         + attributeConfig.allowedValues());
            }
            break;
         case CHARACTER:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof Character)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be a char");
            }
            break;
         case BOOLEAN:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof Boolean)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be a boolean");
            }
            break;
         case INT:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof Integer)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be an int");
            }
            break;
         case LONG:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof Long)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be a long");
            }
            break;
         case FLOAT:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof Float)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be a float");
            }
            break;
         case DOUBLE:
            if (!(value instanceof AnnotationElement.Literal) || !(value.getValue() instanceof Double)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be a double");
            }
            break;
         case ANNOTATION:
            if (!(value instanceof AnnotationElement.Annotation)) {
               throw new AnnotationParserException("Value of attribute '" + attribute.getName()
                                                         + "' of annotation '" + annotation.getName()
                                                         + "' on " + getFullName() + " must be an annotation");
            }
      }
   }

   private void normalizeValues(AnnotationElement.Annotation annotation, AnnotationConfig<?> annotationConfig) {
      for (AnnotationAttributeConfig attributeConfig : annotationConfig.attributes().values()) {
         AnnotationElement.Attribute attribute = annotation.getAttributes().get(attributeConfig.name());
         if (attribute != null) {
            AnnotationElement.Value value = attribute.getValue();
            if (attributeConfig.multiple() && !(value instanceof AnnotationElement.Array)) {
               // a single value will be automatically wrapped in an array node if the attribute was defined as 'multiple'
               value = new AnnotationElement.Array(value.position, Collections.singletonList(value));
               attribute = new AnnotationElement.Attribute(attribute.position, attributeConfig.name(), value);
               annotation.getAttributes().put(attributeConfig.name(), attribute);
            }
         } else if (attributeConfig.defaultValue() != null) {
            AnnotationElement.Value value = attributeConfig.type() == AnnotationAttributeConfig.AttributeType.IDENTIFIER ?
                  new AnnotationElement.Identifier(AnnotationElement.UNKNOWN_POSITION, (String) attributeConfig.defaultValue()) :
                  new AnnotationElement.Literal(AnnotationElement.UNKNOWN_POSITION, attributeConfig.defaultValue());
            if (attributeConfig.multiple()) {
               value = new AnnotationElement.Array(value.position, Collections.singletonList(value));
            }
            attribute = new AnnotationElement.Attribute(AnnotationElement.UNKNOWN_POSITION, attributeConfig.name(), value);
            annotation.getAttributes().put(attributeConfig.name(), attribute);
         } else {
            throw new AnnotationParserException("Attribute '" + attributeConfig.name()
                                                      + "' of annotation '" + annotation.getName()
                                                      + "' on " + getFullName() + " is required");
         }
      }
   }

   /**
    * Subclasses are responsible for fetching the AnnotationConfig from the appropriate place.
    */
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
