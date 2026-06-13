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


package megamek.common.autoResolve.acar.role;

import java.util.EnumSet;
import java.util.Map;

import megamek.common.alphaStrike.ASRange;
import megamek.common.units.UnitRole;

public class Scout implements Role {

    private static final Map<ASRange, Boolean> moveThroughCover = Map.of(
          ASRange.SHORT, true,
          ASRange.MEDIUM, true,
          ASRange.LONG, false,
          ASRange.EXTREME, true,
          ASRange.HORIZON, true
    );

    private static final EnumSet<UnitRole> preferredTargets = EnumSet.of(
          UnitRole.MISSILE_BOAT, UnitRole.SCOUT, UnitRole.SNIPER);

    @Override
    public UnitRole getRole() {
        return UnitRole.SCOUT;
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
        return false;
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
