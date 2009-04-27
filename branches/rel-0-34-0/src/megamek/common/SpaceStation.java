/*
* MegaAero - Copyright (C) 2007 Jay Lawson
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
 * Created on Jun 17, 2007
 *
 */
package megamek.common;

import java.io.Serializable;

/**
 * @author Jay Lawson
 */
public class SpaceStation extends Jumpship implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3160156173650960985L;

    public double getCost() {
        
        double cost = 0.0f;
        
        
        //Double.MAX
        //add in controls
        //bridge
        cost += 200000 + 10 * weight;
        //computer
        cost += 200000;
        //life support
        cost += 5000 * (getNCrew() + getNPassenger());
        //sensors
        cost += 80000;
        //fcs
        cost += 100000;
        //gunnery/control systems
        cost += 10000 * getArcswGuns();
        
        //structural integrity
        cost += 100000 * getSI();
        
        //additional flight systems (attitude thruster and landing gear)
        cost += 25000;
        
        //docking hard point
        cost += 100000 * getDocks();
        
        double engineWeight = getOriginalWalkMP() * weight * 0.06;
        cost += engineWeight * 1000;
        //drive unit
        cost += 500 * getOriginalWalkMP() * weight / 100.0;
        //control equipment
        cost += 1000;
        
        //HPG
        if(hasHPG()) {
            cost += 1000000000;
        }
        
        //fuel tanks
        cost += 200 * getFuel() / getFuelPerTon();

        //armor
        cost += getArmorWeight(locations()-2)*EquipmentType.getArmorCost(armorType);
        
        //heat sinks
        int sinkCost = 2000 + 4000 * getHeatType();// == HEAT_DOUBLE ? 6000: 2000;    
        cost += sinkCost*getHeatSinks();
        
        //grav deck
        cost += 5000000 * getGravDeck();
        cost += 10000000 * getGravDeckLarge();
        cost += 40000000 * getGravDeckHuge();        
        
        //weapons 
        cost += getWeaponsAndEquipmentCost();
        
        //get bays
        //Bay doors are not counted in the AT2r example
        int baydoors = 0;
        int bayCost = 0;
        for(Bay next:getTransportBays()) {
            baydoors += next.getDoors();
            if(next instanceof MechBay || next instanceof ASFBay || next instanceof SmallCraftBay) {
                bayCost += 20000 * next.totalSpace;
            }
            if(next instanceof LightVehicleBay || next instanceof HeavyVehicleBay) {
                bayCost += 20000 * next.totalSpace;
            }
        }
        
        cost += bayCost + baydoors * 1000;
        
        //life boats and escape pods
        cost += 5000 * (getLifeBoats() + getEscapePods());
        
        double weightMultiplier = 5.00f;
        
        return Math.round(cost * weightMultiplier);
        
    }
    
    /**
     * All military space stations automatically have ECM if in space
     */
    public boolean hasActiveECM() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() >= 0;
    }
    
    /**
     * What's the range of the ECM equipment? 
     * 
     * @return the <code>int</code> range of this unit's ECM. This value will
     *         be <code>Entity.NONE</code> if no ECM is active.
     */
    public int getECMRange() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        if(!this.isMilitary()) {
            return Entity.NONE;
        }
        int range = 2;      
        //the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();     
        return range;
    }
    
    public double getBVTypeModifier() {
        return 0.7;
    }
    
}