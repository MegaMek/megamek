/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.artillery;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ThumperCannon extends ArtilleryCannonWeapon {
    @Serial
    private static final long serialVersionUID = -1951764278554798130L;

    public ThumperCannon() {
        super();

        name = "Thumper Cannon";
        setInternalName("ISThumperCannon");
        addLookupName("ISThumperArtilleryCannon");
        addLookupName("IS Thumper Cannon");
        addLookupName("CLThumper Cannon");
        addLookupName("CLThumperArtilleryCannon");
        addLookupName("CL Thumper Cannon");
        sortingName = "Cannon Arty Thumper";
        heat = 5;
        rackSize = 5;
        ammoType = AmmoType.AmmoTypeEnum.THUMPER_CANNON;
        minimumRange = 3;
        shortRange = 4;
        mediumRange = 9;
        longRange = 14;
        extremeRange = 21;
        tonnage = 10;
        criticalSlots = 7;
        bv = 41;
        cost = 200000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_MED;
        rulesRefs = "97, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.LC, Faction.CWF)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.375;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.5;
        } else {
            return 0;
        }
    }

}
