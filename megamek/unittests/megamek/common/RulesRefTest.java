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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.common.weapons.lrms.innerSphere.ISLRM15;
import org.junit.jupiter.api.Test;

class RulesRefTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesBookUsingItsRuntimeSourcebookAbbreviation() throws JsonProcessingException {
        RulesRef rulesRef = new RulesRef(SourceBookCode.TO_AUE, 121);

        assertEquals("{\"book\":\"TO:AUE\",\"page\":121}", objectMapper.writeValueAsString(rulesRef));
    }

    @Test
    void serializesMissingPageAsNull() throws JsonProcessingException {
        RulesRef rulesRef = new RulesRef(SourceBookCode.SHRAPNEL_5, null);

        assertEquals("{\"book\":\"Shrap05\",\"page\":null}", objectMapper.writeValueAsString(rulesRef));
    }

    @Test
    void formatsAReferenceForTextExports() {
        assertEquals("205, TM", new RulesRef(SourceBookCode.TM, 205).toString());
        assertEquals("Gothic", new RulesRef(SourceBookCode.GOTHIC, null).toString());
    }

    @Test
    void formatsReferencesForDisplay() {
        List<RulesRef> references = List.of(
              new RulesRef(SourceBookCode.TM, 205),
              new RulesRef(SourceBookCode.CORE, 111),
              new RulesRef(SourceBookCode.GOTHIC, null));

        assertEquals("TM, 205; Core, 111; Gothic", RulesRef.formatForDisplay(references));
        assertEquals("", RulesRef.formatForDisplay(List.of()));
    }

    @Test
    void exportsTheSourcebookAbbreviationInsteadOfTheEnumName() {
        assertEquals(
              java.util.Map.of("book", "TO:AUE", "page", 121),
              new RulesRef(SourceBookCode.TO_AUE, 121).toYamlData());
    }

    @Test
    void equipmentYamlDataExportsEachReferenceAsBookAndPage() {
        Object exportedRulesRefs = new MultiReferenceEquipment().getYamlData().get("rulesRefs");

        assertEquals(List.of(
              java.util.Map.of("book", "TO:AUE", "page", 121),
              java.util.Map.of("book", "Core", "page", 111)), exportedRulesRefs);
    }

    private static final class MultiReferenceEquipment extends ISLRM15 {
        private MultiReferenceEquipment() {
            rulesRefs = rulesRefs(
                  new RulesRef(SourceBookCode.TO_AUE, 121),
                  new RulesRef(SourceBookCode.CORE, 111));
        }
    }
}
