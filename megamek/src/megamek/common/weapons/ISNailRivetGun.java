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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISNailRivetGun extends NailRivetGunWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5198228513368748633L;

    public ISNailRivetGun() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Nail/Rivet Gun";
        setInternalName(name);
        addLookupName("ISNailRivet Gun");
    }
}
