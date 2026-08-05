/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 8/19/13 6:30 AM
 */
class BehaviorSettingsTest {

    /**
     * The herding accessors were renamed to mutual support, which broke MekHQ's build: it calls them from
     * BotForce, CustomizeBotForceDialog and Unit. They are kept as deprecated delegates so code outside MegaMek
     * keeps working, and this pins that they really do delegate rather than drift into a second copy of the state.
     */
    @Test
    @SuppressWarnings("removal")
    void deprecatedHerdingAccessorsDelegateToMutualSupport() throws PrincessException {
        BehaviorSettings behaviorSettings = new BehaviorSettings();

        behaviorSettings.setHerdMentalityIndex(7);
        assertEquals(7, behaviorSettings.getMutualSupportIndex(), "the old setter must write the new state");
        assertEquals(7, behaviorSettings.getHerdMentalityIndex(), "and the old getter must read it back");

        behaviorSettings.setMutualSupportIndex(2);
        assertEquals(2, behaviorSettings.getHerdMentalityIndex(), "the old getter must see a new-setter write");

        behaviorSettings.setHerdMentalityIndex("9");
        assertEquals(9, behaviorSettings.getMutualSupportIndex(), "the string overload must delegate too");

        assertEquals(behaviorSettings.getMutualSupportValue(), behaviorSettings.getHerdMentalityValue(),
              "values must agree");
        assertEquals(behaviorSettings.getMutualSupportValue(4), behaviorSettings.getHerdMentalityValue(4),
              "indexed values must agree");

        behaviorSettings.setExclusiveHerding(true);
        assertTrue(behaviorSettings.isExclusiveMutualSupport(), "the old flag setter must write the new flag");
        assertTrue(behaviorSettings.isExclusiveHerding(), "and the old flag getter must read it back");

        // The other direction, which is the one that catches a deprecated getter quietly acquiring a field of
        // its own: write through the current setter, read through the deprecated getter.
        behaviorSettings.setExclusiveMutualSupport(false);
        assertFalse(behaviorSettings.isExclusiveHerding(), "the old flag getter must see a new-setter write");
        behaviorSettings.setExclusiveMutualSupport(true);
        assertTrue(behaviorSettings.isExclusiveHerding(), "and must follow it back again");

        behaviorSettings.setExclusiveHerding("false");
        assertFalse(behaviorSettings.isExclusiveMutualSupport(), "the string overload must delegate too");
        behaviorSettings.setExclusiveMutualSupport("true");
        assertTrue(behaviorSettings.isExclusiveHerding(), "including the new string overload, read the old way");
    }

    @Test
    void testSetDescription() throws PrincessException {
        BehaviorSettings behaviorSettings = new BehaviorSettings();

        // Test a normal description.
        String description = "Test behavior";
        behaviorSettings.setDescription(description);
        assertTrue(true);

        // Test a null description.
        try {
            behaviorSettings.setDescription(null);
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
    void testStrategicBuildingTargets() {
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
    void testPreferredUnitTargets() {
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

    /**
     * Behavior presets saved before the herding-to-mutual-support rename must still load, or every bot a player has
     * configured silently reverts to defaults. The old element names are therefore still read, though no longer
     * written.
     */
    @Test
    void behaviorPresetsSavedBeforeTheRenameStillLoad()
          throws ParserConfigurationException, IOException, SAXException, PrincessException {
        String legacyXml = """
              <behavior>
                  <name>Legacy</name>
                  <herdMentalityIndex>8</herdMentalityIndex>
                  <exclusiveHerding>true</exclusiveHerding>
              </behavior>
              """;
        DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new CharArrayReader(legacyXml.toCharArray())));

        BehaviorSettings behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(document.getDocumentElement());

        assertEquals(8, behaviorSettings.getMutualSupportIndex(), "the pre-rename element name must still be read");
        assertTrue(behaviorSettings.isExclusiveMutualSupport(), "the pre-rename exclusive flag must still be read");
    }

    /** A preset saved under the current name must of course also load. */
    @Test
    void behaviorPresetsSavedUnderTheCurrentNameLoad()
          throws ParserConfigurationException, IOException, SAXException, PrincessException {
        String currentXml = """
              <behavior>
                  <name>Current</name>
                  <mutualSupportIndex>3</mutualSupportIndex>
                  <exclusiveMutualSupport>true</exclusiveMutualSupport>
              </behavior>
              """;
        DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new CharArrayReader(currentXml.toCharArray())));

        BehaviorSettings behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(document.getDocumentElement());

        assertEquals(3, behaviorSettings.getMutualSupportIndex());
        assertTrue(behaviorSettings.isExclusiveMutualSupport());
    }

    @Test
    void testFromXml() throws ParserConfigurationException, IOException, SAXException, PrincessException {
        DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();

        // Test loading good behavior settings.
        Reader reader = new CharArrayReader(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_XML.toCharArray());
        Document testDocument = documentBuilder.parse(new InputSource(reader));
        Element testBehaviorElement = testDocument.getDocumentElement();
        Set<String> expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        Set<Integer> expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
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
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
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
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals("null", behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
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
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
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
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertSame(CardinalEdge.NONE, behaviorSettings.getRetreatEdge());

        // Test loading behavior settings w/ a NULL forced withdrawal.
        reader = new CharArrayReader(
              BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_FORCED_WITHDRAWAL.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertFalse(behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
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
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertFalse(behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a Fall Shame > 10.
        // All other indexes use the same method for validation.
        reader = new CharArrayReader(
              BehaviorSettingsTestConstants.BEHAVIOR_XML_TOO_BIG_FALL_SHAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(10, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a Fall Shame < 0.
        // All other indexes use the same method for validation.
        reader = new CharArrayReader(
              BehaviorSettingsTestConstants.BEHAVIOR_XML_TOO_SMALL_FALL_SHAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        expectedUnits = new HashSet<>(1);
        expectedUnits.add(BehaviorSettingsTestConstants.PRIORITY_TARGET);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(0, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ a NULL strategic target.
        reader = new CharArrayReader(
              BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_STRATEGIC_TARGET.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(1);
        expectedTargets.add("null");
        expectedUnits = new HashSet<>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());

        // Test loading behavior settings w/ an Empty strategic target.
        reader = new CharArrayReader(
              BehaviorSettingsTestConstants.BEHAVIOR_XML_EMPTY_STRATEGIC_TARGET.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<>(0);
        expectedUnits = new HashSet<>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getRetreatEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_DESTINATION_EDGE,
              behaviorSettings.getDestinationEdge());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
              behaviorSettings.isForcedWithdrawal());
        assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
              behaviorSettings.getHyperAggressionIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
              behaviorSettings.getSelfPreservationIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_MUTUAL_SUPPORT_INDEX,
              behaviorSettings.getMutualSupportIndex());
        assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        assertEquals(expectedTargets, behaviorSettings.getStrategicBuildingTargets());
        assertEquals(expectedUnits, behaviorSettings.getPriorityUnitTargets());
    }
}
