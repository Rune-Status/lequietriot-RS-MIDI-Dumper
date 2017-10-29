package application.util;

/**
 * Created by hadyn on 4/27/15.
 */
public class Buffer {

    private byte[] bytes;
    private int position;

    public Buffer(int capacity) {
        bytes = new byte[capacity];
    }

    public Buffer(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte getInt8(int off) { return bytes[off]; }
    public byte getInt8() {
        return bytes[position++];
    }

    public int getUInt8(int off) { return bytes[off] & 0xff; }
    public int getUInt8() {
        return bytes[position++] & 0xff;
    }

    public void putInt8(int value) {
        bytes[position++] = (byte) value;
    }

    public void putInt16(int value) {
        bytes[position++] = (byte) (value >> 8);
        bytes[position++] = (byte) value;
    }

    public void putInt24(int value) {
        bytes[position++] = (byte) (value >> 16);
        bytes[position++] = (byte) (value >> 8);
        bytes[position++] = (byte) value;
    }

    public void putInt32(int value) {
        bytes[position++] = (byte) (value >> 24);
        bytes[position++] = (byte) (value >> 16);
        bytes[position++] = (byte) (value >> 8);
        bytes[position++] = (byte) value;
    }

    public void putIntLength(int len) {
        bytes[position - len - 4] = (byte) (len >> 24);
        bytes[position - len - 3] = (byte) (len >> 16);
        bytes[position - len - 2] = (byte) (len >> 8);
        bytes[position - len - 1] = (byte)  len;
    }

    public int getUInt24() { return (bytes[position++] & 0xff) << 16 | (bytes[position++] & 0xff) << 8 | bytes[position++] & 0xff; }

    public int getInt32() {
        return (bytes[position++] & 0xff) << 24 | (bytes[position++] & 0xff) << 16 | (bytes[position++] & 0xff) << 8 | bytes[position++] & 0xff;
    }

    public int getUInt16() {
        return (bytes[position++] & 0xff) << 8 | bytes[position++] & 0xff;
    }

    public int position() {
        return position;
    }

    public void position(int old) {
        this.position = old;
    }

    public void get(byte[] dest, int off, int len) {
        for(int i = 0; i < len; i++) {
            dest[off + i] = bytes[position++];
        }
    }

    public void put(byte[] src) {
        put(src, 0, src.length);
    }

    public void put(byte[] src, int off, int len) {
        for(int i = 0; i < len; i++) {
            bytes[position++] = src[off + i];
        }
    }

    public byte[] array() {
        return bytes;
    }

    public int remaining() {
        return bytes.length - position;
    }

    public void skip(int count) {
        position += count;
    }

    public void putVariableLength(int val) {
        if((0xffffff80 & val) != 0) {
            if((0xffffc000 & val) != 0) {
                if((val & 0xffe00000) != 0) {
                    if((0xf0000000 & val) != 0) {
                        putInt8(val >>> 28 | 128);
                    }
                    putInt8(val >>> 21 | 0x80);
                }
                putInt8(val >>> 14 | 0x80);
            }
            putInt8(val >>> 7 | 0x80);
        }
        putInt8(val & 0x7f);
    }

    public int getVariableLength() {
        byte b = bytes[position++];
        int val;
        for(val = 0; 0 > b; b = bytes[position++]) {
            val = (127 & b | val) << 7;
        }
        return b | val;
    }

    public void xor(int key, int off, int len) {
        for(int i = 0; i < len; i++) {
            bytes[off + i] ^= key;
        }
    }

    public static Buffer wrap(byte[] bytes, int off, int len) {
        Buffer buf = new Buffer(bytes);
        buf.position(off);
        return buf;
    }

    public int capacity() {
        return bytes.length;
    }
}
