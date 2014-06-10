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

/*
 * GameOption.java
 *
 * Created on April 26, 2002, 10:50 AM
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
 * @version
 */
public interface IOption extends IBasicOption, IOptionInfo {

    /**
     * Booolean option type
     */
    public static final int BOOLEAN = 0;

    /**
     * Integer option type
     */
    public static final int INTEGER = 1;

    /**
     * Float option type
     */
    public static final int FLOAT = 2;

    /**
     * String option type
     */
    public static final int STRING = 3;

    /**
     * Choise option type
     */
    public static final int CHOICE = 4;

    /**
     * Returns this option container - GameOptions, PilotOptions etc
     * 
     * @return option container
     */
    public abstract IOptions getOwner();

    /**
     * Returns option type.
     * 
     * @return option type
     * @see IOption#BOOLEAN etc
     */
    public abstract int getType();

    /**
     * Returns default option value
     * 
     * @return default option value
     */
    public abstract Object getDefault();

    /**
     * Return the value as the <code>boolean</code>
     */
    public abstract boolean booleanValue();

    /**
     * Return the value as the <code>int</code>
     */
    public abstract int intValue();

    /**
     * Return the value as the <code>float</code>
     */
    public abstract float floatValue();

    /**
     * Return the value as the <code>String</code>
     */
    public abstract String stringValue();

    /**
     * Sets the value
     * 
     * @param value value to set
     */
    public abstract void setValue(Object value);

    /**
     * Sets the <code>String</code> value
     * 
     * @param value value to set
     */
    public abstract void setValue(String value);

    /**
     * Sets the <code>boolean</code> value
     * 
     * @param value value to set
     */
    public abstract void setValue(boolean value);

    /**
     * Sets the <code>int</code> value
     * 
     * @param value value to set
     */
    public abstract void setValue(int value);

    /**
     * Sets the <code>float</code> value
     * 
     * @param value value to set
     */
    public abstract void setValue(float value);

    /**
     * Resets the value to its "default" state
     */
    public abstract void clearValue();

}
