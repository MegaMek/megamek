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
 */
package megamek.common;

import java.io.Serializable;

/**
 * @author Sebastian Brocks
 *
 * This class represents an iNarc pod attached to an entity
 */

public class INarcPod implements Serializable {
    
    public static int HOMING  = 1;
    public static int ECM     = 2;
    public static int HAYWIRE = 3;
    public static int NEMESIS = 4;
    
    private int team;
    private int type;
    
    /**
     * Creates a new <code>INarcPod</code>,
     * from the team specified.
     */
    public INarcPod(int team) {
        this.team = team;
    }
    
    public int getTeam() {
        return team;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }

}
