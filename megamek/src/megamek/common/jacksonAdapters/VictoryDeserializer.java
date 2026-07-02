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

package megamek.common.jacksonAdapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.server.scriptedEvents.DrawTriggeredEvent;
import megamek.server.scriptedEvents.TriggeredEvent;
import megamek.server.scriptedEvents.VictoryTriggeredEvent;
import megamek.server.trigger.Trigger;

public final class VictoryDeserializer {

    private static final String TRIGGER = "trigger";
    private static final String MODIFY = "modify";
    private static final String ONLY_AT_END = "onlyatend";
    private static final String PLAYER = "player";

    /**
     * Parses the given single victory/draw node to return a TriggeredEvent that is either a DrawTriggeredEvent or
     * VictoryTriggeredEvent depending on the node's contents.
     *
     * @param victoryNode a node from the victory: definition in a scenario file
     *
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
     * Parses the given single victory node to return a DrawTriggeredEvent. It does not apply to a player, and it is
     * automatically game-ending, as explicit draw conditions are unnecessary when the game has already ended.
     *
     * @param victoryNode a node from the victory: definition in a scenario file
     *
     * @return the parsed event
     */
    private static TriggeredEvent parseDrawEvent(JsonNode victoryNode) {
        Trigger trigger = TriggerDeserializer.parseNode(victoryNode.get(TRIGGER));
        return new DrawTriggeredEvent(trigger, true);
    }

    /**
     * Parses the given single victory node to return a VictoryTriggeredEvent.
     *
     * @param victoryNode a node from the victory: definition in a scenario file
     * @param playerName  The name of the player (and thus, team) to which this victory applies
     *
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

    /**
     * Parses a YAML list of victory/draw definitions in the scenario {@code victory:} schema, as sent by the lobby
     * victory condition builder. Example:
     * <PRE>
     * - player: Player A
     *   trigger:
     *     type: victorypoints
     *     points: 5
     * - trigger:
     *     type: roundend
     *     round: 12
     * </PRE>
     *
     * @param victoryConditionsYaml The YAML text holding a list of victory definitions
     *
     * @return The parsed events, in order
     *
     * @throws IOException              When the text is not valid YAML
     * @throws IllegalArgumentException When a definition is not a valid victory condition
     */
    public static List<TriggeredEvent> parseList(String victoryConditionsYaml) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode listNode = yamlMapper.readTree(victoryConditionsYaml);
        if ((listNode == null) || !listNode.isArray()) {
            throw new IllegalArgumentException("Victory conditions must be a YAML list of victory definitions");
        }
        List<TriggeredEvent> events = new ArrayList<>();
        for (JsonNode victoryNode : listNode) {
            events.add(parse(victoryNode));
        }
        return events;
    }

    private VictoryDeserializer() {}
}
