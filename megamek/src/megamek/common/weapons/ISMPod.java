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

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISMPod extends MPodWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7462050177159480L;

    /**
     * 
     */
    public ISMPod() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "M-Pod";
        this.setInternalName("ISMPod");
        this.addLookupName("ISM-Pod");
    }
}
