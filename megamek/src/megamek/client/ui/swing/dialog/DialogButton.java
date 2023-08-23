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
package megamek.client.ui.swing.dialog;

import java.awt.Dimension;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import megamek.client.ui.swing.util.UIUtil;

/** 
 * A JButton that has a minimum width which scales with the GUI scale.
 */
public class DialogButton extends JButton {
    
    private static final long serialVersionUID = 952919304556828345L;
    
    /** The minimum width this button will have at GUI scale == 1 */
    private final static int BUTTON_MIN_WIDTH = 95;
    
    public DialogButton(String text) {
        super(text);
    }
    
    public DialogButton(AbstractAction action) {
        super(action);
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension prefSize = super.getPreferredSize();
        prefSize.width = Math.max(prefSize.width, UIUtil.scaleForGUI(BUTTON_MIN_WIDTH));
        return prefSize;
    }
}
