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
package megamek.common.modifiers;

/**
 * This class represents a modifier that is one of the options for energy weapons at salvage quality, CO p.215. Note that this modifier can
 * be applied to any weapon.
 */
public class SalvageQualityHeatModifier extends SimpleWeaponHeatModifier {

    /**
     * Creates a weapon modifier that makes the weapon create 2 extra heat when fired, as applicable to energy weapons, CO p.215.
     */
    public SalvageQualityHeatModifier() {
        super(2);
    }
}
