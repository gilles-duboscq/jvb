package gd.twohundred.jvb.audioutils;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CircularSampleBuffer {
    private final ByteOrder order;
    private final byte[] buffer;
    private int writePos;
    private int readPos;
    private int readAvailable;

    public CircularSampleBuffer(ByteOrder order, int samples) {
        this.order = order;
        buffer = new byte[samples * 4];
    }

    private int readAvailableBytes() {
        return readAvailable;
    }

    public int readAvailableSamples() {
        return readAvailableBytes() / 4;
    }

    private int writeAvailableBytes() {
        return buffer.length - readAvailable;
    }

    public int writeAvailableSamples() {
        return writeAvailableBytes() / 4;
    }

    public void writeSample(char left, char right) {
        if (writeAvailableBytes() < 4) {
            throw new BufferOverflowException();
        }
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(order);
        bb.putChar(writePos, left);
        bb.putChar(writePos + 2, right);
        writePos += 4;
        readAvailable += 4;
        if (writePos >= buffer.length) {
            writePos = 0;
        }
    }

    public void readTo(Writeable writeable, int samples) {
        int len = samples * 4;
        if (readAvailableBytes() < len) {
            throw new BufferUnderflowException();
        }
        if (writePos > readPos || buffer.length - readPos >= len) {
            writeConsecutiveBytes(writeable, len);
        } else {
            int secondWriteLength = len - (buffer.length - readPos);
            writeConsecutiveBytes(writeable, buffer.length - readPos);
            assert readPos == buffer.length;
            readPos = 0;
            writeConsecutiveBytes(writeable, secondWriteLength);
        }
        readAvailable -= len;
    }

    private void writeConsecutiveBytes(Writeable writeable, int len) {
        int toWrite = len;
        do {
            int read = writeable.write(buffer, readPos, toWrite);
            readPos += read;
            toWrite -= len;
        } while (toWrite > 0);

    }

    public interface Writeable {
        int write(byte[] b, int off, int len);
    }
}
