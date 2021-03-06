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

import java.io.File;

import javax.swing.filechooser.FileFilter;

/** 
 * A FileFilter for MegaMek Board files. Accepts files with 
 * the .board extension and directories.
 *  
 * @author Simon
 */
public class BoardFileFilter extends FileFilter {
    
    @Override
    public boolean accept(File dir) {
        return (dir.getName().endsWith(".board") || dir.isDirectory());
    }

    @Override
    public String getDescription() {
        return "*.board";
    }

}
