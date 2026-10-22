/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.nuclear;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.enums.NuclearWeaponType;

/**
 * A nuclear detonation ordered by a GM command, waiting for the end of the weapons attack phase of its round. Kept in
 * the game, so a strike set for a later round survives a save and reload.
 *
 * @param groundZero      the Ground Zero hex
 * @param profile         the detonation's damage, radii and crater depth
 * @param weaponType      the standard weapon fired, or {@code null} for a custom device
 * @param firingPlayerId  the player held responsible; their side is warned of the strike, as with artillery
 * @param detonationRound the round at the end of whose weapons attack phase the strike detonates
 */
public record ScheduledNuke(Coords groundZero, NuclearBlastProfile profile, @Nullable NuclearWeaponType weaponType,
      int firingPlayerId, int detonationRound) {

    /**
     * The earliest round a strike ordered now can detonate in. Strikes go off at the end of the weapons attack phase,
     * so one ordered before that phase is over can still go off this round; one ordered later waits for the next.
     *
     * @param roundCount the current round
     * @param phase      the current phase
     *
     * @return the earliest detonation round, never before round 1
     */
    public static int earliestDetonationRound(int roundCount, GamePhase phase) {
        boolean weaponsPhaseStillToCome = phase.ordinal() <= GamePhase.FIRING.ordinal();
        return Math.max(1, weaponsPhaseStillToCome ? roundCount : roundCount + 1);
    }

    /**
     * @return the warhead's name for reports and toasts, such as "Santa Ana" or "Custom nuclear device"
     */
    public String warheadName() {
        return (weaponType == null) ? Messages.getString("NuclearWeapon.custom") : weaponType.getDisplayName();
    }
}
