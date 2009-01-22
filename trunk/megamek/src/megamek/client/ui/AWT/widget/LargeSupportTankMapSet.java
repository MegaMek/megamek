/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
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
import megamek.common.LargeSupportTank;
import megamek.common.SupportTank;

/**
 * Class which keeps set of all areas required to represent Tank unit in
 * MechDsiplay.ArmorPanel class.
 */
public class LargeSupportTankMapSet implements DisplayMapSet {

    private static final String IMAGE_DIR = "data/images/widgets";

    private Component comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[14];
    private PMSimpleLabel[] labels = new PMSimpleLabel[15];
    private PMValueLabel[] vLabels = new PMValueLabel[15];
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    private static final int INT_STR_OFFSET = 6;
    // Polygons for all areas
    private Polygon frontArmor = new Polygon(new int[] { 0, 19, 149, 168, 145,
            132, 57, 23 }, new int[] { 55, 27, 27, 55, 68, 49, 49, 68 }, 8);
    
    // front internal structure
    private Polygon frontIS = new Polygon(new int[] { 87, 87, 145, 132, 57, 23,
            81, 81 }, new int[] { 40, 77, 39, 20, 20, 39, 77, 40 }, 8);
    // Left Front armor
    private Polygon leftFrontArmor = new Polygon(new int[] { 0, 0, 23, 23 },
            new int[] { 26, 120, 120, 39 }, 4);

    // Left Front internal structure
    private Polygon leftFrontIS = new Polygon(new int[] {81, 23, 23, 53, 66, 74,
            78, 81}, new int[] {77, 39, 120, 120, 94, 94, 85, 85}, 8);
    
    // Left rear armor
    private Polygon leftRearArmor = new Polygon(new int[] { 0, 0, 23, 23 },
            new int[] { 120, 214, 200, 120 }, 4);
    
    private Polygon leftRearIS = new Polygon(new int[] {23, 23, 60, 53, 53},
            new int[] {120, 200, 183, 168, 120}, 5);
    
    // Right armor
    private Polygon rightFrontArmor = new Polygon(new int[] { 168, 145, 145, 168 },
            new int[] { 26, 120, 120, 39 }, 4);

    // Right internal structure

    private Polygon rightFrontIS = new Polygon(new int[] { 103, 116, 116, 108, 145, 145,
            145, 87, 87, 91, 95 }, new int[] { 94, 120, 168, 183, 200, 200, 39,
            77, 85, 85, 94 }, 11);
    
    // Right armor
    private Polygon rightRearArmor = new Polygon(new int[] { 168, 145, 145, 168 },
            new int[] { 120, 214, 200, 120 }, 4);

    // Right internal structure

    private Polygon rightRearIS = new Polygon(new int[] { 103, 116, 116, 108, 145, 145,
            145, 87, 87, 91, 95 }, new int[] { 94, 120, 168, 183, 200, 200, 39,
            77, 85, 85, 94 }, 11);

    // Rear armor
    private Polygon rearArmor = new Polygon(new int[] { 168, 145, 112, 55, 23,
            0, 11, 166 }, new int[] { 214, 200, 220, 220, 200, 214, 239, 239 },
            8);
    // Rear internal structure
    private Polygon rearIS = new Polygon(new int[] { 145, 108, 99, 70, 60, 23,
            55, 112 }, new int[] { 200, 183, 202, 202, 183, 200, 220, 220 }, 8);
    // Turret armor
    private Polygon turretArmor = new Polygon(new int[] { 84, 94, 109, 109, 59,
            59, 74, 84, 84, 84, 84, 70, 53, 53, 56, 74, 78, 81, 81, 87, 87, 91,
            95, 103, 116, 116, 108, 109, 84 }, new int[] { 187, 187, 160, 139, 139,
            160, 187, 187, 202, 187, 202, 202, 168, 120, 94, 94, 85, 85, 40,
            40, 85, 85, 94, 94, 120, 168, 183, 202, 202 }, 29);
    // Turret internal structure
    private Polygon turretIS = new Polygon(
            new int[] { 59, 59, 74, 94, 109, 109 }, new int[] { 139, 160, 187,
                    187, 160, 139 }, 6);

