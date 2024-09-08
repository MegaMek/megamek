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
package megamek.common;

import java.util.Objects;

/**
 * This is a data record for a possible deployment elevation together with a DeploymentElevationType that
 * signals if the elevation is, e.g., on the ground, on a bridge or submerged. Note that two such records are
 * equal when their elevation and type are equal.
 *
 * @param elevation The elevation or altitude in the hex
 * @param type the DeploymentElevationType (on the ground, on a bridge, ...)
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
