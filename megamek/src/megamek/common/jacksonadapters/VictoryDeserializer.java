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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.server.scriptedevent.VictoryTriggeredEvent;
import megamek.server.trigger.Trigger;

import java.io.IOException;
import java.util.List;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class VictoryDeserializer extends StdDeserializer<VictoryTriggeredEvent> {

    private static final String TEXT = "text";
    private static final String HEADER = "header";
    private static final String TRIGGER = "trigger";
    private static final String IMAGE = "image";
    private static final String MODIFY = "modify";
    private static final String ONLY_AT_END = "onlyatend";

    public VictoryDeserializer() {
        this(null);
    }

    public VictoryDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public VictoryTriggeredEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return parse(jp.getCodec().readTree(jp));
    }

    /**
     * Parses the given map: or maps: node to return a list of one or more boards (the list should
     * ideally never be empty, an exception being thrown instead). Board files are tried first
     * in the given basePath; if not found there, MM's data/boards/ is tried instead.

     * @param victoryNode a map: or maps: node from a YAML definition file
     * @return a list of parsed boards
     * @throws IllegalArgumentException for illegal node combinations and other errors
     */
    public static VictoryTriggeredEvent parse(JsonNode victoryNode) {
        requireFields("MessageScriptedEvent", victoryNode, TRIGGER);

        Trigger trigger = TriggerDeserializer.parseNode(victoryNode.get(TRIGGER));

        boolean isGameEnding = true;
        if (victoryNode.has(MODIFY)) {
            List<String> modifiers = TriggerDeserializer.parseArrayOrSingleNode(victoryNode.get(MODIFY), ONLY_AT_END);
            if (modifiers.contains(ONLY_AT_END)) {
                isGameEnding = false;
            }
        }
        return new VictoryTriggeredEvent(trigger, isGameEnding);
    }
}
