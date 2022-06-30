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
import megamek.common.alphaStrike.AlphaStrikeElement;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;


public class ASLargeAeroCard extends ASCard {

    private final static float BOX_STROKE = 3f;

    private Font largeAeroChassisFont;
    private Font largeAeroModelFont;
    private Font largeAeroHeaderFont;
    private Font pointValueHeaderFont;
    private Font pointValueFont ;
    private Font largeAeroValueFont;
    private Font largeAeroSpecialFont;

    public ASLargeAeroCard(AlphaStrikeElement element) {
        super(element);
    }

    @Override
    protected void initializeFonts(Font lightFont, Font boldFont, Font blackFont) {
        super.initializeFonts(lightFont, boldFont, blackFont);
        largeAeroChassisFont = boldFont.deriveFont(44f);
        largeAeroModelFont = lightFont.deriveFont(32f);
        largeAeroHeaderFont = boldFont.deriveFont(30f);
        pointValueHeaderFont = headerFont.deriveFont(40f);
        pointValueFont = valueFont.deriveFont(54f);
        largeAeroValueFont = blackFont.deriveFont(42f);
        largeAeroSpecialFont = lightFont.deriveFont(31f);

        valueConfig = new StringDrawer.StringDrawerConfig().centerY().scaleX(0.9f)
                .color(Color.BLACK).font(largeAeroValueFont).outline(Color.BLACK, 0.5f);
    }

//    @Override
//    protected void drawCardContent(Graphics2D g2D) {
//        // Data blocks
//        paintBaseInfo(g2D);
//        paintArmor(g2D);
//        paintSpecial(g2D, element);
//        paintPointValue(g2D, element);
//        paintHits(g2D, element);
//
//        // Fluff Image
//        g2D.drawImage(fluffImage, 660, 110, null);
//
//        // Model and Chassis
////        int width = new StringDrawer(element.getChassis()).at(36, 77).font(largeAeroChassisFont).scaleX(0.8f).maxWidth(600).draw(g2D).width;
////        new StringDrawer(element.getModel()).at(56 + width, 77).font(largeAeroModelFont).maxWidth(750 - width).draw(g2D);
//    }

    @Override
    protected void drawFluffImage(Graphics2D g) {
        //TODO
        super.drawFluffImage(g);
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
        new StringDrawer(element.getType().toString()).at(130, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);

        new StringDrawer("SZ: ").at(185, centerY).centerY().font(headerFont).maxWidth(51).draw(g);
        new StringDrawer(element.getSize() + "").at(272, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);

        new StringDrawer("THR: ").at(323, centerY).centerY().font(headerFont).maxWidth(74).draw(g);
        new StringDrawer(element.getMovementAsString()).at(444, centerY - 2).useConfig(valueConfig).centerX().maxWidth(70).draw(g);

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

        new StringDrawer(element.getArmor() + "").at(128, 245).font(largeAeroValueFont).center().maxWidth(140).draw(g);
        new StringDrawer(element.getStructure() + "").at(128, 354).font(largeAeroValueFont).center().maxWidth(140).draw(g);

        int thresholdY = 206;
        new StringDrawer("DAMAGE THRESHOLD").at(628, thresholdY).font(largeAeroHeaderFont).centerY().maxWidth(310).draw(g);
        new StringDrawer(element.getThreshold() + "").at(580, thresholdY).useConfig(valueConfig).center().maxWidth(79).draw(g);
    }

    @Override
    protected void paintSpecial(Graphics2D g) {
        ASCard.drawBox(g, 536, 485, 477, 152, Color.LIGHT_GRAY, BOX_STROKE);
        paintSpecialTextLines(g, element, largeAeroSpecialFont, 551, 497, 447);
    }

    @Override
    protected void paintPointValue(Graphics2D g) {
        if (element != null) {
            new StringDrawer("PV: ").at(861, 53).centerY().font(pointValueHeaderFont).draw(g);
            new StringDrawer(element.getPointValue() + "").at(941, 53).
                    useConfig(valueConfig).font(pointValueFont).draw(g);
        }
    }

    @Override
    protected void paintHits(Graphics2D g) {
        ASCard.drawBox(g, 36, 410, 490, 227, Color.LIGHT_GRAY, BOX_STROKE);

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

    @Override
    protected void paintDamage(Graphics2D g) { }
}