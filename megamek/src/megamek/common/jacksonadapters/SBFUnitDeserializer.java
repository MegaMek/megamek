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
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.BTObject;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFMovementMode;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.strategicBattleSystems.SBFUnitConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static megamek.common.jacksonadapters.MMUReader.*;
import static megamek.common.jacksonadapters.SBFUnitSerializer.*;

public class SBFUnitDeserializer extends StdDeserializer<SBFUnit> {

    public SBFUnitDeserializer() {
        this(null);
    }

    public SBFUnitDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFUnit deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(TYPE) || !node.get(TYPE).textValue().equalsIgnoreCase(SBF_UNIT)) {
            throw new IllegalArgumentException("SBFUnitDeserializer: Wrong Deserializer chosen!");
        }

        requireFields(SBF_UNIT, node, GENERAL_NAME);

        SBFUnit unit = new SBFUnit();
        try {
            unit.setName(node.get(GENERAL_NAME).textValue());
            unit.setSkill(node.has(SKILL) ? node.get("skill").intValue() : 4);

            if (node.has(FORCE)) {
                unit.setForceString(node.get(FORCE).textValue());
            }

            if (node.has(ELEMENTS)) {
                // When the elements are given, read them and convert
                List<BTObject> elementsO = new MMUReader().read(node.get(ELEMENTS));
                if (elementsO.stream().anyMatch(o -> !(o instanceof AlphaStrikeElement))) {
                    //ERROR - how?
                    throw new IllegalArgumentException("SBFUnits may only contain Alpha Strike Elements!");
                }
                List<AlphaStrikeElement> elements = new ArrayList<>();
                elementsO.stream()
                        .map(o -> (AlphaStrikeElement) o)
                        .forEach(elements::add);

                //TODO: elements without skill?
                unit = new SBFUnitConverter(elements,
                        node.get(GENERAL_NAME).textValue(), elements, new DummyCalculationReport()).createSbfUnit();
            } else {
                // When no elements are given, read the unit's values
                // They will be ignored when elements are present!

                requireFields(SBF_UNIT, node, SIZE, SBF_TYPE, ARMOR, PV, MOVE, MOVE_MODE, TMM);

                unit.setSize(node.get(SIZE).intValue());
                unit.setType(SBFElementType.valueOf(node.get(SBF_TYPE).textValue()));
                unit.setArmor(node.get(ARMOR).intValue());
                unit.setPointValue(node.get(PV).intValue());
                unit.setTmm(node.get(TMM).intValue());
                unit.setMovement(node.get(MOVE).intValue());
                unit.setMovementMode(SBFMovementMode.valueOf(node.get(MOVE_MODE).textValue()));
                unit.setTrspMovement(unit.getMovement());
                unit.setTrspMovementMode(unit.getMovementMode());

                if (node.has(TRSP_MOVE)) {
                    unit.setTrspMovement(node.get(TRSP_MOVE).intValue());
                }
                if (node.has(TRSP_MOVE_MODE)) {
                    unit.setTrspMovementMode(SBFMovementMode.valueOf(node.get(TRSP_MOVE_MODE).textValue()));
                }

                if (node.has(DAMAGE)) {
                    unit.setDamage(ASDamageVector.parse(node.get(DAMAGE).textValue()));
                }

                if (node.has(SPECIALS)) {
                    String specials = node.get(SPECIALS).textValue();
                    specials = specials.replaceAll(" ", ""); // remove empty spaces
                    ASElementDeserializer.readSpecials(unit.getSpecialAbilities(), specials);
                }
            }
        } catch (NullPointerException exception) {
            throw new IllegalArgumentException("Missing element in SBFUnit definition!");
        }
        return unit;
    }
}