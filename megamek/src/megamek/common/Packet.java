/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.*;
import java.util.zip.*;

/**
 * Packet has functionality to turn messages into
 * byte arrays and back.
 *
 *
 */
public class Packet
implements Serializable {
    public static final int        COMMAND_SERVER_GREETING      = 1;
    public static final int        COMMAND_CLIENT_NAME          = 2;
    public static final int        COMMAND_LOCAL_PN             = 3;
    
    public static final int        COMMAND_PLAYER_ADD           = 4;
    public static final int        COMMAND_PLAYER_REMOVE        = 5;
    public static final int        COMMAND_PLAYER_UPDATE        = 6;
    public static final int        COMMAND_PLAYER_READY         = 7;
    
    public static final int        COMMAND_CHAT                 = 8;
    
    public static final int        COMMAND_ENTITY_ADD           = 9;
    public static final int        COMMAND_ENTITY_REMOVE        = 10;
    public static final int        COMMAND_ENTITY_MOVE          = 11;
    public static final int        COMMAND_ENTITY_ATTACK        = 12;
    public static final int        COMMAND_ENTITY_READY         = 13;
    public static final int        COMMAND_ENTITY_UPDATE        = 14;
    
    public static final int        COMMAND_PHASE_CHANGE         = 15;
    public static final int        COMMAND_TURN                 = 16;
    
    public static final int        COMMAND_SENDING_BOARD        = 17;
    public static final int        COMMAND_SENDING_ENTITIES     = 18;
    public static final int        COMMAND_SENDING_PLAYERS      = 19;
    public static final int        COMMAND_SENDING_REPORT       = 20;
    public static final int        COMMAND_SENDING_GAME_SETTINGS= 21;
    public static final int        COMMAND_SENDING_MAP_SETTINGS = 22;
    public static final int        COMMAND_QUERY_MAP_SETTINGS   = 23;
    
    
    private int command;
    private Object[] data;
    private boolean zipped = false;
    
    /**
     * Contructs a new Packet with just the command and no
     * message.
     *
     * @param command        the command.
     */
    public Packet(int command) {
        this(command, null);
    }
    
    /**
     * Packet with a command and a single object
     */
    public Packet(int command, Object object) {
        this.command = command;
        this.data = new Object[1];
        this.data[0] = object;
    }
    
    /**
     * Packet with a command and an array of objects
     */
    public Packet(int command, Object[] data) {
        this.command = command;
        this.data = data;
    }
    
    /**
     * Returns the command associated.
     */
    public int getCommand() {
        return command;
    }
    
    /**
     * Returns the data in the packet
     */
    public Object[] getData() {
        if (zipped) {
            unzipData();
        }
        return data;
    }
    
    /**
     * Returns the object at the specified index
     */
    public Object getObject(int index) {
        if (zipped) {
            unzipData();
        }
        return data[index];
    }
    
    
    /**
     * Zips the data.  Sets the zipped flag and puts the zipped data into
     * data[0].
     */
    public void zipData() {
        if (zipped) {
            return;
        }
        
//        long start = System.currentTimeMillis();
        
        // zip
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            GZIPOutputStream zos = new GZIPOutputStream(baos);
            
            ObjectOutputStream oos = new ObjectOutputStream(zos);
            oos.writeObject(data);
            oos.close();
            
            data = new Object[1];
            byte[] bytes = baos.toByteArray();
            
//            System.out.println("zipData: data is " + bytes.length + " total bytes in data.");
//            System.out.println("zipData: data is " + zipEntry.getSize() + " bytes uncompressed, " + zipEntry.getCompressedSize() + " bytes compressed, " + bytes.length + " total bytes in data.");
            
            data[0] = bytes;
            
            // set the flag
            zipped = true;
        } catch (IOException ex) {
            System.err.println("error zipping data");
            System.err.println(ex);
        }
        
//        long end = System.currentTimeMillis();
//        System.out.println("Packet: zipData completed in " + (end - start) + " ms.");
    }
    
    /**
     * Undoes the results of zipData
     */
    public void unzipData() {
        if (!zipped) {
            return;
        }
        
//        long start = System.currentTimeMillis();
        
        // unzip
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream((byte[])data[0]);

            GZIPInputStream zis = new GZIPInputStream(bais);

            ObjectInputStream ois = new ObjectInputStream(zis);
            data = (Object[])ois.readObject();
            ois.close();
        
            // reset the flag
            zipped = false;
        } catch (IOException ex) {
            System.err.println("error unzipping data");
            System.err.println(ex);
        } catch (ClassNotFoundException ex) {
        }
        
//        long end = System.currentTimeMillis();
//        System.out.println("Packet: unzipData completed in " + (end - start) + " ms.");
    }
    
    /**
     * Returns the int value of the object at the specified index
     */
    public int getIntValue(int index) {
        return ((Integer)getObject(index)).intValue();
    }
    
    /**
     * Returns the boolean value of the object at the specified index
     */
    public boolean getBooleanValue(int index) {
        return ((Boolean)getObject(index)).booleanValue();
    }
    
    
    
    
}
