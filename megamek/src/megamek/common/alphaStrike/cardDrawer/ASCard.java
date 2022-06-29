/*
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
package megamek.common.alphaStrike.cardDrawer;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.common.Configuration;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * This class represents the (graphical) card of an AlphaStrike element as available on the MUL. The unit cards have a
 * standard size of 1050 x 750 but can be resized without loss of quality to any size. A card is created by
 * calling the static {@link #createCard(AlphaStrikeElement)}. The basic way to obtain a card image after creating
 * the card is calling the card's {@link #getCardImage()} method. Other methods can be used to configure the
 * resulting card image.
 *
 * Subclasses of ASCard represent the unit cards for some AlphaStrike unit types such as Large Aerospace units.
 * It is not necessary to pay any attention to this when obtaining the card.
 */
public class ASCard {

    protected final static int WIDTH = 1050;
    protected final static int HEIGHT = 750;
    protected final static int BORDER = 21;
    protected final static int BOX_INSET = 8;
    protected final static int ARMOR_PIP_SIZE = 22;
    protected final static int DAMAGE_PIP_SIZE = 18;
    protected final static int BOX_WIDTH_WIDE = 608;
    protected final static int BOX_CORNER = 35;
    protected final static float BOX_STROKE = 2.5f;
    protected final static Color DARKGRAY = new Color(128, 128, 128);

    private static final String FILENAME_BT_LOGO = "BT_Logo_BW.png";
    private static final Image btLogo = ImageUtil.loadImageFromFile(
            new MegaMekFile(Configuration.miscImagesDir(), FILENAME_BT_LOGO).toString());

    protected final AlphaStrikeElement element;
    protected final Image fluffImage;

    protected Font lightFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    protected Font boldFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    protected Font blackFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    protected Font modelFont;
    protected Font chassisFont;
    protected Font headerFont;
    protected Font valueFont;
    protected Font specialsFont;
    protected Font pointValueHeaderFont;
    protected Font pointValueFont;
    protected Font hitsTitleFont;
    protected Font alphaStrikeStatsFont;

    protected StringDrawer.StringDrawerConfig valueConfig;
    protected StringDrawer.StringDrawerConfig hitsTitleConfig;

    protected int baseInfoBoxHeight = 99;
    protected int damageBoxY = 287;
    protected int damageBoxHeight = 104;
    protected int armorBoxY = 410;
    protected int armorBoxHeight = 94;
    protected int armorBoxWidth = 531;
    protected int specialBoxX = 36;
    protected int specialBoxY = 522;
    protected int specialBoxWidth = armorBoxWidth;
    protected int specialBoxHeight = 99;

    /**
     * Creates an ASCard for the given element whih can be used to display a typical AlphaStrike
     * element card. The element can be null in which case the card
     * will contain a message instead of the element's values; the card image will still have the correct size.
     *
     * Note that depending on unit type this method may return an ASCard object or a subclass object but it is not
     * necessary to pay attention to this.
     *
     * @param element The element to display on the card.
     * @return The element's ASCard
     */
    public static ASCard createCard(@Nullable AlphaStrikeElement element) {
        if (element != null) {
            if (element.isLargeAerospace()) {
                return new ASLargeAeroCard(element);
            } else if (element.isAerospace()) {
                return new ASAeroCard(element);
            } else if (element.isMek()) {
                return new ASMekCard(element);
            }
        }
        return new ASCard(element);
    }

    /**
     * Creates and returns an image of the AlphaStrike element's unit card.
     * The card is drawn in grayscale standard MUL style.
     * The card is scaled to fit the given width.
     * This method always fully draws the card so it always reflects the current state of the
     * AlphaStrike element.
     *
     * @param width The desired width of the card in pixels
     * @return The unit card image
     */
    public BufferedImage getCardImage(int width) {
        int height = HEIGHT * width / WIDTH;
        final BufferedImage result = ImageUtil.createAcceleratedImage(width, height);
        final Graphics graphics = result.getGraphics();
        Graphics2D g2D = (Graphics2D) graphics;
        g2D.scale((float) width / WIDTH, (float) width / WIDTH);
        drawCard(graphics);
        graphics.dispose();
        return result;
    }

