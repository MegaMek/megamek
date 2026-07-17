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

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.GamePhase;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.game.Game;
import megamek.common.interfaces.ITechnology;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;

/**
 * This is a superclass for infantry unit types, Conventional Infantry and BattleArmor. Other personnel on the
 * battlefield (e.g., ejected crew) are subtypes of Conventional Infantry.
 * <p>
 * Note that formerly, this class represented conventional infantry directly and BattleArmor was a subtype. This has
 * been changed as a substantional number of rules for CI/BA do not overlap.
 */
public abstract class Infantry extends Entity {

    private static final MMLogger LOGGER = MMLogger.create(Infantry.class);

    protected int squadCount = 1;
    protected int squadSize = 1;
    protected int originalTrooperCount;

    /**
     * The number of troopers alive in this platoon at the beginning of the phase, before taking damage.
     */
    protected int troopersShooting;

    /**
     * The number of troopers alive in this platoon, including any damage sustained in the current phase.
     */
    protected int activeTroopers;

    public int turnsLayingExplosives = -1;

    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_WORKING = 1; // no protection, can't attack
    public static final int DUG_IN_COMPLETE = 2; // protected, restricted arc
    public static final int DUG_IN_FORTIFYING1 = 3; // no protection, can't attack
    public static final int DUG_IN_FORTIFYING2 = 4; // no protection, can't attack
    public static final int DUG_IN_FORTIFYING3 = 5; // no protection, can't attack
    private int dugIn = DUG_IN_NONE;

    /**
     * Tracks damage taken between turns while digging in or fortifying, so that being attacked extends the effort by
     * one turn (TO:AR p.106 / TO:AUE p.153). Server-side runtime state; not written to save files (dug-in progress is
     * itself not persisted).
     */
    private transient FortifyState fortifyState = new FortifyState();

    /**
     * @return the fortify damage tracker, recreated lazily so it is never {@code null} on a deserialized platoon (the field is
     *       transient runtime state, so its initializer does not run during deserialization).
     */
    private FortifyState fortifyState() {
        if (fortifyState == null) {
            fortifyState = new FortifyState();
        }
        return fortifyState;
    }

    /**
     * Number of idle turns (no movement, no fire) the unit must spend "hitting the deck" before it may convert to "dug
     * in" status. Per TO:AR p.106.
     */
    public static final int HIT_DECK_TURNS_TO_DIG_IN = 2;

    /** True when this infantry is "hitting the deck", TO:AR p.106. */
    private boolean hitTheDeck = false;

    /** Consecutive turns spent on the deck without moving or firing, used for the dig-in conversion. */
    private int turnsOnDeck = 0;

    /** Transient flag set when an on-deck unit fires; breaks the idle streak for the dig-in conversion. */
    private boolean firedWhileOnDeck = false;

    private boolean isTakingCover = false;
    private boolean canCallSupport = true;
    private boolean isCallingSupport = false;

    // Anti-Mek attacks
    public static final String LEG_ATTACK = "LegAttack";
    public static final String SWARM_MEK = "SwarmMek";
    public static final String SWARM_WEAPON_MEK = "SwarmWeaponMek";
    public static final String STOP_SWARM = "StopSwarm";

    private static final int[] NUM_OF_SLOTS = { 20, 20 };

    /**
     * Generate a new, blank, infantry platoon. Hopefully, we'll be loaded from somewhere.
     */
    protected Infantry() {
        // Create a "dead" leg rifle platoon.
        originalTrooperCount = 0;
        troopersShooting = 0;
        activeTroopers = 0;
        setMovementMode(EntityMovementMode.INF_LEG);
        setOriginalWalkMP(1);
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.INFANTRY_CREW;
    }

