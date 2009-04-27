/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Entity;
import megamek.common.Protomech;

/**
 * Class which keeps set of all areas required to represent Protomech unit in
 * MechDisplay.ArmorPanel class.
 */
public class ProtomechMapSet implements DisplayMapSet {

    private static final String IMAGE_DIR = "data/images/widgets";

    // Boring list of labels.
    private PMValueLabel[] sectionLabels = new PMValueLabel[Protomech.NUM_PMECH_LOCATIONS];
    private PMValueLabel[] armorLabels = new PMValueLabel[Protomech.NUM_PMECH_LOCATIONS];
    private PMValueLabel[] internalLabels = new PMValueLabel[Protomech.NUM_PMECH_LOCATIONS];
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[Protomech.NUM_PMECH_LOCATIONS];

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
    // Set of Backgrpund drawers which will be sent to PicMap component
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();

    private static final Font FONT_VALUE = new Font(
            "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$

    /**
     * This constructor have to be called anly from addNotify() method
     */
    public ProtomechMapSet(JComponent c) {
        comp = c;
        setAreas();
        setBackGround();
    }

    /*
     * * Set the armor diagram on the mapset.
     */
    private void setAreas() {
        areas[Protomech.LOC_HEAD] = new PMSimplePolygonArea(head);
        areas[Protomech.LOC_LEG] = new PMSimplePolygonArea(legs);
        areas[Protomech.LOC_LARM] = new PMSimplePolygonArea(leftArm);
        areas[Protomech.LOC_RARM] = new PMSimplePolygonArea(rightArm);
        areas[Protomech.LOC_TORSO] = new PMSimplePolygonArea(torso);
        areas[Protomech.LOC_MAINGUN] = new PMSimplePolygonArea(mainGun);

        for (int i = 0; i <= 5; i++) {
            content.addArea(areas[i]);
        }

        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);

        for (int i = 0; i < Protomech.NUM_PMECH_LOCATIONS; i++) {
            sectionLabels[i] = new PMValueLabel(fm, Color.black);
            content.addArea(sectionLabels[i]);
            armorLabels[i] = new PMValueLabel(fm, Color.yellow.brighter());
            content.addArea(armorLabels[i]);
            internalLabels[i] = new PMValueLabel(fm, Color.red.brighter());
            content.addArea(internalLabels[i]);
        }
        sectionLabels[0].moveTo(70, 30);
        armorLabels[0].moveTo(60, 45);
        internalLabels[0].moveTo(80, 45);
        sectionLabels[1].moveTo(70, 70);
        armorLabels[1].moveTo(70, 85);
        internalLabels[1].moveTo(70, 100);
        sectionLabels[2].moveTo(125, 55);
        armorLabels[2].moveTo(125, 70);
        internalLabels[2].moveTo(125, 85);
        sectionLabels[3].moveTo(15, 55);
        armorLabels[3].moveTo(15, 70);
        internalLabels[3].moveTo(15, 85);
        sectionLabels[4].moveTo(70, 150);
        armorLabels[4].moveTo(60, 165);
        internalLabels[4].moveTo(80, 165);
        sectionLabels[5].moveTo(35, 15);
        armorLabels[5].moveTo(25, 30);
        internalLabels[5].moveTo(45, 30);
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    /**
     * Show the diagram for the given Protomech.
     * 
     * @param entity - the <code>Entity</code> to be displayed. This should be
     *            a <code>Protomech</code> unit.
     */
    public void setEntity(Entity entity) {
        Protomech proto = (Protomech) entity;

        int loc = proto.locations();
        if (loc != Protomech.NUM_PMECH_LOCATIONS) {
            armorLabels[5].setVisible(false);
            internalLabels[5].setVisible(false);
            sectionLabels[5].setVisible(false);
        } else {
            armorLabels[5].setVisible(true);
            internalLabels[5].setVisible(true);
            sectionLabels[5].setVisible(true);
        }
        for (int i = 0; i < loc; i++) {
            // armor = proto.getArmor(i);
            // internal = proto.getInternal(i);
            armorLabels[i].setValue(proto.getArmorString(i));
            internalLabels[i].setValue(proto.getInternalString(i));
            sectionLabels[i].setValue(proto.getLocationAbbr(i));
        }
    }

    /*
     * * Sets the background on the mapset.
     */
    private void setBackGround() {
        Image tile = comp.getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR + "/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

    }

}
