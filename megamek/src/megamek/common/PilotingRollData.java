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

public class PilotingRollData
{
    public static final int AUTOMATIC_FALL = Integer.MAX_VALUE;
    
    private int entityId;
    
    private int value;
    private StringBuffer desc;
    
    public PilotingRollData(int entityId, int value, String desc) {
        this.entityId = entityId;
        this.value = value;
        this.desc = new StringBuffer(desc);
    }
    
    public int getEntityId() {
        return entityId;
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
    
    /**
     * Adds a modifer on to the target number
     */
    public void addModifier(int modifier, String reason) {
        if (this.getValue() == AUTOMATIC_FALL) {
            // leave it
        } else if (modifier == AUTOMATIC_FALL) {
            this.value = AUTOMATIC_FALL;
            this.desc = new StringBuffer(reason);
        } else {
            this.value += modifier;
            this.desc.append(" " + (modifier >= 0 ? "+ " : "- ") 
                             + Math.abs(modifier) + " (" + reason + ")");
        }
    }
    
    /**
     * Append another PilotingRollData.
     */
    public void append(PilotingRollData other) {
        if (this.getValue() == AUTOMATIC_FALL) {
            // leave it
        } else if (other.getValue() == AUTOMATIC_FALL) {
            this.value = AUTOMATIC_FALL;
            this.desc = new StringBuffer(other.getDesc());
        } else {
            this.value += other.getValue();
            this.desc.append(other.getDesc());
        }
    }
}
