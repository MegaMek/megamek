/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.acar.role;

import megamek.common.UnitRole;
import megamek.common.alphaStrike.ASRange;

import java.util.EnumSet;
import java.util.Map;

public class MissileBoat implements Role {

    private static final Map<ASRange, Boolean> moveThroughCover = Map.of(
            ASRange.SHORT, true,
            ASRange.MEDIUM, false,
            ASRange.LONG, false,
            ASRange.EXTREME, false,
            ASRange.HORIZON, false
        );

    private static final EnumSet<UnitRole> preferredTargets = EnumSet.of(
        UnitRole.MISSILE_BOAT, UnitRole.STRIKER, UnitRole.SCOUT, UnitRole.SNIPER);

    @Override
    public UnitRole getRole() {
        return UnitRole.MISSILE_BOAT;
    }

    @Override
    public ASRange preferredRange() {
        return ASRange.LONG;
    }

    @Override
    public boolean moveThroughCover(ASRange range) {
        return moveThroughCover.get(range);
    }

    @Override
    public boolean tailTargets() {
        return true;
    }

    @Override
    public boolean targetsLastAttacker() {
        return false;
    }

    @Override
    public boolean dropToCoverIfDamaged() {
        return true;
    }

    @Override
    public boolean disengageIfDamaged() {
        return true;
    }

    @Override
    public boolean preferredTarget(UnitRole role) {
        return preferredTargets.contains(role);
    }
}
