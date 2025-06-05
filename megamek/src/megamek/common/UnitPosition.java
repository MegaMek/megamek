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
package megamek.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class UnitPosition {

    private final Coords position;
    private final Set<Coords> secondaryPositions;
    private final Facing facing;
    private final int elevation;
    private final int altitude;
    private final int height;

    /**
     * Unit Position Data Object
     * @author Luana Coppio
     * @param position unit position using offset coordinates
     * @param secondaryPositions Set of secondary positions of the unit
     * @param facing Facing of the unit, units with no facing are treated as if they were facing north
     * @param elevation current topographical elevation
     * @param altitude current altitude if airborne
     * @param height height of the unit, 0 indexed.
     */
    private UnitPosition(Coords position,
          Set<Coords> secondaryPositions,
          Facing facing,
          int elevation,
          int altitude,
          int height) {
        this.position = position;
        this.secondaryPositions = secondaryPositions;
        this.facing = facing;
        this.elevation = elevation;
        this.altitude = altitude;
        this.height = height;
    }

    public static UnitPosition of(Coords coords) {
        return UnitPosition.of(coords, 0);
    }

    public static UnitPosition of(List<Coords> coords) {
        return new UnitPosition(
              coords.get(0),
              new HashSet<>(coords),
              Facing.NONE,
              0,
              0,
              0);
    }

    public static UnitPosition of(Coords position, int facing) {
        // Fill with zeros as it is a simple position without facing or elevation data
        return new UnitPosition(position, Collections.singleton(position), Facing.valueOfInt(facing), 0, 0, 0);
    }

    public static UnitPosition of(Coords position, Facing facing) {
        // Fill with zeros as it is a simple position without facing or elevation data
        return new UnitPosition(position, Collections.singleton(position), facing, 0, 0, 0);
    }

    public static UnitPosition of(Targetable targetable) {
        return UnitPosition.withFacingOf(targetable, targetable instanceof Entity entity ? entity.getFacing() : 0);
    }

    public static UnitPosition withFacingOf(Targetable targetable, int facing) {
        Set<Coords> targetPositions = new HashSet<>();
        targetPositions.add(targetable.getPosition());
        if (targetable.getSecondaryPositions() != null) {
            targetPositions.addAll(targetable.getSecondaryPositions().values());
        }
        targetPositions.removeIf(Objects::isNull);

        return new UnitPosition(
              targetable.getPosition(),
              targetPositions,
              Facing.valueOfInt(facing),
              targetable.getElevation(),
              targetable.getAltitude(),
              targetable.getHeight());
    }

    public int facingAngle() {
        return facing.getAngle();
    }

    public int relativeDotProduct(Coords target) {
        return relativeDotProduct(position, target);
    }

    public int relativeDotProduct(Coords origin, Coords target) {
        int angle = origin.dotProduct(target) - facingAngle();
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    public Collection<Coords> getCoords() {
        return secondaryPositions;
    }

    public Coords getPosition() {
        return position;
    }

    public Set<Coords> getSecondaryPositions() {
        return secondaryPositions;
    }

    public Facing getFacing() {
        return facing;
    }

    public int getElevation() {
        return elevation;
    }

    public int getAltitude() {
        return altitude;
    }

    public int getHeight() {
        return height;
    }
}
