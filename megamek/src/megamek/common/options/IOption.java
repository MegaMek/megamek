/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.options;

/**
 * Interface of the settable option. The settable option is used for game
 * options which relate to game behavior and as well as pilot options which
 * relate to specific stats about a pilot, such as advantages. Game options are
 * the same across all clients. A settable option's primary purpose is to store
 * a value for the option. Its secondary purpose is to give the desired options
 * dialog enough data to allow the user to set the option.
 * 
 * @author Ben
 * @since April 26, 2002, 10:50 AM
 */
public interface IOption extends IBasicOption, IOptionInfo {

    /**
     * Boolean option type
     */
    int BOOLEAN = 0;

    /**
     * Integer option type
     */
    int INTEGER = 1;

    /**
     * Float option type
     */
    int FLOAT = 2;

    /**
     * String option type
     */
    int STRING = 3;

    /**
     * Choice option type
     */
    int CHOICE = 4;

    /**
     * Returns this option container - GameOptions, PilotOptions etc
     * 
     * @return option container
     */
    AbstractOptions getOwner();

    /**
     * Returns option type.
     * 
     * @return option type
     * @see IOption#BOOLEAN etc
     */
    int getType();

    /**
     * Returns default option value
     * 
     * @return default option value
     */
    Object getDefault();

    /**
     * Return the value as the <code>boolean</code>
     */
    boolean booleanValue();

    /**
     * Return the value as the <code>int</code>
     */
    int intValue();

    /**
     * Return the value as the <code>float</code>
     */
    float floatValue();

    /**
     * Return the value as the <code>String</code>
     */
    String stringValue();

    /**
     * Sets the value
     * 
     * @param value value to set
     */
    void setValue(Object value);

    /**
     * Sets the <code>String</code> value
     * 
     * @param value value to set
     */
    void setValue(String value);

    /**
     * Sets the <code>boolean</code> value
     * 
     * @param value value to set
     */
    void setValue(boolean value);

    /**
     * Sets the <code>int</code> value
     * 
     * @param value value to set
     */
    void setValue(int value);

    /**
     * Sets the <code>float</code> value
     * 
     * @param value value to set
     */
    void setValue(float value);

    /**
     * Resets the value to its "default" state
     */
    void clearValue();

}
