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

import megamek.MMConstants;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * Class which keeps set of all areas required to represent Capital Fighter unit
 * in MechDisplay.ArmorPanel class.
 */
public class SquadronMapSet implements DisplayMapSet {

    private JComponent comp;
    // Images that shows how much armor left.
    private Image[] armorImage;
    // Set of areas to show fighter armor left
    private PMPicArea[] armorArea;
    // Set of labels to show fighter armor left
    // images and areas for each crit tally
    private Image[] avCritImage;
    private PMPicArea[] avCritArea;
    private Image[] engineCritImage;
    private PMPicArea[] engineCritArea;
    private Image[] fcsCritImage;
    private PMPicArea[] fcsCritArea;
    private Image[] sensorCritImage;
    private PMPicArea[] sensorCritArea;
    private Image[] pilotCritImage;
    private PMPicArea[] pilotCritArea;
    private PMSimpleLabel[] nameLabel;
    private PMValueLabel[] armorVLabel;
    private PMSimpleLabel[] avCritLabel;
    private PMSimpleLabel[] engineCritLabel;
    private PMSimpleLabel[] fcsCritLabel;
    private PMSimpleLabel[] sensorCritLabel;
    private PMSimpleLabel[] pilotCritLabel;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private PMAreasGroup content = new PMAreasGroup();

    private int stepY = 11;
    private int squareSize = 7;
    private int armorRows = 6;
    private int armorCols = 8;

