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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.MegaMek;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.force.*;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/** A specialized renderer for the Mek Force tree. */
public class MekForceTreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -2002064111324279609L;
    private final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
            "unknown_unit.gif").toString();

    private ChatLounge lobby;
    //    private final Color TRANSPARENT = new Color(250,250,250,0);
    private boolean isSelected;
    private Color selectionColor = Color.BLUE;
    private Entity entity;
    private IPlayer localPlayer;
    private JTree tree;
    private int row;

    static int counter = 0;

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
            Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
            setFont(scaledFont);
            entity = (Entity) value;
            this.row = row; 
            IPlayer owner = entity.getOwner();
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
                Image image = lobby.getClientgui().bv.getTilesetManager().loadPreviewImage(entity, camo, this);
                setIconTextGap(UIUtil.scaleForGUI(10));
                setIcon(image, size);
            }
        } else if (value instanceof Force) {
            entity = null;
            Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));
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
            return "<HTML>" + UnitToolTip.getEntityTipLobby(entity, localPlayer, lobby.mapSettings).toString();
        }
        return null;
    }

    private void setIcon(Image image, int height) {
        if ((image.getHeight(null) > 0) && (image.getWidth(null) > 0)) {
            int width = height * image.getWidth(null) / image.getHeight(null);
            setIcon(new ImageIcon(ImageUtil.getScaledImage(image, width, height)));
        } else {
            MegaMek.getLogger().error("Trying to resize a unit icon of height or width 0!");
            setIcon(null);
        }
    }

    MekForceTreeRenderer(ChatLounge cl) {
        lobby = cl;
        tree = lobby.mekForceTree;
    }
}