    /**
     * Creates and returns an image of the AlphaStrike element's unit card.
     * The card is drawn in grayscale standard MUL style.
     * The card will have the standard MUL dimension (1050 x 750 pixels) for all unit types
     * except Large Aerospace units. For those, the two card faces are stacked vertically and the resulting
     * card image has a size of 1050 x 1500.
     * This method always fully draws the card so it always reflects the current state of the
     * AlphaStrike element.
     *
     * @return The unit card image
     */
    public BufferedImage getCardImage() {
        return getCardImage(WIDTH);
    }

    public void setFont(Font newFont) {
        lightFont = newFont;
        boldFont = newFont.deriveFont(Font.BOLD);
        blackFont = newFont.deriveFont(Font.BOLD);
    }

    /**
     * Constructs and initializes the ASCard. Hidden so that the static createCard is used instead.
     */
    protected ASCard(AlphaStrikeElement element) {
        this.element = element;
        Image image = getFluffImage(element);
        if (image != null) {
            if (image.getWidth(null) > image.getHeight(null)) {
                image = image.getScaledInstance(290, -1, Image.SCALE_SMOOTH);
            } else {
                image = image.getScaledInstance(-1, 290, Image.SCALE_SMOOTH);
            }
//            var ii = new ImageIcon(image);
            fluffImage = new ImageIcon(image).getImage();
        } else
            fluffImage = null;
        initialize();
    }

    /**
     * Initializes some values for the card and sets standard fonts. Overrides should call super.
     */
    protected void initialize() {
        if (element != null) {
            if (element.isInfantry()) {
                armorBoxWidth = BOX_WIDTH_WIDE;
                specialBoxWidth = BOX_WIDTH_WIDE;
            }
            if (element.tracksHeat()) {
                setHeatTrackingLayout();
            }
        }
    }

    protected void setHeatTrackingLayout() {
        baseInfoBoxHeight = 84;
        damageBoxY = 268;
        damageBoxHeight = 88;
        armorBoxY = 442;
        armorBoxHeight = 79;
        specialBoxY = 537;
        specialBoxHeight = 84;
    }

    protected void initializeFonts(Font lightFont, Font boldFont, Font blackFont) {
        modelFont = lightFont.deriveFont(30f);
        chassisFont = blackFont.deriveFont(70f);
        headerFont = boldFont.deriveFont(32f);
        valueFont = blackFont.deriveFont(38f);
        specialsFont = lightFont.deriveFont(22f);
        pointValueHeaderFont = headerFont.deriveFont(40f);
        pointValueFont = valueFont.deriveFont(54f);
        hitsTitleFont = boldFont.deriveFont(16f);
        alphaStrikeStatsFont = blackFont.deriveFont(40f);

        valueConfig = new StringDrawer.StringDrawerConfig().centerY().color(Color.BLACK).font(valueFont)
                .outline(Color.BLACK, 0.95f).dualOutline(Color.WHITE, 3.5f);

        hitsTitleConfig = new StringDrawer.StringDrawerConfig().rightAlign().centerY()
                .color(Color.BLACK).font(hitsTitleFont).outline(Color.BLACK, 0.4f);
    }

    private void drawCard(Graphics g) {
        initializeFonts(lightFont, boldFont, blackFont);
        Graphics2D g2D = (Graphics2D) g;
        GUIPreferences.AntiAliasifSet(g);
        g2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        paintCardBackground(g2D);

        // No unit or unsupported?
        if (element == null) {
            new StringDrawer("No unit or unit type not supported").at(50, 80).font(pointValueHeaderFont).draw(g);
            return;
        }

        drawCardContent(g2D);
    }

