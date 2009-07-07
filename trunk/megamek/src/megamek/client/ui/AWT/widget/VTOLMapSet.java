/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Entity;
import megamek.common.SupportVTOL;
import megamek.common.VTOL;

/**
 * Class which keeps set of all areas required to represent Tank unit in
 * MechDsiplay.ArmorPanel class.
 */

public class VTOLMapSet implements DisplayMapSet {

    private static final String IMAGE_DIR = "data/images/widgets";

    private Component comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[16];
    private PMSimpleLabel[] labels = new PMSimpleLabel[23];
    private PMValueLabel[] vLabels = new PMValueLabel[17];
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    // Polygons for all areas
    private Polygon frontArmor = new Polygon(new int[] { 30, 60, 90, 120 },
            new int[] { 30, 0, 0, 30 }, 4);
    // front internal structure
    private Polygon frontIS = new Polygon(new int[] { 30, 120, 90, 60 },
            new int[] { 30, 30, 45, 45 }, 4);
    // Left armor
    private Polygon leftArmor1 = new Polygon(new int[] { 30, 30, 60, 60 },
            new int[] { 75, 30, 45, 75 }, 4);
    private Polygon leftArmor2 = new Polygon(new int[] { 30, 30, 60, 60 },
            new int[] { 135, 90, 90, 150 }, 4);
    // Left internal structure
    private Polygon leftIS1 = new Polygon(new int[] { 60, 60, 75, 75 },
            new int[] { 75, 45, 45, 75 }, 4);
    private Polygon leftIS2 = new Polygon(new int[] { 60, 60, 75, 75 },
            new int[] { 150, 90, 90, 150 }, 4);
    // Right armor
    private Polygon rightArmor1 = new Polygon(new int[] { 90, 90, 120, 120 },
            new int[] { 75, 45, 30, 75 }, 4);
    private Polygon rightArmor2 = new Polygon(new int[] { 90, 90, 120, 120 },
            new int[] { 150, 90, 90, 135 }, 4);
    // Right internal structure
    private Polygon rightIS1 = new Polygon(new int[] { 75, 75, 90, 90 },
            new int[] { 75, 45, 45, 75 }, 4);
    private Polygon rightIS2 = new Polygon(new int[] { 75, 75, 90, 90 },
            new int[] { 150, 90, 90, 150 }, 4);
    // Rear armor
    private Polygon rearArmor = new Polygon(new int[] { 67, 67, 83, 83 },
            new int[] { 180, 150, 150, 180 }, 4);
    // Rear internal structure
    private Polygon rearIS = new Polygon(new int[] { 67, 67, 83, 83 },
            new int[] { 240, 180, 180, 240 }, 4);
    // Rotor armor
    private Polygon rotorArmor1 = new Polygon(new int[] { 0, 0, 45, 45 },
            new int[] { 90, 75, 75, 90 }, 4);
    private Polygon rotorArmor2 = new Polygon(new int[] { 105, 105, 150, 150 },
            new int[] { 90, 75, 75, 90 }, 4);
    // Rotor internal structure
    private Polygon rotorIS = new Polygon(new int[] { 45, 45, 105, 105 },
            new int[] { 90, 75, 75, 90 }, 4);

    private static final Font FONT_LABEL = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    public VTOLMapSet(Component c) {
        comp = c;
        setAreas();
        setLabels();
        setBackGround();
        setContent();
    }

