/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004,2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.SupportVTOL;
import megamek.common.VTOL;

/**
 * Class which keeps set of all areas required to represent Tank unit in
 * MechDsiplay.ArmorPanel class.
 */

public class VTOLMapSet implements DisplayMapSet {
    
    public static final int LABEL_NONE          = 0;
    public static final int LABEL_CHIN_TU_ARMOR = 1;
    public static final int LABEL_FRONT_ARMOR   = 2;
    public static final int LABEL_RIGHT_ARMOR_1 = 3;
    public static final int LABEL_RIGHT_ARMOR_2 = 4;
    public static final int LABEL_LEFT_ARMOR_1  = 5;
    public static final int LABEL_LEFT_ARMOR_2  = 6;
    public static final int LABEL_REAR_ARMOR    = 7;
    public static final int LABEL_ROTOR_ARMOR_1 = 8;
    public static final int LABEL_ROTOR_ARMOR_2 = 9;
    public static final int LABEL_NUM_ARMORS    = 10;
    public static final int LABEL_CHIN_TU_IS    = 10;
    public static final int LABEL_FRONT_IS      = 11;
    public static final int LABEL_RIGHT_IS_1    = 12;
    public static final int LABEL_RIGHT_IS_2    = 13;
    public static final int LABEL_LEFT_IS_1     = 14;
    public static final int LABEL_LEFT_IS_2     = 15;
    public static final int LABEL_REAR_IS       = 16;
    public static final int LABEL_ROTOR_IS      = 17;
    public static final int LABEL_LOC_NUMBER    = 18;
    public static final int LABEL_BAR_RATING    = 18;

    UnitDisplay unitDisplay;
    
    private JComponent comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[LABEL_LOC_NUMBER];
    private PMSimpleLabel[] labels = new PMSimpleLabel[25];
    private PMValueLabel[] vLabels = new PMValueLabel[LABEL_LOC_NUMBER+1];
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    // Polygons for all areas
    // Chin Turret Armor
    private Polygon chinTurretArmor = new Polygon( new int[] {50, 50, 100, 100},
            new int[] {5, -50, -50, 5}, 4);
    // Chin Turret IS
    private Polygon chinTurretIS = new Polygon( new int[] {60, 60, 90, 90},
            new int[] {0, -25, -25, 0}, 4);
    // front armor
    private Polygon frontArmor = new Polygon(new int[] { 30, 60, 90, 120 },
            new int[] { 30, 0, 0, 30 }, 4);
    // front internal structure
    private Polygon frontIS = new Polygon(new int[] { 30, 60, 90, 120 },
            new int[] { 30, 45, 45, 30 }, 4);
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

    public VTOLMapSet(JComponent c, UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
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
        VTOL vtol = (VTOL) e;
        int armor = 1;
        int originalArmor = 1;
        int location = 0;
        
        // Cycle through the labels
        for (int i = LABEL_NONE+1; i < LABEL_LOC_NUMBER; i++) {
            // Only draw Chin Turret if it is present
            if ((i == LABEL_CHIN_TU_ARMOR || i == LABEL_CHIN_TU_IS) && vtol.hasNoTurret()) {
                continue;
            }
            
            switch (i) {
                case LABEL_CHIN_TU_ARMOR:
                case LABEL_CHIN_TU_IS:
                    location = VTOL.LOC_TURRET;
                    break;
                case LABEL_FRONT_ARMOR:
                case LABEL_FRONT_IS:
                    location = VTOL.LOC_FRONT;
                    break;
                case LABEL_RIGHT_ARMOR_1:
                case LABEL_RIGHT_ARMOR_2:
                case LABEL_RIGHT_IS_1:
                case LABEL_RIGHT_IS_2:
                    location = VTOL.LOC_RIGHT;
                    break;
                case LABEL_LEFT_ARMOR_1:
                case LABEL_LEFT_ARMOR_2:
                case LABEL_LEFT_IS_1:
                case LABEL_LEFT_IS_2:
                    location = VTOL.LOC_LEFT;
                    break;
                case LABEL_REAR_ARMOR:
                case LABEL_REAR_IS:
                    location = VTOL.LOC_REAR;
                    break;
                case LABEL_ROTOR_ARMOR_1:
                case LABEL_ROTOR_ARMOR_2:
                case LABEL_ROTOR_IS:
                    location = VTOL.LOC_ROTOR;
                    break;
            }
            if (i < LABEL_NUM_ARMORS) { // Armor
                armor = vtol.getArmor(location);
                originalArmor = vtol.getOArmor(location);
                vLabels[i].setValue(vtol.getArmorString(location));
            } else { // IS
                armor = vtol.getInternal(location);
                originalArmor = vtol.getOInternal(location);
                vLabels[i].setValue(vtol.getInternalString(location));
            }
            WidgetUtils.setAreaColor(areas[i], vLabels[i],
                    (double) armor / (double) originalArmor);
        }
        if (vtol.hasNoTurret()) {
            vLabels[LABEL_CHIN_TU_ARMOR].setVisible(false);
            vLabels[LABEL_CHIN_TU_IS].setVisible(false);
            labels[LABEL_CHIN_TU_ARMOR].setVisible(false);
            labels[LABEL_CHIN_TU_IS+1].setVisible(false);
            areas[LABEL_CHIN_TU_ARMOR].setVisible(false);
            areas[LABEL_CHIN_TU_IS].setVisible(false);
        }
        if ((vtol instanceof SupportVTOL) && !vtol.hasPatchworkArmor()) {
            vLabels[LABEL_BAR_RATING].setValue(String.valueOf(((SupportVTOL)vtol).getBARRating(1)));
        } else {
            labels[LABEL_BAR_RATING+6].setVisible(false);
            vLabels[LABEL_BAR_RATING].setVisible(false);
        }
    }

