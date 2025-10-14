/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk).
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;
import javax.swing.JComponent;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.WidgetUtils;
import megamek.client.ui.widget.picmap.PMAreasGroup;
import megamek.client.ui.widget.picmap.PMPicPolygonalArea;
import megamek.client.ui.widget.picmap.PMSimpleLabel;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PMValueLabel;
import megamek.common.Configuration;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Very cumbersome class that handles set of polygonal areas and labels for PicMap component to represent single mek
 * unit in MekDisplay
 *
 * @deprecated Don't use anything, this class is deprecated and will be removed in the future. There is no substitute
 *       display class for the ArmlessMekMapSet as this type of mek is no longer supported.
 */
@Deprecated(since = "0.50.06", forRemoval = true)
public class ArmlessMekMapSet implements DisplayMapSet {

    // Because of keeping all areas of single type in one array
    // some index offset values required
    private static final int REAR_AREA_OFFSET = 7;
    private static final int INT_STRUCTURE_OFFSET = 11;

    private final UnitDisplayPanel unitDisplayPanel;

    // Array of polygonal areas - parts of mek body.
    private final PMSimplePolygonArea[] areas = new PMSimplePolygonArea[19];
    // Array of fixed labels - short names of body parts
    private final PMSimpleLabel[] labels = new PMSimpleLabel[19];
    // Array of value labels to show armor and IS values
    private final PMValueLabel[] vLabels = new PMValueLabel[20];
    // Heat control area
    private PMPicPolygonalArea heatHotArea;
    // Set of Background Drawers
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    // Main areas group that keeps everything in itself and is passed to PicMap
    // component
    private final PMAreasGroup content = new PMAreasGroup();
    // Reference to Component class (need to manage images and fonts)
    private final JComponent jComponent;

    // Points for build hot areas (maybe too heavy, think of to load from external
    // file)
    // Mek armor - Front
    // Head
    private final Polygon head = new Polygon(new int[] { 53, 71, 81, 83, 83, 62, 41, 41, 43 },
          new int[] { 32, 32, 22, 22, 8, 0, 8, 22, 22 },
          9);
    // Central Torso
    private final Polygon centralTorso = new Polygon(new int[] { 48, 54, 70, 76, 76, 48 },
          new int[] { 45, 85, 85, 45, 36, 36 },
          6);
    // left Torso
    private final Polygon leftTorso = new Polygon(new int[] { 54, 48, 48, 62, 62, 53, 43, 41, 41, 31, 29, 27, 25, 26,
                                                              47 },
          new int[] { 82, 45, 36, 36, 32, 32, 22, 22, 20, 25, 27, 33, 37, 47, 82 },
          15);
    // right Torso
    private final Polygon rightTorso = new Polygon(new int[] { 70, 76, 76, 62, 62, 71, 81, 83, 83, 93, 95, 97, 99, 98,
                                                               77 },
          new int[] { 82, 45, 36, 36, 32, 32, 22, 22, 20, 25, 27, 33, 37, 47, 82 },
          15);
    // Left Leg

    private final Polygon leftLeg = new Polygon(new int[] { 0, 7, 37, 47, 54, 54, 61, 34, 40, 40, 34, 33, 7, 6, 0 },
          new int[] { 104, 104, 65, 82, 82, 85, 85, 102, 104, 121, 123, 129, 129, 122, 122 },
          15);
    // right Leg
    private final Polygon rightLeg = new Polygon(new int[] { 125, 118, 88, 77, 70, 70, 64, 91, 85, 85, 91, 92, 118, 119,
                                                             125 },
          new int[] { 104, 104, 63, 82, 82, 85, 85, 102, 104, 121, 123, 129, 129, 122, 122 },
          15);

    // Mek Armor - Rear
    // Left Torso
    private final Polygon rearLeftTorso = new Polygon(new int[] { 142, 142, 148, 139, 123, 123, 142 },
          new int[] { 14, 43, 76, 76, 44, 17, 14 },
          7);
    // Central Torso
    private final Polygon rearCentralTorso = new Polygon(new int[] { 142, 148, 162, 168, 168, 142 },
          new int[] { 44, 76, 76, 44, 14, 14 },
          6);
    // Right Torso
    private final Polygon rearRightTorso = new Polygon(new int[] { 168, 168, 162, 171, 187, 187, 168 },
          new int[] { 14, 43, 76, 76, 44, 17, 14 },
          7);

