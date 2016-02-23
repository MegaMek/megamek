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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.io.File;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.Infantry;

/**
 * Set of areas for PicMap to represent infantry platoon in MechDisplay
 */

public class InfantryMapSet implements DisplayMapSet {

    // Picture to represent single trooper
    private Image infImage;
    // Reference to Component class required to handle images and fonts
    private JComponent comp;
    // Assuming that it will be no more that Infantry.INF_PLT_MAX_MEN men in
    // platoon
    private PMPicArea[] areas = new PMPicArea[Infantry.INF_PLT_MAX_MEN];
    // Main areas group that will be passing to PicMap
    private PMAreasGroup content = new PMAreasGroup();
    // JLabel
    private PMValueLabel label;   
    // JLabel
    private PMValueLabel armorLabel;
    // Set of Backgrownd drawers
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();

    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorMediumFontSize")); //$NON-NLS-1$

    public InfantryMapSet(JComponent c) {
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
        int men = Math.min(inf.getInternal(0),Infantry.INF_PLT_MAX_MEN);
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
        armorLabel.setValue(Messages.getString("InfantryMapSet.Armor") + inf.getArmorDesc());
    }

    private void setAreas() {
        int stepX = 30;
        int stepY = 42;
        infImage = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), "inf.gif").toString()); //$NON-NLS-1$
        PMUtil.setImage(infImage, comp);
        for (int i = 0; i < Infantry.INF_PLT_MAX_MEN; i++) {
            int shiftX = (i % 5) * stepX;
            int shiftY = (i / 5) * stepY;
            areas[i] = new PMPicArea(infImage);
            areas[i].translate(shiftX, shiftY);
            content.addArea(areas[i]);
        }

        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);
        armorLabel = new PMValueLabel(fm, Color.white);
        armorLabel.setValue(Messages.getString(
                "InfantryMapSet.Armor") + "XXXXXXXXXXXX"); //$NON-NLS-1$//$NON-NLS-2$
        Dimension d = armorLabel.getSize();
        content.translate(0, d.height + 5);
        armorLabel.moveTo(0, d.height);
        content.addArea(armorLabel);
        
        label = new PMValueLabel(fm, Color.white);
        label.setValue(Messages.getString(
                "InfantryMapSet.InfantryPlatoon", new Object[] { "00" })); //$NON-NLS-1$//$NON-NLS-2$
        d = label.getSize();
        content.translate(0, d.height + 5);
        label.moveTo(0, d.height);
        content.addArea(label);
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
