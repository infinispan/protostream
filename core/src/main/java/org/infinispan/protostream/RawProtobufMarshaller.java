package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 */
public interface RawProtobufMarshaller<T> extends BaseMarshaller<T> {

   T readFrom(SerializationContext ctx, CodedInputStream in) throws IOException;

   void writeTo(SerializationContext ctx, CodedOutputStream out, T t) throws IOException;
}
