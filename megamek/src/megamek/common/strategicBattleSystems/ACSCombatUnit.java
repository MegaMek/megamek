/*
 *
 *  * Copyright (c) 25.09.21, 14:25 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package megamek.common.strategicBattleSystems;

import megamek.common.ASDamageVector;

public class ACSCombatUnit extends SBFFormation {

    public final int armor;
    public final ASDamageVector damage;

    public ACSCombatUnit(int arm) {
        armor = arm;
        damage = ASDamageVector.createNormRndDmg(8);
    }


    public ASDamageVector getDamage() {
        return damage;
    }
}
