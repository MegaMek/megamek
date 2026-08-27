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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Verifies the {@code ifMotive} predicate matches ruleset file tokens ({@code Leg}, {@code Jump})
 * against a force's movement modes. Historically the predicate compared the file token against
 * {@link EntityMovementMode#toString()} - the localized display string ("Foot Infantry") - so every
 * {@code ifMotive} in the shipped rulesets was permanently false; both sides now normalize to enum
 * constant names.
 */
class RulesetNodeMotivePredicateTest {

    /** Parses a single XML element and wraps it as a {@link RulesetNode}. */
    private static RulesetNode nodeFromXml(String xml) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                                  .newDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return RulesetNode.createFromXml(document.getDocumentElement());
    }

    private static ForceDescriptor forceWithMotive(EntityMovementMode mode) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.getMovementModes().add(mode);
        return descriptor;
    }

    @Test
    void normalizeMotiveProperty_mapsFileTokensToEnumNames() {
        assertEquals("INF_LEG", RulesetNode.normalizeMotiveProperty("Leg"));
        assertEquals("INF_JUMP", RulesetNode.normalizeMotiveProperty("Jump"));
        assertEquals("!INF_LEG", RulesetNode.normalizeMotiveProperty("!Leg"));
        assertEquals("INF_LEG|INF_JUMP", RulesetNode.normalizeMotiveProperty("Leg|Jump"));
        assertEquals("TRACKED,INF_LEG", RulesetNode.normalizeMotiveProperty("Tracked,Leg"));
        // Enum-name input passes through the valueOf path unchanged.
        assertEquals("INF_LEG", RulesetNode.normalizeMotiveProperty("INF_LEG"));
    }

    @Test
    void normalizeMotiveProperty_keepsUnparsableTokensUnchanged() {
        // parseFromString maps garbage to NONE; the normalizer must not let that garbage match a
        // NONE movement mode, so the original token is kept (and can never match an enum name).
        assertEquals("NotAMotive", RulesetNode.normalizeMotiveProperty("NotAMotive"));
        assertEquals("", RulesetNode.normalizeMotiveProperty(""));
    }

    @Test
    void ifMotiveLeg_matchesLegInfantry_notJumpInfantry() throws Exception {
        RulesetNode node = nodeFromXml("<name ifMotive=\"Leg\">LI Regiment</name>");

        assertTrue(node.matches(forceWithMotive(EntityMovementMode.INF_LEG)),
              "ifMotive=\"Leg\" must match a leg-infantry force");
        assertFalse(node.matches(forceWithMotive(EntityMovementMode.INF_JUMP)),
              "ifMotive=\"Leg\" must not match a jump-infantry force");
    }

    @Test
    void ifMotiveJump_matchesJumpInfantry() throws Exception {
        RulesetNode node = nodeFromXml("<name ifMotive=\"Jump\">JI Regiment</name>");

        assertTrue(node.matches(forceWithMotive(EntityMovementMode.INF_JUMP)),
              "ifMotive=\"Jump\" must match a jump-infantry force");
        assertFalse(node.matches(forceWithMotive(EntityMovementMode.INF_LEG)),
              "ifMotive=\"Jump\" must not match a leg-infantry force");
    }

    @Test
    void ifMotiveNegation_rejectsMatchingMotive() throws Exception {
        RulesetNode node = nodeFromXml("<flags ifMotive=\"!Leg\">nonLegFlag</flags>");

        assertFalse(node.matches(forceWithMotive(EntityMovementMode.INF_LEG)),
              "ifMotive=\"!Leg\" must reject a leg-infantry force");
        assertTrue(node.matches(forceWithMotive(EntityMovementMode.INF_JUMP)),
              "ifMotive=\"!Leg\" must accept a jump-infantry force");
    }
}
