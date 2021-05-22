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

import java.awt.Dimension;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
            UIUtil.scaleJPopup(ScalingPopup.this);
            pack();
        } 
        super.setVisible(b);
    }
    
    /** Returns a spacer (empty, small menu item) for a scaling popup menu. */
    public static JMenuItem spacer() {

        JMenuItem result = new JMenuItem() {
            private static final long serialVersionUID = 1249257644704746075L;

            @Override
            public Dimension getPreferredSize() {
                Dimension s = super.getPreferredSize();
                return new Dimension(s.width, UIUtil.scaleForGUI(8));
            }
        };
        result.setEnabled(false);
        return result;
    }

};