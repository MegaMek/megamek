/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.board.Coords;
import megamek.common.jacksonAdapters.ObjectiveDeserializer.ObjectiveInfo;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveScoringScheme.HoldCounting;
import megamek.common.equipment.ObjectiveScoringScheme.SchemePreset;
import org.junit.jupiter.api.Test;

class ObjectiveDeserializerTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private JsonNode parseYaml(String yaml) throws Exception {
        return YAML_MAPPER.readTree(yaml);
    }

    @Test
    void testFullObjective() throws Exception {
        JsonNode node = parseYaml("""
              name: MacGuffin
              at: [ 20, 12 ]
              controlRadius: 2
              vp: 3
              variants: [ potential, fragile, mobile ]
              """);

        ObjectiveInfo objectiveInfo = ObjectiveDeserializer.parse(node);

        assertEquals("MacGuffin", objectiveInfo.marker().generalName());
        assertEquals(new Coords(20, 12), objectiveInfo.position());
        assertEquals(2, objectiveInfo.marker().getControlRadius());
        assertEquals(3, objectiveInfo.marker().getVictoryPointValue());
        assertTrue(objectiveInfo.marker().isPotential());
        assertTrue(objectiveInfo.marker().isFragile());
        assertTrue(objectiveInfo.marker().isMobile());
        assertFalse(objectiveInfo.marker().isFalseObjective());
    }

    @Test
    void testMinimalObjectiveWithDefaults() throws Exception {
        JsonNode node = parseYaml("""
              name: Left Counter
              x: 6
              y: 5
              """);

        ObjectiveInfo objectiveInfo = ObjectiveDeserializer.parse(node);

        assertEquals(new Coords(6, 5), objectiveInfo.position());
        assertEquals(0, objectiveInfo.marker().getControlRadius());
        assertEquals(1, objectiveInfo.marker().getVictoryPointValue());
        assertFalse(objectiveInfo.marker().isPotential());
        assertFalse(objectiveInfo.marker().isFragile());
        assertFalse(objectiveInfo.marker().isMobile());
        assertFalse(objectiveInfo.marker().isFalseObjective());
    }

    @Test
    void testDestructibleFalseMakesObjectiveIndestructible() throws Exception {
        JsonNode node = parseYaml("""
              name: Protected Counter
              at: [ 3, 4 ]
              destructible: false
              """);

        ObjectiveInfo objectiveInfo = ObjectiveDeserializer.parse(node);

        assertTrue(objectiveInfo.marker().isInvulnerable());
    }

    @Test
    void testObjectivesAreDestructibleByDefault() throws Exception {
        // RAW: objectives are destroyed with their building unless the mission states otherwise
        JsonNode node = parseYaml("""
              name: Standard Counter
              at: [ 3, 4 ]
              """);

        ObjectiveInfo objectiveInfo = ObjectiveDeserializer.parse(node);

        assertFalse(objectiveInfo.marker().isInvulnerable());
    }

    @Test
    void testPotentialAndFalseCombinationThrows() throws Exception {
        JsonNode node = parseYaml("""
              name: Contradiction
              at: [ 3, 4 ]
              variants: [ potential, "false" ]
              """);

        assertThrows(IllegalArgumentException.class, () -> ObjectiveDeserializer.parse(node));
    }

    @Test
    void testFalseVariant() throws Exception {
        JsonNode node = parseYaml("""
              name: Decoy
              at: [ 3, 4 ]
              variants: [ "false" ]
              """);

        ObjectiveInfo objectiveInfo = ObjectiveDeserializer.parse(node);

        assertTrue(objectiveInfo.marker().isFalseObjective());
    }

    @Test
    void testMissingPositionThrows() throws Exception {
        JsonNode node = parseYaml("name: Nowhere");

        assertThrows(IllegalArgumentException.class, () -> ObjectiveDeserializer.parse(node));
    }

    @Test
    void testIllegalControlRadiusThrows() throws Exception {
        JsonNode node = parseYaml("""
              name: Too Big
              at: [ 1, 1 ]
              controlRadius: 5
              """);

        assertThrows(IllegalArgumentException.class, () -> ObjectiveDeserializer.parse(node));
    }

    @Test
    void testUnknownVariantThrows() throws Exception {
        JsonNode node = parseYaml("""
              name: Odd One
              at: [ 1, 1 ]
              variants: [ invisible ]
              """);

        assertThrows(IllegalArgumentException.class, () -> ObjectiveDeserializer.parse(node));
    }

    // --- Scoring scheme parsing (part 4: scenarios define the mission) ---

    @Test
    void testObjectiveWithoutASchemeKeepsStandard() throws Exception {
        ObjectiveInfo info = ObjectiveDeserializer.parse(parseYaml("""
              name: Crossroads
              at: [ 4, 4 ]
              """));
        assertEquals(SchemePreset.STANDARD, info.marker().getScoringScheme().getPreset());
    }

    @Test
    void testHoldSchemeParsesTurnsAndCounting() throws Exception {
        ObjectiveInfo info = ObjectiveDeserializer.parse(parseYaml("""
              name: Relay Station
              at: [ 4, 4 ]
              scheme: hold
              turns: 5
              counting: cumulative
              """));
        ObjectiveScoringScheme scheme = info.marker().getScoringScheme();
        assertEquals(SchemePreset.HOLD, scheme.getPreset());
        assertEquals(5, scheme.getThreshold());
        assertEquals(HoldCounting.CUMULATIVE, scheme.getHoldCounting());
    }

    @Test
    void testDefendSchemeParsesGripAndDrain() throws Exception {
        ObjectiveInfo info = ObjectiveDeserializer.parse(parseYaml("""
              name: Supply Dump
              at: [ 4, 4 ]
              scheme: defend
              grip: 3
              drain: 2
              """));
        ObjectiveScoringScheme scheme = info.marker().getScoringScheme();
        assertEquals(SchemePreset.DEFEND, scheme.getPreset());
        assertEquals(3, scheme.getThreshold());
        assertEquals(2, scheme.getRatePerTurn());
    }

    @Test
    void testCaptureSchemeParsesPointsAndRate() throws Exception {
        ObjectiveInfo info = ObjectiveDeserializer.parse(parseYaml("""
              name: Comm Tower
              at: [ 4, 4 ]
              scheme: capture
              points: 4
              rate: 2
              """));
        ObjectiveScoringScheme scheme = info.marker().getScoringScheme();
        assertEquals(SchemePreset.CAPTURE, scheme.getPreset());
        assertEquals(4, scheme.getThreshold());
        assertEquals(2, scheme.getRatePerTurn());
    }

    @Test
    void testUnknownSchemeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ObjectiveDeserializer.parse(parseYaml("""
              name: Bad Point
              at: [ 4, 4 ]
              scheme: conquer
              """)));
    }

    @Test
    void testANullSchemeNumberFallsBackToItsDefault() throws Exception {
        // a key written with no value is a null node: it is present, but reading it as a number gives 0,
        // which would secure a Hold point the moment the first End Phase ran
        ObjectiveInfo info = ObjectiveDeserializer.parse(parseYaml("""
              name: Relay Station
              at: [ 4, 4 ]
              scheme: hold
              turns:
              """));
        assertEquals(1, info.marker().getScoringScheme().getThreshold(),
              "an empty turns: must fall back to the default, not to zero");
    }

    @Test
    void testANullCountingModeFallsBackToConsecutive() throws Exception {
        ObjectiveInfo info = ObjectiveDeserializer.parse(parseYaml("""
              name: Relay Station
              at: [ 4, 4 ]
              scheme: hold
              turns: 3
              counting:
              """));
        assertEquals(HoldCounting.CONSECUTIVE, info.marker().getScoringScheme().getHoldCounting());
    }
}
