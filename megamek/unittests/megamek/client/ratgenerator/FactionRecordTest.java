package megamek.client.ratgenerator;

import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

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
     * <faction key='RA' name='Raven Alliance' minor='false' clan='true' periphery='false'>
     * <years>3083-</years>
     * <ratingLevels>Provisional Garrison,Solahma,Second Line,Front Line,Keshik</ratingLevels>
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
