package org.infinispan.protostream.config;

import java.util.Map;

import org.infinispan.protostream.WrappedMessageTypeIdMapper;
import org.infinispan.protostream.config.impl.ConfigurationImpl;
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
    * The name of the TypeId annotation. This optional annotation defines a unique positive integer type identifier for
    * each message or enum type. This can be used alternatively instead of the fully qualified type name during
    * marshalling of a WrappedMessage to save bandwidth. Values in range [0..65535] are reserved for internal use by
    * Protostream and related projects from the Infinispan organisation.
    * <p>
    * This annotation is pre-defined in all configurations. You do not have to define it manually.
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

   WrappingConfig wrappingConfig();

   interface WrappingConfig {

      WrappedMessageTypeIdMapper wrappedMessageTypeIdMapper();

      interface Builder {

         Builder wrappedMessageTypeIdMapper(WrappedMessageTypeIdMapper wrappedMessageTypeIdMapper);

         Configuration build();
      }
   }

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
          * Should we log a warning every time we encounter an undefined documentation annotation? This is {@code true}
          * by default.
          */
         AnnotationsConfig.Builder setLogUndefinedAnnotations(boolean logUndefinedAnnotations);

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
       *
       * @deprecated use {@link AnnotationsConfig.Builder#setLogUndefinedAnnotations}
       */
      @Deprecated
      default Builder setLogUndefinedAnnotations(boolean logUndefinedAnnotations) {
         annotationsConfig().setLogUndefinedAnnotations(logUndefinedAnnotations);
         return this;
      }

      WrappingConfig.Builder wrappingConfig();

      AnnotationsConfig.Builder annotationsConfig();

      Configuration build();
   }

   static Builder builder() {
      return new ConfigurationImpl.BuilderImpl();
   }
}
