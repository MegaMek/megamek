/*
 * Copyright (c) 2007 - Jay Lawson
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons;

import java.io.Serial;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;

/**
 * @author Jay Lawson
 */
public class TeleMissile extends Aero {
    @Serial
    private static final long serialVersionUID = -5932720323745597199L;

    public static final int LOC_BODY = 0;

    private static final String[] LOCATION_ABBREVIATIONS = { "BODY" };
    private static final String[] LOCATION_NAMES = { "Body" };

    private int originalRideId;
    private int critMod = 0;

    // need another type of boolean for out-of-control status that indicates
    // lack of contact with originating unit
    private boolean outContact = false;

    public TeleMissile() {
        super();
        damThresh = new int[] { 0 };
    }

    @Override
    public boolean isEligibleForFiring() {
        return false;
    }

    public TeleMissile(Entity originalRide, int damageValue, int armorValue, double weight, AmmoTypeEnum type,
          int capMisMod) {
        this();

        String name;
        int fuel = switch (type) {
            case KRAKEN_T -> {
                name = "Kraken-T Missile";
                yield 25;
            }
            case WHITE_SHARK_T -> {
                name = "White Shark-T Missile";
                yield 40;
            }
            case KILLER_WHALE_T -> {
                name = "Killer Whale-T Missile";
                yield 30;
            }
            case BARRACUDA_T -> {
                name = "Barracuda-T Missile";
                yield 30;
            }
            default -> {
                name = "T-Op Missile";
                yield 30;
            }
        };

        setCritMod(capMisMod);

        setFuel(fuel);
        setOriginalWalkMP(fuel);
        setChassis(name);
        setModel("");
        setWeight(weight);
        setDamageValue(damageValue);
        initializeArmor(armorValue, LOC_BODY);
        autoSetInternal();
        setOSI(0);
        setMovementMode(EntityMovementMode.AERODYNE);

        setOwner(originalRide.getOwner());
        initializeInternal(1, LOC_BODY);
        setOriginalRideId(originalRide.getId());
    }

    @Override
    public HitData rollHitLocation(int table, int side, boolean hasUsedEdge) {
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
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public boolean hasStealth() {
        return false;
    }

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return 0;
    }

    @Override
    public int getGenericBattleValue() {
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
        roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
        return roll;
    }

    @Override
    public PilotingRollData checkThrustSITotal(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
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
