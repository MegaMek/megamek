/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Configuration;
import megamek.common.DockingCollar;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to 
 * represent ASF unit in MechDisplay.ArmorPanel class.
 */
public class WarshipMapSet implements DisplayMapSet {

    private UnitDisplay unitDisplay;
    
    private JComponent comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[7];
    private PMSimpleLabel[] labels = new PMSimpleLabel[17];
    private PMValueLabel[] vLabels = new PMValueLabel[17];
    private Vector<BackGroundDrawer>  bgDrawers = new Vector<>();
    private PMAreasGroup content = new PMAreasGroup();

    //private static final int INT_STR_OFFSET = 4;
    //Polygons for all areas
    private Polygon noseArmor = new Polygon( new int[] { 0, 25, 75, 100 },
            new int[] { 50, 0, 0, 50 }, 4);
    //front internal structure
    private Polygon Structure = new Polygon(new int[] { 35, 65, 65, 35 },
            new int[] { 50, 50, 150, 150 }, 4);
    //Left front armor
    private Polygon leftFSArmor = new Polygon(new int[] { 0, 35, 35, 0 },
            new int[] { 50, 50, 110, 110 }, 4);
    //Left aft armor
    private Polygon leftASArmor = new Polygon(new int[] { 0, 35, 35, 0 },
            new int[] { 110, 110, 150, 150 }, 4);

    private Polygon rightFSArmor = new Polygon(new int[] { 65, 100, 100, 65 },
            new int[] { 50, 50, 110, 110 }, 4);
    //right aft armor
    private Polygon rightASArmor = new Polygon(new int[] { 65, 100, 100, 65 },
            new int[] { 110, 110, 150, 150 }, 4);

    //Rear armor
    private Polygon aftArmor = new Polygon (new int[] { 0, -10, 40, 40, 60, 60, 110, 100 },
            new int[] { 150, 200, 190, 200, 200, 190, 200, 150 }, 8);

    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN,
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize"));
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN,
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize"));

    public WarshipMapSet(JComponent c, UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        comp = c;
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
        Jumpship t = (Jumpship) e;
        int a = 1;
        int a0 = 1;
        //TODO: change this back to locations
        for (int i = 0; i < 6; i++) {
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double) a / (double) a0);

        }
        a = t.getSI();
        a0 = t.get0SI();
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
        areas[Jumpship.LOC_NOSE] = new PMSimplePolygonArea(noseArmor, unitDisplay, Jumpship.LOC_NOSE);
        areas[Jumpship.LOC_FLS] = new PMSimplePolygonArea(leftFSArmor, unitDisplay, Jumpship.LOC_FLS);
        areas[Jumpship.LOC_FRS] = new PMSimplePolygonArea(rightFSArmor, unitDisplay, Jumpship.LOC_FRS);
        areas[Jumpship.LOC_ALS] = new PMSimplePolygonArea(leftASArmor, unitDisplay, Jumpship.LOC_ALS);
        areas[Jumpship.LOC_ARS] = new PMSimplePolygonArea(rightASArmor, unitDisplay, Jumpship.LOC_ARS);
        areas[Jumpship.LOC_AFT] = new PMSimplePolygonArea(aftArmor, unitDisplay, Jumpship.LOC_AFT);
        areas[6] = new PMSimplePolygonArea(Structure, unitDisplay, Jumpship.LOC_NOSE);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        //Labels for Front view
        //Prefer to use message thingy but don't know how
        labels[Jumpship.LOC_NOSE] = WidgetUtils.createLabel("NOS", fm, Color.black, 50, 15);
        labels[Jumpship.LOC_FLS] = WidgetUtils.createLabel("FLS", fm, Color.black, 17, 70);
        labels[Jumpship.LOC_FRS] = WidgetUtils.createLabel("FRS", fm, Color.black, 83, 70);
        labels[Jumpship.LOC_ALS] = WidgetUtils.createLabel("ALS", fm, Color.black, 17, 120);
        labels[Jumpship.LOC_ARS] = WidgetUtils.createLabel("ARS", fm, Color.black, 83, 120);
        labels[Jumpship.LOC_AFT] = WidgetUtils.createLabel("AFT", fm, Color.black, 50, 160);
        labels[6] = WidgetUtils.createLabel("SI", fm, Color.black, 50, 90);
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

        // Value labels for all parts of mek
        // front
        fm = comp.getFontMetrics(FONT_VALUE);
        vLabels[Jumpship.LOC_NOSE] = WidgetUtils.createValueLabel(50, 30, "", fm);
        vLabels[Jumpship.LOC_FLS] = WidgetUtils.createValueLabel(17, 85, "", fm);
        vLabels[Jumpship.LOC_FRS] = WidgetUtils.createValueLabel(83, 85, "", fm);
        vLabels[Jumpship.LOC_ALS] = WidgetUtils.createValueLabel(17, 135, "", fm);
        vLabels[Jumpship.LOC_ARS] = WidgetUtils.createValueLabel(83, 135, "", fm);
        vLabels[Jumpship.LOC_AFT] = WidgetUtils.createValueLabel(50, 175, "", fm);
        vLabels[6] = WidgetUtils.createValueLabel(50, 105, "", fm);
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

        Image tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

    private void translateAreas() {

    }

    private String getCriticalHitTally(int tally, int max) {
        String marks = "";

        if (tally < 1) {
            return marks;
        }

        if (tally >= max) {
            marks = "Out";
            return marks;
        }

        while (tally > 0) {
            marks = marks + "X";
            tally--;
        }

        return marks;
    }
}
