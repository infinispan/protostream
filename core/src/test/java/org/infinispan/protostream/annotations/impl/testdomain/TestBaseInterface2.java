package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public interface TestBaseInterface2 {

   @ProtoField(number = 77, defaultValue = "35")
   int getAge();

   @ProtoField(number = 88)
   Integer getHeight();
}
