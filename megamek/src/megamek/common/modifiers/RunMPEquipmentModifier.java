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

public class RunMPEquipmentModifier extends AbstractSystemModifier {

    private final int deltaMP;

    /**
     * Creates a modifier that adds the given delta to run/flank/max thrust MP. The delta value may be any value but will realistically be
     * negative. See Mercenaries Supplemental Update, p.138 (superior rules to CO 4th, p.215).
     *
     * @param deltaMP      The MP to add
     * @param reason       The origin of the modifier
     * @param entitySystem The system that the modifier applies to
     */
    public RunMPEquipmentModifier(int deltaMP, Reason reason, EntitySystem entitySystem) {
        super(reason, entitySystem);
        this.deltaMP = deltaMP;
    }

    public int getDeltaMP() {
        return deltaMP;
    }

    /**
     * @return The delta heat of this modifier with a leading "+" if it is positive, i.e. "+2" or "-1" or "0".
     */
    public String formattedMPModifier() {
        return formattedModifier(deltaMP);
    }
}
