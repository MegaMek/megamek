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

package megamek.server.scriptedEvent;

import megamek.server.trigger.AbstractOneTimeTrigger;
import megamek.server.trigger.OnceTrigger;
import megamek.server.trigger.Trigger;

/**
 * This interface is implemented by pre-determined events that may happen over the course of a game, such as game end or
 * victory that do not change the game status but only examine it. These events are based on a {@link Trigger} that
 * defines when (and how often) they happen. (Note: This has nothing to do with an event listener system.)
 */
public interface TriggeredEvent {

    /**
     * @return The Trigger that controls when this event happens. Note that it is up to the Trigger to control if this
     *       event may happen multiple times. A subclass of {@link AbstractOneTimeTrigger} or a {@link OnceTrigger} can
     *       be used to define a trigger that will never happen more than once.
     */
    Trigger trigger();

    default boolean isGameEnding() {
        return false;
    }
}