    protected void drawCardContent(Graphics2D g2D) {
        // Fluff Image
        if (fluffImage != null) {
            int posX = 825 - fluffImage.getWidth(null) / 2;
            int posY = 285 - fluffImage.getHeight(null) / 2;
            g2D.drawImage(fluffImage, posX, posY, null);
        }

        // Data blocks
        paintBaseInfo(g2D);
        paintDamage(g2D);

        if (element.tracksHeat()) {
            paintHeat(g2D);
        }

        paintArmor(g2D);
        paintSpecial(g2D);
        paintPointValue(g2D);
        paintHits(g2D);

        // Model and Chassis
        new StringDrawer(element.getModel()).at(36, 44).font(modelFont).centerY()
                .maxWidth(750).scaleX(1.3f).draw(g2D);
        new StringDrawer(element.getChassis().toUpperCase(Locale.ROOT)).at(36, 89)
                .font(chassisFont).centerY().maxWidth(770).scaleX(0.8f).draw(g2D);

        // BA Squad Size
        if (element.isBattleArmor()) {
            new StringDrawer("Squad " + element.getSquadSize()).at(36, 137).font(modelFont).centerY().draw(g2D);
        }
    }

    protected void paintBaseInfo(Graphics2D g) {
        drawBox(g, 36, 170, BOX_WIDTH_WIDE, baseInfoBoxHeight, Color.LIGHT_GRAY, BOX_STROKE);

        int upperY = 170 + baseInfoBoxHeight / 2 - 20;
        int lowerY = 170 + baseInfoBoxHeight / 2 + 20;

        new StringDrawer("TP: ").at(44, upperY).centerY().maxWidth(55).font(headerFont).draw(g);
        new StringDrawer(element.getType().toString()).at(107, upperY).useConfig(valueConfig).maxWidth(64).draw(g);

        new StringDrawer("SZ: ").at(182, upperY).centerY().font(headerFont).maxWidth(56).draw(g);
        new StringDrawer(element.getSize() + "").at(244, upperY).useConfig(valueConfig).maxWidth(33).draw(g);

        new StringDrawer("TMM: ").at(281, upperY).centerY().font(headerFont).maxWidth(94).draw(g);
        new StringDrawer(element.getTMM() + "").at(380, upperY).useConfig(valueConfig).maxWidth(44).draw(g);

        new StringDrawer("MV: ").at(438, upperY).centerY().font(headerFont).maxWidth(62).draw(g);
        new StringDrawer(element.getMovementAsString()).at(505, upperY).useConfig(valueConfig).maxWidth(128).draw(g);

        new StringDrawer("ROLE: ").at(44, lowerY).centerY().font(headerFont).maxWidth(85).draw(g);
        new StringDrawer(element.getRole().toString()).at(138, lowerY).useConfig(valueConfig).maxWidth(250).draw(g);

        new StringDrawer("SKILL: ").at(402, lowerY).centerY().font(headerFont).maxWidth(98).draw(g);
        new StringDrawer(element.getSkill() + "").at(506, lowerY).useConfig(valueConfig).maxWidth(120).draw(g);
    }

    protected void paintDamage(Graphics2D g) {
        drawBox(g, 36, damageBoxY, BOX_WIDTH_WIDE, damageBoxHeight, Color.LIGHT_GRAY, BOX_STROKE);

        new StringDrawer("DAMAGE").at(36 + 19, damageBoxY + damageBoxHeight / 2).center()
                .rotate(-Math.PI / 2).font(hitsTitleFont).maxWidth(70).draw(g);

        int upperY = damageBoxY + damageBoxHeight / 2 - 20;
        int lowerY = damageBoxY + damageBoxHeight / 2 + 22;
        int delta = 195;
        int posS = 150;
        ASDamageVector damage = element.getStandardDamage();
        g.setFont(headerFont);
        new StringDrawer("S (+0)").at(posS, upperY).center().maxWidth(145).draw(g);
        new StringDrawer("M (+2)").at(posS + delta, upperY).center().maxWidth(145).draw(g);
        new StringDrawer("L (+4)").at(posS + 2 * delta, upperY).center().maxWidth(145).draw(g);
        g.setFont(valueFont);
        new StringDrawer(damage.S.toStringWithZero()).at(posS, lowerY).useConfig(valueConfig).center().draw(g);
        new StringDrawer(damage.M.toStringWithZero()).at(posS + delta, lowerY).useConfig(valueConfig).center().draw(g);
        new StringDrawer(damage.L.toStringWithZero()).at(posS + 2 * delta, lowerY).useConfig(valueConfig).center().draw(g);
    }

