/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
import java.util.UUID;

import megamek.common.Coords;

// LEGAL (giorgiga) I'm not sure the above copyright is the correct one
//
// The code in this file was originally in Building.java, so I copied the
// license header from that file.

public class DemolitionCharge implements Serializable {

    private static final long serialVersionUID = -1;

    public int damage;
    public int playerId;
    public Coords pos;

    /**
     * A UUID to keep track of the identify of this demolition charge.
     * Since we could have multiple charges in the same building hex, we
     * can't track identity based upon owner and damage.  Additionally,
     * since we pass objects across the network, we need a mechanism to
     * track identify other than memory address.
     */
    public UUID uuid = UUID.randomUUID();

    public DemolitionCharge(int playerId, int damage, Coords p) {
        this.damage = damage;
        this.playerId = playerId;
        pos = p;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DemolitionCharge) {
            return uuid.equals(((DemolitionCharge)o).uuid);
        }
        return false;
    }
 }
