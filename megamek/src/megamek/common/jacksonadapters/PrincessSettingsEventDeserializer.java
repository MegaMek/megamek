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
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.MMMarkdownRenderer;
import megamek.server.scriptedevent.PrincessSettingsEvent;
import megamek.server.trigger.Trigger;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class PrincessSettingsEventDeserializer extends StdDeserializer<PrincessSettingsEvent> {

    private static final String TEXT = "text";
    private static final String HEADER = "header";
    private static final String TRIGGER = "trigger";
    private static final String IMAGE = "image";

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
    public static PrincessSettingsEvent parse(JsonNode eventNode) {
        // TODO: how to give incomplete settings to change only one setting
        // TODO: unify parsing with BotParser
        // TODO: update Lowering the Boom reverse messages
        requireFields("MessageScriptedEvent", eventNode, TEXT, HEADER, TRIGGER);

        String header = eventNode.get(HEADER).textValue();
        String text = eventNode.get(TEXT).textValue();
        // By default, expect this to be markdown and render to HTML; this preserves line breaks and paragraphs
        text = MMMarkdownRenderer.getRenderedHtml(text);
        Trigger trigger = TriggerDeserializer.parseNode(eventNode.get(TRIGGER));


        return new PrincessSettingsEvent(trigger, header, new PrincessSettingsBuilder());
    }
}
