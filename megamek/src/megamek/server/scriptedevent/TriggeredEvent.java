/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.scriptedevent;

import megamek.server.trigger.AbstractOneTimeTrigger;
import megamek.server.trigger.OnceTrigger;
import megamek.server.trigger.Trigger;

/**
 * This interface is implemented by pre-determined events that may happen over the course of a game, such as
 * game end or victory that do not change the game status but only examine it. These events are based
 * on a {@link Trigger} that defines when (and how often) they happen.
 * (Note: This has nothing to do with an event listener system.)
 */
public interface TriggeredEvent {

    /**
     * @return The Trigger that controls when this event happens. Note that it is up to the Trigger to
     * control if this event may happen multiple times. A subclass of {@link AbstractOneTimeTrigger}
     * or a {@link OnceTrigger} can be used to define a trigger that will never happen more than once.
     */
    Trigger trigger();
}
