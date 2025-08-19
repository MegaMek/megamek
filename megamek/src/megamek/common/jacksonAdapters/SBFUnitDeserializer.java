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

import static megamek.common.jacksonAdapters.MMUReader.*;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.ELEMENTS;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.PV;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.SBF_TYPE;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.TMM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFMovementMode;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.strategicBattleSystems.SBFUnitConverter;

/**
 * This Jackson deserializer reads an SBF Unit (part of a formation) from an MMU file. When the MMU file lists the
 * elements, these will be taken and converted to the SBFUnit (and any transients like damage applied). When the MMU
 * file doesn't list the elements, it must have the stats; then the SBFUnit will be constructed without the elements.
 */
public class SBFUnitDeserializer extends StdDeserializer<SBFUnit> {

    private static final String STATUS = "status";
    private static final String LEAD = "lead";
    private static final String COM = "com";

    public SBFUnitDeserializer() {
        this(null);
    }

    public SBFUnitDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFUnit deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(TYPE) || !node.get(TYPE).textValue().equalsIgnoreCase(SBF_UNIT)) {
            throw new IllegalArgumentException("SBFUnitDeserializer: Wrong Deserializer chosen!");
        }

        requireFields(SBF_UNIT, node, GENERAL_NAME);

        SBFUnit unit = new SBFUnit();
        try {
            unit.setName(node.get(GENERAL_NAME).textValue());
            unit.setSkill(node.has(SKILL) ? node.get("skill").intValue() : 4);

            if (node.has(ELEMENTS)) {
                // When the elements are given, read them and convert
                List<Object> elementsO = new MMUReader().read(node.get(ELEMENTS));
                if (elementsO.stream().anyMatch(o -> !(o instanceof AlphaStrikeElement))) {
                    // ERROR - how?
                    throw new IllegalArgumentException("SBFUnits may only contain Alpha Strike Elements!");
                }
                List<AlphaStrikeElement> elements = new ArrayList<>();
                elementsO.stream()
                      .map(o -> (AlphaStrikeElement) o)
                      .forEach(elements::add);

                // TODO: elements without skill?
                unit = new SBFUnitConverter(elements,
                      node.get(GENERAL_NAME).textValue(),
                      new DummyCalculationReport()).createSbfUnit();
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
            assignStatus(unit, node);
        } catch (NullPointerException exception) {
            throw new IllegalArgumentException("Missing element in SBFUnit definition!");
        }
        return unit;
    }

    private void assignStatus(SBFUnit unit, JsonNode node) {
        if (node.has(STATUS)) {
            JsonNode statusNode = node.get(STATUS);
            if (statusNode.isContainerNode() && statusNode.isArray()) {
                statusNode.iterator().forEachRemaining(n -> parseStatus(unit, n.textValue()));
            } else if (statusNode.isTextual()) {
                parseStatus(unit, statusNode.asText());
            }
        }
    }

    private void parseStatus(SBFUnit unit, String statusString) {
        switch (statusString) {
            case LEAD:
                unit.getSpecialAbilities().setSUA(BattleForceSUA.LEAD);
                break;
            case COM:
                unit.getSpecialAbilities().setSUA(BattleForceSUA.COM);
                break;
            default:
                throw new IllegalArgumentException("Unknown status " + statusString);
        }
    }
}
