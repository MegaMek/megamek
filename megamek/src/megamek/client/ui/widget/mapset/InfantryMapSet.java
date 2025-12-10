/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
import megamek.common.Configuration;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Set of areas for PicMap to represent infantry platoon in MekDisplay
 */
public class InfantryMapSet implements DisplayMapSet {

    // Reference to Component class required to handle images and fonts
    private final JComponent jComponent;
    // Assuming that it will be no more than 50 men in
    // platoon - ejected crews can be larger than platoons
    private final PMPicArea[] areas = new PMPicArea[EjectedCrew.EJ_CREW_MAX_MEN];
    // Main areas group that will be passing to PicMap
    private final PMAreasGroup content = new PMAreasGroup();
    // JLabel
    private PMValueLabel label;
    // JLabel
    private PMValueLabel armorLabel;
    // Set of Background drawers
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorMediumFontSize());

    public InfantryMapSet(JComponent c) {
        jComponent = c;
        setAreas();
        setBackGround();
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
        Infantry inf = (Infantry) e;
        int men;
        if (inf instanceof EjectedCrew) {
            men = Math.max(0, Math.min(inf.getInternal(0), EjectedCrew.EJ_CREW_MAX_MEN));
            for (int i = 0; i < men; i++) {
                areas[i].setVisible(true);
            }
            for (int i = men; i < EjectedCrew.EJ_CREW_MAX_MEN; i++) {
                areas[i].setVisible(false);
            }
        } else {
            men = Math.max(0, Math.min(inf.getInternal(0), Infantry.INF_PLT_MAX_MEN));
            for (int i = 0; i < men; i++) {
                areas[i].setVisible(true);
            }
            for (int i = men; i < EjectedCrew.EJ_CREW_MAX_MEN; i++) {
                areas[i].setVisible(false);
            }
        }

        label.setValue(Messages.getString("InfantryMapSet.InfantryPlatoon", Integer.toString(men)));
        armorLabel.setValue(Messages.getString("InfantryMapSet.Armor") + inf.getArmorDesc());
    }

    private void setAreas() {
        int stepX = 30;
        int stepY = 42;
        // Picture to represent single trooper
        Image infImage = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), "inf.gif").toString());
        PMUtil.setImage(infImage, jComponent);
        for (int i = 0; i < EjectedCrew.EJ_CREW_MAX_MEN; i++) {
            int shiftX = (i % 5) * stepX;
            int shiftY = (i / 5) * stepY;
            areas[i] = new PMPicArea(infImage);
            areas[i].translate(shiftX, shiftY);
            content.addArea(areas[i]);
        }

        FontMetrics fm = jComponent.getFontMetrics(FONT_VALUE);
        armorLabel = new PMValueLabel(fm, Color.white);
        armorLabel.setValue(Messages.getString("InfantryMapSet.Armor") + "XXXXXXXXXXXX");
        Dimension d = armorLabel.getSize();
        content.translate(0, d.height + 5);
        armorLabel.moveTo(0, d.height);
        content.addArea(armorLabel);

        label = new PMValueLabel(fm, Color.white);
        label.setValue(Messages.getString("InfantryMapSet.InfantryPlatoon", "00"));
        d = label.getSize();
        content.translate(0, d.height + 5);
        label.moveTo(0, d.height);
        content.addArea(label);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler
              .getUnitDisplaySkin();

        Image tile = jComponent.getToolkit()
              .getImage(
                    new MegaMekFile(Configuration.widgetsDir(), udSpec
                          .getBackgroundTile()).toString());
        PMUtil.setImage(tile, jComponent);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine())
                    .toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine())
                    .toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine())
                    .toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine())
                    .toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner())
                    .toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec
                    .getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(
                    new MegaMekFile(Configuration.widgetsDir(), udSpec
                          .getTopRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec
                    .getBottomRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

}
