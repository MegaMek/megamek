/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

/**
 * This class represents a setting used to calculate walk, run, jump or sprint MP. The setting contains
 * info on whether to include gravity, MASC, weather, current heat and other circumstances in the MP
 * calculation. All MPCalculationSettings can be used for all calculations though some settings may have
 * no special effect; e.g. checking for submerged jump jets will not ever change the walk MP result.
 * The baseline setting for in-game calculations is {@link #STANDARD}.
 *
 * @implNote This class is immutable.
 */
public class MPCalculationSetting {

    /**
     * The standard in-game setting, taking into account every circumstance except submerged jump jets
     * (in this setting, submerged jump jets will not reduce the result of jump MP calculation).
     */
    public static final MPCalculationSetting STANDARD = new Builder().build();

    /** A setting that excludes heat effects on MP. */
    public static final MPCalculationSetting NO_HEAT = new Builder().noHeat().build();

    /** A setting that excludes myomer booster effects on BA/PM. */
    public static final MPCalculationSetting NO_MYOMERBOOSTER = new Builder().noMyomerBooster().build();

    /** A setting that excludes MASC effects on run MP. */
    public static final MPCalculationSetting NO_MASC = new Builder().noMASC().build();

    /** A setting that excludes gravity effects on MP. (Use: e.g. gravity-related PSRs) */
    public static final MPCalculationSetting NO_GRAVITY = new Builder().noGravity().build();

    /** A setting that includes the effect of at most a single MASC on run MP. */
    public static final MPCalculationSetting ONE_MASC = new Builder().singleMASC().build();

    /** A setting that excludes the effects of being grounded for Aeros. */
    public static final MPCalculationSetting NO_GROUNDED = new Builder().noGrounded().build();

    /** A setting for checking if a unit moved too fast under the effects of low gravity. */
    public static final MPCalculationSetting SAFE_MOVE = new Builder().noGravity().noHeat().noModularArmor().build();

    /**
     * A setting for testing if a unit is permanently immobilized. It excludes transient effects such as
     * being grounded for Aeros and the effects of heat and cargo.
     */
    public static final MPCalculationSetting PERM_IMMOBILIZED = new Builder().noGrounded().noHeat().noCargo().build();

    /** A setting that reduces calculated jump MP for submerged jump jets. */
    public static final MPCalculationSetting DEDUCT_SUBMERGED_JJ = new Builder().deductSubmergedJumpJets().build();

    /** A setting for finding full MP on BA, disregarding burdened status and DWP (Use: printing BA record sheets). */
    public static final MPCalculationSetting BA_UNBURDENED = new Builder().noHeat().baNoBurden().noDWP().build();

    /**
     * The setting for Alpha Strike conversion, excluding scenario circumstances as well as
     * myomer boosters, DWP, BA burden and modular armor.
     */
    public static final MPCalculationSetting AS_CONVERSION = new Builder().noGravity().noWeather()
            .noHeat().noCargo().baNoBurden().noMyomerBooster().noDWP().noModularArmor().noGrounded()
            .noConversion().noOptionalRules().build();

    /**
     * The setting for Battle Value calculation, excluding scenario circumstances and heat as well as
     * myomer boosters, DWP, MASC, Cargo and Bombs and grounded state of Aeros.
     */
    public static final MPCalculationSetting BV_CALCULATION = new Builder().noGravity().noWeather()
            .noHeat().noCargo().noDWP().noGrounded().noOptionalRules()
            .noConversion().noModularArmor().forceTSM().baNoBurden().build();

    public final boolean ignoreGravity;
    public final boolean ignoreHeat;
    public final boolean ignoreModularArmor;
    public final boolean ignoreDWP;
    public final boolean ignoreMyomerBooster;
    public final boolean ignoreMASC;
    public final boolean ignoreBurden;
    public final boolean ignoreCargo;
    public final boolean ignoreWeather;
    public final boolean ignoreGrounded;
    public final boolean ignoreOptionalRules;
    public final boolean ignoreConversion;
    public final boolean singleMASC;
    public final boolean ignoreSubmergedJumpJets;
    public final boolean forceTSM;

