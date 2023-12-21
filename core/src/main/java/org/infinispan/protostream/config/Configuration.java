package org.infinispan.protostream.config;

import org.infinispan.protostream.WrappedMessageTypeIdMapper;
import org.infinispan.protostream.config.impl.ConfigurationImpl;
import org.infinispan.protostream.descriptors.AnnotationElement;

import java.util.Map;

/**
 * Configuration interface for the ProtoStream library. This object is not mutable once built. Use the {@link Builder}
 * in order to create a new instance.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface Configuration {

   int DEFAULT_MAX_NESTED_DEPTH = 100;

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

   /**
    * The max nested message depth to apply to all {@link org.infinispan.protostream.annotations.impl.GeneratedMarshallerBase}s.
    * This value is used as way to avoid recurring on circular dependencies without the need to maintain the list of already visited entities.
    * Default to {@link #DEFAULT_MAX_NESTED_DEPTH}
    */
   int maxNestedMessageDepth();

   WrappingConfig wrappingConfig();

   interface WrappingConfig {

      WrappedMessageTypeIdMapper wrappedMessageTypeIdMapper();

      boolean wrapCollectionElements();

      interface Builder {

         Builder wrappedMessageTypeIdMapper(WrappedMessageTypeIdMapper wrappedMessageTypeIdMapper);

         /**
          * Wraps all the elements in a collection or array into a wrapped message.
          * <p>
          * WARNING: enabling this option will change the binary format in an incompatible way. All readers/writers must
          * have this option enabled or disabled in order to be able to parse the messages. Use with caution.
          * <p>
          * This option is required to fix a bug (IPROTO-273) where collections (or arrays) of non-primitive classes are
          * unable to be read.
          *
          * @param wrapCollectionElements {@code true} to enable wrap the elements, {@code false} otherwise.
          * @return This instance.
          */
         Builder wrapCollectionElements(boolean wrapCollectionElements);

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

      Builder maxNestedMessageDepth(int maxNestedMessageDepth);

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
