/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.lrms.innerSphere.enhancedLRM;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lrms.EnhancedLRMWeapon;

/**
 * @author BATTLEMASTER
 */
public class ISEnhancedLRM15 extends EnhancedLRMWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 3287950524687857609L;

    /**
     *
     */
    public ISEnhancedLRM15() {
        super();
        name = "Enhanced LRM 15";
        setInternalName(name);
        addLookupName("ISEnhancedLRM15");
        heat = 5;
        rackSize = 15;
        minimumRange = 3;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 9.0;
        criticalSlots = 6;
        bv = 157;
        cost = 218750;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        rulesRefs = "138, TO:AUE";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS        
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3058, DATE_NONE, 3082).setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
