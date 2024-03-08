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
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.strategicBattleSystems.SBFUnitConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SBFUnitDeserializer extends StdDeserializer<SBFUnit> {

    private static final String ELEMENTS = "elements";

    public SBFUnitDeserializer() {
        this(null);
    }

    public SBFUnitDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFUnit deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(MMUReader.TYPE) || !node.get(MMUReader.TYPE).textValue().equalsIgnoreCase(MMUReader.SBF_UNIT)) {
            throw new IOException("SBFUnitDeserializer: Wrong Deserializer chosen!");
        }

        if (node.has(ELEMENTS)) {
            // When the elements are given, read them and convert
            List<AlphaStrikeElement> elements = new ArrayList<>();
            new MMUReader().read(node.get(ELEMENTS)).stream()
                    .filter(o -> o instanceof AlphaStrikeElement)
                    .map(o -> (AlphaStrikeElement) o)
                    .forEach(elements::add);

            //TODO: elements without skill?
            return new SBFUnitConverter(elements,
                    node.get("name").textValue(), elements, new DummyCalculationReport()).createSbfUnit();
        } else {
            //TODO read values without conversion
            return null;
        }
    }
}
