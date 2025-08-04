/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.battlearmor;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.missiles.RLWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISBARL1 extends RLWeapon {
    private static final long serialVersionUID = 9080214985232453233L;

    public ISBARL1() {
        super();
        name = "Rocket Launcher 1";
        setInternalName("ISBARL1");
        addLookupName("BA RL 1");
        addLookupName("BARL1");
        addLookupName("ISBARocketLauncher1");
        addLookupName("IS BA RLauncher-1");
        rackSize = 1;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 18;
        bv = 2;
        cost = 1500;
        tonnage = .025;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        // This value gets divided by 10 for being one-shot
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.1 : 0;
    }
}
