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
public class ISAC10 extends ACWeapon {
    private static final long serialVersionUID = 814114264108820161L;

    public ISAC10() {
        super();
        name = "AC/10";
        setInternalName("Autocannon/10");
        addLookupName("IS Auto Cannon/10");
        addLookupName("Auto Cannon/10");
        addLookupName("AutoCannon/10");
        addLookupName("AC/10");
        addLookupName("ISAC10");
        addLookupName("IS Autocannon/10");
        heat = 3;
        damage = 10;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 12.0;
        criticals = 7;
        bv = 123;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(true).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
    }
}
