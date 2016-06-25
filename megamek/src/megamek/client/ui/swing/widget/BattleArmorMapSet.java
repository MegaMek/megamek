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
import java.io.File;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.BattleArmor;
import megamek.common.Configuration;
import megamek.common.Entity;

/**
 * Class which keeps set of all areas required to represent Battle Armor unit in
 * MechDsiplay.ArmorPanel class.
 */

public class BattleArmorMapSet implements DisplayMapSet {
    
    // Picture with figure
    private Image battleArmorImage;
    // Images that shows how much armor + 1 internal damage left.
    private Image[] armorImage = new Image[BattleArmor.BA_MAX_MEN];
    // Reference to Component (required for Image handling)
    private JComponent comp;
    // Set of areas to show BA figures
    private PMPicArea[] unitAreas = new PMPicArea[BattleArmor.BA_MAX_MEN];
    // Set of areas to show BA armor left
    private PMPicArea[] armorAreas = new PMPicArea[BattleArmor.BA_MAX_MEN];
    // Set of labels to show BA armor left
    private PMValueLabel[] armorLabels = new PMValueLabel[BattleArmor.BA_MAX_MEN];
    // Content group which will be sent to PicMap component
    private PMAreasGroup content = new PMAreasGroup();
    // Set of Backgrpund drawers which will be sent to PicMap component
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();

    private int stepY = 53;

    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, //$NON-NLS-1$
            GUIPreferences.getInstance().getInt(
                    "AdvancedMechDisplayArmorLargeFontSize"));

    /**
     * This constructor have to be called anly from addNotify() method
     */
    public BattleArmorMapSet(JComponent c) {
        comp = c;
        setAreas();
        setBackGround();
    }

    private void setAreas() {
        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);

        battleArmorImage = comp.getToolkit().getImage(
                new File(Configuration.widgetsDir(), "battle_armor.gif").toString()); //$NON-NLS-1$
        PMUtil.setImage(battleArmorImage, comp);
        for (int i = 0; i < BattleArmor.BA_MAX_MEN; i++) {
            int shiftY = i * stepY;
            unitAreas[i] = new PMPicArea(battleArmorImage);
            unitAreas[i].translate(0, shiftY);
            content.addArea(unitAreas[i]);

            armorImage[i] = comp.createImage(105, 12);
            armorAreas[i] = new PMPicArea(armorImage[i]);
            armorAreas[i].translate(45, shiftY + 12);
            content.addArea(armorAreas[i]);

            armorLabels[i] = new PMValueLabel(fm, Color.red.brighter());
            armorLabels[i].moveTo(160, shiftY + 24);
            content.addArea(armorLabels[i]);
        }
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    public void setEntity(Entity e) {
        BattleArmor ba = (BattleArmor) e;
        int armor = 0;
        int internal = 0;
        // int men = 5;
        int men = Math.round(ba.getTroopers());

        for (int x = 0; x < men; x++) {
            armorAreas[x].setVisible(true);
            armorLabels[x].setVisible(true);
            unitAreas[x].setVisible(true);
        }
        for (int x = men; x < BattleArmor.BA_MAX_MEN; x++) {
            armorAreas[x].setVisible(false);
            armorLabels[x].setVisible(false);
            unitAreas[x].setVisible(false);
        }
        /*
         * if (ba.isClan()){ men = 5; armorAreas[4].setVisible(true);
         * armorLabels[4].setVisible(true); unitAreas[4].setVisible(true); }
         * else{ men = 4; armorAreas[4].setVisible(false);
         * armorLabels[4].setVisible(false); unitAreas[4].setVisible(false); }
         */
        for (int i = 0; i < men; i++) {
            armor = (ba.getArmor(i + 1, false) < 0) ? 0 : ba.getArmor(i + 1,
                    false);
            internal = (ba.getInternal(i + 1) < 0) ? 0 : ba.getInternal(i + 1);
            if ((armor + internal) == 0) {
                armorAreas[i].setVisible(false);
                armorLabels[i].setValue(Messages
                        .getString("BattleArmorMapSet.Killed")); //$NON-NLS-1$
            } else {
                drawArmorImage(armorImage[i], armor + internal);
                armorLabels[i].setValue(Integer.toString(armor + internal));
                armorAreas[i].setVisible(true);
            }
        }
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

    // Redraws armor images
    private void drawArmorImage(Image im, int a) {
        int x = 0;
        int w = im.getWidth(null);
        int h = im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < a; i++) {
            x = i * 7;
            g.setColor(Color.green.darker());
            g.fillRect(x, 0, 5, 12);
        }
    }

}