    // Internal Structure
    // Head
    private final Polygon intStHead = new Polygon(new int[] { 78, 48, 48, 78 },
          new int[] { 149, 149, 127, 127 }, 4);
    // Central Torso
    private final Polygon inStCentralTorso = new Polygon(
          new int[] { 75, 75, 51, 51 }, new int[] { 203, 149, 149, 203 }, 4);
    // Left Torso
    private final Polygon inStLeftTorso = new Polygon(new int[] { 32, 32, 51, 51 },
          new int[] { 188, 160, 160, 193 }, 4);
    // Right Torso
    private final Polygon inStRightTorso = new Polygon(new int[] { 94, 94, 75, 75 },
          new int[] { 188, 160, 160, 193 }, 4);
    // Left Leg
    private final Polygon inStLeftLeg = new Polygon(new int[] { 51, 51, 44, 44, 47, 47, 20, 20, 41, 41, 44, 44 },
          new int[] { 195, 199, 199, 206, 206,
                      230, 230, 206, 206, 192, 192, 195 },
          12);
    // right Leg
    private final Polygon inStRightLeg = new Polygon(new int[] { 75, 75, 82, 82, 79, 79, 106, 106, 85, 85, 82, 82 },
          new int[] { 195, 199, 199, 206, 206, 230, 230, 206, 206, 192, 192, 195 },
          12);
    // Heat control
    private final Polygon heatControl = new Polygon(new int[] { 149, 159, 159, 149 },
          new int[] { 100, 100, 220, 220 }, 4);

    private Image heatImage;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    public ArmlessMekMapSet(JComponent jComponent, UnitDisplayPanel unitDisplayPanel) {
        this.unitDisplayPanel = unitDisplayPanel;
        this.jComponent = jComponent;
        setAreas();
        setLabels();
        setGroups();
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
        Mek m = (Mek) e;
        boolean mtHeat = e.getGame() != null
              && e.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT);
        int a;
        int a0;
        for (int i = 0; i < m.locations(); i++) {
            if (i == Mek.LOC_LEFT_ARM || i == Mek.LOC_RIGHT_ARM) {
                continue;
            }
            a = m.getArmor(i);
            a0 = m.getOArmor(i);
            vLabels[i].setValue(m.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                  / (double) a0);
            if (m.hasRearArmor(i)) {
                a = m.getArmor(i, true);
                a0 = m.getOArmor(i, true);
                vLabels[i + REAR_AREA_OFFSET].setValue(m
                      .getArmorString(i, true));
                WidgetUtils.setAreaColor(areas[i + REAR_AREA_OFFSET], vLabels[i
                      + REAR_AREA_OFFSET], (double) a / (double) a0);
            }
            a = m.getInternal(i);
            a0 = m.getOInternal(i);
            vLabels[i + INT_STRUCTURE_OFFSET].setValue(m.getInternalString(i));
            WidgetUtils.setAreaColor(areas[i + INT_STRUCTURE_OFFSET], vLabels[i
                  + INT_STRUCTURE_OFFSET], (double) a / (double) a0);
        }

