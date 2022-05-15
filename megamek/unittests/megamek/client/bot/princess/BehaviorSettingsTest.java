/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

import megamek.utils.MegaMekXmlUtil;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 8/19/13 6:30 AM
 */
public class BehaviorSettingsTest {

    @Test
    public void testSetDescription() throws PrincessException {
        BehaviorSettings behaviorSettings = new BehaviorSettings();

        // Test a normal description.
        String description = "Test behavior";
        behaviorSettings.setDescription(description);
        assertTrue(true);

        // Test a null description.
        description = null;
        try {
            behaviorSettings.setDescription(description);
            fail("Should have thrown an error!");
        } catch (PrincessException e) {
            assertTrue(true);
        }

        // Test an empty description.
        description = "";
        try {
            behaviorSettings.setDescription(description);
            fail("Should have thrown an error!");
        } catch (PrincessException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testStrategicBuildingTargets() {
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        final String goodHexTarget = "1234";
        final String goodHexTarget2 = "4567";
        Set<String> expectedTargets = new HashSet<>(2);
        expectedTargets.add(goodHexTarget);

        // Test adding a normal hex target.
        behaviorSettings.addStrategicTarget(goodHexTarget);
        Set<String> actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding a duplicate target.
        behaviorSettings.addStrategicTarget(goodHexTarget);
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding a second target.
        expectedTargets.add(goodHexTarget2);
        behaviorSettings.addStrategicTarget(goodHexTarget2);
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding a null target.
        behaviorSettings.addStrategicTarget(null);
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding an empty target.
        behaviorSettings.addStrategicTarget("");
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing a target.
        expectedTargets.remove(goodHexTarget2);
        behaviorSettings.removeStrategicTarget(goodHexTarget2);
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing a null target
        behaviorSettings.removeStrategicTarget(null);
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing an empty target
        behaviorSettings.removeStrategicTarget("");
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing a target not on the list.
        behaviorSettings.removeStrategicTarget("blah");
        actualTargets = behaviorSettings.getStrategicBuildingTargets();
        assertEquals(expectedTargets, actualTargets);
    }

    @Test
    public void testPreferredUnitTargets() {
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        final int goodUnitTarget = 1;
        final int goodUnitTarget2 = 4;
        Set<Integer> expectedTargets = new HashSet<>(2);
        expectedTargets.add(goodUnitTarget);

        // Test adding a normal hex target.
        behaviorSettings.addPriorityUnit(goodUnitTarget);
        Set<Integer> actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding a duplicate target.
        behaviorSettings.addPriorityUnit(goodUnitTarget);
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding a second target.
        expectedTargets.add(goodUnitTarget2);
        behaviorSettings.addPriorityUnit(goodUnitTarget2);
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding a null target.
        behaviorSettings.addPriorityUnit(null);
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test adding an empty target.
        behaviorSettings.addPriorityUnit("");
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing a target.
        expectedTargets.remove(goodUnitTarget2);
        behaviorSettings.removePriorityUnit(goodUnitTarget2);
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing a null target
        behaviorSettings.removePriorityUnit(null);
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing an empty target
        behaviorSettings.removePriorityUnit("");
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);

        // Test removing a target not on the list.
        behaviorSettings.removePriorityUnit("blah");
        actualTargets = behaviorSettings.getPriorityUnitTargets();
        assertEquals(expectedTargets, actualTargets);
    }

    @Test
    public void testFromXml() throws ParserConfigurationException, IOException, SAXException, PrincessException {
        DocumentBuilder documentBuilder = MegaMekXmlUtil.newSafeDocumentBuilder();

        // Test loading good behavior settings.
        Reader reader = new CharArrayReader(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_XML.toCharArray());
        Document testDocument = documentBuilder.parse(new InputSource(reader));
        Element testBehaviorElement = testDocument.getDocumentElement();
        Set<String> expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        Set<Integer> expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading good behavior settings w/out any strategic targets.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_XML_NO_TARGETS.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(0);
        expectedUnits = new HashSet<>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a NULL name.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_NAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals("null", behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ an empty name.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_EMPTY_NAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        try {
            behaviorSettings.fromXml(testBehaviorElement);
            fail("Should have thrown an error!");
        } catch (PrincessException e) {
            assertTrue(true);
        }

        // Test loading behavior settings w/ a NULL home edge.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_HOME_EDGE.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertSame(behaviorSettings.getRetreatEdge(), CardinalEdge.NONE);

        // Test loading behavior settings w/ a NULL forced withdrawal.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_FORCED_WITHDRAWAL.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertFalse(behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a NULL auto-flee.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_AUTO_FLEE.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertFalse(behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a Fall Shame > 10.
        // All other indexes use the same method for validation.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_TOO_BIG_FALL_SHAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(10, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a Fall Shame < 0.
        // All other indexes use the same method for validation.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_TOO_SMALL_FALL_SHAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(0, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a NULL strategic target.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_STRATEGIC_TARGET.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(1);
        expectedTargets.add("null");
        expectedUnits = new HashSet<>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ an Empty strategic target.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_EMPTY_STRATEGIC_TARGET.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(0);
        expectedUnits = new HashSet<>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE, behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                behaviorSettings.getHerdMentalityIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());
    }
}
