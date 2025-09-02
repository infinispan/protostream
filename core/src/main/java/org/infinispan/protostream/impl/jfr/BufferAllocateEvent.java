package org.infinispan.protostream.impl.jfr;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name(BufferAllocateEvent.NAME)
@Label("Buffer Allocation")
@Description("Triggered when a new buffer is allocated")
final class BufferAllocateEvent extends AbstractAllocatorEvent {
    static final String NAME = "org.infinispan.protostream.AllocateEvent";

    private static final BufferAllocateEvent INSTANCE = new BufferAllocateEvent(0);

    BufferAllocateEvent(int size) {
        super(size);
    }

    public static boolean isEventEnabled() {
        return INSTANCE.isEnabled();
    }
}
