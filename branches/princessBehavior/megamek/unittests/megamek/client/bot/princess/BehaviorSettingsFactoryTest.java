package megamek.client.bot.princess;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.internal.util.collections.Sets;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/6/13 10:11 PM
 */
@RunWith(JUnit4.class)
public class BehaviorSettingsFactoryTest extends BehaviorSettingsFactory {

    public BehaviorSettingsFactoryTest() {
    }

    protected static Document buildTestDocument() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Reader reader =
                new CharArrayReader(BehaviorSettingsFactoryTestConstants.GOOD_BEHAVIOR_SETTINGS_FILE.toCharArray());
        return documentBuilder.parse(new InputSource(reader));
    }

    @Test
    public void testLoadBehaviorSettings() throws IOException, SAXException, ParserConfigurationException {
        // Test loading a good behavior settings file.
        behaviorMap = new HashMap<String, BehaviorSettings>();
        TestCase.assertTrue(loadBehaviorSettings(buildTestDocument()));
        TestCase.assertEquals(4, behaviorMap.size());
        String[] expectedBehaviors = new String[]
                {BehaviorSettingsFactoryTestConstants.NM_RECKLESS,
                 BehaviorSettingsFactoryTestConstants.NM_COWARDLY,
                 BehaviorSettingsFactoryTestConstants.NM_ESCAPE,
                 BehaviorSettingsFactoryTestConstants.NM_DEFAULT};
        TestCase.assertEquals(Sets.newSet(expectedBehaviors), Sets.newSet(getBehaviorNames()));

        // Test loading a null behavior settings file.
        behaviorMap = new HashMap<String, BehaviorSettings>();
        TestCase.assertFalse(loadBehaviorSettings(null));
        TestCase.assertEquals(1, behaviorMap.size());
        expectedBehaviors = new String[]{BehaviorSettingsFactoryTestConstants.NM_DEFAULT};
        Assert.assertArrayEquals(expectedBehaviors, getBehaviorNames());
    }
}
