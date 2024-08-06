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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.server.scriptedevent.MessageScriptedEvent;
import megamek.server.trigger.Trigger;

import java.io.IOException;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class MessageDeserializer extends StdDeserializer<MessageScriptedEvent> {

    private static final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory());

    private static final String TEXT = "text";
    private static final String HEADER = "header";
    private static final String TRIGGER = "trigger";

    public MessageDeserializer() {
        this(null);
    }

    public MessageDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MessageScriptedEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        requireFields("MessageScriptedEvent", node, TEXT, HEADER, TRIGGER);

        String header = node.get(HEADER).textValue();
        String text = node.get(TEXT).textValue();
        Trigger trigger = TriggerDeserializer.parseNode(node.get(TRIGGER));
        return new MessageScriptedEvent(trigger, header, text);
    }
}
