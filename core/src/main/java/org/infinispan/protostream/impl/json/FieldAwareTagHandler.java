package org.infinispan.protostream.impl.json;

import org.infinispan.protostream.TagHandler;

interface FieldAwareTagHandler extends TagHandler {

   int field();

   boolean isDone();

   boolean acceptField(int fieldNumber);
}
