/*
 * Copyright (c) 2020, Robbe Van der Gucht
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package mcrcon;

import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static java.util.Arrays.copyOfRange;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author robbe
 */
public class RcPacket {
    
    public static int leBytesToInt32(byte[] buffer) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    public int size;
    public int id;
    public int cmd;
    public byte[] body;

    RcPacket(int size, byte[] packetBytes) throws ProtocolException {
        this.size = size;
        this.id = leBytesToInt32(packetBytes);
        this.cmd = leBytesToInt32(copyOfRange(packetBytes, 4, 8));
        this.body = copyOfRange(packetBytes, 8, size - 2); /* -2 for the body 
                                                            * and packet 
                                                            * terminator*/
        
        if (packetBytes[size - 2] != (byte)0x00 ||
            packetBytes[size - 1] != (byte)0x00) {
            throw new ProtocolException("Terminators are non zero.");
        }
    }
    
    /**
     *
     * @param id
     * @param cmd
     * @param data
     */
    RcPacket(int id, int cmd, String data) {
        this(id, cmd, data.getBytes(StandardCharsets.UTF_8));        
    }
    
    /**
     *
     * @param id
     * @param cmd
     * @param data
     */
    RcPacket(int id, int cmd, byte[] data) {
        this.id = id;
        this.cmd = cmd;
        this.body = data;
        this.size = 4 + 4 + data.length + 1 + 1;
    }
    
    /**
     *
     * @return byte[]
     */
    public byte[] getBytes() {
        byte[] buffer = ByteBuffer.allocate(4 + this.size)
                   .order(ByteOrder.LITTLE_ENDIAN)
                   .putInt(this.size)
                   .putInt(this.id)
                   .putInt(this.cmd)
                   .put(this.body)
                   .put((byte)0x00)
                   .put((byte)0x00)
                   .array();
        return buffer;        
    }

    private static byte[] filterColorBytes(byte[] input) {
        byte[] output = new byte[input.length];
        int currentOutputIndex = 0;
        
        for (int i = 0; i < input.length; i++) {
            if (input[i] == (byte) 0xc2) {
                // Skip this byte but also the following 2
                i += 2;
            } else {
                output[currentOutputIndex++] = input[i];
            }
        }
        return copyOfRange(output, 0, currentOutputIndex);
    }

    public static String printHexBinary(byte[] bytes) {
        String[] sArr = new String[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            sArr[i] = String.format("%02x", bytes[i]);
        }

        return String.join(" ", sArr);
    }

    @Override
    public String toString() {
        //return printHexBinary(filterColorBytes(this.body));
        
        try {
            
            return new String(filterColorBytes(this.body), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RcPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Could not decode body(UTF-8) " + Arrays.toString(this.body);
    }  
}
