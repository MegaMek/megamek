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

import megamek.MMConstants;
import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.Locale;

import static java.awt.Color.WHITE;

/**
 * This class represents the (graphical) card of an AlphaStrike element as available on the MUL. The unit cards have a
 * standard size of 1050 x 750 but can be resized without loss of quality to any size. A card is created by
 * calling the static {@link #createCard(ASCardDisplayable)}. The basic way to obtain a card image after creating
 * the card is calling the card's {@link #getCardImage()} method. Other methods can be used to configure the
 * resulting card image.
 *
 * This class is responsible for painting Infantry cards. Subclasses of ASCard represent the unit cards for
 * other AlphaStrike unit types. It is not necessary to pay any attention to this when obtaining the card but
 * for Large Aerospace units the specific class can be used to have additional functionality for the two
 * card faces of these cards.
 */
public class ASCard {

    public final static int WIDTH = 1050;
    public final static int HEIGHT = 750;
    public final static double PRINT_SCALE = 3.5 * 72 / WIDTH; // 3.5" wide, 1/72 dots per inch
    protected final static int BORDER = 21;
    protected final static int ARMOR_PIP_SIZE = 22;
    protected final static int DAMAGE_PIP_SIZE = 18;
    protected final static int BOX_WIDTH_WIDE = 608;
    protected final static int BOX_CORNER = 35;
    protected final static float BOX_STROKE = 2.5f;
    protected final static Color DARKGRAY = new Color(128, 128, 128);
    protected final static Color BACKGROUND_GRAY = new Color(209, 211, 212);

    private static final String FILENAME_BT_LOGO = "BT_Logo_BW.png";
    private static final Image btLogo = ImageUtil.loadImageFromFile(
            new MegaMekFile(Configuration.miscImagesDir(), FILENAME_BT_LOGO).toString());

    protected final ASCardDisplayable element;
    protected final Image fluffImage;

