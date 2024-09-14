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

/**
 * This enum gives the meaning of an elevation or altitude that a unit can be deployed to. This is used for
 * GUI purposes.
 */
public enum DeploymentElevationType {
    ON_GROUND, ON_ICE, ON_SEAFLOOR, SUBMERGED, BUILDING_FLOOR, BUILDING_TOP, ELEVATION, ALTITUDE, BRIDGE,
    TERRAIN_CEILING, ELEVATIONS_ABOVE, WATER_SURFACE
}
