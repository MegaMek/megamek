/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.Serial;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.tileset.EntityImage;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/** A specialized renderer for the Mek Force tree. */
public class MekForceTreeRenderer extends DefaultTreeCellRenderer {
    private static final MMLogger logger = MMLogger.create(MekForceTreeRenderer.class);

    @Serial
    private static final long serialVersionUID = -2002064111324279609L;
    private final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
          "unknown_unit.gif").toString();

    private final ChatLounge lobby;
    private Entity entity;
    private Player localPlayer;
    private final JTree tree;
    private int row;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
          boolean leaf, int row, boolean hasFocus) {

        Game game = lobby.getClientGUI().getClient().getGame();
        localPlayer = lobby.getClientGUI().getClient().getLocalPlayer();
        Color selectionColor = UIManager.getColor("Tree.selectionBackground");
        setOpaque(true);

        if (sel) {
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
                final Camouflage camouflage = entity.getCamouflageOrElseOwners();
                final Image base = MMStaticDirectoryManager.getMekTileset().imageFor(entity);
                final Image image = new EntityImage(base, camouflage, this, entity).loadPreviewImage(true);
                setIconTextGap(UIUtil.scaleForGUI(10));
                setIcon(image, size);
            }
        } else if (value instanceof Force force) {
            entity = null;
            Font scaledFont = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));
            setFont(scaledFont);
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
            logger.debug("Trying to resize a unit icon of height or width 0!");
            setIcon(null);
        }
    }

    MekForceTreeRenderer(ChatLounge cl) {
        lobby = cl;
        tree = lobby.mekForceTree;
    }
}
