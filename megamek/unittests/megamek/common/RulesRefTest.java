/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RulesRef} parsing and formatting.
 */
class RulesRefTest {

    @Test
    void testParseSimpleReference() {
        Optional<RulesRef> result = RulesRef.parse("205, TM");

        assertTrue(result.isPresent());
        assertEquals("205", result.get().pageNumber());
        assertEquals("TM", result.get().bookAbbreviation());
        assertNull(result.get().rulesText());
    }

    @Test
    void testParseReferenceWithColonInAbbreviation() {
        Optional<RulesRef> result = RulesRef.parse("94, TO: AU&E");

        assertTrue(result.isPresent());
        assertEquals("94", result.get().pageNumber());
        assertEquals("TO: AU&E", result.get().bookAbbreviation());
    }

    @Test
    void testParsePageRange() {
        Optional<RulesRef> result = RulesRef.parse("204-206, IO");

        assertTrue(result.isPresent());
        assertEquals("204-206", result.get().pageNumber());
        assertEquals("IO", result.get().bookAbbreviation());
    }

    @Test
    void testParseWithWhitespace() {
        Optional<RulesRef> result = RulesRef.parse("  205  ,  TM  ");

        assertTrue(result.isPresent());
        assertEquals("205", result.get().pageNumber());
        assertEquals("TM", result.get().bookAbbreviation());
    }

    @Test
    void testParseNullReturnsEmpty() {
        Optional<RulesRef> result = RulesRef.parse(null);
        assertFalse(result.isPresent());
    }

    @Test
    void testParseEmptyStringReturnsEmpty() {
        Optional<RulesRef> result = RulesRef.parse("");
        assertFalse(result.isPresent());
    }

    @Test
    void testParseBlankStringReturnsEmpty() {
        Optional<RulesRef> result = RulesRef.parse("   ");
        assertFalse(result.isPresent());
    }

    @Test
    void testParseInvalidFormatReturnsEmpty() {
        // Missing comma separator
        Optional<RulesRef> result = RulesRef.parse("205 TM");
        assertFalse(result.isPresent());
    }

    @Test
    void testParseMultipleSingleReference() {
        List<RulesRef> results = RulesRef.parseMultiple("205, TM");

        assertEquals(1, results.size());
        assertEquals("205", results.get(0).pageNumber());
        assertEquals("TM", results.get(0).bookAbbreviation());
    }

    @Test
    void testParseMultipleTwoReferences() {
        List<RulesRef> results = RulesRef.parseMultiple("205, TM; 94, TO: AU&E");

        assertEquals(2, results.size());
        assertEquals("205", results.get(0).pageNumber());
        assertEquals("TM", results.get(0).bookAbbreviation());
        assertEquals("94", results.get(1).pageNumber());
        assertEquals("TO: AU&E", results.get(1).bookAbbreviation());
    }

