/**
* MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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
import megamek.common.Entity;
import megamek.common.Pilot;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.util.DirectoryItems;

/**
* Set of elements to reperesent pilot information in MechDisplay
*/

public class PilotMapSet implements DisplayMapSet {

    private static final String IMAGE_DIR = "data/images/widgets";

    private static String STAR3 = "***"; //$NON-NLS-1$
    private JComponent comp;
    private PMAreasGroup content = new PMAreasGroup();
    private PMPicArea portraitArea;
    private PMSimpleLabel nameL, nickL, pilotL, gunneryL, gunneryLL, gunneryML, gunneryBL, toughBL, initBL, commandBL;
    private PMSimpleLabel pilotR, gunneryR, gunneryLR, gunneryMR, gunneryBR, toughBR, initBR, commandBR, hitsR;
    private PMSimpleLabel[] advantagesR;
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    private static final Font FONT_VALUE = new Font(
                "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$
    private static final Font FONT_TITLE = new Font(
           "SansSerif", Font.ITALIC, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$
    private int yCoord = 1;

    // keep track of portrait images
    private DirectoryItems portraits;

    /**
    * This constructor have to be called anly from addNotify() method
    */
    public PilotMapSet(JComponent c) {
        comp = c;
        try {
            portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
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
        return yCoord * 15 - 5;
    }

    private int getNewYCoord() {
        yCoord++;
        return getYCoord();
    }

    private void setAreas() {
        portraitArea = new PMPicArea(new BufferedImage(72,72,BufferedImage.TYPE_BYTE_INDEXED));
        content.addArea(portraitArea);
        yCoord = 6;
        FontMetrics fm = comp.getFontMetrics(FONT_TITLE);
        nameL = createLabel(Messages
                .getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getYCoord()); //$NON-NLS-1$
        nameL.setColor(Color.yellow);
        content.addArea(nameL);

        fm = comp.getFontMetrics(FONT_VALUE);
        nickL = createLabel(Messages
                .getString("GeneralInfoMapSet.LocOstLCT"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(nickL);

        hitsR = createLabel(STAR3, fm, 0, getNewYCoord());
        hitsR.setColor(Color.RED);
        content.addArea(hitsR);
        getNewYCoord();

        pilotL = createLabel(Messages.getString("PilotMapSet.pilotL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(pilotL);
        pilotR = createLabel(STAR3, fm, pilotL.getSize().width + 5, getYCoord());
        content.addArea(pilotR);

        initBL = createLabel(
                Messages.getString("PilotMapSet.initBL"), fm, pilotL.getSize().width + 50, getYCoord()); //$NON-NLS-1$
        content.addArea(initBL);
        initBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 15,
                getYCoord());
        content.addArea(initBR);

        gunneryL = createLabel(
                Messages.getString("PilotMapSet.gunneryL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(gunneryL);
        gunneryR = createLabel(STAR3, fm, pilotL.getSize().width + 5,
                    getYCoord());
        content.addArea(gunneryR);

        commandBL = createLabel(
                Messages.getString("PilotMapSet.commandBL"), fm, pilotL.getSize().width + 50, getYCoord()); //$NON-NLS-1$
        content.addArea(commandBL);
        commandBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 15,
                getYCoord());
        content.addArea(commandBR);
        
        gunneryLL = createLabel(
                    Messages.getString("PilotMapSet.gunneryLL"), fm, 0, getYCoord()); //$NON-NLS-1$
        content.addArea(gunneryLL);
        gunneryLR = createLabel(STAR3, fm, pilotL.getSize().width + 25,
                    getYCoord());
        content.addArea(gunneryLR);
  
        gunneryML = createLabel(
                    Messages.getString("PilotMapSet.gunneryML"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(gunneryML);
        gunneryMR = createLabel(STAR3, fm, pilotL.getSize().width + 25,
                    getYCoord());
        content.addArea(gunneryMR);
         
        toughBL = createLabel(
                Messages.getString("PilotMapSet.toughBL"), fm, pilotL.getSize().width + 50, getYCoord()); //$NON-NLS-1$
        content.addArea(toughBL);
        toughBR = createLabel(STAR3, fm, pilotL.getSize().width + 50 + initBL.getSize().width + 15,
                getYCoord());
        content.addArea(toughBR);

        gunneryBL = createLabel(
                    Messages.getString("PilotMapSet.gunneryBL"), fm, 0, getNewYCoord()); //$NON-NLS-1$
        content.addArea(gunneryBL);
        gunneryBR = createLabel(STAR3, fm, pilotL.getSize().width + 25,
                    getYCoord());
        content.addArea(gunneryBR);

        getNewYCoord();
        advantagesR = new PMSimpleLabel[24];
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

        nameL.setString(en.crew.getName());
        nickL.setString(en.crew.getNickname());
        pilotR.setString(Integer.toString(en.crew.getPiloting()));
        gunneryR.setString(Integer.toString(en.crew.getGunnery()));

        if(null != getPortrait(en.crew)) {
            portraitArea.setIdleImage(getPortrait(en.crew));
        }

        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption("rpg_gunnery")) {
            gunneryLR.setString(Integer.toString(en.crew.getGunneryL()));
            gunneryMR.setString(Integer.toString(en.crew.getGunneryM()));
            gunneryBR.setString(Integer.toString(en.crew.getGunneryB()));
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
        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption("toughness")) {
            toughBR.setString(Integer.toString(en.crew.getToughness()));
        } else {
            toughBL.setVisible(false);
            toughBR.setVisible(false);
        }
        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption("individual_initiative")) {
            initBR.setString(Integer.toString(en.crew.getInitBonus()));
        } else {
            initBL.setVisible(false);
            initBR.setVisible(false);
        }
        if ((en.getGame() != null)
                && en.getGame().getOptions().booleanOption("command_init")) {
            commandBR.setString(Integer.toString(en.crew.getCommandBonus()));
        } else {
            commandBL.setVisible(false);
            commandBR.setVisible(false);
        }
        hitsR.setString(en.crew.getStatusDesc());
        for (int i = 0; i < advantagesR.length; i++) {
            advantagesR[i].setString(""); //$NON-NLS-1$
        }
        int i = 0;
        for (Enumeration<IOptionGroup> advGroups = en.crew.getOptions().getGroups(); advGroups.hasMoreElements();) {
            IOptionGroup advGroup = advGroups.nextElement();
            if(en.crew.countOptions(advGroup.getKey()) > 0) {
                advantagesR[i++].setString(advGroup.getDisplayableName());
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    IOption adv = advs.nextElement();
                    if(adv.booleanValue()) {
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

    private PMSimpleLabel createLabel(String s, FontMetrics fm, int x, int y) {
        PMSimpleLabel l = new PMSimpleLabel(s, fm, Color.white);
        l.moveTo(x, y);
        return l;
    }

    /**
     * Get the portrait for the given pilot.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value
     *         will be <code>null</code> if no portrait was selected
     *          or if there was an error loading it.
     */
    public Image getPortrait(Pilot pilot) {

        String category = pilot.getPortraitCategory();
        String file = pilot.getPortraitFileName();

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == file)) {
            return null;
        }

        if (Pilot.PORTRAIT_NONE.equals(file)) {
            file = "default.gif"; //$NON-NLS-1$
        }

        if(Pilot.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) portraits.getItem(category, file);
            if(null == portrait) {
                //the image could not be found so switch to default one
                category = "";
                file = "default.gif";
                portrait = (Image) portraits.getItem(category, file);
            }
            //make sure no images are longer than 72 pixels
            if(null != portrait) {
                portrait = portrait.getScaledInstance(-1, 72, Image.SCALE_DEFAULT);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return portrait;
    }


}