    /**
     * Generates the {@link TechAdvancement} for the unit's motive type. A value of EntityMovementMode.NONE indicates
     * Beast-mounted infantry.
     *
     * @param movementMode An infantry movement mode.
     *
     * @return The Tech Advancement data for the movement mode.
     */
    public static TechAdvancement getMotiveTechAdvancement(EntityMovementMode movementMode) {
        TechAdvancement techAdvancement = new TechAdvancement(TechBase.ALL).setAdvancement(ITechnology.DATE_PS,
                    ITechnology.DATE_PS,
                    ITechnology.DATE_PS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        switch (movementMode) {
            case INF_MOTORIZED:
                techAdvancement.setTechRating(TechRating.B)
                      .setAvailability(AvailabilityValue.A,
                            AvailabilityValue.A,
                            AvailabilityValue.A,
                            AvailabilityValue.A)
                      .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case INF_JUMP:
                techAdvancement.setAdvancement(ITechnology.DATE_ES, ITechnology.DATE_ES, ITechnology.DATE_ES)
                      .setTechRating(TechRating.D)
                      .setAvailability(AvailabilityValue.B,
                            AvailabilityValue.B,
                            AvailabilityValue.B,
                            AvailabilityValue.B)
                      .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case INF_UMU:
                techAdvancement.setAdvancement(ITechnology.DATE_PS, ITechnology.DATE_PS)
                      .setTechRating(TechRating.B)
                      .setAvailability(AvailabilityValue.D,
                            AvailabilityValue.D,
                            AvailabilityValue.D,
                            AvailabilityValue.D)
                      .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case WHEELED:
                techAdvancement.setTechRating(TechRating.A)
                      .setAvailability(AvailabilityValue.A,
                            AvailabilityValue.B,
                            AvailabilityValue.A,
                            AvailabilityValue.A)
                      .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case TRACKED:
                techAdvancement.setTechRating(TechRating.B)
                      .setAvailability(AvailabilityValue.B,
                            AvailabilityValue.C,
                            AvailabilityValue.B,
                            AvailabilityValue.B)
                      .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case HOVER:
                techAdvancement.setTechRating(TechRating.C)
                      .setAvailability(AvailabilityValue.A,
                            AvailabilityValue.B,
                            AvailabilityValue.A,
                            AvailabilityValue.B)
                      .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case VTOL:
                techAdvancement.setAdvancement(ITechnology.DATE_ES, ITechnology.DATE_ES)
                      .setTechRating(TechRating.C)
                      .setAvailability(AvailabilityValue.C,
                            AvailabilityValue.D,
                            AvailabilityValue.D,
                            AvailabilityValue.C)
                      .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case SUBMARINE:
                techAdvancement.setAdvancement(ITechnology.DATE_PS, ITechnology.DATE_PS)
                      .setTechRating(TechRating.C)
                      .setAvailability(AvailabilityValue.D,
                            AvailabilityValue.D,
                            AvailabilityValue.D,
                            AvailabilityValue.D)
                      .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case NONE:
                // Beast-mounted
                techAdvancement.setAdvancement(ITechnology.DATE_PS, ITechnology.DATE_PS)
                      .setTechRating(TechRating.A)
                      .setAvailability(AvailabilityValue.A,
                            AvailabilityValue.A,
                            AvailabilityValue.A,
                            AvailabilityValue.A)
                      .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case INF_LEG:
            default:
                techAdvancement.setTechRating(TechRating.A)
                      .setAvailability(AvailabilityValue.A,
                            AvailabilityValue.A,
                            AvailabilityValue.A,
                            AvailabilityValue.A)
                      .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
        }
        return techAdvancement;
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return true;
    }

    @Override
    public int clipSecondaryFacing(int dir) {
        return dir;
    }

    /**
     * Creates a local platoon (CO 4th p.82, Urban Guerilla)
     */
    public void createLocalSupport() {
        if (Compute.isInUrbanEnvironment(game, getPosition())) {
            setIsCallingSupport(true);
            canCallSupport = false;
        }
    }

    public void setIsCallingSupport(boolean b) {
        isCallingSupport = b;
    }

    public boolean getIsCallingSupport() {
        return isCallingSupport;
    }

    @Override
    public boolean hasUMU() {
        return getMovementMode().isUMUInfantry() || getMovementMode().isSubmarine();
    }

    @Override
    public int getActiveUMUCount() {
        return getAllUMUCount();
    }

    @Override
    public int getAllUMUCount() {
        return hasUMU() ? jumpMP : 0;
    }

    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_NONE -> "N";
            case MOVE_WALK -> "W";
            case MOVE_RUN -> switch (getMovementMode()) {
                case INF_LEG -> "R";
                case INF_MOTORIZED -> "B";
                case HOVER, TRACKED, WHEELED -> "D";
                default -> "?";
            };
            case MOVE_JUMP -> "J";
            default -> "?";
        };
    }

    /**
     * @return The full original strength (trooper count) of this infantry unit
     */
    public int getOriginalTrooperCount() {
        return originalTrooperCount;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025, Report.PUBLIC);
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7041, Report.PUBLIC);
        r.add(getCrew().getGunnery());
        vDesc.addElement(r);

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
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        // EI bonus for anti-Mek attacks per IO p.69
        // "All Piloting Skill rolls required for the EI-equipped unit receives a -1 target number modifier.
        // This includes checks made for physical attacks, as well as anti-Mek attacks by EI-equipped battle armor."
        if (hasActiveEiCockpit()) {
            prd.addModifier(-1, "Enhanced Imaging");
        }
        return prd;
    }

    @Override
    public void applyDamage() {
        super.applyDamage();
        troopersShooting = activeTroopers;
    }

    public int getActiveTroopers() {
        return activeTroopers;
    }

    /**
     * @return The number of troopers in the platoon before damage to the current phase is applied.
     */
    public int getShootingStrength() {
        return troopersShooting;
    }

    @Override
    public boolean canCharge() {
        return false;
    }

    @Override
    public boolean canDFA() {
        return false;
    }

    @Override
    public PilotingRollData checkBogDown(MoveStep step, EntityMovementType moveType, Hex curHex, Coords lastPos,
          Coords curPos, int lastElev, boolean isPavementStep) {
        return checkBogDown(step, curHex, lastPos, curPos, isPavementStep);
    }

    public PilotingRollData checkBogDown(MoveStep step, Hex curHex, Coords lastPos, Coords curPos,
          boolean isPavementStep) {
        PilotingRollData roll = new PilotingRollData(getId(), 4, "entering boggy terrain");
        int bgMod = curHex.getBogDownModifier(getMovementMode(), false);
        final boolean onBridge = (curHex.terrainLevel(Terrains.BRIDGE) > 0) &&
              (getElevation() == curHex.terrainLevel(Terrains.BRIDGE_ELEV));
        if (!lastPos.equals(curPos) &&
              (bgMod != TargetRoll.AUTOMATIC_SUCCESS) &&
              (step.getMovementType(false) != EntityMovementType.MOVE_JUMP) &&
              (getMovementMode() != EntityMovementMode.HOVER) &&
              (getMovementMode() != EntityMovementMode.VTOL) &&
              (getMovementMode() != EntityMovementMode.WIGE) &&
              (step.getElevation() == 0) &&
              !isPavementStep &&
              !onBridge) {
            roll.append(new PilotingRollData(getId(), bgMod, "avoid bogging down"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                  "Check false: Not entering bog-down terrain, or jumping/hovering over such terrain");
        }
        return roll;
    }

    /**
     * @return True when this infantry unit can still call upon local support (CO 4th p.82, Urban Guerilla); in other
     *       words, if this ability has not been used yet. Note that this method does not check if it has the ability at
     *       all.
     */
    public boolean canCallSupport() {
        return canCallSupport;
    }

    /**
     * @return True if this infantry unit has any type of stealth system.
     */
    public abstract boolean isStealthy();

    /**
     * Returns the maximum number of extraneous limb pairs allowed. Per IO p.85, if glider wings or powered flight wings
     * are installed, only one pair of extraneous limbs is allowed (instead of the normal two pairs).
     *
     * @return 1 if any wing type is installed, 2 otherwise
     */
    public int getMaxExtraneousLimbPairs() {
        return (hasAbility(OptionsConstants.MD_PL_GLIDER) || hasAbility(OptionsConstants.MD_PL_FLIGHT)) ? 1 : 2;
    }

    @Override
    public boolean isEligibleFor(GamePhase phase) {
        if ((turnsLayingExplosives > 0) && !phase.isPhysical()) {
            return false;
        } else if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            return false;
        } else {
            return super.isEligibleFor(phase);
        }
    }

    @Override
    public void newRound(int roundNumber) {
        if (turnsLayingExplosives >= 0) {
            turnsLayingExplosives++;
            // Give up if no longer in the target structure's hex (building, bridge or fuel tank, TO:AUE p.152).
            // This deliberately matches the eligibility check for starting to lay charges so that a platoon that
            // could start (e.g. on a building roof or on a bridge) does not silently abandon its work next round.
            if (!isInDemolishableStructureHex()) {
                turnsLayingExplosives = -1;
            }
        }

        if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            // Damage taken during a digging-in / fortifying turn extends the effort by one turn (TO:AR p.106 /
            // TO:AUE p.153): hold the progress counter this round instead of advancing it.
            if (fortifyState().checkpointWasDamaged(currentFortifyHealthSignature())) {
                LOGGER.debug("[Fortify] {}: damaged while fortifying - effort extended by 1 turn (dug-in stage {})",
                      getShortName(), dugIn);
            } else {
                dugIn++;
                if (dugIn > DUG_IN_FORTIFYING3) {
                    dugIn = DUG_IN_NONE;
                }
            }
        }

        // Track idle turns spent on the deck. Moving clears the deck status (and the counter) on the server, so a
        // unit still on the deck here did not move last turn. Firing breaks the idle streak. TO:AR p.106.
        if (hitTheDeck) {
            if (firedWhileOnDeck) {
                turnsOnDeck = 0;
            } else {
                turnsOnDeck++;
            }
            firedWhileOnDeck = false;
        }

        setTakingCover(false);
        super.newRound(roundNumber);
    }

    public void setDugIn(int i) {
        dugIn = i;
    }

    public int getDugIn() {
        return dugIn;
    }

    /**
     * Begins the one-turn self "digging in" process (TO:AR p.106), or completes it immediately for a unit that has
     * stayed on the deck long enough to convert directly. Seeds the damage baseline used to detect an interrupting
     * attack that would extend the effort.
     *
     * @param convertFromDeck {@code true} if the unit qualifies to convert straight to fully dug-in from the deck (see
     *                        {@link #canDigInFromDeck()})
     */
    public void beginDigIn(boolean convertFromDeck) {
        if (convertFromDeck) {
            setHitTheDeck(false);
            setDugIn(DUG_IN_COMPLETE);
        } else {
            setDugIn(DUG_IN_WORKING);
        }
        fortifyState().begin(currentFortifyHealthSignature());
    }

    /**
     * Begins the three-turn fieldwork that raises a fortified hex (Trench/Fieldworks Engineers, TO:AUE p.153). Seeds
     * the damage baseline used to detect an interrupting attack that extends the effort.
     */
    public void beginFortify() {
        setDugIn(DUG_IN_FORTIFYING1);
        fortifyState().begin(currentFortifyHealthSignature());
    }

    /**
     * @return the health signature used to detect damage between fortifying turns: total armor plus internal structure.
     *       For infantry, internal structure is the trooper count, so any casualties lower this value and mark the turn
     *       as interrupted.
     */
    private int currentFortifyHealthSignature() {
        return getTotalArmor() + getTotalInternal();
    }

    /**
     * @return {@code true} if this unit's digging-in / fortification effort was set back by damage this round (so its
     *       progress counter was held rather than advanced). TO:AR p.106 / TO:AUE p.153.
     */
    public boolean isFortifyExtendedThisRound() {
        return fortifyState().wasExtendedAtLastCheckpoint();
    }

    /**
     * @return {@code true} if this unit is partway through building a fortified hex (one of the multi-turn FORTIFYING
     *       stages), as opposed to plain one-turn self digging-in. TO:AUE p.153.
     */
    public boolean isFortifying() {
        return (dugIn >= DUG_IN_FORTIFYING1) && (dugIn <= DUG_IN_FORTIFYING3);
    }

    /**
     * @return the current fortification stage (1..{@link #getFortifyTotalStages()}) while {@link #isFortifying()}, or 0
     *       when the unit is not building a fortification.
     */
    public int getFortifyStage() {
        return isFortifying() ? (dugIn - DUG_IN_FORTIFYING1 + 1) : 0;
    }

    /** @return the number of turns of work a fortified hex takes to complete. */
    public int getFortifyTotalStages() {
        return DUG_IN_FORTIFYING3 - DUG_IN_FORTIFYING1 + 1;
    }

    /**
     * @return {@code true} if this infantry is currently "hitting the deck", TO:AR p.106.
     */
    public boolean isHitTheDeck() {
        return hitTheDeck;
    }

    /**
     * Sets the "hitting the deck" status. Clearing the status (e.g. when the unit moves or is loaded) also resets the
     * idle-turn counter used for the dig-in conversion.
     *
     * @param hitTheDeck {@code true} to mark the unit as hitting the deck
     */
    public void setHitTheDeck(boolean hitTheDeck) {
        this.hitTheDeck = hitTheDeck;
        if (!hitTheDeck) {
            turnsOnDeck = 0;
            firedWhileOnDeck = false;
        }
    }

    /**
     * @return The number of consecutive idle turns this unit has spent on the deck (no movement, no fire).
     */
    public int getTurnsOnDeck() {
        return turnsOnDeck;
    }

    /**
     * Marks that this unit fired while on the deck this turn, which breaks the idle streak required for converting to
     * "dug in". TO:AR p.106.
     *
     * @param firedWhileOnDeck True if the unit made a weapon attack while on the deck
     */
    public void setFiredWhileOnDeck(boolean firedWhileOnDeck) {
        this.firedWhileOnDeck = firedWhileOnDeck;
    }

    /**
     * @return True if this on-deck unit has remained idle long enough that it may convert directly to "dug in"
     *       (protected) status, per TO:AR p.106.
     */
    public boolean canDigInFromDeck() {
        return hitTheDeck && (turnsOnDeck >= HIT_DECK_TURNS_TO_DIG_IN);
    }

    /**
     * Clears the transient ground defensive postures (dug in and hitting the deck). Called when the unit moves or is
     * loaded into a transport, since either action ends those postures. TO:AR p.106.
     */
    public void clearGroundPostures() {
        setDugIn(DUG_IN_NONE);
        setHitTheDeck(false);
        fortifyState().reset();
    }

    /**
     * This function is called when loading a unit into transport. This is overridden to ensure infantry are no longer
     * considered dug in or hitting the deck when they are being transported.
     *
     * @param transportID The entity ID of the transporter
     */
    @Override
    public void setTransportId(int transportID) {
        super.setTransportId(transportID);
        clearGroundPostures();
    }

    public boolean isMechanized() {
        return movementMode.isTrackedWheeledOrHover() || movementMode.isVTOL() || movementMode.isSubmarine();
    }

    //FIXME: This is currently unused, but MekHQ should probably reset this flag so the unit can call support again
    // in a later scenario
    public void setCanCallSupport(boolean b) {
        canCallSupport = b;
    }

    /**
     * Sets the squad size for this infantry unit. For Conventional Infantry, this is the size of a single squad and the
     * CI unit can be composed of more than one squad. For Battle Armor, this is the entire size of the unit (usually 4,
     * 5 or 6).
     *
     * @param size The new squad size
     */
    public void setSquadSize(int size) {
        squadSize = size;
    }

    /**
     * @return The squad size of this infantry unit. For Conventional Infantry, this is the size of a single squad and
     *       the CI unit can be composed of more than one squad. For Battle Armor, this is the entire size of the unit
     *       (usually 4, 5 or 6).
     */
    public int getSquadSize() {
        return squadSize;
    }

    public void setSquadCount(int n) {
        squadCount = n;
    }

    public int getSquadCount() {
        return squadCount;
    }

    public boolean isSquad() {
        return squadCount == 1;
    }

    /**
     * Returns the base (stored) movement mode for this infantry unit, ignoring any virtual VTOL mode from powered
     * flight wings. This should be used for validation and construction rules that need to know the actual infantry
     * type (leg, motorized, etc.) rather than the effective movement mode.
     *
     * @return The actual stored movement mode, not affected by powered flight
     */
    public EntityMovementMode getBaseMovementMode() {
        return movementMode;
    }

    /**
     * @return True for all infantry that are allowed AM attacks. Mechanized infantry and infantry units with
     *       encumbering armor or field guns are not allowed to make AM attacks, while all other infantry are. Note that
     *       a conventional infantry unit without Anti-Mek gear (15 kg per trooper) can still make AM attacks but has a
     *       fixed 8 AM skill rating.
     */
    public abstract boolean canMakeAntiMekAttacks();

    @Override
    public boolean isUsingManAce() {
        // Infantry don't use MP to change facing, and don't do PSRs, so just don't let them use maneuvering ace
        // otherwise, their movement gets screwed up
        return false;
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    @Override
    public boolean hasEngine() {
        return false;
    }

    @Override
    public PilotingRollData checkLandingInHeavyWoods(EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE, "Infantry cannot fall");
        return roll;
    }

    /**
     * Determines if there is valid cover for an infantry unit to utilize the Using Non-Infantry as Cover rules (TO:AR
     * p.106).
     *
     * @param game      The current {@link Game}
     * @param pos       The hex coords
     * @param elevation The elevation (flying or in building)
     *
     * @return True when this infantry has valid cover
     */
    public static boolean hasValidCover(Game game, Coords pos, int elevation) {
        // Can't do anything if we don't have a position
        // If elevation > 0, we're either flying, or in a building
        // In either case, we shouldn't be allowed to take cover
        if ((pos == null) || (elevation > 0)) {
            return false;
        }
        boolean hasMovedEntity = false;
        // First, look for ground units in the same hex that have already moved
        for (Entity e : game.getEntitiesVector(pos)) {
            if (e.isDone() && !(e instanceof Infantry) && (e.getElevation() == elevation)) {
                hasMovedEntity = true;
                break;
            }
        }
        // If we didn't find anything, check for wrecks
        // The rules don't explicitly cover this, but it makes sense
        if (!hasMovedEntity) {
            Enumeration<Entity> wrecks = game.getWreckedEntities();
            while (wrecks.hasMoreElements()) {
                Entity e = wrecks.nextElement();
                if (pos.equals(e.getPosition()) && !(e instanceof Infantry)) {
                    hasMovedEntity = true;
                }
            }
        }
        return hasMovedEntity;
    }

    public boolean isTakingCover() {
        return isTakingCover;
    }

    public void setTakingCover(boolean isTakingCover) {
        this.isTakingCover = isTakingCover;
    }

    @Override
    protected boolean hasViableWeapons() {
        return !isCrippled();
    }

    @Override
    public boolean hasPatchworkArmor() {
        return false;
    }

    @Override
    public PilotingRollData checkSkid(EntityMovementType moveType, Hex prevHex, EntityMovementType overallMoveType,
          MoveStep prevStep, MoveStep currStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos,
          boolean isInfantry, int distance) {
        return new PilotingRollData(id, TargetRoll.CHECK_FALSE, "Infantry can't skid");
    }

    @Override
    public int getRecoveryTime() {
        // Conventional infantry units have no listed recovery time in CamOps, so we're copying from Battle Armor
        return 10;
    }

    @Override
    public boolean canInitiateInfantryVsInfantryCombat() {
        if (!game.hasBoardLocationOf(this)) {
            return false; // not on board?
        }

        // Must be inside the building to initiate combat (TO:AR p. 169)
        if (!isInBuilding()) {
            return false;
        }

        Hex hex = game.getHex(getPosition(), getBoardId());
        if (hex == null) {
            return false;
        }

        // Look for enemy boardable entities at this location
        // TODO: Once buildings can be captured & change owner, allow this for building carcass
        Entity enemyBoardableEntity = null;
        for (Entity e : game.getEntitiesVector(getBoardLocation())) {
            if (e.isBoardable()
                  && (e.getOwner().isEnemyOf(getOwner())
                  && (e.getCrew() != null && !e.getCrew().isDead()))) {
                enemyBoardableEntity = e;
                break;
            }
        }

        if (enemyBoardableEntity != null) {
            // Check if combat DOES NOT already exist (this is for INITIATING new combat only)
            boolean combatExists = getGame().getEntitiesVector(getBoardLocation()).stream()
                  .anyMatch(e -> e.getInfantryCombatTargetId() != Entity.NONE);

            return !combatExists; // Can only initiate if no combat exists yet
        }

        return false;
    }

    @Override
    public boolean canReinforceInfantryVsInfantry() {
        if (!game.hasBoardLocationOf(this)) {
            return false; // not on board?
        }

        Hex hex = game.getHex(getPosition(), getBoardId());
        if (hex == null) {
            return false;
        }

        // Check if already in combat
        if (getInfantryCombatTargetId() != Entity.NONE) {
            return false;  // Can't reinforce if already in combat
        }

        // Check if can reinforce EXISTING infantry vs. infantry combat
        // Simply check if there's ongoing combat in this hex
        boolean combatExists = getGame().getEntitiesVector(getBoardLocation()).stream()
              .anyMatch(e -> e.getInfantryCombatTargetId() != Entity.NONE);

        if (combatExists && getInfantryCombatTargetId() == Entity.NONE) {
            // Combat exists and we're not in it yet - can reinforce
            return getGame().getEntitiesVector(getBoardLocation()).stream()
                  .anyMatch(e -> e.getInfantryCombatTargetId() != Entity.NONE);
        }

        return false;
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns true if this infantry unit can exit a VTOL using glider wings. Per IO:AE p.79, glider wings give a
     * soldier the ability to leave a VTOL during movement as if the soldier were jump infantry.
     *
     * @return true if this infantry can exit a VTOL using glider wings
     */
    public boolean canExitVTOLWithGliderWings() {
        return false;
    }
}
