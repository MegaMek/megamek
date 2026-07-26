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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import megamek.client.ratgenerator.FormationNamingConvention.DesignatorStyle;
import megamek.client.ratgenerator.FormationNamingConvention.Tier;
import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Verifies the {@code <formationNaming>} ruleset element: that every shipped faction file still
 * validates against the extended schema, and that the four root rulesets declare the conventions the
 * canon research settled on.
 *
 * <p>These tests read the shipped data files directly rather than going through
 * {@link Ruleset#loadData()}. The ratgenerator suite shares one JVM and
 * {@link megamek.common.Configuration}'s data directory is global mutable state (see
 * {@code ForceGeneratorTestFixture}), so pointing the loader at the production data set here would
 * leak into every other test class. Parsing the files in isolation tests the same two things that can
 * actually break - the schema and the authored data - without that coupling.</p>
 */
class FormationNamingConventionTest {

    /**
     * Located rather than hard-coded: the Gradle test working directory is not guaranteed to be the
     * one this path is relative to.
     */
    private static File factionRulesDir;

    // Echelon numbers after constant substitution. These repeat across traditions on purpose - 4 is an
    // Inner Sphere Company, a Clan Binary and a ComStar Choir - which is safe because a convention is
    // only ever read through the ruleset of the faction that declares it.
    private static final int ECHELON_LANCE = 3;
    private static final int ECHELON_COMPANY = 4;
    private static final int ECHELON_BATTALION = 5;
    private static final int ECHELON_REGIMENT = 6;
    private static final int ECHELON_STAR = 3;
    private static final int ECHELON_TRINARY = 5;
    private static final int ECHELON_CLUSTER = 6;
    private static final int ECHELON_GALAXY = 7;
    private static final int ECHELON_LEVEL_III = 5;
    private static final int ECHELON_LEVEL_IV = 6;

    @BeforeAll
    static void locateDataAndConstants() {
        for (String candidatePath : new String[] { "data/forcegenerator/faction_rules",
                                                   "megamek/data/forcegenerator/faction_rules",
                                                   "../megamek/data/forcegenerator/faction_rules" }) {
            File candidate = new File(candidatePath);
            if (candidate.isDirectory()) {
                factionRulesDir = candidate;
                break;
            }
        }
        assertNotNull(factionRulesDir, "could not locate the shipped faction_rules directory");
        // %COMPANY% and friends are resolved through the constants table, so the parser under test
        // needs it loaded. This is the one static the test does touch, and it is idempotent.
        Ruleset.loadConstants(new File(factionRulesDir, "constants.txt"));
    }

    /**
     * Guards the schema change: adding {@code <formationNaming>} must not invalidate any of the
     * shipped faction files, including the ~59 that do not declare it.
     */
    @Test
    void everyShippedFactionFileValidatesAgainstTheSchema() throws Exception {
        File schemaFile = new File(factionRulesDir, "formationRulesetSchema.xsd");
        assertTrue(schemaFile.exists(), "schema not found at " + schemaFile.getAbsolutePath());

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);

        File[] factionFiles = factionRulesDir.listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull(factionFiles, "no faction rule files found");
        assertTrue(factionFiles.length > 50,
              "expected the full shipped faction set, found " + factionFiles.length);

