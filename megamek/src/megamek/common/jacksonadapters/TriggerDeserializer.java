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
import megamek.server.trigger.*;

import java.io.IOException;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class TriggerDeserializer extends StdDeserializer<Trigger> {

    private static final String TYPE = "type";
    private static final String TYPE_GAMESTART = "gamestart";
    private static final String TYPE_ROUNDSTART = "roundstart";
    private static final String TYPE_ROUNDEND = "roundend";
    private static final String ROUND = "round";

    public TriggerDeserializer() {
        this(null);
    }

    public TriggerDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Trigger deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return parseNode(node);
    }

    public static Trigger parseNode(JsonNode node) {
        requireFields("Trigger", node, TYPE);

        String type = node.get(TYPE).asText();
        return switch (type) {
            case TYPE_GAMESTART -> new GameStartTrigger();
            case TYPE_ROUNDSTART -> parseRoundStartTrigger(node);
            case TYPE_ROUNDEND -> parseRoundEndTrigger(node);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private static Trigger parseRoundStartTrigger(JsonNode triggerNode) {
        if (triggerNode.has(ROUND)) {
            return new SpecificRoundStartTrigger(triggerNode.get(ROUND).asInt());
        } else {
            return new AnyRoundStartTrigger();
        }
    }

    private static Trigger parseRoundEndTrigger(JsonNode triggerNode) {
        if (triggerNode.has(ROUND)) {
            return new SpecificRoundEndTrigger(triggerNode.get(ROUND).asInt());
        } else {
            return new AnyRoundEndTrigger();
        }
    }
}
