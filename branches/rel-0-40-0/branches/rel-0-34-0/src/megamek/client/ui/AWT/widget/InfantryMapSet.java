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

package megamek.client.ui.AWT.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Entity;
import megamek.common.Infantry;

/**
 * Set of areas for PicMap to represent infantry platoon in MechDisplay
 */

public class InfantryMapSet implements DisplayMapSet {

    private static final String IMAGE_DIR = "data/images/widgets";

    // Picture to represent single trooper
    private Image infImage;
    // Reference to Component class required to handle images and fonts
    private Component comp;
    // Assuming that it will be no more that Infantry.INF_PLT_MAX_MEN men in
    // platoon
    private PMPicArea[] areas = new PMPicArea[Infantry.INF_PLT_MAX_MEN];
    // Main areas group that will be passing to PicMap
    private PMAreasGroup content = new PMAreasGroup();
    // Label
    private PMValueLabel label;
    // Set of Backgrownd drawers
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();

    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorMediumFontSize")); //$NON-NLS-1$

    public InfantryMapSet(Component c) {
        comp = c;
        setAreas();
        setBackGround();
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    public void setEntity(Entity e) {
        Infantry inf = (Infantry) e;
        int men = inf.getInternal(0);
        for (int i = 0; i < men; i++) {
            areas[i].setVisible(true);
        }
        for (int i = men; i < Infantry.INF_PLT_MAX_MEN; i++) {
            areas[i].setVisible(false);
        }

        label
                .setValue(Messages
                        .getString(
                                "InfantryMapSet.InfantryPlatoon", new Object[] { Integer.toString(men) })); //$NON-NLS-1$
    }

    private void setAreas() {
        int stepX = 30;
        int stepY = 42;
        infImage = comp.getToolkit().getImage(IMAGE_DIR + "/inf.gif"); //$NON-NLS-1$
        PMUtil.setImage(infImage, comp);
        for (int i = 0; i < Infantry.INF_PLT_MAX_MEN; i++) {
            int shiftX = (i % 5) * stepX;
            int shiftY = (i / 5) * stepY;
            areas[i] = new PMPicArea(infImage);
            areas[i].translate(shiftX, shiftY);
            content.addArea(areas[i]);
        }

        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);
        label = new PMValueLabel(fm, Color.white);
        label.setValue(Messages.getString(
                "InfantryMapSet.InfantryPlatoon", new Object[] { "00" })); //$NON-NLS-1$//$NON-NLS-2$
        Dimension d = label.getSize();
        content.translate(0, d.height + 5);
        label.moveTo(d.width / 2, d.height);
        content.addArea(label);
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
