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

import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import megamek.client.ui.Messages;

/** 
 * An {@link javax.swing.AbstractAction Action} for 
 * canceling a dialog (closing without action) using 
 * setVisible(false). Will assign the Messages.Cancel 
 * text to the button.
 * 
 * @author SJuliez
 */
public class CancelAction extends AbstractAction {
    
    private static final long serialVersionUID = 1680850851585381148L;
    
    private final Window owner;
    
    /** 
     * Constructs a new <code>AbstractAction</code> that closes the Window
     * myOwner when called. Assigns the Messages.Cancel text to the button.
     */
    public CancelAction(Window myOwner) {
        owner = myOwner;
        putValue(NAME, Messages.getString("Cancel"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
       owner.setVisible(false);
    }

}
