/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Jun 1, 2005
 *
 */
package megamek.common;

import java.util.Enumeration;

/**
 * @author Andrew Hunter
 * VTOLs are helicopters (more or less.)  They don't really work properly yet.  Don't use them.
 */
public class VTOL extends Tank {
    
    //this is the elevation of the VTOL--with respect to the floor of the hex it's in.
    //In other words, this may need to *change* as it moves from hex to hex--without it going up or down.
    //I.e.--level 0 hex, elevation 5--it moves to a level 2 hex, without going up or down.
    //elevation is now 3.
    private int elevation;

    public static final int LOC_ROTOR = 5;  //will this cause problems w/r/t turrets?
    
    protected static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR", "RO" };
    protected static String[] LOCATION_NAMES = { "Body", "Front", "Right", "Left", "Rear", "Rotor" };

    public VTOL() {
        super();
    }
    
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /* (non-Javadoc)
     * @see megamek.common.Entity#checkSkid(int, megamek.common.IHex, int, megamek.common.MoveStep, int, int, megamek.common.Coords, megamek.common.Coords, boolean, int)
     */
    public PilotingRollData checkSkid(int moveType, IHex prevHex, int overallMoveType, MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos, boolean isInfantry, int distance) {
        PilotingRollData roll = getBasePilotingRoll();
        roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: VTOLs can't skid");
        return roll;
    }
    
    public boolean canGoDown() {
        return canGoDown(elevation,getPosition());
    }
    
