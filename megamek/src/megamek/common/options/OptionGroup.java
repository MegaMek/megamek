/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * OptionGroup.java
 *
 * Created on April 26, 2002, 11:26 AM
 */

package megamek.common.options;

import java.io.*;
import java.util.*;

/**
 * Groups options together, of course.
 *
 * @author  Ben
 * @version 
 */
public class OptionGroup implements Serializable {
    
    private Vector options = new Vector();
    
    private String name;

    /** Creates new OptionGroup */
    public OptionGroup(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public Enumeration options() {
        return options.elements();
    }
    
    public void addOption(GameOption option) {
        options.addElement(option);
    }

}
