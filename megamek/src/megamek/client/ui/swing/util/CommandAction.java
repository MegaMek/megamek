/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.util;

/**
 * Class used in processing KeyEvents in <code>MegamekController</code>.  A 
 * hotkey defined in <code>MegamekController</code> consists of a key and a 
 * command, the command is then linked to a CommandAction that carriers out the
 * desired action.
 * 
 * @author arlith
 *
 */
public abstract class CommandAction {
    
    /**
     * Used to add a condition onto this Action: the default behavior returns
     * true but can be overriden to check for certain conditions.  If this
     * method returns false, then the <code>MegaMekController</code> will not
     * consume the <code>KeyEvent</code>
     * @return
     */
    public boolean shouldPerformAction(){
        return true;
    }
    
    public abstract void performAction();
    
    /**
     * Returns true if <code>releaseAction</code> should be called when the 
     * bound key is released, else false.
     * @return
     */
    public boolean hasReleaseAction(){
        return false;
    }
    
    /**
     * Method that gets called when the bound key is released.  Defaults is to
     * do nothing.
     */
    public void releaseAction(){
        
    }
    
    

}
