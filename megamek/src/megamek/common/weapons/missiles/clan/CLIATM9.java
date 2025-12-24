/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.missiles.clan;

import java.io.Serial;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.CLIATMWeapon;

/**
 * @author Sebastian Brocks, edited by Greg
 */
public class CLIATM9 extends CLIATMWeapon {
    @Serial
    private static final long serialVersionUID = 1L;

    public CLIATM9() {
        super();
        this.name = "Improved ATM 9";
        this.setInternalName("CLiATM9");
        this.addLookupName("Clan iATM-9");
        this.heat = 6;
        this.rackSize = 9;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 5.0;
        this.criticalSlots = 4;
        this.bv = 231; // Ammo BV is 54
        this.cost = 450000;
        this.shortAV = this.getBaseAeroDamage(); // This is a streak weapon so we use the rack size for the AV
        this.medAV = this.shortAV;
        this.maxRange = RANGE_MED;
        rulesRefs = "59, IO:AE";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 2.7;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 1.8;
        } else {
            return 0.9;
        }
    }
}