        // heat
        vLabels[19].setValue(Integer.toString(m.heat));
        drawHeatControl(m.heat, mtHeat);
    }

    private void setAreas() {
        areas[Mek.LOC_HEAD] = new PMSimplePolygonArea(head, unitDisplayPanel, Mek.LOC_HEAD);
        areas[Mek.LOC_CENTER_TORSO] = new PMSimplePolygonArea(centralTorso, unitDisplayPanel, Mek.LOC_CENTER_TORSO);
        areas[Mek.LOC_RIGHT_TORSO] = new PMSimplePolygonArea(rightTorso, unitDisplayPanel, Mek.LOC_RIGHT_TORSO);
        areas[Mek.LOC_LEFT_TORSO] = new PMSimplePolygonArea(leftTorso, unitDisplayPanel, Mek.LOC_LEFT_TORSO);
        areas[Mek.LOC_RIGHT_LEG] = new PMSimplePolygonArea(rightLeg, unitDisplayPanel, Mek.LOC_RIGHT_LEG);
        areas[Mek.LOC_LEFT_LEG] = new PMSimplePolygonArea(leftLeg, unitDisplayPanel, Mek.LOC_LEFT_LEG);
        areas[REAR_AREA_OFFSET + Mek.LOC_CENTER_TORSO] = new PMSimplePolygonArea(
              rearCentralTorso, unitDisplayPanel, Mek.LOC_CENTER_TORSO);
        areas[REAR_AREA_OFFSET + Mek.LOC_RIGHT_TORSO] = new PMSimplePolygonArea(
              rearRightTorso, unitDisplayPanel, Mek.LOC_RIGHT_TORSO);
        areas[REAR_AREA_OFFSET + Mek.LOC_LEFT_TORSO] = new PMSimplePolygonArea(
              rearLeftTorso, unitDisplayPanel, Mek.LOC_LEFT_TORSO);
        areas[INT_STRUCTURE_OFFSET + Mek.LOC_HEAD] = new PMSimplePolygonArea(
              intStHead, unitDisplayPanel, Mek.LOC_HEAD);
        areas[INT_STRUCTURE_OFFSET + Mek.LOC_CENTER_TORSO] = new PMSimplePolygonArea(
              inStCentralTorso, unitDisplayPanel, Mek.LOC_CENTER_TORSO);
        areas[INT_STRUCTURE_OFFSET + Mek.LOC_RIGHT_TORSO] = new PMSimplePolygonArea(
              inStRightTorso, unitDisplayPanel, Mek.LOC_RIGHT_TORSO);
        areas[INT_STRUCTURE_OFFSET + Mek.LOC_LEFT_TORSO] = new PMSimplePolygonArea(
              inStLeftTorso, unitDisplayPanel, Mek.LOC_LEFT_TORSO);
        areas[INT_STRUCTURE_OFFSET + Mek.LOC_RIGHT_LEG] = new PMSimplePolygonArea(
              inStRightLeg, unitDisplayPanel, Mek.LOC_RIGHT_LEG);
        areas[INT_STRUCTURE_OFFSET + Mek.LOC_LEFT_LEG] = new PMSimplePolygonArea(
              inStLeftLeg, unitDisplayPanel, Mek.LOC_LEFT_LEG);
        heatImage = jComponent.createImage(10, 120);
        drawHeatControl(0);
        heatHotArea = new PMPicPolygonalArea(heatControl, heatImage);
    }

    private void setLabels() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[Mek.LOC_HEAD] = WidgetUtils.createLabel(Messages.getString("MekMapSet.l_H"),
              fm, Color.black, 62, 6);
        labels[Mek.LOC_LEFT_TORSO] = WidgetUtils.createLabel(Messages.getString("MekMapSet.l_LT"),
              fm, Color.black, 41, 52);
        labels[Mek.LOC_CENTER_TORSO] = WidgetUtils.createLabel(Messages.getString("MekMapSet.l_CT"),
              fm, Color.black, 62, 42);
        labels[Mek.LOC_RIGHT_TORSO] = WidgetUtils.createLabel(Messages.getString("MekMapSet.l_RT"),
              fm, Color.black, 84, 52);
        labels[Mek.LOC_LEFT_LEG] = WidgetUtils.createLabel(Messages.getString("MekMapSet.l_LL"),
              fm, Color.black, 28, 92);
        labels[Mek.LOC_RIGHT_LEG] = WidgetUtils.createLabel(Messages.getString("MekMapSet.l_RL"),
              fm, Color.black, 98, 92);
        // Labels for Back view
        labels[REAR_AREA_OFFSET + Mek.LOC_LEFT_TORSO] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_LT"), fm, Color.black, 133, 39);
        labels[REAR_AREA_OFFSET + Mek.LOC_CENTER_TORSO] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_CT"), fm, Color.black, 156, 25);
        labels[REAR_AREA_OFFSET + Mek.LOC_RIGHT_TORSO] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_RT"), fm, Color.black, 178, 39);
        // Labels for Internal Structure
        labels[INT_STRUCTURE_OFFSET + Mek.LOC_HEAD] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_H"), fm, Color.black, 63, 130);
        labels[INT_STRUCTURE_OFFSET + Mek.LOC_LEFT_TORSO] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_LT"), fm, Color.black, 42, 166);
        labels[INT_STRUCTURE_OFFSET + Mek.LOC_CENTER_TORSO] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.L_CT"), fm, Color.black, 63, 168);
        labels[INT_STRUCTURE_OFFSET + Mek.LOC_RIGHT_TORSO] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_RT"), fm, Color.black, 85, 166);
        labels[INT_STRUCTURE_OFFSET + Mek.LOC_LEFT_LEG] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_LL"), fm, Color.black, 33, 210);
        labels[INT_STRUCTURE_OFFSET + Mek.LOC_RIGHT_LEG] = WidgetUtils.createLabel(
              Messages.getString("MekMapSet.l_RL"), fm, Color.black, 93, 210);

        // Value labels for all parts of mek
        // front
        fm = jComponent.getFontMetrics(FONT_VALUE);
        vLabels[Mek.LOC_HEAD] = WidgetUtils.createValueLabel(62, 22, "", fm);
        vLabels[Mek.LOC_LEFT_TORSO] = WidgetUtils.createValueLabel(38, 44, "", fm);
        vLabels[Mek.LOC_CENTER_TORSO] = WidgetUtils.createValueLabel(62, 57, "", fm);
        vLabels[Mek.LOC_RIGHT_TORSO] = WidgetUtils.createValueLabel(86, 44, "", fm);
        vLabels[Mek.LOC_LEFT_LEG] = WidgetUtils.createValueLabel(23, 113, "", fm);
        vLabels[Mek.LOC_RIGHT_LEG] = WidgetUtils.createValueLabel(102, 113, "", fm);

        // back
        vLabels[REAR_AREA_OFFSET + Mek.LOC_LEFT_TORSO] = WidgetUtils.createValueLabel(132, 28, "", fm);
        vLabels[REAR_AREA_OFFSET + Mek.LOC_CENTER_TORSO] = WidgetUtils.createValueLabel(156, 39, "", fm);
        vLabels[REAR_AREA_OFFSET + Mek.LOC_RIGHT_TORSO] = WidgetUtils.createValueLabel(177, 28, "", fm);

        // Internal structure
        vLabels[INT_STRUCTURE_OFFSET + Mek.LOC_HEAD] = WidgetUtils.createValueLabel(63, 142, "", fm);
        vLabels[INT_STRUCTURE_OFFSET + Mek.LOC_LEFT_TORSO] = WidgetUtils.createValueLabel(42, 180, "", fm);
        vLabels[INT_STRUCTURE_OFFSET + Mek.LOC_CENTER_TORSO] = WidgetUtils.createValueLabel(63, 182, "", fm);
        vLabels[INT_STRUCTURE_OFFSET + Mek.LOC_RIGHT_TORSO] = WidgetUtils.createValueLabel(85, 180, "", fm);
        vLabels[INT_STRUCTURE_OFFSET + Mek.LOC_LEFT_LEG] = WidgetUtils.createValueLabel(33, 223, "", fm);
        vLabels[INT_STRUCTURE_OFFSET + Mek.LOC_RIGHT_LEG] = WidgetUtils.createValueLabel(92, 223, "", fm);
        // heat
        vLabels[19] = WidgetUtils.createValueLabel(155, 90, "", fm);
    }

    private void setGroups() {
        // Have to remove it later
        PMAreasGroup frontArmor = new PMAreasGroup();
        PMAreasGroup rearArmor = new PMAreasGroup();
        PMAreasGroup intStructure = new PMAreasGroup();
        PMAreasGroup heat = new PMAreasGroup();

        for (int i = 0; i < 8; i++) {
            if (i == Mek.LOC_LEFT_ARM || i == Mek.LOC_RIGHT_ARM) {
                continue;
            }
            frontArmor.addArea(areas[i]);
            frontArmor.addArea(labels[i]);
            frontArmor.addArea(vLabels[i]);
        }

        for (int i = 0; i < 3; i++) {
            rearArmor.addArea(areas[8 + i]);
            rearArmor.addArea(labels[8 + i]);
            rearArmor.addArea(vLabels[8 + i]);
        }

        for (int i = 0; i < 8; i++) {
            if (i == Mek.LOC_LEFT_ARM || i == Mek.LOC_RIGHT_ARM) {
                continue;
            }
            intStructure.addArea(areas[11 + i]);
            intStructure.addArea(labels[11 + i]);
            intStructure.addArea(vLabels[11 + i]);
        }

        heat.addArea(heatHotArea);
        heat.addArea(vLabels[19]);

        frontArmor.translate(7, 18);
        rearArmor.translate(19, 20);
        intStructure.translate(6, 42);
        heat.translate(20, 52);

        // This have to be left
        for (int i = 0; i < 19; i++) {
            if (i == Mek.LOC_LEFT_ARM || i == Mek.LOC_RIGHT_ARM) {
                continue;
            }
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }

        content.addArea(heatHotArea);
        content.addArea(vLabels[19]);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, jComponent);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getMekOutline()).toString());
        PMUtil.setImage(tile, jComponent);
        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_CENTER
              | BackGroundDrawer.H_ALIGN_CENTER;
        BackGroundDrawer bgd = new BackGroundDrawer(tile, b);
        bgDrawers.addElement(bgd);
    }

    private void drawHeatControl(int t) {
        drawHeatControl(t, false);
    }

    private void drawHeatControl(int t, boolean mtHeat) {
        int y;
        int maxHeat, steps;
        if (mtHeat) {
            maxHeat = 50;
            steps = 2;
        } else {
            maxHeat = 30;
            steps = 4;
        }
        Graphics g = heatImage.getGraphics();
        for (int i = 0; i < maxHeat; i++) {
            y = 120 - (i + 1) * steps;
            if (i < t) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.lightGray);
            }
            g.fillRect(0, y, 10, steps);
            g.setColor(Color.black);
            g.drawRect(0, y, 10, steps);
        }
    }

}
