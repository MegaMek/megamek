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
import megamek.common.Configuration;
import megamek.common.equipment.DockingCollar;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent ASF unit in MekDisplay.ArmorPanel class.
 */
public class JumpshipMapSet implements DisplayMapSet {

    private final JComponent jComponent;
    private final PMSimplePolygonArea[] areas = new PMSimplePolygonArea[7];
    private final PMSimpleLabel[] labels = new PMSimpleLabel[17];
    private final PMValueLabel[] vLabels = new PMValueLabel[17];
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private final PMAreasGroup content = new PMAreasGroup();

    UnitDisplayPanel unitDisplayPanel;

    //private static final int INT_STR_OFFSET = 4;
    //Polygons for all areas
    private final Polygon noseArmor = new Polygon(new int[] { 0, 20, 80, 100, 100, 80, 20, 0 },
          new int[] { 20, 0, 0, 20, 40, 60, 60, 40 }, 8);
    //front internal structure
    private final Polygon Structure = new Polygon(new int[] { 40, 60, 60, 40 },
          new int[] { 60, 60, 160, 160 }, 4);
    //Left front armor
    private final Polygon leftFSArmor = new Polygon(new int[] { 20, 40, 40, 20 },
          new int[] { 60, 60, 120, 120 }, 4);
    //Left aft armor
    private final Polygon leftASArmor = new Polygon(new int[] { 20, 40, 40, 20 },
          new int[] { 120, 120, 160, 160 }, 4);

    private final Polygon rightFSArmor = new Polygon(new int[] { 60, 80, 80, 60 },
          new int[] { 60, 60, 120, 120 }, 4);
    //right aft armor
    private final Polygon rightASArmor = new Polygon(new int[] { 60, 80, 80, 60 },
          new int[] { 120, 120, 160, 160 }, 4);

    //Rear armor
    private final Polygon aftArmor = new Polygon(new int[] { 20, 0, 40, 40, 60, 60, 100, 80 },
          new int[] { 160, 200, 190, 200, 200, 190, 200, 160 }, 8);

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    public JumpshipMapSet(JComponent c, UnitDisplayPanel unitDisplayPanel) {
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
        Jumpship t = (Jumpship) e;
        int a;
        int a0;
        //TODO: change this back to locations
        for (int i = 0; i < t.locations(); i++) {
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a / (double) a0);

        }
        a = t.getSI();
        a0 = t.getOSI();
        vLabels[6].setValue(Integer.toString(t.getSI()));
        WidgetUtils.setAreaColor(areas[6], vLabels[6], (double) a / (double) a0);

