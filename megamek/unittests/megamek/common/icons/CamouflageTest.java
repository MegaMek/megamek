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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import megamek.common.battlefieldSupport.OverlayStyle;
import megamek.common.battlefieldSupport.StripeDirection;
import megamek.utilities.xml.MMXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class CamouflageTest {

    @Test
    void missingLegacyOverlayFieldsUseDefaults() throws ReflectiveOperationException {
        Camouflage camouflage = new Camouflage();
        setOverlayField(camouflage, "overlayColor", null);
        setOverlayField(camouflage, "overlayDirection", null);
        setOverlayField(camouflage, "overlayStyle", null);

        assertEquals(Camouflage.DEFAULT_ASSET_OVERLAY_COLOR, camouflage.getOverlayColor());
        assertEquals(StripeDirection.DIAGONAL, camouflage.getOverlayDirection());
        assertEquals(OverlayStyle.BAND, camouflage.getOverlayStyle());
        assertTrue(camouflage.hasDefaultOverlay());
        assertEquals(new Camouflage(), camouflage);
        assertEquals(new Camouflage().hashCode(), camouflage.hashCode());
    }

    @Test
    void partiallyMissingLegacyOverlayFieldsRemainSafeAndNonDefault() throws ReflectiveOperationException {
        Camouflage camouflage = new Camouflage();
        camouflage.setOverlayColor(Color.RED);
        setOverlayField(camouflage, "overlayDirection", null);
        setOverlayField(camouflage, "overlayStyle", null);

        assertEquals(StripeDirection.DIAGONAL, camouflage.getOverlayDirection());
        assertEquals(OverlayStyle.BAND, camouflage.getOverlayStyle());
        assertFalse(camouflage.hasDefaultOverlay());
    }

    @Test
    void nullOverlayStyleRestoresTheDefaultBand() {
        Camouflage camouflage = new Camouflage();
        camouflage.setOverlayStyle(OverlayStyle.NONE);

        camouflage.setOverlayStyle(null);

        assertEquals(OverlayStyle.BAND, camouflage.getOverlayStyle());
        assertTrue(camouflage.hasDefaultOverlay());
    }

    @Test
    void xmlRoundTripKeepsScaleAndRotation() throws Exception {
        Camouflage camouflage = new Camouflage("Standard Camo/", "Desert.jpg");
        camouflage.setScale(25);
        camouflage.setRotationAngle(-90);

        Camouflage parsed = roundTrip(camouflage);

        assertEquals(25, parsed.getScale());
        assertEquals(-90, parsed.getRotationAngle());
        assertEquals(camouflage, parsed);
    }

    @Test
    void xmlRoundTripKeepsOverlaySettings() throws Exception {
        Camouflage camouflage = new Camouflage("Standard Camo/", "Desert.jpg");
        camouflage.setOverlayStyle(OverlayStyle.NONE);
        camouflage.setOverlayDirection(StripeDirection.VERTICAL);
        camouflage.setOverlayColor(new Color(0x12, 0x34, 0x56));

        Camouflage parsed = roundTrip(camouflage);

        assertEquals(camouflage, parsed);
    }

    @Test
    void xmlWithoutTransformUsesDefaults() throws Exception {
        String xml = "<camouflage><category>Standard Camo/</category><filename>Desert.jpg</filename></camouflage>";

        Camouflage parsed = parse(xml);

        assertEquals(10, parsed.getScale());
        assertEquals(0, parsed.getRotationAngle());
        assertTrue(parsed.hasDefaultOverlay());
        assertEquals(new Camouflage("Standard Camo/", "Desert.jpg"), parsed);
    }

    private static Camouflage roundTrip(Camouflage camouflage) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            camouflage.writeToXML(printWriter, 0);
        }
        return parse(stringWriter.toString());
    }

    private static Camouflage parse(String xml) throws Exception {
        Element element = MMXMLUtility.newSafeDocumentBuilder()
              .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
              .getDocumentElement();
        return Camouflage.parseFromXML(element);
    }

    private static void setOverlayField(Camouflage camouflage, String fieldName, Object value)
          throws ReflectiveOperationException {
        Field field = Camouflage.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(camouflage, value);
    }
}
