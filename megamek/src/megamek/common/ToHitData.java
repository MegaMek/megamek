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

package megamek.common;

/**
 * Contains the to-hit number and a short description of how it was reached
 */
public class ToHitData extends TargetRoll
{
    public static final int HIT_NORMAL      = 0;
    public static final int HIT_PUNCH       = 1;
    public static final int HIT_KICK        = 2;
    
    public static final int SIDE_FRONT      = 0;
    public static final int SIDE_REAR       = 1;
    public static final int SIDE_LEFT       = 2;
    public static final int SIDE_RIGHT      = 3;
  
    private int             hitTable = HIT_NORMAL;
    private int             sideTable = SIDE_FRONT;
    
    /**
     * Construct default.
     */
    public ToHitData() {
        super();
    }
    
    /**
     * Construct with value and desc.  Other values default.
     */
    public ToHitData(int value, String desc) {
        this(value, desc, HIT_NORMAL, SIDE_FRONT);
    }
    
    /**
     * Construct with all variables.
     */
    public ToHitData(int value, String desc, int hitTable, int sideTable) {
        super(value, desc);
        this.hitTable = hitTable;
        this.sideTable = sideTable;
    }
    
    public int getHitTable() {
        return hitTable;
    }
    
    public void setHitTable(int hitTable) {
        this.hitTable = hitTable;
    }
    
    public int getSideTable() {
        return sideTable;
    }
    
    public void setSideTable(int sideTable) {
        this.sideTable = sideTable;
    }
    
    /**
     * Describes the table and side we'return hitting on
     */
    public String getTableDesc() {
        if (getSideTable() != SIDE_FRONT
                || getHitTable() != HIT_NORMAL) {
            String tdesc = new String();
            switch(getSideTable()) {
            case SIDE_RIGHT :
                tdesc += "Right Side ";
                break;
            case SIDE_LEFT :
                tdesc += "Left Side ";
                break;
            case SIDE_REAR :
                tdesc += "Rear ";
                break;
            }
            switch(getHitTable()) {
            case HIT_PUNCH :
                tdesc += "Punch ";
                break;
            case HIT_KICK :
                tdesc += "Kick ";
                break;
            }
            return " (using " + tdesc + "table)";
        } else {
            return "";
        }
    }
}
