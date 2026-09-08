/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import java.awt.Image;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JComponent;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.WidgetUtils;
import megamek.client.ui.widget.picmap.LocationSelectListener;
import megamek.client.ui.widget.picmap.PMAreasGroup;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PMValueLabel;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * The armor diagram of an Advanced Building: the building seen from above, one hexagon per hex, laid out the way
 * the hexes sit on the board.
 *
 * <p>A building is hexes by levels, and a diagram can only show one level at a time, so the map set shows the
 * level it is told to ({@link #setLevel}) and each hexagon carries that hex's value at that level - its armor where
 * it has any, otherwise its construction factor - colored by how much of it is left. Clicking a hexagon selects
 * the location for that hex at the shown level. Once the building is placed the hexagons are labelled with their
 * board hex and laid out as the board has them, facing included; before that they are numbered in the building's
 * own order.</p>
 */
public class BuildingMapSet implements DisplayMapSet {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    /**
     * The hexagons are flat-topped and sit in vertical columns with every odd column dropped by half a hex, as
     * the board draws them. These are the map set's own unscaled coordinates; the panel scales the drawing.
     */
    private static final int HEX_WIDTH = 72;
    private static final int HEX_HEIGHT = (int) Math.round(HEX_WIDTH * Math.sqrt(3) / 2);
    private static final int COLUMN_STEP = HEX_WIDTH * 3 / 4;
    private static final int MARGIN = 10;
    /** The hex label sits above the value, both centred; these are their offsets from the hexagon's centre. */
    private static final int LABEL_OFFSET_Y = -8;
    private static final int VALUE_OFFSET_Y = 12;
    /** Names a hex of a building that is not on the board yet, by its number. */
    private static final String UNPLACED_HEX_PREFIX = "H";

    private final JComponent jComponent;
    private final LocationSelectListener locationSelectListener;
    private final PMAreasGroup content = new PMAreasGroup();
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();

    /** The hexagons by the location they currently stand for, so the ones with a critical hit can be striped. */
    private PMSimplePolygonArea[] areasByLocation = new PMSimplePolygonArea[0];
    /** The level the diagram shows; {@code 0} is the ground level. */
    private int level = 0;

    public BuildingMapSet(JComponent jComponent, @Nullable LocationSelectListener locationSelectListener) {
        this.jComponent = jComponent;
        this.locationSelectListener = locationSelectListener;
        setBackGround();
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
     * Chooses the level the diagram shows. Takes effect the next time the building is set on the map set.
     *
     * @param level the level to show, from {@code 0} for the ground level; a level the building does not have
     *              shows its top level
     */
    public void setLevel(int level) {
        this.level = Math.max(level, 0);
    }

    /** @return the level the diagram shows, from {@code 0} for the ground level */
    public int getLevel() {
        return level;
    }

    @Override
    public void setEntity(Entity entity) {
        content.removeAll();
        areasByLocation = new PMSimplePolygonArea[entity.locations()];
        if (!(entity instanceof AbstractBuildingEntity building)) {
            return;
        }
        int height = building.getInternalBuilding().getBuildingHeight();
        if (height <= 0) {
            return;
        }
        int shownLevel = Math.min(level, height - 1);
        List<Coords> hexes = hexLayout(building);
        // Draw relative to the top-left of the footprint, but shift by an even number of columns so that the
        // odd columns - the ones the board drops by half a hex - stay the same columns after the shift.
        int leftColumn = hexes.stream().mapToInt(Coords::getX).min().orElse(0);
        leftColumn -= Math.floorMod(leftColumn, 2);
        int topRow = hexes.stream().mapToInt(Coords::getY).min().orElse(0);

        FontMetrics labelMetrics = jComponent.getFontMetrics(FONT_LABEL);
        FontMetrics valueMetrics = jComponent.getFontMetrics(FONT_VALUE);
        for (int hexIndex = 0; hexIndex < hexes.size(); hexIndex++) {
            int location = (hexIndex * height) + shownLevel;
            if (location >= entity.locations()) {
                continue;
            }
            Coords hex = hexes.get(hexIndex);
            int column = hex.getX() - leftColumn;
            int row = hex.getY() - topRow;
            int centreX = MARGIN + (column * COLUMN_STEP) + (HEX_WIDTH / 2);
            int centreY = MARGIN + (row * HEX_HEIGHT) + ((column & 1) == 1 ? HEX_HEIGHT / 2 : 0)
                  + (HEX_HEIGHT / 2);

            PMSimplePolygonArea area = new PMSimplePolygonArea(hexagon(centreX, centreY), locationSelectListener,
                  location);
            areasByLocation[location] = area;
            content.addArea(area);
            String hexName = (building.getPosition() == null)
                  ? UNPLACED_HEX_PREFIX + (hexIndex + 1)
                  : hex.getBoardNum();
            content.addArea(WidgetUtils.createLabel(hexName, labelMetrics, Color.black,
                  centreX - (labelMetrics.stringWidth(hexName) / 2), centreY + LABEL_OFFSET_Y));

            int originalArmor = Math.max(entity.getOArmor(location), 0);
            int armor = Math.max(entity.getArmor(location), 0);
            int originalInternal = Math.max(entity.getOInternal(location), 0);
            int internal = Math.max(entity.getInternal(location), 0);
            // Show the armor where the hex has it, otherwise its construction factor, as the box diagram does.
            int shown;
            int original;
            if ((originalArmor > 0) || (armor > 0)) {
                shown = armor;
                original = originalArmor;
            } else {
                shown = internal;
                original = originalInternal;
            }
            PMValueLabel valueLabel = WidgetUtils.createValueLabel(centreX, centreY + VALUE_OFFSET_Y,
                  Integer.toString(shown), valueMetrics);
            content.addArea(valueLabel);
            if (original > 0) {
                WidgetUtils.setAreaColor(area, valueLabel, (double) shown / original);
            }
        }
    }

    @Override
    public void setCriticalLocations(Set<Integer> criticalLocations) {
        for (PMSimplePolygonArea area : areasByLocation) {
            if (area != null) {
                area.setCriticalHatch(false);
            }
        }
        for (int location : criticalLocations) {
            boolean isShown = (location >= 0) && (location < areasByLocation.length)
                  && (areasByLocation[location] != null);
            if (isShown) {
                areasByLocation[location].setCriticalHatch(true);
            }
        }
    }

    /**
     * The board hex of each of the building's hexes, in hex order. A placed building gives its real board hexes,
     * so the drawing matches the board, facing included; an unplaced one gives its relative layout as offset
     * coordinates, which draws the shape as declared, facing north.
     */
    private static List<Coords> hexLayout(AbstractBuildingEntity building) {
        int height = building.getInternalBuilding().getBuildingHeight();
        List<Coords> hexes = new ArrayList<>();
        for (int hexIndex = 0; hexIndex < building.getOriginalHexCount(); hexIndex++) {
            Coords placed = building.getLocationCoords(hexIndex * height);
            if (placed != null) {
                hexes.add(placed);
            } else {
                hexes.add(building.getInternalBuilding().getOriginalCoordsList().get(hexIndex).toOffset());
            }
        }
        return hexes;
    }

    /** A flat-topped hexagon, {@link #HEX_WIDTH} across, centred on the given point. */
    private static Polygon hexagon(int centreX, int centreY) {
        int halfWidth = HEX_WIDTH / 2;
        int quarterWidth = HEX_WIDTH / 4;
        int halfHeight = HEX_HEIGHT / 2;
        return new Polygon(
              new int[] { centreX - halfWidth, centreX - quarterWidth, centreX + quarterWidth, centreX + halfWidth,
                          centreX + quarterWidth, centreX - quarterWidth },
              new int[] { centreY, centreY - halfHeight, centreY - halfHeight, centreY, centreY + halfHeight,
                          centreY + halfHeight },
              6);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, jComponent);
        int tilingType = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_LEFT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));

        tilingType = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = jComponent.getToolkit()
              .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, jComponent);
        bgDrawers.addElement(new BackGroundDrawer(tile, tilingType));
    }
}