    private void paintHeat(Graphics2D g) {
        int height = 54;
        drawBox(g, 36, 372, BOX_WIDTH_WIDE, height, Color.LIGHT_GRAY, BOX_STROKE);

        g.drawLine(36 + 141, 372 + 12, 36 + 141, 372 + height - 12);
        // Heat Scale Box
        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(36 + 377, 372 + 6, 210, height - 12, BOX_CORNER / 2, BOX_CORNER / 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(36 + 377, 372 + 6, 210, height - 12, BOX_CORNER / 2, BOX_CORNER / 2);
        g.drawLine(36 + 429, 372 + 7, 36 + 429, 372 + height - 7);
        g.drawLine(36 + 482, 372 + 7, 36 + 482, 372 + height - 7);
        g.drawLine(36 + 535, 372 + 7, 36 + 535, 372 + height - 7);
        int ym = 372 + height / 2;
        g.setFont(headerFont);
        g.setColor(Color.WHITE);
        new StringDrawer("1").at(36 + 403, ym).center().draw(g);
        new StringDrawer("2").at(36 + 455, ym).center().draw(g);
        new StringDrawer("3").at(36 + 508, ym).center().draw(g);
        new StringDrawer("S").at(36 + 561, ym).center().draw(g);
        g.setColor(Color.BLACK);

        if (element != null) {
            new StringDrawer("OV:").at(49, ym).centerY().font(headerFont).maxWidth(45).draw(g);
            new StringDrawer(element.getOverheat() + "").at(111, ym).useConfig(valueConfig).maxWidth(54).draw(g);
            new StringDrawer("HEAT SCALE").at(193, ym).centerY().font(headerFont).maxWidth(208).draw(g);
        }
    }

    protected void paintArmor(Graphics2D g) {
        drawBox(g, 36, armorBoxY, armorBoxWidth, armorBoxHeight, Color.LIGHT_GRAY, BOX_STROKE);

        if (element != null) {
            // Headers A, S
            int upperY = armorBoxY + armorBoxHeight / 2 - 18;
            int lowerY = armorBoxY + armorBoxHeight / 2 + 18;
            g.setFont(headerFont);
            int headerWidth = new StringDrawer("A:").at(44, upperY).centerY().draw(g).width + 12;
            new StringDrawer("S:").at(44, lowerY).centerY().draw(g);

            // Armor Pips
            int cx = 44 + headerWidth;
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < element.getArmor(); i++) {
                g.setColor(Color.WHITE);
                g.fillOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }

            // Structure Pips
            cx = 44 + headerWidth;
            for (int i = 0; i < element.getStructure(); i++) {
                g.setColor(DARKGRAY);
                g.fillOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }
        }
    }

    private void paintSpecial(Graphics2D g) {
        drawBox(g, specialBoxX, specialBoxY, specialBoxWidth, specialBoxHeight, Color.LIGHT_GRAY, BOX_STROKE);
        paintSpecialTextLines(g, element, specialsFont, specialBoxX + BOX_INSET, specialBoxY + 2,
                specialBoxWidth - 2 * BOX_INSET);
    }

