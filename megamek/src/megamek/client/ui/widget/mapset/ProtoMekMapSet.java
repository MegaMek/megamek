/*
 * Copyright (c) 2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk).
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;
import javax.swing.JComponent;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.picmap.PMAreasGroup;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PMValueLabel;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.ProtoMek;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent ProtoMek unit within the MekDisplay.ArmorPanel class.
 */
public class ProtoMekMapSet implements DisplayMapSet {

    private final UnitDisplayPanel unitDisplayPanel;

    // Boring list of labels.
    private final PMValueLabel[] sectionLabels = new PMValueLabel[ProtoMek.NUM_PROTOMEK_LOCATIONS];
    private final PMValueLabel[] armorLabels = new PMValueLabel[ProtoMek.NUM_PROTOMEK_LOCATIONS];
    private final PMValueLabel[] internalLabels = new PMValueLabel[ProtoMek.NUM_PROTOMEK_LOCATIONS];
    private final PMSimplePolygonArea[] areas = new PMSimplePolygonArea[ProtoMek.NUM_PROTOMEK_LOCATIONS];

    private final Polygon head = new Polygon(
          new int[] { 50, 50, 60, 80, 90, 90, 80, 60 },
          new int[] { 40, 20, 10, 10, 20, 40, 50, 50 }, 8);
    private final Polygon mainGun = new Polygon(
          new int[] { 20, 20, 50, 50 },
          new int[] { 30, 0, 0, 30 }, 4);
    private final Polygon leftArm = new Polygon(
          new int[] { 0, 0, 20, 30, 40, 30, 20, 20, 10 },
          new int[] { 100, 40, 30, 30, 60, 60, 70, 110, 110 }, 9);
    private final Polygon rightArm = new Polygon(
          new int[] { 120, 120, 110, 100, 110, 120, 140, 140, 130 },
          new int[] { 110, 70, 60, 60, 30, 30, 40, 100, 110, 110 },
          9);
    private final Polygon torso = new Polygon(
          new int[] { 40, 40, 30, 50, 50, 60, 80, 90, 90, 110, 100, 100 },
          new int[] { 130, 60, 30, 30, 40, 50, 50, 40, 30, 30, 60, 130 }, 12);
    private final Polygon legs = new Polygon(
          new int[] { 0, 0, 10, 30, 30, 40, 100, 110, 110, 130, 140, 140, 100, 90, 90, 80, 60, 50, 50, 40 },
          new int[] { 240, 230, 220, 220, 160, 130, 130, 160, 220, 220, 230, 240, 240, 230, 190, 170, 170, 190, 230,
                      240 }, 20);

    // Reference to Component (required for Image handling)
    private final JComponent jComponent;
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
    public ProtoMekMapSet(JComponent c, UnitDisplayPanel unitDisplayPanel) {
        this.unitDisplayPanel = unitDisplayPanel;
        jComponent = c;
        setAreas();
        setBackGround();
    }

    /**
     * Set the armor diagram on the map set.
     */
    private void setAreas() {
        areas[ProtoMek.LOC_HEAD] = new PMSimplePolygonArea(head, unitDisplayPanel, ProtoMek.LOC_HEAD);
        areas[ProtoMek.LOC_LEG] = new PMSimplePolygonArea(legs, unitDisplayPanel, ProtoMek.LOC_LEG);
        areas[ProtoMek.LOC_LARM] = new PMSimplePolygonArea(leftArm, unitDisplayPanel, ProtoMek.LOC_LARM);
        areas[ProtoMek.LOC_RARM] = new PMSimplePolygonArea(rightArm, unitDisplayPanel, ProtoMek.LOC_RARM);
        areas[ProtoMek.LOC_TORSO] = new PMSimplePolygonArea(torso, unitDisplayPanel, ProtoMek.LOC_TORSO);
        areas[ProtoMek.LOC_MAINGUN] = new PMSimplePolygonArea(mainGun, unitDisplayPanel, ProtoMek.LOC_MAINGUN);

        for (int i = 0; i <= 5; i++) {
            content.addArea(areas[i]);
        }

        FontMetrics fm = jComponent.getFontMetrics(FONT_VALUE);

        for (int i = 0; i < ProtoMek.NUM_PROTOMEK_LOCATIONS; i++) {
            sectionLabels[i] = new PMValueLabel(fm, Color.black);
            content.addArea(sectionLabels[i]);
            armorLabels[i] = new PMValueLabel(fm, Color.yellow.brighter());
            content.addArea(armorLabels[i]);
            internalLabels[i] = new PMValueLabel(fm, Color.red.brighter());
            content.addArea(internalLabels[i]);
        }
        sectionLabels[ProtoMek.LOC_HEAD].moveTo(70, 30);
        armorLabels[ProtoMek.LOC_HEAD].moveTo(60, 45);
        internalLabels[ProtoMek.LOC_HEAD].moveTo(80, 45);
        sectionLabels[ProtoMek.LOC_TORSO].moveTo(70, 70);
        armorLabels[ProtoMek.LOC_TORSO].moveTo(70, 85);
        internalLabels[ProtoMek.LOC_TORSO].moveTo(70, 100);
        sectionLabels[ProtoMek.LOC_RARM].moveTo(125, 55);
        armorLabels[ProtoMek.LOC_RARM].moveTo(125, 70);
        internalLabels[ProtoMek.LOC_RARM].moveTo(125, 85);
        sectionLabels[ProtoMek.LOC_LARM].moveTo(15, 55);
        armorLabels[ProtoMek.LOC_LARM].moveTo(15, 70);
        internalLabels[ProtoMek.LOC_LARM].moveTo(15, 85);
        sectionLabels[ProtoMek.LOC_LEG].moveTo(70, 150);
        armorLabels[ProtoMek.LOC_LEG].moveTo(60, 165);
        internalLabels[ProtoMek.LOC_LEG].moveTo(80, 165);
        sectionLabels[ProtoMek.LOC_MAINGUN].moveTo(35, 15);
        armorLabels[ProtoMek.LOC_MAINGUN].moveTo(25, 30);
        internalLabels[ProtoMek.LOC_MAINGUN].moveTo(45, 30);
    }

    @Override
    public PMAreasGroup getContentGroup() {
        return content;
    }

    @Override
    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    /**
     * Show the diagram for the given ProtoMek.
     *
     * @param entity the <code>Entity</code> to be displayed. This should be a <code>ProtoMek</code> unit.
     */
    @Override
    public void setEntity(Entity entity) {
        ProtoMek protoMek = (ProtoMek) entity;

        int loc = protoMek.locations();
        if (!protoMek.hasMainGun()) {
            armorLabels[ProtoMek.LOC_MAINGUN].setVisible(false);
            internalLabels[ProtoMek.LOC_MAINGUN].setVisible(false);
            sectionLabels[ProtoMek.LOC_MAINGUN].setVisible(false);
        } else {
            armorLabels[ProtoMek.LOC_MAINGUN].setVisible(true);
            internalLabels[ProtoMek.LOC_MAINGUN].setVisible(true);
            sectionLabels[ProtoMek.LOC_MAINGUN].setVisible(true);
        }
        for (int i = 0; i < loc; i++) {
            armorLabels[i].setValue(protoMek.getArmorString(i));
            internalLabels[i].setValue(protoMek.getInternalString(i));
            sectionLabels[i].setValue(protoMek.getLocationAbbr(i));
        }
    }

    /**
     * Sets the background on the map set.
     */
    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
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

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_RIGHT;
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
}
