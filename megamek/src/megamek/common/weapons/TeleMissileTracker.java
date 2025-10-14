/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import java.io.Serial;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

/**
 * TeleMissile Tracker  - holds a list of tele-missiles controlled by this entity and information on what particular
 * weapon controls them
 */
public class TeleMissileTracker implements Serializable {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6913144265531983734L;
    /**
     * map the entity id of the missile to the weapon that it came from
     */
    private final Hashtable<Integer, Integer> missiles;

    /**
     * Creates new instance of the tracker
     */
    public TeleMissileTracker() {
        missiles = new Hashtable<>();
    }

    public void addMissile(int wId, int missileId) {
        missiles.put(wId, missileId);
    }

    public void removeMissile(int wId) {
        missiles.remove(wId);
    }

    public int getMissile(int wId) {
        return missiles.get(wId);
    }

    public boolean containsLauncher(int wId) {
        return missiles.containsKey(wId);
    }

    public Vector<Integer> getMissiles() {
        return new Vector<>(missiles.values());
    }
}
