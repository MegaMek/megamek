/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.battlearmor;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.missiles.RLWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISBARL5 extends RLWeapon {
    private static final long serialVersionUID = -6957164916144671184L;

    public ISBARL5() {
        super();
        name = "Rocket Launcher 5";
        setInternalName("ISBARL5");
        addLookupName("BARL5");
        addLookupName("ISBARocketLauncher5");
        addLookupName("IS BA RLauncher-5");
        rackSize = 5;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 14;
        bv = 6;
        cost = 6000;
        tonnage = .125;
        criticals = 4;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        // This value gets divided by 10 for being one-shot
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.29 : 0;
    }
}
