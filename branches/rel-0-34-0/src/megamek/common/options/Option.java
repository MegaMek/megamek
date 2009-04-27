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

package megamek.common.options;

import java.io.Serializable;
import java.util.Vector;

public class Option implements IOption, Serializable {
    private static final long serialVersionUID = 8310472250031962888L;
    private String name;
    private int type;
    private Object defaultValue;
    private Object value;
    private IOptions owner;

    private transient IOptionInfo info;

    public Option(IOptions owner, String name, String defaultValue) {
        this(owner, name, STRING, defaultValue);
    }

    public Option(IOptions owner, String name, boolean defaultValue) {
        this(owner, name, BOOLEAN, new Boolean(defaultValue));
    }

    public Option(IOptions owner, String name, int defaultValue) {
        this(owner, name, INTEGER, new Integer(defaultValue));
    }

    public Option(IOptions owner, String name, float defaultValue) {
        this(owner, name, FLOAT, new Float(defaultValue));
    }

    public Option(IOptions owner, String name, Vector<String> defaultValue) {
        this(owner, name, CHOICE, ""); //$NON-NLS-1$
    }

    public Option(IOptions owner, String name, int type, Object defaultValue) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        if (isValidValue(defaultValue)) {
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give wrong type of value for option type."); //$NON-NLS-1$
        }
    }

    public IOptions getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDisplayableNameWithValue() {
        updateInfo();
        return info.getDisplayableName()
                + (type == IOption.INTEGER ? " " + value : "");
    }

    public String getDisplayableName() {
        updateInfo();
        return info.getDisplayableName();
    }

    public String getDescription() {
        updateInfo();
        return info.getDescription();
    }

    public int getTextFieldLength() {
        updateInfo();
        return info.getTextFieldLength();
    }

    public boolean isLabelBeforeTextField() {
        updateInfo();
        return info.isLabelBeforeTextField();
    }

    public int getType() {
        return type;
    }

    public Object getDefault() {
        return defaultValue;
    }

    public Object getValue() {
        return value;
    }

    public boolean booleanValue() {
        if (type == INTEGER) {
            return (Integer) value > 0;
        }
        if (type == CHOICE || type == STRING) {
            if (value.equals("None") || value.equals("")) { //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
            return true;
        }
        return ((Boolean) value).booleanValue();
    }

    public int intValue() {
        return ((Integer) value).intValue();
    }

    public float floatValue() {
        return ((Float) value).floatValue();
    }

    public String stringValue() {
        return value.toString();
    }

    public void setValue(Object value) {
        if (isValidValue(value)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give wrong type of value for option type."); //$NON-NLS-1$
        }
    }

    public void setValue(String value) {
        if (type == STRING || type == CHOICE) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give String value to non-String option."); //$NON-NLS-1$
        }
    }

    public void setValue(boolean value) {
        if (type == BOOLEAN) {
            this.value = new Boolean(value);
        } else {
            throw new IllegalArgumentException(
                    "Tried to give boolean value to non-boolean option."); //$NON-NLS-1$
        }
    }

    public void setValue(int value) {
        if (type == INTEGER) {
            this.value = new Integer(value);
        } else {
            throw new IllegalArgumentException(
                    "Tried to give integer value to non-integer option."); //$NON-NLS-1$
        }
    }

    public void setValue(float value) {
        if (type == FLOAT) {
            this.value = new Float(value);
        } else {
            throw new IllegalArgumentException(
                    "Tried to give float value to non-float option."); //$NON-NLS-1$
        }
    }

    // Turns this option "off"
    public void clearValue() {
        switch (type) {
            case STRING:
            case CHOICE:
                setValue(""); //$NON-NLS-1$
                break;
            case BOOLEAN:
                setValue(false);
                break;
            case INTEGER:
                setValue(0);
                break;
            case FLOAT:
                setValue(0);
        }
    }

    private boolean isValidValue(Object object) {
        switch (type) {
            case STRING:
            case CHOICE:
                return object instanceof String;
            case BOOLEAN:
                return object instanceof Boolean;
            case INTEGER:
                return object instanceof Integer;
            case FLOAT:
                return object instanceof Float;
            default:
                return false;
        }
    }

    /**
     * Updates the displayable info about the option
     */
    private void updateInfo() {
        if (info == null) {
            info = owner.getOptionInfo(name);
        }
    }
}
