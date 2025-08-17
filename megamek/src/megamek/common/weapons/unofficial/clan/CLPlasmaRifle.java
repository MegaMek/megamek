/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.unofficial.clan;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.unofficial.PlasmaMFUKWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 13, 2004
 */
public class CLPlasmaRifle extends PlasmaMFUKWeapon {
    private static final long serialVersionUID = 1758452784566087479L;

    public CLPlasmaRifle() {
        super();
        name = "Plasma Rifle";
        setInternalName("MFUK Plasma Rifle");
        addLookupName("Clan Plasma Rifle");
        addLookupName("CL Plasma Rifle");
        addLookupName("CLPlasmaRifle");
        addLookupName("MFUKCLPlasmaRifle");
        heat = 15;
        damage = 10;
        rackSize = 1;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 14;
        longRange = 22;
        extremeRange = 28;
        tonnage = 6.0;
        criticals = 2;
        bv = 400;
        cost = 300000;
        // Gonna use the same tech info as the Cannon
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3068, 3069, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        return (rangeband <= AlphaStrikeElement.RANGE_BAND_MEDIUM) ? 3 : 0;
    }
}
