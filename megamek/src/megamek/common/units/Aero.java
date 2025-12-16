/*
  Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.units;

import java.io.Serial;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.TechAdvancement.AdvancementPhase;
import megamek.common.bays.Bay;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.cost.AeroCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.*;
import megamek.common.exceptions.LocationFullException;
import megamek.common.interfaces.ITechnology;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.util.ConditionalStringJoiner;
import megamek.logging.MMLogger;

/**
 * Taharqa's attempt at creating an Aerospace entity
 */
public abstract class Aero extends Entity implements IAero, IBomber {
    private static final MMLogger LOGGER = MMLogger.create(Aero.class);

    @Serial
    private static final long serialVersionUID = 7196307097459255187L;

    // locations
    public static final int LOC_NOSE = 0;
    public static final int LOC_LEFT_WING = 1;
    public static final int LOC_RIGHT_WING = 2;
    public static final int LOC_AFT = 3;
    /** Location used for capital fighters and squadrons **/
    public static final int LOC_WINGS = 4;
    /** Location used for equipment not allocated to a firing arc **/
    public static final int LOC_FUSELAGE = 5;

    // ramming angles
    public static final int RAM_TOWARD_DIR = 0;
    public static final int RAM_TOWARD_OBL = 1;
    public static final int RAM_AWAY_OBL = 2;
    public static final int RAM_AWAY_DIR = 3;

    // heat type
    public static final int HEAT_SINGLE = 0;
    public static final int HEAT_DOUBLE = 1;

    // cockpit types
    public static final int COCKPIT_STANDARD = 0;
    public static final int COCKPIT_SMALL = 1;
    public static final int COCKPIT_COMMAND_CONSOLE = 2;
    public static final int COCKPIT_PRIMITIVE = 3;
    public static final String[] COCKPIT_STRING = { "Standard Cockpit", "Small Cockpit", "Command Console",
                                                    "Primitive Cockpit" };

    public static final String[] COCKPIT_SHORT_STRING = { "Standard", "Small", "Command Console", "Primitive" };

    // critical hits
    public static final int CRIT_NONE = -1;
    public static final int CRIT_CREW = 0;
    public static final int CRIT_FCS = 1;
    public static final int CRIT_WEAPON = 2;
    public static final int CRIT_CONTROL = 3;
    public static final int CRIT_SENSOR = 4;
    public static final int CRIT_BOMB = 5;
    public static final int CRIT_ENGINE = 6;
    public static final int CRIT_FUEL_TANK = 7;
    public static final int CRIT_AVIONICS = 8;
    public static final int CRIT_GEAR = 9;
    public static final int CRIT_HEATSINK = 10;
    public static final int CRIT_CARGO = 11;
    public static final int CRIT_DOCK_COLLAR = 12;
    public static final int CRIT_DOOR = 13;
    public static final int CRIT_KF_BOOM = 14;
    public static final int CRIT_LIFE_SUPPORT = 15;
    public static final int CRIT_LEFT_THRUSTER = 16;
    public static final int CRIT_RIGHT_THRUSTER = 17;
    public static final int CRIT_CIC = 18;
    public static final int CRIT_KF_DRIVE = 19;
    public static final int CRIT_GRAV_DECK = 20;
    public static final int CRIT_WEAPON_BROAD = 21;

    // Effective elevation of airborne Aerospace units
    public static final int AERO_EFFECTIVE_ELEVATION = 999;
    // aerospace have no critical slot limitations this needs to be larger, it is too easy to go over when you get to
    // warships and bombs and such
    private static final int[] NUM_OF_SLOTS = { 100, 100, 100, 100, 100, 100, 100 };

    private static final String[] LOCATION_ABBREVIATIONS = { "NOS", "LWG", "RWG", "AFT", "WNG", "FSLG" };
    private static final String[] LOCATION_NAMES = { "Nose", "Left Wing", "Right Wing", "Aft", "Wings", "Fuselage" };

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    private int sensorHits = 0;
    private int fcsHits = 0;
    private int engineHits = 0;
    private int avionicsHits = 0;
    private int cicHits = 0;
    private boolean fuelTankHit = false;
    private boolean gearHit = false;
    private int structIntegrity;
    private int orig_structIntegrity;
    // set up damage threshold
    protected int[] damThresh = { 0, 0, 0, 0, 0, 0 };
    // set up an int for what the critical effect would be
    private int potCriticalSlot = CRIT_NONE;

    // ignored crew hit for harjel
    private int ignoredCrewHits = 0;
    private int cockpitType = COCKPIT_STANDARD;

    // Auto-ejection
    private boolean autoEject = true;
    private boolean condEjectAmmo = true;
    private boolean condEjectFuel = true;
    private boolean condEjectSIDest = true;

    private boolean ejecting = false;

    // track straight movement from last turn
    private int straightMoves = 0;

    // are we tracking any altitude loss due to air-to-ground assaults
    private int altLoss = 0;

    // track leaving the ground map
    private OffBoardDirection flyingOff = OffBoardDirection.NONE;

    /**
     * Track how much altitude has been lost this turn. This is important for properly making weapon attacks, so
     * WeaponAttackActions knows what the altitude was before the attack happened, since the altitude lose is applied
     * before the attack resolves.
     */
    private int altLossThisRound = 0;

    private boolean spheroid = false;

    // deal with heat
    private int heatSinksOriginal;
    private int heatSinks;
    private int heatType = HEAT_SINGLE;

    // Track how many heat sinks are pod-mounted for OmniFighters; these are included in the total. This is provided
    // for campaign use; MM does not distribute damage between fixed and pod-mounted.
    private int podHeatSinks;

    protected int maxIntBombPoints = 0;
    protected int maxExtBombPoints = 0;
    protected BombLoadout intBombChoices = new BombLoadout();
    protected BombLoadout extBombChoices = new BombLoadout();

    protected int usedInternalBombs = 0;

    // fuel - number of fuel points
    private int fuel = 0;
    private int currentFuel = 0;

    // these are used by more advanced aerospace
    private boolean lifeSupport = true;
    private int leftThrustHits = 0;
    private int rightThrustHits = 0;

    // out of control
    private boolean outControl = false;
    private boolean outCtrlHeat = false;
    private boolean randomMove = false;

    // set up movement
    private int currentVelocity = 0;
    private int nextVelocity = currentVelocity;
    private boolean accLast = false;
    private boolean rolled = false;
    private boolean failedManeuver = false;
    private boolean accDecNow = false;

    // was the damage threshold exceeded this turn
    boolean criticalThreshold = false;

    // vstol status
    boolean vstol = false;

    // Capital Fighter stuff
    private int capitalArmor = 0;
    private int capitalArmor_orig = 0;
    private int fatalThresh = 2;
    private int currentDamage = 0;
    private boolean wingsHit = false;
    // a hash map of the current weapon groups - the key is the
    // location:internal name, and the value is the weapon id
    Map<String, Integer> weaponGroups = new HashMap<>();

    /*
     * According to the rules if two units of the same type and with the same
     * velocity are in the same hex, you roll 2d6 randomly to see who is
     * considered back one step for purposes of targeting. THis is a bitch to
     * do, so instead we assign a large random variable to each aero unit at the
     * start of the round, and we use that. It works out similarly except that
     * you don't roll separately for each pair of possibilities. That should
     * work well enough for our purposes.
     */
    private int whoFirst = 0;

    private int eccmRoll = 0;

    private int enginesLostRound = Integer.MAX_VALUE;

    // List of escape craft used by this ship
    private final Set<String> escapeCraftList = new HashSet<>();

    // Maps unique id of each assigned marine to marine point value
    private final Map<UUID, Integer> marines = new HashMap<>();

    public Aero() {
        super();
        // need to set altitude to something different from entity
        altitude = 5;
    }

    @Override
    public int getUnitType() {
        return UnitType.AERO;
    }

