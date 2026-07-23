/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */

package megamek.common.battlefieldSupport.cardDrawer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Smoke tests for {@link BattlefieldSupportCard}. Renders every {@code .bfs} test fixture to a temporary PNG and
 * asserts each card renders to an image of the expected size without error (including the frequent no-fluff-art case).
 */
class BattlefieldSupportCardRenderTest {

    @TempDir
    Path temporaryDirectory;

    private static final String[] FIXTURES = {
          "Maxim Heavy Hover Transport.bfs",
          "Mobile Long Tom LT-MOB-25.bfs",
          "Heavy Emplacement.bfs",
          "Foot Platoon (Rifle).bfs",
          "Elemental Battle Armor [MG] (Sqd5).bfs",
          "Browning Mobile HQ.bfs"
    };

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void rendersAllFixturesToPng() throws Exception {
        File outDir = temporaryDirectory.toFile();

        for (String fixture : FIXTURES) {
            BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
                  new File("testresources/data/mekfiles/" + fixture)).getEntity();

            BattlefieldSupportCard card = new BattlefieldSupportCard(asset);
            BufferedImage image = card.getCardImage(BattlefieldSupportCard.WIDTH);

            assertNotNull(image);
            assertTrue(image.getWidth() == BattlefieldSupportCard.WIDTH);

            String pngName = fixture.replace(".bfs", ".png");
            ImageIO.write(image, "png", new File(outDir, pngName));
        }
    }

    @Test
    void rendersVeteranCrewCard() throws Exception {
        // Same fixture as the Regular default, but with a Veteran crew so the Veteran value is the bold one.
        File outDir = temporaryDirectory.toFile();

        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();
        asset.setVeteranCrew(true);

        BufferedImage image = new BattlefieldSupportCard(asset).getCardImage(BattlefieldSupportCard.WIDTH);
        assertNotNull(image);
        ImageIO.write(image, "png", new File(outDir, "Maxim Veteran.png"));
    }

    @Test
    void rendersColorLogoCards() throws Exception {
        // Renders the logo-only and full-color modes so the colored BATTLETECH logo can be visually inspected.
        File outDir = temporaryDirectory.toFile();

        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();

        BattlefieldSupportCard logoOnly = new BattlefieldSupportCard(asset);
        logoOnly.setColorMode(BattlefieldSupportCard.ColorMode.LOGO_ONLY);
        BufferedImage logoImage = logoOnly.getCardImage(BattlefieldSupportCard.WIDTH);
        assertNotNull(logoImage);
        ImageIO.write(logoImage, "png", new File(outDir, "Maxim LogoOnly.png"));

        BattlefieldSupportCard full = new BattlefieldSupportCard(asset);
        full.setColorMode(BattlefieldSupportCard.ColorMode.ALL);
        BufferedImage fullImage = full.getCardImage(BattlefieldSupportCard.WIDTH);
        assertNotNull(fullImage);
        ImageIO.write(fullImage, "png", new File(outDir, "Maxim FullColor.png"));
    }

    @Test
    void rendersDamagedCards() throws Exception {
        // A damaged asset shows the struck-through original Destroy Check next to the current value; verify the card
        // renders (in black-and-white and in a color mode) and that the damaged card differs from the undamaged one.
        File outDir = temporaryDirectory.toFile();

        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();

        BufferedImage undamaged = new BattlefieldSupportCard(asset).getCardImage(BattlefieldSupportCard.WIDTH);

        // Lower the current Destroy Check to represent persistent damage.
        asset.setDestroyCheck(asset.getODestroyCheck() - 2);

        BufferedImage bwDamaged = new BattlefieldSupportCard(asset).getCardImage(BattlefieldSupportCard.WIDTH);
        assertNotNull(bwDamaged);
        ImageIO.write(bwDamaged, "png", new File(outDir, "Maxim Damaged BW.png"));

        BattlefieldSupportCard colorCard = new BattlefieldSupportCard(asset);
        colorCard.setColorMode(BattlefieldSupportCard.ColorMode.LOGO_ONLY);
        BufferedImage colorDamaged = colorCard.getCardImage(BattlefieldSupportCard.WIDTH);
        assertNotNull(colorDamaged);
        ImageIO.write(colorDamaged, "png", new File(outDir, "Maxim Damaged Color.png"));

        // The damaged card must differ from the undamaged one (original + current values and the strikethrough).
        assertFalse(imagesEqual(undamaged, bwDamaged));
    }

    @Test
    void rendersDamageWithSmallCapsDxMarker() throws Exception {
        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();

        List<String> textElements = svgTextElements(new BattlefieldSupportCard(asset));

        assertTrue(textElements.contains("DX"));
        assertFalse(textElements.contains(asset.getDamageDisplay()));
    }

    @Test
    void zeroDamageStillRendersAsAnEmDash() throws Exception {
        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Mobile Long Tom LT-MOB-25.bfs")).getEntity();

        List<String> textElements = svgTextElements(new BattlefieldSupportCard(asset));

        assertTrue(textElements.contains("\u2014"));
        assertFalse(textElements.contains("DX"));
    }

    private static List<String> svgTextElements(BattlefieldSupportCard card) {
        DOMImplementation implementation = SVGDOMImplementation.getDOMImplementation();
        Document document = implementation.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
        SVGGraphics2D graphics = new SVGGraphics2D(document);
        card.drawCard(graphics);
        Element group = graphics.getTopLevelGroup(true);
        NodeList textNodes = group.getElementsByTagName("text");
        List<String> textElements = new ArrayList<>();
        for (int i = 0; i < textNodes.getLength(); i++) {
            textElements.add(textNodes.item(i).getTextContent());
        }
        return textElements;
    }

    private static boolean imagesEqual(BufferedImage a, BufferedImage b) {
        if ((a.getWidth() != b.getWidth()) || (a.getHeight() != b.getHeight())) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
