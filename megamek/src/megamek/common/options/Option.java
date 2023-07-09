/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org)
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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.Vector;

@XmlRootElement(name = "gameoption")
@XmlAccessorType(value = XmlAccessType.NONE)
public class Option implements IOption, Serializable {
    private static final long serialVersionUID = 8310472250031962888L;
    @XmlElement(name = "optionname")
    private String name;
    private int type;
    private Object defaultValue;
    @XmlElement(name = "optionvalue")
    private Object value;
    private AbstractOptions owner;

    private transient IOptionInfo info;

    public Option(AbstractOptions owner, String name, String defaultValue) {
        this(owner, name, STRING, defaultValue);
    }

    public Option(AbstractOptions owner, String name, boolean defaultValue) {
        this(owner, name, BOOLEAN, defaultValue);
    }

    public Option(AbstractOptions owner, String name, int defaultValue) {
        this(owner, name, INTEGER, defaultValue);
    }

    public Option(AbstractOptions owner, String name, float defaultValue) {
        this(owner, name, FLOAT, defaultValue);
    }

    public Option(AbstractOptions owner, String name, Vector<String> defaultValue) {
        this(owner, name, CHOICE, "");
    }

    public Option(AbstractOptions owner, String name, int type, Object defaultValue) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        if (isValidValue(defaultValue)) {
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        } else {
            throw new IllegalArgumentException("Tried to give wrong type of value for option type.");
        }
    }

    /**
     * Constructor that satisfies JAXB.
     */
    protected Option() {
    }

    @Override
    public AbstractOptions getOwner() {
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
                + ((type == IOption.INTEGER) || (type == IOption.CHOICE) ? " [" + value + "]" : "");
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
            return !value.equals("None") && !value.equals("");
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
            throw new IllegalArgumentException("Tried to give wrong type of value for option type.");
        }
    }

    @Override
    public void setValue(String value) {
        if (type == STRING || type == CHOICE) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Tried to give String value to non-String option.");
        }
    }

    @Override
    public void setValue(boolean value) {
        if (type == BOOLEAN) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Tried to give boolean value to non-boolean option.");
        }
    }

    @Override
    public void setValue(int value) {
        if (type == INTEGER) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Tried to give integer value to non-integer option.");
        }
    }

    @Override
    public void setValue(float value) {
        if (type == FLOAT) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Tried to give float value to non-float option.");
        }
    }

    // Turns this option "off"
    @Override
    public void clearValue() {
        switch (type) {
            case STRING:
            case CHOICE:
                setValue("");
                break;
            case BOOLEAN:
                setValue(false);
                break;
            case INTEGER:
                setValue(0);
                break;
            case FLOAT:
                setValue(0f);
                break;
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
