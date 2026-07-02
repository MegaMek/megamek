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
}
