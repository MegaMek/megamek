/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.util.Iterator;

import megamek.client.ui.AWT.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.util.DirectoryItems;

/**
 * This class will list all of the camo files under "{@link Configuration#camoDir()}",
 * according to their categories.
 * 
 * @author James Damour
 */
public class ListCamoFiles {

    public static void main(String[] args) {

        try {
            DirectoryItems images = new DirectoryItems(
                    Configuration.camoDir(),
                    "", //$NON-NLS-1$
                    ImageFileFactory.getInstance()
            );
            Iterator<String> categories = images.getCategoryNames();
            Iterator<String> names = null;
            String catName = null;

            // Walk through the category names, listing all of the image files.
            while (categories.hasNext()) {

                // Get this category name, and replace null with blank.
                catName = categories.next();

                // Print the category.
                System.out.print("Printing files in ");
                if (catName.equals("")) {
                    System.out.print("-- General --");
                } else {
                    System.out.print(catName);
                }
                System.out.println(":");

                // Walk through the item names.
                names = images.getItemNames(catName);
                while (names.hasNext()) {
                    System.out.println(names.next());
                }
            } // Handle the next category.
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