    private void setContent() {
        for (int i = 1; i < areas.length; i++) {
            content.addArea(areas[i]);
            content.addArea(vLabels[i]);
        }
        for (int i = 1; i < labels.length; i++) {
            content.addArea(labels[i]);
        }
    }

    private void setAreas() {
        areas[LABEL_FRONT_ARMOR] = new PMSimplePolygonArea(frontArmor, unitDisplay, VTOL.LOC_FRONT);
        areas[LABEL_RIGHT_ARMOR_1] = new PMSimplePolygonArea(rightArmor1, unitDisplay, VTOL.LOC_RIGHT);
        areas[LABEL_RIGHT_ARMOR_2] = new PMSimplePolygonArea(rightArmor2, unitDisplay, VTOL.LOC_RIGHT);
        areas[LABEL_LEFT_ARMOR_1] = new PMSimplePolygonArea(leftArmor1, unitDisplay, VTOL.LOC_LEFT);
        areas[LABEL_LEFT_ARMOR_2] = new PMSimplePolygonArea(leftArmor2, unitDisplay, VTOL.LOC_LEFT);
        areas[LABEL_REAR_ARMOR] = new PMSimplePolygonArea(rearArmor, unitDisplay, VTOL.LOC_REAR);
        areas[LABEL_ROTOR_ARMOR_1] = new PMSimplePolygonArea(rotorArmor1, unitDisplay, VTOL.LOC_ROTOR);
        areas[LABEL_ROTOR_ARMOR_2] = new PMSimplePolygonArea(rotorArmor2, unitDisplay, VTOL.LOC_ROTOR);
        areas[LABEL_CHIN_TU_ARMOR] = new PMSimplePolygonArea(chinTurretArmor, unitDisplay, VTOL.LOC_TURRET_2);
        areas[LABEL_FRONT_IS] = new PMSimplePolygonArea(frontIS, unitDisplay, VTOL.LOC_FRONT);
        areas[LABEL_RIGHT_IS_1] = new PMSimplePolygonArea(rightIS1, unitDisplay, VTOL.LOC_RIGHT);
        areas[LABEL_RIGHT_IS_2] = new PMSimplePolygonArea(rightIS2, unitDisplay, VTOL.LOC_RIGHT);
        areas[LABEL_LEFT_IS_1] = new PMSimplePolygonArea(leftIS1, unitDisplay, VTOL.LOC_LEFT);
        areas[LABEL_LEFT_IS_2] = new PMSimplePolygonArea(leftIS2, unitDisplay, VTOL.LOC_LEFT);
        areas[LABEL_REAR_IS] = new PMSimplePolygonArea(rearIS, unitDisplay, VTOL.LOC_REAR);
        areas[LABEL_ROTOR_IS] = new PMSimplePolygonArea(rotorIS, unitDisplay, VTOL.LOC_ROTOR);
        areas[LABEL_CHIN_TU_IS] = new PMSimplePolygonArea(chinTurretIS, unitDisplay, VTOL.LOC_TURRET);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);
        int mod = 1;
        
