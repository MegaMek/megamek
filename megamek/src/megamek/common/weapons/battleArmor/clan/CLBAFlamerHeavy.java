/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.battleArmor.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.battleArmor.BAFlamerWeapon;

/**
 * @since Sep 24, 2004
 */
public class CLBAFlamerHeavy extends BAFlamerWeapon {
    @Serial
    private static final long serialVersionUID = 8041763978361592459L;

    public CLBAFlamerHeavy() {
        super();
        name = "Heavy Flamer [BA]";
        setInternalName("CLBAHeavyFlamer");
        addLookupName("ISBAHeavyFlamer");
        addLookupName("IS BA Heavy Flamer");
        sortingName = "Flamer D";
        heat = 5;
        damage = 4;
        infDamageClass = WeaponType.WEAPON_BURST_6D6;
        shortRange = 2;
        mediumRange = 3;
        longRange = 4;
        extremeRange = 6;
        tonnage = .35;
        criticalSlots = 1;
        bv = 15;
        cost = 11250;
        flags = flags.or(F_FLAMER).or(F_ENERGY).or(F_BA_WEAPON)
              .or(F_BURST_FIRE).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "124, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false).setUnofficial(false).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setAdvancement(DATE_NONE, 3070, 3073, DATE_NONE, DATE_NONE)
              .setApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC, Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        return (rangeband <= AlphaStrikeElement.RANGE_BAND_SHORT) ? 4 : 0;
    }
}
