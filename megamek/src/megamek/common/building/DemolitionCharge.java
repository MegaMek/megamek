/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *                     (C) 2018 The MegaMek Team
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
package megamek.common.building;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import megamek.common.Coords;

public class DemolitionCharge implements Serializable {

    private static final long serialVersionUID = -1;

    public DemolitionCharge(int playerId, int damage, Coords pos) {
        if (damage < 1) {
            throw new IllegalArgumentException("damage must be > 0: " + damage); //$NON-NLS-1$
        }
        this.damage = damage;
        this.playerId = playerId;
        this.pos = Objects.requireNonNull(pos);
    }

    /**
     * A UUID to keep track of the identify of this demolition charge.
     * Since we could have multiple charges in the same building hex, we
     * can't track identity based upon owner and damage.  Additionally,
     * since we pass objects across the network, we need a mechanism to
     * track identify other than memory address.
     */
    private final UUID uuid = UUID.randomUUID();
    private final int damage;
    private final int playerId;
    private final Coords pos;

    public UUID getUuid() {
        return uuid;
    }

    public int getDamage() {
        return damage;
    }

    public int getPlayerId() {
        return playerId;
    }

    public Coords getPos() {
        return pos;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DemolitionCharge other = (DemolitionCharge) obj;
        return uuid.equals(other.uuid)
            && damage == other.damage
            && playerId == other.playerId;
    }

}
