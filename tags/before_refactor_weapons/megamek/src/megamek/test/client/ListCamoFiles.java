/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.test.client;

import megamek.common.Settings;
import megamek.client.util.ImageFileFactory;
import megamek.common.util.DirectoryItems;
import java.io.File;
import java.util.Enumeration;

/**
 * This class will list all of the camo files under "data/camo", according to
 * their categories.
 *
 * Created on January 18, 2004
 *
 * @author  James Damour
 * @version 1
 */
public class ListCamoFiles {

    public static void main( String[] args ) {

        try {
            Settings settings = Settings.getInstance();
            String rootDir = settings.get( "datadirectory", "data" );
            File camoLib = new File( rootDir + "/camo");
            DirectoryItems images = new DirectoryItems
                ( camoLib, "", ImageFileFactory.getInstance() );
            Enumeration categories = images.getCategoryNames();
            Enumeration names = null;
            String catName = null;

            // Walk through the category names, listing all of the image files.
            while ( categories.hasMoreElements() ) {

                // Get this category name, and replace null with blank.
                catName = (String) categories.nextElement();

                // Print the category.
                System.out.print( "Printing files in " );
                if ( catName.equals("") ) {
                    System.out.print( "-- General --" );
                } else {
                    System.out.print( catName );
                }
                System.out.println( ":" );

                // Walk through the item names.
                names = images.getItemNames( catName );
                while ( names.hasMoreElements() ) {
                    System.out.println( names.nextElement() );
                }
            } //Handle the next category.
        }
        catch ( Throwable err ) {
            err.printStackTrace();
        }

    }

}
