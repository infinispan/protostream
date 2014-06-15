package org.infinispan.protostream;


import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;

/**
 * An event based interface for consuming (read only) protobuf streams.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface TagHandler {

   void onStart();

   void onTag(int fieldNumber, String fieldName, Type type, JavaType javaType, Object tagValue);

   void onStartNested(int fieldNumber, String fieldName, Descriptor messageDescriptor);

   void onEndNested(int fieldNumber, String fieldName, Descriptor messageDescriptor);

   void onEnd();
}
