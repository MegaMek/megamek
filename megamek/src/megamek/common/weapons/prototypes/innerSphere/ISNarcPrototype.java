/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.prototypes.innerSphere;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.other.NarcWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISNarcPrototype extends NarcWeapon {
    @Serial
    private static final long serialVersionUID = 5562345335764812479L;

    public ISNarcPrototype() {
        super();
        // TODO : IO pg 73 - Proto Narcs only give +1 to cluster table
        this.name = "Prototype Narc Missile Beacon";
        this.setInternalName("ISNarcBeaconPrototype");
        this.addLookupName("PrototypeNARCBeacon");
        shortName = "Narc (P)";
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0;
        this.criticalSlots = 2;
        this.bv = 15;
        this.cost = 300000;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "66, IO:AE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2580, DATE_NONE, DATE_NONE, 2587, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
