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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Ground engineers belong with the ground formation, not under an aerospace wing. Several faction rulesets attached
 * an engineering infantry company to their aerospace rules, so generating an aerospace force produced an engineer
 * company with it (#9200). This checks that no shipped aerospace rule attaches engineers.
 *
 * <p>The Star League wing is the one deliberate exception: it carries a fixed division-support package (a
 * conventional fighter regiment and an engineer battalion).</p>
 */
class AerospaceEngineerPlacementTest {

    private static final Set<String> DELIBERATE_EXCEPTIONS = Set.of("SL.xml");

    private static File factionRulesDir;

    @BeforeAll
    static void locateData() {
        // Same search as FormationNamingConventionTest: the staged copy first, then mm-data itself.
        for (String candidatePath : new String[] { "data/forcegenerator/faction_rules",
                                                   "megamek/data/forcegenerator/faction_rules",
                                                   "../megamek/data/forcegenerator/faction_rules",
                                                   "../../mm-data/data/forcegenerator/faction_rules",
                                                   "../mm-data/data/forcegenerator/faction_rules" }) {
            File candidate = new File(candidatePath);
            if (candidate.isDirectory()) {
                factionRulesDir = candidate;
                break;
            }
        }
        assertNotNull(factionRulesDir, "could not locate the shipped faction_rules directory");
    }

    @Test
    void noAerospaceRuleAttachesGroundEngineers() throws Exception {
        File[] factionFiles = factionRulesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull(factionFiles, "no faction rule files found");

        List<String> offenders = new ArrayList<>();
        for (File factionFile : factionFiles) {
            if (DELIBERATE_EXCEPTIONS.contains(factionFile.getName())) {
                continue;
            }
            Document document = MMXMLUtility.newSafeDocumentBuilder().parse(factionFile);
            NodeList forceRules = document.getElementsByTagName("force");
            for (int i = 0; i < forceRules.getLength(); i++) {
                Element forceRule = (Element) forceRules.item(i);
                if (isAerospaceRule(forceRule) && attachesEngineers(forceRule)) {
                    offenders.add(factionFile.getName() + ": " + forceRule.getAttribute("eschName")
                          + " (" + forceRule.getAttribute("echelon") + ")");
                }
            }
        }
        assertTrue(offenders.isEmpty(),
              "aerospace rules attaching ground engineers:\n" + String.join("\n", offenders));
    }

    private static boolean isAerospaceRule(Element forceRule) {
        String unitTypes = forceRule.getAttribute("ifUnitType");
        return unitTypes.contains("AeroSpaceFighter") || unitTypes.contains("Conventional Fighter");
    }

    private static boolean attachesEngineers(Element forceRule) {
        NodeList attachedBlocks = forceRule.getElementsByTagName("attachedForces");
        for (int i = 0; i < attachedBlocks.getLength(); i++) {
            Element attachedBlock = (Element) attachedBlocks.item(i);
            if (hasEngineerEntry(attachedBlock, "option") || hasEngineerEntry(attachedBlock, "subforce")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEngineerEntry(Element attachedBlock, String tagName) {
        NodeList entries = attachedBlock.getElementsByTagName(tagName);
        for (int i = 0; i < entries.getLength(); i++) {
            if ("engineer".equals(((Element) entries.item(i)).getAttribute("role"))) {
                return true;
            }
        }
        return false;
    }
}
