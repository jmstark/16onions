/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.voidphone.api;

import java.nio.ByteBuffer;
import protocol.Protocol.MessageType;

/**
 *
 * @author troll
 */
public abstract class Message {

    protected int size;
    public static final long UINT32_MAX = (1L << 32) - 1;
    public static final int UINT16_MAX = (1 << 16) - 1;
    public static final short UINT8_MAX = 0x00ff;

    private boolean headerAdded;
    private MessageType type;

    protected Message() {
        this.size = 0;
        this.headerAdded = false;
    }

    protected final void addHeader(MessageType type) {

        assert (!this.headerAdded);
        this.headerAdded = true;
        this.type = type;
        this.size += 4;
    }

    protected final void changeMessageType(MessageType type) {
        assert (this.headerAdded);
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message)) {
            return false;
        }
        Message otherMsg = (Message) obj;
        if (otherMsg.getSize() != size) {
            return false;
        }
        return otherMsg.getType() == type;
    }

    /**
     * Serialize the message into a byte buffer.
     *
     * @param out the bytebuffer to hold the serialized message
     */
    protected void send(ByteBuffer out) {
        assert (this.headerAdded);
        out.putShort((short) this.size);
        out.putShort((short) this.type.getNumVal());
    }

    protected final void sendEmptyBytes(ByteBuffer out, int nbytes) {
        assert (0 < nbytes);
        byte[] zeros = new byte[nbytes];
        out.put(zeros);
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the type
     */
    public MessageType getType() {
        return type;
    }

    public static short unsignedShortFromByte(byte value) {
        return (short) (value & ((short) 0xff));
    }

    public static int unsignedIntFromShort(short value) {
        return ((int)value) & 0xffff;
    }

    public static long unsignedLongFromInt(int value) {
        return value & 0xffffffffL;
    }

    public static int getUnsignedShort(ByteBuffer buf) {
        return unsignedIntFromShort(buf.getShort());
    }
}
