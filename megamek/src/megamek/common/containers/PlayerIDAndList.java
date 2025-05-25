/*
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
package megamek.common.containers;

import java.io.Serial;
import java.util.Vector;

/**
 * TODO: Adjust and replace this class with one that has the data as a member of the class versus just extending it.
 *
 * @param <T> the datatype for the vector.
 *
 * @author dirk This class is one of the common container types which is a player id combined with a list of other data,
 *       such as when transmitting artillery auto hit coordinates.
 */
public class PlayerIDAndList<T> extends Vector<T> {
    @Serial
    private static final long serialVersionUID = 391550235984284684L;
    private int playerID;

    /**
     * Returns the player ID
     *
     * @return the playerID
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * sets the playerID
     *
     * @param playerID the playerID
     */
    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    /**
     * Clone method as is required for Vector. SuppressWarnings is needed as there is no clean way to clone a Vector of
     * Data with type checking.
     *
     * @return a clone of {@link PlayerIDAndList} that is templated to type T
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized PlayerIDAndList<T> clone() {
        PlayerIDAndList<T> playerIDandList = (PlayerIDAndList<T>) super.clone();
        playerIDandList.playerID = playerID;
        return playerIDandList;
    }
}
