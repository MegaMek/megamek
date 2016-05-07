/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2014 Nicholas Walczak (walczak@cs.umn.edu)
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

package megamek.common.event;

import java.util.List;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.net.Packet;

/**
 * A Client Feedback Request Event.  This event is created when the server 
 * requires feedback of some form from the Client.
 * 
 * @see GameListener
 * @author arlith
 */
public class GameCFREvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 230173422932412803L;
    
    private int cfrType;
    
    private int eId;

    private int targetId;
    
    /**
     * The equipment number for the AMS used in AMS_ASSIGN CFRs.
     */
    private int amsEquipNum;

    private List<Integer> apdsDists;

    /**
     * List of WeaponAttackActions that can have an AMS assigned to them for 
     * AMS_ASSIGN CFRs.
     */
    private List<WeaponAttackAction> waas;
    
    /**
     * Construct game event
     */
    public GameCFREvent(Object source, int t) {
        super(source);
        cfrType = t;
    }
    
    /**
     * Sub-classed events implement this method to call their specific method on 
     * a GameListener instance that their event has been fired.
     * @param gl GameListener recipient.
     */
    public void fireEvent(GameListener gl) {
        gl.gameClientFeedbackRquest(this);
    }
    
    public String getEventName() {
        String evtName = "Client Feedback Request, ";
        switch (cfrType) {
            case Packet.COMMAND_CFR_DOMINO_EFFECT:
                evtName += " stepping out of a domino effect for Entity Id "
                        + eId;
                break;
            case Packet.COMMAND_CFR_AMS_ASSIGN:
                evtName += " assigning AMS for Entity Id " + eId;
                break;
            case Packet.COMMAND_CFR_APDS_ASSIGN:
                evtName += " assigning APDS for Entity Id " + eId;
                break;
        }
        return evtName;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(getEventName());
        buff.append(" game event ");
        return buff.toString();
    }

    public int getCFRType() {
        return cfrType;
    }
    
    public int getEntityId() {
        return eId;
    }

    public void setEntityId(int id) {
        eId = id;
    }

    public int getAmsEquipNum() {
        return amsEquipNum;
    }

    public void setAmsEquipNum(int amsEquipNum) {
        this.amsEquipNum = amsEquipNum;
    }

    public List<WeaponAttackAction> getWAAs() {
        return waas;
    }

    public void setWAAs(List<WeaponAttackAction> waas) {
        this.waas = waas;
    }

    public List<Integer> getApdsDists() {
        return apdsDists;
    }

    public void setApdsDists(List<Integer> apdsDist) {
        this.apdsDists = apdsDist;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}