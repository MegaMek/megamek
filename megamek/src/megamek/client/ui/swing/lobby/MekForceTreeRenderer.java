/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby;

import megamek.MMConstants;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;

/** A specialized renderer for the Mek Force tree. */
public class MekForceTreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -2002064111324279609L;
    private final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
            "unknown_unit.gif").toString();

    private ChatLounge lobby;
    private boolean isSelected;
    private Color selectionColor = Color.BLUE;
    private Entity entity;
    private Player localPlayer;
    private JTree tree;
    private int row;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        isSelected = sel;
        Game game = lobby.getClientgui().getClient().getGame();
        localPlayer = lobby.getClientgui().getClient().getLocalPlayer();
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
            if (lobby.isCompact()) {
                setText(LobbyMekCellFormatter.formatUnitCompact(entity, lobby, true));
            } else {
                setText(LobbyMekCellFormatter.formatUnitFull(entity, lobby, true));
            }
            int size = UIUtil.scaleForGUI(40);
            if (lobby.isCompact()) {
                size = size / 2;
            }
            boolean showAsUnknown = owner.isEnemyOf(localPlayer)
                    && game.getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
            if (showAsUnknown) {
                setIcon(getToolkit().getImage(UNKNOWN_UNIT), size - 5);
            } else {
                Camouflage camo = entity.getCamouflageOrElse(entity.getOwner().getCamouflage());
                Image image = lobby.getClientgui().getBoardView().getTilesetManager().loadPreviewImage(entity, camo, this);
                setIconTextGap(UIUtil.scaleForGUI(10));
                setIcon(image, size);
            }
        } else if (value instanceof Force) {
            entity = null;
            Font scaledFont = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));
            setFont(scaledFont);
            Force force = (Force) value;
            if (lobby.isCompact()) {
                setText(LobbyMekCellFormatter.formatForceCompact(force, lobby));
            } else {
                setText(LobbyMekCellFormatter.formatForceFull(force, lobby));
            }
            setIcon(null);
        }
        return this; 
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (entity == null) {
            return null;
        }
        Rectangle r = tree.getRowBounds(row);
        if (r != null && event.getPoint().x > r.getWidth() - UIUtil.scaleForGUI(50)) {
            return "<HTML>" + UnitToolTip.getEntityTipLobby(entity, localPlayer, lobby.mapSettings);
        }
        return null;
    }

    private void setIcon(Image image, int height) {
        if ((image.getHeight(null) > 0) && (image.getWidth(null) > 0)) {
            int width = height * image.getWidth(null) / image.getHeight(null);
            setIcon(new ImageIcon(ImageUtil.getScaledImage(image, width, height)));
        } else {
            LogManager.getLogger().error("Trying to resize a unit icon of height or width 0!");
            setIcon(null);
        }
    }

    MekForceTreeRenderer(ChatLounge cl) {
        lobby = cl;
        tree = lobby.mekForceTree;
    }
}