        //now for the vitals
        //need some extra info for docking collars
        int damagedCollars = 0;
        //We want a different string for this one, in case there are 25 collars...
        String collarDamageString = "";
        for (DockingCollar collar : t.getDockingCollars()) {
            if (collar.isDamaged()) {
                damagedCollars++;
            }
        }
        if (damagedCollars > 0) {
            collarDamageString = String.format("X (%d)", damagedCollars);
        }
        //We want a different string for these too
        String kfDamageString = "";
        if (t.getKFDriveDamage() > 0) {
            kfDamageString = String.format("%d / %d", t.getKFIntegrity(), t.getOKFIntegrity());
        }
        String sailDamageString = "";
        if (t.getSailIntegrity() < t.getOSailIntegrity()) {
            sailDamageString = String.format("%d / %d", t.getSailIntegrity(), t.getOSailIntegrity());
        }
        vLabels[7].setValue(getCriticalHitTally(t.getAvionicsHits(), 3));
        vLabels[8].setValue(getCriticalHitTally(t.getCICHits(), 3));
        vLabels[9].setValue(getCriticalHitTally(t.getEngineHits(), t.getMaxEngineHits()));
        vLabels[10].setValue(getCriticalHitTally(t.getSensorHits(), 3));
        vLabels[11].setValue(getCriticalHitTally(t.getLeftThrustHits(), 3));
        vLabels[12].setValue(getCriticalHitTally(t.getRightThrustHits(), 3));
        vLabels[13].setValue(collarDamageString);
        vLabels[14].setValue(getCriticalHitTally(t.getTotalDamagedGravDeck(), t.getTotalGravDeck()));
        vLabels[15].setValue(kfDamageString);
        vLabels[16].setValue(sailDamageString);

    }

    private void setContent() {
        for (int i = 0; i < 6; i++) {
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        content.addArea(areas[6]);
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
        content.addArea(labels[13]);
        content.addArea(vLabels[13]);
        content.addArea(labels[14]);
        content.addArea(vLabels[14]);
        content.addArea(labels[15]);
        content.addArea(vLabels[15]);
        content.addArea(labels[16]);
        content.addArea(vLabels[16]);

    }

    private void setAreas() {
        areas[Jumpship.LOC_NOSE] = new PMSimplePolygonArea(noseArmor, unitDisplayPanel, Jumpship.LOC_NOSE);
        areas[Jumpship.LOC_FLS] = new PMSimplePolygonArea(leftFSArmor, unitDisplayPanel, Jumpship.LOC_FLS);
        areas[Jumpship.LOC_FRS] = new PMSimplePolygonArea(rightFSArmor, unitDisplayPanel, Jumpship.LOC_FRS);
        areas[Jumpship.LOC_ALS] = new PMSimplePolygonArea(leftASArmor, unitDisplayPanel, Jumpship.LOC_ALS);
        areas[Jumpship.LOC_ARS] = new PMSimplePolygonArea(rightASArmor, unitDisplayPanel, Jumpship.LOC_ARS);
        areas[Jumpship.LOC_AFT] = new PMSimplePolygonArea(aftArmor, unitDisplayPanel, Jumpship.LOC_AFT);
        areas[6] = new PMSimplePolygonArea(Structure, unitDisplayPanel, Jumpship.LOC_NOSE);
    }

    private void setLabels() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_LABEL);

        //Labels for Front view
        //Prefer to use message thingy but don't know how
        labels[Jumpship.LOC_NOSE] = WidgetUtils.createLabel("NOS", fm, Color.black, 50, 20);
        //labels[Aero.LOC_NOSE + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.FrontIS"), fm, Color.black, 10, 57);
        labels[Jumpship.LOC_FLS] = WidgetUtils.createLabel("FLS", fm, Color.black, 30, 80);
        //labels[Aero.LOC_LEFT_WING + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.LIS"), fm, Color.black, 10, 106);
        labels[Jumpship.LOC_FRS] = WidgetUtils.createLabel("FRS", fm, Color.black, 70, 80);
        labels[Jumpship.LOC_ALS] = WidgetUtils.createLabel("ALS", fm, Color.black, 30, 130);
        labels[Jumpship.LOC_ARS] = WidgetUtils.createLabel("ARS", fm, Color.black, 70, 130);
        labels[Jumpship.LOC_AFT] = WidgetUtils.createLabel("AFT", fm, Color.black, 50, 170);
        labels[6] = WidgetUtils.createLabel("SI", fm, Color.black, 50, 110);
        labels[7] = WidgetUtils.createLabel("Avionics:", fm, Color.white, 10, 210);
        labels[8] = WidgetUtils.createLabel("CIC:", fm, Color.white, 10, 225);
        labels[9] = WidgetUtils.createLabel("Engine:", fm, Color.white, 10, 240);
        labels[10] = WidgetUtils.createLabel("Sensors:", fm, Color.white, 10, 255);
        labels[11] = WidgetUtils.createLabel("L Thrust:", fm, Color.white, 90, 210);
        labels[12] = WidgetUtils.createLabel("R Thrust:", fm, Color.white, 90, 225);
        labels[13] = WidgetUtils.createLabel("Collars:", fm, Color.white, 90, 240);
        labels[14] = WidgetUtils.createLabel("Grav Decks:", fm, Color.white, 90, 255);
        labels[15] = WidgetUtils.createLabel("K-F Drive:", fm, Color.white, 10, 270);
        labels[16] = WidgetUtils.createLabel("Jump Sail:", fm, Color.white, 10, 285);

        //Value labels for all parts of the ship
        //front
        fm = jComponent.getFontMetrics(FONT_VALUE);
        vLabels[Jumpship.LOC_NOSE] = WidgetUtils.createValueLabel(50, 35, "", fm);
        //vLabels[Aero.LOC_NOSE + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 58, "", fm);
        vLabels[Jumpship.LOC_FLS] = WidgetUtils.createValueLabel(30, 95, "", fm);
        //vLabels[Aero.LOC_LEFT_WING + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 100, "", fm);
        vLabels[Jumpship.LOC_FRS] = WidgetUtils.createValueLabel(70, 95, "", fm);
        vLabels[Jumpship.LOC_ALS] = WidgetUtils.createValueLabel(30, 145, "", fm);
        vLabels[Jumpship.LOC_ARS] = WidgetUtils.createValueLabel(70, 145, "", fm);
        //vLabels[Aero.LOC_RIGHT_WING + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 100, "", fm);
        vLabels[Jumpship.LOC_AFT] = WidgetUtils.createValueLabel(50, 185, "", fm);
        vLabels[6] = WidgetUtils.createValueLabel(50, 135, "", fm);
        vLabels[7] = WidgetUtils.createValueLabel(40, 210, "", fm);
        vLabels[8] = WidgetUtils.createValueLabel(40, 225, "", fm);
        vLabels[9] = WidgetUtils.createValueLabel(40, 240, "", fm);
        vLabels[10] = WidgetUtils.createValueLabel(40, 255, "", fm);
        vLabels[11] = WidgetUtils.createValueLabel(130, 210, "", fm);
        vLabels[12] = WidgetUtils.createValueLabel(130, 225, "", fm);
        vLabels[13] = WidgetUtils.createValueLabel(130, 240, "", fm);
        vLabels[14] = WidgetUtils.createValueLabel(130, 255, "", fm);
        vLabels[15] = WidgetUtils.createValueLabel(60, 270, "", fm);
        vLabels[16] = WidgetUtils.createValueLabel(60, 285, "", fm);
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
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
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
