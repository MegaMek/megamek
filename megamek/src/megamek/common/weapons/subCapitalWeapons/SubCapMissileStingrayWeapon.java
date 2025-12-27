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

package megamek.common.weapons.subCapitalWeapons;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.interfaces.ITechnology;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class SubCapMissileStingrayWeapon extends SubCapMissileWeapon {
    @Serial
    private static final long serialVersionUID = 3827228773281489872L;

    public SubCapMissileStingrayWeapon() {
        super();
        this.name = "Sub-Capital Missile Launcher (Stingray)";
        this.setInternalName(this.name);
        this.addLookupName("Stingray");
        this.addLookupName("CLStingray");
        this.shortName = "Stingray";
        this.heat = 9;
        this.damage = 3;
        this.ammoType = AmmoType.AmmoTypeEnum.STINGRAY;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 120.0;
        this.bv = 496;
        this.cost = 85000;
        this.flags = flags.or(WeaponType.F_MISSILE);
        this.atClass = WeaponType.CLASS_CAPITAL_MISSILE;
        this.shortAV = 3.5;
        this.medAV = 3.5;
        this.missileArmor = 35;
        this.maxRange = WeaponType.RANGE_MED;
        rulesRefs = "156, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X,
                    AvailabilityValue.X,
                    AvailabilityValue.F,
                    AvailabilityValue.D)
              .setISAdvancement(ITechnology.DATE_NONE, 3060, 3072, ITechnology.DATE_NONE, ITechnology.DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(ITechnology.DATE_NONE, 3070, 3072, ITechnology.DATE_NONE, ITechnology.DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 3.5 : 0;
    }
}
