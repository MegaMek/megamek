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
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.IOException;

import static megamek.common.jacksonadapters.MMUReader.*;
import static megamek.common.jacksonadapters.SBFFormationSerializer.UNITS;

/**
 * This Jackson deserializer reads an SBFFormation from an MMU file. As a formation must have its units
 * fully listed and the stats can then be converted, any given stats in the MMU file are (currently)
 * ignored.
 */
public class SBFFormationDeserializer extends StdDeserializer<SBFFormation> {

    public SBFFormationDeserializer() {
        this(null);
    }

    public SBFFormationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFFormation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(TYPE) || !node.get(TYPE).textValue().equalsIgnoreCase(SBF_FORMATION)) {
            throw new IllegalArgumentException("SBFFormationDeserializer: Wrong Deserializer chosen!");
        }

        requireFields(SBF_FORMATION, node, GENERAL_NAME, UNITS);
        SBFFormation formation = new SBFFormation();
        formation.setName(node.get(GENERAL_NAME).textValue());
        new MMUReader().read(node.get(UNITS)).stream()
                .filter(o -> o instanceof SBFUnit)
                .map(o -> (SBFUnit) o)
                .forEach(formation::addUnit);
        SBFFormationConverter.calculateStatsFromUnits(formation);
        return formation;
    }
}