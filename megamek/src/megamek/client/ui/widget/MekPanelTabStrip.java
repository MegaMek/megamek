/*
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.widget;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.io.Serial;
import java.util.Objects;

import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.widget.picmap.PMHotArea;
import megamek.client.ui.widget.picmap.PMPicPolygonalArea;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

public class MekPanelTabStrip extends PicMap {
    private static final MMLogger logger = MMLogger.create(MekPanelTabStrip.class);

    @Serial
    private static final long serialVersionUID = -1282343469769007184L;
    protected static final int NUM_TABS = 6;
    public static final String SUMMARY = "summary";
    public static final String PILOT = "pilot";
    public static final String ARMOR = "armor";
    public static final String WEAPONS = "weapons";
    public static final String SYSTEMS = "systems";
    public static final String EXTRAS = "extras";

    public static final int SUMMARY_INDEX = 0;
    public static final int PILOT_INDEX = 1;
    public static final int ARMOR_INDEX = 2;
    public static final int WEAPONS_INDEX = 3;
    public static final int SYSTEMS_INDEX = 4;
    public static final int EXTRAS_INDEX = 5;

    private static final Image[] idleImage = new Image[NUM_TABS];
    private static final Image[] activeImage = new Image[NUM_TABS];

    private final PMPicPolygonalArea[] tabs = new PMPicPolygonalArea[NUM_TABS];
    private Image idleCorner, selectedCorner;
    private int activeTab = 0;
    UnitDisplayPanel md;

    public MekPanelTabStrip(UnitDisplayPanel md) {
        super();
        this.md = md;
    }

    public void setTab(int i) {
        if (i > 5) {
            i = 5;
        }
        activeTab = i;
        redrawImages();
        update();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        setImages();
        setAreas();
        setListeners();
        update();
    }

    private void setImages() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();
        MediaTracker mt = new MediaTracker(this);
        Toolkit tk = getToolkit();
        idleImage[SUMMARY_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getGeneralTabIdle()).toString());
        idleImage[PILOT_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getPilotTabIdle()).toString());
        idleImage[ARMOR_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getArmorTabIdle()).toString());
        idleImage[SYSTEMS_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getSystemsTabIdle()).toString());
        idleImage[WEAPONS_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getWeaponsTabIdle()).toString());
        idleImage[EXTRAS_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getExtrasTabIdle()).toString());

        activeImage[SUMMARY_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getGeneralTabActive()).toString());
        activeImage[PILOT_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getPilotTabActive()).toString());
        activeImage[ARMOR_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getArmorTabActive()).toString());
        activeImage[SYSTEMS_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getSystemsTabActive()).toString());
        activeImage[WEAPONS_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getWeaponsTabActive()).toString());
        activeImage[EXTRAS_INDEX] = tk
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getExtraTabActive()).toString());
        idleCorner = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getCornerIdle()).toString());
        selectedCorner = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getCornerActive()).toString());

        // If we don't flush, we might have stale data
        idleCorner.flush();
        selectedCorner.flush();

        for (int i = 0; i < NUM_TABS; i++) {
            // If we don't flush, we might have stale data
            idleImage[i].flush();
            activeImage[i].flush();
            mt.addImage(idleImage[i], 0);
            mt.addImage(activeImage[i], 0);
        }
        mt.addImage(idleCorner, 0);
        mt.addImage(selectedCorner, 0);
        try {
            mt.waitForAll();
        } catch (Exception ex) {
            logger.error("", ex);
        }

        if (mt.isErrorID(0)) {
            logger.warn("Could not load image");
        }

        for (int i = 0; i < NUM_TABS; i++) {
            if (idleImage[i].getWidth(null) != activeImage[i].getWidth(null)) {
                logger.warn("idleImage and activeImage do not match widths for image {}", i);
            }
            if (idleImage[i].getHeight(null) != activeImage[i].getHeight(null)) {
                logger.warn("idleImage and activeImage do not match heights for image {}", i);
            }
        }
        if (idleCorner.getWidth(null) != selectedCorner.getWidth(null)) {
            logger.warn("idleCorner and selectedCorner do not match widths!");
        }
        if (idleCorner.getHeight(null) != selectedCorner.getHeight(null)) {
            logger.warn("idleCorner and selectedCorner do not match heights!");
        }
    }

    private void setAreas() {
        int cornerWidth = idleCorner.getWidth(null);

        for (int i = 0; i < idleImage.length; i++) {
            int width = idleImage[i].getWidth(null);
            int height = idleImage[i].getHeight(null);
            int[] pointsX = new int[] { 0, width, width + cornerWidth, 0 };
            int[] pointsY = new int[] { 0, 0, height, height };
            tabs[i] = new PMPicPolygonalArea(new Polygon(pointsX, pointsY, 4),
                  createImage(width, height));
        }

        int cumWidth = 0;
        for (int i = 0; i < idleImage.length; i++) {
            drawIdleImage(i);
            tabs[i].translate(cumWidth, 0);
            addElement(tabs[i]);
            cumWidth += idleImage[i].getWidth(null);
        }
    }

    private void setListeners() {
        tabs[SUMMARY_INDEX].addActionListener(e -> {
            if (Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                md.showPanel(SUMMARY);
            }
        });
        tabs[PILOT_INDEX].addActionListener(e -> {
            if (Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                md.showPanel(PILOT);
            }
        });
        tabs[ARMOR_INDEX].addActionListener(e -> {
            if (Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                md.showPanel(ARMOR);
            }
        });
        tabs[SYSTEMS_INDEX].addActionListener(e -> {
            if (Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                md.showPanel(SYSTEMS);
            }
        });
        tabs[WEAPONS_INDEX].addActionListener(e -> {
            if (Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                md.showPanel(WEAPONS);
            }
        });
        tabs[EXTRAS_INDEX].addActionListener(e -> {
            if (Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                md.showPanel(EXTRAS);
            }
        });
    }

    private void redrawImages() {
        for (int i = 0; i < NUM_TABS; i++) {
            drawIdleImage(i);
        }
    }

    private void drawIdleImage(int tab) {
        if (tabs[tab] == null) {
            // hmm, display not initialized yet...
            return;
        }
        Graphics g = tabs[tab].getIdleImage().getGraphics();

        if (activeTab == tab) {
            g.drawImage(activeImage[tab], 0, 0, null);
        } else {
            g.drawImage(idleImage[tab], 0, 0, null);
            if ((tab - activeTab) == 1) {
                g.drawImage(selectedCorner, 0, 4, null);
            } else if (tab > 0) {
                g.drawImage(idleCorner, 0, 4, null);
            }
        }
        g.dispose();
    }

    @Override
    public void onResize() {
        // ignore
    }
}
