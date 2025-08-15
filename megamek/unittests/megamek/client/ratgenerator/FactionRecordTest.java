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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
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

        ByteArrayInputStream bais = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
        Document xmlDoc = db.parse(bais);

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
}
