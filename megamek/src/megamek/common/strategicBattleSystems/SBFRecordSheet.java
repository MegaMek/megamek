/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.strategicBattleSystems;

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.common.Configuration;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * This class represents a Strategic BattleForce Record Sheet. It can be drawn to a Graphics2D context using
 * {@link #drawSheet(Graphics)} and supports printing using {@link Printable}. The font for the headers and
 * other fixed texts can be set using {@link #setFont(Font)}; the font for the formation values can be
 * set independently using {@link #setValueFont(Font)}. The color of the lines under values can be set
 * using {@link #setLineColor(Color)}.
 */
public class SBFRecordSheet implements Printable {

    private final static int WIDTH = 1435;
    private final static int HEIGHT = 2000;
    private final static Color SHADOW_COLOR = new Color(213, 213, 215);
    private static final String FILENAME_BT_LOGO = "BT_Logo_BW.png";
    private static final String FILENAME_CGL_LOGO = "CGL_Logo.png";
    private final static Image BT_LOGO = ImageUtil.loadImageFromFile(
            new MegaMekFile(Configuration.miscImagesDir(), FILENAME_BT_LOGO).toString());
    private final static Image CGL_LOGO = ImageUtil.loadImageFromFile(
            new MegaMekFile(Configuration.miscImagesDir(), FILENAME_CGL_LOGO).toString());
    private static final String COPYRIGHT1 = "(C) 2021 The Topps Company, Inc. BattleTech, 'Mech and BattleMech "
            + "are trademarks of the Topps Company, Inc. All rights reserved.";
    private static final String COPYRIGHT2 = "Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of "
            + "InMediaRes Production, LLC. Permission to photocopy for personal use.";
    private static final double MINIMUM_BORDER_X = 1.0 * 72; // 0.7" in print units of 1/72"
    private static final double MINIMUM_BORDER_Y = 0.2 * 72; // 0.2" in print units of 1/72"
    private static final int VALUE_FONT_SIZE = 21;
    private static final int FORMATION_VALUE_FONT_SIZE = 25;
    private static final int UNIT_VALUE_FONT_SIZE = 23;
    private static final int HEADER_FONT_SIZE = 22;

    private final SBFFormation formation;
    private Color underlineColor = Color.LIGHT_GRAY;
    private Font headerFont;
    private Font valueFont;
    private Font formationValueFont;
    private Font unitValueFont;

    /**
     * Constructs and initializes a Strategic BattleForce Record Sheet for the given formation.
     * When formation is null, an empty record sheet will be drawn.
     */
    public SBFRecordSheet(@Nullable SBFFormation formation) {
        this.formation = formation;
        headerFont = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, HEADER_FONT_SIZE);
        valueFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, VALUE_FONT_SIZE);
        formationValueFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, FORMATION_VALUE_FONT_SIZE);
        unitValueFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, UNIT_VALUE_FONT_SIZE);
    }

    /** Draws the sheet to the given Graphics2D. When the formation is null, an empty sheet is drawn. */
    public final void drawSheet(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        GUIPreferences.AntiAliasifSet(g);
        g2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        drawFormationBackground(g2D);
        drawUnitOverviewBackground(g2D);
        drawElementsBackground(g2D);
        writeCopyrightNotice(g2D);

        if (formation != null) {
            writeFormationValues(g2D);
            writeUnitOverviewValues(g2D);
            for (SBFUnit unit : formation.getUnits()) {
                writeElementValues(g2D, unit);
            }
        }
    }

    /** Sets the font used for all headers written in the sheet to the given newFont. The size of newFont doesn't matter. */
    public void setFont(Font newFont) {
        headerFont = newFont.deriveFont(Font.BOLD).deriveFont((float) HEADER_FONT_SIZE);
    }

    /** Sets the font used for all values written in the sheet to the given newFont. The size of newFont doesn't matter. */
    public void setValueFont(Font newFont) {
        valueFont = newFont.deriveFont((float) VALUE_FONT_SIZE);
        formationValueFont = valueFont.deriveFont((float) FORMATION_VALUE_FONT_SIZE);
        unitValueFont = valueFont.deriveFont((float) UNIT_VALUE_FONT_SIZE);
    }

    /** Sets the color of the underlining under values to the given lineColor. */
    public void setLineColor(Color lineColor) {
        underlineColor = new Color(lineColor.getRGB());
    }

    private void drawFormationBackground(Graphics2D g2D) {
        Graphics2D g = (Graphics2D) g2D.create();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Logo
        ImageIcon icon = new ImageIcon(BT_LOGO.getScaledInstance(722, 125, Image.SCALE_AREA_AVERAGING));
        g.drawImage(icon.getImage(), -23, 10, null);
        ImageIcon iconCGL = new ImageIcon(CGL_LOGO.getScaledInstance(125, 72, Image.SCALE_AREA_AVERAGING));
        g.drawImage(iconCGL.getImage(), 1287, 45, null);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(5f));

        // Header Frame
        int[] pointsX = new int[] { 732, 732, 756, 1401, 1424, 1424, 1401, 756, 732, 732 };
        int[] pointsY = new int[] { 60, 55, 32, 32, 55, 107, 130, 130, 107, 60 };
        g.drawPolyline(pointsX, pointsY, 10);
        new StringDrawer("STRATEGIC BATTLEFORCE").at(1019, 63).maxWidth(440)
                .font(headerFont.deriveFont(28f)).center().draw(g);
        new StringDrawer("FORMATION RECORD SHEET").at(1019, 100).maxWidth(440)
                .font(headerFont.deriveFont(28f)).center().draw(g);

        // Formation frame
        g.setColor(SHADOW_COLOR);
        pointsX = new int[] { 1425, 1435, 1435, 1410, 30, 19, 1402, 1424 };
        pointsY = new int[] { 189, 199, 285, 312, 312, 301, 301, 276};
        g.fillPolygon(pointsX, pointsY, 8);
        g.setColor(Color.BLACK);
        pointsX = new int[] { 104, 1401, 1424, 1424, 1401, 24, 0, 0, 24, 104};
        pointsY = new int[] { 167, 167, 193, 276, 302, 302, 276, 193, 167, 167};
        g.drawPolyline(pointsX, pointsY, 10);
        pointsX = new int[] { 11, 30, 214, 233, 214, 30, 11};
        pointsY = new int[] { 198, 179, 179, 198, 217, 217, 198 };
        g.fillPolygon(pointsX, pointsY, 7);

        new StringDrawer("Type").at(470, 210).maxWidth(58).font(headerFont).center().draw(g);
        new StringDrawer("Size").at(535, 210).maxWidth(58).font(headerFont).center().draw(g);
        new StringDrawer("Move").at(604, 210).maxWidth(68).font(headerFont).center().draw(g);
        new StringDrawer("Jump").at(683, 210).maxWidth(68).font(headerFont).center().draw(g);
        new StringDrawer("Transport").at(762, 185).maxWidth(80).font(headerFont).center().draw(g);
        new StringDrawer("Move").at(762, 210).maxWidth(80).font(headerFont).center().draw(g);
        new StringDrawer("TMM").at(839, 210).maxWidth(65).font(headerFont).center().draw(g);
        new StringDrawer("Tactics").at(919, 210).maxWidth(81).font(headerFont).center().draw(g);
        new StringDrawer("Morale").at(1012, 210).maxWidth(84).font(headerFont).center().draw(g);
        new StringDrawer("Skill").at(1088, 210).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("PV").at(1150, 210).maxWidth(60).font(headerFont).center().draw(g);
        new StringDrawer("Formation Specials").at(1196, 210).maxWidth(210).font(headerFont).centerY().draw(g);

        g.setColor(underlineColor);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(21,  278, 425 ,278);
        g.drawLine(446, 278, 494, 278);
        g.drawLine(511, 278, 559, 278);
        g.drawLine(575, 278, 633, 278);
        g.drawLine(654, 278, 712, 278);
        g.drawLine(727, 278, 797, 278);
        g.drawLine(812, 278, 867, 278);
        g.drawLine(884, 278, 955, 278);
        g.drawLine(972, 278, 1052,278);
        g.drawLine(1064,278, 1113,278);
        g.drawLine(1125,278, 1175,278);
        g.drawLine(1188,278, 1403,278);

        g.setColor(Color.WHITE);
        new StringDrawer("FORMATION:").at(122, 198).maxWidth(176).font(headerFont.deriveFont(32f)).center().draw(g);
        g.dispose();
    }

    private void drawUnitOverviewBackground(Graphics2D g2D) {
        Graphics2D g = (Graphics2D) g2D.create();
        g.translate(0, 325);

        g.setColor(SHADOW_COLOR);
        int[] pointsX = new int[] { 1425, 1435, 1435, 1410, 443, 431, 1402, 1424 };
        int[] pointsY = new int[] { 22,   32,   245,  269,  269, 259, 259,  235 };
        g.fillPolygon(pointsX, pointsY, 8);
        pointsX = new int[] { 30,  20,  390, 400 };
        pointsY = new int[] { 232, 222, 222, 232 };
        g.fillPolygon(pointsX, pointsY, 4);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(5f));
        pointsX = new int[] { 104, 1401, 1424, 1424, 1401, 435, 388, 24,  0,   0,  24, 104 };
        pointsY = new int[] { 0,   0,    26,   235,  259,  259, 222, 222, 196, 26, 0,  0 };
        g.drawPolyline(pointsX, pointsY, 12);

        new StringDrawer("Type").at(388, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("Size").at(451, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("Move").at(514, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("Jump").at(577, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("Trsp").at(639, 17).maxWidth(40).font(headerFont).center().draw(g);
        new StringDrawer("Move").at(639, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("TMM").at(702, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("Arm").at(764, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("S/M/L/E").at(856, 35).maxWidth(96).font(headerFont).center().draw(g);
        new StringDrawer("Skill").at(948, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("PV").at(1010, 35).maxWidth(57).font(headerFont).center().draw(g);
        new StringDrawer("Unit Specials").at(1055, 35).maxWidth(345).font(headerFont).centerY().draw(g);

        g.setColor(underlineColor);
        g.setStroke(new BasicStroke(3f));
        int yStart = 85;
        int delta = 41;
        drawUnitOverviewLines(g, underlineColor, yStart);
        drawUnitOverviewLines(g, underlineColor, yStart + delta);
        drawUnitOverviewLines(g, underlineColor, yStart + delta * 2);
        drawUnitOverviewLines(g, underlineColor, yStart + delta * 3);

        g.setColor(Color.BLACK);
        new StringDrawer("UNITS:").at(21, 31).maxWidth(120).font(headerFont.deriveFont(26f)).centerY().draw(g);
        new StringDrawer("Notes:").at(443, 239).maxWidth(80).font(headerFont).centerY().draw(g);
        g.setColor(underlineColor);
        g.drawLine(524, 248, 1394, 248);
        g.dispose();
    }

    private void drawElementsBackground(Graphics2D g2D) {
        drawElementsBackground(g2D, 0);
        drawElementsBackground(g2D, 1);
        drawElementsBackground(g2D, 2);
        drawElementsBackground(g2D, 3);
    }

    private void drawElementsBackground(Graphics2D g2D, int unitIndex) {
        Graphics2D g = (Graphics2D) g2D.create();
        g.translate(0, 575 + 340 * unitIndex);

        g.setColor(SHADOW_COLOR);
        int[] pointsX = new int[] { 1425, 1435, 1435, 1410, 407, 397, 1402, 1424 };
        int[] pointsY = new int[] { 61,   71,   317,  341,  341, 331, 331,  307 };
        g.fillPolygon(pointsX, pointsY, 8);
        pointsX = new int[] { 30,  20,  376, 386 };
        pointsY = new int[] { 320, 310, 310, 320 };
        g.fillPolygon(pointsX, pointsY, 4);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(5f));
        pointsX = new int[] { 104, 333, 379, 1401, 1424, 1424, 1401, 401, 376, 24,  0,   0,  24, 104 };
        pointsY = new int[] { 0,   0,   39,  39,   65,   307,  331,  331, 310, 310, 286, 26, 0,  0 };
        g.drawPolyline(pointsX, pointsY, 14);

        new StringDrawer("Alpha Strike Elements:").at(21, 77).maxWidth(320).font(headerFont).centerY().draw(g);
        new StringDrawer("Type").at(388, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("Size").at(451, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("Move").at(529, 77).maxWidth(82).font(headerFont).center().draw(g);
        new StringDrawer("Arm").at(607, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("Str").at(669, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("S/M/L/E").at(761, 77).maxWidth(100).font(headerFont).center().draw(g);
        new StringDrawer("OV").at(853, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("Skill").at(915, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("PV").at(977, 77).maxWidth(52).font(headerFont).center().draw(g);
        new StringDrawer("Element Specials").at(1019, 77).maxWidth(320).font(headerFont).centerY().draw(g);

        g.setColor(underlineColor);
        g.setStroke(new BasicStroke(3f));
        int yStart = 129;
        int delta = 32;
        drawElementLines(g, underlineColor, yStart);
        drawElementLines(g, underlineColor, yStart + delta);
        drawElementLines(g, underlineColor, yStart + delta * 2);
        drawElementLines(g, underlineColor, yStart + delta * 3);
        drawElementLines(g, underlineColor, yStart + delta * 4);
        drawElementLines(g, underlineColor, yStart + delta * 5);

        g.setColor(Color.BLACK);
        new StringDrawer("Unit " + indexString(unitIndex) + ":").at(20, 37).maxWidth(90)
                .font(headerFont.deriveFont(26f)).draw(g);
        g.setColor(underlineColor);
        g.drawLine(116, 41, 340, 41);
        g.dispose();
    }

    private String indexString(int index) {
        switch (index) {
            case 0:
                return "One";
            case 1:
                return "Two";
            case 2:
                return "Three";
            default:
                return "Four";
        }
    }

    private void drawUnitOverviewLines(Graphics2D g, Color color, int y) {
        g.setColor(color);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(26, y, 351, y);
        g.drawLine(363, y, 414, y);
        g.drawLine(426, y, 476, y);
        g.drawLine(489, y, 539, y);
        g.drawLine(552, y, 602, y);
        g.drawLine(614, y, 664, y);
        g.drawLine(677, y, 727, y);
        g.drawLine(739, y, 789, y);
        g.drawLine(801, y, 911, y);
        g.drawLine(923, y, 973, y);
        g.drawLine(985, y, 1035, y);
        g.drawLine(1047,y, 1404, y);
    }

    private void drawElementLines(Graphics2D g, Color color, int y) {
        g.setColor(color);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(26 , y, 351, y);
        g.drawLine(363, y, 414, y);
        g.drawLine(426, y, 476, y);
        g.drawLine(489, y, 570, y);
        g.drawLine(582, y, 632, y);
        g.drawLine(644, y, 694, y);
        g.drawLine(706, y, 816, y);
        g.drawLine(828, y, 878, y);
        g.drawLine(890, y, 940, y);
        g.drawLine(952, y, 1002, y);
        g.drawLine(1014,y, 1404, y);
    }

    private void writeFormationValues(Graphics2D g) {
        g.setColor(Color.BLACK);
        new StringDrawer(formation.getName()).at(24, 260).maxWidth(390).font(formationValueFont).centerY().draw(g);
        new StringDrawer(formation.getType() + "").at(470, 260).maxWidth(52).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getSize() + "").at(535, 260).maxWidth(52).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getMovement() + "").at(604, 260).maxWidth(62).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getJumpMove() + "").at(683, 260).maxWidth(62).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getTrspMovement() + "").at(762, 260).maxWidth(80).font(formationValueFont).center().draw(g);
        if (!formation.isAerospace()) {
            new StringDrawer(formation.getTmm() + "").at(839, 260).maxWidth(58).font(formationValueFont).center().draw(g);
        }
        new StringDrawer(formation.getTactics() + "").at(919, 260).maxWidth(72).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getMorale() + "").at(1012, 260).maxWidth(80).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getSkill() + "").at(1088, 260).maxWidth(52).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getPointValue() + "").at(1150, 260).maxWidth(58).font(formationValueFont).center().draw(g);
        new StringDrawer(formation.getSpecialsDisplayString(formation)).at(1196, 260).maxWidth(210).font(formationValueFont).centerY().draw(g);
    }

    private void writeUnitOverviewValues(Graphics2D g2D) {
        Graphics2D g = (Graphics2D) g2D.create();
        g.translate(0, 325);
        g.setColor(Color.BLACK);
        int y = 80;
        for (SBFUnit unit : formation.getUnits()) {
            new StringDrawer(unit.getName()).at(34, y).maxWidth(315).font(unitValueFont).draw(g);
            new StringDrawer(unit.getType() + "").at(388, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getSize() + "").at(451, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getMovement() + "").at(514, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getJumpMove() + "").at(577, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getTrspMovement() + "").at(639, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            if (!unit.isAerospace()) {
                new StringDrawer(unit.getTmm() + "").at(702, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            }
            new StringDrawer(unit.getArmor() + "").at(764, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getDamage() + "").at(856, y).maxWidth(90).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getSkill() + "").at(948, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getPointValue() + "").at(1010, y).maxWidth(52).font(unitValueFont).centerX().draw(g);
            new StringDrawer(unit.getSpecialsDisplayString(unit)).at(1055, y).maxWidth(340).font(unitValueFont).draw(g);
            y += 41;
        }
        g.dispose();
    }

    private void writeElementValues(Graphics2D g2D, SBFUnit unit) {
        Graphics2D g = (Graphics2D) g2D.create();
        g.translate(0, 575 + 340 * formation.getUnits().indexOf(unit));
        g.setColor(Color.BLACK);
        int y = 124;
        for (AlphaStrikeElement element : unit.getElements()) {
            new StringDrawer(element.getName()).at(34, y).maxWidth(315).font(valueFont).draw(g);
            new StringDrawer(element.getASUnitType() + "").at(388, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getSize() + "").at(451, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getMovementAsString() + "").at(529, y).maxWidth(74).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getCurrentArmor() + "").at(607, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getCurrentStructure() + "").at(669, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getStandardDamage() + "").at(761, y).maxWidth(100).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getOV() + "").at(853, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getSkill() + "").at(915, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getPointValue() + "").at(977, y).maxWidth(52).font(valueFont).centerX().draw(g);
            new StringDrawer(element.getSpecialsDisplayString(element)).at(1019, y).maxWidth(380).font(valueFont).draw(g);
            y += 32;
        }
        new StringDrawer(unit.getName()).at(119, 37).maxWidth(215).font(formationValueFont).draw(g);
        g.dispose();
    }

    private void writeCopyrightNotice(Graphics2D g2D) {
        Graphics2D g = (Graphics2D) g2D.create();
        g.translate(0, 600 + 340 * 4);
        g.setColor(Color.BLACK);
        Font noticeFont = headerFont.deriveFont(18f);
        new StringDrawer(COPYRIGHT1).at(WIDTH / 2, 0).maxWidth(1390).font(noticeFont).centerX().draw(g);
        new StringDrawer(COPYRIGHT2).at(WIDTH / 2, 22).maxWidth(1390).font(noticeFont).centerX().draw(g);
        g.dispose();
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex == 0) {
            Graphics2D g = (Graphics2D) graphics;
            double addedXBorder = MINIMUM_BORDER_X > pageFormat.getImageableX() ? MINIMUM_BORDER_X - pageFormat.getImageableX() : 0;
            double addedYBorder = MINIMUM_BORDER_Y > pageFormat.getImageableY() ? MINIMUM_BORDER_Y - pageFormat.getImageableY() : 0;
            double printWidth = pageFormat.getImageableWidth() - addedXBorder;
            double printHeight = pageFormat.getImageableHeight() - addedYBorder;
            double xScale = printWidth / WIDTH;
            double yScale = printHeight / HEIGHT;
            double scale = Math.min(xScale, yScale);
            g.translate(pageFormat.getWidth() / 2 - WIDTH * scale / 2, pageFormat.getHeight() / 2 - HEIGHT * scale / 2);
            g.scale(scale, scale);
            drawSheet(g);
            return Printable.PAGE_EXISTS;
        } else {
            return Printable.NO_SUCH_PAGE;
        }
    }
}