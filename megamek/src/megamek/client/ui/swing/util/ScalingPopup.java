/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/ 
package megamek.client.ui.swing.util;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/** 
 * A JPopupMenu that automatically scales with MegaMek's GUI Scaling value 
 * obtained from {@link megamek.client.ui.swing.GUIPreferences#getGUIScale()}
 * @author Juliez
 */
public class ScalingPopup extends JPopupMenu {

    private static final long serialVersionUID = 4466741063088667097L;

    @Override
    public void setVisible(boolean b) {
        if (b) {
            applyGUIScale();
        }
        super.setVisible(b);
    }

    /** 
     * Applies the GUI Scaling on AWT's event thread as demanded
     * by {@link java.awt.Container#getComponent(int)}. 
     */
    public void applyGUIScale() {
        SwingUtilities.invokeLater(guiScaler);
    }

    /** Scales the menu items etc. and calls pack() to refresh the popup size. */
    private Runnable guiScaler = new Runnable() {

        @Override
        public void run() {
            UIUtil.scaleJPopup(ScalingPopup.this);
            ScalingPopup.this.pack();
        }
    };

};