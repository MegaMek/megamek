/*
 * MegaMek - Copyright (C) 2000-2002,2006 Ben Mazur (bmazur@sev.org)
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

public class PilotingRollData extends TargetRoll
{
    static final long serialVersionUID = -8965684775619336323L;
    private int entityId;
    private boolean m_bCumulative = true;
    
    public PilotingRollData(int entityId) {
        this.entityId = entityId;
    }
    
    public PilotingRollData(int entityId, int value, String desc) {
        super(value, desc);
        this.entityId = entityId;
    }
    
    /**
     * Double-logging style for situations where the mech automatically falls,
     * but the pilot can still save to avoid damage.  The game will later strip
     * out any automatic rolls when it lets the pilot roll to save.
     */
    public PilotingRollData(int entityId, int value, int pilotValue, String desc) {
        super(value, desc);
        addModifier(pilotValue, desc);
        this.entityId = entityId;
    }
    
    public int getEntityId() {
        return entityId;
    }
    
    public void setCumulative(boolean b) {
        m_bCumulative = b;
    }
    
    public boolean isCumulative() {
        return m_bCumulative;
    }

}