    void paintSpecialTextLines(Graphics2D g, AlphaStrikeElement element, Font font, int x, int y, int width) {
        if (element != null) {
            String specials = "SPECIAL: " + element.getSpecialsString(",").replace(",", ", ");
            g.setFont(font);
            int ascent = g.getFontMetrics(font).getAscent();

            if (element.getSpecialsString().isBlank() ||
                    (g.getFontMetrics(font).stringWidth(specials) < width)) {
                g.drawString(specials, x, y + ascent);
                return;
            }

            int linedelta = g.getFontMetrics(font).getHeight();
            int line = 1;
            String[] tokens = element.getSpecialsString(",").split(",");
            int index = 0;
            String fittingLine = "SPECIAL: ";

            while (line <= 3) {
                while (index < tokens.length) {
                    String nextItem = tokens[index] + (index == tokens.length - 1 ? "" : ", ");
                    if ((g.getFontMetrics(font).stringWidth(fittingLine + nextItem) < width)
                            || fittingLine.isBlank()) {
                        fittingLine += nextItem;
                        index++;
                    } else {
                        break;
                    }
                }

                g.drawString(fittingLine, x, y + ascent + (line - 1) * linedelta);
                fittingLine = "";
                line++;
                if (index == tokens.length) {
                    break;
                }
            }
        }
    }

    private void paintPointValue(Graphics2D g) {
        if (element != null) {
            new StringDrawer("PV: ").at(861, 53).centerY().font(pointValueHeaderFont).maxWidth(72).draw(g);
            new StringDrawer(element.getPointValue() + "").at(941, 53).
                    useConfig(valueConfig).font(pointValueFont).maxWidth(75).draw(g);
        }
    }

    protected void paintHits(Graphics2D g) {
        if (element.isProtoMek()) {
            paintProtoMekHits(g);
        } else if (element.isGround() && !element.isInfantry()) {
            paintCombatVeeHits(g);
        }
    }

