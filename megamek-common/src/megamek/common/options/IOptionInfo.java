/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.options;

/**
 * Represents GUI data to allow the user to set the option
 */
public interface IOptionInfo {

    /**
     * Returns the user friendly name suitable for displaying in the options
     * editor dialogs etc.
     * 
     * @return displayable name
     */
    public abstract String getDisplayableName();

    /**
     * Returns the user friendly name suitable for displaying in cases where not
     * only the Name but also it value is required (like edge).
     * 
     * @return displayable name
     */
    public abstract String getDisplayableNameWithValue();

    /**
     * Return verbose description of the option suitable for context help, tip
     * etc.
     * 
     * @return option description
     */
    public abstract String getDescription();

    /**
     * returns the length of the textfield (how many characters).
     */
    public abstract int getTextFieldLength();

    /**
     * returns on which side of the text field the label is.
     */
    public abstract boolean isLabelBeforeTextField();

}