    protected Font lightFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 14);
    protected Font boldFont = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 14);
    protected Font blackFont = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 14);

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
    protected StringDrawer.StringDrawerConfig specialsHeaderConfig;

    protected int baseInfoBoxHeight = 99;
    protected int damageBoxY = 287;
    protected int damageBoxHeight = 104;
    protected int armorBoxY = 410;
    protected int armorBoxHeight = 94;
    protected int armorBoxWidth = BOX_WIDTH_WIDE;
    protected int armorPipSpace = 531;
    protected int specialBoxX = 36;
    protected int specialBoxY = 522;
    protected int specialBoxWidth = armorBoxWidth;
    protected int specialBoxHeight = 99;
    protected int fluffXCenter = 823;
    protected int fluffYCenter = 378; // 277
    protected int fluffWidth = 338;
    protected int fluffHeight = 512; //318;

    /**
     * Creates an ASCard for the given AlphaStrike unit (either an AlphaStrikeElement or a MechSummary)
     * which can be used to display a typical AlphaStrike
     * element card. The element can be null in which case the card
     * will contain a message instead of the element's values; the card image will still have the correct size.
     *
     * Note that depending on unit type this method may return an ASCard object or a subclass object but it is not
     * necessary to pay attention to this.
     *
     * @param element The element to display on the card.
     * @return The element's ASCard
     */
    public static ASCard createCard(@Nullable ASCardDisplayable element) {
        if (element != null) {
            if (element.isLargeAerospace()) {
                return new ASLargeAeroCard(element);
            } else if (element.isAerospace()) {
                return new ASAeroCard(element);
            } else if (element.isMek()) {
                return new ASMekCard(element);
            } else if (element.isProtoMek()) {
                return new ASProtoMekCard(element);
            } else if (element.isGround() && !element.isInfantry()) {
                return new ASVehicleCard(element);
            }
        }
        return new ASCard(element);
    }

    /**
     * Creates and returns an image of the AlphaStrike element's unit card.
     * The card is drawn in grayscale standard MUL style.
     * The card is scaled with the given scale, values smaller than 1 making the card smaller.
     * This method always fully draws the card so it always reflects the current state of the
     * AlphaStrike element.
     *
     * @param scale The scaling to apply to the card
     * @return The unit card image
     */
    public BufferedImage getCardImage(float scale) {
        return getCardImage((int) (WIDTH * scale));
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
     * Constructs and initializes the ASCard. Do not use directly, use createCard instead.
     */
    protected ASCard(ASCardDisplayable element) {
        this.element = element;
        fluffImage = getFluffImage(element);
        initialize();
    }

    /** Initializes some values for the card. Overridden for some card types. */
    protected void initialize() { }

    /** Initializes fonts and configs. Overridden for some card types. */
    protected void initializeFonts(Font lightFont, Font boldFont, Font blackFont) {
        modelFont = lightFont.deriveFont(30f);
        chassisFont = blackFont.deriveFont(70f);
        headerFont = boldFont.deriveFont(32f);
        valueFont = blackFont.deriveFont(38f);
        specialsFont = lightFont.deriveFont(22f);
        pointValueHeaderFont = headerFont.deriveFont(40f);
        pointValueFont = valueFont.deriveFont(54f);
        hitsTitleFont = boldFont.deriveFont(16f);
        alphaStrikeStatsFont = blackFont.deriveFont(44f);

        valueConfig = new StringDrawer.StringDrawerConfig().centerY().color(Color.BLACK).font(valueFont)
                .outline(Color.BLACK, 0.95f).dualOutline(WHITE, 3.5f);

        hitsTitleConfig = new StringDrawer.StringDrawerConfig().rightAlign().centerY()
                .color(Color.BLACK).font(hitsTitleFont).outline(Color.BLACK, 0.4f);

        specialsHeaderConfig = new StringDrawer.StringDrawerConfig().color(Color.BLACK).font(specialsFont);
    }

    /** This method controls drawing the card. */
    public final void drawCard(Graphics g) {
        initializeFonts(lightFont, boldFont, blackFont);
        Graphics2D g2D = (Graphics2D) g;
        UIUtil.setHighQualityRendering(g);

        paintCardBackground(g2D, false);

        // No unit or unsupported?
        if (element == null) {
            new StringDrawer("No unit or unit type not supported").at(50, 80).font(pointValueHeaderFont).draw(g);
            return;
        }

        drawFluffImage(g2D);
        paintBaseInfo(g2D);
        paintDamage(g2D);
        paintHeat(g2D);
        paintArmor(g2D);
        paintSpecial(g2D);
        paintPointValue(g2D);
        paintHits(g2D);
        drawModelChassis(g2D);
    }

    /** Scales the fluff image according to control variables which may be set in overriden initialize(). */
    private void drawFluffImage(Graphics2D g) {
        if (fluffImage != null) {
            int width = fluffWidth;
            int height = fluffHeight;
            if ((float) fluffImage.getWidth(null) / fluffWidth >
                    (float) fluffImage.getHeight(null) / fluffHeight) {
                height = fluffImage.getHeight(null) * fluffWidth / fluffImage.getWidth(null);
            } else {
                width = fluffImage.getWidth(null) * fluffHeight / fluffImage.getHeight(null);
            }
            int posX = fluffXCenter - width / 2;
            int posY = fluffYCenter - height / 2;
            ImageIcon icon = new ImageIcon(fluffImage.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING));
            g.drawImage(icon.getImage(), posX, posY, null);
        }
    }

    /** Write model, chassis and squad size. Overridden for some card types. */
    protected void drawModelChassis(Graphics2D g) {
        String model = element.getModel();
        // Remove MM's "(SqdX)" addition to the model
        if (element.getASUnitType().isBattleArmor()) {
            model = model.replaceAll("\\(Sqd[0-9]\\)", "");
        }
        new StringDrawer(model).at(36, 44).font(modelFont).centerY()
                .maxWidth(750).scaleX(1.3f).draw(g);
        new StringDrawer(element.getChassis().toUpperCase(Locale.ROOT)).at(36, 89)
                .font(chassisFont).centerY().maxWidth(770).scaleX(0.8f).draw(g);

        // Add BA Squad Size
        if (element.getASUnitType().isBattleArmor()) {
            new StringDrawer("Squad " + element.getSquadSize()).at(36, 137).maxWidth(500).scaleX(1.3f)
                    .font(modelFont).centerY().draw(g);
        }
    }

    /** Write base info. Overridden for some card types. */
    protected void paintBaseInfo(Graphics2D g) {
        drawBox(g, 36, 170, BOX_WIDTH_WIDE, baseInfoBoxHeight, BACKGROUND_GRAY, BOX_STROKE);

        int upperY = 170 + baseInfoBoxHeight / 2 - 20;
        int lowerY = 170 + baseInfoBoxHeight / 2 + 20;

        new StringDrawer("TP: ").at(44, upperY).centerY().maxWidth(55).font(headerFont).draw(g);
        new StringDrawer(element.getASUnitType().toString()).at(107, upperY).useConfig(valueConfig).maxWidth(64).draw(g);

        new StringDrawer("SZ: ").at(182, upperY).centerY().font(headerFont).maxWidth(56).draw(g);
        new StringDrawer(element.getSize() + "").at(244, upperY).useConfig(valueConfig).maxWidth(33).draw(g);

        new StringDrawer("TMM: ").at(281, upperY).centerY().font(headerFont).maxWidth(94).draw(g);
        new StringDrawer(element.getTMM() + "").at(380, upperY).useConfig(valueConfig).maxWidth(44).draw(g);

        new StringDrawer("MV: ").at(438, upperY).centerY().font(headerFont).maxWidth(62).draw(g);
        new StringDrawer(AlphaStrikeHelper.getMovementAsString(element)).at(505, upperY).useConfig(valueConfig).maxWidth(128).draw(g);

        new StringDrawer("ROLE: ").at(44, lowerY).centerY().font(headerFont).maxWidth(85).draw(g);
        new StringDrawer(element.getRole().toString()).at(138, lowerY).useConfig(valueConfig).maxWidth(250).draw(g);

        new StringDrawer("SKILL: ").at(402, lowerY).centerY().font(headerFont).maxWidth(90).draw(g);
        new StringDrawer(element.getSkill() + "").at(506, lowerY).useConfig(valueConfig).maxWidth(120).draw(g);
    }

    /** Write the ground unit damage block. Overridden for some card types. */
    protected void paintDamage(Graphics2D g) {
        drawBox(g, 36, damageBoxY, BOX_WIDTH_WIDE, damageBoxHeight, BACKGROUND_GRAY, BOX_STROKE);

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

    /** Write the heat block. The subclass for heat tracking cards overrides this to paint content. */
    protected void paintHeat(Graphics2D g) { }

    /** Write the armor block. Overridden for some card types. */
    protected void paintArmor(Graphics2D g) {
        drawBox(g, 36, armorBoxY, armorBoxWidth, armorBoxHeight, BACKGROUND_GRAY, BOX_STROKE);
        if (element != null) {
            // Headers A, S
            int upperY = armorBoxY + armorBoxHeight / 2 - 18;
            int lowerY = armorBoxY + armorBoxHeight / 2 + 18;
            g.setFont(headerFont);
            int headerWidth = new StringDrawer("A:").at(44, upperY).centerY().draw(g).width + 12;
            new StringDrawer("S:").at(44, lowerY).centerY().draw(g);
            paintPipLines(g, 44 + headerWidth, upperY, WHITE, element.getFullArmor());
            paintPipLines(g, 44 + headerWidth, lowerY, DARKGRAY, element.getFullStructure());
        }
    }

    protected void paintPipLines(Graphics2D g, int leftX, int y, Color fillColor, int pipCount) {
        int x = leftX;
        int pipsPerLine = (armorPipSpace - leftX) / ARMOR_PIP_SIZE;
        int pipSize = ARMOR_PIP_SIZE;
        g.setStroke(new BasicStroke(1.5f));
        if (pipCount > pipsPerLine) {
            pipSize = ARMOR_PIP_SIZE - 2;
            y -= ARMOR_PIP_SIZE / 3;
        }
        for (int i = 0; i < Math.min(pipsPerLine, pipCount); i++) {
            g.setColor(fillColor);
            g.fillOval(x, y - pipSize / 2, pipSize, pipSize);
            g.setColor(Color.BLACK);
            g.drawOval(x, y - pipSize / 2, pipSize, pipSize);
            x += ARMOR_PIP_SIZE + 1;
        }
        if (pipCount > pipsPerLine) {
            y += pipSize;
            x = leftX + ARMOR_PIP_SIZE / 2;
            for (int i = 0; i < pipCount - pipsPerLine; i++) {
                g.setColor(fillColor);
                g.fillOval(x, y - pipSize / 2, pipSize, pipSize);
                g.setColor(Color.BLACK);
                g.drawOval(x, y - pipSize / 2, pipSize, pipSize);
                x += ARMOR_PIP_SIZE + 1;
            }
        }
    }

    /** Write the special ability block. Overridden for some card types. */
    protected void paintSpecial(Graphics2D g) {
        drawBox(g, specialBoxX, specialBoxY, specialBoxWidth, specialBoxHeight, BACKGROUND_GRAY, BOX_STROKE);
        paintSpecialTextLines(g, element, specialsFont, specialBoxX + 8, specialBoxY + 2,
                specialBoxWidth - 16, 28);
    }

    /** Helper method that writes the actual special ability text lines. */
    protected void paintSpecialTextLines(Graphics2D g, ASCardDisplayable element, Font font,
                                         int x, int y, int width, int lineHeight) {
        int headerWidth = new StringDrawer("SPECIAL: ").at(x, y + lineHeight)
                .maxWidth(width).useConfig(specialsHeaderConfig).draw(g).width + 10;
        String specials = element.getSpecialAbilities().getSpecialsDisplayString(", ", element);
        int specialsWidth = g.getFontMetrics(font).stringWidth(specials);
        int maxWidth = width;
        int firstLineWidth = width - headerWidth;
        if (specials.isBlank() || specialsWidth < firstLineWidth) {
            new StringDrawer(specials).at(x + headerWidth, y + lineHeight).maxWidth(firstLineWidth).font(font).draw(g);
            return;
        } else if (specialsWidth + headerWidth > 3 * width) {
            // the 1.05 makes lines a little wider to balance line breaks putting too much text in the last line
            width = (int) ((specialsWidth + headerWidth) / 3 * 1.05);
        }

        int line = 1;
        String[] tokens = specials.split(", ");
        int index = 0;
        StringBuilder fittingLine = new StringBuilder();

        while (line <= 3) {
            int lineStart = (line == 1) ? headerWidth : 0;
            while (index < tokens.length) {
                String nextItem = tokens[index] + (index == tokens.length - 1 ? "" : ", ");
                if ((g.getFontMetrics(font).stringWidth(fittingLine + nextItem) + lineStart < width)
                        || fittingLine.toString().isBlank() || (line == 3)) {
                    fittingLine.append(nextItem);
                    index++;
                } else {
                    break;
                }
            }

            new StringDrawer(fittingLine.toString()).at(x + lineStart, y + line * lineHeight)
                    .maxWidth(maxWidth - lineStart).font(font).draw(g);
            fittingLine = new StringBuilder();
            line++;
            if (index == tokens.length) {
                break;
            }
        }
    }

    /** Write the point value. Overridden in Large Aero cards. */
    protected void paintPointValue(Graphics2D g) {
        if (element != null) {
            new StringDrawer("PV: ").at(861, 53).centerY().font(pointValueHeaderFont).maxWidth(72).draw(g);
            new StringDrawer(element.getPointValue() + "").at(941, 53).
                    useConfig(valueConfig).font(pointValueFont).maxWidth(75).draw(g);
        }
    }

    /** Write the critical hits block. Subclasses override to provide content. */
    protected void paintHits(Graphics2D g) { }

    /**
     * Draws a crit damage pip at the provided coordinates (x is left-aligned, y centered).
     *
     * @param g The Graphics2D to draw to
     * @param x the horizontal position (left edge of the damage pip!)
     * @param y the vertical position (center of the damage pip!)
     */
    protected void drawDamagePip(Graphics2D g, int x, int y) {
        g.setStroke(new BasicStroke(3f));
        g.setColor(WHITE);
        g.fillOval(x, y - DAMAGE_PIP_SIZE / 2, DAMAGE_PIP_SIZE, DAMAGE_PIP_SIZE);
        g.setColor(Color.BLACK);
        g.drawOval(x, y - DAMAGE_PIP_SIZE / 2, DAMAGE_PIP_SIZE, DAMAGE_PIP_SIZE);
    }

    /**
     * Draws the card background including the AlphaStrike Stats block. When isFlipSide is true,
     * the point value box and logo are omitted and the copyright notice is positioned
     * differently.
     *
     * @param g The Graphics2D to draw to
     * @param isFlipSide True for the card backside (Large Aerospace units)
     */
    protected void paintCardBackground(Graphics2D g, boolean isFlipSide) {
        g.setColor(WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Alpha Strike Stats box
        g.setColor(BACKGROUND_GRAY);
        int[] pointsX = new int[] { 19, 39, 19 };
        int[] pointsY = new int[] { 699, 730, 730 };
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
                .outline(Color.BLACK, 0.7f).scaleX(1.05f).maxWidth(451).draw(g);

        int copyrightY = 375;
        if (!isFlipSide) {
            copyrightY = 293;
            // Point Value box
            g.setStroke(new BasicStroke(4f));
            g.setColor(BACKGROUND_GRAY);
            pointsX = new int[]{930, 889, 983, 1024};
            pointsY = new int[]{18, 86, 86, 18};
            g.fillPolygon(pointsX, pointsY, 4);
            g.setColor(Color.BLACK);
            pointsX = new int[]{797, 847, 1033};
            pointsY = new int[]{18, 99, 99};
            g.drawPolyline(pointsX, pointsY, 3);
            pointsX = new int[]{814, 855, 1033};
            pointsY = new int[]{18, 86, 86};
            g.drawPolyline(pointsX, pointsY, 3);

            // Logo
            ImageIcon icon = new ImageIcon(btLogo.getScaledInstance(445, 77, Image.SCALE_AREA_AVERAGING));
            g.drawImage(icon.getImage(), 568, 646, null);
        }

        new StringDrawer("(C) " + LocalDate.now().getYear() + " The Topps Company. All rights reserved.").at(1014, copyrightY).rotate(-Math.PI / 2)
                .font(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12)).center().draw(g);

        // Border
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, BORDER);
        g.fillRect(WIDTH - BORDER, 0, BORDER, HEIGHT);
        g.fillRect(0, HEIGHT - BORDER, WIDTH, BORDER);
        g.fillRect(0, 0, BORDER, HEIGHT);
    }

    /**
     * Draws a rounded edge and filled box. The corner radius is fixed to the BOX_CORNER value.
     *
     * @param g The Graphics2D to draw to
     * @param x The left edge of the box
     * @param y The top edge of the box
     * @param width The width of the box
     * @param height The height of the box
     * @param fillColor The color to fill the box width
     * @param borderWidth The width of the black border of the box
     */
    static void drawBox(Graphics2D g, int x, int y, int width, int height, Color fillColor, float borderWidth) {
        g.setStroke(new BasicStroke(borderWidth));
        g.setColor(fillColor);
        g.fillRoundRect(x, y, width, height, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, width, height, BOX_CORNER, BOX_CORNER);
    }

    /** Get the fluff image for the given element. */
    private Image getFluffImage(ASCardDisplayable element) {
        if (element != null) {
            return FluffImageHelper.loadFluffImageHeuristic(element);
        } else {
            return null;
        }
    }
}