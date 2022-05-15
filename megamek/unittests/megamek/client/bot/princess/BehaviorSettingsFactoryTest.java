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
import org.mockito.internal.util.collections.Sets;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 10:11 PM
 */
public class BehaviorSettingsFactoryTest {

    private BehaviorSettingsFactory testFactory = BehaviorSettingsFactory.getInstance();

    private static Document buildTestDocument() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = MegaMekXmlUtil.newSafeDocumentBuilder();
        Reader reader = new CharArrayReader(BehaviorSettingsFactoryTestConstants.GOOD_BEHAVIOR_SETTINGS_FILE.toCharArray());
        return documentBuilder.parse(new InputSource(reader));
    }

    @Test
    public void testLoadBehaviorSettings() throws IOException, SAXException, ParserConfigurationException {
        // Test loading a good behavior settings file.
        testFactory.behaviorMap.clear();
        assertTrue(testFactory.loadBehaviorSettings(buildTestDocument()));
        assertEquals(5, testFactory.behaviorMap.size());
        String[] expectedBehaviors = new String[]
                {BehaviorSettingsFactoryTestConstants.NM_RECKLESS,
                        BehaviorSettingsFactoryTestConstants.NM_COWARDLY,
                        BehaviorSettingsFactoryTestConstants.NM_ESCAPE,
                        BehaviorSettingsFactoryTestConstants.NM_DEFAULT,
                        BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION};
        assertEquals(Sets.newSet(expectedBehaviors), Sets.newSet(testFactory.getBehaviorNames()));

        // Test loading a null behavior settings file.
        testFactory.behaviorMap.clear();
        assertFalse(testFactory.loadBehaviorSettings(null));
        assertEquals(4, testFactory.behaviorMap.size());
        expectedBehaviors = new String[]{BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION,
                BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION,
                BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION,
                BehaviorSettingsFactory.ESCAPE_BEHAVIOR_DESCRIPTION};
        assertArrayEquals(expectedBehaviors, testFactory.getBehaviorNames());
    }
}
