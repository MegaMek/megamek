/*
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import java.util.Objects;

import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.widget.picmap.PMHotArea;
import megamek.client.ui.widget.picmap.PMPicPolygonalArea;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/**
 * The strip of tabs across the top of the unit display.
 * <p>
 * The strip is built from a list of {@link TabDescriptor}s, one per tab, so the same class draws the classic six-tab
 * strip and the three-tab control-layout strip. Each descriptor names the card it opens and the idle and active
 * images that draw it; the images are loaded per strip, never shared between strips.
 */
public class MekPanelTabStrip extends PicMap {
    private static final MMLogger logger = MMLogger.create(MekPanelTabStrip.class);

    @Serial
    private static final long serialVersionUID = -1282343469769007184L;

    public static final String SUMMARY = "summary";
    public static final String PILOT = "pilot";
    public static final String ARMOR = "armor";
    public static final String WEAPONS = "weapons";
    public static final String SYSTEMS = "systems";
    public static final String EXTRAS = "extras";
    public static final String CONTROL = "control";

    /** Positions of the tabs in the classic six-tab strip; see {@link #classicTabs}. */
    public static final int SUMMARY_INDEX = 0;
    public static final int PILOT_INDEX = 1;
    public static final int ARMOR_INDEX = 2;
    public static final int WEAPONS_INDEX = 3;
    public static final int SYSTEMS_INDEX = 4;
    public static final int EXTRAS_INDEX = 5;

    /** The corner images overlap the tab to their left by this much, drawn this far down the strip. */
    private static final int CORNER_TOP = 4;

    /**
     * One tab of the strip.
     *
     * @param cardName    the name of the card the tab opens, as given to {@link UnitDisplayPanel#showPanel(String)}
     * @param idleImage   the file name of the image drawn while the tab is not selected, under the widgets directory
     * @param activeImage the file name of the image drawn while the tab is selected
     */
    public record TabDescriptor(String cardName, String idleImage, String activeImage) {
        public TabDescriptor {
            Objects.requireNonNull(cardName, "cardName");
            Objects.requireNonNull(idleImage, "idleImage");
            Objects.requireNonNull(activeImage, "activeImage");
        }
    }

    /**
     * @param skin the unit display skin the images come from
     *
     * @return the six tabs of the classic layout, in {@link #SUMMARY_INDEX} … {@link #EXTRAS_INDEX} order
     */
    public static List<TabDescriptor> classicTabs(UnitDisplaySkinSpecification skin) {
        return List.of(
              new TabDescriptor(SUMMARY, skin.getGeneralTabIdle(), skin.getGeneralTabActive()),
              new TabDescriptor(PILOT, skin.getPilotTabIdle(), skin.getPilotTabActive()),
              new TabDescriptor(ARMOR, skin.getArmorTabIdle(), skin.getArmorTabActive()),
              new TabDescriptor(WEAPONS, skin.getWeaponsTabIdle(), skin.getWeaponsTabActive()),
              new TabDescriptor(SYSTEMS, skin.getSystemsTabIdle(), skin.getSystemsTabActive()),
              new TabDescriptor(EXTRAS, skin.getExtrasTabIdle(), skin.getExtraTabActive()));
    }

    /**
     * @param skin the unit display skin the images come from
     *
     * @return the three tabs of the control layout: General, Weapon, Control
     */
    public static List<TabDescriptor> controlTabs(UnitDisplaySkinSpecification skin) {
        return List.of(
              new TabDescriptor(SUMMARY, skin.getGeneralTabIdle(), skin.getGeneralTabActive()),
              new TabDescriptor(WEAPONS, skin.getWeaponsTabIdle(), skin.getWeaponsTabActive()),
              new TabDescriptor(CONTROL, skin.getControlTabIdle(), skin.getControlTabActive()));
    }

    private final List<TabDescriptor> descriptors;
    private final Image[] idleImages;
    private final Image[] activeImages;
    private final PMPicPolygonalArea[] tabs;
    private Image idleCorner;
    private Image selectedCorner;
    private int activeTab = 0;
    private final UnitDisplayPanel unitDisplayPanel;

