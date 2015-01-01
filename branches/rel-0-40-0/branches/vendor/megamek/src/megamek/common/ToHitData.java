/**
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
public class ToHitData
{
	public static final int HIT_NORMAL      = 0;
	public static final int HIT_PUNCH       = 1;
	public static final int HIT_KICK        = 2;
    
	public static final int SIDE_FRONT      = 0;
	public static final int SIDE_REAR       = 1;
	public static final int SIDE_LEFT       = 2;
	public static final int SIDE_RIGHT      = 3;
  
    public final static int IMPOSSIBLE      = Integer.MAX_VALUE;
    
    private int             value;
 	private StringBuffer    desc;
    private int             hitTable;
    private int             sideTable;
	
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
        this.value = value;
        this.desc = new StringBuffer(desc);
        this.hitTable = hitTable;
        this.sideTable = sideTable;
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public String getDesc() {
        return desc.toString();
    }
    
    public void setDesc(String desc) {
        this.desc = new StringBuffer(desc);
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
     * Adds a modifer on to the to hit number
     */
    public void addModifier(int modifier, String reason) {
        if (this.getValue() == IMPOSSIBLE) {
            // leave it
        } else if (modifier == IMPOSSIBLE) {
            this.value = IMPOSSIBLE;
            this.desc = new StringBuffer(reason);
        } else {
            this.value += modifier;
            this.desc.append(" " + (modifier >= 0 ? "+ " : "- ") 
                             + Math.abs(modifier) + " (" + reason + ")");
        }
    }
    
    /**
     * Append another ToHitData.  Do not change hit tables.
     */
    public void append(ToHitData other) {
        if (this.getValue() == IMPOSSIBLE) {
            // leave it
        } else if (other.getValue() == IMPOSSIBLE) {
            this.value = IMPOSSIBLE;
            this.desc = new StringBuffer(other.getDesc());
        } else {
            this.value += other.getValue();
            this.desc.append(other.getDesc());
        }
    }
}
