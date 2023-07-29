package com.minihttp.BufferPool;

import com.minihttp.LogWrapper.LogWrapper;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BufferPool {
    private final ConcurrentLinkedDeque<ByteBuffer> buffer;
    private final int cap = 1024;

    public BufferPool() {
        buffer = new ConcurrentLinkedDeque<>();
        try {
            for (int i = 0; i < cap; ++i)
                buffer.push(ByteBuffer.allocateDirect(1024));
        } catch (Exception e) {
            LogWrapper.log(e.getMessage());
        }
    }

    public ByteBuffer acquireBuffer() {
        ByteBuffer b = buffer.poll();
        if (b == null) {
            LogWrapper.log("[-] Buffer pool exhausted");
            throw new IllegalStateException("Buffer pool exhausted");
        }
        return b;
    }

    public void release(ByteBuffer b) {
        b.clear();
        buffer.push(b);
    }
}