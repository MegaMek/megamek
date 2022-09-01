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

import megamek.client.ui.swing.util.StringDrawer;
import megamek.common.alphaStrike.*;
import megamek.common.util.ImageUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import static megamek.common.alphaStrike.ASUnitType.*;


public class ASLargeAeroCard extends ASCard {

    private final static float BOX_STROKE = 3f;
    private final static Color VERY_LIGHT_GRAY = new Color(235, 235, 235);

    private Font largeAeroChassisFont;
    private Font largeAeroModelFont;
    private Font largeAeroHeaderFont;
    private Font pointValueHeaderFont;
    private Font pointValueFont;
    private Font largeAeroValueFont;
    private Font largeAeroSpecialFont;
    private Font damageFont;
    private Font arcTitleFont;
    private Font arcSpecialsFont;
    private Font critTextFont;

    private final StringDrawer.StringDrawerConfig damageValueConfig = new StringDrawer.StringDrawerConfig().centerX()
            .scaleX(0.9f).color(Color.BLACK).font(damageFont);

    public ASLargeAeroCard(ASCardDisplayable element) {
        super(element);
    }

    @Override
    public BufferedImage getCardImage(int width) {
        int height = 2 * HEIGHT * width / WIDTH;
        final BufferedImage result = ImageUtil.createAcceleratedImage(width, height);
        Graphics graphics = result.getGraphics();
        Graphics2D g2D = (Graphics2D) graphics;
        g2D.scale((float) width / WIDTH, (float) width / WIDTH);
        drawCard(graphics);
        g2D.translate(0, HEIGHT);
        drawFlipside(g2D);
        graphics.dispose();
        return result;
    }

    @Override
    protected void initializeFonts(Font lightFont, Font boldFont, Font blackFont) {
        super.initializeFonts(lightFont, boldFont, blackFont);
        largeAeroChassisFont = boldFont.deriveFont(44f);
        largeAeroModelFont = lightFont.deriveFont(32f);
        largeAeroHeaderFont = boldFont.deriveFont(30f);
        pointValueHeaderFont = lightFont.deriveFont(43f);
        pointValueFont = boldFont.deriveFont(48f);
        largeAeroValueFont = blackFont.deriveFont(42f);
        largeAeroSpecialFont = lightFont.deriveFont(31f);
        damageFont = lightFont.deriveFont(32f);
        arcTitleFont = blackFont.deriveFont(22f);
        critTextFont = boldFont.deriveFont(20f);
        arcSpecialsFont = lightFont.deriveFont(28f);

        valueConfig = new StringDrawer.StringDrawerConfig().centerY().scaleX(0.9f)
                .color(Color.BLACK).font(largeAeroValueFont).outline(Color.BLACK, 0.5f);

        specialsHeaderConfig = new StringDrawer.StringDrawerConfig().color(Color.BLACK)
                .font(blackFont.deriveFont((float)largeAeroSpecialFont.getSize()));
    }

    @Override
    protected void initialize() {
        fluffWidth = 456;
        fluffHeight = 224;
        fluffXCenter = 767;
        fluffYCenter = 366;
    }

    private void drawFlipside(Graphics2D g) {
        paintCardBackground(g, true);
        new StringDrawer("WEAPON CRITICALS").at(33, 629).font(arcTitleFont).maxWidth(220)
                .outline(Color.BLACK, 0.4f).centerY().draw(g);
        new StringDrawer("Damage Value Reduced by 25% per hit. -- Randomly determine an appropriate")
                .at(260, 629).font(critTextFont).maxWidth(750).centerY().draw(g);
        String columns = element.getASUnitType().isAnyOf(SC, DS, DA) ? "STD/SCAP/MSL" : "STD/CAP/SCAP/MSL";
        new StringDrawer(columns + " column.")
                .at(697, 660).font(critTextFont).maxWidth(318).centerY().draw(g);
        AffineTransform baseTransform = g.getTransform();
        g.translate(0, -7);
        drawModelChassis(g);
        g.setTransform(baseTransform);

        g.translate(38, 88);
        paintArcBox(g, getFrontArcName() + " DAMAGE", ASArcs.FRONT);
        g.setTransform(baseTransform);

        g.translate(522, 88);
        paintArcBox(g, getRearArcName() + " DAMAGE", ASArcs.REAR);
        g.setTransform(baseTransform);

        g.translate(38, 353);
        paintArcBox(g, "LEFT " + getSideArcName() + " DAMAGE", ASArcs.LEFT);
        g.setTransform(baseTransform);

        g.translate(522, 353);
        paintArcBox(g, "RIGHT " + getSideArcName() + " DAMAGE", ASArcs.RIGHT);
        g.setTransform(baseTransform);
    }

