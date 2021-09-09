/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
import java.io.File;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.options.OptionsConstants;

/**
 * Very cumbersome class that handles set of polygonal areas and labels for
 * PicMap component to represent single mech unit in MechDisplay
 */

public class ArmlessMechMapSet implements DisplayMapSet {

    // Because of keeping all areas of single type in one array
    // some index offset values required
    private static final int REAR_AREA_OFFSET = 7;
    private static final int INT_STRUCTURE_OFFSET = 11;
    
    private UnitDisplay unitDisplay;

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
    // Head
    private Polygon head = new Polygon(new int[] { 53, 71, 81, 83, 83, 62, 41,
            41, 43 }, new int[] { 32, 32, 22, 22, 8, 0, 8, 22, 22 }, 9);
    // Central Torso
    private Polygon centralTorso = new Polygon(new int[] { 48, 54, 70, 76, 76,
            48 }, new int[] { 45, 85, 85, 45, 36, 36 }, 6);
    // left Torso
    private Polygon leftTorso = new Polygon(new int[] { 54, 48, 48, 62, 62, 53,
            43, 41, 41, 31, 29, 27, 25, 26, 47 }, new int[] { 82, 45, 36, 36,
            32, 32, 22, 22, 20, 25, 27, 33, 37, 47, 82 }, 15);
    // right Torso
    private Polygon rightTorso = new Polygon(new int[] { 70, 76, 76, 62, 62,
            71, 81, 83, 83, 93, 95, 97, 99, 98, 77 }, new int[] { 82, 45, 36,
            36, 32, 32, 22, 22, 20, 25, 27, 33, 37, 47, 82 }, 15);
    // Left Leg

    private Polygon leftLeg = new Polygon(new int[] { 0, 7, 37, 47, 54, 54, 61,
            34, 40, 40, 34, 33, 7, 6, 0 }, new int[] { 104, 104, 65, 82, 82,
            85, 85, 102, 104, 121, 123, 129, 129, 122, 122 }, 15);
    // right Leg
    private Polygon rightLeg = new Polygon(new int[] { 125, 118, 88, 77, 70,
            70, 64, 91, 85, 85, 91, 92, 118, 119, 125 }, new int[] { 104, 104,
            63, 82, 82, 85, 85, 102, 104, 121, 123, 129, 129, 122, 122 }, 15);

    // Mek Armor - Rear
    // Left Torso

    private Polygon rearLeftTorso = new Polygon(new int[] { 142, 142, 148, 139,
            123, 123, 142 }, new int[] { 14, 43, 76, 76, 44, 17, 14 }, 7);
    // Central Torso

    private Polygon rearCentralTorso = new Polygon(new int[] { 142, 148, 162,
            168, 168, 142 }, new int[] { 44, 76, 76, 44, 14, 14 }, 6);
    // Right Torso

    private Polygon rearRightTorso = new Polygon(new int[] { 168, 168, 162,
            171, 187, 187, 168 }, new int[] { 14, 43, 76, 76, 44, 17, 14 }, 7);

    // Internal Structure
    // Head
    private Polygon intStHead = new Polygon(new int[] { 78, 48, 48, 78 },
            new int[] { 149, 149, 127, 127 }, 4);
    // Central Torso
    private Polygon inStCentralTorso = new Polygon(
            new int[] { 75, 75, 51, 51 }, new int[] { 203, 149, 149, 203 }, 4);
    // Left Torso
    private Polygon inStLeftTorso = new Polygon(new int[] { 32, 32, 51, 51 },
            new int[] { 188, 160, 160, 193 }, 4);
    // Right Torso
    private Polygon inStRightTorso = new Polygon(new int[] { 94, 94, 75, 75 },
            new int[] { 188, 160, 160, 193 }, 4);
    // Left Leg
    private Polygon inStLeftLeg = new Polygon(new int[] { 51, 51, 44, 44, 47,
            47, 20, 20, 41, 41, 44, 44 }, new int[] { 195, 199, 199, 206, 206,
            230, 230, 206, 206, 192, 192, 195 }, 12);
    // right Leg
    private Polygon inStRightLeg = new Polygon(new int[] { 75, 75, 82, 82, 79,
            79, 106, 106, 85, 85, 82, 82 }, new int[] { 195, 199, 199, 206,
            206, 230, 230, 206, 206, 192, 192, 195 }, 12);
    // Heat control
    private Polygon heatControl = new Polygon(new int[] { 149, 159, 159, 149 },
            new int[] { 100, 100, 220, 220 }, 4);

    private Image heatImage;

