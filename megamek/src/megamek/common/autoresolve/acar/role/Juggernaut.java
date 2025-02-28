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

public class Juggernaut implements Role {

    private static final Map<ASRange, Boolean> moveThroughCover = Map.of(
            ASRange.SHORT, false,
            ASRange.MEDIUM, true,
            ASRange.LONG, true,
            ASRange.EXTREME, true,
            ASRange.HORIZON, true
        );

    private static final EnumSet<UnitRole> preferredTargets = EnumSet.of(
        UnitRole.MISSILE_BOAT, UnitRole.JUGGERNAUT, UnitRole.SKIRMISHER, UnitRole.BRAWLER, UnitRole.SNIPER);

    @Override
    public UnitRole getRole() {
        return UnitRole.JUGGERNAUT;
    }

    @Override
    public ASRange preferredRange() {
        return ASRange.SHORT;
    }

    @Override
    public boolean moveThroughCover(ASRange range) {
        return moveThroughCover.get(range);
    }

    @Override
    public boolean tailTargets() {
        return false;
    }

    @Override
    public boolean targetsLastAttacker() {
        return true;
    }

    @Override
    public boolean dropToCoverIfDamaged() {
        return false;
    }

    @Override
    public boolean disengageIfDamaged() {
        return false;
    }

    @Override
    public boolean preferredTarget(UnitRole role) {
        return preferredTargets.contains(role);
    }
}
