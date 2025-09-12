package org.infinispan.protostream.impl.jfr;

import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name(BufferResizeEvent.NAME)
@Label("Buffer Resize")
@Description("Triggered when a buffer is resized")
final class BufferResizeEvent extends AbstractAllocatorEvent {
    static final String NAME = "org.infinispan.protostream.ResizeEvent";
    private static final BufferResizeEvent INSTANCE = new BufferResizeEvent(0, 0, 0);

    BufferResizeEvent(int before, int after, int size) {
        super(size);
        this.before = before;
        this.after = after;
    }

    public static boolean isEventEnabled() {
        return INSTANCE.isEnabled();
    }

    @DataAmount
    @Description("Buffer size before resizing")
    int before;

    @DataAmount
    @Description("Buffer size after resizing")
    int after;
}