    private MPCalculationSetting(boolean ignoreGravity, boolean ignoreHeat, boolean ignoreModularArmor,
                                 boolean ignoreMASC, boolean ignoreMyomerBooster, boolean ignoreDWP,
                                 boolean ignoreBurden, boolean ignoreCargo, boolean ignoreWeather,
                                 boolean singleMASC, boolean ignoreSubmergedJumpJets, boolean ignoreGrounded,
                                 boolean ignoreOptionalRules, boolean ignoreConversion, boolean forceTSM) {
        this.ignoreGravity = ignoreGravity;
        this.ignoreHeat = ignoreHeat;
        this.ignoreModularArmor = ignoreModularArmor;
        this.ignoreMASC = ignoreMASC;
        this.ignoreMyomerBooster = ignoreMyomerBooster;
        this.ignoreDWP = ignoreDWP;
        this.ignoreBurden = ignoreBurden;
        this.ignoreCargo = ignoreCargo;
        this.ignoreWeather = ignoreWeather;
        this.singleMASC = singleMASC;
        this.ignoreSubmergedJumpJets = ignoreSubmergedJumpJets;
        this.ignoreGrounded = ignoreGrounded;
        this.ignoreOptionalRules = ignoreOptionalRules;
        this.ignoreConversion = ignoreConversion;
        this.forceTSM = forceTSM;
    }

    private static class Builder {

        private boolean ignoreGravity = false;
        private boolean ignoreHeat = false;
        private boolean ignoreModularArmor = false;
        private boolean ignoreBADWP = false;
        private boolean ignoreMyomerBooster = false;
        private boolean ignoreMASC = false;
        private boolean ignoreBABurden = false;
        private boolean ignoreCargo = false;
        private boolean ignoreWeather = false;
        private boolean singleMASC = false;
        private boolean ignoreSubmergedJumpJets = true;
        private boolean ignoreGrounded = false;
        private boolean ignoreOptionalRules = false;
        private boolean ignoreConversion = false;
        private boolean forceTSM = false;

        MPCalculationSetting build() {
            return new MPCalculationSetting(ignoreGravity, ignoreHeat, ignoreModularArmor, ignoreMASC,
                    ignoreMyomerBooster, ignoreBADWP, ignoreBABurden, ignoreCargo, ignoreWeather, singleMASC,
                    ignoreSubmergedJumpJets, ignoreGrounded, ignoreOptionalRules, ignoreConversion, forceTSM);
        }

        /** Disregards the effects of the current heat of the unit. */
        private Builder noHeat() {
            ignoreHeat = true;
            return this;
        }

        /** Disregards the effects of gravity. */
        private Builder noGravity() {
            ignoreGravity = true;
            return this;
        }

        /** Disregards the effects of MASC systems. */
        private Builder noMASC() {
            ignoreMASC = true;
            singleMASC = false;
            return this;
        }

        /** Disregards the effects of DWP on BA. */
        private Builder noDWP() {
            ignoreBADWP = true;
            return this;
        }

        /** Disregards the effects of being burdened by equipment that can be jettisoned on BA. */
        private Builder baNoBurden() {
            ignoreBABurden = true;
            return this;
        }

        /** Disregards the effects of myomer boosters on BA and PM. */
        private Builder noMyomerBooster() {
            ignoreMyomerBooster = true;
            return this;
        }

        /** Disregards the effects of carried or towed cargo and units, including bomb load. */
        private Builder noCargo() {
            ignoreCargo = true;
            return this;
        }

        /** Disregards the effects of wind, temperature and other planetary conditions (but not gravity). */
        private Builder noWeather() {
            ignoreWeather = true;
            return this;
        }

        /** Disregards the effects of modular armor. */
        private Builder noModularArmor() {
            ignoreModularArmor = true;
            return this;
        }

        /** Allows the effects of at most one MASC system if the unit has any MASC. */
        private Builder singleMASC() {
            singleMASC = true;
            ignoreMASC = false;
            return this;
        }

        /** Reduces jump MP for submerged jump jets. Does not affect any other MP calculation. */
        private Builder deductSubmergedJumpJets() {
            ignoreSubmergedJumpJets = false;
            return this;
        }

        /** Ignores the effects of flying units being grounded (considers them airborne). */
        private Builder noGrounded() {
            ignoreGrounded = true;
            return this;
        }

        /** Ignores some baseline movement-enhancing optional rules (Use: BV calculation). */
        private Builder noOptionalRules() {
            ignoreOptionalRules = true;
            return this;
        }

        /** Ignores the current conversion state of the entity and considers it to be in its base state. */
        private Builder noConversion() {
            ignoreConversion = true;
            return this;
        }

        /** Applies the effects of TSM (if the entity has it!) regardless of the current heat of the entity (Use: BV calculation). */
        private Builder forceTSM() {
            forceTSM = true;
            return this;
        }
    }
}