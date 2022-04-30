/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2014 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.common.event;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.net.enums.PacketCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Client Feedback Request Event.  This event is created when the server 
 * requires feedback of some form from the Client.
 * 
 * @see GameListener
 * @author arlith
 */
public class GameCFREvent extends GameEvent {
    private static final long serialVersionUID = 230173422932412803L;
    
    private PacketCommand cfrType;
    
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
     * List of Target IDs for targets of a teleguided missile.
     */
    private List<Integer> telemissileTargets;
    
    /**
     * List of toHit values for the possible telemissile targets.
     */
    private List<Integer> tmToHitValues;
    
    /**
     * List of Target IDs for tagged targets within range.
     */
    private List<Integer> tagTargets;
    
    /**
     * List of Targetable object types for tagged targets within range.
     */
    private List<Integer> tagTargetTypes;
    
    /**
     * Construct game event
     */
    public GameCFREvent(Object source, PacketCommand cfrType) {
        super(source);
        this.cfrType = cfrType;
    }
    
    /**
     * Sub-classed events implement this method to call their specific method on 
     * a GameListener instance that their event has been fired.
     * @param gl GameListener recipient.
     */
    @Override
    public void fireEvent(GameListener gl) {
        gl.gameClientFeedbackRequest(this);
    }
    
    @Override
    public String getEventName() {
        String evtName = "Client Feedback Request, ";
        switch (cfrType) {
            case CFR_DOMINO_EFFECT:
                evtName += " stepping out of a domino effect for Entity Id " + eId;
                break;
            case CFR_AMS_ASSIGN:
                evtName += " assigning AMS for Entity Id " + eId;
                break;
            case CFR_APDS_ASSIGN:
                evtName += " assigning APDS for Entity Id " + eId;
                break;
            case CFR_HIDDEN_PBS:
                evtName += " assigning pointblank shot for Entity Id " + eId + ", target: " + targetId;
                break;
            case CFR_TELEGUIDED_TARGET:
                evtName += " assigning teleguided missile targets: " + telemissileTargets;
                break;
            case CFR_TAG_TARGET:
                evtName += " assigning homing artillery targets: " + tagTargets;
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

    public PacketCommand getCFRType() {
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

    public List<Integer> getTelemissileTargetIds() {
        return Collections.unmodifiableList(telemissileTargets);
    }
    
    public void setTeleguidedMissileTargets(List<Integer> newTargetIds) {
        telemissileTargets = new ArrayList<>(newTargetIds);
    }
    
    public List<Integer> getTmToHitValues() {
        return Collections.unmodifiableList(tmToHitValues);
    }
    
    public void setTmToHitValues(List<Integer> toHitValues) {
        tmToHitValues = new ArrayList<>(toHitValues);
    }

    public List<Integer> getTAGTargets() {
        return Collections.unmodifiableList(tagTargets);
    }
    
    public void setTAGTargets(List<Integer> newTargets) {
        tagTargets = new ArrayList<>(newTargets);
    }
    
    public List<Integer> getTAGTargetTypes() {
        return Collections.unmodifiableList(tagTargetTypes);
    }
    
    public void setTAGTargetTypes(List<Integer> targetTypes) {
        tagTargetTypes = new ArrayList<>(targetTypes);
    }
}