    public void setRest() {
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    public void setEntity(Entity e) {
        VTOL t = (VTOL) e;
        int a = 1;
        int a0 = 1;
        int x = 0;
        for (int i = 1; i <= 8; i++) {
            switch (i) {
                case 1:
                    x = 1;
                    break;
                case 2:
                    x = 2;
                    break;
                case 3:
                    x = 2;
                    break;
                case 4:
                    x = 3;
                    break;
                case 5:
                    x = 3;
                    break;
                case 6:
                    x = 4;
                    break;
                case 7:
                    x = 5;
                    break;
                case 8:
                    x = 5;
                    break;
            }
            a = t.getArmor(x);
            a0 = t.getOArmor(x);
            vLabels[i].setValue(t.getArmorString(x));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
        }
        for (int i = 9; i <= 15; i++) {
            switch (i) {
                case 9:
                    x = 1;
                    break;
                case 10:
                    x = 2;
                    break;
                case 11:
                    x = 2;
                    break;
                case 12:
                    x = 3;
                    break;
                case 13:
                    x = 3;
                    break;
                case 14:
                    x = 4;
                    break;
                case 15:
                    x = 5;
                    break;
            }
            a = t.getInternal(x);
            a0 = t.getOInternal(x);
            vLabels[i].setValue(t.getInternalString(x));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
        }
        if (t instanceof SupportVTOL) {
            vLabels[16].setValue(String.valueOf(((SupportVTOL)t).getBARRating()));
        } else {
            vLabels[16].setVisible(false);
            labels[22].setVisible(false);
        }
    }

    private void setContent() {
        for (int i = 1; i <= 15; i++) {
            content.addArea(areas[i]);
            content.addArea(vLabels[i]);
        }
        for (int i = 1; i <= 21; i++) {
            content.addArea(labels[i]);
        }
        content.addArea(vLabels[16]);
        content.addArea(labels[22]);
    }

    private void setAreas() {
        areas[1] = new PMSimplePolygonArea(frontArmor);
        areas[2] = new PMSimplePolygonArea(rightArmor1);
        areas[3] = new PMSimplePolygonArea(rightArmor2);
        areas[4] = new PMSimplePolygonArea(leftArmor1);
        areas[5] = new PMSimplePolygonArea(leftArmor2);
        areas[6] = new PMSimplePolygonArea(rearIS);
        areas[7] = new PMSimplePolygonArea(rotorArmor1);
        areas[8] = new PMSimplePolygonArea(rotorArmor2);
        areas[9] = new PMSimplePolygonArea(frontIS);
        areas[10] = new PMSimplePolygonArea(rightIS1);
        areas[11] = new PMSimplePolygonArea(rightIS2);
        areas[12] = new PMSimplePolygonArea(leftIS1);
        areas[13] = new PMSimplePolygonArea(leftIS2);
        areas[14] = new PMSimplePolygonArea(rearArmor);
        areas[15] = new PMSimplePolygonArea(rotorIS);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[1] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.FrontArmor"), fm, Color.black, 68, 20); //$NON-NLS-1$
        labels[2] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.LS"), fm, Color.black, 44, 50); //$NON-NLS-1$
        labels[3] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.LS"), fm, Color.black, 44, 100); //$NON-NLS-1$
        labels[4] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.RS"), fm, Color.black, 104, 50); //$NON-NLS-1$
        labels[5] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.RS"), fm, Color.black, 104, 100); //$NON-NLS-1$
        labels[6] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearArmor1"), fm, Color.black, 76, 185); //$NON-NLS-1$
        labels[7] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearArmor2"), fm, Color.black, 76, 195); //$NON-NLS-1$
        labels[8] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RotorArmor"), fm, Color.black, 18, 82); //$NON-NLS-1$
        labels[9] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RotorArmor"), fm, Color.black, 123, 82); //$NON-NLS-1$
        labels[10] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.FrontIS"), fm, Color.black, 68, 35); //$NON-NLS-1$
        labels[11] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS1"), fm, Color.black, 68, 48); //$NON-NLS-1$
        labels[12] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS2"), fm, Color.black, 68, 57); //$NON-NLS-1$
        labels[13] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS1"), fm, Color.black, 68, 100); //$NON-NLS-1$
        labels[14] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS2"), fm, Color.black, 68, 110); //$NON-NLS-1$
        labels[15] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS1"), fm, Color.black, 84, 48); //$NON-NLS-1$
        labels[16] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS2"), fm, Color.black, 84, 57); //$NON-NLS-1$
        labels[17] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS1"), fm, Color.black, 84, 100); //$NON-NLS-1$
        labels[18] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS2"), fm, Color.black, 84, 110); //$NON-NLS-1$
        labels[19] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearIS1"), fm, Color.black, 76, 152); //$NON-NLS-1$
        labels[20] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearIS2"), fm, Color.black, 76, 161); //$NON-NLS-1$
        labels[21] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RotorIS"), fm, Color.black, 73, 82); //$NON-NLS-1$
        labels[22] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.BARRating"), fm, Color.white, 65, 198); //$NON-NLS-1$


        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[1] = WidgetUtils.createValueLabel(101, 22, "", fm); //$NON-NLS-1$ Front
        vLabels[2] = WidgetUtils.createValueLabel(105, 65, "", fm); //$NON-NLS-1$ RS
        vLabels[3] = WidgetUtils.createValueLabel(105, 115, "", fm); //$NON-NLS-1$ RS
        vLabels[4] = WidgetUtils.createValueLabel(44, 65, "", fm); //$NON-NLS-1$ LS
        vLabels[5] = WidgetUtils.createValueLabel(44, 115, "", fm); //$NON-NLS-1$ LS
        vLabels[6] = WidgetUtils.createValueLabel(76, 207, "", fm); //$NON-NLS-1$ Rear
        vLabels[7] = WidgetUtils.createValueLabel(38, 83, "", fm); //$NON-NLS-1$ Rotor
        vLabels[8] = WidgetUtils.createValueLabel(143, 83, "", fm); //$NON-NLS-1$ Rotor
        vLabels[9] = WidgetUtils.createValueLabel(94, 37, "", fm); //$NON-NLS-1$ Front
        vLabels[10] = WidgetUtils.createValueLabel(68, 68, "", fm); //$NON-NLS-1$ LS
        vLabels[11] = WidgetUtils.createValueLabel(68, 122, "", fm); //$NON-NLS-1$ LS
        vLabels[12] = WidgetUtils.createValueLabel(84, 68, "", fm); //$NON-NLS-1$ RS
        vLabels[13] = WidgetUtils.createValueLabel(84, 122, "", fm); //$NON-NLS-1$ RS
        vLabels[14] = WidgetUtils.createValueLabel(76, 172, "", fm); //$NON-NLS-1$ Rear
        vLabels[15] = WidgetUtils.createValueLabel(98, 83, "", fm); //$NON-NLS-1$ Rotor
        vLabels[16] = WidgetUtils.createValueLabel(100, 200, "", fm); //$NON-NLS-1$
    }

    private void setBackGround() {
        Image tile = comp.getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }
}
