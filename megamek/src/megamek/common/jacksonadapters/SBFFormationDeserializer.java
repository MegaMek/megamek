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

import static megamek.common.jacksonadapters.SBFFormationSerializer.UNITS;

public class SBFFormationDeserializer extends StdDeserializer<SBFFormation> {

    public SBFFormationDeserializer() {
        this(null);
    }

    public SBFFormationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFFormation deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(MMUReader.TYPE) || !node.get(MMUReader.TYPE).textValue().equalsIgnoreCase(MMUReader.SBF_FORMATION)) {
            throw new IOException("SBFFormationDeserializer: Wrong Deserializer chosen!");
        }

        if (node.has(UNITS)) {
            SBFFormation formation = new SBFFormation();
            new MMUReader().read(node.get(UNITS)).stream()
                    .filter(o -> o instanceof SBFUnit)
                    .map(o -> (SBFUnit) o)
                    .forEach(formation::addUnit);
            formation.setName(node.get(MMUReader.GENERAL_NAME).textValue());
            SBFFormationConverter.calculateStatsFromUnits(formation);
            return formation;
        } else {
            return null;
        }
    }
}