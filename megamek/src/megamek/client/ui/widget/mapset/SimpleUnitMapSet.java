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
import megamek.common.units.Entity;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * A plain armor diagram for units whose shape does not fit a drawn figure: a handheld weapon, a building, a gun
 * emplacement. It draws one labeled box per location, showing the armor or structure (a building's construction
 * factor) available there, so these units still get a diagram in the same framed style as the rest, kept simple
 * rather than trying to picture the unit. The locations differ from one unit to the next, so the boxes are laid out
 * from the unit each time it is shown rather than fixed in advance.
 */
public class SimpleUnitMapSet implements DisplayMapSet {

    private final JComponent jComponent;
    private final LocationSelectListener locationSelectListener;
    private final PMAreasGroup content = new PMAreasGroup();
    private final Vector<BackGroundDrawer> bgDrawers = new Vector<>();

    /** The boxes by location, so the ones with a critical hit can be striped. */
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[0];

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final Font FONT_LABEL = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorSmallFontSize());
    private static final Font FONT_VALUE = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN,
          GUIP.getUnitDisplayMekArmorLargeFontSize());

    // The layout, in the map set's own unscaled coordinates. The boxes stack in a single column.
    private static final int BOX_LEFT = 8;
    private static final int BOX_WIDTH = 108;
    private static final int BOX_HEIGHT = 26;
    private static final int BOX_GAP = 6;
    private static final int FIRST_BOX_TOP = 10;
    private static final int LABEL_OFFSET_X = 26;
    private static final int VALUE_OFFSET_X = 82;

    public SimpleUnitMapSet(JComponent jComponent, LocationSelectListener locationSelectListener) {
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

    @Override
    public void setEntity(Entity entity) {
        content.removeAll();
        areas = new PMSimplePolygonArea[entity.locations()];
        FontMetrics labelMetrics = jComponent.getFontMetrics(FONT_LABEL);
        FontMetrics valueMetrics = jComponent.getFontMetrics(FONT_VALUE);

        int row = 0;
        for (int location = 0; location < entity.locations(); location++) {
            int top = FIRST_BOX_TOP + row * (BOX_HEIGHT + BOX_GAP);
            Polygon box = new Polygon(
                  new int[] { BOX_LEFT, BOX_LEFT + BOX_WIDTH, BOX_LEFT + BOX_WIDTH, BOX_LEFT },
                  new int[] { top, top, top + BOX_HEIGHT, top + BOX_HEIGHT },
                  4);
            PMSimplePolygonArea area = new PMSimplePolygonArea(box, locationSelectListener, location);
            areas[location] = area;
            content.addArea(area);
            content.addArea(WidgetUtils.createLabel(entity.getLocationAbbr(location), labelMetrics, Color.black,
                  BOX_LEFT + LABEL_OFFSET_X, top + BOX_HEIGHT / 2));

            int originalArmor = Math.max(entity.getOArmor(location), 0);
            int armor = Math.max(entity.getArmor(location), 0);
            int originalInternal = Math.max(entity.getOInternal(location), 0);
            int internal = Math.max(entity.getInternal(location), 0);

            // Show the armor where the location has it, otherwise its structure (a building's construction factor).
            // A location with neither - a gun emplacement's guns - is left as a labeled box with no value.
            int shown;
            int original;
            if ((originalArmor > 0) || (armor > 0)) {
                shown = armor;
                original = originalArmor;
            } else if ((originalInternal > 0) || (internal > 0)) {
                shown = internal;
                original = originalInternal;
            } else {
                row++;
                continue;
            }

            PMValueLabel valueLabel = WidgetUtils.createValueLabel(BOX_LEFT + VALUE_OFFSET_X, top + BOX_HEIGHT / 2,
                  Integer.toString(shown), valueMetrics);
            content.addArea(valueLabel);
            // Color the box by how much is left when there is an original to measure against; a building's current
            // construction factor has no stored original here, so its box is left in the neutral default color.
            if (original > 0) {
                WidgetUtils.setAreaColor(area, valueLabel, (double) shown / original);
            }
            row++;
        }
    }

    @Override
    public void setCriticalLocations(Set<Integer> criticalLocations) {
        for (PMSimplePolygonArea area : areas) {
            if (area != null) {
                area.setCriticalHatch(false);
            }
        }
        for (int location : criticalLocations) {
            if ((location >= 0) && (location < areas.length) && (areas[location] != null)) {
                areas[location].setCriticalHatch(true);
            }
        }
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
