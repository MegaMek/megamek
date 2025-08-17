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
import megamek.common.units.Aero;
import megamek.common.Configuration;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent ASF unit in MekDisplay.ArmorPanel class.
 */
public class AeroMapSet implements DisplayMapSet {

    private final JComponent jComponent;
    private final PMSimplePolygonArea[] areas = new PMSimplePolygonArea[5];
    private final PMSimpleLabel[] labels = new PMSimpleLabel[13];
    private final PMValueLabel[] vLabels = new PMValueLabel[13];
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private final PMAreasGroup content = new PMAreasGroup();

    private final UnitDisplayPanel unitDisplayPanel;

    // private static final int INT_STR_OFFSET = 4;
    // Polygons for all areas
    private final Polygon noseArmor = new Polygon(
          new int[] { 45, 50, 60, 65, 75, 80 }, new int[] { 80, 20, 0, 0, 20, 80 }, 6);
    // front internal structure
    private final Polygon Structure = new Polygon(new int[] { 50, 50, 75, 75 },
          new int[] { 80, 160, 160, 80 }, 4);
    // Left armor
    private final Polygon leftWingArmor = new Polygon(new int[] { 50, 45, 0, 0, 45, 50 },
          new int[] { 80, 80, 140, 180, 160, 160 },
          6);

    // Right armor
    private final Polygon rightWingArmor = new Polygon(new int[] { 75, 80, 125, 125, 80, 75 },
          new int[] { 80, 80, 140, 180, 160, 160 },
          6);

    // Rear armor
    private final Polygon aftArmor = new Polygon(new int[] { 45, 45, 30, 30, 95, 95, 80, 80 },
          new int[] { 160, 180, 190, 200, 200, 190, 180, 160 },
          8);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    public AeroMapSet(JComponent c, UnitDisplayPanel unitDisplayPanel) {
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
        Aero t = (Aero) e;
        int a;
        int a0;
        // TODO: change this back to locations
        for (int i = 0; i < t.locations(); i++) {
            if (i == Aero.LOC_FUSELAGE) {
                continue;
            }
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a / (double) a0);

        }
        a = t.getSI();
        a0 = t.getOSI();
        vLabels[4].setValue(Integer.toString(t.getSI()));
        WidgetUtils.setAreaColor(areas[4], vLabels[4], (double) a / (double) a0);

        // now for the vitals
        vLabels[5].setValue(getCriticalHitTally(t.getAvionicsHits(), 3));
        vLabels[6].setValue(getCriticalHitTally(t.getEngineHits(),
              t.getMaxEngineHits()));
        vLabels[7].setValue(getCriticalHitTally(t.getFCSHits(), 3));
        vLabels[8].setValue(getCriticalHitTally(t.getSensorHits(), 3));
        if (t instanceof SmallCraft sc) {
            // add in thrusters
            vLabels[9].setValue(getCriticalHitTally(sc.getLeftThrustHits(), 3));
            vLabels[10].setValue(getCriticalHitTally(sc.getRightThrustHits(), 3));
        } else {
            vLabels[9].setValue("-");
            vLabels[10].setValue("-");
        }