    private void paintProtoMekHits(Graphics2D g) {
        drawBox(g, 591, 442, 422, 180, Color.LIGHT_GRAY, BOX_STROKE);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(802, 470).center().font(headerFont).maxWidth(380).draw(g);

            new StringDrawer("FIRE CONTROL").at(722, 510).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 510).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 510);
            drawDamagePip(g, 755, 510);
            drawDamagePip(g, 782, 510);
            drawDamagePip(g, 809, 510);

            new StringDrawer("MP").at(722, 552).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("1/2 MV Each").at(834, 552).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 552);
            drawDamagePip(g, 755, 552);
            drawDamagePip(g, 782, 552);
            drawDamagePip(g, 809, 552);

            new StringDrawer("WEAPONS").at(722, 593).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 593).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 593);
            drawDamagePip(g, 755, 593);
            drawDamagePip(g, 782, 593);
            drawDamagePip(g, 809, 593);
        }
    }

    private void paintCombatVeeHits(Graphics2D g) {
        drawBox(g, 591, 442, 422, 180, Color.LIGHT_GRAY, BOX_STROKE);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(802, 470).center().font(headerFont).maxWidth(380).draw(g);

            new StringDrawer("ENGINE").at(722, 509).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("1/2 MV and Damage").at(754, 509).centerY().font(specialsFont).maxWidth(248).draw(g);
            drawDamagePip(g, 728, 509);

            new StringDrawer("FIRE CONTROL").at(722, 538).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 538).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 538);
            drawDamagePip(g, 755, 538);
            drawDamagePip(g, 782, 538);
            drawDamagePip(g, 809, 538);

            new StringDrawer("WEAPONS").at(722, 565).useConfig(hitsTitleConfig).maxWidth(120).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 565).centerY().font(specialsFont).maxWidth(168).draw(g);
            drawDamagePip(g, 728, 565);
            drawDamagePip(g, 755, 565);
            drawDamagePip(g, 782, 565);
            drawDamagePip(g, 809, 565);

            new StringDrawer("MOTIVE").at(663, 593).useConfig(hitsTitleConfig).maxWidth(64).draw(g);
            drawDamagePip(g, 673, 593);
            drawDamagePip(g, 700, 593);
            new StringDrawer("-2 MV").at(724, 593).centerY().font(specialsFont).maxWidth(62).draw(g);
            drawDamagePip(g, 793, 593);
            drawDamagePip(g, 820, 593);
            new StringDrawer("1/2 MV").at(841, 593).centerY().font(specialsFont).maxWidth(64).draw(g);
            drawDamagePip(g, 919, 593);
            new StringDrawer("0 MV").at(944, 593).centerY().font(specialsFont).maxWidth(57).draw(g);
        }
    }

    protected void drawDamagePip(Graphics2D g, int x, int y) {
        g.setStroke(new BasicStroke(3f));
        g.setColor(Color.WHITE);
        g.fillOval(x, y - DAMAGE_PIP_SIZE / 2, DAMAGE_PIP_SIZE, DAMAGE_PIP_SIZE);
        g.setColor(Color.BLACK);
        g.drawOval(x, y - DAMAGE_PIP_SIZE / 2, DAMAGE_PIP_SIZE, DAMAGE_PIP_SIZE);
    }

    protected void paintCardBackground(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Point Value box
        g.setStroke(new BasicStroke(4f));
        g.setColor(Color.LIGHT_GRAY);
        int[] pointsX = new int[] { 930, 889, 983, 1024 };
        int[] pointsY = new int[] { 18, 86, 86, 18 };
        g.fillPolygon(pointsX, pointsY, 4);
        g.setColor(Color.BLACK);
        pointsX = new int[] { 797, 847, 1033 };
        pointsY = new int[] { 18, 99, 99 };
        g.drawPolyline(pointsX, pointsY, 3);
        pointsX = new int[] { 814, 855, 1033 };
        pointsY = new int[] { 18, 86, 86 };
        g.drawPolyline(pointsX, pointsY, 3);

        // Alpha Strike Stats box
        g.setColor(Color.LIGHT_GRAY);
        pointsX = new int[] { 19, 39, 19 };
        pointsY = new int[] { 699, 730, 730 };
        g.fillPolygon(pointsX, pointsY, 3);
        pointsX = new int[] { 112, 153, 247, 206 };
        pointsY = new int[] { 664, 732, 732, 664 };
        g.fillPolygon(pointsX, pointsY, 4);
        pointsX = new int[] { 315, 356, 450, 409 };
        pointsY = new int[] { 664, 732, 732, 664 };
        g.fillPolygon(pointsX, pointsY, 4);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(4f));
        pointsX = new int[] { 2, 506, 553 };
        pointsY = new int[] { 649, 649, 729 };
        g.drawPolyline(pointsX, pointsY, 3);
        pointsX = new int[] { 2, 495, 533 };
        pointsY = new int[] { 664, 664, 729 };
        g.drawPolyline(pointsX, pointsY, 3);
        new StringDrawer("ALPHA STRIKE STATS").at(38, 712).font(alphaStrikeStatsFont)
                .outline(Color.BLACK, 0.5f).scaleX(1.05f).maxWidth(451).draw(g);

        // Border
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, BORDER);
        g.fillRect(WIDTH - BORDER, 0, BORDER, HEIGHT);
        g.fillRect(0, HEIGHT - BORDER, WIDTH, BORDER);
        g.fillRect(0, 0, BORDER, HEIGHT);

        // Copyright
        new StringDrawer("(C) 2022 The Topps Company. All rights reserved.").at(1014, 293).rotate(-Math.PI / 2)
                .font(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).center().draw(g);

        g.drawImage(btLogo, 568, 646, 445, 77, null);
    }

    static void drawBox(Graphics2D g, int x, int y, int width, int height, Color fillColor, float borderWidth) {
        g.setStroke(new BasicStroke(borderWidth));
        g.setColor(fillColor);
        g.fillRoundRect(x, y, width, height, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, width, height, BOX_CORNER, BOX_CORNER);
    }

    /**
     * Get the fluff image for the given element.
     */
    private Image getFluffImage(AlphaStrikeElement element) {
        if (element == null) {
            return null;
        }
        Image image = FluffImageHelper.loadFluffImageHeuristic(element);
        if (image != null) {
            MediaTracker tracker = new MediaTracker(null);
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
        return image;
    }

}