    private static final Font FONT_LABEL = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    public LargeSupportTankMapSet(Component c) {
        comp = c;
        setAreas();
        setLabels();
        setBackGround();
        translateAreas();
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
        LargeSupportTank t = (LargeSupportTank) e;
        int a = 1;
        int a0 = 1;
        for (int i = 1; i < 8; i++) {
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
        }
        for (int i = 8; i < 14; i++) {
            a = t.getInternal(i - 8);
            a0 = t.getOInternal(i - 8);
            vLabels[i].setValue(t.getInternalString(i - 8));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
        }
        vLabels[14].setValue(String.valueOf(((SupportTank)t).getBARRating()));
    }

    private void setContent() {
        for (int i = 1; i < 8; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        for (int i = 1; i < 8; i++) {
            content.addArea(areas[i + INT_STR_OFFSET]);
            content.addArea(labels[i + INT_STR_OFFSET]);
            content.addArea(vLabels[i + INT_STR_OFFSET]);
        }
        content.addArea(labels[14]);
        content.addArea(vLabels[14]);
    }

    private void setAreas() {
        areas[LargeSupportTank.LOC_FRONT] = new PMSimplePolygonArea(frontArmor);
        areas[LargeSupportTank.LOC_FRONTRIGHT] = new PMSimplePolygonArea(rightFrontArmor);
        areas[LargeSupportTank.LOC_FRONTLEFT] = new PMSimplePolygonArea(leftFrontArmor);
        areas[LargeSupportTank.LOC_REARLEFT] = new PMSimplePolygonArea(leftRearArmor);
        areas[LargeSupportTank.LOC_REARRIGHT] = new PMSimplePolygonArea(rightRearArmor);
        areas[LargeSupportTank.LOC_REAR] = new PMSimplePolygonArea(rearArmor);
        areas[LargeSupportTank.LOC_TURRET] = new PMSimplePolygonArea(turretArmor);
        areas[LargeSupportTank.LOC_FRONT + INT_STR_OFFSET] = new PMSimplePolygonArea(
                frontIS);
        areas[LargeSupportTank.LOC_FRONTRIGHT + INT_STR_OFFSET] = new PMSimplePolygonArea(
                rightFrontIS);
        areas[LargeSupportTank.LOC_FRONTLEFT + INT_STR_OFFSET] = new PMSimplePolygonArea(leftFrontIS);
        areas[LargeSupportTank.LOC_REARLEFT + INT_STR_OFFSET] = new PMSimplePolygonArea(leftRearIS);
        areas[LargeSupportTank.LOC_REARRIGHT + INT_STR_OFFSET] = new PMSimplePolygonArea(rightRearIS);
        areas[LargeSupportTank.LOC_REAR + INT_STR_OFFSET] = new PMSimplePolygonArea(rearIS);
        areas[LargeSupportTank.LOC_TURRET + INT_STR_OFFSET] = new PMSimplePolygonArea(
                turretIS);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[LargeSupportTank.LOC_FRONT] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.FrontArmor"), fm, Color.black, 85, 35); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_FRONT + INT_STR_OFFSET] = WidgetUtils
                .createLabel(
                        Messages.getString("TankMapSet.FrontIS"), fm, Color.black, 83, 57); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_FRONTLEFT] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.LS"), fm, Color.black, 18, 125); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_FRONTLEFT + INT_STR_OFFSET] = WidgetUtils.createLabel(
                Messages.getString("TankMapSet.LIS"), fm, Color.black, 48, 96); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_FRONTRIGHT] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.RS"), fm, Color.black, 165, 125); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_FRONTRIGHT + INT_STR_OFFSET] = WidgetUtils.createLabel(
                Messages.getString("TankMapSet.RIS"), fm, Color.black, 136, 96); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_REARLEFT] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.LS"), fm, Color.black, 18, 165); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_REARLEFT + INT_STR_OFFSET] = WidgetUtils.createLabel(
                Messages.getString("TankMapSet.LIS"), fm, Color.black, 48, 136); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_REARRIGHT] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.RS"), fm, Color.black, 165, 165); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_REARRIGHT + INT_STR_OFFSET] = WidgetUtils.createLabel(
                Messages.getString("TankMapSet.RIS"), fm, Color.black, 136, 136); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_REAR] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.RearArmor"), fm, Color.black, 85, 257); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_REAR + INT_STR_OFFSET] = WidgetUtils
                .createLabel(
                        Messages.getString("TankMapSet.RearIS"), fm, Color.black, 83, 239); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_TURRET] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.TurretArmor"), fm, Color.black, 93, 145); //$NON-NLS-1$
        labels[LargeSupportTank.LOC_TURRET + INT_STR_OFFSET] = WidgetUtils
                .createLabel(
                        Messages.getString("TankMapSet.TurretIS"), fm, Color.black, 93, 173); //$NON-NLS-1$
        labels[14] = WidgetUtils.createLabel(Messages
                .getString("TankMapSet.BARRating"), fm, Color.white, 65, 270); //$NON-NLS-1$

        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[LargeSupportTank.LOC_FRONT] = WidgetUtils.createValueLabel(121, 37, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_FRONT + INT_STR_OFFSET] = WidgetUtils
                .createValueLabel(111, 58, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_FRONTLEFT] = WidgetUtils.createValueLabel(20, 150, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_FRONTLEFT + INT_STR_OFFSET] = WidgetUtils.createValueLabel(
                44, 121, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_FRONTRIGHT] = WidgetUtils
                .createValueLabel(165, 150, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_FRONTRIGHT + INT_STR_OFFSET] = WidgetUtils
                .createValueLabel(142, 121, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_REARLEFT] = WidgetUtils.createValueLabel(20, 150, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_REARLEFT + INT_STR_OFFSET] = WidgetUtils.createValueLabel(
                44, 121, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_REARRIGHT] = WidgetUtils
                .createValueLabel(165, 150, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_REARRIGHT + INT_STR_OFFSET] = WidgetUtils
                .createValueLabel(142, 121, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_REAR] = WidgetUtils.createValueLabel(119, 258, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_REAR + INT_STR_OFFSET] = WidgetUtils.createValueLabel(
                111, 241, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_TURRET] = WidgetUtils
                .createValueLabel(93, 159, "", fm); //$NON-NLS-1$
        vLabels[LargeSupportTank.LOC_TURRET + INT_STR_OFFSET] = WidgetUtils
                .createValueLabel(93, 193, "", fm); //$NON-NLS-1$
        vLabels[14] = WidgetUtils.createValueLabel(120, 280, "", fm); //$NON-NLS-1$
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

    private void translateAreas() {
        areas[LargeSupportTank.LOC_FRONT].translate(8, 0);
        areas[LargeSupportTank.LOC_FRONT + INT_STR_OFFSET].translate(8, 29);
        areas[LargeSupportTank.LOC_FRONTLEFT].translate(8, 29);
        areas[LargeSupportTank.LOC_FRONTLEFT + INT_STR_OFFSET].translate(8, 29);
        areas[LargeSupportTank.LOC_FRONTRIGHT].translate(8, 29);
        areas[LargeSupportTank.LOC_FRONTRIGHT + INT_STR_OFFSET].translate(8, 29);
        areas[LargeSupportTank.LOC_REARLEFT].translate(8, 29);
        areas[LargeSupportTank.LOC_REARLEFT + INT_STR_OFFSET].translate(8, 29);
        areas[LargeSupportTank.LOC_REARRIGHT].translate(8, 29);
        areas[LargeSupportTank.LOC_REARRIGHT + INT_STR_OFFSET].translate(8, 29);
        areas[LargeSupportTank.LOC_REAR].translate(8, 29);
        areas[LargeSupportTank.LOC_REAR + INT_STR_OFFSET].translate(8, 29);
        areas[LargeSupportTank.LOC_TURRET].translate(8, 29);
        areas[LargeSupportTank.LOC_TURRET + INT_STR_OFFSET].translate(8, 29);
    }
}
