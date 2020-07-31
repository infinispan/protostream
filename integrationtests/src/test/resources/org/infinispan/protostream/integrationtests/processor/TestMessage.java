package test_basic_stuff;

import org.infinispan.protostream.annotations.*;

public class TestMessage {

   @ProtoField(number = 1)
   String txt;
}
