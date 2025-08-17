/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk).
 * Copyright (C) 2003-2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.widget.mapset;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;
import javax.swing.JComponent;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.picmap.PMAreasGroup;
import megamek.client.ui.widget.picmap.PMPicArea;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PMValueLabel;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.Configuration;
import megamek.common.units.Entity;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent Battle Armor unit in MekDisplay.ArmorPanel class.
 */
public class BattleArmorMapSet implements DisplayMapSet {
    // Images that shows how much armor + 1 internal damage left.
    private final Image[] armorImage = new Image[BattleArmor.BA_MAX_MEN];
    // Reference to Component (required for Image handling)
    private final JComponent jComponent;
    // Set of areas to show BA figures
    private final PMPicArea[] unitAreas = new PMPicArea[BattleArmor.BA_MAX_MEN];
    // Set of areas to show BA armor left
    private final PMPicArea[] armorAreas = new PMPicArea[BattleArmor.BA_MAX_MEN];
    // Set of labels to show BA armor left
    private final PMValueLabel[] armorLabels = new PMValueLabel[BattleArmor.BA_MAX_MEN];
    // Content group which will be sent to PicMap component
    private final PMAreasGroup content = new PMAreasGroup();
    // Set of Background drawers which will be sent to PicMap component
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    /**
     * This constructor can only be called from the addNotify method
     */
    public BattleArmorMapSet(JComponent c) {
        jComponent = c;
        setAreas();
        setBackGround();
    }

    private void setAreas() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_VALUE);

        // Picture with figure
        Image battleArmorImage = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), "battle_armor.gif").toString());
        PMUtil.setImage(battleArmorImage, jComponent);
        for (int i = 0; i < BattleArmor.BA_MAX_MEN; i++) {
            int stepY = 53;
            int shiftY = i * stepY;
            unitAreas[i] = new PMPicArea(battleArmorImage);
            unitAreas[i].translate(0, shiftY);
            content.addArea(unitAreas[i]);

            armorImage[i] = jComponent.createImage(105, 12);
            armorAreas[i] = new PMPicArea(armorImage[i]);
            armorAreas[i].translate(45, shiftY + 12);
            content.addArea(armorAreas[i]);

            armorLabels[i] = new PMValueLabel(fm, Color.red.brighter());
            armorLabels[i].moveTo(160, shiftY + 24);
            content.addArea(armorLabels[i]);
        }
    }

    @Override
    public PMAreasGroup getContentGroup() {
        return content;
    }

    @Override
    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    @Override
    public void setEntity(Entity e) {
        BattleArmor ba = (BattleArmor) e;
        int armor;
        int internal;
        int men = ba.getTroopers();

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

        for (int i = 0; i < men; i++) {
            armor = (ba.getArmor(i + 1, false) < 0) ? 0 : ba.getArmor(i + 1, false);
            internal = (ba.getInternal(i + 1) < 0) ? 0 : ba.getInternal(i + 1);
            if ((armor + internal) == 0) {
                armorAreas[i].setVisible(false);
                armorLabels[i].setValue(Messages.getString("BattleArmorMapSet.Killed"));
            } else {
                drawArmorImage(armorImage[i], armor + internal);
                armorLabels[i].setValue(Integer.toString(armor + internal));
                armorAreas[i].setVisible(true);
            }
        }
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = jComponent.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
              udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, jComponent);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

    // Redraws armor images
    private void drawArmorImage(Image im, int a) {
        int x;
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
