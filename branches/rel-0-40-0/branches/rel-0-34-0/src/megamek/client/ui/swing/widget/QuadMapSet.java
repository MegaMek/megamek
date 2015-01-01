/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Entity;
import megamek.common.Mech;

/**
 * Very cumbersome class that handles set of polygonal areas and labels for
 * PicMap component to represent single mech unit in MechDisplay
 */

public class QuadMapSet implements DisplayMapSet {

    private static final String IMAGE_DIR = "data/images/widgets";

    // Because of keeping all areas of single type in one array
    // some index offset values required
    private static final int REAR_AREA_OFFSET = 7;
    private static final int INT_STRUCTURE_OFFSET = 11;

    // Array of polygonal areas - parts of mech body.
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[19];
    // Array of fixed labels - short names of body parts
    private PMSimpleLabel[] labels = new PMSimpleLabel[19];
    // Array of value labels to show armor and IS values
    private PMValueLabel[] vLabels = new PMValueLabel[20];
    // Heat control area
    private PMPicPolygonalArea heatHotArea;
    // Set of Background Drawers
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    // Main areas group that keeps everything in itself and is passed to PicMap
    // component
    private PMAreasGroup content = new PMAreasGroup();
    // Reference to Component class (need to manage images and fonts)
    private JComponent comp;

    // Points for build hot areas (may be too heavy, think of to load from
    // exteranl file)
    // Mek armor - Front
    private Polygon rightArm = new Polygon(new int[] { 102, 102, 100, 95, 95,
            100, 110, 120, 120, 125 }, new int[] { 120, 70, 65, 65, 50, 55, 55,
            65, 115, 120 }, 10);
    private Polygon leftArm = new Polygon(new int[] { 0, 5, 5, 15, 25, 30, 30,
            25, 23, 23 },
            new int[] { 120, 115, 65, 55, 55, 50, 65, 65, 70, 120 }, 10);
    private Polygon head = new Polygon(new int[] { 50, 50, 55, 70, 75, 75 },
            new int[] { 40, 25, 20, 20, 25, 40 }, 6);
    private Polygon centralTorso = new Polygon(new int[] { 50, 50, 75, 75 },
            new int[] { 80, 40, 40, 80 }, 4);
    private Polygon leftTorso = new Polygon(
            new int[] { 50, 35, 30, 30, 35, 50 }, new int[] { 80, 80, 75, 45,
                    40, 40 }, 6);
    private Polygon rightTorso = new Polygon(
            new int[] { 75, 75, 90, 95, 95, 90 }, new int[] { 80, 40, 40, 45,
                    75, 80 }, 6);
    private Polygon leftLeg = new Polygon(new int[] { 30, 30, 35, 50, 50, 55 },
            new int[] { 120, 85, 80, 80, 115, 120 }, 6);
    private Polygon rightLeg = new Polygon(
            new int[] { 70, 75, 75, 90, 95, 95 }, new int[] { 120, 115, 80, 80,
                    85, 120 }, 6);

    // Mek Armor - Rear
    private Polygon rearLeftTorso = new Polygon(new int[] { 142, 142, 148, 139,
            123, 123, 142 }, new int[] { 14, 43, 76, 76, 44, 17, 14 }, 7);
    private Polygon rearCentralTorso = new Polygon(new int[] { 142, 148, 162,
            168, 168, 142 }, new int[] { 44, 76, 76, 44, 14, 14 }, 6);
    private Polygon rearRightTorso = new Polygon(new int[] { 168, 168, 162,
            171, 187, 187, 168 }, new int[] { 14, 43, 76, 76, 44, 17, 14 }, 7);

