/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.autocannons;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ISAC5 extends ACWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public ISAC5() {
        super();
        name = "AC/5";
        setInternalName("Autocannon/5");
        addLookupName("IS Auto Cannon/5");
        addLookupName("Auto Cannon/5");
        addLookupName("AC/5");
        addLookupName("AutoCannon/5");
        addLookupName("ISAC5");
        addLookupName("IS Autocannon/5");
        sortingName = "AC/05";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 8.0;
        criticals = 4;
        bv = 70;
        cost = 125000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(true).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2240, 2250, 2255, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2240, 2250, 2255, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false).setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
    }
}
