/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.net.packets;

import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketCommand;

/**
 * Application layer data packet used to exchange information between client and server.
 */
public class Packet {
    private PacketCommand command;
    private Object[] data;

    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     *
     * @param command
     * @param data
     */
    public Packet(PacketCommand command, Object... data) {
        this.command = command;
        this.data = data;
    }

    /**
     * Returns the command associated.
     */
    public PacketCommand getCommand() {
        return command;
    }

    /**
     * Returns the data in the packet
     */
    public Object[] getData() {
        return data;
    }

    /**
     * Returns the object at the specified index
     *
     * @param index the index of the desired object
     * @return the object at the specified index
     */
    public @Nullable Object getObject(final int index) {
        return (index < data.length) ? data[index] : null;
    }

    /**
     * Returns the <code>int</code> value of the object at the specified index
     *
     * @param index the index of the desired object
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(int index) {
        return (Integer) getObject(index);
    }

    /**
     * Returns the <code>boolean</code> value of the object at the specified
     * index
     *
     * @param index the index of the desired object
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(int index) {
        return (Boolean) getObject(index);
    }
}
