/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.prototypes;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class PrototypeArrowIV extends ArtilleryWeapon {
    @Serial
    private static final long serialVersionUID = -4495524659692575107L;

    public PrototypeArrowIV() {
        super();
        name = "Prototype Arrow IV";
        setInternalName("ProtoTypeArrowIV");
        addLookupName("ProtoArrowIVSystem");
        shortName = "Arrow IV (P)";
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.AmmoTypeEnum.ARROWIV_PROTO;
        shortRange = 1;
        mediumRange = 2;
        longRange = 8;
        extremeRange = 8; // No extreme range.
        tonnage = 16;
        criticalSlots = 16;
        bv = 240;
        cost = 1800000;
        this.flags = flags.or(F_MISSILE).or(F_PROTOTYPE).or(F_ARTILLERY);
        rulesRefs = "64, IO:AE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2593, DATE_NONE, DATE_NONE, 2613, 3044)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
