/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
 *  
 * listener program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * listener program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */ 
package megamek.client.ui.swing.lobby;

import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.ui.swing.util.ScalingPopup;

class MapListPopup {

    static ScalingPopup mapListPopup(List<String> boards, List<MapPreviewButton> mapButtons,
            ActionListener listener, ChatLounge lobby) {
        
        if (boards.isEmpty()) {
            return new ScalingPopup();
        }
        
        boolean oneSelected = boards.size() == 1;

        ScalingPopup popup = new ScalingPopup();
        popup.add(singleBoardMenu(oneSelected, listener, mapButtons.size(), boards));
        popup.add(multiBoardRandomMenu(!oneSelected, listener, mapButtons.size(), boards));
        popup.add(multiBoardSurpriseMenu(!oneSelected, listener, mapButtons.size(), boards));
        return popup;
    }

    /**
     * Returns the "Load" submenu, allowing general embarking
     */
    private static JMenu singleBoardMenu(boolean enabled, ActionListener listener, 
            int numB, List<String> boards) {

        JMenu menu = new JMenu("Set as Board...");
        menu.setEnabled(enabled);
        if (enabled) {
            for (int i = 0; i < numB; i++) {
                menu.add(menuItem("Board " + i, "BOARD:" + i + ":" + boards.get(0), enabled, listener));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }

    /**
     * Returns the "Load" submenu, allowing general embarking
     */
    private static JMenu multiBoardRandomMenu(boolean enabled, ActionListener listener, 
            int numB, List<String> boards) {

        JMenu menu = new JMenu("Set Random Board...");
        menu.setEnabled(enabled);
        if (enabled) {
            // Since it's not visible to the player, the random board can be chose here
            int rnd = (int)(Math.random() * boards.size());
            for (int i = 0; i < numB; i++) {
                menu.add(menuItem("Board " + i, "BOARD:" + i + ":" + boards.get(rnd), enabled, listener));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }
    
    /**
     * Returns the "Load" submenu, allowing general embarking
     */
    private static JMenu multiBoardSurpriseMenu(boolean enabled, ActionListener listener, 
            int numB, List<String> boards) {

        JMenu menu = new JMenu("Set Surprise Board...");
        menu.setEnabled(enabled);
        if (enabled) {
            String boardList = "";
            for (String board: boards) {
                boardList += "\n" + board;
            }
            for (int i = 0; i < numB; i++) {
                menu.add(menuItem("Board " + i, "SURPRISE:" + i + ":" + boardList, enabled, listener));
            }
        }
        menu.setEnabled(enabled && (menu.getItemCount() > 0));
        return menu;
    }
    

    /**
     * Returns a single menu item with the given text, the given command string
     * cmd, the given enabled state, and assigned the given listener.
     */
    private static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener) {

        return menuItem(text, cmd, enabled, listener, Integer.MIN_VALUE);
    }

    /**
     * Returns a single menu item with the given text, the given command string
     * cmd, the given enabled state, and assigned the given listener. Also assigns
     * the given key mnemonic.
     */
    private static JMenuItem menuItem(String text, String cmd, boolean enabled, 
            ActionListener listener, int mnemonic) {

        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(cmd);
        result.addActionListener(listener);
        result.setEnabled(enabled);
        if (mnemonic != Integer.MIN_VALUE) {
            result.setMnemonic(mnemonic);
        }
        return result;
    }
}

