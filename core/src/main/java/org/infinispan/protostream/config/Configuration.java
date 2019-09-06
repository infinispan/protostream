package org.infinispan.protostream.config;

import java.util.Map;

import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * Configuration interface for the ProtoStream library. This object is not mutable. Use the {@link Builder} in order to
 * create and mutate a new instance.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface Configuration {

   /**
    * The TypeId annotation name. This optional annotation defines a unique integer type identifier for each message or
    * enum type. This can be used alternatively instead of the fully qualified type name during marshalling to save some
    * bandwidth. Values starting at 1000000 are reserved for internal use by Protostream and other projects from the
    * Infinispan organisation.
    * <p>
    * This annotation is pre-defined in all configurations.
    */
   String TYPE_ID_ANNOTATION = "TypeId";

   /**
    * Flag that indicates in out of sequence reads should be logged as warnings. This is {@code true} by default.
    */
   boolean logOutOfSequenceReads();

   /**
    * Flag that indicates in out of sequence writes should be logged as warnings. This is {@code true} by default.
    */
   boolean logOutOfSequenceWrites();

   AnnotationsConfig annotationsConfig();

   interface AnnotationsConfig {

      /**
       * Should we log a warning every time we encounter an undefined documentation annotation? This is {@code true} by
       * default.
       */
      boolean logUndefinedAnnotations();

      Map<String, AnnotationConfiguration> annotations();

      interface Builder {

         /**
          * Create a new annotation with the given name and return its builder to continue define it.
          */
         AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target);

         Configuration build();
      }
   }

   interface Builder {

      Builder setLogOutOfSequenceReads(boolean logOutOfSequenceReads);

      Builder setLogOutOfSequenceWrites(boolean logOutOfSequenceWrites);

      /**
       * Should we log a warning every time we encounter an undefined documentation annotation? This is {@code true} by
       * default.
       */
      Builder setLogUndefinedAnnotations(boolean logUndefinedAnnotations);

      AnnotationsConfig.Builder annotationsConfig();

      Configuration build();
   }

   static Builder builder() {
      return new ConfigurationImpl.BuilderImpl();
   }
}
