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

/*
 * ClubAttackAction.java
 *
 * Created on April 3, 2002, 2:37 PM
 */

package megamek.common.actions;

import megamek.common.*;

/**
 * The attacker makes a club attack on the target.  This also covers mech
 * melee weapons like hatchets.
 *
 * @author  Ben
 * @version 
 */
public class ClubAttackAction extends AbstractAttackAction {
    
    private Mounted club;

    /** Creates new ClubAttackAction */
    public ClubAttackAction(int entityId, int targetId, Mounted club) {
        super(entityId, targetId);
        this.club = club;
    }

    public Mounted getClub() {
        return club;
    }
    
    public void setClub(Mounted club) {
        this.club = club;
    }
}
