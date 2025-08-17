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

import static megamek.common.jacksonAdapters.MMUReader.requireFields;

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
     * Parses the given messages: node to return a list of one or more message events.
     *
     * @param messageNode a messages: node
     * @param basePath    a path to search image files in (e.g. scenario path)
     *
     * @return a list of parsed message events
     *
     * @throws IllegalArgumentException for illegal node combinations and other errors
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
