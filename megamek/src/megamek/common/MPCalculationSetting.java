package megamek.common;

/**
 * This class represents a setting used to calculate walk, run, jump or sprint MP. The setting contains
 * info on whether to include gravity, MASC, weather, current heat and other circumstances in the MP
 * calculation. All MPCalculationSettings can be used for all calculations though some settings may have
 * no special effect; e.g. checking for submerged jump jets doesn't change the walk MP result.
 *
 * @implNote This class is immutable.
 */
public class MPCalculationSetting {

    /**
     * The standard in-game setting, taking into account every circumstance except submerged jump jets
     * (meaning that submerged jump jets will not reduce the result of jump MP calculation).
     */
    public static final MPCalculationSetting STANDARD = new Builder().build();

    /** A setting that excludes heat effects on MP. */
    public static final MPCalculationSetting NO_HEAT = new Builder().noHeat().build();

    /** A setting that excludes myomer boosters on BA/PM. */
    public static final MPCalculationSetting NO_MYOMERBOOSTER = new Builder().noMyomerBooster().build();

    /** A setting that excludes MASC effects on run MP. */
    public static final MPCalculationSetting NO_MASC = new Builder().noMASC().build();

    /** A setting that excludes gravity effects on MP. */
    public static final MPCalculationSetting NO_GRAVITY = new Builder().noGravity().build();

    /** A setting that includes the effect of at most a single MASC on run MP. */
    public static final MPCalculationSetting ONE_MASC = new Builder().singleMASC().build();

    /** A setting that excludes the effects of being grounded for Aeros. */
    public static final MPCalculationSetting NO_GROUNDED = new Builder().noGrounded().build();

    /** A setting for checking if a unit moved too fast under the effects of low gravity. */
    public static final MPCalculationSetting SAFE_MOVE = new Builder().noGravity().noHeat().noModularArmor().build();

    /**
     * A setting for testing if a unit is permanently immobilized. It excludes the effects of
     * being grounded for Aeros and the effects of heat.
     */
    public static final MPCalculationSetting PERM_IMMOBILIZED = new Builder().noGrounded().noHeat().build();

    /** A setting that reduces calculated jump MP for submerged jump jets. */
    public static final MPCalculationSetting DISCOUNT_SUBMERGED_JJ = new Builder().discountSubmergedJumpJets().build();

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
            .noMASC().noHeat().noCargo().noMyomerBooster().noDWP().noGrounded().noOptionalRules()
            .noConversion().noModularArmor().forceTSM().build();

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

    /** @return A calculation setting that includes all effects on MP. (Use: in-game movement) */
    public static MPCalculationSetting fullInGameSetting() {
        return new Builder().build();
    }

    /** @return A calculation setting that excludes only gravity. (Use: e.g. gravity-related PSRs) */
    public static MPCalculationSetting noGravitySetting() {
        return new Builder().noGravity().build();
    }


    /** @return A calculation setting that excludes only Myomer Booster effects. (Use: Test if MyB was used) */
    public static MPCalculationSetting noMyomerBoosterSetting() {
        return new Builder().noMyomerBooster().build();
    }

    /** @return A calculation setting that excludes scenario effects. (Use: Alpha Strike conversion) */
    public static MPCalculationSetting alphaStrikeConversionSetting() {
        return new Builder().noGravity().noWeather().noHeat().noCargo().noConversion().build();
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

        private Builder noHeat() {
            ignoreHeat = true;
            return this;
        }

        private Builder noGravity() {
            ignoreGravity = true;
            return this;
        }

        private Builder noMASC() {
            ignoreMASC = true;
            singleMASC = false;
            return this;
        }

        private Builder noDWP() {
            ignoreBADWP = true;
            return this;
        }

        private Builder baNoBurden() {
            ignoreBABurden = true;
            return this;
        }

        private Builder noMyomerBooster() {
            ignoreMyomerBooster = true;
            return this;
        }

        private Builder noCargo() {
            ignoreCargo = true;
            return this;
        }

        private Builder noWeather() {
            ignoreWeather = true;
            return this;
        }

        private Builder noModularArmor() {
            ignoreModularArmor = true;
            return this;
        }

        private Builder singleMASC() {
            singleMASC = true;
            ignoreMASC = false;
            return this;
        }

        private Builder discountSubmergedJumpJets() {
            ignoreSubmergedJumpJets = false;
            return this;
        }

        private Builder noGrounded() {
            ignoreGrounded = true;
            return this;
        }

        private Builder noOptionalRules() {
            ignoreOptionalRules = true;
            return this;
        }

        private Builder noConversion() {
            ignoreConversion = true;
            return this;
        }

        private Builder forceTSM() {
            forceTSM = true;
            return this;
        }
    }


}
