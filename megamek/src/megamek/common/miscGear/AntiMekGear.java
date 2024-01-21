/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMekLab.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.miscGear;

import megamek.common.EquipmentTypeLookup;
import megamek.common.MiscType;
import megamek.common.SimpleTechLevel;

public class AntiMekGear extends MiscType {

    public AntiMekGear() {
        name = "Anti-Mek Gear";
        setInternalName(EquipmentTypeLookup.ANTI_MEK_GEAR);
        tonnage = 0.015;
        flags = flags.or(F_INF_EQUIPMENT);
        cost = COST_VARIABLE;
        rulesRefs = "155, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(2456, 2460, 2500)
                .setStaticTechLevel(SimpleTechLevel.STANDARD)
                .setApproximate(true, false, false).setTechBase(RATING_D)
                .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D);
    }
}
