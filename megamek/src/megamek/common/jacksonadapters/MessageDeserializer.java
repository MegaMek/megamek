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

import static megamek.common.jacksonadapters.MMUReader.requireFields;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import megamek.client.ui.MMMarkdownRenderer;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.server.scriptedevent.MessageTriggeredActiveEvent;
import megamek.server.trigger.Trigger;

public class MessageDeserializer extends StdDeserializer<MessageTriggeredActiveEvent> {
    private static final MMLogger logger = MMLogger.create(MessageDeserializer.class);

    private static final String TEXT = "text";
    private static final String HEADER = "header";
    private static final String TRIGGER = "trigger";
    private static final String IMAGE = "image";

    public MessageDeserializer() {
        this(null);
    }

    public MessageDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MessageTriggeredActiveEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return parse(jp.getCodec().readTree(jp), new File(""));
    }

    /**
     * Parses the given map: or maps: node to return a list of one or more boards
     * (the list should
     * ideally never be empty, an exception being thrown instead). Board files are
     * tried first
     * in the given basePath; if not found there, MM's data/boards/ is tried
     * instead.
     *
     * @param messageNode a map: or maps: node from a YAML definition file
     * @param basePath    a path to search board files in (e.g. scenario path)
     * @return a list of parsed boards
     * @throws IllegalArgumentException for illegal node combinations and other
     *                                  errors
     */
    public static MessageTriggeredActiveEvent parse(JsonNode messageNode, File basePath) {
        requireFields("MessageScriptedEvent", messageNode, TEXT, HEADER, TRIGGER);

        String header = messageNode.get(HEADER).textValue();
        String text = messageNode.get(TEXT).textValue();
        // By default, expect this to be markdown and render to HTML; this preserves
        // line breaks and paragraphs
        text = MMMarkdownRenderer.getRenderedHtml(text);
        Trigger trigger = TriggerDeserializer.parseNode(messageNode.get(TRIGGER));

        Image image = null;
        if (messageNode.has(IMAGE)) {
            try {
                image = loadImage(messageNode.get(IMAGE).asText(), basePath);
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }
        }

        return new MessageTriggeredActiveEvent(trigger, header, text, image);
    }

    @Nullable
    private static Image loadImage(String fileName, File basePath) throws IOException {
        File imageFile = new File(basePath, fileName);
        if (!imageFile.exists()) {
            imageFile = new File(Configuration.imagesDir(), fileName);
            if (!imageFile.exists()) {
                throw new IllegalArgumentException("Image file does not exist: " + imageFile + " in " + basePath);
            }
        }
        return ImageIO.read(imageFile);
    }
}
