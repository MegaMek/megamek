/*
 * Copyright (c) 2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk).
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.ProtoMek;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent ProtoMek unit within the
 * MekDisplay.ArmorPanel class.
 */
public class ProtoMekMapSet implements DisplayMapSet {

    private UnitDisplay unitDisplay;

    // Boring list of labels.
    private PMValueLabel[] sectionLabels = new PMValueLabel[ProtoMek.NUM_PROTOMEK_LOCATIONS];
    private PMValueLabel[] armorLabels = new PMValueLabel[ProtoMek.NUM_PROTOMEK_LOCATIONS];
    private PMValueLabel[] internalLabels = new PMValueLabel[ProtoMek.NUM_PROTOMEK_LOCATIONS];
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[ProtoMek.NUM_PROTOMEK_LOCATIONS];

    private Polygon head = new Polygon(new int[] { 50, 50, 60, 80, 90, 90, 80,
            60 }, new int[] { 40, 20, 10, 10, 20, 40, 50, 50 }, 8);
    private Polygon mainGun = new Polygon(new int[] { 20, 20, 50, 50 },
            new int[] { 30, 0, 0, 30 }, 4);
    private Polygon leftArm = new Polygon(new int[] { 0, 0, 20, 30, 40, 30, 20,
            20, 10 }, new int[] { 100, 40, 30, 30, 60, 60, 70, 110, 110 }, 9);
    private Polygon rightArm = new Polygon(new int[] { 120, 120, 110, 100, 110,
            120, 140, 140, 130 }, new int[] { 110, 70, 60, 60, 30, 30, 40, 100,
            110, 110 }, 9);
    private Polygon torso = new Polygon(new int[] { 40, 40, 30, 50, 50, 60, 80,
            90, 90, 110, 100, 100 }, new int[] { 130, 60, 30, 30, 40, 50, 50,
            40, 30, 30, 60, 130 }, 12);
    private Polygon legs = new Polygon(new int[] { 0, 0, 10, 30, 30, 40, 100,
            110, 110, 130, 140, 140, 100, 90, 90, 80, 60, 50, 50, 40 },
            new int[] { 240, 230, 220, 220, 160, 130, 130, 160, 220, 220, 230,
                    240, 240, 230, 190, 170, 170, 190, 230, 240 }, 20);

    // Reference to Component (required for Image handling)
    private JComponent comp;
    // Content group which will be sent to PicMap component
    private PMAreasGroup content = new PMAreasGroup();
    // Set of Background drawers which will be sent to PicMap component
    private Vector<BackGroundDrawer> bgDrawers = new Vector<>();

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
            GUIP.getUnitDisplayMekArmorLargeFontSize());

    /**
     * This constructor can only be called from the addNotify method
     */
    public ProtoMekMapSet(JComponent c, UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        comp = c;
        setAreas();
        setBackGround();
    }

    /**
     * Set the armor diagram on the mapset.
     */
    private void setAreas() {
        areas[ProtoMek.LOC_HEAD] = new PMSimplePolygonArea(head, unitDisplay, ProtoMek.LOC_HEAD);
        areas[ProtoMek.LOC_LEG] = new PMSimplePolygonArea(legs, unitDisplay, ProtoMek.LOC_LEG);
        areas[ProtoMek.LOC_LARM] = new PMSimplePolygonArea(leftArm, unitDisplay, ProtoMek.LOC_LARM);
        areas[ProtoMek.LOC_RARM] = new PMSimplePolygonArea(rightArm, unitDisplay, ProtoMek.LOC_RARM);
        areas[ProtoMek.LOC_TORSO] = new PMSimplePolygonArea(torso, unitDisplay, ProtoMek.LOC_TORSO);
        areas[ProtoMek.LOC_MAINGUN] = new PMSimplePolygonArea(mainGun, unitDisplay, ProtoMek.LOC_MAINGUN);

        for (int i = 0; i <= 5; i++) {
            content.addArea(areas[i]);
        }

        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);

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
     * Sets the background on the mapset.
     */
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

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM | BackGroundDrawer.HALIGN_LEFT;
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

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }
}
