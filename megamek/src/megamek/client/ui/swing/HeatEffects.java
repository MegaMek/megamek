/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.ui.Messages;

public class HeatEffects {

    public static String getHeatEffects(int heat, boolean mtHeat, boolean hasTSM) {
        String whichOne = "HeatEffects";
        int maxheat = 30;
        if (hasTSM) {
            whichOne += ".tsm";
        }
        if (mtHeat) {
            if (heat >= 30) {
                whichOne += ".mt";
            }
            maxheat = 50;
        }
        whichOne += "." + Math.min(maxheat, heat);
        return Messages.getString(whichOne);
    }

}
