/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.bombs;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Jay Lawson
 * @since Oct 20, 2004
 */
public class BombArrowIV extends ArtilleryWeapon {
    @Serial
    private static final long serialVersionUID = -1321502140176775035L;

    public BombArrowIV() {
        super();
        this.name = "Arrow IV Bomb Mount";
        this.setInternalName(BombTypeEnum.ARROW.getWeaponName());
        this.heat = 0;
        this.rackSize = 20;
        this.ammoType = AmmoType.AmmoTypeEnum.ARROW_IV_BOMB;
        this.shortRange = 1; //
        this.mediumRange = 2;
        this.longRange = 9;
        this.extremeRange = 9; // No extreme range.
        this.tonnage = 0;
        this.criticalSlots = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 0;
        flags = flags.or(F_BOMB_WEAPON);
        rulesRefs = "171, TO:AUE";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2622, 2623, DATE_NONE, 2850, 3047)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2622, 2623, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.CC);
    }
}
