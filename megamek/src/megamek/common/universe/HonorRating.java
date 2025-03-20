/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMekRoot.megamek.main.
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
 */
package megamek.common.universe;

/**
 * Represents the honor of a Clan
 */
public enum HonorRating {
    NONE(0.0, Integer.MAX_VALUE),
    LIBERAL(1.25, Integer.MAX_VALUE),
    OPPORTUNISTIC(1.0, 5),
    STRICT(0.75, 0);

    private final double bvMultiplier;
    private final int bondsmanTargetNumber;

    /**
     * Constructor for HonorRating enum to initialize its properties.
     *
     * @param bvMultiplier       Battle Value multiplier associated with the honor level - used by
     *                          Clan Bidding
     * @param bondsmanTargetNumber Target number for determining bondsmen with this style
     */
    HonorRating(double bvMultiplier, int bondsmanTargetNumber) {
        this.bvMultiplier = bvMultiplier;
        this.bondsmanTargetNumber = bondsmanTargetNumber;
    }

    /**
     * Gets the Battle Value multiplier associated with this capture style.
     *
     * @return the bvMultiplier
     */
    public double getBvMultiplier() {
        return bvMultiplier;
    }

    /**
     * Gets the target number for becoming a bondsman.
     *
     * @return the bondsmanTargetNumber
     */
    public int getBondsmanTargetNumber() {
        return bondsmanTargetNumber;
    }
}
