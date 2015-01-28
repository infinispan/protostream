package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("@TypeId(10)")
public class Simple {

   @ProtoField(number = 1111)
   public Simple simple;

   @ProtoField(number = 1, required = true) //todo [anistor] should be possible not to require required in this case?
   public float afloat;

   @ProtoField(number = 2)
   public Integer anInteger;

   @ProtoField(number = 314, name = "my_enum_field", defaultValue = "AX")
   public TestEnum myEnumField;

   @ProtoUnknownFieldSet
   public UnknownFieldSet unknownFieldSet;
}
