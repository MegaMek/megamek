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
package megamek.common.jacksonadapters;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.server.scriptedevent.DrawTriggeredEvent;
import megamek.server.scriptedevent.TriggeredEvent;
import megamek.server.scriptedevent.VictoryTriggeredEvent;
import megamek.server.trigger.Trigger;

import java.util.List;

public final class VictoryDeserializer {

    private static final String TRIGGER = "trigger";
    private static final String MODIFY = "modify";
    private static final String ONLY_AT_END = "onlyatend";
    private static final String PLAYER = "player";

    /**
     * Parses the given single victory/draw node to return a TriggeredEvent that is either a
     * DrawTriggeredEvent or VictoryTriggeredEvent depending on the node's contents.

     * @param victoryNode a node from the victory: definition in a scenario file
     * @return the parsed event
     */
    public static TriggeredEvent parse(JsonNode victoryNode) {
        if (victoryNode.has(PLAYER)) {
            return parse(victoryNode, victoryNode.get(PLAYER).asText());
        } else {
            return parseDrawEvent(victoryNode);
        }
    }

    /**
     * Parses the given single victory node to return a DrawTriggeredEvent. It does not apply to a player
     * and it is automatically game-ending, as explicit draw conditions are unnecessary when the game has
     * already ended.
     *
     * @param victoryNode a node from the victory: definition in a scenario file
     * @return the parsed event
     */
    private static TriggeredEvent parseDrawEvent(JsonNode victoryNode) {
        Trigger trigger = TriggerDeserializer.parseNode(victoryNode.get(TRIGGER));
        return new DrawTriggeredEvent(trigger, true);
    }

    /**
     * Parses the given single victory node to return a VictoryTriggeredEvent.
     * @param victoryNode a node from the victory: definition in a scenario file
     * @param playerName The name of the player (and thus, team) to which this victory applies
     * @return the parsed event
     */
    public static VictoryTriggeredEvent parse(JsonNode victoryNode, String playerName) {
        Trigger trigger = TriggerDeserializer.parseNode(victoryNode.get(TRIGGER));
        return new VictoryTriggeredEvent(trigger, isGameEnding(victoryNode), playerName);
    }

    private static boolean isGameEnding(JsonNode victoryNode) {
        if (victoryNode.has(MODIFY)) {
            List<String> modifiers = TriggerDeserializer.parseArrayOrSingleNode(victoryNode.get(MODIFY), ONLY_AT_END);
            return !modifiers.contains(ONLY_AT_END);
        }
        return true;
    }

    private VictoryDeserializer() { }
}
