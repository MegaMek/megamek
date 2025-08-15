/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.forceDisplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.tooltip.PilotToolTip;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.tileset.EntityImage;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.EntityVisibilityUtils;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.icons.Camouflage;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/** A specialized renderer for the Mek Force tree. */
public class ForceDisplayMekTreeRenderer extends DefaultTreeCellRenderer {
    private static final MMLogger logger = MMLogger.create(ForceDisplayMekTreeRenderer.class);

    private final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
          "unknown_unit.gif").toString();

    private ClientGUI clientGUI;
    private Client client;
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
        localPlayer = client.getLocalPlayer();
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
            setText(ForceDisplayMekCellFormatter.formatUnitCompact(entity, client, row));
            int size = UIUtil.scaleForGUI(20);
            boolean showAsUnknown = owner.isEnemyOf(localPlayer)
                  && !EntityVisibilityUtils.detectedOrHasVisual(localPlayer, client.getGame(), entity);
            if (showAsUnknown) {
                setIcon(getToolkit().getImage(UNKNOWN_UNIT), size - 5);
            } else {
                Image image;
                Camouflage camo = entity.getCamouflageOrElseOwners();
                if (clientGUI != null) {
                    image = clientGUI.getTilesetManager().loadPreviewImage(entity, camo, false);
                } else {
                    Image base = MMStaticDirectoryManager.getMekTileset().imageFor(entity);
                    EntityImage entityImage = EntityImage.createIcon(base, camo, entity, false);
                    entityImage.loadFacings();
                    image = entityImage.getFacing(entity.getFacing());
                }
                setIconTextGap(UIUtil.scaleForGUI(10));
                setIcon(image, size);
            }
        } else if (value instanceof Force) {
            entity = null;
            Font scaledFont = new Font(MMConstants.FONT_DIALOG, Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));
            setFont(scaledFont);
            Force force = (Force) value;
            setText(ForceDisplayMekCellFormatter.formatForceCompact(force, client));
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

    ForceDisplayMekTreeRenderer(Client client, JTree tree) {
        this.clientGUI = null;
        this.client = client;
        this.tree = tree;
    }

    ForceDisplayMekTreeRenderer(ClientGUI clientGUI, JTree tree) {
        this(clientGUI.getClient(), tree);
        this.clientGUI = clientGUI;
    }
}
