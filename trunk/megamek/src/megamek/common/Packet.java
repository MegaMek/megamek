/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
    public static final int        COMMAND_CLOSE_CONNECTION     = 0;
    public static final int        COMMAND_SERVER_GREETING      = 1;
    public static final int        COMMAND_CLIENT_NAME          = 2;
    public static final int        COMMAND_LOCAL_PN             = 3;
    
    public static final int        COMMAND_PLAYER_ADD           = 4;
    public static final int        COMMAND_PLAYER_REMOVE        = 5;
    public static final int        COMMAND_PLAYER_UPDATE        = 6;
    public static final int        COMMAND_PLAYER_READY         = 7;
    // This next one not currently used, and has same number as
    //  unload stranded?
    public static final int        COMMAND_PLAYER_DISMOUNT      = 40;  // reorder me
    
    public static final int        COMMAND_CHAT                 = 8;
    
    public static final int        COMMAND_ENTITY_ADD           = 9;
    public static final int        COMMAND_ENTITY_REMOVE        = 10;
    public static final int        COMMAND_ENTITY_MOVE          = 11;
    public static final int        COMMAND_ENTITY_DEPLOY        = 27;  // reorder me
    public static final int        COMMAND_ENTITY_ATTACK        = 12;
//    public static final int        COMMAND_ENTITY_READY         = 13; // remove me
    public static final int        COMMAND_ENTITY_UPDATE        = 14;
    public static final int        COMMAND_ENTITY_MODECHANGE    = 26;  // reorder me
    public static final int        COMMAND_ENTITY_AMMOCHANGE    = 33;  // reorder me
    
    public static final int        COMMAND_ENTITY_VISIBILITY_INDICATOR = 41;  // reorder me
    public static final int        COMMAND_CHANGE_HEX           = 25;  // reorder me

    public static final int        COMMAND_BLDG_ADD             = 29; // reorder me
    public static final int        COMMAND_BLDG_REMOVE          = 30; // reorder me
    public static final int        COMMAND_BLDG_UPDATE_CF       = 31; // reorder me
    public static final int        COMMAND_BLDG_COLLAPSE        = 32; // reorder me
    
    public static final int        COMMAND_PHASE_CHANGE         = 15;
    public static final int        COMMAND_TURN                 = 16;
    public static final int        COMMAND_ROUND_UPDATE         = 34; //reorder me

    public static final int        COMMAND_SENDING_BOARD        = 17;
    public static final int        COMMAND_SENDING_ENTITIES     = 18;
    public static final int        COMMAND_SENDING_PLAYERS      = 19;
    public static final int        COMMAND_SENDING_TURNS        = 28;  // reorder me
    public static final int        COMMAND_SENDING_REPORT       = 20;
    public static final int        COMMAND_SENDING_GAME_SETTINGS= 21;
    public static final int        COMMAND_SENDING_MAP_SETTINGS = 22;
    public static final int        COMMAND_QUERY_MAP_SETTINGS   = 23;
                                                                
    public static final int        COMMAND_END_OF_GAME          = 24;
    public static final int        COMMAND_DEPLOY_MINEFIELDS    = 35;
    public static final int        COMMAND_REVEAL_MINEFIELD     = 36;
    public static final int        COMMAND_REMOVE_MINEFIELD     = 37;
    public static final int        COMMAND_SENDING_MINEFIELDS   = 38;

    public static final int        COMMAND_REROLL_INITIATIVE    = 39;    
    public static final int        COMMAND_UNLOAD_STRANDED      = 40;    
    
    public static final int        COMMAND_SET_ARTYAUTOHITHEXES = 42;
    public static final int        COMMAND_ARTY_FIRED           = 43;
    
    private int command;
    private Object[] data;
    private boolean zipped = false;
    public int byteLength = 0;
    
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
        if (index >= data.length) {
            return null;
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

        // Don't zip if we have no data.
        if ( null == data ) {
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
            this.byteLength = bytes.length;
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
    
    /**
     * Returns this packet's approximate size, in bytes, if we happen to know 
     * it.  Otherwise, returns 0.
     */
    public int size() {
        if (zipped) {
            return ((byte[])data[0]).length;
        } else {
            return 0;
        }
    }
    /**
     * Dermine if the packet has been zipped.
     *
     * @return  <code>true</code> if the packet has been zipped.
     *          <code>false</code> if the packet has no data or if it
     *          has not been zipped.
     */
    public boolean isZipped() {
        return zipped;
    }

}
