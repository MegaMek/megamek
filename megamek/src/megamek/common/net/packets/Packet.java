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

import java.io.Serializable;
import java.util.Arrays;

import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketCommand;

/**
 * Application layer data packet used to exchange information between client and server.
 */
public class Packet implements Serializable {
    private final PacketCommand command;
    private final Object[] data;

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
     * @return the command associated.
     */
    public PacketCommand getCommand() {
        return command;
    }

    /**
     * @return the data in the packet
     */
    public Object[] getData() {
        return data;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the object at the specified index
     */
    public @Nullable Object getObject(final int index) {
        return (index >= 0 && index < data.length) ? data[index] : null;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(int index) {
        Object o = getObject(index);

        if (o instanceof Integer integer) {
            return integer;
        }

        return 0;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(int index) {
        Object o = getObject(index);

        if (o instanceof Boolean bool) {
            return bool;
        }

        return false;
    }

    /**
     * @param index the index of the desired object
     *
     * @return the <code>String</code> value of the object at the specified index
     */
    public String getStringValue(int index) {
        Object o = getObject(index);

        if (o instanceof String string) {
            return string;
        }

        return "";
    }


    @Override
    public String toString() {
        return "Packet [" + command + "] - " + Arrays.toString(data);
    }
}
