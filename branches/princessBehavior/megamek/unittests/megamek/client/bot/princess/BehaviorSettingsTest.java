package megamek.client.bot.princess;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 8/19/13 6:30 AM
 */
@RunWith(JUnit4.class)
public class BehaviorSettingsTest {

    @Test
    public void testSetDescription() throws PrincessException {
        BehaviorSettings behaviorSettings = new BehaviorSettings();

        // Test a normal description.
        String description = "Test behavior";
        behaviorSettings.setDescription(description);
        TestCase.assertTrue(true);

        // Test a null description.
        description = null;
        try {
            behaviorSettings.setDescription(description);
            TestCase.fail("Should have thrown an error!");
        } catch (IllegalArgumentException e) {
            TestCase.assertTrue(true);
        }

        // Test an empty description.
        description = "";
        try {
            behaviorSettings.setDescription(description);
            TestCase.fail("Should have thrown an error!");
        } catch (IllegalArgumentException e) {
            TestCase.assertTrue(true);
        }
    }

    @Test
    public void testStrategicTargetList() {
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        final String goodHexTarget = "1234";
        final String goodHexTarget2 = "4567";
        Set<String> expectedTargets = new HashSet<String>(2);
        expectedTargets.add(goodHexTarget);

        // Test adding a normal hex target.
        behaviorSettings.addStrategicTarget(goodHexTarget);
        Set<String> actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test adding a duplicate target.
        behaviorSettings.addStrategicTarget(goodHexTarget);
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test adding a second target.
        expectedTargets.add(goodHexTarget2);
        behaviorSettings.addStrategicTarget(goodHexTarget2);
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test adding a null target.
        behaviorSettings.addStrategicTarget(null);
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test adding an empty target.
        behaviorSettings.addStrategicTarget("");
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test removing a target.
        expectedTargets.remove(goodHexTarget2);
        behaviorSettings.removeStrategicTarget(goodHexTarget2);
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test removing a null target
        behaviorSettings.removeStrategicTarget(null);
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test removing an empty target
        behaviorSettings.removeStrategicTarget("");
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);

        // Test removing a target not on the list.
        behaviorSettings.removeStrategicTarget("blah");
        actualTargets = behaviorSettings.getStrategicTargets();
        TestCase.assertEquals(expectedTargets, actualTargets);
    }

    @Test
    public void testFromXml() throws ParserConfigurationException, IOException, SAXException, PrincessException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Test loading good behavior settings.
        Reader reader = new CharArrayReader(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_XML.toCharArray());
        Document testDocument = documentBuilder.parse(new InputSource(reader));
        Element testBehaviorElement = testDocument.getDocumentElement();
        Set<String> expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        BehaviorSettings behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading good behavior settings w/out any strategic targets.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_XML_NO_TARGETS.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ a NULL name.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_NAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals("null", behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ an empty name.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_EMPTY_NAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        try {
            behaviorSettings.fromXml(testBehaviorElement);
            TestCase.fail("Should have thrown an error!");
        } catch (PrincessException e) {
            TestCase.assertTrue(true);
        }

        // Test loading behavior settings w/ a NULL home edge.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_HOME_EDGE.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        try {
            behaviorSettings.fromXml(testBehaviorElement);
            TestCase.fail("Should have thrown an error!");
        } catch (PrincessException e) {
            TestCase.assertTrue(true);
        }

        // Test loading behavior settings w/ a NULL forced withdrawal.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_FORCED_WITHDRAWAL.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(false, behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ a NULL go home.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_GO_HOME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(false, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ a NULL auto-flee.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_AUTO_FLEE.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(false, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ a Fall Shame > 10.
        // All other indexes use the same method for validation.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_TOO_BIG_FALL_SHAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(10, behaviorSettings.getFallShameIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ a Fall Shame < 0.
        // All other indexes use the same method for validation.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_TOO_SMALL_FALL_SHAME.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(2);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_1);
        expectedTargets.add(BehaviorSettingsTestConstants.STRATEGIC_TARGET_2);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(0, behaviorSettings.getFallShameIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ a NULL strategic target.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_NULL_STRATEGIC_TARGET.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(1);
        expectedTargets.add("null");
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());

        // Test loading behavior settings w/ an Empty strategic target.
        reader = new CharArrayReader(BehaviorSettingsTestConstants.BEHAVIOR_XML_EMPTY_STRATEGIC_TARGET.toCharArray());
        testDocument = documentBuilder.parse(new InputSource(reader));
        testBehaviorElement = testDocument.getDocumentElement();
        expectedTargets = new HashSet<String>(0);
        behaviorSettings = new BehaviorSettings();
        behaviorSettings.fromXml(testBehaviorElement);
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BEHAVIOR_NAME, behaviorSettings.getDescription());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HOME_EDGE, behaviorSettings.getHomeEdge());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FORCED_WITHDRAWAL,
                              behaviorSettings.isForcedWithdrawal());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_GO_HOME, behaviorSettings.shouldGoHome());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_AUTO_FLEE, behaviorSettings.shouldAutoFlee());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_FALL_SHAME_INDEX, behaviorSettings.getFallShameIndex
                ());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HYPER_AGGRESSION_INDEX,
                              behaviorSettings.getHyperAggressionIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_SELF_PRESERVATION_INDEX,
                              behaviorSettings.getSelfPreservationIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_HERD_MENTALITY_INDEX,
                              behaviorSettings.getHerdMentalityIndex());
        TestCase.assertEquals(BehaviorSettingsTestConstants.GOOD_BRAVERY_INDEX, behaviorSettings.getBraveryIndex());
        TestCase.assertEquals(expectedTargets, behaviorSettings.getStrategicTargets());
    }
}
