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
 * GameOption.java
 *
 * Created on April 26, 2002, 10:50 AM
 */

package megamek.common.options;

import java.io.*;

/**
 * The base class for a game option.  Game options relate to game behavior and
 * are the same across all clients.  A game option's primary purpose is to 
 * store a value for the option.  Its secondary purpose is to give the game 
 * options dialog enough data to allow the user to set the option.
 *
 * @author  Ben
 * @version 
 */
public class GameOption implements Serializable {
    
    public static final int     BOOLEAN = 0;
    public static final int     INTEGER = 1;
    public static final int     FLOAT   = 2;
    
    
    private String shortName;
    private String fullName;
    private String desc;
    
    private int type;
    
    private Object defaultValue;
    private Object value;

    public GameOption(String shortName, String fullName, String desc, boolean defaultValue) {
        this(shortName, fullName, desc, BOOLEAN, new Boolean(defaultValue));
    }
    public GameOption(String shortName, String fullName, String desc, int defaultValue) {
        this(shortName, fullName, desc, INTEGER, new Integer(defaultValue));
    }
    public GameOption(String shortName, String fullName, String desc, float defaultValue) {
        this(shortName, fullName, desc, FLOAT, new Float(defaultValue));
    }
    
    /** Creates new GameOption */
    public GameOption(String shortName, String fullName, String desc, int type, Object defaultValue) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.desc = desc;
        this.type = type;
        if (isValidValue(defaultValue)) {
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        } else {
            throw new IllegalArgumentException("Tried to give wrong type of value for option type.");
        }
    }
    
    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDesc() {
        return desc;
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
        return ((Boolean)value).booleanValue();
    }

    public int intValue() {
        return ((Integer)value).intValue();
    }

    public float floatValue() {
        return ((Float)value).floatValue();
    }
    
    public String stringValue() {
        return value.toString();
    }
    
    public void setValue(Object value) {
        if (isValidValue(value)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Tried to give wrong type of value for option type.");
        }
    }
    
    public void setValue(boolean value) {
        if (type == BOOLEAN) {
            this.value = new Boolean(value);
        } else {
            throw new IllegalArgumentException("Tried to give boolean value to non-boolean option.");
        }
    }

    public void setValue(int value) {
        if (type == INTEGER) {
            this.value = new Integer(value);
        } else {
            throw new IllegalArgumentException("Tried to give integer value to non-integer option.");
        }
    }

    public void setValue(float value) {
        if (type == FLOAT) {
            this.value = new Float(value);
        } else {
            throw new IllegalArgumentException("Tried to give float value to non-float option.");
        }
    }
    
    private boolean isValidValue(Object object) {
        switch (type) {
            case BOOLEAN : 
                return object instanceof Boolean;
            case INTEGER : 
                return object instanceof Integer;
            case FLOAT : 
                return object instanceof Float;
            default: 
                return false;
        }
    }

}
