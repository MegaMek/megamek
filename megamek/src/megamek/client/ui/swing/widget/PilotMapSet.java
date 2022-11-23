/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Set of elements to represent pilot information in MechDisplay
 */
public class PilotMapSet implements DisplayMapSet {

    private static String STAR3 = "***";
    private static int N_ADV = 35;
    private JComponent comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMPicArea portraitArea;
    private PMSimpleLabel nameL, nickL, pilotL, gunneryL, gunneryLL, gunneryML, gunneryBL, toughBL, initBL, commandBL;
    private PMSimpleLabel pilotR, gunneryR, gunneryLR, gunneryMR, gunneryBR, toughBR, initBR, commandBR, hitsR;
    private PMSimpleLabel[] advantagesR;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<>();
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize"));
    private static final Font FONT_TITLE = new Font(MMConstants.FONT_SANS_SERIF, Font.ITALIC,
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize"));
    private int yCoord = 1;

    /**
     * This constructor have to be called only from addNotify() method
     */
    public PilotMapSet(JComponent c) {
        comp = c;
        setAreas();
        setBackGround();
    }

    // These two methods are used to vertically position new labels on the
    // display.
    private int getYCoord() {
        return (yCoord * 15) - 5;
    }

    private int getNewYCoord() {
        yCoord++;
        return getYCoord();
    }

    private void setAreas() {
        portraitArea = new PMPicArea(new BufferedImage(72, 72, BufferedImage.TYPE_BYTE_INDEXED));
        content.addArea(portraitArea);
        yCoord = 6;
        FontMetrics fm = comp.getFontMetrics(FONT_TITLE);
        nameL = createLabel(Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getYCoord());
        nameL.setColor(Color.yellow);
        content.addArea(nameL);

        fm = comp.getFontMetrics(FONT_VALUE);
        nickL = createLabel(Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getNewYCoord());
        content.addArea(nickL);

        hitsR = createLabel(STAR3, fm, 0, getNewYCoord());
        hitsR.setColor(Color.RED);
        content.addArea(hitsR);
        getNewYCoord();

        pilotL = createLabel(Messages.getString("PilotMapSet.pilotLAntiMech"), fm, 0, getNewYCoord());
        content.addArea(pilotL);
        pilotR = createLabel(STAR3, fm, pilotL.getSize().width + 5, getYCoord());
        content.addArea(pilotR);

        initBL = createLabel(Messages.getString("PilotMapSet.initBL"), fm, pilotL.getSize().width + 50, getYCoord());
        content.addArea(initBL);
        initBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 25, getYCoord());
        content.addArea(initBR);

        gunneryL = createLabel(Messages.getString("PilotMapSet.gunneryL"), fm, 0, getNewYCoord());
        content.addArea(gunneryL);
        gunneryR = createLabel(STAR3, fm, pilotL.getSize().width + 5, getYCoord());
        content.addArea(gunneryR);

        commandBL = createLabel(Messages.getString("PilotMapSet.commandBL"), fm, pilotL.getSize().width + 50,
                getYCoord());
        content.addArea(commandBL);
        commandBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 25, getYCoord());
        content.addArea(commandBR);

        gunneryLL = createLabel(Messages.getString("PilotMapSet.gunneryLL"), fm, 0, getYCoord());
        content.addArea(gunneryLL);
        gunneryLR = createLabel(STAR3, fm, pilotL.getSize().width + 25, getYCoord());
        content.addArea(gunneryLR);

        gunneryML = createLabel(Messages.getString("PilotMapSet.gunneryML"), fm, 0, getNewYCoord());
        content.addArea(gunneryML);
        gunneryMR = createLabel(STAR3, fm, pilotL.getSize().width + 25, getYCoord());
        content.addArea(gunneryMR);

