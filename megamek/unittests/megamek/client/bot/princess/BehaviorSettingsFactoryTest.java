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

import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.stream.Streams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import megamek.utilities.xml.MMXMLUtility;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 10:11 PM
 */
class BehaviorSettingsFactoryTest {

    private BehaviorSettingsFactory testFactory = BehaviorSettingsFactory.getInstance();

    private static Document buildTestDocument() {
        try {
            DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
            File file = new File("testresources/megamek/client/bot/behaviorSettings.xml");
            try (Reader reader = new FileReader(file)) {
                return documentBuilder.parse(new InputSource(reader));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        testFactory.behaviorMap.clear();
    }

    @Test
    void testLoadBehaviorSettingsWithNoSaveFile() {
        // Test loading a null behavior settings file.
        assertFalse(testFactory.loadBehaviorSettings(null));
        var expectedBehaviors = Set.of(BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.CONVOY_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.ESCAPE_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.PIRATE_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.RUTHLESS_BEHAVIOR_DESCRIPTION);
        assertEquals(expectedBehaviors, Sets.newSet(testFactory.getBehaviorNames()));
    }

    @Test
    void testLoadBehaviorSettingsWithTestDocument() {
        assertTrue(testFactory.loadBehaviorSettings(buildTestDocument()));
        var expectedBehaviors = Set.of(BehaviorSettingsFactory.BERSERK_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.ESCAPE_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.CONVOY_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.PIRATE_BEHAVIOR_DESCRIPTION,
              BehaviorSettingsFactory.RUTHLESS_BEHAVIOR_DESCRIPTION);
        assertTrue(Sets.newSet(testFactory.getBehaviorNames()).containsAll(expectedBehaviors));
    }
}
