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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "gameoption")
@XmlAccessorType(XmlAccessType.NONE)
public class Option implements IOption, Serializable {
    private static final long serialVersionUID = 8310472250031962888L;
    @XmlElement(name = "optionname")
    private String name;
    private int type;
    private Object defaultValue;
    @XmlElement(name = "optionvalue")
    private Object value;
    private IOptions owner;

    private transient IOptionInfo info;

    public Option(IOptions owner, String name, String defaultValue) {
        this(owner, name, STRING, defaultValue);
    }

    public Option(IOptions owner, String name, boolean defaultValue) {
        this(owner, name, BOOLEAN, Boolean.valueOf(defaultValue));
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

    /**
     * Constructor that satisfies JAXB.
     */
    protected Option() {
    }

    @Override
    public IOptions getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayableNameWithValue() {
        updateInfo();
        return info.getDisplayableName()
                + (type == IOption.INTEGER ? " " + value : "");
    }

    @Override
    public String getDisplayableName() {
        updateInfo();
        return info.getDisplayableName();
    }

    @Override
    public String getDescription() {
        updateInfo();
        return info.getDescription();
    }

    @Override
    public int getTextFieldLength() {
        updateInfo();
        return info.getTextFieldLength();
    }

    @Override
    public boolean isLabelBeforeTextField() {
        updateInfo();
        return info.isLabelBeforeTextField();
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public Object getDefault() {
        return defaultValue;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
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
        return ((Boolean) value);
    }

    @Override
    public int intValue() {
        return ((Integer) value);
    }

    @Override
    public float floatValue() {
        return ((Float) value);
    }

    @Override
    public String stringValue() {
        return value.toString();
    }

    @Override
    public void setValue(Object value) {
        if (isValidValue(value)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give wrong type of value for option type."); //$NON-NLS-1$
        }
    }

    @Override
    public void setValue(String value) {
        if (type == STRING || type == CHOICE) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give String value to non-String option."); //$NON-NLS-1$
        }
    }

    @Override
    public void setValue(boolean value) {
        if (type == BOOLEAN) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give boolean value to non-boolean option."); //$NON-NLS-1$
        }
    }

    @Override
    public void setValue(int value) {
        if (type == INTEGER) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give integer value to non-integer option."); //$NON-NLS-1$
        }
    }

    @Override
    public void setValue(float value) {
        if (type == FLOAT) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(
                    "Tried to give float value to non-float option."); //$NON-NLS-1$
        }
    }

    // Turns this option "off"
    @Override
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
    
    @Override
    public String toString() {
        return "Option - " + getName() + ": " + getValue();
    }
}
