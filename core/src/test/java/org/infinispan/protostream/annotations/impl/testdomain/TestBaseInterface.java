package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public interface TestBaseInterface {

   @ProtoField(number = 7, defaultValue = "33")
   int getAge();   // todo this should be required by default

   @ProtoField(number = 8)
   Integer getHeight();
}
