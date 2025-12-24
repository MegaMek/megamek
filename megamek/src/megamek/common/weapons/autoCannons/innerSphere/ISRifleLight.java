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

package megamek.common.weapons.autoCannons.innerSphere;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.autoCannons.RifleWeapon;

/**
 * @author Jason Tighe
 * @since Sep 25, 2004
 */
public class ISRifleLight extends RifleWeapon {
    @Serial
    private static final long serialVersionUID = -8493561115043967893L;

    public ISRifleLight() {
        super();
        name = "Rifle (Cannon, Light)";
        setInternalName(name);
        shortName = "Light Rifle";
        addLookupName("IS Light Rifle");
        addLookupName("ISLightRifle");
        sortingName = "Rifle Cannon B";
        heat = 1;
        damage = 3;
        rackSize = 3;
        minimumRange = 0;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 18;
        tonnage = 3.0;
        criticalSlots = 1;
        bv = 21;
        cost = 37750;
        explosive = false; // when firing incendiary ammo
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        maxRange = RANGE_MED;
        explosionDamage = 0;
        rulesRefs = "150, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.D)
              .setISAdvancement(DATE_PS, DATE_PS, 3084, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
