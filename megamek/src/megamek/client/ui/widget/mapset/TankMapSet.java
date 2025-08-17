/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
import megamek.client.ui.widget.picmap.PMSimpleLabel;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PMValueLabel;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent Tank unit in MekDisplay.ArmorPanel class.
 */
public class TankMapSet implements DisplayMapSet {

    private final JComponent jComponent;
    private final PMSimplePolygonArea[] areas = new PMSimplePolygonArea[12];
    private final PMSimpleLabel[] labels = new PMSimpleLabel[13];
    private final PMValueLabel[] vLabels = new PMValueLabel[13];
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private final PMAreasGroup content = new PMAreasGroup();

    private final UnitDisplayPanel unitDisplayPanel;

    private static final int INT_STR_OFFSET = 6;
    // Polygons for all areas
    private final Polygon frontArmor = new Polygon(
          new int[] { 0, 19, 109, 128, 105, 92, 37, 23 },
          new int[] { 55, 27, 27, 55, 68, 49, 49, 68 }, 8);
    // front internal structure
    private final Polygon frontIS = new Polygon(
          new int[] { 67, 67, 105, 92, 37, 23, 61, 61 },
          new int[] { 40, 77, 39, 20, 20, 39, 77, 40 }, 8);
    // Left armor
    private final Polygon leftArmor = new Polygon(
          new int[] { 0, 0, 23, 23 },
          new int[] { 26, 214, 200, 39 }, 4);

    // Left internal structure
    private final Polygon leftIS = new Polygon(
          new int[] { 61, 23, 23, 23, 40, 33, 33, 46, 54, 58, 61 },
          new int[] { 77, 39, 200, 200, 183, 168, 120, 94, 94, 85, 85 }, 11);
    // Right armor
    private final Polygon rightArmor = new Polygon(
          new int[] { 128, 105, 105, 128 },
          new int[] { 26, 39, 200, 214 }, 4);

    // Right internal structure

    private final Polygon rightIS = new Polygon(
          new int[] { 83, 96, 96, 88, 105, 105, 105, 67, 67, 71, 75 },
          new int[] { 94, 120, 168, 183, 200, 200, 39, 77, 85, 85, 94 },
          11);

    // Rear armor
    private final Polygon rearArmor = new Polygon(
          new int[] { 128, 105, 92, 35, 23, 0, 11, 116 },
          new int[] { 214, 200, 220, 220, 200, 214, 239, 239 },
          8);
    // Rear internal structure
    private final Polygon rearIS = new Polygon(
          new int[] { 105, 88, 79, 50, 40, 23, 35, 92 },
          new int[] { 200, 183, 202, 202, 183, 200, 220, 220 }, 8);
    // Turret armor
    private final Polygon turretArmor = new Polygon(
          new int[] { 64, 74, 89, 89, 39, 39, 54, 64, 64, 64, 64, 50, 33, 33, 46, 54, 58, 61, 61, 67, 67, 71, 75, 83,
                      96, 96, 88, 79, 64 },
          new int[] { 187, 187, 160, 139, 139, 160, 187, 187, 202, 187, 202, 202, 168, 120, 94, 94, 85, 85, 40, 40, 85,
                      85, 94, 94, 120, 168, 183, 202, 202 },
          29);
    // Turret internal structure
    private final Polygon turretIS = new Polygon(
          new int[] { 39, 39, 54, 74, 89, 89 },
          new int[] { 139, 160, 187, 187, 160, 139 }, 6);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    public TankMapSet(JComponent c, UnitDisplayPanel unitDisplayPanel) {
        this.unitDisplayPanel = unitDisplayPanel;
        jComponent = c;
        setAreas();
        setLabels();
        setBackGround();
        translateAreas();
        setContent();
    }

