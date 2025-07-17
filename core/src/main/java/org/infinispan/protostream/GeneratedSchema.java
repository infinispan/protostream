package org.infinispan.protostream;

import org.infinispan.protostream.schema.Schema;

import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;

/**
 * An annotation-based generated proto schema file. This is just a more specific flavour of
 * {@link SerializationContextInitializer} that also exposes the generated Protobuf schema, which consists of the file
 * name and the file contents. Users will never implement this interface directly. Implementations are always generated
 * by the annotation processor based on the {@link org.infinispan.protostream.annotations.ProtoSchema}
 * annotation, identically as for {@link SerializationContextInitializer}.
 *
 * @author anistor@redhat.com
 * @since 4.3.4
 */
public interface GeneratedSchema extends SerializationContextInitializer, Schema {

   @Override
   default String getName() {
      return getProtoFileName();
   }

   @Override
   default String getContent() {
      return getProtoFile();
   }

   /**
    * Returns the name of the proto file. The name is allowed to contain slashes so it can look like an absolute
    * or relative path. The returned value must be the same (equals) on each invocation.
    */
   String getProtoFileName();

   /**
    * Returns the contents of the proto file as a {@link String}. The returned value must be guaranteed to be the same
    * (equals) on each invocation. Implementations can return a constant or a value stored in memory but they are
    * generally free to also retrieve it from somewhere else, including the classpath, the disk, or even a mechanism
    * that can potentially fail with an {@link UncheckedIOException}.
    *
    * @throws UncheckedIOException if the file contents cannot be retrieved
    */
   String getProtoFile() throws UncheckedIOException;

   /**
    * Convenience method to obtain a {@link Reader} of the schema file contents. The caller is responsible for closing
    * the {@link Reader} once done with it.
    *
    * @throws UncheckedIOException if the file contents cannot be retrieved
    */
   default Reader getProtoFileReader() throws UncheckedIOException {
      return new StringReader(getProtoFile());
   }
}
