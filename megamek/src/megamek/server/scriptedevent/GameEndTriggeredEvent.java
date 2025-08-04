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

package megamek.server.scriptedevent;

import megamek.server.trigger.ActiveUnitsTrigger;
import megamek.server.trigger.AndTrigger;
import megamek.server.trigger.FledUnitsTrigger;
import megamek.server.trigger.Trigger;

/**
 * This class represents events that can be added programmatically or from MM scenarios to check for game end. When the
 * game ends and no victory conditions are met, the game is a draw. Note: The Trigger must *not* be a one-time trigger.
 *
 * <P>Some examples for game end triggers:</P>
 * <P>To end the game after the 10th round:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(new SpecificRoundEndTrigger(10)));</code></P>
 * <P>To end the game after after the unit with ID 102 has been killed:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(new UnitKilledTrigger(102)));</code></P>
 * <P>To end the game after after the units with IDs 10 and 18 have both fled:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(new FledUnitsTrigger(null, 2, List.of(10,
 * 18))));</code></P>
 *
 * <P>Adding multiple conditions to the game is equivalent to OR-ing them. Conditions can be ANDed or NOTed
 * as well using the AndTrigger and NotTrigger:</P>
 * <P><code>game.addScriptedEvent(new GameEndTriggeredEvent(
 * new AndTrigger( new UnitKilledTrigger(2), new FledUnitsTrigger(null, 1))));</code></P>
 *
 * @param trigger The trigger that decides if victory has occurred
 *
 * @see TriggeredEvent
 * @see AndTrigger
 * @see FledUnitsTrigger
 * @see ActiveUnitsTrigger
 */
public record GameEndTriggeredEvent(Trigger trigger) implements TriggeredEvent {

    @Override
    public String toString() {
        return "GameEnd: " + trigger;
    }

    @Override
    public boolean isGameEnding() {
        return true;
    }
}
