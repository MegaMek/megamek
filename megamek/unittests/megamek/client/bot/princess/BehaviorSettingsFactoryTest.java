/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import junit.framework.TestCase;
import megamek.utilities.xml.MMXMLUtility;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.internal.util.collections.Sets;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 10:11 PM
 */
@RunWith(JUnit4.class)
public class BehaviorSettingsFactoryTest {

    private BehaviorSettingsFactory testFactory = BehaviorSettingsFactory.getInstance();

    private static Document buildTestDocument() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
        Reader reader =
                new CharArrayReader(BehaviorSettingsFactoryTestConstants.GOOD_BEHAVIOR_SETTINGS_FILE.toCharArray());
        return documentBuilder.parse(new InputSource(reader));
    }

    @Test
    public void testLoadBehaviorSettings() throws IOException, SAXException, ParserConfigurationException {
        // Test loading a good behavior settings file.
        testFactory.behaviorMap.clear();
        TestCase.assertTrue(testFactory.loadBehaviorSettings(buildTestDocument()));
        TestCase.assertEquals(5, testFactory.behaviorMap.size());
        String[] expectedBehaviors = new String[]
                {BehaviorSettingsFactoryTestConstants.NM_RECKLESS,
                        BehaviorSettingsFactoryTestConstants.NM_COWARDLY,
                        BehaviorSettingsFactoryTestConstants.NM_ESCAPE,
                        BehaviorSettingsFactoryTestConstants.NM_DEFAULT,
                        BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION};
        TestCase.assertEquals(Sets.newSet(expectedBehaviors), Sets.newSet(testFactory.getBehaviorNames()));

        // Test loading a null behavior settings file.
        testFactory.behaviorMap.clear();
        TestCase.assertFalse(testFactory.loadBehaviorSettings(null));
        TestCase.assertEquals(4, testFactory.behaviorMap.size());
        expectedBehaviors = new String[]{BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION,
                BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION,
                BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION,
                BehaviorSettingsFactory.ESCAPE_BEHAVIOR_DESCRIPTION};
        Assert.assertArrayEquals(expectedBehaviors, testFactory.getBehaviorNames());
    }
}