    // Internal Structure
    private Polygon inStRightArm = new Polygon(new int[] { 102, 102, 100, 95,
            95, 100, 110, 120, 120, 125 }, new int[] { 112 + 120, 112 + 70,
            112 + 65, 112 + 65, 112 + 50, 112 + 55, 112 + 55, 112 + 65,
            112 + 115, 112 + 120 }, 10);
    private Polygon inStLeftArm = new Polygon(new int[] { 0, 5, 5, 15, 25, 30,
            30, 25, 23, 23 }, new int[] { 112 + 120, 112 + 115, 112 + 65,
            112 + 55, 112 + 55, 112 + 50, 112 + 65, 112 + 65, 112 + 70,
            112 + 120 }, 10);
    private Polygon intStHead = new Polygon(
            new int[] { 50, 50, 55, 70, 75, 75 }, new int[] { 112 + 40,
                    112 + 25, 112 + 20, 112 + 20, 112 + 25, 112 + 40 }, 6);
    private Polygon inStCentralTorso = new Polygon(
            new int[] { 50, 50, 75, 75 }, new int[] { 112 + 80, 112 + 40,
                    112 + 40, 112 + 80 }, 4);
    private Polygon inStLeftTorso = new Polygon(new int[] { 50, 35, 30, 30, 35,
            50 }, new int[] { 112 + 80, 112 + 80, 112 + 75, 112 + 45, 112 + 40,
            112 + 40 }, 6);
    private Polygon inStRightTorso = new Polygon(new int[] { 75, 75, 90, 95,
            95, 90 }, new int[] { 112 + 80, 112 + 40, 112 + 40, 112 + 45,
            112 + 75, 112 + 80 }, 6);
    private Polygon inStLeftLeg = new Polygon(new int[] { 30, 30, 35, 50, 50,
            55 }, new int[] { 112 + 120, 112 + 85, 112 + 80, 112 + 80,
            112 + 115, 112 + 120 }, 6);
    private Polygon inStRightLeg = new Polygon(new int[] { 70, 75, 75, 90, 95,
            95 }, new int[] { 112 + 120, 112 + 115, 112 + 80, 112 + 80,
            112 + 85, 112 + 120 }, 6);

    // Heat control
    private Polygon heatControl = new Polygon(new int[] { 149, 159, 159, 149 },
            new int[] { 100, 100, 220, 220 }, 4);

    private Image heatImage;

