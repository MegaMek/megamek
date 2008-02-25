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

import java.util.Enumeration;

/**
 * Interface that represents the options container
 */
public interface IOptions {

    /**
     * Returns the <code>Enumeration</code> of the option groups in this
     * options container.
     * 
     * @return <code>Enumeration</code> of the <code>IOptionGroup</code>
     */
    public abstract Enumeration<IOptionGroup> getGroups();

    /**
     * Returns the <code>Enumeration</code> of the options in this options
     * container. The order of options is not specified.
     * 
     * @return <code>Enumeration</code> of the <code>IOption</code>
     */
    public abstract Enumeration<IOption> getOptions();

    /**
     * Returns the option by name or <code>null</code> if there is no such
     * option
     * 
     * @param name option name
     * @return the option or <code>null</code> if there is no such option
     */
    public abstract IOption getOption(String name);

    /**
     * Returns the UI specific data to allow the user to set the option
     * 
     * @param name option name
     * @return UI specific data
     * @see IOptionInfo
     */
    public abstract IOptionInfo getOptionInfo(String name);

    /**
     * Returns the value of the desired option as the <code>boolean</code>
     * 
     * @param name option name
     * @return the value of the desired option as the <code>boolean</code>
     */
    public abstract boolean booleanOption(String name);

    /**
     * Returns the value of the desired option as the <code>int</code>
     * 
     * @param name option name
     * @return the value of the desired option as the <code>int</code>
     */
    public abstract int intOption(String name);

    /**
     * Returns the value of the desired option as the <code>float</code>
     * 
     * @param name option name
     * @return the value of the desired option as the <code>float</code>
     */
    public abstract float floatOption(String name);

    /**
     * Returns the value of the desired option as the <code>String</code>
     * 
     * @param name option name
     * @return the value of the desired option as the <code>String</code>
     */
    public abstract String stringOption(String name);
}
