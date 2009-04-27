/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2005 Mike Gratton <mike@vee.net>
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
import megamek.common.GunEmplacement;

/**
 * Class which keeps set of all areas required to represent GunEmplacement unit
 * in MechDsiplay.ArmorPanel class.
 */
public class GunEmplacementMapSet implements DisplayMapSet {

    private Component comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[5];
    private PMSimpleLabel[] labels = new PMSimpleLabel[5];
    private PMValueLabel[] vLabels = new PMValueLabel[5];
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    private static final String IMAGE_DIR = "data/images/widgets";
    private static final Polygon BUILDING_CF = new Polygon(new int[] { 0, 0,
            40, 40 }, new int[] { 0, 160, 160, 0 }, 4);
    private static final Polygon TURRET_ARMOR = new Polygon(new int[] { 0, 0,
            80, 80, 90, 150, 160, 160, 150, 110, 80, 80 }, new int[] { 20, 25,
            25, 30, 35, 35, 30, 15, 0, 0, 15, 20 }, 12);

    private static final Font FONT_LABEL = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    public GunEmplacementMapSet(Component c) {
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
        GunEmplacement ge = (GunEmplacement) e;
        int loc = GunEmplacement.LOC_BUILDING;
        vLabels[loc].setValue(ge.getArmorString(loc));
        WidgetUtils.setAreaColor(areas[loc], vLabels[loc], (double) ge
                .getArmor(loc)
                / (double) ge.getOArmor(loc));
        loc = GunEmplacement.LOC_TURRET;
        vLabels[loc].setValue(ge.getArmorString(loc));
        WidgetUtils.setAreaColor(areas[loc], vLabels[loc],
                ge.hasTurret() ? (double) ge.getArmor(loc)
                        / (double) ge.getOArmor(loc) : 0);

    }

    private void setContent() {
        content.addArea(areas[GunEmplacement.LOC_BUILDING]);
        content.addArea(labels[GunEmplacement.LOC_BUILDING]);
        content.addArea(vLabels[GunEmplacement.LOC_BUILDING]);
        content.addArea(areas[GunEmplacement.LOC_TURRET]);
        content.addArea(labels[GunEmplacement.LOC_TURRET]);
        content.addArea(vLabels[GunEmplacement.LOC_TURRET]);
    }

    private void setAreas() {
        areas[GunEmplacement.LOC_BUILDING] = new PMSimplePolygonArea(
                BUILDING_CF);
        areas[GunEmplacement.LOC_TURRET] = new PMSimplePolygonArea(TURRET_ARMOR);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[GunEmplacement.LOC_BUILDING] = WidgetUtils
                .createLabel(
                        Messages
                                .getString("GunEmplacementMapSet.ConstructionFactor"), fm, Color.white, 90, 200); //$NON-NLS-1$
        labels[GunEmplacement.LOC_TURRET] = WidgetUtils
                .createLabel(
                        Messages.getString("GunEmplacementMapSet.TurretArmor"), fm, Color.white, 90, -25); //$NON-NLS-1$

        // Value labels for all parts of mek
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[GunEmplacement.LOC_BUILDING] = WidgetUtils.createValueLabel(90,
                90, "", fm); //$NON-NLS-1$
        vLabels[GunEmplacement.LOC_TURRET] = WidgetUtils.createValueLabel(90,
                10, "", fm); //$NON-NLS-1$
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
        areas[GunEmplacement.LOC_BUILDING].translate(70, 25);
        areas[GunEmplacement.LOC_TURRET].translate(-30, -10);
    }
}
