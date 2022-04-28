package test_basic_stuff;

import org.infinispan.protostream.annotations.ProtoField;

public class TestMessage {

   @ProtoField(1)
   String txt;
}
