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

import java.util.Objects;

/**
 * This is a data record for a possible deployment elevation together with a DeploymentElevationType that signals if the
 * elevation is, e.g., on the ground, on a bridge or submerged. Note that two such records are equal when their
 * elevation and type are equal. ElevationOptions are comparable, with the natural ordering being by their elevation
 * only.
 *
 * @param elevation The elevation or altitude in the hex
 * @param type      the DeploymentElevationType (on the ground, on a bridge, ...)
 */
public record ElevationOption(int elevation, DeploymentElevationType type) implements Comparable<ElevationOption> {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || (getClass() != o.getClass())) {
            return false;
        }
        ElevationOption that = (ElevationOption) o;
        return (elevation == that.elevation) && (type == that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elevation, type);
    }

    @Override
    public int compareTo(ElevationOption other) {
        return Integer.compare(this.elevation, other.elevation);
    }

    @Override
    public String toString() {
        return "Elevation: " + elevation + " (" + type + ")";
    }
}
