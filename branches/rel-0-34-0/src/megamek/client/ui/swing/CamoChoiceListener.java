/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;

/**
 * Helper class to handle camo selection. This class will update an
 * <code>JButton</code> to show the selection, update a <code>Player</code>
 * to use the selection, and (optionally) communicate the selection to the
 * server through a <code>Client</code>. <p/> Created on January 24, 2004
 *
 * @author James Damour
 * @version 1
 */
public class CamoChoiceListener implements ItemListener {

    /**
     * The camo button.
     */
    private final JButton butCamo;

    /**
     * The selection dialog.
     */
    private final CamoChoiceDialog dialog;

    /**
     * The <code>Player</code> whose camo selection is being updated.
     */
    private final Player localPlayer;

    /**
     * holds the chat lounge. This needs cleanup
     */
    private final ChatLounge chatLounge;

    /**
     * Create a new camo selection listener that alerts a server.
     *
     * @param camoDialog - the <code>CamoChoiceDialog</code> that is being
     *            listened to.
     * @param button - the <code>JButton</code> that gets updated.
     * @param background - the default background <code>Color</code> for the
     *            button when a camo image is selected.
     * @param player - the <code>int</code> ID of the player whose camo is
     *            updated.
     * @param sender - the <code>Client</code> that sends the update.
     */
    public CamoChoiceListener(CamoChoiceDialog camoDialog, JButton button,
            ChatLounge chat) {
        dialog = camoDialog;
        butCamo = button;
        localPlayer = null;
        chatLounge = chat;
    }

    /**
     * Create a new camo selection listener that does not alert a server.
     *
     * @param camoDialog - the <code>CamoChoiceDialog</code> that is being
     *            listened to.
     * @param button - the <code>JButton</code> that gets updated.
     * @param background - the default background <code>Color</code> for the
     *            button when a camo image is selected.
     * @param player - the <code>Player</code> whose camo is updated.
     */
    public CamoChoiceListener(CamoChoiceDialog camoDialog, JButton button,
            Player player) {
        dialog = camoDialog;
        butCamo = button;
        localPlayer = player;
        chatLounge = null;
    }

    /**
     * Update the camo button when the selection dialog tells us to. <p/>
     * Implements <code>ItemListener</code>.
     *
     * @param event - the <code>ItemEvent</code> of the camo selection.
     */
    public void itemStateChanged(ItemEvent event) {

        // Get the player that needs to be updated.
        Player player;
        if (chatLounge != null) {
            player = chatLounge.getPlayerListSelectedClient().getLocalPlayer();
        } else {
            player = localPlayer;
        }

        // Get the camo image, category, and name that was selected.
        Image image = (Image) event.getItem();
        String category = dialog.getCategory();
        String itemName = dialog.getItemName();

        // If the image is null, a color was selected instead.
        if (null == image) {
            for (int color = 0; color < Player.colorNames.length; color++) {
                if (Player.colorNames[color].equals(itemName)) {
                    BufferedImage tempImage = new BufferedImage(80, 72, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = tempImage.createGraphics();
                    graphics.setColor(PlayerColors.getColor(color));
                    graphics.fillRect(0, 0, 84, 72);
                    butCamo.setIcon(new ImageIcon(tempImage));
                    player.setColorIndex(color);
                    break;
                }
            }
            itemName = null;
        }

        // We need to copy the image to make it appear.
        else {
            // Update the butCamo's image.
            butCamo.setIcon(new ImageIcon(image));
        }

        // Update the local player's camo info.
        player.setCamoCategory(category);
        player.setCamoFileName(itemName);

        // Send a message to a server, if called for.
        if (chatLounge != null) {
            chatLounge.getPlayerListSelectedClient().sendPlayerInfo();
        }
    }
}
