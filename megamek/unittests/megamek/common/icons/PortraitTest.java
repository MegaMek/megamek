/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.icons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import megamek.utilities.xml.MMXMLUtility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Node;

class PortraitTest {

    /**
     * Parses a portrait from XML the same way a saved campaign does, so that the compatibility handlers in
     * {@link AbstractIcon#parseNode(Node)} are exercised.
     */
    private static Portrait parse(final String category, final String filename) throws Exception {
        final String xml = "<portrait><category>" +
              category +
              "</category><filename>" +
              filename +
              "</filename></portrait>";

        final DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
        final Node node = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
              .getDocumentElement();

        return Portrait.parseFromXML(node);
    }

    @ParameterizedTest
    @CsvSource(value = { "Male/Admin/Command/, Male/Administrator/", "Male/Admin/HR/, Male/Administrator/",
                         "Male/Admin/Logistical/, Male/Administrator/", "Male/Admin/Transport/, Male/Administrator/",
                         "Female/Admin/Command/, Female/Administrator/", "Female/Admin/HR/, Female/Administrator/",
                         "Female/Admin/Logistical/, Female/Administrator/",
                         "Female/Admin/Transport/, Female/Administrator/" })
    void shippedAdministratorPortraitIsRepointed(final String saved, final String expected) throws Exception {
        final Portrait portrait = parse(saved, "Adm_HR_M_1.png");

        assertEquals(expected, portrait.getCategory());
        assertEquals("Adm_HR_M_1.png", portrait.getFilename());
    }

    @Test
    void shippedAdministratorPortraitIsRepointedWithoutTrailingSeparator() throws Exception {
        final Portrait portrait = parse("Male/Admin/Transport", "Adm_Trans_M_1.png");

        assertEquals("Male/Administrator", portrait.getCategory());
    }

    /**
     * No portrait ever shipped in the Admin folder itself, so a category naming it without one of the four shipped
     * sub-folders belongs to a folder the user created. Those folders are not renamed, so the category must not be
     * either.
     */
    @ParameterizedTest
    @CsvSource(value = { "Male/Admin/", "Female/Admin/", "Male/Admin", "Male/Admin/HR Reserves/",
                         "Male/Admin/Veterans/" })
    void userOwnedAdminFolderIsLeftAlone(final String saved) throws Exception {
        final Portrait portrait = parse(saved, "portrait.png");

        assertEquals(saved, portrait.getCategory());
    }

    @ParameterizedTest
    @CsvSource(value = { "Male/Administrator/", "Female/Administrator/", "Male/MekWarrior/",
                         "Male/Vehicle Crew/Ground/", "Male/Doctor/" })
    void unaffectedCategoriesAreLeftAlone(final String saved) throws Exception {
        final Portrait portrait = parse(saved, "portrait.png");

        assertEquals(saved, portrait.getCategory());
    }
}