    public void setRest() {
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
        Tank t = (Tank) e;
        int a;
        int a0;
        for (int i = 1; i < 6; i++) {
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a / (double) a0);
        }
        for (int i = 7; i < 12; i++) {
            a = t.getInternal(i - 6);
            a0 = t.getOInternal(i - 6);
            vLabels[i].setValue(t.getInternalString(i - 6));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a / (double) a0);
        }
        if (!t.hasPatchworkArmor() && t.hasBARArmor(1)) {
            vLabels[12].setValue(String.valueOf(t.getBARRating(1)));
        } else {
            labels[12].setVisible(false);
            vLabels[12].setVisible(false);
        }

    }

    private void setContent() {
        for (int i = 1; i < 6; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        for (int i = 1; i < 6; i++) {
            content.addArea(areas[i + INT_STR_OFFSET]);
            content.addArea(labels[i + INT_STR_OFFSET]);
            content.addArea(vLabels[i + INT_STR_OFFSET]);
        }
        content.addArea(labels[12]);
        content.addArea(vLabels[12]);
    }

    private void setAreas() {
        areas[Tank.LOC_FRONT] = new PMSimplePolygonArea(frontArmor, unitDisplayPanel, Tank.LOC_FRONT);
        areas[Tank.LOC_RIGHT] = new PMSimplePolygonArea(rightArmor, unitDisplayPanel, Tank.LOC_RIGHT);
        areas[Tank.LOC_LEFT] = new PMSimplePolygonArea(leftArmor, unitDisplayPanel, Tank.LOC_LEFT);
        areas[Tank.LOC_REAR] = new PMSimplePolygonArea(rearArmor, unitDisplayPanel, Tank.LOC_REAR);
        areas[Tank.LOC_TURRET] = new PMSimplePolygonArea(turretArmor, unitDisplayPanel, Tank.LOC_TURRET);
        areas[Tank.LOC_FRONT + INT_STR_OFFSET] = new PMSimplePolygonArea(frontIS, unitDisplayPanel, Tank.LOC_FRONT);
        areas[Tank.LOC_RIGHT + INT_STR_OFFSET] = new PMSimplePolygonArea(rightIS, unitDisplayPanel, Tank.LOC_FRONT);
        areas[Tank.LOC_LEFT + INT_STR_OFFSET] = new PMSimplePolygonArea(leftIS, unitDisplayPanel, Tank.LOC_LEFT);
        areas[Tank.LOC_REAR + INT_STR_OFFSET] = new PMSimplePolygonArea(rearIS, unitDisplayPanel, Tank.LOC_REAR);
        areas[Tank.LOC_TURRET + INT_STR_OFFSET] = new PMSimplePolygonArea(turretIS, unitDisplayPanel, Tank.LOC_TURRET);
    }

    private void setLabels() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[Tank.LOC_FRONT] = WidgetUtils.createLabel(Messages.getString("TankMapSet.FrontArmor"),
              fm, Color.black, 65, 35);
        labels[Tank.LOC_FRONT + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.FrontIS"),
              fm, Color.black, 63, 57);
        labels[Tank.LOC_LEFT] = WidgetUtils.createLabel(Messages.getString("TankMapSet.LS"),
              fm, Color.black, 19, 135);
        labels[Tank.LOC_LEFT + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.LIS"),
              fm, Color.black, 49, 106);
        labels[Tank.LOC_RIGHT] = WidgetUtils.createLabel(Messages.getString("TankMapSet.RS"),
              fm, Color.black, 124, 135);
        labels[Tank.LOC_RIGHT + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.RIS"),
              fm, Color.black, 95, 106);
        labels[Tank.LOC_REAR] = WidgetUtils.createLabel(Messages.getString("TankMapSet.RearArmor"),
              fm, Color.black, 65, 257);
        labels[Tank.LOC_REAR + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.RearIS"),
              fm, Color.black, 63, 239);
        labels[Tank.LOC_TURRET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.TurretArmor"),
              fm, Color.black, 73, 145);
        labels[Tank.LOC_TURRET + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.TurretIS"),
              fm, Color.black, 73, 173);
        labels[12] = WidgetUtils.createLabel(Messages.getString("TankMapSet.BARRating"),
              fm, Color.white, 65, 278);

        // Value labels for all parts of mek
        // front
        fm = jComponent.getFontMetrics(FONT_VALUE);
        vLabels[Tank.LOC_FRONT] = WidgetUtils.createValueLabel(101, 37, "", fm);
        vLabels[Tank.LOC_FRONT + INT_STR_OFFSET] = WidgetUtils.createValueLabel(91, 58, "", fm);
        vLabels[Tank.LOC_LEFT] = WidgetUtils.createValueLabel(20, 150, "", fm);
        vLabels[Tank.LOC_LEFT + INT_STR_OFFSET] = WidgetUtils.createValueLabel(44, 121, "", fm);
        vLabels[Tank.LOC_RIGHT] = WidgetUtils.createValueLabel(125, 150, "", fm);
        vLabels[Tank.LOC_RIGHT + INT_STR_OFFSET] = WidgetUtils.createValueLabel(102, 121, "", fm);
        vLabels[Tank.LOC_REAR] = WidgetUtils.createValueLabel(99, 258, "", fm);
        vLabels[Tank.LOC_REAR + INT_STR_OFFSET] = WidgetUtils.createValueLabel(91, 241, "", fm);
        vLabels[Tank.LOC_TURRET] = WidgetUtils.createValueLabel(73, 159, "", fm);
        vLabels[Tank.LOC_TURRET + INT_STR_OFFSET] = WidgetUtils.createValueLabel(73, 193, "", fm);
        vLabels[12] = WidgetUtils.createValueLabel(100, 280, "", fm);
    }

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

    private void translateAreas() {
        areas[Tank.LOC_FRONT].translate(8, 0);
        areas[Tank.LOC_FRONT + INT_STR_OFFSET].translate(8, 29);
        areas[Tank.LOC_LEFT].translate(8, 29);
        areas[Tank.LOC_LEFT + INT_STR_OFFSET].translate(8, 29);
        areas[Tank.LOC_RIGHT].translate(8, 29);
        areas[Tank.LOC_RIGHT + INT_STR_OFFSET].translate(8, 29);
        areas[Tank.LOC_REAR].translate(8, 29);
        areas[Tank.LOC_REAR + INT_STR_OFFSET].translate(8, 29);
        areas[Tank.LOC_TURRET].translate(8, 29);
        areas[Tank.LOC_TURRET + INT_STR_OFFSET].translate(8, 29);
    }

}
