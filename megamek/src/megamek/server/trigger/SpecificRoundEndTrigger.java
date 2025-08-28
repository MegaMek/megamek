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

package megamek.server.trigger;

import megamek.common.game.IGame;

/**
 * This Trigger reacts at the end of the specified game round. This is more or less the same as a RoundStartTrigger of
 * the following round, but this can be used to trigger something without actually entering the following round just yet
 * (e.g. ending the game at the end of round 10 instead of at the beginning of round 11). Note that this Trigger can
 * react multiple times!
 */
public final class SpecificRoundEndTrigger implements Trigger {

    private final int gameRound;

    public SpecificRoundEndTrigger(int round) {
        gameRound = round;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return (event == TriggerSituation.ROUND_END) && (game.getCurrentRound() == gameRound);
    }

    @Override
    public String toString() {
        return "GameRound End: " + gameRound;
    }
}
