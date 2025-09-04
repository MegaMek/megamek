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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;

import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 10:11 PM
 */
class BehaviorSettingsFactoryTest {

    private final BehaviorSettingsFactory testFactory = BehaviorSettingsFactory.getInstance();

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
