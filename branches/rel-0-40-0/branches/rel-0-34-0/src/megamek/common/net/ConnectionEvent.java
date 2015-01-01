/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.net;

/**
 * Instances of descendant classes are sent as a result of changes of the
 * Connection state or packet arrival.
 * 
 * @see ConnectionListener
 */
public abstract class ConnectionEvent extends java.util.EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = 6124300183866317006L;
    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = 1;
    public static final int PACKET_RECEIVED = 2;

    private int type;

    /**
     * Constructs connection event
     * 
     * @param source The object on which the Event initially occurred.
     * @param type event type
     */
    protected ConnectionEvent(Object source, int type) {
        super(source);
        this.type = type;
    }

    /**
     * Returns the type of the event
     * 
     * @return the type of the event
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the connection on which the Event occured; Equivalent to the
     * getSource()
     * 
     * @return
     */
    public IConnection getConnection() {
        return (IConnection) getSource();
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        switch (this.type) {
            case CONNECTED:
                buff.append("Connected");
                break;
            case DISCONNECTED:
                buff.append("Disconnected");
                break;
            case PACKET_RECEIVED:
                buff.append("Packet Received");
                break;
            default:
                buff.append("Unknown");
                break;
        }
        buff.append(" connection event ");
        return buff.toString();
    }

}
