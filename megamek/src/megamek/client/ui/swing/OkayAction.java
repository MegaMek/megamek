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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import megamek.client.ui.Messages;

/** 
 * An {@link javax.swing.AbstractAction Action} for 
 * getting an Okay/Done response from a button in a dialog  
 * Assigns the Messages.Okay text to the button. When this
 * action is activated, sends OkayAction.OKAY as the command
 * string.
 * 
 * @author SJuliez
 */
public class OkayAction extends AbstractAction {
    
    private static final long serialVersionUID = 1680850851585381148L;
    
    public final static String OKAY = "OkayAction.Okay";
    
    private final ActionListener owner;
    
    /** 
     * Constructs a new <code>AbstractAction</code> that forwards an
     * Okay/Done to myOwner. Assigns the Messages.Okay text to the button.
     */
    public OkayAction(ActionListener myOwner) {
        owner = myOwner;
        putValue(NAME, Messages.getString("Okay")); //$NON-NLS-N$
        putValue(ACTION_COMMAND_KEY, OKAY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
       owner.actionPerformed(e);
    }

}