        List<String> failures = new ArrayList<>();
        for (File factionFile : factionFiles) {
            Validator validator = schema.newValidator();
            try {
                validator.validate(new StreamSource(factionFile));
            } catch (Exception validationFailure) {
                failures.add(factionFile.getName() + ": " + validationFailure.getMessage());
            }
        }
        assertTrue(failures.isEmpty(),
              "faction files failed schema validation:\n" + String.join("\n", failures));
    }

    /**
     * The player's Formation Naming Method has to reach the whole tree, so every Inner Sphere echelon
     * defers to it. None is qualified with its parent: a formation is identified by where it sits.
     */
    @Test
    void everyInnerSphereEchelonDefersToThePlayersNamingMethod() {
        FormationNamingConvention innerSphere = parseConvention("IS.xml");
        assertTier(innerSphere, ECHELON_REGIMENT, DesignatorStyle.ALPHABET, false);
        assertTier(innerSphere, ECHELON_BATTALION, DesignatorStyle.ALPHABET, false);
        assertTier(innerSphere, ECHELON_COMPANY, DesignatorStyle.ALPHABET, false);
        assertTier(innerSphere, ECHELON_LANCE, DesignatorStyle.ALPHABET, false);
    }

    @Test
    void peripheryRestatesTheInnerSphereConvention() {
        // Periphery is a root ruleset with no parent, so it has to declare rather than inherit.
        FormationNamingConvention periphery = parseConvention("Periphery.xml");
        assertTier(periphery, ECHELON_COMPANY, DesignatorStyle.ALPHABET, false);
        assertTier(periphery, ECHELON_REGIMENT, DesignatorStyle.ALPHABET, false);
    }

    @Test
    void clanUsesGreekAtGalaxyAndKeepsRulesetNamesBelowCluster() {
        FormationNamingConvention clan = parseConvention("CLAN.xml");
        // Galaxies are canonically "named for a letter of the Greek alphabet", so this is fixed rather
        // than left to the player's alphabet preference.
        assertTier(clan, ECHELON_GALAXY, DesignatorStyle.GREEK, false);
        // Clusters are numbered in canon ("11th Mechanized Cluster"), not lettered, so they
        // ignore the player's alphabet the same way galaxies do.
        assertTier(clan, ECHELON_CLUSTER, DesignatorStyle.NUMERIC_ORDINAL, false);
        // CLAN.xml already produces "Trinary [Battle]" and similar; a positional designator must not
        // overwrite it.
        assertTier(clan, ECHELON_TRINARY, DesignatorStyle.ENGINE, false);
        assertTier(clan, ECHELON_STAR, DesignatorStyle.ALPHABET, false);
    }

    @Test
    void comStarKeepsLevelNamesBecauseGreekSuffixesAreBranchNotSequence() {
        FormationNamingConvention comStar = parseConvention("CS.xml");
        // In the Com Guards a Greek letter denotes branch specialisation, so "IV-alpha" and "IV-beta"
        // are different kinds of formation rather than the first and second of a kind. Neither may be
        // rewritten into the other.
        assertTier(comStar, ECHELON_LEVEL_IV, DesignatorStyle.ENGINE, false);
        assertTier(comStar, ECHELON_LEVEL_III, DesignatorStyle.ENGINE, false);
    }

    @Test
    void comStarDeclaresNoRuleForDivisions() {
        // Divisions are not individually designated in standard Com Guard operations. Declaring no
        // tier is the way to say "keep the ruleset's name"; a fabricated default would be wrong.
        FormationNamingConvention comStar = parseConvention("CS.xml");
        assertNull(comStar.getTier(8), "ComStar divisions must not carry a generated designator");
    }

    @Test
    void wordOfBlakeDeclaresNoConventionSoItInheritsComStar() {
        FormationNamingConvention wordOfBlake = parseConvention("WoB.xml");
        assertTrue(wordOfBlake.isEmpty(),
              "WoB is expected to inherit the ComStar convention through parent=\"CS\","
              + " not to declare its own");
    }

    @Test
    void factionFilesWithNoNamingBlockParseToAnEmptyConvention() {
        // The overwhelming majority of the 63 shipped files declare nothing, and must stay loadable.
        FormationNamingConvention clanWolf = parseConvention("CW.xml");
        assertTrue(clanWolf.isEmpty());
        assertNull(clanWolf.getTier(ECHELON_GALAXY),
              "a file with no naming block must resolve nothing locally, leaving inheritance to Ruleset");
    }

    @Test
    void unrecognisedDesignatorFallsBackToKeepingTheRulesetName() throws Exception {
        FormationNamingConvention convention = parseConventionFromXml("""
              <formationNaming>
                  <tier echelon="4" designator="notAStyle"/>
              </formationNaming>
              """);
        Tier tier = convention.getTier(4);
        assertNotNull(tier, "a bad designator must not drop the tier entirely");
        assertEquals(DesignatorStyle.ENGINE, tier.designatorStyle(),
              "an unrecognised designator falls back to keeping the ruleset's own name");
    }

    @Test
    void tierWithAnUnparsableEchelonIsSkippedRatherThanFailingTheLoad() throws Exception {
        FormationNamingConvention convention = parseConventionFromXml("""
              <formationNaming>
                  <tier echelon="%NOT_A_CONSTANT%" designator="ordinal"/>
                  <tier echelon="%COMPANY%" designator="alphabet" qualifyWith="parent"/>
              </formationNaming>
              """);
        // One malformed tier must not cost the faction the rest of its convention.
        assertTier(convention, ECHELON_COMPANY, DesignatorStyle.ALPHABET, true);
    }

    private static FormationNamingConvention parseConvention(String fileName) {
        File factionFile = new File(factionRulesDir, fileName);
        assertTrue(factionFile.exists(), "missing faction file " + factionFile.getAbsolutePath());
        try {
            DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
            Document document = documentBuilder.parse(factionFile);
            NodeList children = document.getDocumentElement().getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                Node child = children.item(childIndex);
                if ("formationNaming".equals(child.getNodeName())) {
                    return FormationNamingConvention.createFromXml(child, fileName);
                }
            }
            return new FormationNamingConvention();
        } catch (Exception parseFailure) {
            throw new AssertionError("failed parsing " + fileName, parseFailure);
        }
    }

    private static FormationNamingConvention parseConventionFromXml(String xml) throws Exception {
        DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
        Document document = documentBuilder.parse(
              new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Element root = document.getDocumentElement();
        return FormationNamingConvention.createFromXml(root, "test");
    }

    private static void assertTier(FormationNamingConvention convention, int echelon,
          DesignatorStyle expectedStyle, boolean expectedQualifiedByParent) {
        Tier tier = convention.getTier(echelon);
        assertNotNull(tier, () -> "no naming tier declared for echelon " + echelon);
        assertEquals(expectedStyle, tier.designatorStyle(),
              () -> "designator style for echelon " + echelon);
        assertEquals(expectedQualifiedByParent, tier.qualifiedByParent(),
              () -> "parent qualification for echelon " + echelon);
    }
}
