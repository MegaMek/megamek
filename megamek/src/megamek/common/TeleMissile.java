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
public class TeleMissile extends Aero implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5932720323745597199L;

    public static final int        LOC_BODY               = 0;

    protected static String[] LOCATION_ABBRS = { "BODY"};
    protected static String[] LOCATION_NAMES = { "Body" };

    private int originalRideId;
    private int originalRideExternalId;

    private int damThresh[] = {0};

    private int critMod = 0;

    //need another type of boolean for out-of-control status that indicates
    //lack of contact with originating unit
    private boolean outContact = false;

    public TeleMissile(Entity originalRide, int damageValue, float weight, int type, int capMisMod) {
        super();
        //fuel
        int fuel = 0;
        String name = "T-Op Missile";
        switch(type) {
        case(AmmoType.T_KRAKEN_T):
            fuel = 25;
        name = "Kraken-T Missile";
        break;
        case(AmmoType.T_WHITE_SHARK):
            fuel = 40;
        name = "White Shark-T Missile";
        break;
        case(AmmoType.T_KILLER_WHALE):
            fuel = 30;
        name = "Killer Whale-T Missile";
        break;
        case(AmmoType.T_BARRACUDA):
            fuel = 30;
        name = "Barracuda-T Missile";
        break;    
        default:
            fuel = 30;
        }

        setCritMod(capMisMod);

        setFuel(fuel);
        setOriginalWalkMP(fuel);
        setChassis(name);
        setModel("");
        setWeight(weight);
        setDamageValue(damageValue);
        initializeArmor(damageValue*10, LOC_BODY);
        autoSetInternal();
        initializeSI(0);

        // Finish initializing this unit.
        setOwner(originalRide.getOwner());
        initializeInternal(1, Infantry.LOC_INFANTRY);
        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalId());
    }

    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_BODY, false, HitData.EFFECT_NONE);
    }

    int damageValue = 0;

    public void setDamageValue(int dv) {
        this.damageValue = dv;
    }

    public int getDamageValue() {
        return damageValue;
    }

    public int getOriginalRideId() {
        return originalRideId;
    }
    public void setOriginalRideId(int originalRideId) {
        this.originalRideId = originalRideId;
    }
    public int getOriginalRideExternalId() {
        return originalRideExternalId;
    }
    public void setOriginalRideExternalId(int originalRideExternalId) {
        this.originalRideExternalId = originalRideExternalId;
    }

    public void setThresh(int val, int loc) {
        damThresh[loc] = val;
    }

    public int getThresh(int loc) {
        return damThresh[loc];
    }

    public void autoSetThresh()
    {
        for(int x = 0; x < locations(); x++)
        {
            initializeThresh(x);
        }    
    }

    public void initializeThresh(int loc)
    {
        int nThresh = (int)Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh,loc);
    }

    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    public String[] getLocationNames() { 
        return LOCATION_NAMES; 
    }

    public int calculateBattleValue() {
        return 0;
    }

    public PilotingRollData checkThrustSI(int thrust, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding SI");
        return roll;
    }

    public PilotingRollData checkThrustSITotal(int thrust, int overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);

        roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding SI");
        return roll;
    }

    public boolean isOutContact() {
        return outContact;
    }

    public void setOutContact(boolean b) {
        this.outContact = b;
    }

    public boolean isOutControlTotal() {
        //due to control roll, heat, shut down, or crew unconscious
        return (isOutControl() || outContact || shutDown || crew.isUnconscious());
    }

    public void setCritMod(int m) {
        this.critMod = m;
    }

    public int getCritMod() {
        return critMod;
    }
    
    public int locations() {
        return 1;
    }

}