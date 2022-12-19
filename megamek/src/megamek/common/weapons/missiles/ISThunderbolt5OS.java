/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
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
package megamek.common.weapons.missiles;

/**
 * The One-Shot Thunderbolt 5
 * @author Simon (Juliez)
 */
public class ISThunderbolt5OS extends Thunderbolt5Weapon {

    public ISThunderbolt5OS() {
        super();
        name = "Thunderbolt 5 (OS)";
        setInternalName(name);
        addLookupName("IS OS Thunderbolt-5");
        addLookupName("ISThunderbolt5 (OS)");
        addLookupName("IS Thunderbolt 5 (OS)");
        tonnage = 3.5;
        bv = 13;
        cost = 25000;
        flags = flags.or(F_ONESHOT);
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setISAdvancement(2665, 2676, 3045, 2800, 3030)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2665, 2676, 3045, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW);
    }
}