    private static final Font FONT_LABEL = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    public QuadMapSet(JComponent c) {
        comp = c;
        setAreas();
        setLabels();
        setGroups();
        setBackGround();
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    public void setEntity(Entity e) {
        Mech m = (Mech) e;
        boolean mtHeat = false;
        if (e.getGame() != null
                && e.getGame().getOptions().booleanOption("tacops_heat")) {
            mtHeat = true;
        }
        int a = 1;
        int a0 = 1;
        for (int i = 0; i < m.locations(); i++) {
            a = m.getArmor(i);
            a0 = m.getOArmor(i);
            vLabels[i].setValue(m.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
            if (m.hasRearArmor(i)) {
                a = m.getArmor(i, true);
                a0 = m.getOArmor(i, true);
                vLabels[i + REAR_AREA_OFFSET].setValue(m
                        .getArmorString(i, true));
                WidgetUtils.setAreaColor(areas[i + REAR_AREA_OFFSET], vLabels[i
                        + REAR_AREA_OFFSET], (double) a / (double) a0);
            }
            a = m.getInternal(i);
            a0 = m.getOInternal(i);
            vLabels[i + INT_STRUCTURE_OFFSET].setValue(m.getInternalString(i));
            WidgetUtils.setAreaColor(areas[i + INT_STRUCTURE_OFFSET], vLabels[i
                    + INT_STRUCTURE_OFFSET], (double) a / (double) a0);
        }

        // heat
        vLabels[19].setValue(Integer.toString(m.heat));
        drawHeatControl(m.heat, mtHeat);
    }

    private void setAreas() {
        areas[Mech.LOC_HEAD] = new PMSimplePolygonArea(head);
        areas[Mech.LOC_CT] = new PMSimplePolygonArea(centralTorso);
        areas[Mech.LOC_RT] = new PMSimplePolygonArea(rightTorso);
        areas[Mech.LOC_LT] = new PMSimplePolygonArea(leftTorso);
        areas[Mech.LOC_RARM] = new PMSimplePolygonArea(rightArm);
        areas[Mech.LOC_LARM] = new PMSimplePolygonArea(leftArm);
        areas[Mech.LOC_RLEG] = new PMSimplePolygonArea(rightLeg);
        areas[Mech.LOC_LLEG] = new PMSimplePolygonArea(leftLeg);
        areas[REAR_AREA_OFFSET + Mech.LOC_CT] = new PMSimplePolygonArea(
                rearCentralTorso);
        areas[REAR_AREA_OFFSET + Mech.LOC_RT] = new PMSimplePolygonArea(
                rearRightTorso);
        areas[REAR_AREA_OFFSET + Mech.LOC_LT] = new PMSimplePolygonArea(
                rearLeftTorso);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_HEAD] = new PMSimplePolygonArea(
                intStHead);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_CT] = new PMSimplePolygonArea(
                inStCentralTorso);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_RT] = new PMSimplePolygonArea(
                inStRightTorso);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_LT] = new PMSimplePolygonArea(
                inStLeftTorso);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_RARM] = new PMSimplePolygonArea(
                inStRightArm);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_LARM] = new PMSimplePolygonArea(
                inStLeftArm);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_RLEG] = new PMSimplePolygonArea(
                inStRightLeg);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_LLEG] = new PMSimplePolygonArea(
                inStLeftLeg);
        heatImage = comp.createImage(10, 120);
        drawHeatControl(0);
        heatHotArea = new PMPicPolygonalArea(heatControl, heatImage);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[Mech.LOC_HEAD] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_H"), fm, Color.black, 58, 29); //$NON-NLS-1$
        labels[Mech.LOC_LARM] = WidgetUtils.createLabel(Messages
                .getString("QuadMapSet.L_LA"), fm, Color.black, 14, 69); //$NON-NLS-1$
        labels[Mech.LOC_LT] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_LT"), fm, Color.black, 41, 52); //$NON-NLS-1$
        labels[Mech.LOC_CT] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_CT"), fm, Color.black, 62, 45); //$NON-NLS-1$
        labels[Mech.LOC_RT] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_RT"), fm, Color.black, 84, 52); //$NON-NLS-1$
        labels[Mech.LOC_RARM] = WidgetUtils.createLabel(Messages
                .getString("QuadMapSet.L_RA"), fm, Color.black, 111, 69); //$NON-NLS-1$
        labels[Mech.LOC_LLEG] = WidgetUtils.createLabel(Messages
                .getString("QuadMapSet.L_LL"), fm, Color.black, 39, 87); //$NON-NLS-1$
        labels[Mech.LOC_RLEG] = WidgetUtils.createLabel(Messages
                .getString("QuadMapSet.L_RL"), fm, Color.black, 85, 87); //$NON-NLS-1$
        // Labels for Back view
        labels[REAR_AREA_OFFSET + Mech.LOC_LT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_LT"), fm, Color.black, 133, 39); //$NON-NLS-1$
        labels[REAR_AREA_OFFSET + Mech.LOC_CT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_CT"), fm, Color.black, 156, 25); //$NON-NLS-1$
        labels[REAR_AREA_OFFSET + Mech.LOC_RT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_RT"), fm, Color.black, 178, 39); //$NON-NLS-1$
        // Labels for Internal Structure
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_HEAD] = WidgetUtils.createLabel(
                Messages.getString("MechMapSet.l_H"), fm, Color.black, 63, 130); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_LARM] = WidgetUtils
                .createLabel(
                        Messages.getString("QuadMapSet.L_LA"), fm, Color.black, 14, 179); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_LT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_LT"), fm, Color.black, 42, 166); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_CT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.L_CT"), fm, Color.black, 63, 160); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_RT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_RT"), fm, Color.black, 85, 166); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_RARM] = WidgetUtils
                .createLabel(
                        Messages.getString("QuadMapSet.L_RA"), fm, Color.black, 111, 179); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_LLEG] = WidgetUtils
                .createLabel(
                        Messages.getString("QuadMapSet.L_LL"), fm, Color.black, 39, 200); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_RLEG] = WidgetUtils
                .createLabel(
                        Messages.getString("QuadMapSet.L_RL"), fm, Color.black, 85, 200); //$NON-NLS-1$

        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[Mech.LOC_HEAD] = WidgetUtils.createValueLabel(68, 30, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_LARM] = WidgetUtils.createValueLabel(13, 82, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_LT] = WidgetUtils.createValueLabel(40, 66, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_CT] = WidgetUtils.createValueLabel(62, 60, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_RT] = WidgetUtils.createValueLabel(85, 66, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_RARM] = WidgetUtils.createValueLabel(112, 82, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_LLEG] = WidgetUtils.createValueLabel(39, 103, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_RLEG] = WidgetUtils.createValueLabel(85, 103, "", fm); //$NON-NLS-1$

        // back
        vLabels[REAR_AREA_OFFSET + Mech.LOC_LT] = WidgetUtils.createValueLabel(
                132, 28, "", fm); //$NON-NLS-1$
        vLabels[REAR_AREA_OFFSET + Mech.LOC_CT] = WidgetUtils.createValueLabel(
                156, 39, "", fm); //$NON-NLS-1$
        vLabels[REAR_AREA_OFFSET + Mech.LOC_RT] = WidgetUtils.createValueLabel(
                177, 28, "", fm); //$NON-NLS-1$

        // Internal structure
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_HEAD] = WidgetUtils
                .createValueLabel(63, 142, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_LARM] = WidgetUtils
                .createValueLabel(15, 192, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_LT] = WidgetUtils
                .createValueLabel(42, 180, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_CT] = WidgetUtils
                .createValueLabel(63, 175, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_RT] = WidgetUtils
                .createValueLabel(85, 180, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_RARM] = WidgetUtils
                .createValueLabel(111, 192, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_LLEG] = WidgetUtils
                .createValueLabel(39, 215, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_RLEG] = WidgetUtils
                .createValueLabel(85, 215, "", fm); //$NON-NLS-1$
        // heat
        vLabels[19] = WidgetUtils.createValueLabel(155, 90, "", fm); //$NON-NLS-1$
    }

    private void setGroups() {
        // Have to remove it later
        PMAreasGroup frontArmor = new PMAreasGroup();
        PMAreasGroup rearArmor = new PMAreasGroup();
        PMAreasGroup intStructure = new PMAreasGroup();
        PMAreasGroup heat = new PMAreasGroup();

        for (int i = 0; i < 8; i++) {
            frontArmor.addArea(areas[i]);
            frontArmor.addArea(labels[i]);
            frontArmor.addArea(vLabels[i]);
        }

        for (int i = 0; i < 3; i++) {
            rearArmor.addArea(areas[8 + i]);
            rearArmor.addArea(labels[8 + i]);
            rearArmor.addArea(vLabels[8 + i]);
        }

        for (int i = 0; i < 8; i++) {
            intStructure.addArea(areas[11 + i]);
            intStructure.addArea(labels[11 + i]);
            intStructure.addArea(vLabels[11 + i]);
        }

        heat.addArea(heatHotArea);
        heat.addArea(vLabels[19]);

        frontArmor.translate(7, 18);
        rearArmor.translate(19, 20);
        intStructure.translate(6, 42);
        heat.translate(20, 52);

        // This have to be left
        for (int i = 0; i < 19; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }

        content.addArea(heatHotArea);
        content.addArea(vLabels[19]);
    }

    private void setBackGround() {
        Image tile = comp.getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/bg_mech.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_CENTER
                | BackGroundDrawer.HALIGN_CENTER;
        BackGroundDrawer bgd = new BackGroundDrawer(tile, b);
        bgDrawers.addElement(bgd);
    }

    private void drawHeatControl(int t) {
        drawHeatControl(t, false);
    }

    private void drawHeatControl(int t, boolean mtHeat) {
        int y = 0;
        int maxHeat, steps;
        if (mtHeat) {
            maxHeat = 50;
            steps = 2;
        } else {
            maxHeat = 30;
            steps = 4;
        }
        Graphics g = heatImage.getGraphics();
        for (int i = 0; i < maxHeat; i++) {
            y = 120 - (i + 1) * steps;
            if (i < t) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.lightGray);
            }
            g.fillRect(0, y, 10, steps);
            g.setColor(Color.black);
            g.drawRect(0, y, 10, steps);
        }
    }
}