    protected static final TechAdvancement TA_ASF = new TechAdvancement(TechBase.ALL).setAdvancement(
                ITechnology.DATE_NONE,
                2470,
                2490)
          .setProductionFactions(Faction.TH)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.C,
                AvailabilityValue.E,
                AvailabilityValue.D,
                AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    protected static final TechAdvancement TA_ASF_PRIMITIVE = new TechAdvancement(TechBase.IS)
          // Per MUL team and per availability codes should exist to around 2781
          .setISAdvancement(Map.of(
                AdvancementPhase.PROTOTYPE, ITechnology.DATE_ES,
                AdvancementPhase.PRODUCTION, 2200,
                AdvancementPhase.EXTINCT, 2781
          ))
          .setISApproximate(
                AdvancementPhase.PRODUCTION,
                AdvancementPhase.EXTINCT)
          .setPrototypeFactions(Faction.TA)
          .setProductionFactions(Faction.TA)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.D,
                AvailabilityValue.X,
                AvailabilityValue.F,
                AvailabilityValue.F)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        if (isPrimitive()) {
            return TA_ASF_PRIMITIVE;
        } else {
            return TA_ASF;
        }
    }

    protected static final TechAdvancement[] COCKPIT_TA = {
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(Map.of(
                      AdvancementPhase.PROTOTYPE, 2460,
                      AdvancementPhase.PRODUCTION, 2470,
                      AdvancementPhase.COMMON, 2491
                ))
                .setApproximate(AdvancementPhase.PROTOTYPE)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C,
                      AvailabilityValue.C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          // Standard
          new TechAdvancement(TechBase.IS)
                .setISAdvancement(Map.of(
                      AdvancementPhase.PROTOTYPE, 3065,
                      AdvancementPhase.PRODUCTION, 3070,
                      AdvancementPhase.COMMON, 3080
                ))
                .setClanAdvancement(Map.of(
                      AdvancementPhase.COMMON, 3080
                ))
                .setISApproximate(AdvancementPhase.PROTOTYPE)
                .setPrototypeFactions(Faction.WB)
                .setProductionFactions(Faction.WB, Faction.CSR)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          // Small
          new TechAdvancement(TechBase.ALL)
                .setISAdvancement(Map.of(
                      AdvancementPhase.PROTOTYPE, 2625,
                      AdvancementPhase.PRODUCTION, 2631,
                      AdvancementPhase.EXTINCT, 2850,
                      AdvancementPhase.REINTRODUCED, 3030
                ))
                .setISApproximate(AdvancementPhase.PROTOTYPE, AdvancementPhase.EXTINCT, AdvancementPhase.REINTRODUCED)
                .setClanAdvancement(Map.of(
                      AdvancementPhase.PROTOTYPE, 2625,
                      AdvancementPhase.PRODUCTION, 2631
                ))
                .setClanApproximate(AdvancementPhase.PROTOTYPE)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.C,
                      AvailabilityValue.F,
                      AvailabilityValue.E,
                      AvailabilityValue.D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          // Cockpit command console
          new TechAdvancement(TechBase.ALL)
                .setAdvancement(Map.of(
                      AdvancementPhase.PROTOTYPE, ITechnology.DATE_ES,
                      AdvancementPhase.PRODUCTION, 2300,
                      AdvancementPhase.EXTINCT, 2520
                ))
                .setISApproximate(AdvancementPhase.PRODUCTION)
                .setPrototypeFactions(Faction.TA)
                .setProductionFactions(Faction.TA)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D,
                      AvailabilityValue.X,
                      AvailabilityValue.X,
                      AvailabilityValue.F)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          // Primitive
    };

    public static TechAdvancement getCockpitTechAdvancement(int cockpitType) {
        if (cockpitType >= 0 && cockpitType < COCKPIT_TA.length) {
            return new TechAdvancement(COCKPIT_TA[cockpitType]);
        }
        return null;
    }

    public TechAdvancement getCockpitTechAdvancement() {
        return getCockpitTechAdvancement(getCockpitType());
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (isFighter() && (getCockpitTechAdvancement() != null)) {
            ctl.addComponent(getCockpitTechAdvancement());
        }
    }

    // Is it Civilian or Military
    public static final int CIVILIAN = 0;
    public static final int MILITARY = 1;
    protected int designType = MILITARY;

    /**
     * Sets the unit as either a civilian or military design
     */
    public void setDesignType(int design) {
        designType = design;
    }

    @Override
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
        land();
    }

    /**
     * Returns the unit's design type
     */
    public int getDesignType() {
        return designType;
    }

    /**
     * A method to determine if an aero has suffered 3 sensor hits. When double-blind is on, this affects both standard
     * visibility and sensor rolls
     */
    @Override
    public boolean isAeroSensorDestroyed() {
        return getSensorHits() >= 3;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        if (engineHits >= getMaxEngineHits()) {
            return 0;
        }

        int engineLoss = 2;
        if ((this instanceof SmallCraft) || (this instanceof Jumpship)) {
            engineLoss = 1;
        }
        mp = Math.max(0, mp - (engineHits * engineLoss));

        if (!mpCalculationSetting.ignoreCargo()) {
            mp = Math.max(0, mp - getCargoMpReduction(this));
        }

        if ((null != game) && !mpCalculationSetting.ignoreWeather()) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);
            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_WIND) &&
                  conditions.getWind().isTornadoF1ToF3() &&
                  conditions.getWeather().isClear()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreCargo()) {
            mp = reduceMPByBombLoad(mp);
        }

        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (getPartialRepairs().booleanOption("aero_engine_crit")) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreGrounded() && !isAirborne()) {
            mp = isSpheroid() ? 0 : mp / 2;
        }

        return mp;
    }

    /**
     * @return same as {@link #getWalkMP}, but does not divide by 2 when grounded
     */
    @Override
    public int getCurrentThrust() {
        return getWalkMP(MPCalculationSetting.NO_GROUNDED);
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        return 6;
    }

    @Override
    public int getBodyLocation() {
        return LOC_FUSELAGE;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return false;
    }

    @Override
    public boolean isValidSecondaryFacing(int n) {
        return false;
    }

    /**
     * Aerospace really can't torso twist?
     */
    @Override
    public int clipSecondaryFacing(int n) {
        return getFacing();
    }

    @Override
    public boolean isOutControlTotal() {
        // due to control roll, heat, shut down, or crew unconscious
        return (outControl || shutDown || getCrew().isUnconscious());
    }

    @Override
    public boolean isOutControl() {
        return outControl;
    }

    @Override
    public boolean isOutControlHeat() {
        return outCtrlHeat;
    }

    @Override
    public boolean isRandomMove() {
        return randomMove;
    }

    @Override
    public boolean didAccLast() {
        return accLast;
    }

    @Override
    public boolean hasLifeSupport() {
        return lifeSupport;
    }

    public void setLifeSupport(boolean b) {
        lifeSupport = b;
    }

    @Override
    public boolean isRolled() {
        return rolled;
    }

    @Override
    public void setOutControl(boolean outControl) {
        this.outControl = outControl;
    }

    @Override
    public void setOutControlHeat(boolean outControlHeat) {
        outCtrlHeat = outControlHeat;
    }

    @Override
    public void setRandomMove(boolean randomMove) {
        this.randomMove = randomMove;
    }

    @Override
    public void setRolled(boolean roll) {
        rolled = roll;
    }

    @Override
    public void setAccLast(boolean b) {
        accLast = b;
    }

    @Override
    public int getMaxBombPoints() {
        return getMaxExtBombPoints() + getMaxIntBombPoints();
    }

    @Override
    public int getMaxIntBombPoints() {
        return (hasQuirk(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)) ? maxIntBombPoints : 0;
    }

    @Override
    public int getMaxExtBombPoints() {
        return maxExtBombPoints;
    }

    /**
     * Recalculates the internal and external hardpoints that this aero unit has. TM p.217, TW p.245
     */
    public void autoSetMaxBombPoints() {
        // Stock Aerospace units cannot carry bombs
        maxExtBombPoints = maxIntBombPoints = 0;
    }

    @Override
    public BombLoadout getIntBombChoices() {
        return new BombLoadout(intBombChoices);
    }

    @Override
    public void setIntBombChoices(BombLoadout bc) {
        intBombChoices = new BombLoadout(bc);
    }

    @Override
    public BombLoadout getExtBombChoices() {
        return new BombLoadout(extBombChoices);
    }

    @Override
    public void setExtBombChoices(BombLoadout bc) {
        extBombChoices = new BombLoadout(bc);
    }

    @Override
    public void clearBombChoices() {
        intBombChoices.clear();
        extBombChoices.clear();
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        // The base Aero cannot carry bombs so no MP reduction
        return t;
    }

    @Override
    public void setUsedInternalBombs(int b) {
        usedInternalBombs = b;
    }

    @Override
    public void increaseUsedInternalBombs(int b) {
        usedInternalBombs += b;
    }

    @Override
    public int getUsedInternalBombs() {
        return usedInternalBombs;
    }

    public void setWhoFirst() {
        whoFirst = Compute.randomInt(500);
    }

    public int getWhoFirst() {
        return whoFirst;
    }

    @Override
    public int getCurrentVelocity() {
        // if using advanced movement then I just want to sum up
        // the different vectors
        if ((game != null) && game.useVectorMove()) {
            return getVelocity();
        }
        return currentVelocity;
    }

    @Override
    public void setCurrentVelocity(int velocity) {
        currentVelocity = velocity;
    }

    @Override
    public int getNextVelocity() {
        return nextVelocity;
    }

    @Override
    public void setNextVelocity(int velocity) {
        nextVelocity = velocity;
    }

    // need some way of retrieving true current velocity
    // even when using advanced movement
    @Override
    public int getCurrentVelocityActual() {
        return currentVelocity;
    }

    public int getPotCrit() {
        return potCriticalSlot;
    }

    public void setPotCrit(int crit) {
        potCriticalSlot = crit;
    }

    @Override
    public int getSI() {
        return structIntegrity;
    }

    @Override
    public int getOSI() {
        return orig_structIntegrity;
    }

    /**
     * Used to determine modifier for landing; different for Aero and LAM.
     */
    @Override
    public int getNoseArmor() {
        return getArmor(LOC_NOSE);
    }

    @Override
    public int getCapArmor() {
        return capitalArmor;
    }

    @Override
    public void setCapArmor(int i) {
        capitalArmor = i;
    }

    @Override
    public int getCap0Armor() {
        return capitalArmor_orig;
    }

    @Override
    public int getFatalThresh() {
        return fatalThresh;
    }

    @Override
    public int getCurrentDamage() {
        return currentDamage;
    }

    @Override
    public void setCurrentDamage(int i) {
        currentDamage = i;
    }

    /**
     * Set the starting Structural Integrity of this unit. Also sets the current SI as if by {@link #setSI(int)}.
     *
     * @param si The new value for SI
     */
    public void setOSI(int si) {
        orig_structIntegrity = si;
        structIntegrity = si;
    }

    public void autoSetSI() {
        int siWeight = (int) Math.floor(weight / 10.0);
        int siThrust = getOriginalWalkMP();
        initializeSI(Math.max(siWeight, siThrust));
    }

    @Override
    public void autoSetCapArmor() {
        double divisor = 10.0;
        if ((null != game) && gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            divisor = 1.0;
        }
        capitalArmor_orig = (int) Math.round(getTotalOArmor() / divisor);
        capitalArmor = (int) Math.round(getTotalArmor() / divisor);
    }

    @Override
    public void autoSetFatalThresh() {
        int baseThresh = 2;
        if ((null != game) && gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            baseThresh = 20;
        }
        fatalThresh = Math.max(baseThresh, (int) Math.ceil(capitalArmor / 4.0));
    }

    /**
     * @deprecated use {@link Aero#setOSI(int)} instead
     */
    @Deprecated(since = "0.50.05")
    public void initializeSI(int val) {
        setOSI(val);
    }

    @Override
    public void setSI(int si) {
        structIntegrity = si;
    }

    @Override
    public int getSensorHits() {
        return sensorHits;
    }

    public void setSensorHits(int hits) {
        if (hits > 3) {
            hits = 3;
        }
        sensorHits = hits;
    }

    @Override
    public int getFCSHits() {
        return fcsHits;
    }

    public void setFCSHits(int hits) {
        if (hits > 3) {
            hits = 3;
        }
        fcsHits = hits;
    }

    public boolean fuelTankHit() {
        return fuelTankHit;
    }

    public void setFuelTankHit(boolean value) {
        fuelTankHit = value;
    }

    public void setCICHits(int hits) {
        if (hits > 3) {
            hits = 3;
        }
        cicHits = hits;
    }

    public int getCICHits() {
        return cicHits;
    }

    public void setIgnoredCrewHits(int hits) {
        ignoredCrewHits = hits;
    }

    public int getIgnoredCrewHits() {
        return ignoredCrewHits;
    }

    @Override
    public int getEngineHits() {
        return engineHits;
    }

    public void setEngineHits(int hits) {
        engineHits = hits;
        if ((engineHits >= getMaxEngineHits()) && getEnginesLostRound() == Integer.MAX_VALUE) {
            setEnginesLostRound(game != null ? game.getCurrentRound() : -1);
        }
    }

    @Override
    public int getAvionicsHits() {
        return avionicsHits;
    }

    public void setAvionicsHits(int hits) {
        avionicsHits = hits;
    }

    public boolean isGearHit() {
        return gearHit;
    }

    @Override
    public void setGearHit(boolean hit) {
        gearHit = hit;
    }

    /**
     * Modifier to landing or vertical takeoff roll for landing gear damage.
     *
     * @param vTakeoff true if this is for a vertical takeoff, false if for a landing
     *
     * @return the control roll modifier
     */
    @Override
    public int getLandingGearMod(boolean vTakeoff) {
        if (gearHit) {
            return vTakeoff ? 1 : 5;
        } else {
            return 0;
        }
    }

    // Landing mods for partial repairs
    @Override
    public int getLandingGearPartialRepairs() {
        if (getPartialRepairs().booleanOption("aero_gear_crit")) {
            return 2;
        } else if (getPartialRepairs().booleanOption("aero_gear_replace")) {
            return 1;
        } else {
            return 0;
        }
    }

    // Avionics mods for partial repairs
    @Override
    public int getAvionicsMisReplaced() {
        if (getPartialRepairs().booleanOption("aero_avionics_replace")) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getAvionicsMisrepaired() {
        if (getPartialRepairs().booleanOption("aero_avionics_crit")) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setOHeatSinks(int hs) {
        heatSinksOriginal = hs;
    }

    public int getOHeatSinks() {
        return heatSinksOriginal;
    }

    public void setHeatSinks(int hs) {
        heatSinks = hs;
    }

    @Override
    public int getHeatSinks() {
        return heatSinks;
    }

    public int getHeatSinkHits() {
        return heatSinksOriginal - heatSinks;
    }

    public void setHeatType(int heatSinkType) {
        heatType = heatSinkType;
    }

    public int getPodHeatSinks() {
        return podHeatSinks;
    }

    public void setPodHeatSinks(int hs) {
        podHeatSinks = hs;
    }

    @Override
    public boolean tracksHeat() {
        return true;
    }

    public void setLeftThrustHits(int hits) {
        leftThrustHits = hits;
    }

    @Override
    public int getLeftThrustHits() {
        return leftThrustHits;
    }

    public void setRightThrustHits(int hits) {
        rightThrustHits = hits;
    }

    @Override
    public int getRightThrustHits() {
        return rightThrustHits;
    }

    public int getOriginalFuel() {
        return fuel;
    }

    @Override
    public int getFuel() {
        if ((getPartialRepairs().booleanOption("aero_asf_fueltank_crit")) ||
              (getPartialRepairs().booleanOption("aero_fueltank_crit"))) {
            return (int) (fuel * 0.9);
        } else {
            return fuel;
        }
    }

    @Override
    public int getCurrentFuel() {
        if ((getPartialRepairs().booleanOption("aero_asf_fueltank_crit")) ||
              (getPartialRepairs().booleanOption("aero_fueltank_crit"))) {
            return (int) (currentFuel * 0.9);
        } else {
            return currentFuel;
        }
    }

    /**
     * Sets the number of fuel points.
     *
     * @param gas Number of fuel points.
     */
    @Override
    public void setFuel(int gas) {
        fuel = gas;
        currentFuel = gas;
    }

    @Override
    public void setCurrentFuel(int gas) {
        currentFuel = gas;
    }

    @Override
    public double getFuelPointsPerTon() {
        if (isPrimitive()) {
            return 80 / primitiveFuelFactor();
        }
        return 80;
    }

    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons The number of tons of fuel
     */
    @Override
    public void setFuelTonnage(double fuelTons) {
        double pointsPerTon = getFuelPointsPerTon();
        fuel = (int) Math.floor(pointsPerTon * fuelTons + 0.001);
    }

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    @Override
    public double getFuelTonnage() {
        // Rounding required for primitive small craft/dropship fuel multipliers. The reason this is rounded normally
        // instead of up is that the fuel points are actually calculated from the tonnage and rounded down.
        return Math.round(2.0 * fuel / getFuelPointsPerTon()) / 2.0;
    }

    /**
     * Used by SmallCraft and Jumpship and their child classes.
     *
     * @return The tons of fuel burned in a day at 1G using strategic movement.
     */
    public double getStrategicFuelUse() {
        return 0.0;
    }

    /**
     * Some primitive aerospace units have their fuel efficiency reduced by a factor based on construction year.
     *
     * @return The primitive fuel factor for the build year.
     */
    public double primitiveFuelFactor() {
        return 1.0;
    }

    public int getHeatType() {
        return heatType;
    }

    @Override
    public boolean wasCritThresh() {
        return criticalThreshold;
    }

    @Override
    public void setCritThresh(boolean b) {
        criticalThreshold = b;
    }

    @Override
    public boolean isImmobile() {
        // aerospace are never immobile when in the air or space
        if (isAirborne() || isSpaceborne()) {
            return false;
        }
        return super.isImmobile();
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        // reset threshold critical
        setCritThresh(false);

        // reset maneuver status
        setFailedManeuver(false);
        // reset acc/dec this turn
        setAccDecNow(false);

        updateBays();

        // update recovery turn if in recovery
        if (getRecoveryTurn() > 0) {
            setRecoveryTurn(getRecoveryTurn() - 1);
        }

        // if in atmosphere, then halve next turn's velocity
        if (!isSpaceborne() && isDeployed()) {
            setNextVelocity((int) Math.floor(getNextVelocity() / 2.0));
        }

        // update velocity
        setCurrentVelocity(getNextVelocity());

        // if using variable damage thresholds then auto set them
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_VARIABLE_DAMAGE_THRESH)) {
            autoSetThresh();
            autoSetFatalThresh();
        }

        // if they are out of control due to heat, then apply this and reset
        if (isOutControlHeat()) {
            setOutControl(true);
            setOutControlHeat(false);
        }

        // reset eccm bonus
        setECCMRoll(Compute.d6(2));

        // get new random `who first`
        setWhoFirst();

        resetAltLossThisRound();

        // Reset usedInternalBombs
        setUsedInternalBombs(0);

        // Reset flying off dir
        flyingOff = OffBoardDirection.NONE;
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementString(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_SKID -> "Skidded";
            case MOVE_NONE -> "None";
            case MOVE_WALK -> "Cruised";
            case MOVE_RUN -> "Flanked";
            case MOVE_SAFE_THRUST -> "Safe Thrust";
            case MOVE_OVER_THRUST -> "Over Thrust";
            default -> "Unknown!";
        };
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_NONE -> "N";
            case MOVE_SAFE_THRUST -> "S";
            case MOVE_OVER_THRUST -> "O";
            default -> "?";
        };
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    // need to figure out aft-pointed wing weapons
    // need to figure out new arcs
    @Override
    public int getWeaponArc(int weaponNumber) {
        final Mounted<?> mounted = getEquipment(weaponNumber);
        if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB) ||
              mounted.getType().hasFlag(WeaponType.F_DIVE_BOMB) ||
              mounted.getType().hasFlag(WeaponType.F_ALT_BOMB)) {
            return Compute.ARC_360;
        }
        int arc;
        switch (mounted.getLocation()) {
            case LOC_NOSE:
            case LOC_WINGS:
                arc = Compute.ARC_NOSE;
                break;
            case LOC_RIGHT_WING:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_RIGHT_WING_AFT;
                } else {
                    arc = Compute.ARC_RIGHT_WING;
                }
                break;
            case LOC_LEFT_WING:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_LEFT_WING_AFT;
                } else {
                    arc = Compute.ARC_LEFT_WING;
                }
                break;
            case LOC_AFT:
                arc = Compute.ARC_AFT;
                break;
            default:
                arc = Compute.ARC_360;
                break;
        }

        return rollArcs(arc);
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        // just leave true for now in case we implement rolls or
        // newtonian movement this way
        return true;
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {

        /*
         * Unlike other units, ASFs determine potential critical slots based on the to-hit roll, so I need to set this
         * potential value as well as return the to hit data
         */

        int roll = Compute.d6(2);

        // first check for above/below
        if ((table == ToHitData.HIT_ABOVE) || (table == ToHitData.HIT_BELOW)) {

            // have to decide which wing
            int wingLocation = LOC_RIGHT_WING;
            int wingRoll = Compute.d6(1);
            if (wingRoll > 3) {
                wingLocation = LOC_LEFT_WING;
            }
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3, 11:
                    setPotCrit(CRIT_GEAR);
                    return new HitData(wingLocation, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_CREW);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 6, 8:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(wingLocation, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }

        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
                case 2, 12:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_HEATSINK);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_FCS);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_HEATSINK);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 11:
                    setPotCrit(CRIT_GEAR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3, 11:
                    setPotCrit(CRIT_GEAR);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_CREW);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_BOMB);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal right-side hits
            switch (roll) {
                case 2:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 3, 11:
                    setPotCrit(CRIT_GEAR);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_CREW);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_AVIONICS);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_BOMB);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch (roll) {
                case 2, 12:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 3, 11:
                    setPotCrit(CRIT_HEATSINK);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 4:
                    setPotCrit(CRIT_FUEL_TANK);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 6, 8:
                    setPotCrit(CRIT_ENGINE);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_CONTROL);
                    return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
                case 9:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_FUEL_TANK);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
            }
        }
        return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(LOC_DESTROYED);
    }

    /**
     * Not used directly but is overwritten in 5 other classes.
     *
     * @return BV Type Modifier.
     */
    public double getBVTypeModifier() {
        return 1.2;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        // this is a control roll. Affected by:
        // avionics damage
        // partial repairs
        // pilot damage
        // current velocity
        int avionicsHits = getAvionicsHits();
        int pilotHits = getCrew().getHits();

        if ((avionicsHits > 0) && (avionicsHits < 3)) {
            prd.addModifier(avionicsHits, "Avionics Damage");
        }

        // this should probably be replaced with some kind of AVI_DESTROYED
        // boolean
        if (avionicsHits >= 3) {
            prd.addModifier(5, "Avionics Destroyed");
        }

        // partial repairs to avionics system, but only if the avionics aren't already
        // destroyed
        if ((getPartialRepairs() != null) && (avionicsHits < 3)) {
            if (getPartialRepairs().booleanOption("aero_avionics_crit")) {
                prd.addModifier(1, "Partial repair of Avionics");
            }
            if (getPartialRepairs().booleanOption("aero_avionics_replace")) {
                prd.addModifier(1, "Mis-replaced Avionics");
            }
        }

        if (pilotHits > 0) {
            prd.addModifier(pilotHits, "Pilot Hits");
        }

        // movement effects
        // some question whether "above safe thrust" applies to thrust or velocity I will treat it as thrust until it
        // is resolved

        if (moved == EntityMovementType.MOVE_OVER_THRUST) {
            prd.addModifier(+1, "Used more than safe thrust");
        }

        int vel = getCurrentVelocity();
        int velocityMod = vel - (2 * getWalkMP());
        if (!isSpaceborne() && (velocityMod > 0)) {
            prd.addModifier(velocityMod, "Velocity greater than 2x safe thrust");
        }

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        // add in atmospheric effects later
        boolean spaceOrVacuum = isSpaceborne() || conditions.getAtmosphere().isVacuum();
        if (!spaceOrVacuum && isAirborne()) {
            prd.addModifier(+2, "Atmospheric operations");

            // check type
            if (this instanceof Dropship) {
                if (isSpheroid()) {
                    prd.addModifier(+1, "spheroid dropship");
                } else {
                    prd.addModifier(0, "aerodyne dropship");
                }
            } else {
                prd.addModifier(-1, "fighter/small craft");
            }
        }

        // life support (only applicable to non-ASFs
        if (!hasLifeSupport()) {
            prd.addModifier(+2, "No life support");
        }

        if (hasModularArmor()) {
            prd.addModifier(1, "Modular Armor");
        }
        // VDNI bonus?
        if (hasAbility(OptionsConstants.MD_VDNI) && !hasAbility(OptionsConstants.MD_BVDNI)) {
            prd.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Aero.COCKPIT_SMALL) &&
              !hasAbility(OptionsConstants.MD_BVDNI) &&
              !hasAbility(OptionsConstants.UNOFFICIAL_SMALL_PILOT)) {
            prd.addModifier(1, "Small Cockpit");
        }

        // quirks?
        if (hasQuirk(OptionsConstants.QUIRK_POS_ATMOSPHERE_FLYER) && !isSpaceborne()) {
            prd.addModifier(-1, "atmospheric flyer");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_ATMOSPHERE_INSTABILITY) && !isSpaceborne()) {
            prd.addModifier(+1, "atmospheric flight instability");
        }
        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
              && !hasAbility(OptionsConstants.UNOFFICIAL_SMALL_PILOT)) {
            prd.addModifier(1, "cramped cockpit");
        }

        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        if (((getEntityType() & Entity.ETYPE_DROPSHIP) == 0) ||
              ((getEntityType() & Entity.ETYPE_SMALL_CRAFT) == 0) ||
              ((getEntityType() & Entity.ETYPE_FIGHTER_SQUADRON) == 0) ||
              ((getEntityType() & Entity.ETYPE_JUMPSHIP) == 0) ||
              ((getEntityType() & Entity.ETYPE_SPACE_STATION) == 0)) {
            r = new Report(7036);
        } else {
            r = new Report(7030);
        }
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                if (this instanceof FighterSquadron) {
                    r = new Report(7076, Report.PUBLIC);
                } else {
                    r = new Report(7073, Report.PUBLIC);
                }
            }
            vDesc.addElement(r);
        } else if (getCrew().isEjected()) {
            r = new Report(7074, Report.PUBLIC);
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (isAirborne()) {
            return super.getRunMP(mpCalculationSetting);
        } else {
            return super.getWalkMP(mpCalculationSetting);
        }
    }

    @Override
    public int getHeatCapacity(boolean includeRadicalHeatSink) {
        int capacity = (getHeatSinks() * (getHeatType() + 1));
        if (includeRadicalHeatSink && hasWorkingMisc(MiscType.F_RADICAL_HEATSINK)) {
            capacity += (int) Math.ceil(getHeatSinks() * 0.4);
        }
        return capacity;
    }

    @Override
    public int getHeatCapacityWithWater() {
        // If the aero is in the water, it is dead so no worries
        return getHeatCapacity(false);
    }

    @Override
    public int getEngineCritHeat() {
        // Engine hits cause excess heat for fighters, TW pg 240
        if (!((this instanceof SmallCraft) || (this instanceof Jumpship))) {
            return 2 * getEngineHits();
        } else {
            return 0;
        }
    }

    @Override
    public void autoSetInternal() {
        for (int x = 0; x < locations(); x++) {
            initializeInternal(0, x);
        }
    }

    // initialize the Damage threshold
    public void autoSetThresh() {
        for (int x = 0; x < locations(); x++) {
            initializeThresh(x);
        }
    }

    public void setThresh(int val, int loc) {
        if (loc < damThresh.length) {
            damThresh[loc] = val;
        }
    }

    public void initializeThresh(int loc) {
        int nThresh = (int) Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh, loc);
    }

    @Override
    public int getThresh(int loc) {
        if (isCapitalFighter()) {
            if ((null != game) && gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_VARIABLE_DAMAGE_THRESH)) {
                    return (int) Math.round(getCapArmor() / 40.0) + 1;
                } else {
                    return (int) Math.round(getCap0Armor() / 40.0) + 1;
                }
            } else {
                // Return 1 so that "> 1" triggers on 2+ capital damage per SO p.116
                // ("at least 15 points of standard-scale damage" = 2 capital)
                return 1;
            }
        } else if (loc < damThresh.length) {
            return damThresh[loc];
        }
        return 0;
    }

    @Override
    public int getHighestThresh() {
        int max = damThresh[0];
        for (int i = 1; i < damThresh.length; i++) {
            if (damThresh[i] > max) {
                max = damThresh[i];
            }
        }
        return max;
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can be repaired (given enough time and
     *       parts); if this value is
     *       <code>false</code>, the unit is only a source of spares.
     *
     * @see Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
        return true; // deal with this later
    }

    @Override
    public boolean canCharge() {
        // ramming is resolved differently than charging
        return false;
    }

    @Override
    public boolean canDFA() {
        // Aero can't DFA
        return false;
    }

    @Override
    public boolean canRam() {
        return !isImmobile() && (getWalkMP() > 0);
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return AeroCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        double priceModifier = 1.0;
        if (isOmni()) {
            priceModifier *= 1.25f;
        }
        priceModifier *= 1 + (weight / 200f);
        return priceModifier;
    }

    @Override
    public int implicitClanCASE() {
        if (!isClan() || !isFighter()) {
            return 0;
        }
        // Ammo is actually supposed to be assigned to a fuselage location rather than
        // one of the four
        // weapon arcs. We will use LOC_NONE to record the existence of non-weapon
        // explosive equipment.
        Set<Integer> caseLocations = new HashSet<>();
        int explicit = 0;
        for (Mounted<?> m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && (m.getType().hasFlag(MiscType.F_CASE))) {
                explicit++;
            } else if (m.getType().isExplosive(m)) {
                if (m.getType() instanceof WeaponType) {
                    caseLocations.add(m.getLocation());
                } else {
                    caseLocations.add(LOC_NONE);
                }
            }
        }
        return Math.max(0, caseLocations.size() - explicit);
    }

    @Override
    public boolean doomedInAtmosphere() {
        return false;
    }

    @Override
    public boolean doomedInSpace() {
        return false;
    }

    @Override
    public void setEngine(Engine e) {
        super.setEngine(e);
        if (hasEngine() && getEngine().engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }

    /**
     * Returns the percent of the SI remaining
     */
    @Override
    public double getInternalRemainingPercent() {
        return ((double) getSI() / (double) getOSI());
    }

    protected int calculateWalk() {
        if (!hasEngine()) {
            return 0;
        }
        if (isPrimitive()) {
            double rating = getEngine().getRating();
            rating /= 1.2;
            if ((rating % 5) != 0) {
                return (int) (((rating - (rating % 5)) + 5) / (int) weight) + 2;
            }
            return (int) (rating / (int) weight) + 2;
        }
        return (getEngine().getRating() / (int) weight) + 2;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted) throws LocationFullException {
        if (getEquipmentNum(mounted) == -1) {
            super.addEquipment(mounted, loc, rearMounted);
        }
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));
    }

    /**
     * get the type of critical caused by a critical roll, taking account of existing damage
     *
     * @param roll   the final dice roll
     * @param target the hit location
     *
     * @return a critical type
     */
    public int getCriticalEffect(int roll, int target) {
        // just grab the latest potential critical
        if (roll < target) {
            return CRIT_NONE;
        }

        return getPotCrit();
    }

    /**
     *
     */
    @Override
    public void setOmni(boolean omni) {

        // Perform the superclass' action.
        super.setOmni(omni);

    }

    @Override
    public void addClanCase() {
        if (!(isClan() && isFighter())) {
            return;
        }
        boolean explosiveFound;
        EquipmentType clCase = EquipmentType.get(EquipmentTypeLookup.CLAN_CASE);
        for (int i = 0; i < locations(); i++) {
            // Ignore wings location: it's not a valid loc to put equipment in
            if (i == LOC_WINGS) {
                continue;
            }
            // Skip location if it already contains CASE
            if (locationHasCase(i) || hasCASEII(i)) {
                continue;
            }

            explosiveFound = false;
            for (Mounted<?> m : getEquipment()) {
                if (m.getType().isExplosive(m, true) && (m.getLocation() == i)) {
                    explosiveFound = true;
                }
            }
            if (explosiveFound) {
                try {
                    addEquipment(Mounted.createMounted(this, clCase), i, false);
                } catch (LocationFullException ex) {
                    // um, that's impossible.
                }
            }
        }

    }

    /**
     * @return see if case is available anywhere
     */
    @Override
    public boolean hasCase() {

        boolean hasCase = false;

        for (int x = 0; x < locations(); x++) {
            if (!hasCase) {
                hasCase = locationHasCase(x);
            }
        }
        return hasCase;
    }

    /**
     * Used to determine net velocity of ramming attack
     */
    @Override
    public int sideTableRam(Coords src) {
        int side = super.sideTableRam(src);
        if (game.useVectorMove() && game.getBoard().isSpace()) {
            int newSide = chooseSideRam(src);
            if (newSide != -1) {
                side = newSide;
            }
        }
        return side;

    }

    public int chooseSideRam(Coords src) {
        // loop through directions and if we have a non-zero vector, then
        // compute
        // the target side table. If we come to a higher vector, then replace. If
        // we come to an equal vector then take it if it is better
        int thrust;
        int high = -1;
        int side = -1;
        for (int dir = 0; dir < 6; dir++) {
            thrust = getVector(dir);
            if (thrust == 0) {
                continue;
            }

            if (thrust > high) {
                high = thrust;
                side = sideTableRam(src, dir);
            }

            // what if they tie
            if (thrust == high) {
                int newSide = sideTableRam(src, dir);
                // choose the better
                if (newSide > side) {
                    side = newSide;
                }
                // that should be the only case, because it can't shift you from front to aft or vice versa
            }

        }
        return side;
    }

    public int getMaxEngineHits() {
        return 3;
    }

    @Override
    public int getMaxElevationChange() {
        if (isAirborne()) {
            return UNLIMITED_JUMP_DOWN;
        }
        return 1;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth can be active and not working when
     * under ECCM)
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *       currently active, <code>false</code> if there is no stealth system or if it is inactive.
     */
    @Override
    public boolean isStealthActive() {
        // Try to find a Mek Stealth system.
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_STEALTH)) {

                if (mEquip.curMode().equals("On") && hasActiveECM()) {
                    // Return true if the mode is "On" and ECM is working
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth can be active and not working when
     * under ECCM)
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *       currently active, <code>false</code> if there is no stealth system or if it is inactive.
     */
    @Override
    public boolean isStealthOn() {
        // Try to find a Mek Stealth system.
        for (Mounted<?> mEquip : getMisc()) {
            MiscType miscType = (MiscType) mEquip.getType();
            if (miscType.hasFlag(MiscType.F_STEALTH)) {
                if (mEquip.curMode().equals("On")) {
                    // Return true if the mode is "On"
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given range. If the value supplied for
     * <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p>
     * Subclasses are encouraged to override this method.
     *
     * @param range - an <code>int</code> value that must match one of the
     *              <code>Compute</code> class range constants.
     * @param ae    - entity making the attack
     *
     * @return a <code>TargetRoll</code> value that contains the stealth modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Stealth or null sig must be active.
        if (!isStealthActive()) {
            result = new TargetRoll(0, "stealth not active");
        }
        // Determine the modifier based upon the range.
        // Infantry do not ignore Chameleon LPS!!!
        else {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                    if (!ae.isConventionalInfantry()) {
                        result = new TargetRoll(0, "stealth");
                    } else {
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_MEDIUM:
                    if (!ae.isConventionalInfantry()) {
                        result = new TargetRoll(1, "stealth");
                    } else {
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    if (!ae.isConventionalInfantry()) {
                        result = new TargetRoll(2, "stealth");
                    } else {
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Return the result.
        return result;

    } // End public TargetRoll getStealthModifier( char )

    @Override
    public void setArmorType(int armType) {
        setArmorType(armType, true);
    }

    public void setArmorType(int armType, boolean addMount) {
        super.setArmorType(armType);
        if ((armType == EquipmentType.T_ARMOR_STEALTH_VEHICLE) && addMount) {
            try {
                this.addEquipment(EquipmentType.get(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH_VEHICLE,
                      false)), LOC_AFT);
            } catch (LocationFullException e) {
                // this should never happen
            }
        }
    }

    @Override
    public boolean isLocationProhibited(Coords testPosition, int testBoardId, int testAltitude) {
        if (!game.hasBoardLocation(testPosition, testBoardId)) {
            return true;
        }

        if ((testAltitude != 0) || isSpaceborne()) {
            return false;
        }

        Hex hex = game.getHex(testPosition, testBoardId);

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if (hex.containsTerrain(Terrains.PAVEMENT) || hex.containsTerrain(Terrains.ROAD)) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == testAltitude) && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER)) {
                return true;
            }
        }

        // grounded aerospace have the same prohibitions as wheeled tanks
        return taxingAeroProhibitedTerrains(hex);
    }

    @Override
    public boolean isNightwalker() {
        return false;
    }

    @Override
    public boolean isSpheroid() {
        return spheroid;
    }

    public void setSpheroid(boolean b) {
        spheroid = b;
    }

    @Override
    public int getStraightMoves() {
        return straightMoves;
    }

    @Override
    public void setStraightMoves(int i) {
        straightMoves = i;
    }

    @Override
    public boolean isVSTOL() {
        return vstol;
    }

    @Override
    public boolean isSTOL() {
        return false;
    }

    public void setVSTOL(boolean b) {
        vstol = b;
    }

    @Override
    public boolean didFailManeuver() {
        return failedManeuver;
    }

    @Override
    public void setFailedManeuver(boolean b) {
        failedManeuver = b;
    }

    @Override
    public void setAccDecNow(boolean b) {
        accDecNow = b;
    }

    @Override
    public boolean didAccDecNow() {
        return accDecNow;
    }

    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    /**
     * The number of critical slots that are destroyed in the component.
     */
    @Override
    public int getBadCriticalSlots(int type, int index, int loc) {
        return 0;
    }

    public int getCockpitType() {
        return cockpitType;
    }

    public void setCockpitType(int type) {
        cockpitType = type;
        if (type == COCKPIT_COMMAND_CONSOLE) {
            setCrew(new Crew(CrewType.COMMAND_CONSOLE));
        } else {
            setCrew(new Crew(CrewType.SINGLE));
        }
    }

    public String getCockpitTypeString() {
        return Aero.getCockpitTypeString(getCockpitType());
    }

    public static String getCockpitTypeString(int inCockpitType) {
        if ((inCockpitType < 0) || (inCockpitType >= COCKPIT_STRING.length)) {
            return "Unknown";
        }
        return COCKPIT_STRING[inCockpitType];
    }

    @Override
    public boolean hasCommandConsoleBonus() {
        return getCockpitType() == COCKPIT_COMMAND_CONSOLE &&
              getCrew().hasActiveCommandConsole() &&
              getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY;
    }

    @Override
    public double getArmorRemainingPercent() {
        int armor0 = getTotalOArmor();
        int armor = getTotalArmor();
        if (isCapitalFighter()) {
            armor0 = getCap0Armor();
            armor = getCapArmor();
        }
        if (armor0 == 0) {
            return IArmorState.ARMOR_NA;
        }
        return ((double) armor / (double) armor0);
    }

    /**
     * keep track of whether the wings have suffered a weapon critical hit
     */
    public boolean areWingsHit() {
        return wingsHit;
    }

    public void setWingsHit(boolean b) {
        wingsHit = b;
    }

    /**
     * what location is opposite the given one
     */
    public int getOppositeLocation(int loc) {
        return switch (loc) {
            case Aero.LOC_NOSE -> Aero.LOC_AFT;
            case Aero.LOC_LEFT_WING -> Aero.LOC_RIGHT_WING;
            case Aero.LOC_RIGHT_WING -> Aero.LOC_LEFT_WING;
            default -> Aero.LOC_NOSE;
        };
    }

    /**
     * get modifications to the cluster hit table for critical hits
     */
    @Override
    public int getClusterMods() {
        return -1 * (getFCSHits() + getSensorHits());
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *       <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if (!gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM) ||
              !isSpaceborne()) {
            return super.getECMRange();
        }
        return Math.min(super.getECMRange(), 0);
    }

    /**
     * @return the strength of the ECCM field this unit emits
     */
    @Override
    public double getECCMStrength() {
        if (!gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM) ||
              !isSpaceborne()) {
            return super.getECCMStrength();
        }
        if (hasActiveECCM()) {
            return 1;
        }
        return 0;
    }

    public void setECCMRoll(int i) {
        eccmRoll = i;
    }

    public int getECCMRoll() {
        return eccmRoll;
    }

    public int getECCMTarget() {
        return getCrew().getPiloting() + getSensorHits() + getCICHits() + getFCSHits();
    }

    public int getECCMBonus() {
        return Math.max(0, eccmRoll - getECCMTarget());
    }

    /**
     * @return is the crew of this vessel protected from gravitational effects, see StratOps, pg. 36
     */
    public boolean isCrewProtected() {
        return true;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *       otherwise.
     */
    @Override
    public boolean canLoad(Entity unit, boolean checkFalse) {
        // capital fighters can load other capital fighters (becoming squadrons)
        // but not in the deployment phase
        if (isCapitalFighter() &&
              !unit.isEnemyOf(this) &&
              unit.isCapitalFighter() &&
              (getId() != unit.getId()) &&
              !getGame().getPhase().isDeployment()) {
            return true;
        }

        return super.canLoad(unit, checkFalse);
    }

    @Override
    public Map<String, Integer> getWeaponGroups() {
        return weaponGroups;
    }

    /**
     * Iterate through current weapons and count the number in each capital fighter location.
     *
     * @return A map with keys in the format "weaponName:loc", with the number of weapons of that type in that location
     *       as the value.
     */
    @Override
    public Map<String, Integer> groupWeaponsByLocation() {
        Map<String, Integer> groups = new HashMap<>();
        for (Mounted<?> mounted : getTotalWeaponList()) {
            int loc = mounted.getLocation();
            if (isFighter() && ((loc == Aero.LOC_RIGHT_WING) || (loc == Aero.LOC_LEFT_WING))) {
                loc = Aero.LOC_WINGS;
            }
            if (mounted.isRearMounted()) {
                loc = Aero.LOC_AFT;
            }
            String key = mounted.getType().getInternalName() + ":" + loc;
            groups.merge(key, mounted.getNWeapons(), Integer::sum);
        }
        return groups;
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticalSlots(LOC_AFT); slot++) {
            CriticalSlot cs = getCritical(LOC_AFT, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mek.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }

    /**
     * see {@link Entity#getForwardArc()}
     */
    @Override
    public int getForwardArc() {
        return Compute.ARC_NOSE;
    }

    /**
     * see {@link Entity#getRearArc()}
     */
    @Override
    public int getRearArc() {
        return Compute.ARC_AFT;
    }

    @Override
    public int getAltLoss() {
        return altLoss;
    }

    @Override
    public void setAltLoss(int i) {
        altLoss = i;
    }

    @Override
    public void resetAltLoss() {
        altLoss = 0;
    }

    @Override
    public int getAltLossThisRound() {
        return altLossThisRound;
    }

    @Override
    public void setAltLossThisRound(int i) {
        altLossThisRound = i;
    }

    @Override
    public void resetAltLossThisRound() {
        altLossThisRound = 0;
    }

    @Override
    public int getElevation() {
        if ((game != null) && isSpaceborne()) {
            return 0;
        }
        // Altitude is not the same as elevation. If an aero is at 0 altitude, then it is grounded
        // and uses elevation normally. Otherwise, just set elevation to a very large number so that
        // a flying aero won't interact with the ground maps in any way
        return isAirborne() ? AERO_EFFECTIVE_ELEVATION : super.getElevation();
    }

    @Override
    public boolean canGoDown() {
        return canGoDown(altitude, getPosition(), getBoardId());
    }

    @Override
    public boolean isPrimitive() {
        return (getCockpitType() == Aero.COCKPIT_PRIMITIVE);
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    public String getCritDamageString() {
        ConditionalStringJoiner conditionalStringJoiner = new ConditionalStringJoiner();
        conditionalStringJoiner.add(getEngineHits() > 0,
              () -> String.format(Messages.getString("Aero.engineDamageString"), getEngineHits()));
        conditionalStringJoiner.add(getSensorHits() > 0,
              () -> String.format(Messages.getString("Aero.sensorDamageString"), getSensorHits()));
        conditionalStringJoiner.add(getAvionicsHits() > 0,
              () -> String.format(Messages.getString("Aero.avionicsDamageString"), getAvionicsHits()));
        conditionalStringJoiner.add(getFCSHits() > 0,
              () -> String.format(Messages.getString("Aero.fcsDamageString"), getFCSHits()));
        conditionalStringJoiner.add(getCICHits() > 0,
              () -> String.format(Messages.getString("Aero.cicDamageString"), getCICHits()));
        conditionalStringJoiner.add(isGearHit(),
              () -> String.format(Messages.getString("Aero.landingGearDamageString"), isGearHit()));
        conditionalStringJoiner.add(!hasLifeSupport(),
              () -> Messages.getString("Aero.lifeSupportDamageString"));
        conditionalStringJoiner.add(getLeftThrustHits() > 0,
              () -> String.format(Messages.getString("Aero.leftThrusterDamageString"), getLeftThrustHits()));
        conditionalStringJoiner.add(getRightThrustHits() > 0,
              () -> String.format(Messages.getString("Aero.rightThrusterDamageString"), getRightThrustHits()));
        // Cargo bays and bay doors for large craft
        for (Bay transportBay : getTransportBays()) {
            conditionalStringJoiner.add(transportBay.getBayDamage() > 0,
                  () -> String.format(Messages.getString("Aero.bayDamageString"),
                        transportBay.getTransporterType(),
                        transportBay.getBayNumber()));
            conditionalStringJoiner.add(transportBay.getCurrentDoors() < transportBay.getDoors(),
                  () -> String.format(Messages.getString("Aero.bayDoorDamageString"),
                        transportBay.getTransporterType(),
                        transportBay.getBayNumber(),
                        (transportBay.getDoors() - transportBay.getCurrentDoors())));
        }
        return conditionalStringJoiner.toString();
    }

    @Override
    public boolean isCrippled() {
        return isCrippled(true);
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        if (isEjecting()) {
            LOGGER.debug("{} CRIPPLED: The crew is currently ejecting.", getDisplayName());
            return true;
        } else if (getInternalRemainingPercent() < 0.5) {
            LOGGER.debug("{} CRIPPLED: Only {} internals remaining.",
                  getDisplayName(),
                  NumberFormat.getPercentInstance().format(getInternalRemainingPercent()));
            return true;
        } else if (getEngineHits() > 0) {
            LOGGER.debug("{} CRIPPLED: {} Engine Hits.", getDisplayName(), engineHits);
            return true;
        } else if (fuelTankHit()) {
            LOGGER.debug("{} CRIPPLED: Fuel Tank Hit", getDisplayName());
            return true;
        } else if (checkCrew && (getCrew() != null) && (getCrew().getHits() >= 4)) {
            LOGGER.debug("{} CRIPPLED: {} Crew Hits taken.", getDisplayName(), getCrew().getHits());
            return true;
        } else if (getFCSHits() >= 3) {
            LOGGER.debug("{} CRIPPLED: Fire Control Destroyed by taking {}", getDisplayName(), fcsHits);
            return true;
        } else if (getCICHits() >= 3) {
            LOGGER.debug("{} CRIPPLED: Combat Information Center Destroyed by taking {}", getDisplayName(), cicHits);
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        if (!hasViableWeapons()) {
            LOGGER.debug("{} CRIPPLED: No more viable weapons.", getDisplayName());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isDmgHeavy() {
        if (getArmorRemainingPercent() <= 0.33) {
            LOGGER.debug("{} Heavily Damaged: Armour Remaining percent of {} is less than or equal to 0.33.",
                  getDisplayName(),
                  getArmorRemainingPercent());
            return true;
        } else if (getInternalRemainingPercent() < 0.67) {
            LOGGER.debug("{} Heavily Damaged: Internal Structure Remaining percent of {} is less than 0.67.",
                  getDisplayName(),
                  getInternalRemainingPercent());
            return true;
        } else if ((getCrew() != null) && (getCrew().getHits() == 3)) {
            LOGGER.debug("{} Moderately Damaged: The crew has taken a minimum of three hits.", getDisplayName());
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        List<WeaponMounted> weaponList = getTotalWeaponList();
        int totalWeapons = weaponList.size();
        int totalInoperable = 0;
        for (WeaponMounted weaponMounted : weaponList) {
            if (weaponMounted.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.75;
    }

    @Override
    public boolean isDmgModerate() {
        if (getArmorRemainingPercent() <= 0.5) {
            LOGGER.debug("{} Moderately Damaged: Armour Remaining percent of {} is less than or equal to 0.50.",
                  getDisplayName(),
                  getArmorRemainingPercent());
            return true;
        } else if (getInternalRemainingPercent() < 0.75) {
            LOGGER.debug("{} Moderately Damaged: Internal Structure Remaining percent of {} is less than 0.75.",
                  getDisplayName(),
                  getInternalRemainingPercent());
            return true;
        } else if ((getCrew() != null) && (getCrew().getHits() == 2)) {
            LOGGER.debug("{} Moderately Damaged: The crew has taken a minimum of two hits.", getDisplayName());
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.5;
    }

    @Override
    public boolean isDmgLight() {
        if (getArmorRemainingPercent() <= 0.75) {
            LOGGER.debug("{} Lightly Damaged: Armour Remaining percent of {} is less than or equal to 0.75.",
                  getDisplayName(),
                  getArmorRemainingPercent());
            return true;
        } else if (getInternalRemainingPercent() < 0.9) {
            LOGGER.debug("{} Lightly Damaged: Internal Structure Remaining percent of {} is less than 0.9.",
                  getDisplayName(),
                  getInternalRemainingPercent());
            return true;
        } else if ((getCrew() != null) && (getCrew().getHits() == 1)) {
            LOGGER.debug("{} Lightly Damaged: The crew has taken a minimum of one hit.", getDisplayName());
            return true;
        }

        // If this is not a military unit, we don't care about weapon status.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.25;
    }

    @Override
    public boolean canSpot() {
        // per a recent ruling on the official forums, aero units can't spot
        // for indirect LRM fire, unless they have a recon cam, an infrared or
        // hyperspace imager, or a high-res imager and it's not night
        boolean hiresLighted = hasWorkingMisc(MiscType.F_HIRES_IMAGER) &&
              game.getPlanetaryConditions().getLight().isDayOrDusk();
        return !isAirborne() ||
              hasWorkingMisc(MiscType.F_RECON_CAMERA) ||
              hasWorkingMisc(MiscType.F_INFRARED_IMAGER) ||
              hasWorkingMisc(MiscType.F_HYPERSPECTRAL_IMAGER) ||
              hiresLighted;
    }

    // Damage a fighter that was part of a squadron when splitting it. Per
    // StratOps pg. 32 & 34
    @Override
    public void doDisbandDamage() {

        int dealt = 0;

        // Check for critical threshold and if so damage all armor on one facing
        // of the fighter completely,
        // reduce SI by half, and mark three engine hits.
        if (isDestroyed() || isDoomed()) {
            int loc = Compute.randomInt(4);
            dealt = getArmor(loc);
            setArmor(0, loc);
            int finalSI = Math.min(getSI(), getSI() / 2);
            dealt += getSI() - finalSI;
            setSI(finalSI);
            setEngineHits(Math.max(3, getEngineHits()));
        }

        // Move on to actual damage...
        int damage = getCap0Armor() - getCapArmor();
        // Fix for #587. Only multiply if Aero Sanity is off
        if ((null != game) && !gameOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            damage *= 10;
        }
        damage -= dealt; // We already dealt a bunch of damage, move on.
        if (damage < 1) {
            return;
        }
        int hits = (int) Math.ceil(damage / 5.0);
        int damPerHit = 5;
        for (int i = 0; i < hits; i++) {
            int loc = Compute.randomInt(4);
            // Fix for #587. Apply in 5 point groups unless damage remainder is less.
            setArmor(getArmor(loc) - Math.min(damPerHit, damage), loc);
            // We did too much damage, so we need to damage the SI, but we won't reduce the SI below 1 here unless
            // the fighter is destroyed.
            if (getArmor(loc) < 0) {
                if (getSI() > 1) {
                    int si = getSI() + (getArmor(loc) / 2);
                    si = Math.max(si, isDestroyed() || isDoomed() ? 0 : 1);
                    setSI(si);
                }
                setArmor(0, loc);
            }
            damage -= damPerHit;
        }
    }

    /**
     * @return The total number of crew available to supplement marines onboarding actions. Includes officers, enlisted,
     *       and bay personnel, but not marines/ba or passengers.
     */
    @Override
    public int getNCrew() {
        return 1;
    }

    /**
     * @return The total number of officers for vessels.
     */
    public int getNOfficers() {
        return 0;
    }

    /**
     * @return The total number of gunners for vessels.
     */
    public int getNGunners() {
        return 0;
    }

    /**
     * @return number of passengers on this unit Intended for spacecraft, where we want to get the crews of transported
     *       units plus actual passengers assigned to quarters
     */
    @Override
    public int getNPassenger() {
        return 0;
    }

    /**
     * @return Set of Entity IDs used by this ship as escape craft
     */
    public Set<String> getEscapeCraft() {
        return escapeCraftList;
    }

    /**
     * Adds an Escape Craft. Used by MHQ to track where escaped crew and passengers end up.
     *
     * @param id The Entity ID of the ship to add.
     */
    public void addEscapeCraft(String id) {
        escapeCraftList.add(id);
    }

    /**
     * @return The number battle armored marines available to vessels for boarding actions.
     */
    public int getNBattleArmor() {
        return 0;
    }

    @Override
    public int getNMarines() {
        return 0;
    }

    /**
     * @return Map of unique individuals being transported as marines
     */
    public Map<UUID, Integer> getMarines() {
        return marines;
    }

    /**
     * @return number of marines assigned to a unit Used for abandoning a unit
     */
    public int getMarineCount() {
        return 0;
    }

    /**
     * @return The number of escape pods carried by the unit
     */
    public int getEscapePods() {
        return 0;
    }

    /**
     * @return the number of escape pods remaining
     */
    public int getPodsLeft() {
        return getEscapePods() - getLaunchedEscapePods();
    }

    /**
     * @return The number of lifeboats carried by the unit
     */
    public int getLifeBoats() {
        return 0;
    }

    /**
     * Returns the total number of escape pods launched so far
     */
    public int getLaunchedEscapePods() {
        return 0;
    }

    /**
     * Updates the total number of escape pods launched so far
     *
     * @param n The number to change
     */
    public void setLaunchedEscapePods(int n) {
    }

    /**
     * Returns the total number of lifeboats launched so far
     */
    public int getLaunchedLifeBoats() {
        return 0;
    }

    /**
     * @return return the number of lifeboats remaining
     */
    public int getLifeBoatsLeft() {
        return getLifeBoats() - getLaunchedLifeBoats();
    }

    /**
     * Updates the total number of lifeboats launched so far
     *
     * @param n The number to change
     */
    public void setLaunchedLifeBoats(int n) {
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO;
    }

    @Override
    public boolean isAero() {
        return true;
    }

    /**
     * Fighters may carry external ordnance; Other Aerospace units with cargo bays and the Internal Bomb Bay quirk may
     * carry bombs internally.
     *
     * @return boolean
     */
    @Override
    public boolean isBomber() {
        return false;
    }

    @Override
    public int availableBombLocation(int cost) {
        return LOC_NOSE;
    }

    @Override
    public int getSpriteDrawPriority() {
        return 10;
    }

    @Override
    public List<WeaponMounted> getActiveAMS() {
        // Large craft use AMS and Point Defense bays
        if (isLargeCraft()) {
            List<WeaponMounted> ams = new ArrayList<>();
            for (WeaponMounted weapon : getWeaponBayList()) {
                // Skip anything that's not an AMS, AMS Bay or Point Defense Bay
                if (!weapon.getType().hasFlag(WeaponType.F_AMS) &&
                      !weapon.getType().hasFlag(WeaponType.F_AMS_BAY) &&
                      !weapon.getType().hasFlag(WeaponType.F_PD_BAY)) {
                    continue;
                }

                // Make sure the AMS is good to go
                if (!weapon.isReady() ||
                      weapon.isMissing() ||
                      weapon.curMode().equals("Off") ||
                      weapon.curMode().equals("Normal")) {
                    continue;
                }

                // AMS blocked by transported units can not fire
                if (isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted())) {
                    continue;
                }

                // Make sure ammo is loaded
                for (WeaponMounted bayW : weapon.getBayWeapons()) {
                    AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
                    if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY)) &&
                          ((bayWAmmo == null) || (bayWAmmo.getUsableShotsLeft() == 0) || bayWAmmo.isDumping())) {
                        loadWeapon(weapon);
                        bayWAmmo = weapon.getLinkedAmmo();
                    }

                    // try again
                    if (!(weapon.getType().hasFlag(WeaponType.F_ENERGY))) {
                        if ((bayWAmmo != null)) {
                            bayWAmmo.getUsableShotsLeft();
                        }
                    }// No ammo for this AMS.
                }
                ams.add(weapon);
            }
            return ams;
        }
        // ASFs and Small Craft should use regular old AMS...
        return super.getActiveAMS();
    }

    /**
     * A method to add/remove sensors that only work in space as we transition in and out of an atmosphere
     */
    @Override
    public void updateSensorOptions() {
        // Prevent adding duplicates
        boolean hasSpacecraftThermal = false;
        boolean hasAeroThermal = false;
        boolean hasESM = false;

        for (Sensor sensor : getSensors()) {
            if (sensor.type() == Sensor.TYPE_SPACECRAFT_THERMAL) {
                hasSpacecraftThermal = true;
            }
            if (sensor.type() == Sensor.TYPE_AERO_THERMAL) {
                hasAeroThermal = true;
            }
            if (sensor.type() == Sensor.TYPE_SPACECRAFT_ESM) {
                hasESM = true;
            }
        }

        // Remove everything but Radar if we're not in space
        if (!isSpaceborne()) {
            Vector<Sensor> sensorsToRemove = new Vector<>();
            if (hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                for (Sensor sensor : getSensors()) {
                    if (sensor.type() == Sensor.TYPE_SPACECRAFT_ESM) {
                        hasESM = false;
                        sensorsToRemove.add(sensor);
                    }
                    if (sensor.type() == Sensor.TYPE_SPACECRAFT_THERMAL) {
                        hasSpacecraftThermal = false;
                        sensorsToRemove.add(sensor);
                    }
                }
            } else if (hasETypeFlag(Entity.ETYPE_AERO)) {
                for (Sensor sensor : getSensors()) {
                    if (sensor.type() == Sensor.TYPE_AERO_THERMAL) {
                        hasAeroThermal = false;
                        sensorsToRemove.add(sensor);
                    }
                }
            }
            getSensors().removeAll(sensorsToRemove);
            if (!sensorsToRemove.isEmpty()) {
                setNextSensor(getSensors().firstElement());
            }
        }

        // If we are in space, add them back...
        if (isSpaceborne()) {
            if (hasETypeFlag(Entity.ETYPE_DROPSHIP) ||
                  hasETypeFlag(Entity.ETYPE_SPACE_STATION) ||
                  hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
                  hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                // Large craft get thermal/optical sensors
                if (!hasSpacecraftThermal) {
                    getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_THERMAL));
                }
                // Only military craft get ESM, which detects active radar
                if (getDesignType() == Aero.MILITARY) {
                    if (!hasESM) {
                        getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_ESM));
                    }
                }
            } else if (hasETypeFlag(Entity.ETYPE_AERO) || hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
                // ASFs and small craft get thermal/optical sensors
                if (!hasAeroThermal) {
                    getSensors().add(new Sensor(Sensor.TYPE_AERO_THERMAL));
                }
            }
        }
    }

    // auto-ejection methods

    /**
     * @return unit has an ejection seat
     */
    public boolean hasEjectSeat() {
        return !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);
    }

    /**
     * @return Returns the autoEject.
     */
    public boolean isAutoEject() {
        return autoEject && hasEjectSeat();
    }

    /**
     * @param autoEject Turn the master auto-ejection system on or off
     */
    public void setAutoEject(boolean autoEject) {
        this.autoEject = autoEject;
    }

    /**
     * @return Is Auto-Ejection enabled for ammo explosions?
     */
    public boolean isCondEjectAmmo() {
        return condEjectAmmo;
    }

    /**
     * Used by Conditional Auto Ejection - will we eject when an ammo explosion is triggered?
     *
     * @param condEjectAmmo Sets auto-ejection for ammo explosions
     */
    public void setCondEjectAmmo(boolean condEjectAmmo) {
        this.condEjectAmmo = condEjectAmmo;
    }

    /**
     * @return Is auto-ejection enabled for fuel explosions?
     */
    public boolean isCondEjectFuel() {
        return condEjectFuel;
    }

    /**
     * Used by Conditional Auto Ejection - will we eject when a fuel explosion is triggered?
     *
     * @param condEjectFuel Sets auto-ejection for fuel tank explosions
     */
    public void setCondEjectFuel(boolean condEjectFuel) {
        this.condEjectFuel = condEjectFuel;
    }

    /**
     * @return Is auto-ejection enabled for SI destruction (Fighter only)?
     */
    public boolean isCondEjectSIDest() {
        return condEjectSIDest;
    }

    /**
     * Used by Conditional Auto Ejection - will we eject when structural integrity is reduced to 0?
     *
     * @param condEjectSIDest Sets auto-ejection for structural integrity destruction
     */
    public void setCondEjectSIDest(boolean condEjectSIDest) {
        this.condEjectSIDest = condEjectSIDest;
    }

    /**
     * Intended for large craft.
     *
     * @return Indicates that the ship is being abandoned.
     */
    public boolean isEjecting() {
        return ejecting;
    }

    /**
     * Changes the ejecting flag when the order to abandon ship is given
     *
     * @param ejecting Change to the ejecting status of this ship
     */
    public void setEjecting(boolean ejecting) {
        this.ejecting = ejecting;
    }

    @Override
    public int getEnginesLostRound() {
        return this.enginesLostRound;
    }

    @Override
    public void setEnginesLostRound(int enginesLostRound) {
        this.enginesLostRound = enginesLostRound;
    }

    @Override
    public boolean isFlyingOff() {
        return flyingOff != OffBoardDirection.NONE;
    }

    @Override
    public void setFlyingOff(OffBoardDirection direction) {
        this.flyingOff = direction;
    }

    @Override
    public OffBoardDirection getFlyingOffDirection() {
        return this.flyingOff;
    }
}
