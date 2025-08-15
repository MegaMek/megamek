/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
