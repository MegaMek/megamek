/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.board;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the valid facings for deploying a multi-hex entity at a specific position and elevation.
 * For facing-dependent entities (like non-symmetrical BuildingEntity), different facings may result
 * in different deployment validity due to secondary hex positions being rotated.
 */
public class FacingOption {
    private final Coords position;
    private final int elevation;
    private final Set<Integer> validFacings;

    /**
     * Creates a new FacingOption for a given position and elevation.
     *
     * @param position  The board coordinates
     * @param elevation The elevation/altitude
     */
    public FacingOption(Coords position, int elevation) {
        this.position = position;
        this.elevation = elevation;
        this.validFacings = new HashSet<>();
    }

    /**
     * Gets the position for this facing option.
     *
     * @return The board coordinates
     */
    public Coords getPosition() {
        return position;
    }

    /**
     * Gets the elevation for this facing option.
     *
     * @return The elevation/altitude
     */
    public int getElevation() {
        return elevation;
    }

    /**
     * Gets the set of valid facings (0-5) for deployment at this position and elevation.
     *
     * @return Set of valid facing values
     */
    public Set<Integer> getValidFacings() {
        return new HashSet<>(validFacings);
    }

    /**
     * Adds a valid facing.
     *
     * @param facing The facing to add (0-5)
     */
    public void addValidFacing(int facing) {
        if (facing >= 0 && facing < 6) {
            validFacings.add(facing);
        }
    }

    /**
     * Checks if a specific facing is valid for deployment.
     *
     * @param facing The facing to check (0-5)
     * @return true if this facing allows deployment
     */
    public boolean isFacingValid(int facing) {
        return validFacings.contains(facing);
    }

    /**
     * Checks if any facings are valid.
     *
     * @return true if at least one facing is valid
     */
    public boolean hasValidFacings() {
        return !validFacings.isEmpty();
    }

    /**
     * Gets the number of valid facings.
     *
     * @return Count of valid facings
     */
    public int getValidFacingCount() {
        return validFacings.size();
    }

    @Override
    public String toString() {
        return "FacingOption{position=" + position + ", elevation=" + elevation +
              ", validFacings=" + validFacings + "}";
    }
}