/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.moves;

/**
 * Enum representing the result of a phase pass.
 * @author Luana Coppio
 * @since 0.50.07
 */
enum PhasePassResult {
    BREAK, // if there is no need to compile the movement on this step, then it should return "break"
    COMPILE; // if there is need to compile the movement, then it should return "compile"

    // This makes it so that we don't need to externally handle the raw values of the enum

    /**
     * Check if the phase pass result requires the move to be compiled.
     * @return true if the phase pass result is compile
     */
    public boolean isCompile() {
        return this == COMPILE;
    }

    // This makes it so that we don't need to externally handle the raw values of the enum

    /**
     * Check if the phase pass result does not want the move to be compiled.
     * @return true if the phase pass result is break
     */
    public boolean isBreak() {
        return this == BREAK;
    }
}
