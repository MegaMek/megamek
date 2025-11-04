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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.Briefcase;
import megamek.common.equipment.GroundObject;
import megamek.common.jacksonAdapters.dtos.GroundObjectInfo;

public class CarryableDeserializer extends StdDeserializer<GroundObjectInfo> {

    private static final String NAME = "name";
    private static final String WEIGHT = "weight";
    private static final String AT = "at";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String STATUS = "status";
    private static final String INVULNERABLE = "invulnerable";

    public CarryableDeserializer() {
        this(null);
    }

    public CarryableDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public GroundObjectInfo deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        requireFields("Carryable", node, NAME, WEIGHT);

        GroundObject groundObject = new Briefcase(); //TODO: Differentiate between Briefcase and Cargo

        groundObject.setName(node.get(NAME).textValue());
        groundObject.setTonnage(node.get(WEIGHT).doubleValue());
        assignStatus(groundObject, node);
        Coords position = readPosition(groundObject, node);
        return new GroundObjectInfo(groundObject, position);
    }

    private @Nullable Coords readPosition(GroundObject groundObject, JsonNode node) {
        try {
            if (node.has(AT)) {
                List<Integer> xyList = new ArrayList<>();
                node.get(AT).elements().forEachRemaining(n -> xyList.add(n.asInt()));
                return new Coords(xyList.get(0), xyList.get(1));

            } else if (node.has(X) || node.has(Y)) {
                requireFields("Carryable", node, X, Y);
                return new Coords(node.get(X).asInt(), node.get(Y).asInt());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal position information for carryable " + groundObject, e);
        }
        return null;
    }

    private void assignStatus(GroundObject groundObject, JsonNode node) {
        if (node.has(STATUS)) {
            JsonNode statusNode = node.get(STATUS);
            if (statusNode.isContainerNode() && statusNode.isArray()) {
                statusNode.iterator().forEachRemaining(n -> parseStatus(groundObject, n.textValue()));
            } else if (statusNode.isTextual()) {
                parseStatus(groundObject, statusNode.asText());
            }
        }
    }

    private void parseStatus(GroundObject groundObject, String statusString) {
        if (statusString.equals(INVULNERABLE)) {
            groundObject.setInvulnerable(true);
        } else {
            throw new IllegalArgumentException("Unknown status " + statusString);
        }
    }
}
