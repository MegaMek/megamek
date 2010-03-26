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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.server.Server;

/**
 * @author Ben Grills
 */
public class InfantrySupportPortablePlasmaWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5741978934100309295L;

    public InfantrySupportPortablePlasmaWeapon() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Portable Plasma Rifle";
        setInternalName(name);
        addLookupName("InfantryPlasmaRifle");
        addLookupName("InfantryPlasmaPortable");
        addLookupName("InfantryMPPR");
        // Plasma Rifle (man-portable), TM p. 351
        cost = 7500;
        bv = 6.6;
        flags = flags.or(F_DIRECT_FIRE).or(F_PLASMA).or(F_BALLISTIC).or(F_INF_SUPPORT).or(F_INF_ENCUMBER);
        infantryDamage = 1.58;
        infantryRange = 2;
        crew = 1;
    }
}