    @Test
    void testParseMultipleNullReturnsEmptyList() {
        List<RulesRef> results = RulesRef.parseMultiple(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testParseMultipleEmptyStringReturnsEmptyList() {
        List<RulesRef> results = RulesRef.parseMultiple("");
        assertTrue(results.isEmpty());
    }

    @Test
    void testToLegacyString() {
        RulesRef ref = new RulesRef("205", "TM");
        assertEquals("205, TM", ref.toLegacyString());
    }

    @Test
    void testToLegacyStringWithComplexAbbreviation() {
        RulesRef ref = new RulesRef("94", "TO: AU&E");
        assertEquals("94, TO: AU&E", ref.toLegacyString());
    }

    @Test
    void testToMultipleLegacyString() {
        List<RulesRef> refs = List.of(
              new RulesRef("205", "TM"),
              new RulesRef("94", "TO: AU&E")
        );

        String result = RulesRef.toMultipleLegacyString(refs);
        assertEquals("205, TM; 94, TO: AU&E", result);
    }

    @Test
    void testToMultipleLegacyStringEmptyList() {
        String result = RulesRef.toMultipleLegacyString(List.of());
        assertEquals("", result);
    }

    @Test
    void testToMultipleLegacyStringNull() {
        String result = RulesRef.toMultipleLegacyString(null);
        assertEquals("", result);
    }

    @Test
    void testRoundTripParsing() {
        String original = "205, TM; 94, TO: AU&E";
        List<RulesRef> parsed = RulesRef.parseMultiple(original);
        String reconstructed = RulesRef.toMultipleLegacyString(parsed);

        assertEquals(original, reconstructed);
    }

    @Test
    void testToString() {
        RulesRef ref = new RulesRef("205", "TM");
        assertEquals("205, TM", ref.toString());
    }

    @Test
    void testConstructorWithRulesText() {
        RulesRef ref = new RulesRef("205", "TM", "Some rules text here");

        assertEquals("205", ref.pageNumber());
        assertEquals("TM", ref.bookAbbreviation());
        assertEquals("Some rules text here", ref.rulesText());
    }

    @Test
    void testConstructorWithoutRulesText() {
        RulesRef ref = new RulesRef("205", "TM");

        assertEquals("205", ref.pageNumber());
        assertEquals("TM", ref.bookAbbreviation());
        assertNull(ref.rulesText());
    }

    // ==================== YAML Serialization Tests ====================

    @Test
    void testToMap() {
        RulesRef ref = new RulesRef("205", "TM");
        Map<String, Object> map = ref.toMap();

        assertEquals("205", map.get(RulesRef.YAML_PAGE));
        assertEquals("TM", map.get(RulesRef.YAML_BOOK));
        assertFalse(map.containsKey(RulesRef.YAML_TEXT));
    }

    @Test
    void testToMapWithRulesText() {
        RulesRef ref = new RulesRef("205", "TM", "Some rules text");
        Map<String, Object> map = ref.toMap();

        assertEquals("205", map.get(RulesRef.YAML_PAGE));
        assertEquals("TM", map.get(RulesRef.YAML_BOOK));
        assertEquals("Some rules text", map.get(RulesRef.YAML_TEXT));
    }

    @Test
    void testFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("page", "205");
        map.put("book", "TM");

        Optional<RulesRef> result = RulesRef.fromMap(map);

        assertTrue(result.isPresent());
        assertEquals("205", result.get().pageNumber());
        assertEquals("TM", result.get().bookAbbreviation());
        assertNull(result.get().rulesText());
    }

    @Test
    void testFromMapWithRulesText() {
        Map<String, Object> map = new HashMap<>();
        map.put("page", "205");
        map.put("book", "TM");
        map.put("text", "Some rules text");

        Optional<RulesRef> result = RulesRef.fromMap(map);

        assertTrue(result.isPresent());
        assertEquals("205", result.get().pageNumber());
        assertEquals("TM", result.get().bookAbbreviation());
        assertEquals("Some rules text", result.get().rulesText());
    }

    @Test
    void testFromMapMissingPage() {
        Map<String, Object> map = new HashMap<>();
        map.put("book", "TM");

        Optional<RulesRef> result = RulesRef.fromMap(map);
        assertFalse(result.isPresent());
    }

    @Test
    void testFromMapMissingBook() {
        Map<String, Object> map = new HashMap<>();
        map.put("page", "205");

        Optional<RulesRef> result = RulesRef.fromMap(map);
        assertFalse(result.isPresent());
    }

    @Test
    void testFromMapNull() {
        Optional<RulesRef> result = RulesRef.fromMap(null);
        assertFalse(result.isPresent());
    }

    @Test
    void testFromMapNotAMap() {
        Optional<RulesRef> result = RulesRef.fromMap("not a map");
        assertFalse(result.isPresent());
    }

    @Test
    void testToMapList() {
        List<RulesRef> refs = List.of(
              new RulesRef("205", "TM"),
              new RulesRef("94", "TO: AU&E")
        );

        List<Map<String, Object>> result = RulesRef.toMapList(refs);

        assertEquals(2, result.size());
        assertEquals("205", result.get(0).get(RulesRef.YAML_PAGE));
        assertEquals("TM", result.get(0).get(RulesRef.YAML_BOOK));
        assertEquals("94", result.get(1).get(RulesRef.YAML_PAGE));
        assertEquals("TO: AU&E", result.get(1).get(RulesRef.YAML_BOOK));
    }

    @Test
    void testToMapListEmpty() {
        List<Map<String, Object>> result = RulesRef.toMapList(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void testToMapListNull() {
        List<Map<String, Object>> result = RulesRef.toMapList(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFromYamlDataLegacyString() {
        List<RulesRef> result = RulesRef.fromYamlData("205, TM; 94, TO: AU&E");

        assertEquals(2, result.size());
        assertEquals("205", result.get(0).pageNumber());
        assertEquals("TM", result.get(0).bookAbbreviation());
        assertEquals("94", result.get(1).pageNumber());
        assertEquals("TO: AU&E", result.get(1).bookAbbreviation());
    }

    @Test
    void testFromYamlDataStructuredList() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("page", "205");
        map1.put("book", "TM");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("page", "94");
        map2.put("book", "TO: AU&E");

        List<RulesRef> result = RulesRef.fromYamlData(List.of(map1, map2));

        assertEquals(2, result.size());
        assertEquals("205", result.get(0).pageNumber());
        assertEquals("TM", result.get(0).bookAbbreviation());
        assertEquals("94", result.get(1).pageNumber());
        assertEquals("TO: AU&E", result.get(1).bookAbbreviation());
    }

    @Test
    void testFromYamlDataSingleMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("page", "205");
        map.put("book", "TM");

        List<RulesRef> result = RulesRef.fromYamlData(map);

        assertEquals(1, result.size());
        assertEquals("205", result.get(0).pageNumber());
        assertEquals("TM", result.get(0).bookAbbreviation());
    }

    @Test
    void testFromYamlDataNull() {
        List<RulesRef> result = RulesRef.fromYamlData(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testYamlRoundTrip() {
        RulesRef original = new RulesRef("205", "TM", "Some rules text");

        Map<String, Object> map = original.toMap();
        Optional<RulesRef> reconstructed = RulesRef.fromMap(map);

        assertTrue(reconstructed.isPresent());
        assertEquals(original.pageNumber(), reconstructed.get().pageNumber());
        assertEquals(original.bookAbbreviation(), reconstructed.get().bookAbbreviation());
        assertEquals(original.rulesText(), reconstructed.get().rulesText());
    }
}
