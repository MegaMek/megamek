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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import megamek.server.scriptedevent.TriggeredActiveEvent;

import java.io.File;

public class GeneralEventDeserializer {

    private static final String TYPE = "type";
    private static final String EVENT = "event";
    private static final String PRINCESS_SETTINGS = "princesssettings";
    private static final String MESSAGE = "message";


    public static TriggeredActiveEvent parse(JsonNode node, File basePath) throws JsonProcessingException {
        String type = node.get(TYPE).asText();
        JsonNode eventNode = node.get(EVENT);
        return switch (type) {
            case PRINCESS_SETTINGS -> PrincessSettingsEventDeserializer.parse(eventNode);
            case MESSAGE -> MessageDeserializer.parse(eventNode, basePath);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }

    private GeneralEventDeserializer() { }
}
