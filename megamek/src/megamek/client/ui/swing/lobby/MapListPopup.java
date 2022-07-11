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
import java.io.File;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.ui.swing.util.ScalingPopup;
import org.apache.logging.log4j.core.util.FileUtils;

class MapListPopup {
    
    static final String MLP_BOARD = "BOARD";
    static final String MLP_SURPRISE = "SURPRISE";

    static ScalingPopup mapListPopup(List<String> boards, int numButtons, ActionListener listener, 
            ChatLounge lobby, boolean enableRotation) {
        
        if (boards.isEmpty()) {
            return new ScalingPopup();
        }
        
        boolean oneSelected = boards.size() == 1;

        ScalingPopup popup = new ScalingPopup();
        if (numButtons == 1) {
            String name = mapShortName(boards.get(0));
            JMenuItem mi = menuItem("Set \"" + name + "\" as Board 0", MLP_BOARD + ":" + 0 + ":" + boards.get(0) + ":" + false, true, listener);
            popup.add(mi);
            mi.setEnabled(oneSelected);
            if (enableRotation) {
                mi = menuItem("Set Rotated \"" + name + "\" as Board 0", MLP_BOARD + ":" + 0 + ":" + boards.get(0) + ":" + true, true, listener);
                popup.add(mi);
                mi.setEnabled(oneSelected);
            }
        } else {
            popup.add(singleBoardMenu(oneSelected, false, listener, numButtons, boards));

            if (enableRotation) {
                popup.add(singleBoardMenu(oneSelected, true, listener, numButtons, boards));
            }
        }
        
        popup.add(multiBoardRandomMenu(!oneSelected, listener, numButtons, boards));
        popup.add(multiBoardSurpriseMenu(!oneSelected, listener, numButtons, boards));
        return popup;
    }

    /** @return the short map name given the long path */
    private static String mapShortName(String mapPath) {
        // File is the most robust way to split
        File f = new File(mapPath);
        return f.getName();
    }

    /**
     * Returns the "set as board" submenu.
     */
    private static JMenu singleBoardMenu(boolean enabled, boolean rotated, ActionListener listener, 
            int numB, List<String> boards) {
        String name = mapShortName(boards.get(0));

        JMenu menu = new JMenu(!rotated ? "Set \"" + name + "\" as Board..." : " Set \"" + name + "\" as Board (rotated)...");
        menu.setEnabled(enabled);
        if (enabled) {
            for (int i = 0; i < numB; i++) {
                menu.add(menuItem("Board " + (i + 1), MLP_BOARD + ":" + i + ":" + boards.get(0) + ":" + rotated, enabled, listener));
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
            // Since it's not visible to the player, the random board can already be chosen here
            int rnd = (int) (Math.random() * boards.size());
            for (int i = 0; i < numB; i++) {
                menu.add(menuItem("Board " + (i + 1), MLP_BOARD + ":" + i + ":" + boards.get(rnd), enabled, listener));
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
            for (int i = 0; i < numB; i++) {
                menu.add(menuItem("Board " + (i + 1), MLP_SURPRISE + ":" + i + ":" 
                        + String.join("\n", boards), enabled, listener));
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

