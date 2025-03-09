/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.common;

/**
 * Enum for difficulty levels of the CASPAR AI.
 * @author Luana Coppio
 */
public enum DifficultyLevel {
    BEGINNER(40, 0.8),
    EASY(20, 0.9),
    MEDIUM(10, 1.0),
    HARD(3, 1.1),
    HARDCORE(1, 1.2);

    private final int topPathsToConsider;
    private final double alphaValue;

    DifficultyLevel(int topPathsToConsider, double alphaValue) {
        this.topPathsToConsider = topPathsToConsider;
        this.alphaValue = alphaValue;
    }

    public int getTopPathsToConsider() {
        return topPathsToConsider;
    }

    public double getAlphaValue() {
        return alphaValue;
    }
}