        toughBL = createLabel(Messages.getString("PilotMapSet.toughBL"), fm, pilotL.getSize().width + 50, getYCoord());
        content.addArea(toughBL);
        toughBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 25, getYCoord());
        content.addArea(toughBR);

        gunneryBL = createLabel(Messages.getString("PilotMapSet.gunneryBL"), fm, 0, getNewYCoord());
        content.addArea(gunneryBL);
        gunneryBR = createLabel(STAR3, fm, pilotL.getSize().width + 25, getYCoord());
        content.addArea(gunneryBR);

        getNewYCoord();
        advantagesR = new PMSimpleLabel[N_ADV];
        for (int i = 0; i < advantagesR.length; i++) {
            advantagesR[i] = createLabel(Integer.valueOf(i).toString(), fm, 10, getNewYCoord());
            content.addArea(advantagesR[i]);
        }
        // DO NOT PLACE ANY MORE LABELS BELOW HERE. They will get
        // pushed off the bottom of the screen by the pilot advantage
        // labels. Why not just allocate the number of pilot advantage
        // labels required instead of a hard 24? Because we don't have
        // an entity at this point. Bleh.
    }

    /**
     * updates fields for the unit
     */
    @Override
    public void setEntity(Entity en) {
        setEntity(en, 0);
    }
    
    public void setEntity(Entity en, int slot) {
        if (en instanceof Infantry) {
            pilotL.setString(Messages.getString("PilotMapSet.pilotLAntiMech"));
        } else {
            pilotL.setString(Messages.getString("PilotMapSet.pilotL"));
        }
        if (en.getCrew().isMissing(slot)) {
            nameL.setString(Messages.getString("PilotMapSet.empty"));
            nickL.setString("");
            pilotL.setVisible(false);
            pilotR.setVisible(false);
            gunneryL.setVisible(false);
            gunneryR.setVisible(false);
            gunneryLL.setVisible(false);
            gunneryLR.setVisible(false);
            gunneryML.setVisible(false);
            gunneryMR.setVisible(false);
            gunneryBL.setVisible(false);
            gunneryBR.setVisible(false);
        } else {
            nameL.setString(en.getCrew().getName(slot));
            nickL.setString(en.getCrew().getNickname(slot));
            pilotR.setString(Integer.toString(en.getCrew().getPiloting(slot)));
            gunneryR.setString(Integer.toString(en.getCrew().getGunnery(slot)));
            pilotL.setVisible(true);
            pilotR.setVisible(true);

            portraitArea.setIdleImage(en.getCrew().getPortrait(slot).getImage());

            if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
                gunneryLR.setString(Integer.toString(en.getCrew().getGunneryL(slot)));
                gunneryMR.setString(Integer.toString(en.getCrew().getGunneryM(slot)));
                gunneryBR.setString(Integer.toString(en.getCrew().getGunneryB(slot)));
                gunneryL.setVisible(false);
                gunneryR.setVisible(false);
                gunneryLL.setVisible(true);
                gunneryLR.setVisible(true);
                gunneryML.setVisible(true);
                gunneryMR.setVisible(true);
                gunneryBL.setVisible(true);
                gunneryBR.setVisible(true);
            } else {
                gunneryLL.setVisible(false);
                gunneryLR.setVisible(false);
                gunneryML.setVisible(false);
                gunneryMR.setVisible(false);
                gunneryBL.setVisible(false);
                gunneryBR.setVisible(false);
                gunneryL.setVisible(true);
                gunneryR.setVisible(true);
            }
        }

        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_TOUGHNESS)
                && !en.getCrew().isMissing(slot)) {
            toughBR.setString(Integer.toString(en.getCrew().getToughness(slot)));
        } else {
            toughBL.setVisible(false);
            toughBR.setVisible(false);
        }
        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)
                && !en.getCrew().isMissing(slot)) {
            initBR.setString(Integer.toString(en.getCrew().getInitBonus()));
        } else {
            initBL.setVisible(false);
            initBR.setVisible(false);
        }
        if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)
                && !en.getCrew().isMissing(slot)) {
            commandBR.setString(Integer.toString(en.getCrew().getCommandBonus()));
        } else {
            commandBL.setVisible(false);
            commandBR.setVisible(false);
        }
        if (en.getCrew().isMissing(slot)) {
            hitsR.setString("");
        } else {
            hitsR.setString(en.getCrew().getStatusDesc(slot));
        }
        for (int i = 0; i < advantagesR.length; i++) {
            advantagesR[i].setString("");
        }
        int i = 0;
        for (Enumeration<IOptionGroup> advGroups = en.getCrew().getOptions().getGroups(); advGroups
                .hasMoreElements();) {
            if (i >= advantagesR.length - 1) {
                advantagesR[advantagesR.length - 1].setString(Messages.getString("PilotMapSet.more"));
                break;
            }
            IOptionGroup advGroup = advGroups.nextElement();
            if (en.getCrew().countOptions(advGroup.getKey()) > 0) {
                advantagesR[i++].setString(advGroup.getDisplayableName());
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    if (i >= advantagesR.length - 1) {
                        advantagesR[advantagesR.length - 1].setString("  " + Messages.getString("PilotMapSet.more"));
                        return;
                    }
                    IOption adv = advs.nextElement();
                    if ((adv != null) && adv.booleanValue()) {
                        advantagesR[i++].setString("  " + adv.getDisplayableNameWithValue());
                    }
                }
            }
        }
    }

    @Override
    public PMAreasGroup getContentGroup() {
        return content;
    }

    @Override
    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
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

    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y) {
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l;
    }

}