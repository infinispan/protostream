package org.infinispan.protostream.impl.jfr;

public final class JfrEventPublisher {

    public static void bufferAllocateEvent(int size) {
        if (BufferAllocateEvent.isEventEnabled()) {
            BufferAllocateEvent ev = new BufferAllocateEvent(size);
            ev.commit();
        }
    }

    public static void bufferResizeEvent(int before, int after, int size) {
        if (BufferResizeEvent.isEventEnabled()) {
            BufferResizeEvent ev = new BufferResizeEvent(before, after, size);
            ev.commit();
        }
    }
}
