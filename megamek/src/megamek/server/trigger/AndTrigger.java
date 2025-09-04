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

import java.util.Arrays;
import java.util.List;

import jakarta.annotation.Nonnull;
import megamek.common.game.IGame;

/**
 * This Trigger implements a logic AND for all its subtriggers, i.e. it triggers only at a moment when all its
 * subtriggers trigger at the same time.
 */
public record AndTrigger(List<Trigger> triggers) implements Trigger {

    public AndTrigger(Trigger... triggers) {
        this(Arrays.asList(triggers));
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return triggers.stream().allMatch(trigger -> trigger.isTriggered(game, event));
    }

    @Override
    @Nonnull
    public String toString() {
        List<String> triggerStrings = triggers.stream().map(Trigger::toString).toList();
        return "(" + String.join(" and ", triggerStrings) + ")";
    }
}
