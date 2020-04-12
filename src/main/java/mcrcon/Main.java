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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ConnectException;
import static java.util.Arrays.copyOfRange;

/**
 *
 * @author robbe
 */
public class Main {
    
    public static final int SERVERDATA_AUTH = 3;
    public static final int SERVERDATA_EXECCOMMAND = 2;
    public static final int SERVERDATA_AUTH_RESPONSE = 2;
    public static final int SERVERDATA_RESPONSE_VALUE = 0;
    
    public static final int RCON_PID = 0xBADC0DE;
    
    public static int port = 25575;    
    public static String host = "localhost";
    public static String password = "";
    
    public static Socket s;
    public static OutputStream os;
    public static InputStream is;

    public static void send(RcPacket rp) throws IOException {
        os.write(rp.getBytes());
    }
    
    public static void send(int id, int cmd, String data) throws IOException {
        RcPacket rp = new RcPacket(id, cmd, data);
        send(rp);
    }
    
    public static RcPacket receive() throws IOException {
            byte[] sizeBytes = new byte[4];
            int readLenth = is.read(sizeBytes);
            
            if (readLenth != 4) {
                throw new IOException("Unable to read out the size bytes");
            }
            
            int rcPacketSize = RcPacket.leBytesToInt32(sizeBytes);
            
            if (rcPacketSize < 10) {
                throw new ProtocolException("The size bytes indicate that the length is less than 10.");
            }
            
            byte[] rcPacketBytes = new byte[rcPacketSize];
            readLenth = is.read(rcPacketBytes);
            
            if (readLenth != rcPacketSize) {
                throw new IOException("Unable to read packet bytes");
            }
            
            RcPacket rp = new RcPacket(rcPacketSize, rcPacketBytes);
            return rp;            
    }

    /* WORKING WITH BYTE ARRAYS */
    /* BIG ENDIAN 0x12 34 56 78 
    byte[] bytes;
    bytes = ByteBuffer.allocate(4).putInt(RCON_PID).array();
    System.out.println(Arrays.toString(bytes));
    System.out.println(bytes);
    /* LITTLE ENDIAN 0x78 56 34 12 
    bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(RCON_PID).array();
    System.out.println(Arrays.toString(bytes));
    System.out.println(bytes);
    */
    
    /* PROTOCOL DESCRIPTION 
     * RCON Documentation: https://developer.valvesoftware.com/wiki/Source_RCON_Protocol
     * Java Sockets: http://zetcode.com/java/socket/
     * Bytebuffer: https://blog.fossasia.org/tag/bytebuffer/
     * */
    /* Size | 32-bit signed int LE (i<) The size does not include itself
        * ID   | 32-bit signed int LE 
        * Type | 32-bit signed int LE 
        * Body | Null terminated ASCII string
        * 1 B  | 0x00 terminator
        */
    
    /* AUTH example */
    /* |SIZE       |ID          |AUTH REQ   |Password               |TERM
        *  11 00 00 00 00 00 00 00  03 00 00 00 70 61 73 73 77 72 64 00 00   
        * |SIZE       |ID          |RESP VAL   |EB|TERM
        *  0a 00 00 00 00 00 00 00  00 00 00 00 00 00 
        *                                       ^-Empty Body
        * 
        * |SIZE       |ID          |AUTH RES   |EB|TERM
        *  0a 00 00 00 00 00 00 00  02 00 00 00 00 00 
        *                                       ^-Empty Body
        * |SIZE       |ID          |EXEC CMD   |CMD                                             |TERM
        *  19 00 00 00 00 00 00 00  02 00 00 00 65 63 68 6f 20 48 4c 53 57 3a 20 54  65 73 74 000 0 
        *                                      ^e  c  h  o     H  L  S  W  :     T   e  s  t  .           
        * |SIZE       |ID          |RESP VAL   |RESP DATA
        *  17 00 00 00 00 00 00 00  00 00 00 00 48 4c 53 57 20 3a 20 54 65 73 74 20  0a 00 00 
    */

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        
        final Map<String, String> env = System.getenv();
        
        if (env.containsKey("MCRCON_HOST")) {
            host = env.get("MCRCON_HOST");
        }
        
        if (env.containsKey("MCRCON_PORT")) {
            try {
                String sPort = env.get("MCRCON_PORT");
                port = Integer.parseInt(sPort);
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
                System.err.println("Port: " + env.get("MCRCON_PORT"));
            }
        }
        
        if (env.containsKey("MCRCON_PASS")) {
            password = env.get("MCRCON_PASS");
        }
        
        String command = "";
        int i = 0;
        String arg = args[i];

        while (arg.startsWith("-")) {
            if (arg.equals("-H") || arg.equals("--host")) {
                host = args[i+1];
            }

            if (arg.equals("-P") || arg.equals("--port")) {
                try {
                    port = Integer.parseInt(args[i+1]);
                } catch (NumberFormatException ex) {
                    System.err.println(ex.getMessage());
                    System.err.println("Port: " + args[i+1]);
                }
            }

            if (arg.equals("-p") || arg.equals("--password")) {
                password = args[i+1];
            }

            if (arg.equals("-v") || arg.equals("--version")) {
                System.out.println("Java-based mcrcon");
                System.out.println("Inspired by: https://github.com/Tiiffi/mcrcon");
                return;
            }

            i += 2;
            arg = args[i];
        }
        
        command = String.join(" ", copyOfRange(args, i, args.length));

        if (env.containsKey("MCRCON_VERBOSE") && env.get("MCRCON_VERBOSE").equals("yes")) {
            System.out.println(String.format("Host: %s", host));
            System.out.println(String.format("Port: %d", port));
            System.out.println(String.format("Password: %s", password));
            System.out.println(String.format("Command: %s", command));            
        }
        
        try {
            s = new Socket(host, port);
            System.out.printf("Connected %s %d\r\n", host, port);
            is = s.getInputStream();
            os = s.getOutputStream();
            
            send(RCON_PID, SERVERDATA_AUTH, password);       
            RcPacket ret = receive();
            
            if (ret.cmd == SERVERDATA_AUTH_RESPONSE) {
                System.out.println("We are succesfully authenticated");
            }

            send(RCON_PID, SERVERDATA_EXECCOMMAND, command);       
            ret = receive();
            System.out.println(ret.toString());

  
        } catch (ProtocolException ex) {
            System.err.println(ex.getMessage());
        } catch (ConnectException ex) {
            System.err.println(ex.getMessage() + String.format(" Host: %s:%d\n", host, port));
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                if (s != null) {
                    s.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }    
}
