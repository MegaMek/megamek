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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import megamek.common.jacksonadapters.MessageDeserializer;
import megamek.server.IGameManager;
import megamek.server.trigger.Trigger;

/**
 * This interface is implemented by pre-determined events that may happen over the course of a game, such as
 * story messages or board changes. Much like WeaponHandlers, ScriptedGameManagerEvents are part of the
 * GameManager's code and must fully work out whatever the event brings, including sending the necessary packets.
 * ScriptedEvents are based on a {@link Trigger} that defines when  (and how often) they happen. When they
 * happen, the {@link #process(IGameManager)} method is called to determine the results.
 * (Note: This has nothing to do with an event listener system.)
 */
@JsonDeserialize(using = MessageDeserializer.class)
public interface TriggeredActiveEvent extends TriggeredEvent {

    /**
     * This method is called when the Trigger of this event is satisfied (returns true). This method must
     * fully work out whatever the event brings, including sending the necessary packets. The code of
     * this method is essentially part of the GameManager's code.
     */
    void process(IGameManager gameManager);
}
