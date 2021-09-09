/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Aero;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.options.OptionsConstants;

/**
 * Class which keeps set of all areas required to represent Capital Fighter unit
 * in MechDsiplay.ArmorPanel class.
 */
public class CapitalFighterMapSet implements DisplayMapSet {

    private JComponent comp;
    // Images that shows how much armor left.
    private Image armorImage;
    // Set of areas to show fighter armor left
    private PMPicArea armorArea;
    // Set of labels to show fighter armor left
    // images and areas for each crit tally
    private Image avCritImage;
    private PMPicArea avCritArea;
    private Image engineCritImage;
    private PMPicArea engineCritArea;
    private Image fcsCritImage;
    private PMPicArea fcsCritArea;
    private Image sensorCritImage;
    private PMPicArea sensorCritArea;
    private Image pilotCritImage;
    private PMPicArea pilotCritArea;
    private PMSimpleLabel armorLabel;
    private PMValueLabel armorVLabel;
    private PMSimpleLabel avCritLabel;
    private PMSimpleLabel engineCritLabel;
    private PMSimpleLabel fcsCritLabel;
    private PMSimpleLabel sensorCritLabel;
    private PMSimpleLabel pilotCritLabel;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    private int stepY = 14;
    private int squareSize = 7;
    private int armorRows = 8;
    private int armorCols = 6;

    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, //$NON-NLS-1$
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize"));

    public CapitalFighterMapSet(JComponent c) {
        comp = c;
        setAreas();
        setLabels();
        setBackGround();
        translateAreas();
        setContent();
    }

    public void setRest() {
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
    }

    public void setEntity(Entity e) {
        Aero t = (Aero) e;

        int armor = t.getCapArmor();
        int armorO = t.getCap0Armor();
        armorVLabel.setValue(Integer.toString(armor));

        if (t.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            armor = (int) Math.ceil(armor / 10.0);
            armorO = (int) Math.ceil(armorO / 10.0);
        }

        drawArmorImage(armorImage, armor, armorO);
        drawCrits(avCritImage, t.getAvionicsHits());
        drawCrits(engineCritImage, t.getEngineHits());
        drawCrits(fcsCritImage, t.getFCSHits());
        drawCrits(sensorCritImage, t.getSensorHits());
        drawCrits(pilotCritImage, t.getCrew().getHits());
    }

    private void setContent() {
        content.addArea(armorLabel);
        content.addArea(armorArea);
        content.addArea(armorVLabel);
        content.addArea(avCritLabel);
        content.addArea(engineCritLabel);
        content.addArea(fcsCritLabel);
        content.addArea(sensorCritLabel);
        content.addArea(pilotCritLabel);
        content.addArea(avCritArea);
        content.addArea(engineCritArea);
        content.addArea(fcsCritArea);
        content.addArea(sensorCritArea);
        content.addArea(pilotCritArea);
    }

    private void setAreas() {
        armorImage = comp.createImage(armorCols * (squareSize + 1), armorRows * (squareSize + 1));
        armorArea = new PMPicArea(armorImage);

        avCritImage = comp.createImage(3 * (squareSize + 1), squareSize + 1);
        avCritArea = new PMPicArea(avCritImage);
        engineCritImage = comp.createImage(3 * (squareSize + 1), squareSize + 1);
        engineCritArea = new PMPicArea(engineCritImage);
        fcsCritImage = comp.createImage(3 * (squareSize + 1), squareSize + 1);
        fcsCritArea = new PMPicArea(fcsCritImage);
        sensorCritImage = comp.createImage(3 * (squareSize + 1), squareSize + 1);
        sensorCritArea = new PMPicArea(sensorCritImage);
        pilotCritImage = comp.createImage(6 * (squareSize + 1), squareSize + 1);
        pilotCritArea = new PMPicArea(pilotCritImage);
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);
        armorLabel = new PMSimpleLabel("Armor:", fm, Color.white);
        armorVLabel = new PMValueLabel(fm, Color.red.brighter());

        avCritLabel = new PMSimpleLabel("Avionics:", fm, Color.white); //$NON-NLS-1$
        engineCritLabel = new PMSimpleLabel("Engine:", fm, Color.white); //$NON-NLS-1$
        fcsCritLabel = new PMSimpleLabel("FCS:", fm, Color.white); //$NON-NLS-1$
        sensorCritLabel = new PMSimpleLabel("Sensors:", fm, Color.white); //$NON-NLS-1$
        pilotCritLabel = new PMSimpleLabel("Pilot hits:", fm, Color.white); //$NON-NLS-1$
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = comp.getToolkit()
                .getImage(new File(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit()
                .getImage(new File(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new File(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit()
                .getImage(new File(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

    private void translateAreas() {
        armorLabel.translate(0, 0);
        armorArea.translate(0, squareSize);
        armorVLabel.translate((armorCols * (squareSize + 1)) / 2, squareSize + (armorRows * (squareSize + 1)) / 2);

        avCritLabel.translate(5 + armorCols * (squareSize + 1), stepY);
        engineCritLabel.translate(5 + armorCols * (squareSize + 1), 2 * stepY);
        fcsCritLabel.translate(5 + armorCols * (squareSize + 1), 3 * stepY);
        sensorCritLabel.translate(5 + armorCols * (squareSize + 1), 4 * stepY);
        pilotCritLabel.translate(5 + armorCols * (squareSize + 1), 5 * stepY);

        avCritArea.translate(10 + pilotCritLabel.width + armorCols * (squareSize + 1), stepY - (squareSize + 1));
        engineCritArea.translate(10 + pilotCritLabel.width + armorCols * (squareSize + 1),
                2 * stepY - (squareSize + 1));
        fcsCritArea.translate(10 + pilotCritLabel.width + armorCols * (squareSize + 1), 3 * stepY - (squareSize + 1));
        sensorCritArea.translate(10 + pilotCritLabel.width + armorCols * (squareSize + 1),
                4 * stepY - (squareSize + 1));
        pilotCritArea.translate(10 + pilotCritLabel.width + armorCols * (squareSize + 1), 5 * stepY - (squareSize + 1));
    }

    private void drawCrits(Image im, int crits) {
        int w = im.getWidth(null);
        int h = im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < crits; i++) {
            g.setColor(Color.red.darker());
            g.fillRect(i * (squareSize + 1), 0, squareSize, squareSize);
        }
    }

    // Redraws armor images
    private void drawArmorImage(Image im, int a, int initial) {
        int w = im.getWidth(null);
        int h = im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.gray);
        g.fillRect(0, 0, w, h);
        // first fill up the initial armor area with black
        for (int i = 0; i < initial; i++) {
            // 6 across and 8 down
            int row = i / armorRows;
            int column = i - row * armorRows;
            g.setColor(Color.black);
            g.fillRect(row * (squareSize + 1), column * (squareSize + 1), (squareSize + 1), (squareSize + 1));
        }
        for (int i = 0; i < a; i++) {
            int row = i / armorRows;
            int column = i - row * armorRows;
            g.setColor(Color.green.darker());
            g.fillRect(row * (squareSize + 1), column * (squareSize + 1), squareSize, squareSize);
        }
    }

}
