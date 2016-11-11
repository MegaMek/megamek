/**
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.DirectoryItems;

/**
 * Set of elements to reperesent pilot information in MechDisplay
 */

public class PilotMapSet implements DisplayMapSet {

    private static String STAR3 = "***"; //$NON-NLS-1$
    private static int N_ADV = 35;
    private JComponent comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMPicArea portraitArea;
    private PMSimpleLabel nameL, nickL, pilotL, gunneryL, gunneryLL, gunneryML, gunneryBL, toughBL, initBL, commandBL;
    private PMSimpleLabel pilotR, gunneryR, gunneryLR, gunneryMR, gunneryBR, toughBR, initBR, commandBR, hitsR;
    private PMSimpleLabel[] advantagesR;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, //$NON-NLS-1$
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize"));
    private static final Font FONT_TITLE = new Font("SansSerif", Font.ITALIC, //$NON-NLS-1$
            GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize"));
    private int yCoord = 1;

    // keep track of portrait images
    private DirectoryItems portraits;

    /**
     * This constructor have to be called anly from addNotify() method
     */
    public PilotMapSet(JComponent c) {
        comp = c;
        try {
            portraits = new DirectoryItems(Configuration.portraitImagesDir(), "", //$NON-NLS-1$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            portraits = null;
        }
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
        nameL = createLabel(Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getYCoord()); //$NON-NLS-1$
        nameL.setColor(Color.yellow);
        content.addArea(nameL);

        fm = comp.getFontMetrics(FONT_VALUE);
        nickL = createLabel(Messages.getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(nickL);

        hitsR = createLabel(STAR3, fm, 0, getNewYCoord());
        hitsR.setColor(Color.RED);
        content.addArea(hitsR);
        getNewYCoord();

        pilotL = createLabel(Messages.getString("PilotMapSet.pilotLAntiMech"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(pilotL);
        pilotR = createLabel(STAR3, fm, pilotL.getSize().width + 5, getYCoord());
        content.addArea(pilotR);

        initBL = createLabel(Messages.getString("PilotMapSet.initBL"), fm, pilotL.getSize().width + 50, getYCoord()); //$NON-NLS-1$
        content.addArea(initBL);
        initBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 15, getYCoord());
        content.addArea(initBR);

        gunneryL = createLabel(Messages.getString("PilotMapSet.gunneryL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(gunneryL);
        gunneryR = createLabel(STAR3, fm, pilotL.getSize().width + 5, getYCoord());
        content.addArea(gunneryR);

        commandBL = createLabel(Messages.getString("PilotMapSet.commandBL"), fm, pilotL.getSize().width + 50, //$NON-NLS-1$
                getYCoord());
        content.addArea(commandBL);
        commandBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 15, getYCoord());
        content.addArea(commandBR);

        gunneryLL = createLabel(Messages.getString("PilotMapSet.gunneryLL"), fm, 0, getYCoord()); //$NON-NLS-1$
        content.addArea(gunneryLL);
        gunneryLR = createLabel(STAR3, fm, pilotL.getSize().width + 25, getYCoord());
        content.addArea(gunneryLR);

        gunneryML = createLabel(Messages.getString("PilotMapSet.gunneryML"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(gunneryML);
        gunneryMR = createLabel(STAR3, fm, pilotL.getSize().width + 25, getYCoord());
        content.addArea(gunneryMR);

        toughBL = createLabel(Messages.getString("PilotMapSet.toughBL"), fm, pilotL.getSize().width + 50, getYCoord()); //$NON-NLS-1$
        content.addArea(toughBL);
        toughBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 15, getYCoord());
        content.addArea(toughBR);

        gunneryBL = createLabel(Messages.getString("PilotMapSet.gunneryBL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(gunneryBL);
        gunneryBR = createLabel(STAR3, fm, pilotL.getSize().width + 25, getYCoord());
        content.addArea(gunneryBR);

        getNewYCoord();
        advantagesR = new PMSimpleLabel[N_ADV];
        for (int i = 0; i < advantagesR.length; i++) {
            advantagesR[i] = createLabel(new Integer(i).toString(), fm, 10, getNewYCoord());
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
    public void setEntity(Entity en) {

        if (en instanceof Infantry) {
            pilotL.setString(Messages.getString("PilotMapSet.pilotLAntiMech"));
        } else {
            pilotL.setString(Messages.getString("PilotMapSet.pilotL"));
        }
        nameL.setString(en.getCrew().getName());
        nickL.setString(en.getCrew().getNickname());
        pilotR.setString(Integer.toString(en.getCrew().getPiloting()));
        gunneryR.setString(Integer.toString(en.getCrew().getGunnery()));

        if (null != getPortrait(en.getCrew())) {
            portraitArea.setIdleImage(getPortrait(en.getCrew()));
        }

        if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            gunneryLR.setString(Integer.toString(en.getCrew().getGunneryL()));
            gunneryMR.setString(Integer.toString(en.getCrew().getGunneryM()));
            gunneryBR.setString(Integer.toString(en.getCrew().getGunneryB()));
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
        if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_TOUGHNESS)) {
            toughBR.setString(Integer.toString(en.getCrew().getToughness()));
        } else {
            toughBL.setVisible(false);
            toughBR.setVisible(false);
        }
        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            initBR.setString(Integer.toString(en.getCrew().getInitBonus()));
        } else {
            initBL.setVisible(false);
            initBR.setVisible(false);
        }
        if ((en.getGame() != null) && en.getGame().getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)) {
            commandBR.setString(Integer.toString(en.getCrew().getCommandBonus()));
        } else {
            commandBL.setVisible(false);
            commandBR.setVisible(false);
        }
        hitsR.setString(en.getCrew().getStatusDesc());
        for (int i = 0; i < advantagesR.length; i++) {
            advantagesR[i].setString(""); //$NON-NLS-1$
        }
        int i = 0;
        for (Enumeration<IOptionGroup> advGroups = en.getCrew().getOptions().getGroups(); advGroups
                .hasMoreElements();) {
            if (i >= (N_ADV - 1)) {
                advantagesR[i++].setString(Messages.getString("PilotMapSet.more"));
                break;
            }
            IOptionGroup advGroup = advGroups.nextElement();
            if (en.getCrew().countOptions(advGroup.getKey()) > 0) {
                advantagesR[i++].setString(advGroup.getDisplayableName());
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    if (i >= (N_ADV - 1)) {
                        advantagesR[i++].setString("  " + Messages.getString("PilotMapSet.more"));
                        break;
                    }
                    IOption adv = advs.nextElement();
                    if ((adv != null) && adv.booleanValue()) {
                        advantagesR[i++].setString("  " + adv.getDisplayableNameWithValue());
                    }
                }
            }
        }
    }

    public PMAreasGroup getContentGroup() {
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers() {
        return bgDrawers;
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

    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y) {
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l;
    }

    /**
     * Get the portrait for the given pilot.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value will
     *         be <code>null</code> if no portrait was selected or if there was
     *         an error loading it.
     */
    public Image getPortrait(Crew pilot) {

        String category = pilot.getPortraitCategory();
        String file = pilot.getPortraitFileName();

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == file) || (null == portraits)) {
            return null;
        }

        if (Crew.PORTRAIT_NONE.equals(file)) {
            file = "default.gif"; //$NON-NLS-1$
        }

        if (Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) portraits.getItem(category, file);
            if (null == portrait) {
                // the image could not be found so switch to default one
                category = "";
                file = "default.gif";
                portrait = (Image) portraits.getItem(category, file);
            }
            // make sure no images are longer than 72 pixels
            if (null != portrait) {
                portrait = portrait.getScaledInstance(-1, 72, Image.SCALE_DEFAULT);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return portrait;
    }

}