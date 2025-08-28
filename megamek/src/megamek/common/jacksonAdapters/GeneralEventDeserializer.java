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

import java.io.File;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import megamek.server.scriptedEvents.TriggeredActiveEvent;

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

    private GeneralEventDeserializer() {}
}
