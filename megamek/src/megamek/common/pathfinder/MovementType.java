/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.pathfinder;

import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;

/**
 * Movement types that are relevant for "destruction-aware pathfinding" Have a close relationship but are not exactly
 * one to one with entity movement modes.
 */
public enum MovementType {
    Walker,
    Wheeled,
    WheeledAmphibious,
    Tracked,
    TrackedAmphibious,
    Hover,
    Foot,
    Jump,
    Flyer,
    Water,
    None;

    public static boolean canUseBridge(MovementType movementType) {
        return movementType != Jump &&
              movementType != Flyer &&
              movementType != Water &&
              movementType != None;
    }

    /**
     * Figures out the relevant entity movement mode (for path caching) based on properties of the entity. Mostly just
     * movement mode, but some complications exist for tracked/wheeled vehicles.
     */
    public static MovementType getMovementType(Entity entity) {
        return switch (entity.getMovementMode()) {
            case BIPED, TRIPOD, QUAD -> Walker;
            case INF_LEG -> Foot;
            case TRACKED -> entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ? TrackedAmphibious : Tracked;
            // technically MiscType.F_AMPHIBIOUS and MiscType.F_LIMITED_AMPHIBIOUS apply
            // here too, but are not implemented in general
            case INF_MOTORIZED, WHEELED ->
                  entity.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ? WheeledAmphibious : Wheeled;
            case HOVER -> Hover;
            case BIPED_SWIM, QUAD_SWIM, INF_UMU, SUBMARINE, NAVAL, HYDROFOIL -> Water;
            case VTOL, SPHEROID, AERODYNE, AEROSPACE, AIRSHIP -> Flyer;
            default -> None;
        };
    }
}