    //is it possible to go down, or are we landed/just above the water/treeline?
    //assuming passed elevation.
    public boolean canGoDown(int assumedElevation,Coords assumedPos) {
        boolean inWaterOrWoods = false;
        IHex hex = getGame().getBoard().getHex(assumedPos);
        int absoluteElevation = assumedElevation+hex.floor();
        if(hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.WATER)) {
            inWaterOrWoods=true;
        }
        if(inWaterOrWoods) {
            return ((absoluteElevation-1)>hex.ceiling());
        } else {
            return ((absoluteElevation-1)>=hex.ceiling());
        }
    }

    public PilotingRollData checkSideSlip(int moveType, IHex prevHex,
            int overallMoveType, MoveStep prevStep,
            int prevFacing, int curFacing,
            Coords lastPos, Coords curPos,
            int distance) {
        PilotingRollData roll = getBasePilotingRoll();

        // TODO: add check for elevation of pavement, road,
        //       or bridge matches entity elevation.
        if (moveType != Entity.MOVE_JUMP
            && prevHex != null
            /* Bug 754610: Revert fix for bug 702735.
               && ( prevHex.contains(Terrain.PAVEMENT) ||
               prevHex.contains(Terrain.ROAD) ||
               prevHex.contains(Terrain.BRIDGE) )
            */
            && overallMoveType == Entity.MOVE_RUN
            && prevFacing != curFacing
            && !lastPos.equals(curPos))
            {
                roll.append(new PilotingRollData(getId(), 0, "VTOL flanking and turning"));//is there a mod on this roll?
             
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: VTOL is not apparently sideslipping");
        }

        return roll;
        
    }

    /* (non-Javadoc)
     * @see megamek.common.Tank#calculateBattleValue(boolean)
     */
    public int calculateBattleValue(boolean assumeLinkedC3) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv
        
        // total armor points
        dbv += getTotalArmor();
        
        // total internal structure        
        dbv += getTotalInternal() / 2;
        
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Enumeration i = equipmentList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if ((etype instanceof WeaponType && ((WeaponType)etype).getAmmoType() == AmmoType.T_AMS)
            || (etype instanceof AmmoType && ((AmmoType)etype).getAmmoType() == AmmoType.T_AMS)
            || etype.hasFlag(MiscType.F_ECM)) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;
        
        double typeModifier;
        /*switch (getMovementType()) {
            case Entity.MovementType.TRACKED:
                typeModifier = 0.8;
                break;
            case Entity.MovementType.WHEELED:
                typeModifier = 0.7;
                break;
            case Entity.MovementType.HOVER:
                typeModifier = 0.6;
                break;
            // vtol and naval to come
            default:
                typeModifier = 0.5;
        }*/
        typeModifier=.4;
        
        dbv *= typeModifier;
        
        // adjust for target movement modifier
        int tmmRan = Compute.getTargetMovementModifier(getOriginalRunMP(), false).getValue();
        if (tmmRan > 5) {
            tmmRan = 5;
        }
        double[] tmmFactors = { 1.0, 1.1, 1.2, 1.3, 1.4, 1.5 };
        dbv *= tmmFactors[tmmRan];
        
        double weaponBV = 0;
        
        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            // don't count AMS, it's defensive
            if (wtype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            
            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if (mLinker.getType() instanceof MiscType && 
                        mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                }
            } 
            
            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.2;
            }
            
            if (mounted.getLocation() == LOC_REAR) {
                weaponsBVRear += dBV;
            } else {
                weaponsBVFront += dBV;
            }
        }
        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }
        
        // add ammo bv
        double ammoBV = 0;
        for (Enumeration i = ammoList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            AmmoType atype = (AmmoType)mounted.getType();
            
            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0)
                continue;

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(this);
        }
        weaponBV += ammoBV;
        
        // adjust further for speed factor
        double speedFactor = getOriginalRunMP() - 5;
        speedFactor /= 10;
        speedFactor++;
        speedFactor = Math.pow(speedFactor, 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        
        obv = weaponBV * speedFactor;

        // we get extra bv from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here.  could be better
        // also, each 'has' loops through all equipment.  inefficient to do it 3 times
        double xbv = 0.0;
        if ((hasC3MM() && calculateFreeC3MNodes() < 2) ||
            (hasC3M() && calculateFreeC3Nodes() < 3) ||
            (hasC3S() && C3Master > NONE) ||
            (hasC3i() && calculateFreeC3Nodes() < 5) ||
            assumeLinkedC3) {
                xbv = (double)(Math.round(0.35 * weaponsBVFront + (0.5 * weaponsBVRear)));
        }
        
        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();

        return (int)Math.round((dbv + obv + xbv) * pilotFactor);
    }

    /* (non-Javadoc)
     * @see megamek.common.Tank#canCharge()
     */
    public boolean canCharge() {
        return false;
    }

    /* TODO:make this work for VTOLs
     */
    public int elevationOccupied(IHex hex) {
        return (hex.floor() + elevation); //I wince even typing this.  I'll make it work properly so they can move up and down once they can fight and crash and move side to side.
    }
    
    public int getElevation() {
        return elevation;
    }
    
    public void setElevation(int elevation) {
        this.elevation=elevation;
    }
    
    
    
    //A helper function for fiddling with elevation.
    //Takes the current hex, a hex being moved to, returns the elevation the VTOL will be considered to be at w/r/t it's new hex.
    public int calcElevation(IHex current, IHex next,int assumedElevation) {
        int absoluteElevation = current.floor()+assumedElevation;
        return absoluteElevation-next.floor();
    }

    public int calcElevation(IHex current, IHex next) {
        return calcElevation(current,next,elevation);
    }

    /* TODO:make this work for VTOLs
     */
    public int getMaxElevationChange() {
        //return 0; //correct implementation, ignore it for now
        return 999; //correct?
        //return super.getMaxElevationChange();
    }

    /* TODO:I don't think this is actually correct...
     */
    public boolean isHexProhibited(IHex hex) {
        return false;
    }

    /* (non-Javadoc)
     * @see megamek.common.Tank#isRepairable()
     */
    public boolean isRepairable() {
        boolean retval = this.isSalvage();
        int loc = Tank.LOC_FRONT;
        while ( retval && loc < VTOL.LOC_ROTOR ) {
            int loc_is = this.getInternal( loc );
            loc++;
            retval = (loc_is != ARMOR_DOOMED) && (loc_is != ARMOR_DESTROYED);
        }
        return retval;
    }

    /* (non-Javadoc)
     * This really, really isn't right.
     */
    public HitData rollHitLocation(int table, int side) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        if (side == ToHitData.SIDE_LEFT) {
            nArmorLoc = LOC_LEFT;
            bSide = true;
        }
        else if (side == ToHitData.SIDE_RIGHT) {
            nArmorLoc = LOC_RIGHT;
            bSide = true;
        }
        else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
        }
        switch (Compute.d6(2)) {
            case 2:
                return new HitData(LOC_ROTOR, false, HitData.EFFECT_CRITICAL);//also rotor destroyed?
            case 3:
                return new HitData(LOC_ROTOR, false, HitData.EFFECT_VEHICLE_MOVE_DESTROYED);
            case 4:
            case 5:
                return new HitData(LOC_ROTOR, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
            case 6:
            case 7:
            case 8:
                return new HitData(nArmorLoc);
            case 9:
                if (bSide) {
                    return new HitData(nArmorLoc); //should be main weapon destroyed, but how in the world?
                }
                else {
                    return new HitData(nArmorLoc);
                }
            case 10:
            case 11:
                return new HitData(LOC_ROTOR, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
            case 12:
                return new HitData(LOC_ROTOR, false, HitData.EFFECT_CRITICAL); //but also -1 to move?  how?
                
        }
        return null;
    }

}
