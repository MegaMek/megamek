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

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.*;
import megamek.common.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class CarryableDeserializer extends StdDeserializer<CarryableDeserializer.CarryableInfo> {

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

    /**
     * This is a temporary record for parsed info about a Carryable object (which do not have a position field)
     */
    @JsonRootName(value = "Carryable")
    @JsonDeserialize(using = CarryableDeserializer.class)
    public record CarryableInfo(ICarryable carryable, Coords position) { }

    @Override
    public CarryableInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        requireFields("Carryable", node, NAME, WEIGHT);

        Briefcase briefcase = new Briefcase();

        briefcase.setName(node.get(NAME).textValue());
        briefcase.setTonnage(node.get(WEIGHT).doubleValue());
        assignStatus(briefcase, node);
        Coords position = readPosition(briefcase, node);
        return new CarryableInfo(briefcase, position);
    }

    private @Nullable Coords readPosition(Briefcase briefcase, JsonNode node) {
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
            throw new IllegalArgumentException("Illegal position information for carryable " + briefcase, e);
        }
        return null;
    }

    private void assignStatus(Briefcase briefcase, JsonNode node) {
        if (node.has(STATUS)) {
            JsonNode statusNode = node.get(STATUS);
            if (statusNode.isContainerNode() && statusNode.isArray()) {
                statusNode.iterator().forEachRemaining(n -> parseStatus(briefcase, n.textValue()));
            } else if (statusNode.isTextual()) {
                parseStatus(briefcase, statusNode.asText());
            }
        }
    }

    private void parseStatus(Briefcase briefcase, String statusString) {
        switch (statusString) {
            case INVULNERABLE:
                briefcase.setInvulnerable(true);
                break;
            default:
                throw new IllegalArgumentException("Unknown status " + statusString);
        }
    }
}