    private static final Font FONT_LABEL = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    public ArmlessMechMapSet(JComponent c, UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
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
        if (e.getGame() != null && e.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
            mtHeat = true;
        }
        int a = 1;
        int a0 = 1;
        for (int i = 0; i < m.locations(); i++) {
            if (i == Mech.LOC_LARM || i == Mech.LOC_RARM) {
                continue;
            }
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
        areas[Mech.LOC_HEAD] = new PMSimplePolygonArea(head, unitDisplay,
                Mech.LOC_HEAD);
        areas[Mech.LOC_CT] = new PMSimplePolygonArea(centralTorso, unitDisplay,
                Mech.LOC_CT);
        areas[Mech.LOC_RT] = new PMSimplePolygonArea(rightTorso, unitDisplay,
                Mech.LOC_RT);
        areas[Mech.LOC_LT] = new PMSimplePolygonArea(leftTorso, unitDisplay,
                Mech.LOC_LT);
        areas[Mech.LOC_RLEG] = new PMSimplePolygonArea(rightLeg, unitDisplay,
                Mech.LOC_RLEG);
        areas[Mech.LOC_LLEG] = new PMSimplePolygonArea(leftLeg, unitDisplay,
                Mech.LOC_LLEG);
        areas[REAR_AREA_OFFSET + Mech.LOC_CT] = new PMSimplePolygonArea(
                rearCentralTorso, unitDisplay, Mech.LOC_CT);
        areas[REAR_AREA_OFFSET + Mech.LOC_RT] = new PMSimplePolygonArea(
                rearRightTorso, unitDisplay, Mech.LOC_RT);
        areas[REAR_AREA_OFFSET + Mech.LOC_LT] = new PMSimplePolygonArea(
                rearLeftTorso, unitDisplay, Mech.LOC_LT);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_HEAD] = new PMSimplePolygonArea(
                intStHead, unitDisplay, Mech.LOC_HEAD);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_CT] = new PMSimplePolygonArea(
                inStCentralTorso, unitDisplay, Mech.LOC_CT);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_RT] = new PMSimplePolygonArea(
                inStRightTorso, unitDisplay, Mech.LOC_RT);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_LT] = new PMSimplePolygonArea(
                inStLeftTorso, unitDisplay, Mech.LOC_LT);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_RLEG] = new PMSimplePolygonArea(
                inStRightLeg, unitDisplay, Mech.LOC_RLEG);
        areas[INT_STRUCTURE_OFFSET + Mech.LOC_LLEG] = new PMSimplePolygonArea(
                inStLeftLeg, unitDisplay, Mech.LOC_LLEG);
        heatImage = comp.createImage(10, 120);
        drawHeatControl(0);
        heatHotArea = new PMPicPolygonalArea(heatControl, heatImage);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[Mech.LOC_HEAD] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_H"), fm, Color.black, 62, 6); //$NON-NLS-1$
        labels[Mech.LOC_LT] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_LT"), fm, Color.black, 41, 52); //$NON-NLS-1$
        labels[Mech.LOC_CT] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_CT"), fm, Color.black, 62, 42); //$NON-NLS-1$
        labels[Mech.LOC_RT] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_RT"), fm, Color.black, 84, 52); //$NON-NLS-1$
        labels[Mech.LOC_LLEG] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_LL"), fm, Color.black, 28, 92); //$NON-NLS-1$
        labels[Mech.LOC_RLEG] = WidgetUtils.createLabel(Messages
                .getString("MechMapSet.l_RL"), fm, Color.black, 98, 92); //$NON-NLS-1$
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
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_LT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_LT"), fm, Color.black, 42, 166); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_CT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.L_CT"), fm, Color.black, 63, 168); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_RT] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_RT"), fm, Color.black, 85, 166); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_LLEG] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_LL"), fm, Color.black, 33, 210); //$NON-NLS-1$
        labels[INT_STRUCTURE_OFFSET + Mech.LOC_RLEG] = WidgetUtils
                .createLabel(
                        Messages.getString("MechMapSet.l_RL"), fm, Color.black, 93, 210); //$NON-NLS-1$

        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[Mech.LOC_HEAD] = WidgetUtils.createValueLabel(62, 22, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_LT] = WidgetUtils.createValueLabel(38, 44, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_CT] = WidgetUtils.createValueLabel(62, 57, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_RT] = WidgetUtils.createValueLabel(86, 44, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_LLEG] = WidgetUtils.createValueLabel(23, 113, "", fm); //$NON-NLS-1$
        vLabels[Mech.LOC_RLEG] = WidgetUtils.createValueLabel(102, 113, "", fm); //$NON-NLS-1$

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
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_LT] = WidgetUtils
                .createValueLabel(42, 180, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_CT] = WidgetUtils
                .createValueLabel(63, 182, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_RT] = WidgetUtils
                .createValueLabel(85, 180, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_LLEG] = WidgetUtils
                .createValueLabel(33, 223, "", fm); //$NON-NLS-1$
        vLabels[INT_STRUCTURE_OFFSET + Mech.LOC_RLEG] = WidgetUtils
                .createValueLabel(92, 223, "", fm); //$NON-NLS-1$
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
            if (i == Mech.LOC_LARM || i == Mech.LOC_RARM) {
                continue;
            }
            frontArmor.addArea(areas[i]);
            frontArmor.addArea(labels[i]);
            frontArmor.addArea(vLabels[i]);
        }
        // content.addArea(new PMSimplePolygonArea(new Polygon(new
        // int[]{-7,-6,-7}, new int[]{18,19,19},3)));

        for (int i = 0; i < 3; i++) {
            rearArmor.addArea(areas[8 + i]);
            rearArmor.addArea(labels[8 + i]);
            rearArmor.addArea(vLabels[8 + i]);
        }

        for (int i = 0; i < 8; i++) {
            if (i == Mech.LOC_LARM || i == Mech.LOC_RARM) {
                continue;
            }
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
            if (i == Mech.LOC_LARM || i == Mech.LOC_RARM) {
                continue;
            }
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }

        content.addArea(heatHotArea);
        content.addArea(vLabels[19]);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler
                .getUnitDisplaySkin();

        Image tile = comp.getToolkit()
                .getImage(
                        new File(Configuration.widgetsDir(), udSpec
                                .getBackgroundTile()).toString());
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getMechOutline())
                        .toString());
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
