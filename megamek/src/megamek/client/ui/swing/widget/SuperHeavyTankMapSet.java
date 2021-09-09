/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
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
import megamek.common.SuperHeavyTank;

/**
 * Class which keeps set of all areas required to represent Tank unit in
 * MechDsiplay.ArmorPanel class.
 */
public class SuperHeavyTankMapSet implements DisplayMapSet {

    private UnitDisplay unitDisplay;
    
    private JComponent comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[15];
    private PMSimpleLabel[] labels = new PMSimpleLabel[16];
    private PMValueLabel[] vLabels = new PMValueLabel[16];
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    // Polygons for all areas

    // front armor
    private Polygon frontArmor = new Polygon(
            new int[] { 0, 23, 157, 180, 157, 118, 62, 23 },
            new int[] { 30, 7, 7,   30,  41,  25,  25, 41 }, 8);

    // front internal structure
    private Polygon frontIS = new Polygon(
            new int[] { 23, 62, 118, 157, 93, 93, 87, 87 },
            new int[] { 41, 25, 25,  41,  73, 42, 42, 73 }, 8);
    // Left Front armor
    private Polygon leftFrontArmor = new Polygon(
            new int[] { 0,  0,   23,  23 },
            new int[] { 30, 109, 109, 41 }, 4);

    // Left Front internal structure
    private Polygon leftFrontIS = new Polygon(
            new int[] {23, 87, 87, 84, 80, 71, 59,  23},
            new int[] {41, 73, 80, 80, 87, 87, 109, 109}, 8);

    // Left rear armor
    private Polygon leftRearArmor = new Polygon(
            new int[] { 0,   0,   23,  23 },
            new int[] { 109, 187, 175, 109 }, 4);

    // Left rear internal structure
    private Polygon leftRearIS = new Polygon(
            new int[] {23, 23,   66,  59,  59},
            new int[] {109, 175, 161, 149, 109}, 5);

    // Right front armor
    private Polygon rightFrontArmor = new Polygon(
            new int[] { 157, 180, 180, 157 },
            new int[] { 41,  30,  109, 109 }, 4);

    // Right front internal structure
    private Polygon rightFrontIS = new Polygon(
            new int[] { 93, 157, 157, 121, 109, 100, 96, 93 },
            new int[] { 73, 41,  109, 109, 87,  87,  80, 80 }, 8);

    // Right rear armor
    private Polygon rightRearArmor = new Polygon(
            new int[] { 157, 180, 180, 157 },
            new int[] { 109, 109, 187, 175 }, 4);

    // Right rear internal structure
    private Polygon rightRearIS = new Polygon(
            new int[] { 121, 157, 157, 114, 121, 121 },
            new int[] { 109, 109, 175, 161, 149, 109 }, 6);

    // Rear armor
    private Polygon rearArmor = new Polygon(
            new int[] { 180, 152, 26,  0,   23,  59,  121, 157 },
            new int[] { 187, 208, 208, 187, 175, 192, 192, 175 }, 8);

    // Rear internal structure
    private Polygon rearIS = new Polygon(
            new int[] { 157, 121, 59,  23,  66,  76,  105, 114 },
            new int[] { 175, 192, 192, 175, 161, 177, 177, 161 }, 8);

    // Turret armor
    private Polygon turretArmor = new Polygon(
            new int[] { 87, 87, 84, 80, 71, 59,  59,  75,  90,  90,  80,  65,
            65,  115, 115, 100, 90,  90,  105, 121, 121, 109, 100, 96, 93, 93 },
            new int[] { 42, 80, 80, 87, 87, 109, 149, 177, 177, 165, 165, 142,
            125, 125, 142, 165, 165, 177, 177, 149, 109, 87,  87 , 80, 80, 42 },
            26);
    // Turret internal structure
    private Polygon turretIS = new Polygon(
            new int[] { 65,  65,  80,  100, 115, 115 },
            new int[] { 125, 142, 165, 165, 142, 125 }, 6);

