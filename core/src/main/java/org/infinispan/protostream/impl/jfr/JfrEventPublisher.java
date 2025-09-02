package org.infinispan.protostream.impl.jfr;

public final class JfrEventPublisher {

    private static void allocateEvent(int size, int scale) {
        if (BufferAllocateEvent.isEventEnabled()) {
            BufferAllocateEvent ev = new BufferAllocateEvent(size * scale);
            ev.commit();
        }
    }

    public static void bufferAllocateEvent(int size) {
        if (BufferAllocateEvent.isEventEnabled()) {
            BufferAllocateEvent ev = new BufferAllocateEvent(size);
            ev.commit();
        }
    }

    public static void intBufferAllocateEvent(int size) {
        allocateEvent(size, Integer.BYTES);
    }

    public static void longBufferAllocateEvent(int size) {
        allocateEvent(size, Long.BYTES);
    }

    public static void doubleBufferAllocateEvent(int size) {
        allocateEvent(size, Double.BYTES);
    }

    public static void floatBufferAllocateEvent(int size) {
        allocateEvent(size, Float.BYTES);
    }

    public static void bufferResizeEvent(int before, int after) {
        if (BufferResizeEvent.isEventEnabled()) {
            BufferResizeEvent ev = new BufferResizeEvent(before, after, after - before);
            ev.commit();
        }
    }

    public static void intBufferResizeEvent(int before, int after) {
        if (BufferResizeEvent.isEventEnabled()) {
            int actualBefore = before * Integer.BYTES;
            int actualAfter = after * Integer.BYTES;
            BufferResizeEvent ev = new BufferResizeEvent(actualBefore, actualAfter, actualAfter - actualBefore);
            ev.commit();
        }
    }
}
