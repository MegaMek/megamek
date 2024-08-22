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

import megamek.server.trigger.*;

/**
 * This class represents events that can be added programmatically or from MM scenarios to check
 * for game end. When the game ends and no victory conditions are met, the game is a draw.
 * Note: The Trigger must *not* be a one-time trigger.
 *
 * <P>Some examples for game end triggers:</P>
 * <P>To end the game after the 10th round:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(new SpecificRoundEndTrigger(10)));</code></P>
 * <P>To end the game after after the unit with ID 102 has been killed:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(new UnitKilledTrigger(102)));</code></P>
 * <P>To end the game after after the units with IDs 10 and 18 have both fled:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(new FledUnitsTrigger(null, 2, List.of(10, 18))));</code></P>
 *
 * <P>Adding multiple conditions to the game is equivalent to OR-ing them. Conditions can be ANDed or NOTed
 * as well using the AndTrigger and NotTrigger:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(
 *                 new AndTrigger(
 *                         new UnitKilledTrigger(2),
 *                         new FledUnitsTrigger(null, 1))));</code></P>
 * @param trigger The trigger that decides if victory has occurred
 * @see TriggeredEvent
 * @see AndTrigger
 * @see FledUnitsTrigger
 * @see ActiveUnitsTrigger
 */
public record GameEndTriggeredEvent(Trigger trigger) implements TriggeredEvent {

}