    private static final Font FONT_LABEL = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    public SuperHeavyTankMapSet(JComponent c, UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        comp = c;
        setAreas();
        setLabels();
        setBackGround();
        //translateAreas();
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
        SuperHeavyTank t = (SuperHeavyTank) e;
        int a = 1;
        int a0 = 1;
        for (int i = 1; i < 8; i++) {
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
        }
        for (int i = 8; i < 15; i++) {
            a = t.getInternal(i - 8);
            a0 = t.getOInternal(i - 8);
            vLabels[i].setValue(t.getInternalString(i - 8));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                    / (double) a0);
        }
    }

    private void setContent() {
        for (int i = 1; i < 15; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        content.addArea(labels[15]);
        content.addArea(vLabels[15]);
    }

    private void setAreas() {
        areas[1] = new PMSimplePolygonArea(frontArmor, unitDisplay, SuperHeavyTank.LOC_FRONT);
        areas[2] = new PMSimplePolygonArea(rightFrontArmor, unitDisplay, SuperHeavyTank.LOC_FRONTRIGHT);
        areas[3] = new PMSimplePolygonArea(leftFrontArmor, unitDisplay, SuperHeavyTank.LOC_FRONTLEFT);
        areas[4] = new PMSimplePolygonArea(rightRearArmor, unitDisplay, SuperHeavyTank.LOC_REARRIGHT);
        areas[5] = new PMSimplePolygonArea(leftRearArmor, unitDisplay, SuperHeavyTank.LOC_REARLEFT);
        areas[6] = new PMSimplePolygonArea(rearArmor, unitDisplay, SuperHeavyTank.LOC_REAR);
        areas[7] = new PMSimplePolygonArea(turretArmor, unitDisplay, SuperHeavyTank.LOC_TURRET);
        areas[8] = new PMSimplePolygonArea(frontIS, unitDisplay, SuperHeavyTank.LOC_FRONT);
        areas[9] = new PMSimplePolygonArea(rightFrontIS, unitDisplay, SuperHeavyTank.LOC_FRONTRIGHT);
        areas[10] = new PMSimplePolygonArea(leftFrontIS, unitDisplay, SuperHeavyTank.LOC_FRONTLEFT);
        areas[11] = new PMSimplePolygonArea(rightRearIS, unitDisplay, SuperHeavyTank.LOC_REARRIGHT);
        areas[12] = new PMSimplePolygonArea(leftRearIS, unitDisplay, SuperHeavyTank.LOC_REARLEFT);
        areas[13] = new PMSimplePolygonArea(rearIS, unitDisplay, SuperHeavyTank.LOC_REAR);
        areas[14] = new PMSimplePolygonArea(turretIS, unitDisplay, SuperHeavyTank.LOC_TURRET);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[1] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.FrontArmor"), fm, Color.black, 85, 15); //$NON-NLS-1$
        labels[2] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.FRS"), fm, Color.black, 170, 80); //$NON-NLS-1$
        labels[3] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.FLS"), fm, Color.black, 10, 80); //$NON-NLS-1$
        labels[4] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.RRS"), fm, Color.black, 170, 155); //$NON-NLS-1$
        labels[5] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.RLS"), fm, Color.black, 10, 155); //$NON-NLS-1$
        labels[6] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.RearArmor"), fm, Color.black, 85, 200); //$NON-NLS-1$
        labels[7] = WidgetUtils.createLabel(Messages
                .getString("LargeSupportTankMapSet.TurretArmor"), fm, Color.black, 90, 104); //$NON-NLS-1$
        labels[8] = WidgetUtils
                .createLabel(
                        Messages.getString("LargeSupportTankMapSet.FrontIS"), fm, Color.black, 80, 30); //$NON-NLS-1$
        labels[9] = WidgetUtils.createLabel(
                Messages.getString("LargeSupportTankMapSet.FRIS"), fm, Color.black, 120, 80); //$NON-NLS-1$
        labels[10] = WidgetUtils.createLabel(
                Messages.getString("LargeSupportTankMapSet.FLIS"), fm, Color.black, 43, 80); //$NON-NLS-1$
        labels[11] = WidgetUtils.createLabel(
                Messages.getString("LargeSupportTankMapSet.RRIS"), fm, Color.black, 140, 155); //$NON-NLS-1$
        labels[12] = WidgetUtils.createLabel(
                Messages.getString("LargeSupportTankMapSet.RLIS"), fm, Color.black, 43, 155); //$NON-NLS-1$
        labels[13] = WidgetUtils
                .createLabel(
                        Messages.getString("LargeSupportTankMapSet.RearIS"), fm, Color.black, 85, 185); //$NON-NLS-1$
        labels[14] = WidgetUtils
                .createLabel(
                        Messages.getString("LargeSupportTankMapSet.TurretIS"), fm, Color.black, 90, 140); //$NON-NLS-1$

        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[1] = WidgetUtils.createValueLabel(115, 17, "", fm); //$NON-NLS-1$
        vLabels[2] = WidgetUtils
        .createValueLabel(164, 70, "", fm); //$NON-NLS-1$
        vLabels[3] = WidgetUtils.createValueLabel(6, 70, "", fm); //$NON-NLS-1$
        vLabels[4] = WidgetUtils.createValueLabel(
                164, 140, "", fm); //$NON-NLS-1$
        vLabels[5] = WidgetUtils
        .createValueLabel(6, 140, "", fm); //$NON-NLS-1$
        vLabels[6] = WidgetUtils
        .createValueLabel(113, 202, "", fm); //$NON-NLS-1$
        vLabels[7] = WidgetUtils
        .createValueLabel(93, 115, "", fm); //$NON-NLS-1$
        vLabels[8] = WidgetUtils.createValueLabel(93, 151, "", fm);//$NON-NLS-1$
        vLabels[9] = WidgetUtils
        .createValueLabel(140, 65, "", fm); //$NON-NLS-1$
        vLabels[10] = WidgetUtils
        .createValueLabel(43, 65, "", fm); //$NON-NLS-1$
        vLabels[11] = WidgetUtils
        .createValueLabel(145, 140, "", fm); //$NON-NLS-1$
        vLabels[12] = WidgetUtils
        .createValueLabel(43, 140, "", fm); //$NON-NLS-1$
        vLabels[13] = WidgetUtils
        .createValueLabel(113,187, "", fm); //$NON-NLS-1$
        vLabels[14] = WidgetUtils
        .createValueLabel(
                110, 32, "", fm); //$NON-NLS-1$
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

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getTopLine())
                        .toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getBottomLine())
                        .toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getLeftLine())
                        .toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getRightLine())
                        .toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getTopLeftCorner())
                        .toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec
                        .getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit()
                .getImage(
                        new File(Configuration.widgetsDir(), udSpec
                                .getTopRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec
                        .getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

}
