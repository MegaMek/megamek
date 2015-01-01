/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLMPod extends MPodWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1428507917582780048L;

    /**
     * 
     */
    public CLMPod() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        this.name = "M-Pod";
        this.setInternalName("CLMPod");
        this.addLookupName("CLM-Pod");
        this.introDate = 3064;
        this.techLevel.put(3064, techLevel.get(3071));
        this.availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D };
        this.techRating = RATING_D;
    }
}
