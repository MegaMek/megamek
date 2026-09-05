/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;

import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class FactionRecordTest {

    private static final int TEST_ERA = 3075;
    private static final String FIVE_RATINGS = "F,D,C,B,A";
    private static final int RATING_F = 0;
    private static final int RATING_A = 4;

    private static DocumentBuilder db;
    private final HashMap<String, FactionRecord> factions = new HashMap<>();

    @BeforeEach
    void setUp() {
        try {
            db = MMXMLUtility.newSafeDocumentBuilder();
        } catch (Exception ignored) {

        }
    }

    /**
     * <faction key='RA' name='Raven Alliance' minor='false' clan='true' periphery= 'false'>
     * <years>3083-</years>
     * <ratingLevels>Provisional Garrison,Solahma,Second Line,Front
     * Line,Keshik</ratingLevels>
     * <parentFaction>CLAN.IS</parentFaction>
     * </faction>
     */
    void createRavenAlliance() throws ParseException {
        // Set up faction record
        FactionRecord fr = new FactionRecord("RA", "Raven Alliance");
        fr.setClan(true);
        fr.setYears("3083-");
        fr.setRatings("Provisional Garrison,Solahma,Second Line,Front Line,Keshik");
        fr.setParentFactions("CLAN.IS");
        factions.put("RA", fr);
    }

    @Test
    void testLoadEraRavenAlliance3151AeroSpaceFighters() throws IOException, SAXException, ParseException {
        createRavenAlliance();

        // Taken from 3150 xml
        String xmlString = String.join("\n",
              "<?xml version='1.0' encoding='UTF-8'?>",
              "<!-- Era 3150-->",
              "<ratgen>",
              "    <factions>",
              "        <faction key='RA'>",
              "            <pctOmni>0,0,0,48,100</pctOmni>",
              "            <pctClan>70,70,95,100,100</pctClan>",
              "            <pctSL>30,30,5,0,0</pctSL>",
              "            <pctOmni unitType='AeroSpaceFighter'>0,0,10,75,100</pctOmni>",
              "            <pctClan unitType='AeroSpaceFighter'>80,80,100,100,100</pctClan>",
              "            <pctSL unitType='AeroSpaceFighter'>20,0,0,0,0</pctSL>",
              "            <pctClan unitType='Vehicle'>15,0,45,45,45</pctClan>",
              "            <pctSL unitType='Vehicle'>85,0,55,55,55</pctSL>",
              "            <techMargin>16</techMargin>",
              "            <salvage pct='10'>FS:4,DC:10</salvage>",
              "        </faction>",
              "    </factions>",
              "    <units>",
              "    </units>",
              "</ratgen>");

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
        Document xmlDoc = db.parse(byteArrayInputStream);

        Element element = xmlDoc.getDocumentElement();
        NodeList nl = element.getChildNodes();
        element.normalize();
        Node mainNode = nl.item(1);

        // Get faction key from entry
        Node wn = mainNode.getChildNodes().item(1);
        String fKey = wn.getAttributes().getNamedItem("key").getTextContent();

        // Load appropriate faction and load with record
        FactionRecord fr = factions.get(fKey);
        fr.loadEra(wn, 3151);

        // Validate Omni ASF value
        int pct = fr.findPctTech(FactionRecord.TechCategory.OMNI_AERO, 3151, 4);
        assertEquals(100, pct);

        // Assert Clan ASF value
        pct = fr.findPctTech(FactionRecord.TechCategory.CLAN_AERO, 3151, 0);
        assertEquals(80, pct);

        // Assert SL/IS ASF value
        pct = fr.findPctTech(FactionRecord.TechCategory.IS_ADVANCED_AERO, 3151, 0);
        assertEquals(20, pct);
    }

    @Test
    void lineageCodesWithoutAliasesReturnsOnlyKey() {
        FactionRecord factionRecord = new FactionRecord("CGS");
        assertEquals(List.of("CGS"), factionRecord.getLineageCodesForYear(3100));
    }

    @Test
    void lineageCodesPreferEraActiveAliasFirst() {
        FactionRecord factionRecord = new FactionRecord("CGS");
        factionRecord.addAlias(3080, "CEI");
        factionRecord.addAlias(3141, "SE");

        // Before the first alias year, the faction's own key is era-active.
        assertEquals(List.of("CGS", "CEI", "SE"), factionRecord.getLineageCodesForYear(3050));
        // In the Escorpion Imperio era, CEI is preferred, then the key, then the remaining alias.
        assertEquals(List.of("CEI", "CGS", "SE"), factionRecord.getLineageCodesForYear(3100));
        // Exactly on a boundary year, the alias that begins that year is era-active.
        assertEquals(List.of("CEI", "CGS", "SE"), factionRecord.getLineageCodesForYear(3080));
        // In the Scorpion Empire era, SE is preferred.
        assertEquals(List.of("SE", "CGS", "CEI"), factionRecord.getLineageCodesForYear(3200));
    }

    /** A faction with the standard five equipment ratings and no parents. */
    private static FactionRecord factionWithFiveRatings(String key) {
        FactionRecord factionRecord = new FactionRecord(key);
        factionRecord.setRatings(FIVE_RATINGS);
        return factionRecord;
    }

    @Test
    void singleDeclaredPercentageAppliesToEveryRating() {
        // The Word of Blake Shadow Divisions shape: one value, meaning "this is our profile".
        FactionRecord shadowDivisions = factionWithFiveRatings("WOBTEST.SD");
        shadowDivisions.setPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, "50");

        for (int rating = RATING_F; rating <= RATING_A; rating++) {
            assertEquals(50, shadowDivisions.getPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, rating),
                  "a lone declared percentage describes the faction, so it answers for rating " + rating);
        }
        // With nothing declared beyond it there is no parent to defer to either.
        assertEquals(50, shadowDivisions.findPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_A));
    }

    @Test
    void fiveDeclaredPercentagesAreStillReadPositionally() {
        // Regression guard: the common case must keep differentiating by rating, worst first.
        FactionRecord wordOfBlake = factionWithFiveRatings("WOBTEST");
        wordOfBlake.setPctTech(FactionRecord.TechCategory.IS_ADVANCED, TEST_ERA, "5,25,45,80,92");

        int[] expectedByRating = { 5, 25, 45, 80, 92 };
        for (int rating = RATING_F; rating <= RATING_A; rating++) {
            assertEquals(expectedByRating[rating],
                  wordOfBlake.getPctTech(FactionRecord.TechCategory.IS_ADVANCED, TEST_ERA, rating),
                  "index 0 is the worst rating, so rating " + rating + " must read positionally");
        }
    }

    @Test
    void partlyDeclaredPercentagesAnswerOnlyWhatTheyDeclare() {
        // Ambiguous rather than shorthand: there is no way to tell which ratings were meant to be
        // left to the parent, so the declared ones answer and the rest defer.
        FactionRecord partiallyRated = factionWithFiveRatings("WOBTEST.PM");
        partiallyRated.setPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, "10,20,30,40");

        assertEquals(10, partiallyRated.getPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_F));
        assertEquals(40, partiallyRated.getPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, 3));
        assertNull(partiallyRated.getPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_A),
              "rating A is undeclared, so it must defer rather than repeat rating B");
    }

    @Test
    void nothingDeclaredReturnsNull() {
        FactionRecord factionRecord = factionWithFiveRatings("WOBTEST.NONE");
        assertNull(factionRecord.getPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_F),
              "a category the era file never mentions declares nothing");

        factionRecord.setPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, "");
        assertNull(factionRecord.getPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_F),
              "an empty list declares nothing");
    }

    @Test
    void declaredAccessorReportsOnlyWhatTheFileHolds() {
        // What an editor must show: a one-value entry stays a one-value entry, so editing it cannot
        // silently write back five.
        FactionRecord shadowDivisions = factionWithFiveRatings("WOBTEST.SD2");
        shadowDivisions.setPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, "50");

        assertEquals(50, shadowDivisions.getDeclaredPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_F));
        assertNull(shadowDivisions.getDeclaredPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_A),
              "the file declares nothing for rating A, whatever generation resolves it to");
    }

    @Test
    void childSinglePercentageOutranksParentFullList() {
        // The reported bug: the rulesets default these commands to rating A, so a child profile that
        // only answered at rating F was never the one that applied.
        FactionRecord parent = factionWithFiveRatings("WOBTEST.PARENT");
        parent.setPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, "0,0,0,0,30");

        FactionRecord child = factionWithFiveRatings("WOBTEST.CHILD");
        child.setPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, "50");
        child.setParentFactions("WOBTEST.PARENT");

        RATGenerator.getInstance().addFaction(parent);
        RATGenerator.getInstance().addFaction(child);

        assertEquals(50, child.findPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_A),
              "the child declares its own profile, so it must not inherit the parent's rating A");
        assertEquals(30, parent.findPctTech(FactionRecord.TechCategory.OMNI, TEST_ERA, RATING_A),
              "the parent's own full list is unaffected");
    }
}