    private int max_size;

    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 9);

    public SquadronMapSet(JComponent c, Game g) {
        comp = c;

        /*
         * Set the max_size based on current game options
         */
        if ((g != null) && g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS)) {
            max_size = FighterSquadron.ALTERNATE_MAX_SIZE;
        } else {
            max_size = FighterSquadron.MAX_SIZE;
        }

        /*
         * Now set all our variables based upon that max_size
         */
        armorImage = new Image[max_size];
        armorArea = new PMPicArea[max_size];
        avCritImage = new Image[max_size];
        avCritArea = new PMPicArea[max_size];
        engineCritImage = new Image[max_size];
        engineCritArea = new PMPicArea[max_size];
        fcsCritImage = new Image[max_size];
        fcsCritArea = new PMPicArea[max_size];
        sensorCritImage = new Image[max_size];
        sensorCritArea = new PMPicArea[max_size];
        pilotCritImage = new Image[max_size];
        pilotCritArea = new PMPicArea[max_size];
        nameLabel = new PMSimpleLabel[max_size];
        armorVLabel = new PMValueLabel[max_size];
        avCritLabel = new PMSimpleLabel[max_size];
        engineCritLabel = new PMSimpleLabel[max_size];
        fcsCritLabel = new PMSimpleLabel[max_size];
        sensorCritLabel = new PMSimpleLabel[max_size];
        pilotCritLabel = new PMSimpleLabel[max_size];
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
        List<Entity> fighters = e.getSubEntities();
        for (int i = 0; i < max_size; ++ i) {
            if (i < fighters.size()) {
                final Entity fighter = fighters.get(i);
                IAero a = (IAero) fighter;
                int armor = a.getCapArmor();
                int armorO = a.getCap0Armor();
                armorVLabel[i].setValue(Integer.toString(armor));

                if (fighter.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                    armor = (int) Math.ceil(armor / 10.0);
                    armorO = (int) Math.ceil(armorO / 10.0);
                }

                drawArmorImage(armorImage[i], armor, armorO);
                drawCrits(avCritImage[i], a.getAvionicsHits());
                drawCrits(engineCritImage[i], fighter.getEngineHits());
                drawCrits(fcsCritImage[i], a.getFCSHits());
                drawCrits(sensorCritImage[i], a.getSensorHits());
                drawCrits(pilotCritImage[i], fighter.getCrew().getHits());

                nameLabel[i].setString(fighter.getDisplayName());

                armorArea[i].setVisible(true);
                armorVLabel[i].setVisible(true);
                avCritArea[i].setVisible(true);
                engineCritArea[i].setVisible(true);
                fcsCritArea[i].setVisible(true);
                sensorCritArea[i].setVisible(true);
                pilotCritArea[i].setVisible(true);
                nameLabel[i].setVisible(true);
                avCritLabel[i].setVisible(true);
                engineCritLabel[i].setVisible(true);
                fcsCritLabel[i].setVisible(true);
                sensorCritLabel[i].setVisible(true);
                pilotCritLabel[i].setVisible(true);
            } else {
                armorArea[i].setVisible(false);
                armorVLabel[i].setVisible(false);
                avCritArea[i].setVisible(false);
                engineCritArea[i].setVisible(false);
                fcsCritArea[i].setVisible(false);
                sensorCritArea[i].setVisible(false);
                pilotCritArea[i].setVisible(false);
                nameLabel[i].setVisible(false);
                avCritLabel[i].setVisible(false);
                engineCritLabel[i].setVisible(false);
                fcsCritLabel[i].setVisible(false);
                sensorCritLabel[i].setVisible(false);
                pilotCritLabel[i].setVisible(false);
            }
        }
    }

    private void setContent() {

        for (int i = 0; i < max_size; i++) {
            content.addArea(nameLabel[i]);
            content.addArea(armorArea[i]);
            content.addArea(armorVLabel[i]);
            content.addArea(avCritLabel[i]);
            content.addArea(engineCritLabel[i]);
            content.addArea(fcsCritLabel[i]);
            content.addArea(sensorCritLabel[i]);
            content.addArea(pilotCritLabel[i]);
            content.addArea(avCritArea[i]);
            content.addArea(engineCritArea[i]);
            content.addArea(fcsCritArea[i]);
            content.addArea(sensorCritArea[i]);
            content.addArea(pilotCritArea[i]);
        }
    }

    private void setAreas() {
        for (int i = 0; i < max_size; i++) {
            armorImage[i] = comp.createImage(armorCols * (squareSize + 1), armorRows * (squareSize + 1));
            armorArea[i] = new PMPicArea(armorImage[i]);

            avCritImage[i] = comp.createImage(3 * (squareSize + 1), squareSize + 1);
            avCritArea[i] = new PMPicArea(avCritImage[i]);
            engineCritImage[i] = comp.createImage(3 * (squareSize + 1), squareSize + 1);
            engineCritArea[i] = new PMPicArea(engineCritImage[i]);
            fcsCritImage[i] = comp.createImage(3 * (squareSize + 1), squareSize + 1);
            fcsCritArea[i] = new PMPicArea(fcsCritImage[i]);
            sensorCritImage[i] = comp.createImage(3 * (squareSize + 1), squareSize + 1);
            sensorCritArea[i] = new PMPicArea(sensorCritImage[i]);
            pilotCritImage[i] = comp.createImage(6 * (squareSize + 1), squareSize + 1);
            pilotCritArea[i] = new PMPicArea(pilotCritImage[i]);
        }
    }

    private void setLabels() {
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);
        for (int i = 0; i < max_size; i++) {
            nameLabel[i] = new PMSimpleLabel("Unknown", fm, Color.white);
            armorVLabel[i] = new PMValueLabel(fm, Color.red.brighter());
            avCritLabel[i] = new PMSimpleLabel("Avionics:", fm, Color.white);
            engineCritLabel[i] = new PMSimpleLabel("Engine:", fm, Color.white);
            fcsCritLabel[i] = new PMSimpleLabel("FCS:", fm, Color.white);
            sensorCritLabel[i] = new PMSimpleLabel("Sensors:", fm, Color.white);
            pilotCritLabel[i] = new PMSimpleLabel("Pilot hits:", fm, Color.white);
        }
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = comp.getToolkit()
                .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM | BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit()
                .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM | BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit()
                .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

    private void translateAreas() {
        // get size of each fighter block
        int blockSize = 6 * stepY;
        for (int i = 0; i < max_size; i++) {
            nameLabel[i].translate(0, blockSize * i);
            armorArea[i].translate(0, squareSize + (blockSize * i));
            armorVLabel[i].translate((armorCols * (squareSize + 1)) / 2,
                    (blockSize * i) + squareSize + ((armorRows * (squareSize + 1)) / 2));

            avCritLabel[i].translate(5 + (armorCols * (squareSize + 1)), stepY + (blockSize * i));
            engineCritLabel[i].translate(5 + (armorCols * (squareSize + 1)), (2 * stepY) + (blockSize * i));
            fcsCritLabel[i].translate(5 + (armorCols * (squareSize + 1)), (3 * stepY) + (blockSize * i));
            sensorCritLabel[i].translate(5 + (armorCols * (squareSize + 1)), (4 * stepY) + (blockSize * i));
            pilotCritLabel[i].translate(5 + (armorCols * (squareSize + 1)), (5 * stepY) + (blockSize * i));

            avCritArea[i].translate(10 + pilotCritLabel[0].width + (armorCols * (squareSize + 1)),
                    (stepY - (squareSize + 1)) + (blockSize * i));
            engineCritArea[i].translate(10 + pilotCritLabel[0].width + (armorCols * (squareSize + 1)),
                    ((2 * stepY) - (squareSize + 1)) + (blockSize * i));
            fcsCritArea[i].translate(10 + pilotCritLabel[0].width + (armorCols * (squareSize + 1)),
                    ((3 * stepY) - (squareSize + 1)) + (blockSize * i));
            sensorCritArea[i].translate(10 + pilotCritLabel[0].width + (armorCols * (squareSize + 1)),
                    ((4 * stepY) - (squareSize + 1)) + (blockSize * i));
            pilotCritArea[i].translate(10 + pilotCritLabel[0].width + (armorCols * (squareSize + 1)),
                    ((5 * stepY) - (squareSize + 1)) + (blockSize * i));
        }
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
            int column = i - (row * armorRows);
            g.setColor(Color.black);
            g.fillRect(row * (squareSize + 1), column * (squareSize + 1), (squareSize + 1), (squareSize + 1));
        }
        for (int i = 0; i < a; i++) {
            int row = i / armorRows;
            int column = i - (row * armorRows);
            g.setColor(Color.green.darker());
            g.fillRect(row * (squareSize + 1), column * (squareSize + 1), squareSize, squareSize);
        }
    }

}
