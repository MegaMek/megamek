/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.net;

import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketCommand;

/**
 * Application layer data packet used to exchange information between client and server.
 */
public class Packet {
    //region Variable Declarations
    private final PacketCommand command;
    private final Object[] data;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a <code>Packet</code> with a command and an array of objects
     */
    public Packet(final PacketCommand command, final Object... data) {
        this.command = command;
        this.data = data;
    }
    //endregion Constructors

    //region Getters
    /**
     * @return the command associated with this packet
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
     * @return the object at the specified index
     */
    public @Nullable Object getObject(final int index) {
        return (index < data.length) ? data[index] : null;
    }

    /**
     * @param index the index of the desired object
     * @return the <code>int</code> value of the object at the specified index
     */
    public int getIntValue(final int index) {
        return (Integer) getObject(index);
    }

    /**
     * @param index the index of the desired object
     * @return the <code>boolean</code> value of the object at the specified index
     */
    public boolean getBooleanValue(final int index) {
        return (Boolean) getObject(index);
    }
    //endregion Getters
}
