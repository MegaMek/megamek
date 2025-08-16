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
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;
import javax.swing.JComponent;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.picmap.PMAreasGroup;
import megamek.client.ui.widget.picmap.PMPicArea;
import megamek.client.ui.widget.picmap.PMSimpleLabel;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PMValueLabel;
import megamek.common.Aero;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.options.OptionsConstants;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Class which keeps set of all areas required to represent Capital Fighter unit in MekDisplay.ArmorPanel class.
 */
public class CapitalFighterMapSet implements DisplayMapSet {

    private final JComponent jComponent;
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
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private final PMAreasGroup content = new PMAreasGroup();

    private final int squareSize = 7;
    private final int armorRows = 8;
    private final int armorCols = 6;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());

    public CapitalFighterMapSet(JComponent c) {
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
        armorImage = jComponent.createImage(armorCols * (squareSize + 1), armorRows * (squareSize + 1));
        armorArea = new PMPicArea(armorImage);

        avCritImage = jComponent.createImage(3 * (squareSize + 1), squareSize + 1);
        avCritArea = new PMPicArea(avCritImage);
        engineCritImage = jComponent.createImage(3 * (squareSize + 1), squareSize + 1);
        engineCritArea = new PMPicArea(engineCritImage);
        fcsCritImage = jComponent.createImage(3 * (squareSize + 1), squareSize + 1);
        fcsCritArea = new PMPicArea(fcsCritImage);
        sensorCritImage = jComponent.createImage(3 * (squareSize + 1), squareSize + 1);
        sensorCritArea = new PMPicArea(sensorCritImage);
        pilotCritImage = jComponent.createImage(6 * (squareSize + 1), squareSize + 1);
        pilotCritArea = new PMPicArea(pilotCritImage);
    }

    private void setLabels() {
        FontMetrics fm = jComponent.getFontMetrics(FONT_LABEL);
        armorLabel = new PMSimpleLabel("Armor:", fm, Color.white);
        armorVLabel = new PMValueLabel(fm, Color.red.brighter());

        avCritLabel = new PMSimpleLabel("Avionics:", fm, Color.white);
        engineCritLabel = new PMSimpleLabel("Engine:", fm, Color.white);
        fcsCritLabel = new PMSimpleLabel("FCS:", fm, Color.white);
        sensorCritLabel = new PMSimpleLabel("Sensors:", fm, Color.white);
        pilotCritLabel = new PMSimpleLabel("Pilot hits:", fm, Color.white);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, jComponent);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, b));
    }

    private void translateAreas() {
        armorLabel.translate(0, 0);
        armorArea.translate(0, squareSize);
        armorVLabel.translate((armorCols * (squareSize + 1)) / 2, squareSize + (armorRows * (squareSize + 1)) / 2);

        int stepY = 14;
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
