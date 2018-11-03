/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons.gaussrifles;

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Andrew Hunter
 */
public abstract class GaussWeapon extends AmmoWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 8640523093316267351L;

    public GaussWeapon() {
        super();
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PROTO_WEAPON)
                .or(F_BALLISTIC).or(F_DIRECT_FIRE).or(F_NO_FIRES);
        explosive = true;
        atClass = CLASS_AC;
    }

    @Override
    public void adaptToGameOptions(GameOptions gOp) {
        super.adaptToGameOptions(gOp);

        // Add modes for powering down Gauss weapons PPC field inhibitors according to TacOps, p.102
        if (gOp.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GAUSS_WEAPONS)) {
            addMode("Powered Up");
            addMode("Powered Down");
            setInstantModeSwitch(false);
        } else {
            removeMode("Powered Up");
            removeMode("Powered Down");
        }
    }

}
