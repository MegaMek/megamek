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

package megamek.client.ui.swing;

import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import megamek.client.ui.swing.dialog.DialogButton;

/** 
 * A {@link javax.swing.JButton JButton} that will react
 * to ESC key presses when in a focused window. 
 * 
 * @author SJuliez
 */
public class ButtonEsc extends DialogButton {

    private static final long serialVersionUID = -1259826896841611521L;

    private static final String MYACTION = "ButtonEscAction";

    /** 
     * Constructs a {@link javax.swing.JButton JButton} that will react
     * to ESC key presses by calling the 
     * {@link javax.swing.AbstractAction Action}
     * <code>escAction</code> when the button is in a focused window
     */
    public ButtonEsc(AbstractAction escAction) {
        super(escAction);
        
        // Link ESC to the escAction
        getActionMap().put(MYACTION, escAction);
        InputMap imap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), MYACTION);
    }
}
