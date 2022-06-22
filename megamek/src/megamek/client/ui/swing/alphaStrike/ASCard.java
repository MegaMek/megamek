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
package megamek.client.ui.swing.alphaStrike;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

import static megamek.common.alphaStrike.ASUnitType.*;

public class ASCard extends JComponent {

    private final static int WIDTH = 1050;
    private final static int HEIGHT = 750;
    private final static int BORDER = 21;
    private final static int PADDING = 15;
    private final static int BOX_INSET = 8;
    private final static float BOX_STROKE = 2.5f;
    private final static int ARMOR_PIP_SIZE = 24;

    private final static int BOX_WIDTH_MEK_WIDE = 608;
    private final static int BOX_CORNER = 30;

    private final static int BOX_HEAT_HEIGHT = 54;

    private final static Color DARKGRAY = new Color(128, 128, 128);

    private AlphaStrikeElement element;

    Font baseFont = new Font("Eurosti", Font.PLAIN, 14);
    Font boldFont = baseFont.deriveFont(Font.BOLD);
    Font modelFont = baseFont.deriveFont(30f);
    Font chassisFont = boldFont.deriveFont(70f);
    Font headerFont = boldFont.deriveFont(32f);
    Font font = boldFont.deriveFont(35f);
    Font specialsFont = baseFont.deriveFont(20f);
    Font pointValueHeaderFont = headerFont.deriveFont(40f);
    Font pointValueFont = font.deriveFont(54f);
    Font smallerFont = boldFont.deriveFont(16f);

    private int baseInfoBoxHeight;
    private int damageBoxY;
    private int damageBoxHeight;
    private int armorBoxY;
    private int armorBoxHeight;
    private int armorBoxWidth;
    private int specialBoxY;
    private int specialBoxWidth;
    private int specialBoxHeight;

    StringDrawer.StringDrawerConfig valueConfig = new StringDrawer.StringDrawerConfig().centerY()
            .color(Color.BLACK).font(font).outline(Color.WHITE, 1f);

    public ASCard() { }

    public ASCard(@Nullable AlphaStrikeElement element) {
        this.element = element;
        initializeDimensions();
    }

