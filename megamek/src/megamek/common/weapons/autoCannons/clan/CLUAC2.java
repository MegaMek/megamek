/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.autoCannons.clan;

import java.io.Serial;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.autoCannons.UACWeapon;

/**
 * @author Andrew Hunter
 * @since Oct 2, 2004
 */
public class CLUAC2 extends UACWeapon {
    @Serial
    private static final long serialVersionUID = 7982946203794957045L;

    public CLUAC2() {
        super();
        name = "Ultra AC/2";
        setInternalName("CLUltraAC2");
        addLookupName("Clan Ultra AC/2");
        sortingName = "Ultra AC/02";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 2;
        shortRange = 9;
        mediumRange = 18;
        longRange = 27;
        extremeRange = 36;
        tonnage = 5.0;
        criticalSlots = 2;
        bv = 62.0;
        cost = 120000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        maxRange = RANGE_EXT;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CLAN)
              .setProductionFactions(Faction.CLAN);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> ignore) {
        return range == AlphaStrikeElement.SHORT_RANGE ? 0.249 : 0.3;
    }
}
