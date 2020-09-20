package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public interface TestBaseInterface {

   @ProtoField(number = 7, defaultValue = "33")
   int getAge();   // TODO when the type is a non-nullable primitive, maybe this should be considered required=true by default if no defaultValue is set?

   @ProtoField(8)
   Integer getHeight();
}
