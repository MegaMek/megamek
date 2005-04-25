/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.equip;

import java.util.Vector;

/**
 * This class defines all of the ballistic weapon subtypes.
 * Currently, mostly a marker.
 */

public abstract class BallisticType extends UsesAmmoType {

    public BallisticType( Vector valid_ammo ) {
	super(valid_ammo);
    }
    
}

