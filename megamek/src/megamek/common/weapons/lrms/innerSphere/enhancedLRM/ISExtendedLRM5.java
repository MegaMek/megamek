/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.lrms.innerSphere.enhancedLRM;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.lrms.ExtendedLRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISExtendedLRM5 extends ExtendedLRMWeapon {
    @Serial
    private static final long serialVersionUID = -6153832907941260136L;

    public ISExtendedLRM5() {
        super();
        name = "Extended LRM 5";
        setInternalName(name);
        addLookupName("IS Extended LRM-5");
        addLookupName("ISExtendedLRM5");
        addLookupName("IS Extended LRM 5");
        addLookupName("ELRM-5 (THB)");
        heat = 3;
        rackSize = 5;
        tonnage = 6.0;
        criticalSlots = 1;
        bv = 67;
        cost = 60000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        rulesRefs = "139, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement
              .setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> ignore) {
        return range == AlphaStrikeElement.SHORT_RANGE ? 0.1 : 0.3;
    }
}