        // Labels for Front view
        labels[LABEL_CHIN_TU_ARMOR] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.chinTurretArmor"), fm, Color.black, 68, -37); //$NON-NLS-1$
        labels[LABEL_FRONT_ARMOR] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.FrontArmor"), fm, Color.black, 68, 20); //$NON-NLS-1$
        labels[LABEL_RIGHT_ARMOR_1] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.RS"), fm, Color.black, 104, 50); //$NON-NLS-1$
        labels[LABEL_RIGHT_ARMOR_2] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.RS"), fm, Color.black, 104, 100); //$NON-NLS-1$
        labels[LABEL_LEFT_ARMOR_1] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.LS"), fm, Color.black, 44, 50); //$NON-NLS-1$
        labels[LABEL_LEFT_ARMOR_2] = WidgetUtils.createLabel(
                Messages.getString("VTOLMapSet.LS"), fm, Color.black, 44, 100); //$NON-NLS-1$
        labels[LABEL_REAR_ARMOR] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearArmor1"), fm, Color.black, 76, 185); //$NON-NLS-1$
        labels[LABEL_REAR_ARMOR+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearArmor2"), fm, Color.black, 76, 195); //$NON-NLS-1$
        labels[LABEL_ROTOR_ARMOR_1+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RotorArmor"), fm, Color.black, 18, 82); //$NON-NLS-1$
        labels[LABEL_ROTOR_ARMOR_2+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RotorArmor"), fm, Color.black, 123, 82); //$NON-NLS-1$
        labels[LABEL_CHIN_TU_IS+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.chinTurretIS"), fm, Color.black, 75, -20); //$NON-NLS-1$
        labels[LABEL_FRONT_IS+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.FrontIS"), fm, Color.black, 68, 35); //$NON-NLS-1$
        labels[LABEL_RIGHT_IS_1+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS1"), fm, Color.black, 84, 48); //$NON-NLS-1$
        labels[LABEL_RIGHT_IS_2+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS2"), fm, Color.black, 84, 57); //$NON-NLS-1$
        mod += 2; // Increment modifier since we're continuing to shift, at +3 now
        labels[LABEL_RIGHT_IS_1+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS1"), fm, Color.black, 84, 100); //$NON-NLS-1$
        labels[LABEL_RIGHT_IS_2+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RIS2"), fm, Color.black, 84, 110); //$NON-NLS-1$
        labels[LABEL_LEFT_IS_1+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS1"), fm, Color.black, 68, 48); //$NON-NLS-1$
        labels[LABEL_LEFT_IS_2+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS2"), fm, Color.black, 68, 57); //$NON-NLS-1$
        mod += 2; // Increment modifier since we're continuing to shift, at +5 now
        labels[LABEL_LEFT_IS_1+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS1"), fm, Color.black, 68, 100); //$NON-NLS-1$
        labels[LABEL_LEFT_IS_2+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.LIS2"), fm, Color.black, 68, 110); //$NON-NLS-1$
        labels[LABEL_REAR_IS+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearIS1"), fm, Color.black, 76, 152); //$NON-NLS-1$
        mod++; // Increment modifier since we're continuing to shift, at +6 now
        labels[LABEL_REAR_IS+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RearIS2"), fm, Color.black, 76, 161); //$NON-NLS-1$
        labels[LABEL_ROTOR_IS+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.RotorIS"), fm, Color.black, 73, 82); //$NON-NLS-1$
        labels[LABEL_BAR_RATING+mod] = WidgetUtils.createLabel(Messages
                .getString("VTOLMapSet.BARRating"), fm, Color.white, 65, 198); //$NON-NLS-1$

        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[LABEL_CHIN_TU_ARMOR] = WidgetUtils.createValueLabel(92, -36, "", fm); //$NON-NLS-1$ Chin TU
        vLabels[LABEL_FRONT_ARMOR] = WidgetUtils.createValueLabel(101, 22, "", fm); //$NON-NLS-1$ Front
        vLabels[LABEL_RIGHT_ARMOR_1] = WidgetUtils.createValueLabel(105, 65, "", fm); //$NON-NLS-1$ RS
        vLabels[LABEL_RIGHT_ARMOR_2] = WidgetUtils.createValueLabel(105, 115, "", fm); //$NON-NLS-1$ RS
        vLabels[LABEL_LEFT_ARMOR_1] = WidgetUtils.createValueLabel(44, 65, "", fm); //$NON-NLS-1$ LS
        vLabels[LABEL_LEFT_ARMOR_2] = WidgetUtils.createValueLabel(44, 115, "", fm); //$NON-NLS-1$ LS
        vLabels[LABEL_REAR_ARMOR] = WidgetUtils.createValueLabel(76, 207, "", fm); //$NON-NLS-1$ Rear
        vLabels[LABEL_ROTOR_ARMOR_1] = WidgetUtils.createValueLabel(38, 83, "", fm); //$NON-NLS-1$ Rotor
        vLabels[LABEL_ROTOR_ARMOR_2] = WidgetUtils.createValueLabel(143, 83, "", fm); //$NON-NLS-1$ Rotor
        vLabels[LABEL_CHIN_TU_IS] = WidgetUtils.createValueLabel(75, -8, "", fm); //$NON-NLS-1$ Chin TU
        vLabels[LABEL_FRONT_IS] = WidgetUtils.createValueLabel(94, 37, "", fm); //$NON-NLS-1$ Front
        vLabels[LABEL_RIGHT_IS_1] = WidgetUtils.createValueLabel(84, 68, "", fm); //$NON-NLS-1$ RS
        vLabels[LABEL_RIGHT_IS_2] = WidgetUtils.createValueLabel(84, 122, "", fm); //$NON-NLS-1$ RS
        vLabels[LABEL_LEFT_IS_1] = WidgetUtils.createValueLabel(68, 68, "", fm); //$NON-NLS-1$ LS
        vLabels[LABEL_LEFT_IS_2] = WidgetUtils.createValueLabel(68, 122, "", fm); //$NON-NLS-1$ LS
        vLabels[LABEL_REAR_IS] = WidgetUtils.createValueLabel(76, 172, "", fm); //$NON-NLS-1$ Rear
        vLabels[LABEL_ROTOR_IS] = WidgetUtils.createValueLabel(98, 83, "", fm); //$NON-NLS-1$ Rotor
        vLabels[LABEL_BAR_RATING] = WidgetUtils.createValueLabel(100, 200, "", fm); //$NON-NLS-1$
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

    private void translateAreas() {
    }

}