    /**
     * Creates a strip with the given tabs. The images are loaded when the strip is added to a window.
     *
     * @param unitDisplayPanel the display whose card a clicked tab opens
     * @param descriptors      the tabs, left to right; at least one
     */
    public MekPanelTabStrip(UnitDisplayPanel unitDisplayPanel, List<TabDescriptor> descriptors) {
        super();
        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException("A tab strip needs at least one tab");
        }
        this.unitDisplayPanel = unitDisplayPanel;
        this.descriptors = List.copyOf(descriptors);
        idleImages = new Image[descriptors.size()];
        activeImages = new Image[descriptors.size()];
        tabs = new PMPicPolygonalArea[descriptors.size()];
    }

    /**
     * @return the number of tabs in the strip
     */
    public int getTabCount() {
        return descriptors.size();
    }

    /**
     * @param cardName the name of a card
     *
     * @return the position of the tab that opens the card, or -1 if no tab does
     */
    public int indexOf(String cardName) {
        for (int index = 0; index < descriptors.size(); index++) {
            if (descriptors.get(index).cardName().equals(cardName)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * @param index the position of a tab
     *
     * @return the name of the card the tab at that position opens
     */
    public String getCardName(int index) {
        return descriptors.get(index).cardName();
    }

    /**
     * @return the position of the selected tab
     */
    public int getActiveTab() {
        return activeTab;
    }

    /**
     * Selects the tab at the given position, clamped to the strip.
     *
     * @param index the position of the tab to select
     */
    public void setTab(int index) {
        activeTab = Math.clamp(index, 0, descriptors.size() - 1);
        redrawImages();
        update();
    }

    /**
     * Selects the tab that opens the given card. A card no tab opens leaves the selection alone.
     *
     * @param cardName the name of the card
     *
     * @return {@code true} if a tab opens the card
     */
    public boolean setTab(String cardName) {
        int index = indexOf(cardName);
        if (index == -1) {
            return false;
        }
        setTab(index);
        return true;
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
        MediaTracker tracker = new MediaTracker(this);
        Toolkit toolkit = getToolkit();
        for (int index = 0; index < descriptors.size(); index++) {
            idleImages[index] = loadWidgetImage(toolkit, descriptors.get(index).idleImage());
            activeImages[index] = loadWidgetImage(toolkit, descriptors.get(index).activeImage());
        }
        idleCorner = loadWidgetImage(toolkit, udSpec.getCornerIdle());
        selectedCorner = loadWidgetImage(toolkit, udSpec.getCornerActive());

        // If we don't flush, we might have stale data
        idleCorner.flush();
        selectedCorner.flush();

        for (int index = 0; index < descriptors.size(); index++) {
            // If we don't flush, we might have stale data
            idleImages[index].flush();
            activeImages[index].flush();
            tracker.addImage(idleImages[index], 0);
            tracker.addImage(activeImages[index], 0);
        }
        tracker.addImage(idleCorner, 0);
        tracker.addImage(selectedCorner, 0);
        try {
            tracker.waitForAll();
        } catch (Exception ex) {
            logger.error("", ex);
        }

        if (tracker.isErrorID(0)) {
            logger.warn("Could not load image");
        }

        for (int index = 0; index < descriptors.size(); index++) {
            if (idleImages[index].getWidth(null) != activeImages[index].getWidth(null)) {
                logger.warn("idleImage and activeImage do not match widths for tab {}",
                      descriptors.get(index).cardName());
            }
            if (idleImages[index].getHeight(null) != activeImages[index].getHeight(null)) {
                logger.warn("idleImage and activeImage do not match heights for tab {}",
                      descriptors.get(index).cardName());
            }
        }
        if (idleCorner.getWidth(null) != selectedCorner.getWidth(null)) {
            logger.warn("idleCorner and selectedCorner do not match widths!");
        }
        if (idleCorner.getHeight(null) != selectedCorner.getHeight(null)) {
            logger.warn("idleCorner and selectedCorner do not match heights!");
        }
    }

    private static Image loadWidgetImage(Toolkit toolkit, String fileName) {
        return toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), fileName).toString());
    }

    private void setAreas() {
        int cornerWidth = idleCorner.getWidth(null);

        for (int index = 0; index < descriptors.size(); index++) {
            int width = idleImages[index].getWidth(null);
            int height = idleImages[index].getHeight(null);
            int[] pointsX = new int[] { 0, width, width + cornerWidth, 0 };
            int[] pointsY = new int[] { 0, 0, height, height };
            tabs[index] = new PMPicPolygonalArea(new Polygon(pointsX, pointsY, 4), createImage(width, height));
        }

        int cumulativeWidth = 0;
        for (int index = 0; index < descriptors.size(); index++) {
            drawIdleImage(index);
            tabs[index].translate(cumulativeWidth, 0);
            addElement(tabs[index]);
            cumulativeWidth += idleImages[index].getWidth(null);
        }
    }

    private void setListeners() {
        for (int index = 0; index < descriptors.size(); index++) {
            final String cardName = descriptors.get(index).cardName();
            tabs[index].addActionListener(event -> {
                if (Objects.equals(event.getActionCommand(), PMHotArea.MOUSE_DOWN)) {
                    unitDisplayPanel.showPanel(cardName);
                }
            });
        }
    }

    private void redrawImages() {
        for (int index = 0; index < descriptors.size(); index++) {
            drawIdleImage(index);
        }
    }

    private void drawIdleImage(int tab) {
        if (tabs[tab] == null) {
            // hmm, display not initialized yet...
            return;
        }
        Graphics graphics = tabs[tab].getIdleImage().getGraphics();

        if (activeTab == tab) {
            graphics.drawImage(activeImages[tab], 0, 0, null);
        } else {
            graphics.drawImage(idleImages[tab], 0, 0, null);
            if ((tab - activeTab) == 1) {
                graphics.drawImage(selectedCorner, 0, CORNER_TOP, null);
            } else if (tab > 0) {
                graphics.drawImage(idleCorner, 0, CORNER_TOP, null);
            }
        }
        graphics.dispose();
    }

    @Override
    public void onResize() {
        // ignore
    }
}
