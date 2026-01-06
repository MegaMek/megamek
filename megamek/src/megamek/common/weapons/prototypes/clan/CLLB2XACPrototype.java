/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.prototypes.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class CLLB2XACPrototype extends CLLBXACPrototypeWeapon {
    @Serial
    private static final long serialVersionUID = 3580100820831141313L;

    public CLLB2XACPrototype() {
        super();
        name = "Prototype LB 2-X Autocannon";
        setInternalName("CLLBXAC2Prototype");
        shortName = "LB 2-X (P)";
        sortingName = "Prototype LB 02-X Autocannon";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 9;
        mediumRange = 18;
        longRange = 27;
        extremeRange = 36;
        tonnage = 6.0;
        criticalSlots = 5;
        bv = 42;
        cost = 150000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        extAV = 2;
        maxRange = RANGE_EXT;
        rulesRefs = "91, IO:AE";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2826, DATE_NONE)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CGS)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return (range <= AlphaStrikeElement.SHORT_RANGE) ? 0.069 : 0.105;
    }
}
