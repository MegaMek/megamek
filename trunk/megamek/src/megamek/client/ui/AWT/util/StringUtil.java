/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client.util;

import java.util.Vector;

public class StringUtil {

    public static Vector splitString(String s, String divider) {
    	if (s == null || s.equals("")) {
    		return new Vector();
    	}
    	
    	Vector v = new Vector();
    	int oldIndex = 0;
		int newIndex = s.indexOf(divider);
    	
    	while (newIndex != -1) {
    		String sub = s.substring(oldIndex, newIndex);
    		v.add(sub);
    		oldIndex = newIndex + 1;
    		newIndex = s.indexOf(divider, oldIndex);
    	}
    	
    	if (oldIndex != s.length()) {
    		String sub = s.substring(oldIndex);
    		v.add(sub);    		
    	}

    	return v;
    }

}
