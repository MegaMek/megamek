/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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

import static java.util.stream.Collectors.toList;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.CompositeTechLevel;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.Messages;
import megamek.common.RangeType;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.cost.InfantryCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.GamePhase;
import megamek.common.enums.ProstheticEnhancementType;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.interfaces.ITechnology;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Wind;
import megamek.common.rolls.TargetRoll;
import megamek.common.verifier.TestInfantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;

/**
 * This class represents Conventional Infantry. The lowest of the low, the ground pounders, the city rats, the PBI (Poor
 * Bloody Infantry).
 */
public class ConvInfantry extends Infantry {

    private static final MMLogger logger = MMLogger.create(ConvInfantry.class);

    public static final int LOC_INFANTRY = 0;
    public static final int LOC_FIELD_GUNS = 1;

    // Infantry only have critical slots for field gun ammo

    private static final String[] LOCATION_ABBREVIATIONS = { "TPRS", "FGUN" };
    private static final String[] LOCATION_NAMES = { "Troopers", "Field Guns" };


    // Infantry Specializations
    public static int BRIDGE_ENGINEERS = 1;
    public static int DEMO_ENGINEERS = 1 << 1;
    public static int FIRE_ENGINEERS = 1 << 2;
    public static int MINE_ENGINEERS = 1 << 3;
    public static int SENSOR_ENGINEERS = 1 << 4;
    public static int TRENCH_ENGINEERS = 1 << 5;
    public static int MARINES = 1 << 6;
    public static int MOUNTAIN_TROOPS = 1 << 7;
    public static int PARAMEDICS = 1 << 8;
    public static int PARATROOPS = 1 << 9;
    public static int TAG_TROOPS = 1 << 10;
    public static int XCT = 1 << 11;
    public static int SCUBA = 1 << 12;
    public static int NUM_SPECIALIZATIONS = 13;
    public static int COMBAT_ENGINEERS = BRIDGE_ENGINEERS
          | DEMO_ENGINEERS
          | FIRE_ENGINEERS
          | MINE_ENGINEERS
          | SENSOR_ENGINEERS
          | TRENCH_ENGINEERS;

    // Information on primary and secondary weapons. This must be kept separate from
    // the equipment array
    // because they are not fired as separate weapons
    private InfantryWeapon primaryWeapon;
    private String primaryName;
    private InfantryWeapon secondaryWeapon;
    private String secondName;
    private int secondaryWeaponsPerSquad = 0;

    // Number of rounds this platoon's energy weapons are rendered inoperative by an Improved Magnetic
    // Pulse (iATM IMP) missile hit (IO IMP rules). Set to 2 on hit so the effect lasts through the End
    // Phase of the following turn.
    private int impEnergyWeaponsDisabledRounds = 0;

    // Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing): a one-shot weapon carried by every trooper, used for
    // a single once-per-scenario attack instead of the platoon's standard weapon attack. Unlike primary/secondary, the
    // disposable weapon IS added to the equipment array as a separate, fireable WeaponMounted. disposableWeapon is
    // transient: an EquipmentType cannot be relied on to survive the entity's client/server serialization (it is
    // referenced by name from the registry, not serialized directly). It is reconstructed from disposableName, which
    // does survive, by getDisposableWeapon().
    private transient InfantryWeapon disposableWeapon;
    private String disposableName;

    private InfantryMount mount = null;

    // Armor
    private double customArmorDamageDivisor = 1.0;
    private boolean encumbering = false;
    private boolean spaceSuit = false;
    private boolean dest = false;
    private boolean sneak_camo = false;
    private boolean sneak_ir = false;
    private boolean sneak_ecm = false;

    /** The active specializations of this platoon. */
    private int infSpecs = 0;

    /**
     * Firefighting engineers (FIRE_ENGINEERS): the hex this platoon last fought a fire in, the round it did so, and how
     * many consecutive rounds it has fought that same hex. Used for the cumulative -1 target number per consecutive
     * turn of firefighting (TO:AuE p.153, minimum target number 3).
     */
    private Coords lastFirefightCoords = null;
    private int lastFirefightRound = -1;
    private int consecutiveFirefightTurns = 0;

    /**
     * For mechanized VTOL infantry, stores whether the platoon are microlite troops, which need to enter a hex every
     * turn to remain in flight.
     */
    private boolean isMicrolite = false;

    /** Base VTOL MP for a Microlite VTOL platoon (TO:AUE p.136). */
    private static final int MICROLITE_VTOL_MP = 6;
    /** Base VTOL MP for a Micro-Copter VTOL platoon (TO:AUE p.136). */
    private static final int MICRO_COPTER_VTOL_MP = 5;

    private boolean pheromoneImpaired = false;

    public static final int ANTI_MEK_SKILL_NO_GEAR = 8;

    // Prosthetic Enhancement (Enhanced Limbs) - IO p.84
    // Standard Enhanced (MD_PL_ENHANCED): Uses slot 1 only, same type up to 2x
    // Improved Enhanced (MD_PL_I_ENHANCED): Uses both slots, different types, each up to 2x, max 4 total
    private ProstheticEnhancementType prostheticEnhancement1 = null;
    private int prostheticEnhancement1Count = 0; // 0, 1, or 2 per trooper
    private ProstheticEnhancementType prostheticEnhancement2 = null;
    private int prostheticEnhancement2Count = 0; // 0, 1, or 2 per trooper

    // Extraneous (Enhanced) Limbs - IO p.84
    // MD_PL_EXTRA_LIMBS: Up to 2 pairs of extra limbs (4 limbs total)
    // Each pair provides 2 identical enhancement items (1 per limb)
    // Items in a pair must be same type but can differ from other prosthetics
    // Conventional infantry only
    private ProstheticEnhancementType extraneousPair1 = null; // Always 2 items per pair
    private ProstheticEnhancementType extraneousPair2 = null; // Always 2 items per pair

    /** The maximum number of troopers in an infantry platoon. */
    public static final int INF_PLT_MAX_MEN = 30;

    /**
     * Generate a new, blank, infantry platoon. Hopefully, we'll be loaded from somewhere.
     */
    public ConvInfantry() {
        super();
        // Create a "dead" leg rifle platoon.
        originalTrooperCount = 0;
        troopersShooting = 0;
        activeTroopers = 0;
        setMovementMode(EntityMovementMode.INF_LEG);
        setOriginalWalkMP(1);
    }

    @Override
    public boolean isMechanized() {
        return isMounted() ? false : super.isMechanized();
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
    public int locations() {
        return 2;
    }

    @Override
    public int getMaxElevationChange() {
        return hasSpecialization(MOUNTAIN_TROOPS) ? 3 : super.getMaxElevationChange();
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel techLevel) {
        super.addSystemTechAdvancement(techLevel);
        techLevel.addComponent(getMotiveTechAdvancement(), getMovementModeAsString());
        if (hasSpecialization(COMBAT_ENGINEERS)) {
            techLevel.addComponent(getCombatEngineerTA(), getSpecializationName(COMBAT_ENGINEERS));
        }
        if (hasSpecialization(MARINES)) {
            techLevel.addComponent(getMarineTA(), getSpecializationName(MARINES));
        }
        if (hasSpecialization(MOUNTAIN_TROOPS)) {
            techLevel.addComponent(getMountainTA(), getSpecializationName(MOUNTAIN_TROOPS));
        }
        if (hasSpecialization(PARATROOPS)) {
            techLevel.addComponent(getParatrooperTA(), getSpecializationName(PARATROOPS));
        }
        if (hasSpecialization(PARAMEDICS)) {
            techLevel.addComponent(getParamedicTA(), getSpecializationName(PARAMEDICS));
        }
        if (hasSpecialization(TAG_TROOPS)) {
            techLevel.addComponent(getTAGTroopsTA(), getSpecializationName(TAG_TROOPS));
        }
    }


    /**
     * Returns true if this unit carries anti-mek gear. Only with this gear can it improve its anti-mek skill below 8.
     *
     * @return True when this infantry carries anti-mek gear
     */
    public boolean hasAntiMekGear() {
        return hasWorkingMisc(MiscType.F_ANTI_MEK_GEAR);
    }

    @Override
    public boolean canMakeAntiMekAttacks() {
        return !isMechanized() && !isArmorEncumbering() && !hasActiveFieldWeapon();
    }


    @Override
    public boolean canAssaultDrop() {
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_PARATROOPERS)) {
            return true;
        }

        EntityMovementMode moveMode = getMovementMode();
        boolean hasInherentDropCapability = List.of(EntityMovementMode.INF_JUMP,
              EntityMovementMode.HOVER,
              EntityMovementMode.VTOL).contains(moveMode);
        boolean hasGliderWings = isConventionalInfantry()
              && hasAbility(OptionsConstants.MD_PL_GLIDER)
              && canUseGliderWings();
        boolean hasPoweredFlightWings = hasPoweredFlightWings() && canUsePoweredFlightWings();

