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

package megamek.common.weapons.battleArmor.clan;

import java.io.Serial;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.battleArmor.BAFlamerWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class CLBAFlamer extends BAFlamerWeapon {
    @Serial
    private static final long serialVersionUID = 8782512971175525221L;

    public CLBAFlamer() {
        super();
        name = "Flamer [BA]";
        setInternalName("CLBAFlamer");
        addLookupName("Clan BA Flamer");
        addLookupName("ISBAFlamer");
        sortingName = "Flamer C";
        heat = 3;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.15;
        criticalSlots = 1;
        bv = 5;
        cost = 7500;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        rulesRefs = "255, TM";
        techAdvancement.setTechBase(TechBase.ALL);
        techAdvancement.setClanAdvancement(2860, 2868, 3050);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3050);
        techAdvancement.setIntroLevel(false);
        techAdvancement.setPrototypeFactions(Faction.CWF).setProductionFactions(Faction.CWF);
        techAdvancement.setTechRating(TechRating.C);
        techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.D,
              AvailabilityValue.B,
              AvailabilityValue.B);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        return (rangeband <= AlphaStrikeElement.RANGE_BAND_SHORT) ? 2 : 0;
    }
}
