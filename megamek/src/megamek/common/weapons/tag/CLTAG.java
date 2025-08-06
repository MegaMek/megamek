/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.tag;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class CLTAG extends TAGWeapon {
    private static final long serialVersionUID = 7446980554102548125L;

    public CLTAG() {
        super();
        name = "TAG";
        setInternalName("CLTAG");
        addLookupName("Clan TAG");
        addLookupName("TAG (Clan)");
        tonnage = 1;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        bv = 0;
        cost = 50000;
        rulesRefs = "238, TM";
        flags = flags.or(F_AERO_WEAPON).or(F_PROTO_WEAPON).andNot(F_BA_WEAPON);
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2828, 2830, 2834, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
    }
}