    public void setASElement(@Nullable AlphaStrikeElement element) {
        this.element = element;
        initializeDimensions();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    private void initializeDimensions() {
        baseInfoBoxHeight = 84;
        damageBoxY = 268;
        damageBoxHeight = 88;
        armorBoxY = 442;
        armorBoxHeight = 79;
        armorBoxWidth = 531;
        specialBoxY = 537;
        specialBoxWidth = armorBoxWidth;
        specialBoxHeight = 84;

        if (element != null) {
            if (!element.tracksHeat()) {
                baseInfoBoxHeight = 99;
                damageBoxY = 287;
                damageBoxHeight = 104;
                armorBoxY = 410;
                armorBoxHeight = 94;
                specialBoxY = 522;
                specialBoxHeight = 99;
            }

            if (element.usesThreshold() || element.isInfantry()) {
                armorBoxWidth = BOX_WIDTH_MEK_WIDE;
            }

            if (element.isInfantry()) {
                specialBoxWidth = BOX_WIDTH_MEK_WIDE;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2D = (Graphics2D) g;
        GUIPreferences.AntiAliasifSet(g);
        g2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2D.setColor(Color.WHITE);
        g2D.fillRect(0, 0, WIDTH, HEIGHT);

        // Point Value box
        g2D.setStroke(new BasicStroke(BOX_STROKE));
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
        g2D.setColor(Color.BLACK);
        g2D.setStroke(new BasicStroke(2.5f));
        pointsX = new int[] { 2, 506, 553 };
        pointsY = new int[] { 649, 649, 729 };
        g2D.drawPolyline(pointsX, pointsY, 3);
        pointsX = new int[] { 2, 495, 533 };
        pointsY = new int[] { 664, 664, 729 };
        g2D.drawPolyline(pointsX, pointsY, 3);
        new StringDrawer("ALPHA STRIKE STATS").at(38, 712).font(pointValueHeaderFont).draw(g);

        // Border
        g2D.setColor(Color.BLACK);
        g2D.fillRect(0, 0, WIDTH, BORDER);
        g2D.fillRect(WIDTH - BORDER, 0, BORDER, HEIGHT);
        g2D.fillRect(0, HEIGHT - BORDER, WIDTH, BORDER);
        g2D.fillRect(0, 0, BORDER, HEIGHT);

        // Copyright
        new StringDrawer("(C) 2018 The Topps Company. All rights reserved.").at(1014, 293).rotate(-Math.PI / 2)
                .font(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).center().draw(g);

        // No unit or unsupported?
        if (element == null) {
            new StringDrawer("No unit or unit type not supported").at(50, 80).font(pointValueHeaderFont).draw(g);
            return;
        }

        // Data blocks
        paintBaseInfo(g2D, BORDER + PADDING, 170);
        paintDamage(g2D, BORDER + PADDING);
        if (element.tracksHeat()) {
            paintHeat(g2D, BORDER + PADDING, 372);
        }
        if (element.isAerospace()) {
            paintAeroArmor(g2D, BORDER + PADDING);
        } else {
            paintArmor(g2D, BORDER + PADDING);
        }
        paintSpecial(g2D);
        paintPointValue(g2D);
        if (element.isAnyTypeOf(BM, IM)) {
            paintMekDamage(g2D);
        } else if (element.isFighter()) {
            paintAeroDamage(g2D);
        } else if (element.isType(PM)) {
            paintProtoMekDamage(g2D);
        } else if (element.isGround() && !element.isInfantry()) {
            paintCombatVeeDamage(g2D);
        }
        paintFluffImage(g2D);

        // Model
        new StringDrawer(element.getModel()).at(BORDER + PADDING, 44).font(modelFont).centerY().draw(g);

        // Chassis
        new StringDrawer(element.getChassis()).at(BORDER + PADDING, 89).font(chassisFont).centerY().draw(g);

        // BA Squad Size
        if (element.isType(BA)) {
            new StringDrawer("Squad " + element.getSquadSize()).at(BORDER + PADDING, 137).font(modelFont).centerY().draw(g);
        }
    }

    private void paintBaseInfo(Graphics2D g, int x, int y) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(x, y, BOX_WIDTH_MEK_WIDE, baseInfoBoxHeight, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, BOX_WIDTH_MEK_WIDE, baseInfoBoxHeight, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            int upperY = y + baseInfoBoxHeight / 2 - 20;
            int lowerY = y + baseInfoBoxHeight / 2 + 20;

            // Type
            int width = new StringDrawer("TP: ").at(x + BOX_INSET, upperY).centerY().font(headerFont).draw(g).width;
            new StringDrawer(element.getType().toString()).at(x + BOX_INSET + width, upperY).useConfig(valueConfig).draw(g);

            // Size
            int posX = 147;
            width = new StringDrawer("SZ: ").at(x + BOX_INSET + posX, upperY).centerY().font(headerFont).draw(g).width;
            new StringDrawer(element.getSize() + "").at(x + BOX_INSET + posX + width, upperY).useConfig(valueConfig).draw(g);

            if (element.isGround()) {
                // TMM
                posX = 246;
                width = new StringDrawer("TMM: ").at(x + BOX_INSET + posX, upperY).centerY().font(headerFont).draw(g).width;
                new StringDrawer(element.getTMM() + "").at(x + BOX_INSET + posX + width, upperY).useConfig(valueConfig).draw(g);

                // MV
                posX = 402;
                width = new StringDrawer("MV: ").at(x + BOX_INSET + posX, upperY).centerY().font(headerFont).draw(g).width;
                new StringDrawer(element.getMovementAsString()).at(x + BOX_INSET + posX + width, upperY).useConfig(valueConfig).draw(g);
            } else {
                // Thrust
                posX = 246;
                width = new StringDrawer("THR: ").at(x + BOX_INSET + posX, upperY).centerY().font(headerFont).draw(g).width;
                new StringDrawer(element.getMovementAsString()).at(x + BOX_INSET + posX + width, upperY).useConfig(valueConfig).draw(g);
            }

            // Role
            width = new StringDrawer("ROLE: ").at(x + BOX_INSET, lowerY).centerY().font(headerFont).draw(g).width;
            new StringDrawer(element.getRole().toString()).at(x + BOX_INSET + width, lowerY).useConfig(valueConfig).draw(g);

            // Skill
            posX = 367;
            width = new StringDrawer("SKILL: ").at(x + BOX_INSET + posX, lowerY).centerY().font(headerFont).draw(g).width;
            new StringDrawer(element.getSkill() + "").at(x + BOX_INSET + posX + width, lowerY).useConfig(valueConfig).draw(g);
        }
    }

    private void paintDamage(Graphics2D g, int x) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(BORDER + PADDING, damageBoxY, BOX_WIDTH_MEK_WIDE, damageBoxHeight, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(BORDER + PADDING, damageBoxY, BOX_WIDTH_MEK_WIDE, damageBoxHeight, BOX_CORNER, BOX_CORNER);

        new StringDrawer("DAMAGE").at(BORDER + PADDING + 19, damageBoxY + damageBoxHeight / 2).center()
                .rotate(-Math.PI / 2).font(smallerFont).draw(g);

        if (element != null) {
            if (!element.usesArcs()) {
                int upperY = damageBoxY + damageBoxHeight / 2 - 22;
                int lowerY = damageBoxY + damageBoxHeight / 2 + 22;
                if (element.usesSML()) {
                    int delta = 195;
                    int posS = 150;
                    ASDamageVector damage = element.getStandardDamage();
                    g.setFont(headerFont);
                    new StringDrawer("S (+0)").at(posS, upperY).center().draw(g);
                    new StringDrawer("M (+2)").at(posS + delta, upperY).center().draw(g);
                    new StringDrawer("L (+4)").at(posS + 2 * delta, upperY).center().draw(g);
                    g.setFont(font);
                    new StringDrawer(damage.S.toStringWithZero()).at(posS, lowerY).useConfig(valueConfig).center().draw(g);
                    new StringDrawer(damage.M.toStringWithZero()).at(posS + delta, lowerY).useConfig(valueConfig).center().draw(g);
                    new StringDrawer(damage.L.toStringWithZero()).at(posS + 2 * delta, lowerY).useConfig(valueConfig).center().draw(g);
                } else {
                    int delta = 150;
                    int posS = 120;
                    ASDamageVector damage = element.getStandardDamage();
                    g.setFont(headerFont);
                    new StringDrawer("S (+0)").at(posS, upperY).center().draw(g);
                    new StringDrawer("M (+2)").at(posS + delta, upperY).center().draw(g);
                    new StringDrawer("L (+4)").at(posS + 2 * delta, upperY).center().draw(g);
                    new StringDrawer("E (+6)").at(posS + 3 * delta, upperY).center().draw(g);
                    g.setFont(font);
                    new StringDrawer(damage.S.toStringWithZero()).at(posS, lowerY).useConfig(valueConfig).center().draw(g);
                    new StringDrawer(damage.M.toStringWithZero()).at(posS + delta, lowerY).useConfig(valueConfig).center().draw(g);
                    new StringDrawer(damage.L.toStringWithZero()).at(posS + 2 * delta, lowerY).useConfig(valueConfig).center().draw(g);
                    new StringDrawer(damage.E.toStringWithZero()).at(posS + 3 * delta, lowerY).useConfig(valueConfig).center().draw(g);
                }
            }
        }
    }

    private void paintHeat(Graphics2D g, int x, int y) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(x, y, BOX_WIDTH_MEK_WIDE, BOX_HEAT_HEIGHT, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, BOX_WIDTH_MEK_WIDE, BOX_HEAT_HEIGHT, BOX_CORNER, BOX_CORNER);
        g.drawLine(x + 141, y + 12, x + 141, y + BOX_HEAT_HEIGHT - 12);
        // Heat Scale Box
        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(x + 377, y + 6, 210, BOX_HEAT_HEIGHT - 12, BOX_CORNER / 2, BOX_CORNER / 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x + 377, y + 6, 210, BOX_HEAT_HEIGHT - 12, BOX_CORNER / 2, BOX_CORNER / 2);
        g.drawLine(x + 429, y + 7, x + 429, y + BOX_HEAT_HEIGHT - 7);
        g.drawLine(x + 482, y + 7, x + 482, y + BOX_HEAT_HEIGHT - 7);
        g.drawLine(x + 535, y + 7, x + 535, y + BOX_HEAT_HEIGHT - 7);
        int xb = x + BOX_INSET;
        int ym = y + BOX_HEAT_HEIGHT / 2;
        g.setFont(headerFont);
        g.setColor(Color.WHITE);
        new StringDrawer("1").at(x + 403, ym).center().draw(g);
        new StringDrawer("2").at(x + 455, ym).center().draw(g);
        new StringDrawer("3").at(x + 508, ym).center().draw(g);
        new StringDrawer("S").at(x + 561, ym).center().draw(g);
        g.setColor(Color.BLACK);

        if (element != null) {
            xb += new StringDrawer("OV:").at(xb, ym).centerY().font(headerFont).draw(g).width + 15;
            new StringDrawer(element.getOverheat() + "").at(xb, ym).useConfig(valueConfig).draw(g);
            new StringDrawer("HEAT SCALE").at(194, ym).centerY().font(headerFont).draw(g);
        }
    }

    private void paintArmor(Graphics2D g, int x) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(x, armorBoxY, armorBoxWidth, armorBoxHeight, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, armorBoxY, armorBoxWidth, armorBoxHeight, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            // Headers A, S
            int upperY = armorBoxY + armorBoxHeight / 2 - 18;
            int lowerY = armorBoxY + armorBoxHeight / 2 + 18;
            g.setFont(headerFont);
            int headerWidth = new StringDrawer("A:").at(x + BOX_INSET, upperY).centerY().draw(g).width + 12;
            new StringDrawer("S:").at(x + BOX_INSET, lowerY).centerY().draw(g);

            // Armor Pips
            int cx = x + BOX_INSET + headerWidth;
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < element.getArmor(); i++) {
                g.setColor(Color.WHITE);
                g.fillOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }

            // Structure Pips
            cx = x + BOX_INSET + headerWidth;
            for (int i = 0; i < element.getStructure(); i++) {
                g.setColor(DARKGRAY);
                g.fillOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }
        }
    }

    private void paintAeroArmor(Graphics2D g, int x) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(x, armorBoxY, armorBoxWidth, armorBoxHeight, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, armorBoxY, armorBoxWidth, armorBoxHeight, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            // Headers A, S
            int upperY = armorBoxY + armorBoxHeight / 2 - 18;
            int lowerY = armorBoxY + armorBoxHeight / 2 + 18;
            g.setFont(headerFont);
            int headerWidth = new StringDrawer("A:").at(x + BOX_INSET, upperY).centerY().draw(g).width + 12;
            new StringDrawer("S:").at(x + BOX_INSET, lowerY).centerY().draw(g);

            // Armor Pips
            int cx = x + BOX_INSET + headerWidth;
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < element.getArmor(); i++) {
                g.setColor(Color.WHITE);
                g.fillOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }

            // Structure Pips
            cx = x + BOX_INSET + headerWidth;
            for (int i = 0; i < element.getStructure(); i++) {
                g.setColor(DARKGRAY);
                g.fillOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }

            new StringDrawer("TH").at(606, upperY).font(headerFont).center().draw(g);
            new StringDrawer(element.getThreshold() + "").at(606, lowerY).useConfig(valueConfig).center().draw(g);
        }
    }

    private void paintSpecial(Graphics2D g) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(BORDER + PADDING, specialBoxY, specialBoxWidth, specialBoxHeight, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(BORDER + PADDING, specialBoxY, specialBoxWidth, specialBoxHeight, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            int y = specialBoxY + 2;
            String specials = "SPECIAL: " + element.getSpecialsString();
            g.setFont(specialsFont);
            int ascent = g.getFontMetrics(specialsFont).getAscent();

            if (element.getSpecialsString().isBlank() ||
                    (g.getFontMetrics(specialsFont).stringWidth(specials) < specialBoxWidth - 2 * BOX_INSET)) {
                g.drawString(specials, BORDER + PADDING + BOX_INSET, y + ascent);
                return;
            }

            int linedelta = g.getFontMetrics(specialsFont).getHeight();
            int line = 1;
            String[] tokens = element.getSpecialsString().split(", ");
            int index = 0;
            String fittingLine = "SPECIAL: ";

            while (line <= 3) {
                while (index < tokens.length) {
                    String nextItem = tokens[index] + (index == tokens.length - 1 ? "" : ", ");
                    if ((g.getFontMetrics(specialsFont).stringWidth(fittingLine + nextItem) < specialBoxWidth - 2 * BOX_INSET)
                            || fittingLine.isBlank()) {
                        fittingLine += nextItem;
                        index++;
                    } else {
                        break;
                    }
                }

                g.drawString(fittingLine, BORDER + PADDING + BOX_INSET, y + ascent + (line - 1) * linedelta);
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
            new StringDrawer("PV: ").at(861, 53).centerY().font(pointValueHeaderFont).draw(g);
            new StringDrawer(element.getPointValue() + "").at(941, 53).
                    useConfig(valueConfig).font(pointValueFont).draw(g);

        }
    }

    private void paintMekDamage(Graphics2D g) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(591, 442, 422, 180, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(591, 442, 422, 180, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(802, 470).center().font(headerFont).draw(g);

            new StringDrawer("ENGINE").at(722, 509).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("+1 Heat/Firing Weapons").at(754, 509).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 509);

            new StringDrawer("FIRE CONTROL").at(722, 538).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 538).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 538);
            drawDamagePip(g, 755, 538);
            drawDamagePip(g, 782, 538);
            drawDamagePip(g, 809, 538);

            new StringDrawer("MP").at(722, 565).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("1/2 MV Each").at(834, 565).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 565);
            drawDamagePip(g, 755, 565);
            drawDamagePip(g, 782, 565);
            drawDamagePip(g, 809, 565);

            new StringDrawer("WEAPONS").at(722, 593).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 593).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 593);
            drawDamagePip(g, 755, 593);
            drawDamagePip(g, 782, 593);
            drawDamagePip(g, 809, 593);
        }
    }

    private void paintProtoMekDamage(Graphics2D g) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(591, 442, 422, 180, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(591, 442, 422, 180, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(802, 470).center().font(headerFont).draw(g);

            new StringDrawer("FIRE CONTROL").at(722, 510).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 510).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 510);
            drawDamagePip(g, 755, 510);
            drawDamagePip(g, 782, 510);
            drawDamagePip(g, 809, 510);

            new StringDrawer("MP").at(722, 552).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("1/2 MV Each").at(834, 552).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 552);
            drawDamagePip(g, 755, 552);
            drawDamagePip(g, 782, 552);
            drawDamagePip(g, 809, 552);

            new StringDrawer("WEAPONS").at(722, 593).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 593).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 593);
            drawDamagePip(g, 755, 593);
            drawDamagePip(g, 782, 593);
            drawDamagePip(g, 809, 593);
        }
    }

    private void paintCombatVeeDamage(Graphics2D g) {
        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(591, 442, 422, 180, BOX_CORNER, BOX_CORNER);
        g.setColor(Color.BLACK);
        g.drawRoundRect(591, 442, 422, 180, BOX_CORNER, BOX_CORNER);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(802, 470).center().font(headerFont).draw(g);

            new StringDrawer("ENGINE").at(722, 509).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("1/2 MV and Damage").at(754, 509).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 509);

            new StringDrawer("FIRE CONTROL").at(722, 538).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("+2 To-Hit Each").at(834, 538).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 538);
            drawDamagePip(g, 755, 538);
            drawDamagePip(g, 782, 538);
            drawDamagePip(g, 809, 538);

            new StringDrawer("WEAPONS").at(722, 565).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("-1 Damage Each").at(834, 565).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 728, 565);
            drawDamagePip(g, 755, 565);
            drawDamagePip(g, 782, 565);
            drawDamagePip(g, 809, 565);

            new StringDrawer("MOTIVE").at(598, 593).centerY().font(smallerFont).draw(g);
            drawDamagePip(g, 673, 593);
            drawDamagePip(g, 700, 593);
            new StringDrawer("-2 MV").at(724, 593).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 793, 593);
            drawDamagePip(g, 820, 593);
            new StringDrawer("1/2 MV").at(846, 593).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 919, 593);
            new StringDrawer("0 MV").at(949, 593).centerY().font(specialsFont).draw(g);
        }
    }

    private void paintAeroDamage(Graphics2D g) {
        Path2D.Double box = new Path2D.Double();
        box.moveTo(847, 442);
        box.lineTo(994, 442);
        Arc2D.Double arc = new Arc2D.Double(982, 442, 30, 30, 90, -90, Arc2D.OPEN);
        box.append(arc, true);
        box.lineTo(1012, 604);
        arc = new Arc2D.Double(982, 590, 30, 30, 0, -90, Arc2D.OPEN);
        box.append(arc, true);
        box.lineTo(606, 620);
        arc = new Arc2D.Double(591, 590, 30, 30, 270, -90, Arc2D.OPEN);
        box.append(arc, true);
        box.lineTo(591, 552);
        arc = new Arc2D.Double(591, 537, 30, 30, 180, -90, Arc2D.OPEN);
        box.append(arc, true);
        box.lineTo(645, 537);
        arc = new Arc2D.Double(630, 507, 30, 30, 270, 90, Arc2D.OPEN);
        box.append(arc, true);
        box.lineTo(660, 458);
        arc = new Arc2D.Double(660, 442, 30, 30, 180, -90, Arc2D.OPEN);
        box.append(arc, true);
        box.closePath();

        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fill(box);
        g.setColor(Color.BLACK);
        g.draw(box);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(836, 470).center().font(headerFont).draw(g);

            new StringDrawer("ENGINE").at(736, 515).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("1/4 MV (Minimum 1)").at(796, 515).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 741, 515);
            drawDamagePip(g, 768, 515);

            new StringDrawer("FIRE CONTROL").at(736, 553).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("+2 To-Hit Each").at(849, 553).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 741, 553);
            drawDamagePip(g, 768, 553);
            drawDamagePip(g, 795, 553);
            drawDamagePip(g, 822, 553);

            new StringDrawer("WEAPONS").at(736, 593).centerY().rightAlign().font(smallerFont).draw(g);
            new StringDrawer("-1 Damage Each").at(849, 593).centerY().font(specialsFont).draw(g);
            drawDamagePip(g, 741, 593);
            drawDamagePip(g, 768, 593);
            drawDamagePip(g, 795, 593);
            drawDamagePip(g, 822, 593);
        }
    }

    private void drawDamagePip(Graphics2D g, int x, int y) {
        int d = 20;
        g.setStroke(new BasicStroke(3f));
        g.setColor(Color.WHITE);
        g.fillOval(x, y - d / 2, d, d);
        g.setColor(Color.BLACK);
        g.drawOval(x, y - d / 2, d, d);
    }

    private void paintFluffImage(Graphics2D g) {
        Image image = FluffImageHelper.loadFluffImageHeuristic(element);
        if (image != null) {
            Image image2 = image.getScaledInstance(50, -1, Image.SCALE_AREA_AVERAGING);
            g.drawImage(image2, 660, 110, this);
        }
    }

}