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

import static megamek.common.jacksonAdapters.BotParser.YAML_MAPPER;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.server.scriptedevent.PrincessSettingsEvent;
import megamek.server.trigger.Trigger;

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
     *
     * @param eventNode a messages: node
     *
     * @return a list of parsed message events
     *
     * @throws IllegalArgumentException for illegal node combinations and other errors
     */
    public static PrincessSettingsEvent parse(JsonNode eventNode) throws JsonProcessingException {
        Trigger trigger = TriggerDeserializer.parseNode(eventNode.get(TRIGGER));
        String playerName = eventNode.get(PLAYER_NAME).asText();
        PrincessSettingsBuilder builder = YAML_MAPPER.treeToValue(eventNode, PrincessSettingsBuilder.class);
        return new PrincessSettingsEvent(trigger, playerName, builder);
    }
}
