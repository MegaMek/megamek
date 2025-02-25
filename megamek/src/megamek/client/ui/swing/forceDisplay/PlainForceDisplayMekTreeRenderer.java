/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.forceDisplay;

import megamek.MMConstants;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.tileset.TilesetManager;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.force.Force;
import megamek.common.icons.Camouflage;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;

/** A specialized renderer for the Mek Force tree. */
public class PlainForceDisplayMekTreeRenderer extends DefaultTreeCellRenderer {
    private static final MMLogger logger = MMLogger.create(PlainForceDisplayMekTreeRenderer.class);

    private final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
            "unknown_unit.gif").toString();

    private boolean isSelected;
    private Color selectionColor = Color.BLUE;
    private JTree tree;
    private int row;
    private Entity entity;

    private final Player localPlayer;
    private final TilesetManager tilesetManager;
    private final Game game;

    public PlainForceDisplayMekTreeRenderer(Player localPlayer, TilesetManager tilesetManager, Game game) {
        this.localPlayer = localPlayer;
        this.tilesetManager = tilesetManager;
        this.game = game;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        isSelected = sel;
        selectionColor = UIManager.getColor("Tree.selectionBackground");
        setOpaque(true);

        if (isSelected) {
            setBackground(new Color(selectionColor.getRGB()));
        } else {
            setForeground(null);
            setBackground(null);
        }

        if (value instanceof Entity) {
            Font scaledFont = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
            setFont(scaledFont);
            entity = (Entity) value;
            this.row = row;
            Player owner = entity.getOwner();
            setText(ForceDisplayMekCellFormatter.formatUnitCompact(entity, game, localPlayer));
            int size = UIUtil.scaleForGUI(20);
            boolean showAsUnknown = owner.isEnemyOf(localPlayer)
                    && !EntityVisibilityUtils.detectedOrHasVisual(localPlayer, game, entity);
            if (showAsUnknown) {
                setIcon(getToolkit().getImage(UNKNOWN_UNIT), size - 5);
            } else {
                Camouflage camo = entity.getCamouflageOrElseOwners();
                Image image = tilesetManager.loadPreviewImage(entity, camo, false);
                setIconTextGap(UIUtil.scaleForGUI(10));
                setIcon(image, size);
            }
        } else if (value instanceof Force force) {
            entity = null;
            Font scaledFont = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));
            setFont(scaledFont);
            setText(ForceDisplayMekCellFormatter.formatForceCompact(force, game, localPlayer));
            setIcon(null);
        }
        return this;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (entity == null) {
            return null;
        }

        String txt = UnitToolTip.getEntityTipUnitDisplay(entity, localPlayer).toString();
        txt += PilotToolTip.getCrewAdvs(entity, true).toString();
        return UnitToolTip.wrapWithHTML(txt);
    }

    private void setIcon(Image image, int height) {
        if ((image.getHeight(null) > 0) && (image.getWidth(null) > 0)) {
            int width = height * image.getWidth(null) / image.getHeight(null);
            setIcon(new ImageIcon(ImageUtil.getScaledImage(image, width, height)));
        } else {
            logger.error("Trying to resize a unit icon of height or width 0!");
            setIcon(null);
        }
    }
}
