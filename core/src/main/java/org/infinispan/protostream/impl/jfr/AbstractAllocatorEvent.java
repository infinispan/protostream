package org.infinispan.protostream.impl.jfr;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Event;

@Enabled(value = false)
@Category("ProtoStream")
class AbstractAllocatorEvent extends Event {

    @DataAmount
    @Description("Allocation size")
    int size;

    AbstractAllocatorEvent(int size) {
        this.size = size;
    }
}