    private String getFrontArcName() {
        return element.getASUnitType().isAnyOf(SC, DA, DS) ? "NOSE ARC" : "FRONT ARC";
    }

    private String getSideArcName() {
        if (element.getASUnitType().isAnyOf(SC, DA)) {
            return "WING";
        } else if (element.getASUnitType().isAnyOf(DS)) {
            return "SIDE";
        } else {
            return "ARC";
        }
    }

    private String getRearArcName() {
        return element.getASUnitType().isAnyOf(SC, DA, DS) ? "AFT ARC" : "REAR ARC";
    }

    private void paintArcBox(Graphics2D g, String arcTitle, ASArcs arc) {
        drawBox(g, 0, 0, 477, 259, BACKGROUND_GRAY, BOX_STROKE);
        new StringDrawer(arcTitle).at(19, 130).maxWidth(200).font(arcTitleFont).rotate(-Math.PI/2).center().draw(g);

        int titleY = 32;
        int lineDelta = 35;
        int lineS = 68;
        int lineM = lineS + lineDelta;
        int lineL = lineM + lineDelta;
        int lineE = lineL + lineDelta;
        int lineSPE = lineE + lineDelta;

        g.setColor(VERY_LIGHT_GRAY);
        g.fillRect(35, lineS - 27, 431, 34);
        g.fillRect(35, lineL - 27, 431, 34);
        g.fillRect(35, lineSPE - 27, 431, 34);
        g.setColor(Color.BLACK);

        new StringDrawer("S").at(44, lineS).maxWidth(29).font(headerFont).draw(g);
        new StringDrawer("M").at(44, lineM).maxWidth(29).font(headerFont).draw(g);
        new StringDrawer("L").at(44, lineL).maxWidth(29).font(headerFont).draw(g);
        new StringDrawer("E").at(44, lineE).maxWidth(29).font(headerFont).draw(g);
        new StringDrawer("(+0)").at(77, lineS).maxWidth(60).font(headerFont).draw(g);
        new StringDrawer("(+2)").at(77, lineM).maxWidth(60).font(headerFont).draw(g);
        new StringDrawer("(+4)").at(77, lineL).maxWidth(60).font(headerFont).draw(g);
        new StringDrawer("(+6)").at(77, lineE).maxWidth(60).font(headerFont).draw(g);
        new StringDrawer("SPE").at(44, 210).maxWidth(90).font(headerFont).draw(g);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(146, lineSPE + 7, 466, lineSPE + 7);
        new StringDrawer("CRIT").at(44, 245).maxWidth(90).font(headerFont).draw(g);
        if (element.getASUnitType().isAnyOf(SC, DS, DA)) {
            int stdX = 177;
            int delta = 122;
            new StringDrawer("STD").at(stdX, titleY).maxWidth(93).centerX().font(headerFont).draw(g);
            new StringDrawer("SCAP").at(stdX + delta, titleY).maxWidth(delta - 10).centerX().font(headerFont).draw(g);
            new StringDrawer("MSL").at(stdX + 2 * delta, titleY).maxWidth(delta - 10).centerX().font(headerFont).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().S.toString()).at(stdX, lineS).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().S.toString()).at(stdX + delta, lineS).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().S.toString()).at(stdX + 2 * delta, lineS).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().M.toString()).at(stdX, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().M.toString()).at(stdX + delta, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().M.toString()).at(stdX + 2 * delta, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().L.toString()).at(stdX, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().L.toString()).at(stdX + delta, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().L.toString()).at(stdX + 2 * delta, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().E.toString()).at(stdX, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().E.toString()).at(stdX + delta, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().E.toString()).at(stdX + 2 * delta, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getSpecials().toString()).at(146, lineSPE).maxWidth(310).font(arcSpecialsFont).draw(g);

            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + delta + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + 2 * delta + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            g.drawLine(stdX + delta / 2, 221, stdX + delta / 2, 251);
            g.drawLine(stdX + 3 * delta / 2, 221, stdX + 3 * delta / 2, 251);
        } else {
            int stdX = 177;
            int delta = 82;
            new StringDrawer("STD").at(stdX, titleY).maxWidth(delta - 6).centerX().font(headerFont).draw(g);
            new StringDrawer("CAP").at(stdX + delta, titleY).maxWidth(delta - 6).centerX().font(headerFont).draw(g);
            new StringDrawer("SCAP").at(stdX + 2 * delta, titleY).maxWidth(delta - 6).centerX().font(headerFont).draw(g);
            new StringDrawer("MSL").at(stdX + 3 * delta, titleY).maxWidth(delta - 6).centerX().font(headerFont).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().S.toString()).at(stdX, lineS).maxWidth(delta - 16).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getCAPDamage().S.toString()).at(stdX + delta, lineS).maxWidth(delta - 16).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().S.toString()).at(stdX + 2 * delta, lineS).maxWidth(delta - 16).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().S.toString()).at(stdX + 3 * delta, lineS).maxWidth(delta - 16).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().M.toString()).at(stdX, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getCAPDamage().M.toString()).at(stdX + delta, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().M.toString()).at(stdX + 2 * delta, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().M.toString()).at(stdX + 3 * delta, lineM).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().L.toString()).at(stdX, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getCAPDamage().L.toString()).at(stdX + delta, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().L.toString()).at(stdX + 2 * delta, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().L.toString()).at(stdX + 3 * delta, lineL).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getStdDamage().E.toString()).at(stdX, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getCAPDamage().E.toString()).at(stdX + delta, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getSCAPDamage().E.toString()).at(stdX + 2 * delta, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);
            new StringDrawer(getArc(arc).getMSLDamage().E.toString()).at(stdX + 3 * delta, lineE).maxWidth(delta - 10).useConfig(damageValueConfig).draw(g);

            new StringDrawer(getArc(arc).getSpecials().toString()).at(146, lineSPE).maxWidth(310).font(arcSpecialsFont).draw(g);

            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + delta + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + 2 * delta + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            for (int i = 0; i < 4; i++) {
                drawDamagePip(g, stdX + 3 * delta + (i - 2) * (DAMAGE_PIP_SIZE + 2) + 1, 236);
            }
            g.drawLine(stdX + delta / 2, 221, stdX + delta / 2, 251);
            g.drawLine(stdX + 3 * delta / 2, 221, stdX + 3 * delta / 2, 251);
            g.drawLine(stdX + 5 * delta / 2, 221, stdX + 5 * delta / 2, 251);
        }
    }

    @Override
    protected void drawModelChassis(Graphics2D g) {
        int width = new StringDrawer(element.getChassis()).at(36, 77).font(largeAeroChassisFont).scaleX(0.8f).maxWidth(600).draw(g).width;
        new StringDrawer(element.getModel()).at(56 + width, 77).font(largeAeroModelFont).maxWidth(750 - width).draw(g);
    }

    @Override
    protected void paintBaseInfo(Graphics2D g) {
        ASCard.drawBox(g, 36, 97, 624, 63, Color.LIGHT_GRAY, BOX_STROKE);

        int centerY = 129;
        new StringDrawer("TP: ").at(44, centerY).centerY().font(headerFont).maxWidth(47).draw(g);
        new StringDrawer(element.getASUnitType().toString()).at(130, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);

        new StringDrawer("SZ: ").at(185, centerY).centerY().font(headerFont).maxWidth(51).draw(g);
        new StringDrawer(element.getSize() + "").at(272, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);

        new StringDrawer("THR: ").at(323, centerY).centerY().font(headerFont).maxWidth(74).draw(g);
        new StringDrawer(AlphaStrikeHelper.getMovementAsString(element)).at(444, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);

        new StringDrawer("SKILL: ").at(485, centerY).centerY().font(headerFont).maxWidth(87).draw(g);
        new StringDrawer(element.getSkill() + "").at(610, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);
    }

    @Override
    protected void paintArmor(Graphics2D g) {
        Path2D.Double box = new Path2D.Double();
        box.moveTo(480, 170);
        box.append(new Arc2D.Double(918, 170, 30, 30, 90, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(918, 212, 30, 30, 0, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(524, 242, 30, 30, 90, 90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(494, 370, 30, 30, 0, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(36, 370, 30, 30, 270, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(36, 170, 30, 30, 180, -90, Arc2D.OPEN), true);
        box.closePath();

        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(Color.LIGHT_GRAY);
        g.fill(box);
        g.setColor(Color.BLACK);
        g.draw(box);

        g.drawLine(200, 238, 230, 238);
        g.drawLine(200, 249, 230, 249);
        g.drawLine(200, 347, 230, 347);
        g.drawLine(200, 358, 230, 358);
        ASCard.drawBox(g, 44, 210, 168, 70, Color.WHITE, BOX_STROKE);
        ASCard.drawBox(g, 221, 210, 295, 70, Color.WHITE, BOX_STROKE);
        ASCard.drawBox(g, 44, 319, 168, 70, Color.WHITE, BOX_STROKE);
        ASCard.drawBox(g, 221, 319, 295, 70, Color.WHITE, BOX_STROKE);

        int armorY = 190;
        int structureY = 300;
        g.setFont(largeAeroHeaderFont);
        new StringDrawer("ARMOR").at(128, armorY).center().maxWidth(170).draw(g);
        new StringDrawer("DAMAGE").at(363, armorY).center().maxWidth(290).draw(g);
        new StringDrawer("STRUCTURE").at(128, structureY).center().maxWidth(170).draw(g);
        new StringDrawer("DAMAGE").at(363, structureY).center().maxWidth(290).draw(g);

        new StringDrawer(element.getFullArmor() + "").at(128, 245).font(largeAeroValueFont).center().maxWidth(140).draw(g);
        new StringDrawer(element.getFullStructure() + "").at(128, 354).font(largeAeroValueFont).center().maxWidth(140).draw(g);

        int thresholdY = 206;
        new StringDrawer("DAMAGE THRESHOLD").at(628, thresholdY).font(largeAeroHeaderFont).centerY().maxWidth(310).draw(g);
        new StringDrawer(element.getThreshold() + "").at(580, thresholdY).useConfig(valueConfig).center().maxWidth(79).draw(g);
    }

    @Override
    protected void paintSpecial(Graphics2D g) {
        ASCard.drawBox(g, 536, 485, 477, 152, Color.LIGHT_GRAY, BOX_STROKE);
        paintSpecialTextLines(g, element, largeAeroSpecialFont, 551, 484, 447, 44);
    }

    @Override
    protected void paintPointValue(Graphics2D g) {
        if (element != null) {
            new StringDrawer("PV: ").at(853, 70).maxWidth(70).font(pointValueHeaderFont).scaleX(0.9f).draw(g);
            new StringDrawer(element.getPointValue() + "").at(980, 70).outline(Color.BLACK, 0.3f)
                    .scaleX(0.85f).maxWidth(86).centerX().font(pointValueFont).draw(g);
        }
    }

    @Override
    protected void paintHits(Graphics2D g) {
        ASCard.drawBox(g, 36, 410, 490, 227, Color.LIGHT_GRAY, BOX_STROKE);
        if (element.getASUnitType().isAnyOf(WS, SS, JS)) {
            new StringDrawer("CRITICAL HITS").at(281, 435).center().font(headerFont).maxWidth(450).draw(g);

            new StringDrawer("CREW").at(168, 482).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("{").at(281, 482).centerY().font(specialsFont).draw(g);
            new StringDrawer("+2 Weapon To-Hit Each").at(289, 472).centerY().font(specialsFont).maxWidth(225).draw(g);
            new StringDrawer("+2 Controll Roll Each").at(289, 492).centerY().font(specialsFont).maxWidth(225).draw(g);
            drawDamagePip(g, 173, 482);
            drawDamagePip(g, 200, 482);
            drawDamagePip(g, 227, 482);
            drawDamagePip(g, 254, 482);

            new StringDrawer("ENGINE").at(168, 516).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("-25%/-50%/-100% THR").at(253, 516).centerY().font(specialsFont).maxWidth(260).draw(g);
            drawDamagePip(g, 173, 516);
            drawDamagePip(g, 200, 516);
            drawDamagePip(g, 227, 516);

            new StringDrawer("FIRE CONTROL").at(168, 550).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("+2 To-Hit Each").at(279, 550).centerY().font(specialsFont).maxWidth(233).draw(g);
            drawDamagePip(g, 173, 550);
            drawDamagePip(g, 200, 550);
            drawDamagePip(g, 227, 550);
            drawDamagePip(g, 254, 550);

            new StringDrawer("THRUSTER").at(168, 584).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("-1 Thrust (THR)").at(199, 584).centerY().font(specialsFont).maxWidth(313).draw(g);
            drawDamagePip(g, 173, 584);

            new StringDrawer("WEAPONS").at(168, 618).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("See Back").at(281, 618).centerY().font(specialsFont).maxWidth(313).draw(g);

            for (int x = 180; x <= 270; x += 4) {
                g.fillRect(x, 617, 2, 2);
            }
        } else {
            new StringDrawer("CRITICAL HITS").at(281, 429).center().font(headerFont).maxWidth(450).draw(g);

            new StringDrawer("CREW").at(168, 463).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("{").at(227, 463).centerY().font(specialsFont).draw(g);
            new StringDrawer("+2 Weapon To-Hit Each").at(235, 453).centerY().font(specialsFont).maxWidth(274).draw(g);
            new StringDrawer("+2 Controll Roll Each").at(235, 473).centerY().font(specialsFont).maxWidth(274).draw(g);
            drawDamagePip(g, 173, 463);
            drawDamagePip(g, 200, 463);

            new StringDrawer("ENGINE").at(168, 494).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("-25%/-50%/-100% THR").at(253, 494).centerY().font(specialsFont).maxWidth(260).draw(g);
            drawDamagePip(g, 173, 494);
            drawDamagePip(g, 200, 494);
            drawDamagePip(g, 227, 494);

            new StringDrawer("FIRE CONTROL").at(168, 520).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("+2 To-Hit Each").at(279, 520).centerY().font(specialsFont).maxWidth(233).draw(g);
            drawDamagePip(g, 173, 520);
            drawDamagePip(g, 200, 520);
            drawDamagePip(g, 227, 520);
            drawDamagePip(g, 254, 520);

            new StringDrawer("KF BOOM").at(168, 547).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("Cannot transport via JumpShip").at(199, 547).centerY().font(specialsFont).maxWidth(313).draw(g);
            drawDamagePip(g, 173, 547);

            new StringDrawer("DOCK COLLAR").at(168, 572).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("DropShip only; cannot dock").at(199, 572).centerY().font(specialsFont).maxWidth(313).draw(g);
            drawDamagePip(g, 173, 572);

            new StringDrawer("THRUSTER").at(168, 598).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("-1 Thrust (THR)").at(199, 598).centerY().font(specialsFont).maxWidth(313).draw(g);
            drawDamagePip(g, 173, 598);

            new StringDrawer("WEAPONS").at(168, 624).useConfig(hitsTitleConfig).maxWidth(125).draw(g);
            new StringDrawer("See Back").at(281, 624).centerY().font(specialsFont).maxWidth(313).draw(g);

            for (int x = 180; x <= 270; x += 4) {
                g.fillRect(x, 623, 2, 2);
            }
        }
    }

    private ASArcSummary getArc(ASArcs arc) {
        switch (arc) {
            case FRONT:
                return element.getFrontArc();
            case LEFT:
                return element.getLeftArc();
            case REAR:
                return element.getRearArc();
            default:
                return element.getRightArc();
        }
    }

    @Override
    protected void paintDamage(Graphics2D g) { }
}