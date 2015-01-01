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
 */package megamek.common;

import java.io.Serializable;

public class NarcPod implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 8883459353515484784L;
    private int team;
    private int location;

    public NarcPod(int team, int location) {
        this.team = team;
        this.location = location;
    }

    public int getTeam() {
        return team;
    }

    public int getLocation() {
        return location;
    }

    public boolean equals(NarcPod other) {
        if (this.location == other.location && this.team == other.team) {
            return true;
        }
        return false;
    }
}