        if (t instanceof Dropship ds) {
            // add kf boom and docking collar
            int kfboom = 0;
            int collar = 0;
            if (ds.isKFBoomDamaged()) {
                kfboom = 1;
            }
            vLabels[11].setValue(getCriticalHitTally(kfboom, 1));
            if (ds.isDockCollarDamaged()) {
                collar = 1;
            }
            vLabels[12].setValue(getCriticalHitTally(collar, 1));
        }

    }

    private void setContent() {

        for (int i = 0; i < 4; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        content.addArea(areas[4]);
        content.addArea(labels[4]);
        content.addArea(vLabels[4]);

        content.addArea(labels[5]);
        content.addArea(vLabels[5]);
        content.addArea(labels[6]);
        content.addArea(vLabels[6]);
        content.addArea(labels[7]);
        content.addArea(vLabels[7]);
        content.addArea(labels[8]);
        content.addArea(vLabels[8]);
        content.addArea(labels[9]);
        content.addArea(vLabels[9]);
        content.addArea(labels[10]);
        content.addArea(vLabels[10]);
        content.addArea(labels[11]);
        content.addArea(vLabels[11]);
        content.addArea(labels[12]);
        content.addArea(vLabels[12]);
    }

    private void setAreas() {
        areas[Aero.LOC_NOSE] = new PMSimplePolygonArea(noseArmor, unitDisplayPanel, Aero.LOC_NOSE);
        areas[Aero.LOC_RWING] = new PMSimplePolygonArea(rightWingArmor, unitDisplayPanel, Aero.LOC_RWING);
        areas[Aero.LOC_LWING] = new PMSimplePolygonArea(leftWingArmor, unitDisplayPanel, Aero.LOC_LWING);
        areas[Aero.LOC_AFT] = new PMSimplePolygonArea(aftArmor, unitDisplayPanel, Aero.LOC_AFT);
        areas[4] = new PMSimplePolygonArea(Structure, unitDisplayPanel, Aero.LOC_NOSE);
    }

    private void setLabels() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_LABEL);

        // Labels for Front view
        // Prefer to use message thingy but don't know how
        labels[Aero.LOC_NOSE] = WidgetUtils.createLabel("NOS", fm, Color.black, 62, 30);
        //   labels[Aero.LOC_NOSE + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.FrontIS"), fm, Color.black, 10, 57);
        labels[Aero.LOC_LWING] = WidgetUtils.createLabel("LWG", fm, Color.black, 32, 120);
        //    labels[Aero.LOC_LWING + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.LIS"), fm, Color.black, 10, 106);
        labels[Aero.LOC_RWING] = WidgetUtils.createLabel("RWG", fm, Color.black, 92, 120);
        //    labels[Aero.LOC_RWING + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.RIS"), fm, Color.black, 10, 106);
        labels[Aero.LOC_AFT] = WidgetUtils.createLabel("AFT", fm, Color.black, 62, 170);
        labels[4] = WidgetUtils.createLabel("SI", fm, Color.black, 62, 120);
        labels[5] = WidgetUtils.createLabel("Avionics:", fm, Color.white, 10, 210);
        labels[6] = WidgetUtils.createLabel("Engine:", fm, Color.white, 10, 225);
        labels[7] = WidgetUtils.createLabel("FCS:", fm, Color.white, 10, 240);
        labels[8] = WidgetUtils.createLabel("Sensors:", fm, Color.white, 10, 255);
        labels[9] = WidgetUtils.createLabel("L Thrust:", fm, Color.white, 90, 210);
        labels[10] = WidgetUtils.createLabel("R Thrust:", fm, Color.white, 90, 225);
        labels[11] = WidgetUtils.createLabel("K-F Boom:", fm, Color.white, 90, 240);
        labels[12] = WidgetUtils.createLabel("Collar:", fm, Color.white, 90, 255);

        // Value labels for all parts of aero
        // front
        fm = jComponent.getFontMetrics(FONT_VALUE);
        vLabels[Aero.LOC_NOSE] = WidgetUtils.createValueLabel(62, 45, "", fm);
        //   vLabels[Aero.LOC_NOSE + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 58, "", fm);
        vLabels[Aero.LOC_LWING] = WidgetUtils.createValueLabel(32, 135, "", fm);
        //   vLabels[Aero.LOC_LWING + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 100, "", fm);
        vLabels[Aero.LOC_RWING] = WidgetUtils.createValueLabel(92, 135, "", fm);
        //   vLabels[Aero.LOC_RWING + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 100, "", fm);
        vLabels[Aero.LOC_AFT] = WidgetUtils.createValueLabel(62, 185, "", fm);
        vLabels[4] = WidgetUtils.createValueLabel(62, 135, "", fm);
        vLabels[5] = WidgetUtils.createValueLabel(40, 210, "", fm);
        vLabels[6] = WidgetUtils.createValueLabel(40, 225, "", fm);
        vLabels[7] = WidgetUtils.createValueLabel(40, 240, "", fm);
        vLabels[8] = WidgetUtils.createValueLabel(40, 255, "", fm);
        vLabels[9] = WidgetUtils.createValueLabel(130, 210, "", fm);
        vLabels[10] = WidgetUtils.createValueLabel(130, 225, "", fm);
        vLabels[11] = WidgetUtils.createValueLabel(135, 240, "", fm);
        vLabels[12] = WidgetUtils.createValueLabel(130, 255, "", fm);
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

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

    private void translateAreas() {
        areas[Aero.LOC_NOSE].translate(0, 0);
        // areas[Aero.LOC_NOSE + INT_STR_OFFSET].translate(8, 29);
        areas[Aero.LOC_LWING].translate(0, 0);
        // areas[Aero.LOC_LWING + INT_STR_OFFSET].translate(8, 29);
        areas[Aero.LOC_RWING].translate(0, 0);
        // areas[Aero.LOC_RWING + INT_STR_OFFSET].translate(8, 29);
        areas[Aero.LOC_AFT].translate(0, 0);
        areas[4].translate(0, 0);
    }

    private String getCriticalHitTally(int tally, int max) {
        StringBuilder marks = new StringBuilder();

        if (tally < 1) {
            return marks.toString();
        }

        if (tally >= max) {
            marks = new StringBuilder("Out");
            return marks.toString();
        }

        while (tally > 0) {
            marks.append("X");
            tally--;
        }

        return marks.toString();
    }
}
