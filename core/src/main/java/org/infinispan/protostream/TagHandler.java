package org.infinispan.protostream;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * An event based interface for consuming (read only) protobuf streams.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface TagHandler {

   void onStart();

   void onTag(int fieldNumber, String fieldName, FieldDescriptor.Type type, FieldDescriptor.JavaType javaType, Object tagValue);

   void onStartNested(int fieldNumber, String fieldName, Descriptor messageDescriptor);

   void onEndNested(int fieldNumber, String fieldName, Descriptor messageDescriptor);

   void onEnd();
}
