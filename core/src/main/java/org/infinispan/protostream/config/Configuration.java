package org.infinispan.protostream.config;

import java.util.Map;

import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * Configuration interface for the ProtoStream library.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface Configuration {

   /**
    * The TypeId annotation. This optional annotation defines a unique integer type identifier for each message or enum
    * type. This can be used alternatively instead of the fully qualified type name during marshalling to save some
    * space.
    */
   String TYPE_ID_ANNOTATION = "TypeId";

   boolean logOutOfSequenceReads();

   boolean logOutOfSequenceWrites();

   AnnotationsConfig annotationsConfig();

   interface AnnotationsConfig {

      Map<String, AnnotationConfiguration> annotations();

      interface Builder {

         /**
          * Create a new annotation with the given name and return its builder to continue configure it.
          */
         AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target);

         Configuration build();
      }
   }

   interface Builder {

      Builder setLogOutOfSequenceReads(boolean logOutOfSequenceReads);

      Builder setLogOutOfSequenceWrites(boolean logOutOfSequenceWrites);

      AnnotationsConfig.Builder annotationsConfig();

      Configuration build();
   }

   static Builder builder() {
      return new ConfigurationImpl.BuilderImpl();
   }
}
