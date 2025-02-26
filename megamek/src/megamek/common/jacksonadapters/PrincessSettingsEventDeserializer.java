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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.server.scriptedevent.PrincessSettingsEvent;
import megamek.server.trigger.Trigger;

import java.io.IOException;

import static megamek.common.jacksonadapters.BotParser.YAML_MAPPER;

public class PrincessSettingsEventDeserializer extends StdDeserializer<PrincessSettingsEvent> {

    private static final String PLAYER_NAME = "player";
    private static final String TRIGGER = "trigger";

    public PrincessSettingsEventDeserializer() {
        this(null);
    }

    public PrincessSettingsEventDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PrincessSettingsEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return parse(jp.getCodec().readTree(jp));
    }

    /**
     * Parses the given messages: node to return a list of one or more message events.

     * @param eventNode a messages: node
     * @return a list of parsed message events
     * @throws IllegalArgumentException for illegal node combinations and other errors
     */
    public static PrincessSettingsEvent parse(JsonNode eventNode) throws JsonProcessingException {
        Trigger trigger = TriggerDeserializer.parseNode(eventNode.get(TRIGGER));
        String playerName = eventNode.get(PLAYER_NAME).asText();
        PrincessSettingsBuilder builder = YAML_MAPPER.treeToValue(eventNode, PrincessSettingsBuilder.class);
        return new PrincessSettingsEvent(trigger, playerName, builder);
    }
}
