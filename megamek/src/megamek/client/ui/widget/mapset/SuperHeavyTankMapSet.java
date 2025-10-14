/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.Entity;
import megamek.common.units.SuperHeavyTank;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent Tank unit in MekDisplay.ArmorPanel class.
 */
public class SuperHeavyTankMapSet implements DisplayMapSet {

    private final UnitDisplayPanel unitDisplayPanel;

    private final JComponent jComponent;
    private final PMSimplePolygonArea[] areas = new PMSimplePolygonArea[15];
    private final PMSimpleLabel[] labels = new PMSimpleLabel[16];
    private final PMValueLabel[] vLabels = new PMValueLabel[16];
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private final PMAreasGroup content = new PMAreasGroup();

    // Polygons for all areas

    // front armor
    private final Polygon frontArmor = new Polygon(
          new int[] { 0, 23, 157, 180, 157, 118, 62, 23 },
          new int[] { 30, 7, 7, 30, 41, 25, 25, 41 }, 8);

    // front internal structure
    private final Polygon frontIS = new Polygon(
          new int[] { 23, 62, 118, 157, 93, 93, 87, 87 },
          new int[] { 41, 25, 25, 41, 73, 42, 42, 73 }, 8);
    // Left Front armor
    private final Polygon leftFrontArmor = new Polygon(
          new int[] { 0, 0, 23, 23 },
          new int[] { 30, 109, 109, 41 }, 4);

    // Left Front internal structure
    private final Polygon leftFrontIS = new Polygon(
          new int[] { 23, 87, 87, 84, 80, 71, 59, 23 },
          new int[] { 41, 73, 80, 80, 87, 87, 109, 109 }, 8);

    // Left rear armor
    private final Polygon leftRearArmor = new Polygon(
          new int[] { 0, 0, 23, 23 },
          new int[] { 109, 187, 175, 109 }, 4);

    // Left rear internal structure
    private final Polygon leftRearIS = new Polygon(
          new int[] { 23, 23, 66, 59, 59 },
          new int[] { 109, 175, 161, 149, 109 }, 5);

    // Right front armor
    private final Polygon rightFrontArmor = new Polygon(
          new int[] { 157, 180, 180, 157 },
          new int[] { 41, 30, 109, 109 }, 4);

    // Right front internal structure
    private final Polygon rightFrontIS = new Polygon(
          new int[] { 93, 157, 157, 121, 109, 100, 96, 93 },
          new int[] { 73, 41, 109, 109, 87, 87, 80, 80 }, 8);

    // Right rear armor
    private final Polygon rightRearArmor = new Polygon(
          new int[] { 157, 180, 180, 157 },
          new int[] { 109, 109, 187, 175 }, 4);

    // Right rear internal structure
    private final Polygon rightRearIS = new Polygon(
          new int[] { 121, 157, 157, 114, 121, 121 },
          new int[] { 109, 109, 175, 161, 149, 109 }, 6);

    // Rear armor
    private final Polygon rearArmor = new Polygon(
          new int[] { 180, 152, 26, 0, 23, 59, 121, 157 },
          new int[] { 187, 208, 208, 187, 175, 192, 192, 175 }, 8);

    // Rear internal structure
    private final Polygon rearIS = new Polygon(
          new int[] { 157, 121, 59, 23, 66, 76, 105, 114 },
          new int[] { 175, 192, 192, 175, 161, 177, 177, 161 }, 8);

