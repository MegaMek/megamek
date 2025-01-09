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

public interface Role {

    ASRange preferredRange();

    UnitRole getRole();

    boolean moveThroughCover(ASRange range);

    boolean tailTargets();

    boolean targetsLastAttacker();

    boolean dropToCoverIfDamaged();

    boolean disengageIfDamaged();

    boolean preferredTarget(UnitRole role);

    static Role getRole(UnitRole role) {
        return switch (role) {
            case AMBUSHER -> new Ambusher();
            case BRAWLER -> new Brawler();
            case JUGGERNAUT -> new Juggernaut();
            case MISSILE_BOAT -> new MissileBoat();
            case SCOUT -> new Scout();
            case SKIRMISHER -> new Skirmisher();
            case SNIPER -> new Sniper();
            case STRIKER -> new Striker();
            default -> new Brawler();
        };
    }
}
