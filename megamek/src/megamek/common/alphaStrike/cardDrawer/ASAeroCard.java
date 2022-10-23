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
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

public class ASAeroCard extends ASHeatTrackingCard {

    public ASAeroCard(ASCardDisplayable element) {
        super(element);
    }

    @Override
    protected void initialize() {
        super.initialize();
        specialBoxWidth = 531;
    }

    @Override
    protected void paintBaseInfo(Graphics2D g) {
        drawBox(g, 36, 170, BOX_WIDTH_WIDE, baseInfoBoxHeight, BACKGROUND_GRAY, BOX_STROKE);

        if (element != null) {
            int upperY = 170 + baseInfoBoxHeight / 2 - 20;
            int lowerY = 170 + baseInfoBoxHeight / 2 + 20;

            new StringDrawer("TP: ").at(44, upperY).centerY().maxWidth(55).font(headerFont).draw(g);
            new StringDrawer(element.getASUnitType().toString()).at(107, upperY).useConfig(valueConfig).maxWidth(64).draw(g);

            new StringDrawer("SZ: ").at(182, upperY).centerY().font(headerFont).maxWidth(56).draw(g);
            new StringDrawer(element.getSize() + "").at(244, upperY).useConfig(valueConfig).maxWidth(33).draw(g);

            new StringDrawer("THR: ").at(281, upperY).centerY().font(headerFont).maxWidth(94).draw(g);
            new StringDrawer(AlphaStrikeHelper.getMovementAsString(element)).at(380, upperY).useConfig(valueConfig).maxWidth(44).draw(g);

            new StringDrawer("ROLE: ").at(44, lowerY).centerY().font(headerFont).maxWidth(85).draw(g);
            new StringDrawer(element.getRole().toString()).at(138, lowerY).useConfig(valueConfig).maxWidth(250).draw(g);

            new StringDrawer("SKILL: ").at(402, lowerY).centerY().font(headerFont).maxWidth(98).draw(g);
            new StringDrawer(element.getSkill() + "").at(506, lowerY).useConfig(valueConfig).maxWidth(120).draw(g);
        }
    }

    @Override
    protected void paintHits(Graphics2D g) {
        Path2D.Double box = new Path2D.Double();
        box.moveTo(847, 442);
        box.append(new Arc2D.Double(982, 442, 30, 30, 90, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(982, 590, 30, 30, 0, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(591, 590, 30, 30, 270, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(591, 537, 30, 30, 180, -90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(630, 507, 30, 30, 270, 90, Arc2D.OPEN), true);
        box.append(new Arc2D.Double(660, 442, 30, 30, 180, -90, Arc2D.OPEN), true);
        box.closePath();

        g.setStroke(new BasicStroke(BOX_STROKE));
        g.setColor(BACKGROUND_GRAY);
        g.fill(box);
        g.setColor(Color.BLACK);
        g.draw(box);

        if (element != null) {
            new StringDrawer("CRITICAL HITS").at(836, 470).center().font(headerFont).maxWidth(320).draw(g);

            new StringDrawer("ENGINE").at(736, 515).useConfig(hitsTitleConfig).maxWidth(67).draw(g);
            new StringDrawer("1/4 MV (Minimum 1)").at(796, 515).centerY().font(specialsFont).maxWidth(206).draw(g);
            drawDamagePip(g, 741, 515);
            drawDamagePip(g, 768, 515);

            new StringDrawer("FIRE CONTROL").at(736, 553).useConfig(hitsTitleConfig).maxWidth(135).draw(g);
            new StringDrawer("+2 To-Hit Each").at(849, 553).centerY().font(specialsFont).maxWidth(154).draw(g);
            drawDamagePip(g, 741, 553);
            drawDamagePip(g, 768, 553);
            drawDamagePip(g, 795, 553);
            drawDamagePip(g, 822, 553);

            new StringDrawer("WEAPONS").at(736, 593).useConfig(hitsTitleConfig).maxWidth(135).draw(g);
            new StringDrawer("-1 Damage Each").at(849, 593).centerY().font(specialsFont).maxWidth(154).draw(g);
            drawDamagePip(g, 741, 593);
            drawDamagePip(g, 768, 593);
            drawDamagePip(g, 795, 593);
            drawDamagePip(g, 822, 593);
        }
    }

    @Override
    protected void paintArmor(Graphics2D g) {
        drawBox(g, 36, armorBoxY, armorBoxWidth, armorBoxHeight, BACKGROUND_GRAY, BOX_STROKE);

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
            for (int i = 0; i < element.getFullArmor(); i++) {
                g.setColor(Color.WHITE);
                g.fillOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, upperY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }

            // Structure Pips
            cx = 44 + headerWidth;
            for (int i = 0; i < element.getFullStructure(); i++) {
                g.setColor(DARKGRAY);
                g.fillOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                g.setColor(Color.BLACK);
                g.drawOval(cx, lowerY - ARMOR_PIP_SIZE / 2, ARMOR_PIP_SIZE, ARMOR_PIP_SIZE);
                cx += ARMOR_PIP_SIZE + 1;
            }

            new StringDrawer("TH").at(606, upperY).font(headerFont).center().maxWidth(52).draw(g);
            new StringDrawer(element.getThreshold() + "").at(606, lowerY).useConfig(valueConfig).center().maxWidth(52).draw(g);
        }
    }

    @Override
    protected void paintDamage(Graphics2D g) {
        drawBox(g, 36, damageBoxY, BOX_WIDTH_WIDE, damageBoxHeight, BACKGROUND_GRAY, BOX_STROKE);

        new StringDrawer("DAMAGE").at(36 + 19, damageBoxY + damageBoxHeight / 2).center()
                .rotate(-Math.PI / 2).font(hitsTitleFont).maxWidth(70).draw(g);

        int upperY = damageBoxY + damageBoxHeight / 2 - 20;
        int lowerY = damageBoxY + damageBoxHeight / 2 + 22;
        int delta = 150;
        int posS = 120;
        ASDamageVector damage = element.getStandardDamage();
        g.setFont(headerFont);
        new StringDrawer("S (+0)").at(posS, upperY).center().maxWidth(110).draw(g);
        new StringDrawer("M (+2)").at(posS + delta, upperY).center().maxWidth(110).draw(g);
        new StringDrawer("L (+4)").at(posS + 2 * delta, upperY).center().maxWidth(110).draw(g);
        new StringDrawer("E (+6)").at(posS + 3 * delta, upperY).center().maxWidth(110).draw(g);
        g.setFont(valueFont);
        new StringDrawer(damage.S.toStringWithZero()).at(posS, lowerY).useConfig(valueConfig).center().maxWidth(110).draw(g);
        new StringDrawer(damage.M.toStringWithZero()).at(posS + delta, lowerY).useConfig(valueConfig).center().maxWidth(110).draw(g);
        new StringDrawer(damage.L.toStringWithZero()).at(posS + 2 * delta, lowerY).useConfig(valueConfig).center().maxWidth(110).draw(g);
        new StringDrawer(damage.E.toStringWithZero()).at(posS + 3 * delta, lowerY).useConfig(valueConfig).center().maxWidth(110).draw(g);
    }
}