    // Turret armor
    private final Polygon turretArmor = new Polygon(
          new int[] { 87, 87, 84, 80, 71, 59, 59, 75, 90, 90, 80, 65,
                      65, 115, 115, 100, 90, 90, 105, 121, 121, 109, 100, 96, 93, 93 },
          new int[] { 42, 80, 80, 87, 87, 109, 149, 177, 177, 165, 165, 142,
                      125, 125, 142, 165, 165, 177, 177, 149, 109, 87, 87, 80, 80, 42 }, 26);
    // Turret internal structure
    private final Polygon turretIS = new Polygon(
          new int[] { 65, 65, 80, 100, 115, 115 },
          new int[] { 125, 142, 165, 165, 142, 125 }, 6);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    public SuperHeavyTankMapSet(JComponent c, UnitDisplayPanel unitDisplayPanel) {
        this.unitDisplayPanel = unitDisplayPanel;
        jComponent = c;
        setAreas();
        setLabels();
        setBackGround();
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
        SuperHeavyTank t = (SuperHeavyTank) e;
        int a;
        int a0;
        for (int i = 1; i < 8; i++) {
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                  / (double) a0);
        }
        for (int i = 8; i < 15; i++) {
            a = t.getInternal(i - 8);
            a0 = t.getOInternal(i - 8);
            vLabels[i].setValue(t.getInternalString(i - 8));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a
                  / (double) a0);
        }
    }

    private void setContent() {
        for (int i = 1; i < 15; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        content.addArea(labels[15]);
        content.addArea(vLabels[15]);
    }

    private void setAreas() {
        areas[1] = new PMSimplePolygonArea(frontArmor, unitDisplayPanel, SuperHeavyTank.LOC_FRONT);
        areas[2] = new PMSimplePolygonArea(rightFrontArmor, unitDisplayPanel, SuperHeavyTank.LOC_FRONT_RIGHT);
        areas[3] = new PMSimplePolygonArea(leftFrontArmor, unitDisplayPanel, SuperHeavyTank.LOC_FRONT_LEFT);
        areas[4] = new PMSimplePolygonArea(rightRearArmor, unitDisplayPanel, SuperHeavyTank.LOC_REAR_RIGHT);
        areas[5] = new PMSimplePolygonArea(leftRearArmor, unitDisplayPanel, SuperHeavyTank.LOC_REAR_LEFT);
        areas[6] = new PMSimplePolygonArea(rearArmor, unitDisplayPanel, SuperHeavyTank.LOC_REAR);
        areas[7] = new PMSimplePolygonArea(turretArmor, unitDisplayPanel, SuperHeavyTank.LOC_TURRET);
        areas[8] = new PMSimplePolygonArea(frontIS, unitDisplayPanel, SuperHeavyTank.LOC_FRONT);
        areas[9] = new PMSimplePolygonArea(rightFrontIS, unitDisplayPanel, SuperHeavyTank.LOC_FRONT_RIGHT);
        areas[10] = new PMSimplePolygonArea(leftFrontIS, unitDisplayPanel, SuperHeavyTank.LOC_FRONT_LEFT);
        areas[11] = new PMSimplePolygonArea(rightRearIS, unitDisplayPanel, SuperHeavyTank.LOC_REAR_RIGHT);
        areas[12] = new PMSimplePolygonArea(leftRearIS, unitDisplayPanel, SuperHeavyTank.LOC_REAR_LEFT);
        areas[13] = new PMSimplePolygonArea(rearIS, unitDisplayPanel, SuperHeavyTank.LOC_REAR);
        areas[14] = new PMSimplePolygonArea(turretIS, unitDisplayPanel, SuperHeavyTank.LOC_TURRET);
    }

    private void setLabels() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        labels[1] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.FrontArmor"),
              fm, Color.black, 85, 15);
        labels[2] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.FRS"),
              fm, Color.black, 170, 80);
        labels[3] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.FLS"),
              fm, Color.black, 10, 80);
        labels[4] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.RRS"),
              fm, Color.black, 170, 155);
        labels[5] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.RLS"),
              fm, Color.black, 10, 155);
        labels[6] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.RearArmor"),
              fm, Color.black, 85, 200);
        labels[7] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.TurretArmor"),
              fm, Color.black, 90, 104);
        labels[8] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.FrontIS"),
              fm, Color.black, 80, 30);
        labels[9] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.FRIS"),
              fm, Color.black, 120, 80);
        labels[10] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.FLIS"),
              fm, Color.black, 43, 80);
        labels[11] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.RRIS"),
              fm, Color.black, 140, 155);
        labels[12] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.RLIS"),
              fm, Color.black, 43, 155);
        labels[13] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.RearIS"),
              fm, Color.black, 85, 185);
        labels[14] = WidgetUtils.createLabel(Messages.getString("LargeSupportTankMapSet.TurretIS"),
              fm, Color.black, 90, 140);

        // Value labels for all parts of mek
        // front
        fm = jComponent.getFontMetrics(FONT_VALUE);
        vLabels[1] = WidgetUtils.createValueLabel(115, 17, "", fm);
        vLabels[2] = WidgetUtils.createValueLabel(164, 70, "", fm);
        vLabels[3] = WidgetUtils.createValueLabel(6, 70, "", fm);
        vLabels[4] = WidgetUtils.createValueLabel(164, 140, "", fm);
        vLabels[5] = WidgetUtils.createValueLabel(6, 140, "", fm);
        vLabels[6] = WidgetUtils.createValueLabel(113, 202, "", fm);
        vLabels[7] = WidgetUtils.createValueLabel(93, 115, "", fm);
        vLabels[8] = WidgetUtils.createValueLabel(93, 151, "", fm);
        vLabels[9] = WidgetUtils.createValueLabel(140, 65, "", fm);
        vLabels[10] = WidgetUtils.createValueLabel(43, 65, "", fm);
        vLabels[11] = WidgetUtils.createValueLabel(145, 140, "", fm);
        vLabels[12] = WidgetUtils.createValueLabel(43, 140, "", fm);
        vLabels[13] = WidgetUtils.createValueLabel(113, 187, "", fm);
        vLabels[14] = WidgetUtils.createValueLabel(110, 32, "", fm);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

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
