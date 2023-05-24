/*
 * MegaAero - Copyright (C) 2007 Jay Lawson
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
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class TeleMissile extends Aero {
    private static final long serialVersionUID = -5932720323745597199L;

    public static final int LOC_BODY = 0;

    private static String[] LOCATION_ABBRS = { "BODY" };
    private static String[] LOCATION_NAMES = { "Body" };

    private int originalRideId;

    private int critMod = 0;

    // need another type of boolean for out-of-control status that indicates
    // lack of contact with originating unit
    private boolean outContact = false;
    
    public TeleMissile() {
        super();
        damThresh = new int[] { 0 };
    }

    /**
     * Tele-missiles shouldn't get a firing phase
     * @return false
     */
    @Override
    public boolean isEligibleForFiring() {
        return false;
    }

    public TeleMissile(Entity originalRide, int damageValue, int armorValue, double weight, int type, int capMisMod) {
        this();

        String name;
        int fuel;
        switch (type) {
            case AmmoType.T_KRAKEN_T:
                name = "Kraken-T Missile";
                fuel = 25;
                break;
            case AmmoType.T_WHITE_SHARK_T:
                name = "White Shark-T Missile";
                fuel = 40;
                break;
            case AmmoType.T_KILLER_WHALE_T:
                name = "Killer Whale-T Missile";
                fuel = 30;
                break;
            case AmmoType.T_BARRACUDA_T:
                name = "Barracuda-T Missile";
                fuel = 30;
                break;
            default:
                name = "T-Op Missile";
                fuel = 30;
                break;
        }

        setCritMod(capMisMod);

        setFuel(fuel);
        setOriginalWalkMP(fuel);
        setChassis(name);
        setModel("");
        setWeight(weight);
        setDamageValue(damageValue);
        initializeArmor(armorValue, LOC_BODY);
        autoSetInternal();
        initializeSI(0);
        setMovementMode(EntityMovementMode.AERODYNE);

        // Finish initializing this unit.
        setOwner(originalRide.getOwner());
        initializeInternal(1, LOC_BODY);
        setOriginalRideId(originalRide.getId());
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_BODY, false, HitData.EFFECT_NONE);
    }

    int damageValue = 0;

    public void setDamageValue(int dv) {
        damageValue = dv;
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

    @Override
    public void setThresh(int val, int loc) {
        damThresh[loc] = val;
    }

    @Override
    public int getThresh(int loc) {
        return damThresh[loc];
    }

    @Override
    public void autoSetThresh() {
        for (int x = 0; x < locations(); x++) {
            initializeThresh(x);
        }
    }

    @Override
    public void initializeThresh(int loc) {
        int nThresh = (int) Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh, loc);
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Telemissiles don't mount Stealth systems. Would be kind of cool if they did, though.
     */
    @Override
    public boolean hasStealth() {
        return false;
    }

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return 0;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        return getCurrentFuel();
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        return getWalkMP(mpCalculationSetting);
    }

    @Override
    public PilotingRollData checkThrustSI(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding SI");
        return roll;
    }

    @Override
    public PilotingRollData checkThrustSITotal(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Entity is not exceeding SI");
        return roll;
    }

    public boolean isOutContact() {
        return outContact;
    }

    public void setOutContact(boolean b) {
        outContact = b;
    }

    @Override
    public boolean isOutControlTotal() {
        // due to control roll, heat, shut down, or crew unconscious
        return (isOutControl() || outContact || shutDown || getCrew().isUnconscious());
    }

    public void setCritMod(int m) {
        critMod = m;
    }

    public int getCritMod() {
        return critMod;
    }

    @Override
    public int locations() {
        return 1;
    }
    
    @Override
    public boolean canRam() {
        return false;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO & Entity.ETYPE_TELEMISSILE;
    }
}
