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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Test
    void checkValueAndWriteInBoxUseFixedAnchors() throws Exception {
        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();
        Element singleDigitCard = svgGroup(new BattlefieldSupportCard(asset));

        asset.setODestroyCheck(10);
        Element doubleDigitCard = svgGroup(new BattlefieldSupportCard(asset));

        asset.setODestroyCheck(7);
        asset.setDestroyCheck(5);
        Element damagedCard = svgGroup(new BattlefieldSupportCard(asset));

        Element singleDigit = textElement(singleDigitCard, "7");
        Element doubleDigit = textElement(doubleDigitCard, "10");
        assertEquals(attribute(singleDigit, "x"), attribute(doubleDigit, "x"), 0.01);
        assertEquals(attribute(singleDigit, "x"), attribute(rightmostTextElement(damagedCard, "5"), "x"), 0.01);
        assertEquals(attribute(textElement(singleDigitCard, "3/6/9"), "y"), attribute(singleDigit, "y"), 0.01);

        Element singleDigitBox = writeInBox(singleDigitCard);
        Element doubleDigitBox = writeInBox(doubleDigitCard);
        assertEquals(points(singleDigitBox), points(doubleDigitBox));
        assertEquals(points(singleDigitBox), points(writeInBox(damagedCard)));
        assertEquals(List.of(900d, 527d, 990d, 527d, 990d, 587d, 890d, 587d, 890d, 537d),
              points(singleDigitBox));
    }

    @Test
    void statLabelsUseTwoPixelLetterSpacing() throws Exception {
        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();
        Element card = svgGroup(new BattlefieldSupportCard(asset));

        for (String label : List.of("RANGE", "SKILL", "DMG", "CHECK")) {
            Element labelElement = textElement(card, label);
            assertEquals(BattlefieldSupportCard.STAT_LABEL_LETTER_SPACING_PX + "px",
                  labelElement.getAttribute("letter-spacing"));
        }
        Element value = textElement(card, "3/6/9");
        assertTrue(value.getAttribute("letter-spacing").isBlank());
        assertFalse(value.getAttribute("style").contains("letter-spacing"));
    }

    @Test
    void footerBorderFollowsSelectedFontWidth() throws Exception {
        BattlefieldSupportAsset asset = (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();
        Font standardFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        Font wideFont = standardFont.deriveFont(AffineTransform.getScaleInstance(1.4, 1));

        BattlefieldSupportCard standardCard = new BattlefieldSupportCard(asset);
        standardCard.setFont(standardFont);
        List<Double> standardBorder = outerBorder(svgGroup(standardCard));
        FontRenderContext context = new FontRenderContext(null, true, true);
        Font brandFont = standardFont.deriveFont(Font.BOLD).deriveFont(26f);
        Rectangle supportBounds = brandFont.createGlyphVector(context, "SUPPORT").getPixelBounds(null, 0, 0);
        Font copyrightFont = standardFont.deriveFont(Font.PLAIN).deriveFont(13f);
        String copyright = "\u00A9 " + LocalDate.now().getYear() + " The Topps Company. All rights reserved.";
        Rectangle copyrightBounds = copyrightFont.createGlyphVector(context, copyright).getPixelBounds(null, 0, 0);
        int copyrightLeft = BattlefieldSupportCard.WIDTH - 40 - copyrightBounds.width;

        assertEquals(420 + supportBounds.x + supportBounds.width + 6, standardBorder.get(0), 0.01);
        assertEquals(copyrightLeft, standardBorder.get(4), 0.01);

        BattlefieldSupportCard wideCard = new BattlefieldSupportCard(asset);
        wideCard.setFont(wideFont);
        List<Double> wideBorder = outerBorder(svgGroup(wideCard));

        assertTrue(wideBorder.get(0) > standardBorder.get(0),
              "the border must resume farther right when SUPPORT is wider");
        assertTrue(wideBorder.get(2) < standardBorder.get(2),
              "the shelf rise must move left when the copyright is wider");
    }

    private static List<String> svgTextElements(BattlefieldSupportCard card) {
        Element group = svgGroup(card);
        NodeList textNodes = group.getElementsByTagName("text");
        List<String> textElements = new ArrayList<>();
        for (int i = 0; i < textNodes.getLength(); i++) {
            textElements.add(textNodes.item(i).getTextContent());
        }
        return textElements;
    }

    private static Element svgGroup(BattlefieldSupportCard card) {
        DOMImplementation implementation = SVGDOMImplementation.getDOMImplementation();
        Document document = implementation.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
        SVGGraphics2D graphics = new SVGGraphics2D(document);
        card.drawCard(graphics);
        Element group = graphics.getTopLevelGroup(true);
        BattlefieldSupportCard.applySvgStyles(group);
        return group;
    }

    private static Element textElement(Element group, String value) {
        NodeList textNodes = group.getElementsByTagName("text");
        for (int i = 0; i < textNodes.getLength(); i++) {
            if (value.equals(textNodes.item(i).getTextContent())) {
                return (Element) textNodes.item(i);
            }
        }
        throw new AssertionError("No SVG text element found for " + value);
    }

    private static Element rightmostTextElement(Element group, String value) {
        NodeList textNodes = group.getElementsByTagName("text");
        Element rightmost = null;
        for (int i = 0; i < textNodes.getLength(); i++) {
            Element textElement = (Element) textNodes.item(i);
            if (value.equals(textElement.getTextContent())
                  && ((rightmost == null) || (attribute(textElement, "x") > attribute(rightmost, "x")))) {
                rightmost = textElement;
            }
        }
        if (rightmost == null) {
            throw new AssertionError("No SVG text element found for " + value);
        }
        return rightmost;
    }

    private static Element writeInBox(Element group) {
        NodeList polygons = group.getElementsByTagName("polygon");
        for (int i = 0; i < polygons.getLength(); i++) {
            Element polygon = (Element) polygons.item(i);
            List<Double> points = points(polygon);
            if (points.contains(990d) && points.contains(587d) && (points.size() == 10)) {
                return polygon;
            }
        }
        throw new AssertionError("No CHECK write-in box found");
    }

    private static List<Double> outerBorder(Element group) {
        NodeList paths = group.getElementsByTagName("path");
        for (int i = 0; i < paths.getLength(); i++) {
            String pathData = ((Element) paths.item(i)).getAttribute("d");
            List<Double> points = Arrays.stream(pathData.split("[A-Za-z,\\s]+"))
                  .filter(value -> !value.isBlank())
                  .map(value -> parseSvgDouble(value, "path data"))
                  .toList();
            if ((points.size() == 20) && points.contains((double) BattlefieldSupportCard.WIDTH - 12)) {
                return points;
            }
        }
        throw new AssertionError("No outer card border found");
    }

    private static List<Double> points(Element element) {
        String pointsAttribute = element.getAttribute("points").strip();
        return Arrays.stream(pointsAttribute.split("[,\\s]+"))
              .map(value -> parseSvgDouble(value, "points attribute"))
              .toList();
    }

    private static double attribute(Element element, String name) {
        String value = element.getAttribute(name);
        return parseSvgDouble(value, "attribute '" + name + "'");
    }

    private static double parseSvgDouble(String value, String context) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new AssertionError("Invalid numeric SVG %s: %s".formatted(context, value), exception);
        }
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
