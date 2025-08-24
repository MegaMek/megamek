/*

 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import megamek.common.board.Coords;

public class DemolitionCharge implements Serializable {
    @Serial
    private static final long serialVersionUID = -6655782801564155668L;
    public int damage;
    public int playerId;
    public Coords pos;
    /**
     * A UUID to keep track of to identify of this demolition charge. Since we could have multiple charges in the same
     * building hex, we can't track identity based upon owner and damage. Additionally, since we pass objects across the
     * network, we need a mechanism to track identify other than memory address.
     */
    public UUID uuid = UUID.randomUUID();

    public DemolitionCharge(int playerId, int damage, Coords p) {
        this.damage = damage;
        this.playerId = playerId;
        this.pos = p;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DemolitionCharge) {
            return uuid.equals(((DemolitionCharge) o).uuid);
        }
        return false;
    }
}