        return hasInherentDropCapability || hasSpecialization(PARATROOPS) || hasGliderWings || hasPoweredFlightWings;
    }


    /**
     * Returns true if this conventional infantry unit has powered flight wings. Powered flight wings can only be used
     * by conventional infantry (IO p.85).
     *
     * @return true if unit has powered flight wings ability
     */
    public boolean hasPoweredFlightWings() {
        return hasAbility(OptionsConstants.MD_PL_FLIGHT);
    }


    /**
     * Returns the VTOL movement points provided by powered flight wings. Per IO p.85, powered flight wings provide 2
     * MPs of VTOL movement.
     *
     * @return 2 if powered flight is usable, 0 otherwise
     */
    public int getPoweredFlightMP() {
        if (hasPoweredFlightWings() && canUsePoweredFlightWings()) {
            return 2;
        }
        return 0;
    }

    /**
     * Returns true if this infantry unit can use VTOL-style movement. This includes infantry with VTOL movement mode
     * (microcopter/microlite) OR infantry with usable powered flight wings.
     *
     * @return true if unit can use VTOL movement
     */
    public boolean hasVTOLMovementCapability() {
        if (getMovementMode() == EntityMovementMode.VTOL) {
            return true;
        }
        return hasPoweredFlightWings() && canUsePoweredFlightWings();
    }


    /**
     * Returns the VTOL movement points for this infantry unit. For VTOL infantry (microcopter/microlite), this returns
     * jumpMP. For powered flight infantry, this returns 2 MP per IO p.85.
     *
     * @return VTOL MP available, or 0 if no VTOL capability
     */
    public int getVTOLMP() {
        return getMovementMode().isVTOL() ? getJumpMP() : getPoweredFlightMP();
    }


    /**
     * Returns true if this infantry unit is protected from fall damage. Glider wings and powered flight wings protect
     * against damage from falls, whether from walking off terrain 2+ levels high (including buildings) or by
     * displacement (IO p.85). Only conventional infantry can use these prosthetic wings.
     *
     * @return true if protected from fall damage
     */
    public boolean isProtectedFromFallDamage() {
        if (!isConventionalInfantry()) {
            return false;
        }
        boolean hasGliderProtection = hasAbility(OptionsConstants.MD_PL_GLIDER) && canUseGliderWings();
        boolean hasPoweredFlightProtection = hasPoweredFlightWings() && canUsePoweredFlightWings();
        return hasGliderProtection || hasPoweredFlightProtection;
    }


    public boolean isXCT() {
        return hasSpecialization(XCT);
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0;

        // Anti-Mek Trained Multiplier
        if (hasAntiMekGear()) {
            priceMultiplier *= 5.0;
        }

        // Motive type costs
        switch (getMovementMode()) {
            case INF_UMU:
                priceMultiplier *= getAllUMUCount() > 1 ? 2.5 : 2;
                break;
            case INF_LEG:
                priceMultiplier *= 1.0;
                break;
            case INF_MOTORIZED:
                priceMultiplier *= 1.6;
                break;
            case INF_JUMP:
                priceMultiplier *= 2.6;
                break;
            case HOVER:
            case WHEELED:
            case TRACKED:
            case SUBMARINE: // No cost given in TacOps, using basic mechanized cost for now
                priceMultiplier *= 3.2;
                break;
            case VTOL:
                priceMultiplier *= hasMicrolite() ? 4 : 4.5;
                break;
            default:
                break;
        }

        // Specialization costs
        if (hasSpecialization(COMBAT_ENGINEERS)) {
            priceMultiplier *= 5;
        }
        if (hasSpecialization(MARINES)) {
            priceMultiplier *= 3;
        }
        if (hasSpecialization(MOUNTAIN_TROOPS)) {
            priceMultiplier *= 2;
        }
        if (hasSpecialization(PARATROOPS)) {
            priceMultiplier *= 3;
        }
        if (hasSpecialization(XCT)) {
            priceMultiplier *= 5;
        }
        // TODO : paramedics cost an addition x0.375 per paramedic
        return priceMultiplier;
    }

    public boolean hasSpaceSuit() {
        return spaceSuit;
    }

    public void setSpaceSuit(boolean b) {
        spaceSuit = b;
    }

    public boolean hasDEST() {
        return dest;
    }

    public void setDEST(boolean b) {
        dest = b;
    }

    public boolean hasSpecialization() {
        return infSpecs != 0;
    }

    public boolean hasSpecialization(int spec) {
        return (infSpecs & spec) > 0;
    }

    /** @return {@code true} if this platoon are firefighting engineers (FIRE_ENGINEERS specialization). */
    @Override
    public boolean isFirefighter() {
        return hasSpecialization(FIRE_ENGINEERS);
    }

    /**
     * Number of consecutive prior rounds this platoon has already spent fighting a fire in the given hex, used for the
     * cumulative firefighting target-number reduction (TO:AuE p.153). Returns 0 when the platoon did not fight this same
     * hex on the immediately preceding round.
     *
     * @param coords the burning hex being targeted
     * @param round  the current game round
     *
     * @return the prior consecutive firefighting streak for this hex, or 0 if the streak is broken
     */
    public int getPriorFirefightStreak(Coords coords, int round) {
        if ((coords != null) && coords.equals(lastFirefightCoords) && (lastFirefightRound == round - 1)) {
            return consecutiveFirefightTurns;
        }
        return 0;
    }

    /**
     * Records that this platoon fought a fire in the given hex on the given round, advancing the consecutive-turn
     * streak. The streak resets when the platoon did not fight this same hex on the immediately preceding round (TO:AR
     * p.53: a platoon that stops fighting a blaze starts over).
     *
     * @param coords the burning hex that was fought
     * @param round  the current game round
     */
    public void recordFirefight(Coords coords, int round) {
        if ((coords != null) && coords.equals(lastFirefightCoords) && (lastFirefightRound == round - 1)) {
            consecutiveFirefightTurns++;
        } else {
            consecutiveFirefightTurns = 1;
        }
        lastFirefightCoords = coords;
        lastFirefightRound = round;
    }

    public int getSpecializations() {
        return infSpecs;
    }

    public void setSpecializations(int spec) {
        // Engineer specializations come with their tools; add or remove them with the specialization
        updateEngineerEquipment(spec, TRENCH_ENGINEERS, MiscTypeFlag.S_VIBRO_SHOVEL, EquipmentTypeLookup.VIBRO_SHOVEL);
        updateEngineerEquipment(spec, DEMO_ENGINEERS, MiscTypeFlag.S_DEMOLITION_CHARGE,
              EquipmentTypeLookup.DEMOLITION_CHARGE);
        updateEngineerEquipment(spec, BRIDGE_ENGINEERS, MiscTypeFlag.S_BRIDGE_KIT,
              EquipmentTypeLookup.INFANTRY_BRIDGE_KIT);

        // Equipment for Firefighting Engineers: a Fire Extinguisher the platoon selects and fires in place
        // of a weapon attack (TO:AuE p.153). It carries the F_SOLO_ATTACK flag, so firing it stops the platoon
        // also firing their small arms that turn. It is a weapon (not a MiscType tool), so it is handled here
        // rather than by updateEngineerEquipment.
        if ((spec & FIRE_ENGINEERS) > 0 && (infSpecs & FIRE_ENGINEERS) == 0) {
            boolean hasExtinguisher = getWeaponList().stream()
                  .anyMatch(w -> w.getType().hasFlag(WeaponType.F_EXTINGUISHER));
            if (!hasExtinguisher) {
                try {
                    EquipmentType extinguisher = EquipmentType.get("Fire Extinguisher");
                    addEquipment(extinguisher, LOC_INFANTRY);
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        } else if ((spec & FIRE_ENGINEERS) == 0 && (infSpecs & FIRE_ENGINEERS) > 0) {
            // Need to remove the Fire Extinguisher
            List<Mounted<?>> eqToRemove = new ArrayList<>();
            for (Mounted<?> eq : getWeaponList()) {
                if (eq.getType().hasFlag(WeaponType.F_EXTINGUISHER)) {
                    eqToRemove.add(eq);
                }
            }
            getEquipment().removeAll(eqToRemove);
            getWeaponList().removeAll(eqToRemove);
        }
        infSpecs = spec;
    }

    /**
     * Adds or removes the tool equipment tied to an engineer specialization when that specialization is gained or lost.
     * The tool is only added if not already present (it may already be loaded from the unit file).
     *
     * @param newSpecs       the new specialization bitmask being applied
     * @param specialization the single specialization flag whose equipment is managed
     * @param toolFlag       the MiscType subtype flag identifying the specialization's tool
     * @param equipmentName  the EquipmentTypeLookup name of the tool to add
     */
    private void updateEngineerEquipment(int newSpecs, int specialization, MiscTypeFlag toolFlag,
          String equipmentName) {
        boolean isGainingSpecialization = ((newSpecs & specialization) > 0) && ((infSpecs & specialization) == 0);
        boolean isLosingSpecialization = ((newSpecs & specialization) == 0) && ((infSpecs & specialization) > 0);
        if (isGainingSpecialization) {
            boolean hasTool = getMisc().stream()
                  .anyMatch(mounted -> mounted.getType().hasFlag(MiscType.F_TOOLS)
                        && mounted.getType().hasFlag(toolFlag));
            if (!hasTool) {
                try {
                    EquipmentType tool = EquipmentType.get(equipmentName);
                    addEquipment(tool, LOC_INFANTRY);
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        } else if (isLosingSpecialization) {
            List<Mounted<?>> equipmentToRemove = new ArrayList<>();
            for (Mounted<?> equipment : getMisc()) {
                if (equipment.getType().hasFlag(MiscType.F_TOOLS) && equipment.getType().hasFlag(toolFlag)) {
                    equipmentToRemove.add(equipment);
                }
            }
            getEquipment().removeAll(equipmentToRemove);

            for (Mounted<?> mounted : equipmentToRemove) {
                if (mounted instanceof MiscMounted) {
                    getMisc().remove(mounted);
                }
            }
        }
    }

    public static String getSpecializationName(int spec) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < NUM_SPECIALIZATIONS; i++) {
            int currSpec = 1 << i;
            if ((spec & currSpec) < 1) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(" ");
            }
            name.append(Messages.getString("Infantry.specialization" + i));
        }
        return name.toString();
    }

    public static String getSpecializationTooltip(int spec) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < NUM_SPECIALIZATIONS; i++) {
            int currSpec = 1 << i;
            if ((spec & currSpec) < 1) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(" ");
            }
            name.append(Messages.getString("Infantry.specializationTip" + i));
        }
        return name.toString();
    }

    // region Bridge building (TO:AUE p.152, Bridge-Building Engineers)

    /** Number of full turns of work needed to raise a bridge, before any extensions from casualties. TO:AUE p.152. */
    public static final int BRIDGE_BUILD_TURNS = 6;

    /** A Light Bridge (CF 15). The value doubles as its point cost: the budget of 2 allows two per scenario. */
    public static final int BRIDGE_TYPE_LIGHT = 1;

    /** A Medium Bridge (CF 40). The value doubles as its point cost: the budget of 2 allows one per scenario. */
    public static final int BRIDGE_TYPE_MEDIUM = 2;

    /** The per-scenario bridge building budget: 2 Light Bridges (1 point each) or 1 Medium Bridge (2 points). */
    public static final int BRIDGE_BUILD_POINTS = 2;

    private static final int LIGHT_BRIDGE_CF = 15;
    private static final int MEDIUM_BRIDGE_CF = 40;

    /** Full turns of bridge building completed before the current round; -1 while not building. */
    private int bridgeBuildTurns = -1;

    /** Turns of work needed to finish the current bridge; starts at {@link #BRIDGE_BUILD_TURNS}, +1 per casualty turn. */
    private int bridgeBuildRequiredTurns = BRIDGE_BUILD_TURNS;

    /** The hex the bridge is being raised in, or null while not building. */
    private Coords bridgeTargetCoords = null;

    /** Exits bitmask of the two hexsides the finished bridge will connect. */
    private int bridgeExits = 0;

    /** The bridge type under construction, {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}. */
    private int bridgeType = BRIDGE_TYPE_LIGHT;

    /** Remaining per-scenario bridge building points. */
    private int bridgeBuildPoints = BRIDGE_BUILD_POINTS;

    /** Trooper count at the last casualty check, used to detect losses that extend the build by a turn. */
    private int bridgeBuildTroopersSnapshot = 0;

    /** Full turns of dismantling completed before the current round; -1 while not dismantling. */
    private int bridgeDismantleTurns = -1;

    /** Turns of dismantling needed to fully cancel the build; set to the turns already spent building. */
    private int bridgeDismantleRequiredTurns = 0;

    /**
     * {@code true} while a bridge build is paused: the partial progress is held on the platoon but the platoon is
     * freed (it may move and fight normally) until it returns to the site and resumes. Stored in saves (non-transient).
     */
    private boolean bridgeBuildPaused = false;

    /**
     * {@code true} when the current work is repairing a destroyed section of an existing bridge (the unofficial
     * bridge-repair option) rather than raising a new bridge. Decides which placement rule runs when the work finishes:
     * a repaired section matches the surviving span's deck so the bridge reconnects. Stored in saves (non-transient).
     */
    private boolean bridgeBuildIsRepair = false;

    /**
     * @return {@code true} while this platoon is actively raising a bridge (not paused). While actively building, the
     *       platoon is eligible only in the movement phase (to continue or change the build) and takes no other action.
     */
    public boolean isBuildingBridge() {
        return (bridgeBuildTurns >= 0) && !bridgeBuildPaused;
    }

    /**
     * @return {@code true} while a bridge build is paused: progress is held but the platoon is freed to act normally
     *       and may return to its site to resume. TO:AUE p.152.
     */
    public boolean isBridgePaused() {
        return (bridgeBuildTurns >= 0) && bridgeBuildPaused;
    }

    /**
     * @return {@code true} while this platoon is dismantling a cancelled bridge. Dismantling takes as many turns as
     *       were spent building; on completion the bridge building budget is refunded and the platoon is freed.
     */
    public boolean isDismantlingBridge() {
        return bridgeDismantleTurns >= 0;
    }

    /**
     * @return {@code true} while this platoon is actively occupied raising or dismantling a bridge - it takes no other
     *       action this turn. A <i>paused</i> build is not "busy": the platoon is freed (see {@link #isBridgePaused()}).
     */
    public boolean isBusyWithBridge() {
        return isBuildingBridge() || isDismantlingBridge();
    }

    /**
     * @return {@code true} if this platoon has any bridge work in progress - actively building, paused, or dismantling.
     *       Used to block starting a second bridge and to show the in-progress indicator.
     */
    public boolean hasBridgeInProgress() {
        return (bridgeBuildTurns >= 0) || isDismantlingBridge();
    }

    /**
     * @param game     the current game
     * @param boardId  the board the target hex is on
     * @param target   the candidate bridge hex
     * @param excluded a platoon to ignore (the one considering the build), or null
     *
     * @return {@code true} if another platoon already has bridge work in progress (building, paused, or dismantling)
     *       targeting this hex. An in-progress bridge places no terrain until it completes, so this is the only way to
     *       stop two platoons from raising two bridges in the same hex. TO:AUE p.152.
     */
    public static boolean isBridgeTargetClaimed(Game game, int boardId, Coords target, @Nullable Entity excluded) {
        if (target == null) {
            return false;
        }
        for (Entity entity : game.getEntitiesVector()) {
            if ((entity != excluded) && (entity instanceof ConvInfantry convInfantry)
                  && convInfantry.hasBridgeInProgress()
                  && (convInfantry.getBoardId() == boardId)
                  && target.equals(convInfantry.getBridgeTargetCoords())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Begins raising a bridge in the given hex. The declaring turn counts as the first full turn of work.
     *
     * @param target     the hex the bridge will be raised in; must be adjacent to this platoon
     * @param exits      exits bitmask of the two hexsides the finished bridge will connect
     * @param bridgeType {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}
     */
    public void startBridgeBuild(Coords target, int exits, int bridgeType) {
        bridgeBuildTurns = 0;
        bridgeBuildRequiredTurns = BRIDGE_BUILD_TURNS;
        bridgeTargetCoords = target;
        bridgeExits = exits;
        this.bridgeType = bridgeType;
        bridgeBuildIsRepair = false;
        bridgeBuildTroopersSnapshot = Math.max(getInternal(LOC_INFANTRY), 0);
        logger.debug("[BuildBridge] {} starts a type-{} bridge at {} (exits bitmask {}, {} troopers, {} turns of "
                    + "work)", getShortName(), bridgeType, target, exits, bridgeBuildTroopersSnapshot,
              bridgeBuildRequiredTurns);
    }

    /**
     * Begins repairing a single destroyed section of an existing bridge (the unofficial bridge-repair option). The
     * work, turn count, casualty extension, pause/dismantle/abandon lifecycle and budget cost are identical to a new
     * bridge build; only the finished section differs - it is placed at the surviving span's deck so the bridge
     * reconnects. The declaring turn counts as the first full turn of work.
     *
     * @param target     the gap hex the section will be rebuilt in; must be adjacent to this platoon
     * @param exits      exits bitmask of the two hexsides the repaired section will connect
     * @param bridgeType {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}
     */
    public void startBridgeRepair(Coords target, int exits, int bridgeType) {
        startBridgeBuild(target, exits, bridgeType);
        bridgeBuildIsRepair = true;
        logger.debug("[BridgeRepair] {} starts repairing a type-{} section at {} (exits bitmask {})", getShortName(),
              bridgeType, target, exits);
    }

    /** Stops all bridge work, losing any progress. The bridge building budget is not refunded. */
    public void cancelBridgeBuild() {
        bridgeBuildTurns = -1;
        bridgeBuildRequiredTurns = BRIDGE_BUILD_TURNS;
        bridgeTargetCoords = null;
        bridgeExits = 0;
        bridgeType = BRIDGE_TYPE_LIGHT;
        bridgeBuildTroopersSnapshot = 0;
        bridgeDismantleTurns = -1;
        bridgeDismantleRequiredTurns = 0;
        bridgeBuildPaused = false;
        bridgeBuildIsRepair = false;
    }

    /**
     * Abandons any bridge work in progress immediately: the partial structure is lost this turn and the spent budget
     * is
     * <b>not</b> refunded. Unlike dismantling (which takes turns and refunds), abandoning is instant and forfeits the
     * points. TO:AUE p.152.
     */
    public void abandonBridge() {
        logger.debug("[BuildBridge] {} abandons its bridge at {} - progress lost, {} point(s) forfeit",
              getShortName(), bridgeTargetCoords, bridgeType);
        cancelBridgeBuild();
    }

    /**
     * Pauses an active bridge build: the partial progress is held on the platoon, which is then freed to move and fight
     * normally until it returns to the site and resumes. The spent budget stays committed. TO:AUE p.152.
     */
    public void pauseBridgeBuild() {
        bridgeBuildPaused = true;
        logger.debug("[BuildBridge] {} pauses its type-{} bridge at {} at {} of {} turns; the platoon is freed",
              getShortName(), bridgeType, bridgeTargetCoords, bridgeBuildTurns, bridgeBuildRequiredTurns);
    }

    /**
     * Begins dismantling the partially built bridge instead of finishing it. Dismantling takes as many full turns as
     * were already spent building (the declaring turn counts as the first). The target hex, exits and type are kept so
     * the in-progress structure can keep being shown; the spent budget is refunded only once dismantling finishes.
     */
    public void startBridgeDismantle() {
        // Dismantling undoes the turns of work actually banked so far (END-phase completions); at least one turn.
        bridgeDismantleRequiredTurns = Math.max(1, bridgeBuildTurns);
        bridgeDismantleTurns = 0;
        bridgeBuildTurns = -1;
        logger.debug("[BuildBridge] {} starts dismantling its type-{} bridge at {} ({} turns of dismantling)",
              getShortName(), bridgeType, bridgeTargetCoords, bridgeDismantleRequiredTurns);
    }

    /**
     * Resumes building, from either a paused build or a dismantling. From a pause, the held progress simply continues.
     * From a dismantling, the structure still standing (the dismantle countback position) becomes the build's
     * completed-turn total, so building resumes from where the dismantling left off. Either way the trooper snapshot is
     * refreshed so casualty extensions resume cleanly, and the bridge site, exits, type and spent budget are unchanged.
     * TO:AUE p.152.
     */
    public void resumeBridgeBuild() {
        if (isDismantlingBridge()) {
            bridgeBuildTurns = getBridgeDismantleRemaining();
            bridgeDismantleTurns = -1;
            bridgeDismantleRequiredTurns = 0;
        }
        bridgeBuildPaused = false;
        bridgeBuildTroopersSnapshot = Math.max(getInternal(LOC_INFANTRY), 0);
        logger.debug("[BuildBridge] {} resumes building its type-{} bridge at {} from {} of {} turns",
              getShortName(), bridgeType, bridgeTargetCoords, bridgeBuildTurns, bridgeBuildRequiredTurns);
    }

    /**
     * Banks one full turn of bridge building (called once per round from the END phase, where the work is actually
     * completed) and returns the new completed-turn total.
     *
     * @return the number of full turns of work now completed
     */
    public int bankBridgeBuildTurn() {
        bridgeBuildTurns++;
        return bridgeBuildTurns;
    }

    /**
     * Banks one full turn of bridge dismantling (called once per round from the END phase) and returns the new
     * completed-turn total.
     *
     * @return the number of full turns of dismantling now completed
     */
    public int bankBridgeDismantleTurn() {
        bridgeDismantleTurns++;
        return bridgeDismantleTurns;
    }

    /**
     * @return the turns of built structure still standing while dismantling: the work banked at cancel time counts back
     *       down to zero. Shown on the same {@code N / build-required} scale as the build (e.g. cancel at 4/6 -> the
     *       dismantling counts 4/6, 3/6, 2/6, 1/6).
     */
    public int getBridgeDismantleRemaining() {
        return Math.max(0, bridgeDismantleRequiredTurns - bridgeDismantleTurns);
    }

    /** Refunds the points spent on the cancelled bridge, capped at the per-scenario budget. */
    public void refundBridgeBuildPoints() {
        bridgeBuildPoints = Math.min(BRIDGE_BUILD_POINTS, bridgeBuildPoints + bridgeType);
        logger.debug("[BuildBridge] {} refunded {} bridge building point(s); budget now {}", getShortName(),
              bridgeType, bridgeBuildPoints);
    }

    /** @return full turns of dismantling completed so far before the current round, or -1 while not dismantling. */
    public int getBridgeDismantleTurns() {
        return bridgeDismantleTurns;
    }

    /** @return total turns of dismantling needed to fully cancel the build. */
    public int getBridgeDismantleRequiredTurns() {
        return bridgeDismantleRequiredTurns;
    }

    /**
     * @return The number of full turns of bridge building completed before the current round, or -1 while not building.
     *       The current (in-progress) round is not included.
     */
    public int getBridgeBuildTurns() {
        return bridgeBuildTurns;
    }

    /**
     * @return The total turns of work needed to finish the current bridge: {@link #BRIDGE_BUILD_TURNS} plus one for
     *       each turn the platoon took casualties while building. TO:AUE p.152.
     */
    public int getBridgeBuildRequiredTurns() {
        return bridgeBuildRequiredTurns;
    }

    /**
     * @return The hex the bridge is being raised in, or null while not building.
     */
    public @Nullable Coords getBridgeTargetCoords() {
        return bridgeTargetCoords;
    }

    /**
     * @return The exits bitmask of the two hexsides the finished bridge will connect.
     */
    public int getBridgeExits() {
        return bridgeExits;
    }

    /**
     * @return The bridge type under construction, {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}.
     */
    public int getBridgeType() {
        return bridgeType;
    }

    /**
     * @return {@code true} if the current work is repairing a destroyed section of an existing bridge (unofficial
     *       bridge-repair option) rather than raising a new bridge. Decides the placement rule used when the work
     *       finishes.
     */
    public boolean isBridgeBuildRepair() {
        return bridgeBuildIsRepair;
    }

    /**
     * @return The remaining per-scenario bridge building points (Light Bridge costs 1, Medium Bridge costs 2).
     */
    public int getBridgeBuildPoints() {
        return bridgeBuildPoints;
    }

    /**
     * Spends bridge building points when a build begins. The cost equals the bridge type value.
     *
     * @param bridgeType {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}
     */
    public void spendBridgeBuildPoints(int bridgeType) {
        bridgeBuildPoints = Math.max(0, bridgeBuildPoints - bridgeType);
    }

    /**
     * @param bridgeType {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}
     *
     * @return {@code true} if the remaining budget covers a bridge of the given type.
     */
    public boolean canAffordBridge(int bridgeType) {
        return bridgeBuildPoints >= bridgeType;
    }

    /**
     * @return {@code true} if this platoon is a Bridge-Building Engineer unit that still carries its bridge kit, has
     *       budget left, and is not already raising a bridge. Does not check the game option or terrain.
     */
    public boolean canStartBridgeBuild() {
        return hasSpecialization(BRIDGE_ENGINEERS) && hasBridgeKit() && canAffordBridge(BRIDGE_TYPE_LIGHT)
              && !hasBridgeInProgress();
    }

    /**
     * @return {@code true} if this platoon carries an Infantry Bridge Kit.
     */
    public boolean hasBridgeKit() {
        return getMisc().stream()
              .anyMatch(mounted -> mounted.getType().hasFlag(MiscType.F_TOOLS)
                    && mounted.getType().hasFlag(MiscTypeFlag.S_BRIDGE_KIT));
    }

    /**
     * @return {@code true} while this platoon is adjacent to its bridge construction site. A platoon that is displaced,
     *       transported or destroyed is no longer adjacent and abandons the build.
     */
    public boolean isAdjacentToBridgeSite() {
        return (getPosition() != null) && (bridgeTargetCoords != null)
              && (getPosition().distance(bridgeTargetCoords) == 1);
    }

    /**
     * Checks whether this platoon lost troopers since the last check; a turn with casualties extends the build by one
     * turn, regardless of how many separate attacks caused them. TO:AUE p.152. Called once per turn from the END phase.
     *
     * @return {@code true} if casualties extended the build this turn.
     */
    public boolean updateBridgeBuildCasualties() {
        if (!isBuildingBridge()) {
            return false;
        }
        int currentTroopers = Math.max(getInternal(LOC_INFANTRY), 0);
        boolean tookCasualties = currentTroopers < bridgeBuildTroopersSnapshot;
        if (tookCasualties) {
            bridgeBuildRequiredTurns++;
            logger.debug("[BuildBridge] {} took casualties while building ({} -> {} troopers); the build now "
                        + "requires {} turns", getShortName(), bridgeBuildTroopersSnapshot, currentTroopers,
                  bridgeBuildRequiredTurns);
        }
        bridgeBuildTroopersSnapshot = currentTroopers;
        return tookCasualties;
    }

    /**
     * @param bridgeType  the bridge type, {@link #BRIDGE_TYPE_LIGHT} or {@link #BRIDGE_TYPE_MEDIUM}
     * @param isOverWater {@code true} if the bridge is being raised over a water hex (water of any depth, swamp or
     *                    rapids; see {@link megamek.common.board.BridgeConstruction#isOverWater})
     *
     * @return The Construction Factor of a bridge raised by Bridge-Building Engineers: 15 for a Light Bridge, 40 for a
     *       Medium Bridge, doubled when built over water. TO:AUE p.152.
     */
    public static int getBuiltBridgeCF(int bridgeType, boolean isOverWater) {
        int constructionFactor = (bridgeType == BRIDGE_TYPE_MEDIUM) ? MEDIUM_BRIDGE_CF : LIGHT_BRIDGE_CF;
        return isOverWater ? constructionFactor * 2 : constructionFactor;
    }

    @Override
    public void newRound(int roundNumber) {
        // Turns of work are banked once per round in the END phase (TWGameManager.checkBuildBridges), not here, so the
        // counter reflects turns actually completed. newRound (INITIATIVE phase) only abandons the work if the platoon
        // was displaced or transported away from its site since the END phase.
        if (isBuildingBridge() && !isAdjacentToBridgeSite()) {
            logger.debug("[BuildBridge] {} abandons its bridge build: no longer adjacent to {} (position {})",
                  getShortName(), bridgeTargetCoords, getPosition());
            cancelBridgeBuild();
        } else if (isDismantlingBridge() && !isAdjacentToBridgeSite()) {
            // Displacement while dismantling abandons the work; the partial bridge is lost and no points are refunded.
            logger.debug("[BuildBridge] {} abandons its bridge dismantling: no longer adjacent to {} (position {})",
                  getShortName(), bridgeTargetCoords, getPosition());
            cancelBridgeBuild();
        } else if (isBuildingBridge()) {
            logger.debug("[BuildBridge] {} newRound (round {}): {} of {} build turns banked",
                  getShortName(), roundNumber, bridgeBuildTurns, bridgeBuildRequiredTurns);
        } else if (isDismantlingBridge()) {
            logger.debug("[BuildBridge] {} newRound (round {}): {} of {} dismantle turns banked",
                  getShortName(), roundNumber, bridgeDismantleTurns, bridgeDismantleRequiredTurns);
        }
        if (impEnergyWeaponsDisabledRounds > 0) {
            impEnergyWeaponsDisabledRounds--;
        }
        super.newRound(roundNumber);
    }

    @Override
    public boolean isEligibleFor(GamePhase phase) {
        // A platoon raising or dismantling a bridge takes no other action, but stays eligible in the movement phase so
        // the player can keep working, cancel the build, or start dismantling. Building/dismantling progresses and
        // completes in END phase processing. TO:AUE p.152.
        if (isBusyWithBridge()) {
            return phase.isMovement() && super.isEligibleFor(phase);
        }
        return super.isEligibleFor(phase);
    }

    // endregion

    /**
     * Returns the movement mode for this infantry unit. For powered flight infantry (IO p.85), this returns VTOL when
     * the wings are usable, allowing them to use existing VTOL movement infrastructure. Note: During construction, crew
     * is null so hasPoweredFlightWings() returns false, ensuring this override doesn't interfere with setMovementMode()
     * initialization.
     */
    @Override
    public EntityMovementMode getMovementMode() {
        // If powered flight is usable, behave like VTOL
        if (hasPoweredFlightWings() && canUsePoweredFlightWings()) {
            return EntityMovementMode.VTOL;
        }
        return movementMode;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        // Beast mounted infantry depends entirely on the creature
        if (mount == null) {
            // Encumbering armor (TacOps, pg. 318)
            if (encumbering) {
                mp = Math.max(mp - 1, 1);
            }
            if ((getSecondaryWeaponsPerSquad() > 1)
                  && !hasAbility(OptionsConstants.MD_TSM_IMPLANT)
                  && !hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
                  && !hasNonEncumberingSecondaryWeaponSpecialization()
                  && (null != secondaryWeapon)
                  && secondaryWeapon.hasFlag(WeaponType.F_INF_SUPPORT)
                  && !getMovementMode().isTracked()
                  && !getMovementMode().isJumpInfantry()) {
                mp = Math.max(mp - 1, 0);
            }
            // PL-MASC IntOps p.84
            if ((null != getCrew())
                  && hasAbility(OptionsConstants.MD_PL_MASC)
                  && getMovementMode().isLegInfantry()
                  && isConventionalInfantry()) {
                mp += 1;
            }

            if ((null != getCrew())
                  && hasAbility(OptionsConstants.INFANTRY_FOOT_CAV)
                  && getMovementMode().isJumpOrLegInfantry()) {
                mp += 1;
            }

            if (hasActiveFieldArtillery()) {
                mp = Math.min(mp, 1);
            }
        }

        if (!mpCalculationSetting.ignoreWeather() && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (conditions.getWeather().isGustingRain() && getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_RAIN)) {
                if ((mp != 0) || getMovementMode().isMotorizedInfantry()) {
                    mp += 1;
                }
            }

            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_SNOW)) {
                if (conditions.getWeather().isSnowFlurriesOrIceStorm() && (getOriginalWalkMP() != 0)) {
                    mp += 1;
                }
            }

            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_WIND) && conditions.getWeather().isClear()) {
                if (conditions.getWind().isModerateGale()) {
                    mp += 1;
                }

                if (conditions.getWind().isStrongGale() && ((mp != 0) || getMovementMode().isMotorizedInfantry())) {
                    mp += 1;
                }
            }
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return mp;
    }

    @Override
    public boolean doomedInVacuum() {
        return getMovementMode().isVTOL() || !hasSpaceSuit();
    }

    @Override
    public boolean doomedInExtremeTemp() {
        // If there is no game object, count any temperature protection.
        if (getArmorKit() != null) {
            if (getArmorKit().hasFlag(MiscTypeFlag.S_XCT_VACUUM)) {
                return false;
            } else if (getArmorKit().hasFlag(MiscTypeFlag.S_COLD_WEATHER) && ((game == null)
                  || game.getPlanetaryConditions().getTemperature() < -30)) {
                return false;
            } else {
                return !getArmorKit().hasFlag(MiscTypeFlag.S_HOT_WEATHER) || ((game != null)
                      && game.getPlanetaryConditions().getTemperature() <= 50);
            }
        }
        return !hasSpaceSuit() && !isMechanized();
    }

    public EquipmentType getArmorKit() {
        return getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_ARMOR_KIT))
              .findFirst()
              .map(Mounted::getType)
              .orElse(null);
    }

    public void setArmorKit(EquipmentType armorKit) {
        // If the desired kit is already equipped, just apply flags without
        // removing and re-adding (which would reorder the equipment list).
        EquipmentType currentKit = getArmorKit();
        if (armorKit != null && armorKit.equals(currentKit)) {
            applyArmorKitFlags(armorKit);
            return;
        }

        removeArmorKits();
        if ((armorKit != null) && armorKit.hasFlag(MiscType.F_ARMOR_KIT)) {
            try {
                addEquipment(armorKit, LOC_INFANTRY);
            } catch (LocationFullException ex) {
                logger.error("", ex);
            }
        }
        applyArmorKitFlags(armorKit);
    }

    /**
     * Applies the armor kit's flags (encumbering, space suit, DEST, sneak properties) and recalculates the damage
     * divisor, without modifying the equipment list.
     */
    private void applyArmorKitFlags(EquipmentType armorKit) {
        if ((armorKit != null) && armorKit.hasFlag(MiscType.F_ARMOR_KIT)) {
            encumbering = armorKit.hasFlag(MiscTypeFlag.S_ENCUMBERING);
            spaceSuit = armorKit.hasFlag(MiscTypeFlag.S_SPACE_SUIT);
            dest = armorKit.hasFlag(MiscTypeFlag.S_DEST);
            sneak_camo = armorKit.hasFlag(MiscTypeFlag.S_SNEAK_CAMO);
            sneak_ir = armorKit.hasFlag(MiscTypeFlag.S_SNEAK_IR);
            sneak_ecm = armorKit.hasFlag(MiscTypeFlag.S_SNEAK_ECM);
        }
        calcDamageDivisor();
    }

    private void removeArmorKits() {
        List<MiscMounted> toRemove = getEquipment().stream()
              .filter(m -> m instanceof MiscMounted)
              .map(m -> (MiscMounted) m)
              .filter(m -> m.getType().hasFlag(MiscType.F_ARMOR_KIT))
              .toList();

        getEquipment().removeAll(toRemove);
        getMisc().removeAll(toRemove);

        for (CriticalSlot slot : getCriticalSlots(LOC_INFANTRY)) {
            if ((slot != null) &&
                  (slot.getMount() instanceof MiscMounted) &&
                  toRemove.contains((MiscMounted) slot.getMount())) {
                removeCriticalSlots(LOC_INFANTRY, slot);
            }
        }
    }

    public double calcDamageDivisor() {
        double divisor;
        EquipmentType armorKit = getArmorKit();
        if (armorKit != null) {
            divisor = ((MiscType) armorKit).getDamageDivisor();
        } else {
            // For custom armor kits, which aren't found by getArmorKit()
            divisor = getCustomArmorDamageDivisor();
        }
        // TSM implant reduces divisor to 0.5 if no other armor is worn
        if ((armorKit == null) && (divisor == 1.0) && hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            divisor = 0.5;
        }
        // Dermal camo armor provides divisor of 1.0 (prevents 0.5 from TSM alone)
        // but does NOT add to divisor like regular dermal armor
        if ((divisor == 0.5) && hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR)) {
            divisor = 1.0;
        }
        // Dermal armor adds one to the divisor, cumulative with armor kit and TSM implant
        if (hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
            divisor += 1.0;
        }
        if (mount != null) {
            divisor *= mount.damageDivisor();
        }
        return divisor;
    }

    /**
     * Gets the damage divisor of this entity's custom armor kit. Meaningless when the infantry possesses a normal,
     * non-custom armor kit, so check for one first.
     *
     * @return The damage divisor of the custom armor kit, or 1.0 for no armor.
     */
    public double getCustomArmorDamageDivisor() {
        return customArmorDamageDivisor;
    }

    /**
     * Creates a custom armor kit for this entity. Should not be set for anything other than a custom armor kit, see
     * {@link #calcDamageDivisor()} to add any other source of damage divisor.
     *
     * @param d The damage divisor of the custom armor kit. Set it to 1.0 for no armor.
     */
    public void setCustomArmorDamageDivisor(double d) {
        customArmorDamageDivisor = d;
    }

    public boolean isArmorEncumbering() {
        return encumbering;
    }

    public void setArmorEncumbering(boolean b) {
        encumbering = b;
    }


    public boolean hasSneakCamo() {
        return sneak_camo;
    }

    public void setSneakCamo(boolean b) {
        sneak_camo = b;
    }

    public boolean hasSneakIR() {
        return sneak_ir;
    }

    public void setSneakIR(boolean b) {
        sneak_ir = b;
    }

    public boolean hasSneakECM() {
        return sneak_ecm;
    }

    public void setSneakECM(boolean b) {
        sneak_ecm = b;
    }

    public void setPrimaryWeapon(InfantryWeapon w) {
        primaryWeapon = w;
        primaryName = w.getInternalName();
    }

    public InfantryWeapon getPrimaryWeapon() {
        return primaryWeapon;
    }

    public void setSecondaryWeapon(InfantryWeapon w) {
        secondaryWeapon = w;
        if (null == w) {
            secondName = null;
        } else {
            secondName = w.getInternalName();
        }
    }

    public InfantryWeapon getSecondaryWeapon() {
        return secondaryWeapon;
    }

    /**
     * Sets the platoon's one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing). All troopers carry the
     * same Disposable Weapon. This only records the weapon type; the corresponding fireable {@link
     * megamek.common.equipment.WeaponMounted} is added to {@code LOC_INFANTRY} by the loader/editor.
     *
     * @param weapon the Disposable Weapon, or null to clear it
     */
    public void setDisposableWeapon(@Nullable InfantryWeapon weapon) {
        disposableWeapon = weapon;
        disposableName = (weapon == null) ? null : weapon.getInternalName();
    }

    /**
     * @return the platoon's one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing), or null if it has none
     */
    @Nullable
    public InfantryWeapon getDisposableWeapon() {
        // Reconstruct the type from its name after client/server serialization dropped the transient reference.
        if ((disposableWeapon == null) && (disposableName != null)
              && (EquipmentType.get(disposableName) instanceof InfantryWeapon infantryWeapon)) {
            disposableWeapon = infantryWeapon;
        }
        return disposableWeapon;
    }

    /**
     * @return {@code true} if this platoon carries a one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth
     *       Printing)
     */
    public boolean hasDisposableWeapon() {
        return getDisposableWeapon() != null;
    }

    /**
     * Sets the platoon's one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) and synchronizes the
     * corresponding fireable mount. Any previously-equipped Disposable Weapon mount is removed first; pass null to
     * remove the Disposable Weapon entirely. Use this (rather than {@link #setDisposableWeapon}) when changing the
     * loadout of an already-built platoon, e.g. from the lobby configuration dialog.
     *
     * @param weapon the Disposable Weapon to equip, or null to remove it
     */
    public void equipDisposableWeapon(@Nullable InfantryWeapon weapon) {
        List<WeaponMounted> existingDisposableMounts = weaponList.stream()
              .filter(WeaponMounted::isDisposableWeapon)
              .toList();
        for (WeaponMounted disposableMount : existingDisposableMounts) {
            equipmentList.remove(disposableMount);
            weaponList.remove(disposableMount);
            totalWeaponList.remove(disposableMount);
        }

        setDisposableWeapon(weapon);

        if (weapon != null) {
            try {
                WeaponMounted disposableMount = (WeaponMounted) Mounted.createMounted(this, weapon);
                disposableMount.setDisposableWeapon(true);
                addEquipment(disposableMount, LOC_INFANTRY, false);
            } catch (LocationFullException ex) {
                logger.error("Could not equip Disposable Weapon {}", weapon.getName(), ex);
            }
        }
    }

    public void setSecondaryWeaponsPerSquad(int n) {
        secondaryWeaponsPerSquad = n;
    }

    public int getSecondaryWeaponsPerSquad() {
        return secondaryWeaponsPerSquad;
    }

    private boolean hasNonEncumberingSecondaryWeaponSpecialization() {
        return hasSpecialization(TAG_TROOPS);
    }

    public double getDamagePerTrooper() {
        if (null == primaryWeapon) {
            return 0;
        }

        // Improved Magnetic Pulse missiles render energy weapons inoperative for a turn (IO IMP rules).
        boolean energyDisabled = impEnergyWeaponsDisabledRounds > 0;

        // per 09/2021 errata, primary infantry weapon damage caps out at 0.6
        double adjustedDamage = Math.min(MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP,
              primaryWeapon.getInfantryDamage());
        if (energyDisabled && primaryWeapon.hasFlag(WeaponType.F_ENERGY)) {
            adjustedDamage = 0;
        }
        double damage = adjustedDamage * (squadSize - secondaryWeaponsPerSquad);
        if ((null != secondaryWeapon)
              && !(energyDisabled && secondaryWeapon.hasFlag(WeaponType.F_ENERGY))) {
            damage += secondaryWeapon.getInfantryDamage() * secondaryWeaponsPerSquad;
        }
        return damage / squadSize;
    }

    /**
     * Records an Improved Magnetic Pulse (iATM IMP) missile hit on this platoon (IO IMP rules). If the platoon uses
     * energy weapons, they are rendered inoperative through the End Phase of the following turn. Other weapon types are
     * unaffected.
     */
    public void applyImpEnergyWeaponDisable() {
        // 2 rounds so the effect lasts through the End Phase of the turn after the attack.
        impEnergyWeaponsDisabledRounds = 2;
    }

    /**
     * @return {@code true} while this platoon's energy weapons are rendered inoperative by an Improved Magnetic Pulse
     *       missile hit (IO IMP rules).
     */
    public boolean isEnergyWeaponsDisabled() {
        return impEnergyWeaponsDisabledRounds > 0;
    }

    /**
     * @return {@code true} if this platoon is equipped with cybernetic enhancements of any kind (IO p.84 prosthetic
     *       enhancements). Improved Magnetic Pulse missiles deal double damage to such units.
     */
    public boolean isCyberneticallyEnhanced() {
        return hasProstheticEnhancement();
    }

    /**
     * @return {@code true} if this platoon's primary or secondary weapon is an energy weapon. Improved Magnetic Pulse
     *       missiles
     *       render such weapons inoperative through the End Phase of the following turn.
     */
    public boolean isUsingEnergyWeapons() {
        return ((primaryWeapon != null) && primaryWeapon.hasFlag(WeaponType.F_ENERGY))
              || ((secondaryWeapon != null) && secondaryWeapon.hasFlag(WeaponType.F_ENERGY));
    }

    public boolean primaryWeaponDamageCapped() {
        return getPrimaryWeaponDamage() > MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP;
    }

    public double getPrimaryWeaponDamage() {
        return (primaryWeapon != null) ? primaryWeapon.getInfantryDamage() : 0;
    }

    public String getArmorDesc() {
        StringBuilder sArmor = new StringBuilder();
        sArmor.append(calcDamageDivisor());
        if (isArmorEncumbering()) {
            sArmor.append("E");
        }

        if (hasSpaceSuit()) {
            sArmor.append(" (Spacesuit) ");
        }

        if (hasDEST()) {
            sArmor.append(" (DEST) ");
        }

        if (hasSneakCamo() || (getCrew() != null && hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR))) {
            sArmor.append(" (Camo) ");
        }

        if (hasSneakIR()) {
            sArmor.append(" (IR) ");
        }

        if (hasSneakECM()) {
            sArmor.append(" (ECM) ");
        }

        return sArmor.toString();
    }

    @Override
    public void restore() {
        super.restore();

        if (null != primaryName) {
            primaryWeapon = restoreInfantryWeapon(primaryName);
            if (null != primaryWeapon) {
                primaryName = primaryWeapon.getInternalName();
            }
        }

        if (null != secondName) {
            secondaryWeapon = restoreInfantryWeapon(secondName);
            if (null != secondaryWeapon) {
                secondName = secondaryWeapon.getInternalName();
            }
        }
    }

    private static @Nullable InfantryWeapon restoreInfantryWeapon(String weaponName) {
        if (EquipmentType.get(weaponName) instanceof InfantryWeapon infantryWeapon) {
            return infantryWeapon;
        }
        return null;
    }

    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Note: infantry are immune to stealth, but not camouflage or mimetic armor
        if ((sneak_ir || dest) && !(ae instanceof Infantry)) {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                case RangeType.RANGE_MEDIUM:
                    result = new TargetRoll(+1, "Sneak, IR/DEST suit");
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    result = new TargetRoll(+2, "Sneak, IR/DEST suit");
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Simple camo modifier is on top of the movement modifier
        // This can also be in addition to IR/DEST stealth mods!
        if (sneak_camo && (delta_distance < 3)) {
            int mod = Math.max(3 - delta_distance, 0);
            if (result == null) {
                result = new TargetRoll(mod, "Sneak, Camo");
            } else {
                result.append(new TargetRoll(mod, "Sneak, Camo"));
            }
        }

        // Dermal camo armor provides mimetic stealth for foot/jump infantry only
        // when not wearing other armor. Modifier based on movement: +3/+2/+1/+0
        if (hasDermalCamoStealth() && (delta_distance < 3)) {
            int mod = Math.max(3 - delta_distance, 0);
            if (result == null) {
                result = new TargetRoll(mod, "Dermal Camo");
            } else {
                result.append(new TargetRoll(mod, "Dermal Camo"));
            }
        }

        if (dest && (delta_distance == 0)) {
            if (result == null) {
                result = new TargetRoll(1, "DEST suit");
            } else {
                result.append(new TargetRoll(1, "DEST Suit"));
            }
        }

        if (result == null) {
            result = new TargetRoll(0, "no sneak mods");
        }

        return result;
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        int mp = hasUMU() ? 0 : getOriginalJumpMP();

        // Powered flight infantry (non-native VTOL) get 2 MP from their wings (IO p.85)
        // Note: Use the stored movementMode field, not getMovementMode() which returns VTOL for powered flight
        if (movementMode != EntityMovementMode.VTOL &&
              hasPoweredFlightWings() && canUsePoweredFlightWings()) {
            mp = 2;
        }

        if (mount == null) {
            if ((getSecondaryWeaponsPerSquad() > 1) &&
                  !hasAbility(OptionsConstants.MD_TSM_IMPLANT) &&
                  !hasAbility(OptionsConstants.MD_DERMAL_ARMOR) &&
                  !hasNonEncumberingSecondaryWeaponSpecialization() &&
                  !getMovementMode().isSubmarine() &&
                  (null != secondaryWeapon) &&
                  secondaryWeapon.hasFlag(WeaponType.F_INF_SUPPORT)) {
                mp = Math.max(mp - 1, 0);
            } else if (movementMode.isVTOL() && getSecondaryWeaponsPerSquad() > 0) {
                mp = Math.max(mp - 1, 0);
            }
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            mp = applyGravityEffectsOnMP(mp);
        }

        if (!mpCalculationSetting.ignoreWeather() && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            if (conditions.getWind().isStrongerThan(Wind.MOD_GALE)) {
                return 0;
            } else if (conditions.getWind().isModerateGale() &&
                  !getCrew().getOptions()
                        .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                        .equals(Crew.ENVIRONMENT_SPECIALIST_WIND)) {
                mp--;
            }

            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_SNOW)) {
                if (conditions.getWeather().isSnowFlurries() && conditions.getWeather().isIceStorm()) {
                    mp += 1;
                }
            }
        }
        return Math.max(mp, 0);
    }


    @Override
    public boolean isStealthy() {
        return dest || sneak_camo || sneak_ir || sneak_ecm;
    }


    @Override
    public boolean antiTSMVulnerable() {
        if (!hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            return false;
        }
        EquipmentType armorKit = getArmorKit();
        return (armorKit == null) ||
              !armorKit.hasAnyFlag(MiscTypeFlag.S_SPACE_SUIT, MiscTypeFlag.S_XCT_VACUUM,
                    MiscTypeFlag.S_TOXIC_ATMOSPHERE);
    }


    /**
     * The alternate cost here is used by MekHQ to create costs that reflect just the cost of equipment. The motive
     * costs here are based on the costs associated with an auto-rifle platoon.
     */
    @Override
    public double getAlternateCost() {
        double cost = 0;
        if (null != primaryWeapon) {
            cost += primaryWeapon.getCost(this, false, -1) * (squadSize - secondaryWeaponsPerSquad);
        }
        if (null != secondaryWeapon) {
            cost += secondaryWeapon.getCost(this, false, -1) * secondaryWeaponsPerSquad;
        }
        cost = cost / squadSize;

        EquipmentType armor = getArmorKit();
        if (armor != null) {
            cost += armor.getCost(this, false, LOC_INFANTRY);
        }

        // Add in motive type costs
        switch (getMovementMode()) {
            case INF_UMU:
                cost += 17888;
                if (getAllUMUCount() > 1) {
                    cost += 17888 * 0.5;
                }
                break;
            case INF_MOTORIZED:
                cost += 17888 * 0.6;
                break;
            case INF_JUMP:
                cost += 17888 * 1.6;
                break;
            case HOVER:
            case WHEELED:
            case TRACKED:
            case SUBMARINE: // FIXME: there is no cost shown for mek. scuba in tac ops
                cost += 17888 * 2.2;
                break;
            case VTOL:
                cost += 17888 * (hasMicrolite() ? 3 : 3.5);
                break;
            default:
                break;
        }
        cost *= originalTrooperCount;
        // add in field gun costs
        cost += originalFieldWeapons().stream().mapToDouble(m -> m.getType().getCost(this, false, -1)).sum();
        return cost;
    }

    @Override
    public boolean isLocationProhibited(Coords c, int testBoardId, int currElevation) {
        if (!game.hasBoardLocation(c, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(c, testBoardId);
        // Taharqa: waiting to hear back from Welshie, but I am going to assume that
        // units pulling artillery
        // should be treated as wheeled rather than motorized because otherwise
        // mechanized units face fewer
        // terrain restrictions when pulling field artillery

        if (hex.containsAnyTerrainOf(Terrains.IMPASSABLE, Terrains.MAGMA)) {
            return true;
        }
        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        if (isHidden()) {
            if ((hex.containsTerrain(Terrains.PAVEMENT) || hex.containsTerrain(Terrains.ROAD)) &&
                  (!hex.containsTerrain(Terrains.BUILDING) && !hex.containsTerrain(Terrains.RUBBLE))) {
                return true;
            }
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation) && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
        }

        if (getMovementMode().isWheeled()) {
            if (hex.containsTerrain(Terrains.WOODS) ||
                  hex.containsTerrain(Terrains.ROUGH) ||
                  hex.containsTerrain(Terrains.RUBBLE) ||
                  hex.containsTerrain(Terrains.JUNGLE) ||
                  (hex.terrainLevel(Terrains.SNOW) > 1) ||
                  (hex.terrainLevel(Terrains.GEYSER) == 2)) {
                return true;
            }
        }

        if (getMovementMode().isTracked()) {
            if ((hex.terrainLevel(Terrains.WOODS) > 1) ||
                  hex.containsTerrain(Terrains.JUNGLE) ||
                  (hex.terrainLevel(Terrains.ROUGH) > 1) ||
                  (hex.terrainLevel(Terrains.RUBBLE) > 5)) {
                return true;
            }
        }

        if (getMovementMode().isHover()) {
            if (hex.containsTerrain(Terrains.WOODS) ||
                  hex.containsTerrain(Terrains.JUNGLE) ||
                  (hex.terrainLevel(Terrains.ROUGH) > 1) ||
                  (hex.terrainLevel(Terrains.RUBBLE) > 5)) {
                return true;
            }
        }

        if ((hex.terrainLevel(Terrains.WATER) <= 0) &&
              getMovementMode().isSubmarine() &&
              ((mount == null) || (mount.secondaryGroundMP() == 0))) {
            return true;
        }

        if (currElevation < 0) {
            if (mount == null) {
                if (!getMovementMode().isUMUInfantry() && !getMovementMode().isSubmarine()) {
                    return true;
                }
            } else {
                if (-currElevation > mount.maxWaterDepth()) {
                    return true;
                }
            }
        }

        if (hex.hasDepth1WaterOrDeeper() && !hex.containsTerrain(Terrains.ICE)) {
            if (mount == null) {
                return !getMovementMode().isHover() &&
                      !getMovementMode().isUMUInfantry() &&
                      !getMovementMode().isSubmarine() &&
                      !getMovementMode().isVTOL();
            } else {
                return hex.terrainLevel(Terrains.WATER) > mount.maxWaterDepth();
            }
        }
        return false;
    }

    @Override
    public boolean isElevationValid(int assumedElevation, Hex hex) {
        if (mount != null) {
            // Mounted infantry can enter water hexes if the mount allows it
            if (hex.containsTerrain(Terrains.WATER) && (hex.terrainLevel(Terrains.WATER) <= mount.maxWaterDepth())) {
                return true;
            }
            // Aquatic mounts may be able to move onto land
            if (!hex.containsTerrain(Terrains.WATER) && movementMode.isSubmarine()) {
                return mount.secondaryGroundMP() > 0;
            }
        }
        return super.isElevationValid(assumedElevation, hex);
    }


    @Override
    public int getUnitType() {
        return UnitType.INFANTRY;
    }

    @Override
    public void setMovementMode(EntityMovementMode movementMode) {
        super.setMovementMode(movementMode);
        setOriginalJumpMP(0);
        switch (getMovementMode()) {
            case INF_MOTORIZED:
            case TRACKED:
                setOriginalWalkMP(3);
                break;
            case HOVER:
                setOriginalWalkMP(5);
                break;
            case WHEELED:
                setOriginalWalkMP(4);
                break;
            case SUBMARINE:
                setOriginalJumpMP(3);
                setOriginalWalkMP(0);
                setSpecializations(getSpecializations() | SCUBA);
                break;
            case VTOL:
                setOriginalJumpMP(hasMicrolite() ? MICROLITE_VTOL_MP : MICRO_COPTER_VTOL_MP);
                setOriginalWalkMP(1);
                break;
            case INF_UMU:
                setOriginalJumpMP(1);
                setOriginalWalkMP(1);
                setSpecializations(getSpecializations() | SCUBA);
                break;
            case INF_JUMP:
                // fall through to get the original Walk MP is deliberate
                setOriginalJumpMP(3);
            case INF_LEG:
                setOriginalWalkMP(1);
                break;
            default:
                setOriginalWalkMP(1);
        }
        addTechComponent(getMotiveTechAdvancement());
    }

    public TechAdvancement getMotiveTechAdvancement() {
        return getMotiveTechAdvancement(mount == null ? getMovementMode() : EntityMovementMode.NONE);
    }

    @Override
    public int height() {
        return mount == null ? 0 : mount.size().height;
    }


    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_INFANTRY);
    }

    @Override
    public int getInternal(int loc) {
        if (loc != LOC_INFANTRY) {
            return 0;
        }
        return (activeTroopers > 0 ? activeTroopers : IArmorState.ARMOR_DESTROYED);
    }

    @Override
    public int getOInternal(int loc) {
        return loc == LOC_INFANTRY ? originalTrooperCount : 0;
    }


    /**
     * @return Active field guns and artillery of this infantry (= not including destroyed ones). Empty on BA.
     */
    public List<Mounted<?>> activeFieldWeapons() {
        return originalFieldWeapons().stream().filter(e -> !e.isDestroyed()).collect(toList());
    }

    /**
     * @return All field guns and artillery of this infantry including destroyed ones. Empty on BA.
     */
    public List<Mounted<?>> originalFieldWeapons() {
        return getEquipment().stream().filter(this::isFieldWeapon).collect(toList());
    }

    /**
     * @return True when the given Mounted is a Field Gun or Artillery. On BA, always returns false.
     */
    protected boolean isFieldWeapon(Mounted<?> equipment) {
        return (equipment.getType() instanceof WeaponType) && (equipment.getLocation() == LOC_FIELD_GUNS);
    }

    /**
     * @return True if this infantry has Dermal Camo Armor and can benefit from its mimetic stealth properties (leg or
     *       jump infantry only, not wearing other armor).
     */
    public boolean hasDermalCamoStealth() {
        return hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR)
              && (getMovementMode().isLegInfantry() || getMovementMode().isJumpInfantry())
              && (getArmorKit() == null);
    }

    public boolean hasMicrolite() {
        return isMicrolite;
    }

    public void setMicrolite(boolean microlite) {
        this.isMicrolite = microlite;
        // Keep VTOL MP in sync when the unit is already in VTOL mode, so the MP is correct no matter
        // whether the microlite flag is set before or after the movement mode (TO:AUE p.136).
        if (getMovementMode() == EntityMovementMode.VTOL) {
            setOriginalJumpMP(microlite ? MICROLITE_VTOL_MP : MICRO_COPTER_VTOL_MP);
        }
    }

    public void setMount(InfantryMount mount) {
        this.mount = mount;
        if (mount != null) {
            setMovementMode(mount.movementMode());
            if (mount.movementMode().isLegInfantry()) {
                setOriginalWalkMP(mount.getMP());
            } else {
                setOriginalWalkMP(mount.secondaryGroundMP());
                setOriginalJumpMP(mount.getMP());
            }
        }
        calcDamageDivisor();
    }


    public @Nullable InfantryMount getMount() {
        return mount;
    }

    public boolean isMounted() {
        return mount != null;
    }

    @Override
    public boolean isDmgHeavy() {
        return (((double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.5);
    }

    @Override
    public boolean isDmgModerate() {
        return (((double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.75);
    }

    @Override
    public boolean isDmgLight() {
        return (((double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.9);
    }

    /**
     * @return True if this infantry has any field gun or artillery (destroyed or not).
     */
    public boolean hasFieldWeapon() {
        return !originalFieldWeapons().isEmpty();
    }

    /**
     * @return True if this infantry has any working (not destroyed) field gun or artillery.
     */
    public boolean hasActiveFieldWeapon() {
        return !activeFieldWeapons().isEmpty();
    }

    /**
     * @return True if this infantry has a field artillery weapon that is not destroyed.
     */
    public boolean hasActiveFieldArtillery() {
        return activeFieldWeapons().stream().anyMatch(TestInfantry::isFieldArtilleryWeapon);
    }

    @Override
    public double getWeight() {
        return TestInfantry.getWeight(this);
    }

    @Override
    public boolean isCrippled() {
        double activeTroopPercent = (double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY);
        if (activeTroopPercent < 0.25) {
            logger.debug("{} CRIPPLED: Only {} troops remaining.",
                  getDisplayName(),
                  NumberFormat.getPercentInstance().format(activeTroopPercent));
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted) throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add equipment slots for ammo switching of field guns and field artillery
        addCritical(loc, new CriticalSlot(mounted));
    }


    @Override
    public long getEntityType() {
        return Entity.ETYPE_INFANTRY;
    }

    @Override
    public int getSpriteDrawPriority() {
        return 1;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.586 + 0.336 * Math.log(getWeight())));
    }

    @Override
    public boolean isConventionalInfantry() {
        return true;
    }

    @Override
    public String getMovementModeAsString() {
        if (mount == null) {
            if (getMovementMode().isVTOL()) {
                return hasMicrolite() ? "Microlite" : "Microcopter";
            }
            if (getMovementMode().isUMUInfantry()) {
                return getOriginalJumpMP() > 1 ? "Motorized SCUBA" : "SCUBA";
            }
        }
        return super.getMovementModeAsString();
    }

    // region Prosthetic Enhancement

    // --- Slot 1 (Standard Enhanced uses this, Improved Enhanced uses both) ---

    /**
     * @return The prosthetic enhancement type in slot 1, or null if none
     */
    public @Nullable ProstheticEnhancementType getProstheticEnhancement1() {
        return prostheticEnhancement1;
    }

    /**
     * Sets the prosthetic enhancement type in slot 1.
     *
     * @param enhancement The enhancement type to set, or null to remove
     */
    public void setProstheticEnhancement1(@Nullable ProstheticEnhancementType enhancement) {
        this.prostheticEnhancement1 = enhancement;
        if (enhancement == null) {
            prostheticEnhancement1Count = 0;
        }
    }

    /**
     * @return The number of slot 1 enhancements per trooper (0, 1, or 2)
     */
    public int getProstheticEnhancement1Count() {
        return prostheticEnhancement1Count;
    }

    /**
     * Sets the number of slot 1 enhancements per trooper. Clamped to valid range of 0-2.
     *
     * @param count The enhancement count (0, 1, or 2)
     */
    public void setProstheticEnhancement1Count(int count) {
        this.prostheticEnhancement1Count = Math.clamp(count, 0, 2);
    }

    // --- Slot 2 (Improved Enhanced only) ---

    /**
     * @return The prosthetic enhancement type in slot 2, or null if none
     */
    public @Nullable ProstheticEnhancementType getProstheticEnhancement2() {
        return prostheticEnhancement2;
    }

    /**
     * Sets the prosthetic enhancement type in slot 2.
     *
     * @param enhancement The enhancement type to set, or null to remove
     */
    public void setProstheticEnhancement2(@Nullable ProstheticEnhancementType enhancement) {
        this.prostheticEnhancement2 = enhancement;
        if (enhancement == null) {
            prostheticEnhancement2Count = 0;
        }
    }

    /**
     * @return The number of slot 2 enhancements per trooper (0, 1, or 2)
     */
    public int getProstheticEnhancement2Count() {
        return prostheticEnhancement2Count;
    }

    /**
     * Sets the number of slot 2 enhancements per trooper. Clamped to valid range of 0-2.
     *
     * @param count The enhancement count (0, 1, or 2)
     */
    public void setProstheticEnhancement2Count(int count) {
        this.prostheticEnhancement2Count = Math.clamp(count, 0, 2);
    }

    // --- Combined helpers ---

    /**
     * @return True if this unit has any prosthetic enhancement configured (in either slot)
     */
    public boolean hasProstheticEnhancement() {
        return (prostheticEnhancement1 != null && prostheticEnhancement1Count > 0)
              || (prostheticEnhancement2 != null && prostheticEnhancement2Count > 0);
    }

    /**
     * @return True if this unit has a prosthetic enhancement in slot 1
     */
    public boolean hasProstheticEnhancement1() {
        return prostheticEnhancement1 != null && prostheticEnhancement1Count > 0;
    }

    /**
     * @return True if this unit has a prosthetic enhancement in slot 2
     */
    public boolean hasProstheticEnhancement2() {
        return prostheticEnhancement2 != null && prostheticEnhancement2Count > 0;
    }

    /**
     * Calculates the total prosthetic enhancement damage bonus per trooper from both slots. This sums the damage from
     * both enhancement slots.
     *
     * @return The total damage bonus per trooper from all prosthetic enhancements
     */
    public double getProstheticDamageBonus() {
        double bonus = 0.0;
        if (hasProstheticEnhancement1()) {
            bonus += prostheticEnhancement1.getDamagePerTrooper() * prostheticEnhancement1Count;
        }
        if (hasProstheticEnhancement2()) {
            bonus += prostheticEnhancement2.getDamagePerTrooper() * prostheticEnhancement2Count;
        }
        return bonus;
    }

    /**
     * Gets the best (most negative) anti-Mek modifier from all prosthetic enhancements.
     *
     * @return The best anti-Mek modifier, or 0 if no enhancement provides one
     */
    public int getBestProstheticAntiMekModifier() {
        int best = 0;
        // Check regular prosthetic enhancements
        if (hasProstheticEnhancement1() && prostheticEnhancement1.hasAntiMekBonus()) {
            best = Math.min(best, prostheticEnhancement1.getAntiMekModifier());
        }
        if (hasProstheticEnhancement2() && prostheticEnhancement2.hasAntiMekBonus()) {
            best = Math.min(best, prostheticEnhancement2.getAntiMekModifier());
        }
        // Check extraneous limb enhancements (only the single best modifier from all sources applies)
        if (hasExtraneousPair1() && extraneousPair1.hasAntiMekBonus()) {
            best = Math.min(best, extraneousPair1.getAntiMekModifier());
        }
        if (hasExtraneousPair2() && extraneousPair2.hasAntiMekBonus()) {
            best = Math.min(best, extraneousPair2.getAntiMekModifier());
        }
        return best;
    }

    /**
     * Gets the display name of the enhancement providing the best anti-Mek bonus.
     *
     * @return The display name, or null if no enhancement provides anti-Mek bonus
     */
    public @Nullable String getBestProstheticAntiMekName() {
        int best = 0;
        String name = null;
        // Check regular prosthetic enhancements
        if (hasProstheticEnhancement1() && prostheticEnhancement1.hasAntiMekBonus()) {
            if (prostheticEnhancement1.getAntiMekModifier() < best) {
                best = prostheticEnhancement1.getAntiMekModifier();
                name = prostheticEnhancement1.getDisplayName();
            }
        }
        if (hasProstheticEnhancement2() && prostheticEnhancement2.hasAntiMekBonus()) {
            if (prostheticEnhancement2.getAntiMekModifier() < best) {
                best = prostheticEnhancement2.getAntiMekModifier();
                name = prostheticEnhancement2.getDisplayName();
            }
        }
        // Check extraneous limb enhancements
        if (hasExtraneousPair1() && extraneousPair1.hasAntiMekBonus()) {
            if (extraneousPair1.getAntiMekModifier() < best) {
                best = extraneousPair1.getAntiMekModifier();
                name = extraneousPair1.getDisplayName();
            }
        }
        if (hasExtraneousPair2() && extraneousPair2.hasAntiMekBonus()) {
            if (extraneousPair2.getAntiMekModifier() < best) {
                best = extraneousPair2.getAntiMekModifier();
                name = extraneousPair2.getDisplayName();
            }
        }
        return name;
    }

    /**
     * Checks if any prosthetic enhancement provides an Anti-Mek BV multiplier. Per IO p.84, Grappler Lines and Climbing
     * Claws provide a 1.2x multiplier on the Anti-Mek Battle Rating.
     *
     * @return True if any enhancement provides an Anti-Mek BV multiplier
     */
    public boolean hasAntiMekBvMultiplier() {
        return getBestProstheticAntiMekModifier() != 0;
    }

    /**
     * Gets the Anti-Mek BV multiplier from prosthetic enhancements. Per IO p.84, units with Grappler Lines or Climbing
     * Claws multiply their Anti-Mek Battle Rating by 1.2.
     *
     * @return 1.2 if the unit has Grappler or Climbing Claws, 1.0 otherwise
     */
    public double getAntiMekBvMultiplier() {
        return hasAntiMekBvMultiplier() ? 1.2 : 1.0;
    }

    /**
     * Checks if any prosthetic enhancement (regular or extraneous) is a melee type.
     *
     * @return True if any enhancement is melee
     */
    public boolean hasProstheticMeleeEnhancement() {
        return (hasProstheticEnhancement1() && prostheticEnhancement1.isMelee())
              || (hasProstheticEnhancement2() && prostheticEnhancement2.isMelee())
              || (hasExtraneousPair1() && extraneousPair1.isMelee())
              || (hasExtraneousPair2() && extraneousPair2.isMelee());
    }

    /**
     * Gets the melee to-hit modifier from prosthetic enhancements. Per IO p.83, maximum modifier is +2 regardless of
     * number of melee weapons.
     *
     * @return The melee to-hit modifier (capped at +2)
     */
    public int getProstheticMeleeToHitModifier() {
        int modifier = 0;
        // Check regular prosthetic enhancements
        if (hasProstheticEnhancement1() && prostheticEnhancement1.isMelee()) {
            modifier = Math.max(modifier, prostheticEnhancement1.getToHitModifier());
        }
        if (hasProstheticEnhancement2() && prostheticEnhancement2.isMelee()) {
            modifier = Math.max(modifier, prostheticEnhancement2.getToHitModifier());
        }
        // Check extraneous limb enhancements
        if (hasExtraneousPair1() && extraneousPair1.isMelee()) {
            modifier = Math.max(modifier, extraneousPair1.getToHitModifier());
        }
        if (hasExtraneousPair2() && extraneousPair2.isMelee()) {
            modifier = Math.max(modifier, extraneousPair2.getToHitModifier());
        }
        // Cap at +2 per IO p.83
        return Math.min(modifier, 2);
    }

    // --- Legacy compatibility methods ---

    /**
     * @return The prosthetic enhancement type in slot 1 (legacy method)
     *
     * @deprecated Use {@link #getProstheticEnhancement1()} instead
     */
    @Deprecated(since = "0.50.07")
    public @Nullable ProstheticEnhancementType getProstheticEnhancement() {
        return getProstheticEnhancement1();
    }

    /**
     * Sets the prosthetic enhancement type in slot 1 (legacy method).
     *
     * @param enhancement The enhancement type to set
     *
     * @deprecated Use {@link #setProstheticEnhancement1(ProstheticEnhancementType)} instead
     */
    @Deprecated(since = "0.50.07")
    public void setProstheticEnhancement(@Nullable ProstheticEnhancementType enhancement) {
        setProstheticEnhancement1(enhancement);
    }

    /**
     * @return The slot 1 enhancement count (legacy method)
     *
     * @deprecated Use {@link #getProstheticEnhancement1Count()} instead
     */
    @Deprecated(since = "0.50.07")
    public int getProstheticEnhancementCount() {
        return getProstheticEnhancement1Count();
    }

    /**
     * Sets the slot 1 enhancement count (legacy method).
     *
     * @param count The count to set
     *
     * @deprecated Use {@link #setProstheticEnhancement1Count(int)} instead
     */
    @Deprecated(since = "0.50.07")
    public void setProstheticEnhancementCount(int count) {
        setProstheticEnhancement1Count(count);
    }

    // --- Extraneous (Enhanced) Limbs methods ---

    /**
     * @return The prosthetic enhancement type for extraneous pair 1, or null if not set
     */
    public @Nullable ProstheticEnhancementType getExtraneousPair1() {
        return extraneousPair1;
    }

    /**
     * Sets the prosthetic enhancement type for extraneous pair 1. Each pair always provides 2 items.
     *
     * @param enhancement The enhancement type to set, or null to clear
     */
    public void setExtraneousPair1(@Nullable ProstheticEnhancementType enhancement) {
        this.extraneousPair1 = enhancement;
    }

    /**
     * @return True if extraneous pair 1 has an enhancement type set
     */
    public boolean hasExtraneousPair1() {
        return extraneousPair1 != null;
    }

    /**
     * @return The prosthetic enhancement type for extraneous pair 2, or null if not set
     */
    public @Nullable ProstheticEnhancementType getExtraneousPair2() {
        return extraneousPair2;
    }

    /**
     * Sets the prosthetic enhancement type for extraneous pair 2. Each pair always provides 2 items.
     *
     * @param enhancement The enhancement type to set, or null to clear
     */
    public void setExtraneousPair2(@Nullable ProstheticEnhancementType enhancement) {
        this.extraneousPair2 = enhancement;
    }

    /**
     * @return True if extraneous pair 2 has an enhancement type set
     */
    public boolean hasExtraneousPair2() {
        return extraneousPair2 != null;
    }

    /**
     * @return True if this infantry unit has any extraneous limb pairs configured
     */
    public boolean hasExtraneousLimbs() {
        return hasExtraneousPair1() || hasExtraneousPair2();
    }

    /**
     * Gets the total bonus damage per trooper from extraneous limb enhancements. Each pair provides 2 items (1 per
     * limb), so the damage is multiplied by 2 for each pair.
     *
     * @return Total damage bonus from extraneous limb enhancements
     */
    public double getExtraneousDamageBonus() {
        double bonus = 0;
        if (hasExtraneousPair1() && extraneousPair1.hasDamageBonus()) {
            bonus += extraneousPair1.getDamagePerTrooper() * 2; // 2 items per pair
        }
        if (hasExtraneousPair2() && extraneousPair2.hasDamageBonus()) {
            bonus += extraneousPair2.getDamagePerTrooper() * 2; // 2 items per pair
        }
        return bonus;
    }

    /**
     * Gets the best anti-Mek modifier from extraneous limb enhancements. Multiple enhancements do not stack; only the
     * best modifier is used.
     *
     * @return Best anti-Mek modifier from extraneous limbs (negative values are better), or 0 if none
     */
    public int getExtraneousAntiMekModifier() {
        int best = 0;
        if (hasExtraneousPair1() && extraneousPair1.hasAntiMekBonus()) {
            best = Math.min(best, extraneousPair1.getAntiMekModifier());
        }
        if (hasExtraneousPair2() && extraneousPair2.hasAntiMekBonus()) {
            best = Math.min(best, extraneousPair2.getAntiMekModifier());
        }
        return best;
    }

    /**
     * Gets the name of the extraneous limb enhancement that provides the best anti-Mek modifier.
     *
     * @return Name of the enhancement with best anti-Mek modifier, or null if none
     */
    public @Nullable String getExtraneousAntiMekEnhancementName() {
        int best = 0;
        String name = null;
        if (hasExtraneousPair1() && extraneousPair1.hasAntiMekBonus()) {
            if (extraneousPair1.getAntiMekModifier() < best) {
                best = extraneousPair1.getAntiMekModifier();
                name = extraneousPair1.getDisplayName();
            }
        }
        if (hasExtraneousPair2() && extraneousPair2.hasAntiMekBonus()) {
            if (extraneousPair2.getAntiMekModifier() < best) {
                best = extraneousPair2.getAntiMekModifier();
                name = extraneousPair2.getDisplayName();
            }
        }
        return name;
    }

    /**
     * @return True if any extraneous limb enhancement is a melee weapon
     */
    public boolean hasExtraneousMeleeEnhancement() {
        return (hasExtraneousPair1() && extraneousPair1.isMelee())
              || (hasExtraneousPair2() && extraneousPair2.isMelee());
    }

    /**
     * Gets the melee to-hit modifier from extraneous limb enhancements. Per IO p.83, maximum modifier is +2 regardless
     * of number of melee weapons.
     *
     * @return The melee to-hit modifier from extraneous limbs (capped at +2)
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public int getExtraneousMeleeToHitModifier() {
        int modifier = 0;
        if (hasExtraneousPair1() && extraneousPair1.isMelee()) {
            modifier = Math.max(modifier, extraneousPair1.getToHitModifier());
        }
        if (hasExtraneousPair2() && extraneousPair2.isMelee()) {
            modifier = Math.max(modifier, extraneousPair2.getToHitModifier());
        }
        // Cap at +2 per IO p.83
        return Math.min(modifier, 2);
    }

    // endregion Prosthetic Enhancement


    /**
     * Marks field guns and artillery as hit according to their crew requirement when losing troopers. Having them
     * destroyed requires a later call of {@link #applyDamage()}. This method does not restore FG/FA when damage is
     * removed from the unit. Only affects Conventional Infantry, not BattleArmor (can be called safely on BA).
     */
    protected void damageFieldWeapons() {
        int totalCrewNeeded = 0;
        for (Mounted<?> weapon : activeFieldWeapons()) {
            totalCrewNeeded += requiredCrewForFieldWeapon((WeaponType) weapon.getType());
            if (totalCrewNeeded > activeTroopers) {
                weapon.setHit(true);
            }
        }
    }

    /**
     * Destroys and restores field guns and artillery according to their crew requirements. This method is intended for
     * any place where damage can be assigned and removed without cost (lobby). Only affects Conventional Infantry, not
     * BattleArmor (can be called safely on BA).
     */
    public void damageOrRestoreFieldWeapons() {
        int totalCrewNeeded = 0;
        for (Mounted<?> weapon : originalFieldWeapons()) {
            totalCrewNeeded += requiredCrewForFieldWeapon((WeaponType) weapon.getType());
            weapon.setHit(totalCrewNeeded > activeTroopers);
            weapon.setDestroyed(totalCrewNeeded > activeTroopers);
        }
    }

    /**
     * Field guns that are hit (uncrewed) will be set to not hit if they have the appropriate number of active troopers.
     * Field guns that are destroyed will not be un-hit.
     */
    public void restoreUncrewedFieldWeapons() {
        int totalCrewNeeded = 0;
        for (Mounted<?> weapon : originalFieldWeapons()) {
            totalCrewNeeded += requiredCrewForFieldWeapon((WeaponType) weapon.getType());
            if (activeTroopers >= totalCrewNeeded && !weapon.isDestroyed()) {
                weapon.setHit(false);
            }
        }
    }

    /**
     * @return The crew required to operate the given field gun or field artillery weapon, TO:AUE p.123. The rules are
     *       silent on rounding for the weight of artillery, therefore adopting that of field guns.
     */
    public int requiredCrewForFieldWeapon(WeaponType weaponType) {
        int roundedWeight = (int) Math.ceil(weaponType.getTonnage(this));
        return weaponType.hasFlag(WeaponType.F_ARTILLERY) ? roundedWeight : Math.max(2, roundedWeight);
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !(hasActiveFieldArtillery() || getAlreadyTwisted());
    }

    @Override
    public void setInternal(int val, int loc) {
        super.setInternal(val, loc);
        if (loc == LOC_INFANTRY) {
            activeTroopers = Math.max(val, 0);
            damageFieldWeapons();
            restoreUncrewedFieldWeapons();
        }
    }

    @Override
    public double getInternalRemainingPercent() {
        // the remaining trooper percentage
        return (double) Math.max(activeTroopers, 0) / originalTrooperCount;
    }

    @Override
    public void initializeInternal(int val, int loc) {
        if (loc == LOC_INFANTRY) {
            originalTrooperCount = val;
            troopersShooting = val;
        }
        super.initializeInternal(val, loc);
    }

    @Override
    public boolean isSecondaryArcWeapon(int wn) {
        return isFieldWeapon((getEquipment(wn))) && !hasActiveFieldArtillery();
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(squadSize * squadCount, LOC_INFANTRY);
    }

    /**
     * Returns true if the current extraneous limb configuration exceeds the allowed maximum. Per IO p.85, if any wing
     * prosthetics are installed, only one pair of extraneous limbs is allowed.
     *
     * @return true if invalid configuration (too many extraneous limb pairs)
     */
    public boolean hasExcessiveExtraneousLimbs() {
        boolean hasWings = hasAbility(OptionsConstants.MD_PL_GLIDER) || hasAbility(OptionsConstants.MD_PL_FLIGHT);
        if (!hasWings) {
            return false;
        }
        // With any wing type, only 1 pair allowed - pair 2 must be empty
        return hasExtraneousPair2();
    }

    @Override
    public String getMovementString(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_NONE -> "None";
            case MOVE_WALK, MOVE_RUN -> switch (getMovementMode()) {
                case INF_LEG -> mount == null ? "Walked" : "Rode";
                case INF_MOTORIZED -> "Biked";
                case HOVER, TRACKED, WHEELED -> "Drove";
                default -> "Unknown!";
            };
            case MOVE_VTOL_WALK, MOVE_VTOL_RUN -> "Flew";
            case MOVE_JUMP -> "Jumped";
            default -> "Unknown!";
        };
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        Mounted<?> weapon = getEquipment(weaponNumber);
        // Infantry can fire all around themselves. But field guns are set up to a
        // vehicular turret facing
        if (isFieldWeapon(weapon)) {
            if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS)) {
                return Compute.ARC_TURRET;
            }
            return Compute.ARC_FORWARD;
        }
        // Hitting the Deck (TO:AR p.106): a unit that can only move or fire (i.e. one set up with field weapons)
        // must designate a facing and may only fire in its front arc while on the deck.
        if (isHitTheDeck() && hasActiveFieldWeapon()) {
            return Compute.ARC_FORWARD;
        }
        // According to TacOps rules, dug-in units no longer have to declare a facing
        return Compute.ARC_360;
    }

    /**
     * Checks if this infantry unit is protected from gas attacks (including pheromone and toxin gas attacks).
     * Protection comes from MD_FILTRATION implant or hostile environment gear (space suit, XCT vacuum, or toxic
     * atmosphere armor kits).
     *
     * @return true if protected from gas attacks
     */
    public boolean isProtectedFromGasAttacks() {
        // Check for filtration implants
        if (hasAbility(OptionsConstants.MD_FILTRATION)) {
            return true;
        }

        // Check for hostile environment armor kit
        EquipmentType armorKit = getArmorKit();
        if (armorKit != null) {
            return armorKit.hasAnyFlag(MiscTypeFlag.S_SPACE_SUIT, MiscTypeFlag.S_XCT_VACUUM,
                  MiscTypeFlag.S_TOXIC_ATMOSPHERE);
        }

        return false;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return InfantryCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        int walkMP = getWalkMP(mpCalculationSetting);
        if (!mpCalculationSetting.ignoreOptionalRules() &&
              (game != null) &&
              gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE)) {
            return (walkMP > 0) ? walkMP + 1 : walkMP + 2;
        } else {
            return walkMP;
        }
    }

    /**
     * @return True when this conventional infantry unit has ground movement of 0*, meaning it may still move a single
     *       hex despite having 0 walking MP.
     */
    public boolean hasMinimalGroundMP(MPCalculationSetting mpCalculationSetting) {
        return isConventionalInfantry() && (getWalkMP(mpCalculationSetting) == 0);
    }

    /**
     * @return True when this conventional infantry unit has ground movement of 0*, meaning it may still move a single
     *       hex despite having 0 walking MP.
     */
    public boolean hasMinimalGroundMP() {
        return hasMinimalGroundMP(MPCalculationSetting.STANDARD);
    }

    /**
     * Per TO:AR p.25, 0 MP infantry that used fast movement (MOVE_RUN) in the previous turn cannot move or fire in the
     * following turn.
     *
     * @return true if this unit is exhausted from using fast movement last round
     */
    public boolean isExhaustedFromFastMove() {
        return gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE)
              && (getWalkMP() == 0)
              && (movedLastRound == EntityMovementType.MOVE_RUN);
    }

    @Override
    public boolean isEligibleForMovement() {
        return !isExhaustedFromFastMove() && super.isEligibleForMovement();
    }

    public boolean usedFastMoveThisTurn() {
        return gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE)
              && (moved == EntityMovementType.MOVE_RUN);
    }

    @Override
    public boolean isEligibleForFiring() {
        return !isExhaustedFromFastMove() && !usedFastMoveThisTurn() && super.isEligibleForFiring();
    }

    /**
     * @return true if this unit is impaired by pheromone gas attack (IO pg 79)
     */
    public boolean isPheromoneImpaired() {
        return pheromoneImpaired;
    }

    /**
     * Sets whether this unit is impaired by pheromone gas attack. Impaired units suffer +1 to-hit on all actions for
     * remainder of scenario.
     *
     * @param impaired true to mark as pheromone impaired
     */
    public void setPheromoneImpaired(boolean impaired) {
        this.pheromoneImpaired = impaired;
    }

    /**
     * Standard and motorized SCUBA only differ in base movement, so they both use INF_UMU. If the motion_type contains
     * the string "motorized", the movement is set here instead.
     */
    public void setMotorizedScuba() {
        setMovementMode(EntityMovementMode.INF_UMU);
        setOriginalJumpMP(2);
    }

    @Override
    public boolean isEligibleForPavementOrRoadBonus() {
        return gameOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_INF_PAVE_BONUS)
              && (movementMode.isTrackedWheeledOrHover() || movementMode == EntityMovementMode.INF_MOTORIZED);
    }

    /**
     * Used to check for standard or motorized SCUBA infantry, which have a maximum depth of 2.
     *
     * @return true if this is a conventional infantry unit with non-mechanized SCUBA specialization
     */
    public boolean isNonMechSCUBA() {
        return getMovementMode().isUMUInfantry();
    }

    @Override
    public boolean isNuclearHardened() {
        return false;
    }

    /**
     * Returns true if this infantry unit can use glider wings in the current conditions. Glider wings cannot be used in
     * vacuum or trace (very thin) atmospheres (IO:AE 3rd p.79).
     *
     * @return true if glider wings are usable
     */
    public boolean canUseGliderWings() {
        // Allow if no game context
        return (game == null) || game.getPlanetaryConditions().getAtmosphere().isDenserThan(Atmosphere.TRACE);
    }

    /**
     * Returns true if this infantry unit can use powered flight wings in the current conditions. Powered flight wings
     * cannot be used in vacuum (IO:AE 3rd p.79). Note: Unlike glider wings, powered flight only mentions vacuum
     * restriction, not trace atmosphere. However, for consistency and realism, we apply the same atmospheric
     * restriction as glider wings.
     *
     * @return true if powered flight wings are usable
     */
    public boolean canUsePoweredFlightWings() {
        // Allow if no game context
        return canUseGliderWings();
    }

    /**
     * Returns the maximum downward elevation change this infantry can make. Infantry with glider wings can descend any
     * number of levels safely (IO p.85).
     */
    @Override
    public int getMaxElevationDown(int currElevation) {
        if (canUseGliderWings() && hasAbility(OptionsConstants.MD_PL_GLIDER)) {
            return Integer.MAX_VALUE;
        }
        return getMaxElevationChange();
    }

    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(Entity.LOC_DESTROYED);
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    public static TechAdvancement getCombatEngineerTA() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.A,
                    AvailabilityValue.B,
                    AvailabilityValue.A,
                    AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getMarineTA() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getMountainTA() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getParatrooperTA() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A,
                    AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getParamedicTA() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C,
                    AvailabilityValue.C,
                    AvailabilityValue.C,
                    AvailabilityValue.C)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getTAGTroopsTA() {
        return new TechAdvancement(TechBase.ALL).setISAdvancement(2585,
                    2600,
                    ITechnology.DATE_NONE,
                    2535,
                    3037)
              .setClanAdvancement(2585, 2600)
              .setApproximate(true, false, false, false, false)
              .setTechRating(TechRating.E)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS)
              .setAvailability(AvailabilityValue.F,
                    AvailabilityValue.X,
                    AvailabilityValue.E,
                    AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public boolean canExitVTOLWithGliderWings() {
        return hasAbility(OptionsConstants.MD_PL_GLIDER) && canUseGliderWings();
    }
}
