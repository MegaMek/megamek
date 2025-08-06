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

/**
 * @author Sebastian Brocks
 */
public class CLAdvancedSRM6 extends AdvancedSRMWeapon {
    private static final long serialVersionUID = -6710415894545970512L;

    public CLAdvancedSRM6() {
        super();
        name = "Advanced SRM 6";
        setInternalName("CLAdvancedSRM6");
        addLookupName("Clan Advanced SRM-6");
        addLookupName("Clan Advanced SRM 6");
        rackSize = 6;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        bv = 90;
        flags = flags.or(F_NO_FIRES)
              .or(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        tonnage = .21;
        criticals = 4;
        cost = 90000;
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
    }
}
