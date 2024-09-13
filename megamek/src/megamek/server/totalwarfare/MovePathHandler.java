/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalwarfare;

import java.util.*;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.common.*;
import megamek.common.actions.AirMekRamAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.ClearMinefieldAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.RamAttackAction;
import megamek.common.actions.UnjamAction;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.logging.MMLogger;
import megamek.server.ServerHelper;
import megamek.server.SmokeCloud;

/**
 * Processes an Entity's MovePath when an ENTITY_MOVE packet is received.
 */
class MovePathHandler extends AbstractTWRuleHandler {
    private static final MMLogger logger = MMLogger.create(MovePathHandler.class);

    private final Entity entity;
    private final MovePath md;
    private final Map<UnitTargetPair, LosEffects> losCache;

    private boolean sideslipped = false;
    private Coords lastPos;
    private Coords curPos;
    private Hex firstHex; // Used to check for start/end magma damage
    private int curFacing;
    private int curVTOLElevation;
    private int curElevation;
    private int lastElevation;
    private int curAltitude;
    private boolean curClimbMode;
    // if the entity already used some MPs,
    // it previously tried to get up and fell,
    // and then got another turn. set moveType
    // and overallMoveType accordingly
    // (these are all cleared by Entity.newRound)
    private int distance;
    private int mpUsed;
    private EntityMovementType moveType;
    private EntityMovementType overallMoveType;
    private EntityMovementType lastStepMoveType;
    private boolean firstStep;
    private MoveStep prevStep = null;
    private boolean wasProne;
    private boolean fellDuringMovement;
    private boolean crashedDuringMovement;
    private boolean dropshipStillUnloading;
    private boolean detectedHiddenHazard;
    private boolean turnOver;
    private int prevFacing;
    private Hex prevHex;
    private boolean isInfantry;
    private AttackAction charge;
    private RamAttackAction ram;
    // cache this here, otherwise changing MP in the turn causes
    // erroneous gravity PSRs
    private int cachedGravityLimit = -1;
    private int thrustUsed = 0;
    private int j = 0;
    private boolean didMove;
    private boolean recovered = false;
    private Entity loader = null;
    private boolean continueTurnFromPBS = false;
    private boolean continueTurnFromFishtail = false;
    private boolean continueTurnFromLevelDrop = false;
    private boolean continueTurnFromCliffAscent = false;

    // get a list of coordinates that the unit passed through this turn
    // so that I can later recover potential bombing targets
    // it may already have some values
    private final Vector<Coords> passedThrough = new Vector<>();
    private final List<Integer> passedThroughFacing = new ArrayList<>();
    private final List<Entity> hiddenEnemies = new ArrayList<>();
    private final Vector<UnitLocation> movePath = new Vector<>();

    private Report r;
    private PilotingRollData rollTarget;

    /**
     * Steps through an entity movement packet, executing it.
     *
     * @param gameManager The server's GameManager
     * @param entity      The Entity that is moving
     * @param md          The MovePath that defines how the Entity moves
     * @param losCache    A cache that stores Los between various Entities and
     *                    targets. In double blind games, we may need to compute a
     *                    lot of LosEffects, so caching them can really speed
     *                    things up.
     */
    MovePathHandler(TWGameManager gameManager, Entity entity, MovePath md, Map<UnitTargetPair, LosEffects> losCache) {
        super(gameManager);
        this.entity = entity;
        this.md = md;
        this.losCache = (losCache == null) ? new HashMap<>() : losCache;
    }

    void processMovement() {
        // check for fleeing
        if (md.contains(MovePath.MoveStepType.FLEE)) {
            addReport(gameManager.processLeaveMap(md, false, -1));
            return;
        }

        if (md.contains(MovePath.MoveStepType.EJECT)) {
            if (entity.isLargeCraft() && !entity.isCarcass()) {
                r = new Report(2026);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
                Aero ship = (Aero) entity;
                ship.setEjecting(true);
                gameManager.entityUpdate(ship.getId());
                Coords legalPos = entity.getPosition();
                // Get the step so we can pass it in and get the abandon coords from it
                for (final Enumeration<MoveStep> i = md.getSteps(); i
                        .hasMoreElements();) {
                    final MoveStep step = i.nextElement();
                    if (step.getType() == MovePath.MoveStepType.EJECT) {
                        legalPos = step.getTargetPosition();
                    }
                }
                addReport(gameManager.ejectSpacecraft(ship, ship.isSpaceborne(),
                        (ship.isAirborne() && !ship.isSpaceborne()), legalPos));
                // If we're grounded or destroyed by crew loss, end movement
                if (entity.isDoomed() || (!entity.isSpaceborne() && !entity.isAirborne())) {
                    return;
                }
            } else if ((entity instanceof Mek) || (entity instanceof Aero)) {
                r = new Report(2020);
                r.subject = entity.getId();
                r.add(entity.getCrew().getName());
                r.addDesc(entity);
                addReport(r);
                addReport(gameManager.ejectEntity(entity, false));
                return;
            } else if ((entity instanceof Tank) && !entity.isCarcass()) {
                r = new Report(2025);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);
                addReport(gameManager.ejectEntity(entity, false));
                return;
            }
        }

        if (md.contains(MovePath.MoveStepType.CAREFUL_STAND)) {
            entity.setCarefulStand(true);
        }
        if (md.contains(MovePath.MoveStepType.BACKWARDS)) {
            entity.setMovedBackwards(true);
            if (md.getMpUsed() > entity.getWalkMP()) {
                entity.setPowerReverse(true);
            }
        }

        if (md.contains(MovePath.MoveStepType.TAKEOFF) && entity.isAero()) {
            IAero a = (IAero) entity;
            a.setCurrentVelocity(1);
            a.liftOff(1);
            if (entity instanceof Dropship) {
                gameManager.applyDropShipProximityDamage(md.getFinalCoords(), true, md.getFinalFacing(), entity);
            }
            gameManager.checkForTakeoffDamage(a);
            entity.setPosition(entity.getPosition().translated(entity.getFacing(), a.getTakeOffLength()));
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        if (md.contains(MovePath.MoveStepType.VTAKEOFF) && entity.isAero()) {
            IAero a = (IAero) entity;
            rollTarget = a.checkVerticalTakeOff();
            if (gameManager.doVerticalTakeOffCheck(entity, rollTarget)) {
                a.setCurrentVelocity(0);
                a.liftOff(1);
                if (entity instanceof Dropship) {
                    gameManager.applyDropShipProximityDamage(md.getFinalCoords(), (Dropship) a);
                }
                gameManager.checkForTakeoffDamage(a);
            }
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        if (md.contains(MovePath.MoveStepType.LAND) && entity.isAero()) {
            IAero a = (IAero) entity;
            rollTarget = a.checkLanding(md.getLastStepMovementType(), md.getFinalVelocity(),
                    md.getFinalCoords(), md.getFinalFacing(), false);
            gameManager.attemptLanding(entity, rollTarget);
            gameManager.checkLandingTerrainEffects(a, true, md.getFinalCoords(),
                    md.getFinalCoords().translated(md.getFinalFacing(), a.getLandingLength()), md.getFinalFacing());
            a.land();
            entity.setPosition(md.getFinalCoords().translated(md.getFinalFacing(),
                    a.getLandingLength()));
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        if (md.contains(MovePath.MoveStepType.VLAND) && entity.isAero()) {
            IAero a = (IAero) entity;
            rollTarget = a.checkLanding(md.getLastStepMovementType(),
                    md.getFinalVelocity(), md.getFinalCoords(),
                    md.getFinalFacing(), true);
            gameManager.attemptLanding(entity, rollTarget);
            if (entity instanceof Dropship) {
                gameManager.applyDropShipLandingDamage(md.getFinalCoords(), (Dropship) a);
            }
            gameManager.checkLandingTerrainEffects(a, true, md.getFinalCoords(), md.getFinalCoords(),
                    md.getFinalFacing());
            a.land();
            entity.setPosition(md.getFinalCoords());
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        // okay, proceed with movement calculations
        lastPos = entity.getPosition();
        curPos = entity.getPosition();
        firstHex = getGame().getBoard().getHex(curPos); // Used to check for start/end magma damage
        curFacing = entity.getFacing();
        curVTOLElevation = entity.getElevation();
        lastElevation = entity.getElevation();
        curAltitude = entity.getAltitude();
        curClimbMode = entity.climbMode();
        // if the entity already used some MPs,
        // it previously tried to get up and fell,
        // and then got another turn. set moveType
        // and overallMoveType accordingly
        // (these are all cleared by Entity.newRound)
        distance = entity.delta_distance;
        mpUsed = entity.mpUsed;
        moveType = entity.moved;
        wasProne = entity.isProne();
        fellDuringMovement = false;
        crashedDuringMovement = false;
        dropshipStillUnloading = false;
        detectedHiddenHazard = false;
        prevFacing = curFacing;
        prevHex = getGame().getBoard().getHex(curPos);
        isInfantry = entity instanceof Infantry;
        // cache this here, otherwise changing MP in the turn causes
        // erroneous gravity PSRs

        // get a list of coordinates that the unit passed through this turn
        // so that I can later recover potential bombing targets
        // it may already have some values
        passedThrough.addAll(entity.getPassedThrough());
        passedThrough.add(curPos);
        passedThroughFacing.addAll(entity.getPassedThroughFacing());
        passedThroughFacing.add(curFacing);

        // Compile the move - don't clip
        // Clipping could affect hidden units; illegal steps aren't processed
        md.compile(getGame(), entity, false);

        // if advanced movement is being used then set the new vectors based on
        // move path
        entity.setVectors(md.getFinalVectors());

        overallMoveType = md.getLastStepMovementType();

        // check for starting in liquid magma
        if ((getGame().getBoard().getHex(entity.getPosition())
                .terrainLevel(Terrains.MAGMA) == 2)
                && (entity.getElevation() == 0)) {
            gameManager.doMagmaDamage(entity, false);
        }

        // set acceleration used to default
        if (entity.isAero()) {
            ((IAero) entity).setAccLast(false);
        }

        // check for dropping troops and drop them
        if (entity.isDropping() && !md.contains(MovePath.MoveStepType.HOVER)) {
            entity.setAltitude(entity.getAltitude() - getGame().getPlanetaryConditions().getDropRate());
            // they may have changed their facing
            if (md.length() > 0) {
                entity.setFacing(md.getFinalFacing());
            }
            passedThrough.add(entity.getPosition());
            entity.setPassedThrough(passedThrough);
            passedThroughFacing.add(entity.getFacing());
            entity.setPassedThroughFacing(passedThroughFacing);
            // We may still need to process any conversions for dropping LAMs
            if (entity instanceof LandAirMek && md.contains(MovePath.MoveStepType.CONVERT_MODE)) {
                entity.setMovementMode(md.getFinalConversionMode());
                entity.setConvertingNow(true);
                r = new Report(1210);
                r.subject = entity.getId();
                r.addDesc(entity);
                if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                    r.messageId = 2452;
                } else if (entity.getMovementMode() == EntityMovementMode.AERODYNE) {
                    r.messageId = 2453;
                } else {
                    r.messageId = 2450;
                }
                addReport(r);
            }
            entity.setDone(true);
            gameManager.entityUpdate(entity.getId());
            return;
        }

        // iterate through steps
        firstStep = true;
        turnOver = false;

        if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
            for (Entity e : getGame().getEntitiesVector()) {
                if (e.isHidden() && e.isEnemyOf(entity) && (e.getPosition() != null)) {
                    hiddenEnemies.add(e);
                }
            }
        }

        lastStepMoveType = md.getLastStepMovementType();

        processSteps();

        // set entity parameters
        entity.setPosition(curPos);
        entity.setFacing(curFacing);
        entity.setSecondaryFacing(curFacing);
        entity.delta_distance = distance;
        entity.moved = moveType;
        entity.mpUsed = mpUsed;
        if (md.isAllUnderwater(getGame())) {
            entity.underwaterRounds++;
            if ((entity instanceof Infantry) && (((Infantry) entity).getMount() != null)
                    && entity.getMovementMode().isSubmarine()
                    && entity.underwaterRounds > ((Infantry) entity).getMount().getUWEndurance()) {
                r = new Report(2412);
                r.addDesc(entity);
                addReport(r);
                gameManager.destroyEntity(entity, "mount drowned");
            }
        } else {
            entity.underwaterRounds = 0;
        }
        entity.setClimbMode(curClimbMode);
        if (!sideslipped && !fellDuringMovement && !crashedDuringMovement
                && (entity.getMovementMode() == EntityMovementMode.VTOL)) {
            entity.setElevation(curVTOLElevation);
        }
        entity.setAltitude(curAltitude);
        entity.setClimbMode(curClimbMode);

        // add a list of places passed through
        entity.setPassedThrough(passedThrough);
        entity.setPassedThroughFacing(passedThroughFacing);

        // if we ran with destroyed hip or gyro, we need a psr
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE && entity.canFall()) {
            gameManager.doSkillCheckInPlace(entity, rollTarget);
        }

        // if we sprinted with MASC or a supercharger, then we need a PSR
        rollTarget = entity.checkSprintingWithMASCXorSupercharger(overallMoveType,
                entity.mpUsed);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE && entity.canFall()) {
            gameManager.doSkillCheckInPlace(entity, rollTarget);
        }

        // if we used ProtoMek myomer booster, roll 2d6
        // pilot damage on a 2
        if ((entity instanceof ProtoMek) && ((ProtoMek) entity).hasMyomerBooster()
                && (md.getMpUsed() > entity.getRunMP(MPCalculationSetting.NO_MYOMER_BOOSTER))) {
            r = new Report(2373);
            r.addDesc(entity);
            r.subject = entity.getId();
            Roll diceRoll = Compute.rollD6(2);
            r.add(diceRoll);

            if (diceRoll.getIntValue() > 2) {
                r.choose(true);
                addReport(r);
            } else {
                r.choose(false);
                addReport(r);
                addReport(gameManager.damageCrew(entity, 1));
            }
        }

        rollTarget = entity.checkSprintingWithMASCAndSupercharger(overallMoveType, entity.mpUsed);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            gameManager.doSkillCheckInPlace(entity, rollTarget);
        }
        if ((md.getLastStepMovementType() == EntityMovementType.MOVE_SPRINT)
                && (md.hasActiveMASC() || md.hasActiveSupercharger()) && entity.canFall()) {
            gameManager.doSkillCheckInPlace(entity, entity.getBasePilotingRoll(EntityMovementType.MOVE_SPRINT));
        }

        if (entity.isAirborne() && entity.isAero()) {

            IAero a = (IAero) entity;
            int thrust = md.getMpUsed();

            // consume fuel
            if (((entity.isAero())
                    && getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION))
                    || (entity instanceof TeleMissile)) {
                int fuelUsed = ((IAero) entity).getFuelUsed(thrust);

                // if we're a gas hog, aerospace fighter and going faster than walking, then use
                // 2x fuel
                if (((overallMoveType == EntityMovementType.MOVE_RUN) ||
                        (overallMoveType == EntityMovementType.MOVE_SPRINT) ||
                        (overallMoveType == EntityMovementType.MOVE_OVER_THRUST)) &&
                        entity.hasQuirk(OptionsConstants.QUIRK_NEG_GAS_HOG)) {
                    fuelUsed *= 2;
                }

                a.useFuel(fuelUsed);
            }

            // JumpShips and space stations need to reduce accumulated thrust if
            // they spend some
            if (entity instanceof Jumpship) {
                Jumpship js = (Jumpship) entity;
                double penalty = 0.0;
                // JumpShips do not accumulate thrust when they make a turn or
                // change velocity
                if (md.contains(MovePath.MoveStepType.TURN_LEFT) || md.contains(MovePath.MoveStepType.TURN_RIGHT)) {
                    // I need to subtract the station keeping thrust from their
                    // accumulated thrust
                    // because they did not actually use it
                    penalty = js.getStationKeepingThrust();
                }
                if (thrust > 0) {
                    penalty = thrust;
                }
                if (penalty > 0.0) {
                    js.setAccumulatedThrust(Math.max(0, js.getAccumulatedThrust() - penalty));
                }
            }

            // check to see if thrust exceeded SI

            rollTarget = a.checkThrustSITotal(thrust, overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                getGame().addControlRoll(new PilotingRollData(entity.getId(), 0,
                        "Thrust spent during turn exceeds SI"));
            }

            if (!getGame().getBoard().inSpace()) {
                rollTarget = a.checkVelocityDouble(md.getFinalVelocity(),
                        overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(new PilotingRollData(entity.getId(), 0,
                            "Velocity greater than 2x safe thrust"));
                }

                rollTarget = a.checkDown(md.getFinalNDown(), overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(
                            new PilotingRollData(entity.getId(), md.getFinalNDown(),
                                    "descended more than two altitudes"));
                }

                // check for hovering
                rollTarget = a.checkHover(md);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(
                            new PilotingRollData(entity.getId(), 0, "hovering"));
                }

                // check for aero stall
                rollTarget = a.checkStall(md);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    r = new Report(9391);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    addReport(r);
                    getGame().addControlRoll(new PilotingRollData(entity.getId(), 0,
                            "stalled out"));
                    entity.setAltitude(entity.getAltitude() - 1);
                    // check for crash
                    if (gameManager.checkCrash(entity, entity.getPosition(), entity.getAltitude())) {
                        addReport(gameManager.processCrash(entity, 0, entity.getPosition()));
                    }
                }

                // check to see if spheroids should lose one altitude
                if (a.isSpheroid() && !a.isSpaceborne()
                        && a.isAirborne() && (md.getFinalNDown() == 0) && (md.getMpUsed() == 0)) {
                    r = new Report(9392);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    addReport(r);
                    entity.setAltitude(entity.getAltitude() - 1);
                    // check for crash
                    if (gameManager.checkCrash(entity, entity.getPosition(), entity.getAltitude())) {
                        addReport(gameManager.processCrash(entity, 0, entity.getPosition()));
                    }
                } else if (entity instanceof EscapePods && entity.isAirborne() && md.getFinalVelocity() < 2) {
                    // Atmospheric Escape Pods that drop below velocity 2 lose altitude as dropping
                    // units
                    entity.setAltitude(entity.getAltitude()
                            - getGame().getPlanetaryConditions().getDropRate());
                    r = new Report(6676);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(getGame().getPlanetaryConditions().getDropRate());
                    addReport(r);
                }
            }
        }

        // We need to check for the removal of hull-down for tanks.
        // Tanks can just drive out of hull-down: if the tank was hull-down
        // and doesn't end hull-down we can remove the hull-down status
        if (entity.isHullDown() && !md.getFinalHullDown()
                && (entity instanceof Tank
                        || (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))) {
            entity.setHullDown(false);
        }

        // If the entity is being swarmed, erratic movement may dislodge the
        // fleas.
        final int swarmerId = entity.getSwarmAttackerId();
        if ((Entity.NONE != swarmerId) && md.contains(MovePath.MoveStepType.SHAKE_OFF_SWARMERS)) {
            final Entity swarmer = getGame().getEntity(swarmerId);
            rollTarget = entity.getBasePilotingRoll(overallMoveType);

            entity.addPilotingModifierForTerrain(rollTarget);

            // Add a +4 modifier.
            if (md.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_RUN) {
                rollTarget.addModifier(2,
                        "dislodge swarming infantry with VTOL movement");
            } else {
                rollTarget.addModifier(4, "dislodge swarming infantry");
            }

            // If the swarmer has Assault claws, give a 1 modifier.
            // We can stop looking when we find our first match.
            for (Mounted<?> mount : swarmer.getMisc()) {
                EquipmentType equip = mount.getType();
                if (equip.hasFlag(MiscType.F_MAGNET_CLAW)) {
                    rollTarget.addModifier(1, "swarmer has magnetic claws");
                    break;
                }
            }

            // okay, print the info
            r = new Report(2125);
            r.subject = entity.getId();
            r.addDesc(entity);
            addReport(r);

            // roll
            final Roll diceRoll = Compute.rollD6(2);
            r = new Report(2130);
            r.subject = entity.getId();
            r.add(rollTarget.getValueAsString());
            r.add(rollTarget.getDesc());
            r.add(diceRoll);

            if (diceRoll.getIntValue() < rollTarget.getValue()) {
                r.choose(false);
                addReport(r);
            } else {
                // Dislodged swarmers don't get turns.
                getGame().removeTurnFor(swarmer);
                gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

                // Update the report and the swarmer's status.
                r.choose(true);
                addReport(r);
                entity.setSwarmAttackerId(Entity.NONE);
                swarmer.setSwarmTargetId(Entity.NONE);

                Hex curHex = getGame().getBoard().getHex(curPos);

                // Did the infantry fall into water?
                if (curHex.terrainLevel(Terrains.WATER) > 0) {
                    // Swarming infantry die.
                    swarmer.setPosition(curPos);
                    r = new Report(2135);
                    r.subject = entity.getId();
                    r.indent();
                    r.addDesc(swarmer);
                    addReport(r);
                    addReport(gameManager.destroyEntity(swarmer, "a watery grave", false));
                } else {
                    // Swarming infantry take a 3d6 point hit.
                    // ASSUMPTION : damage should not be doubled.
                    r = new Report(2140);
                    r.subject = entity.getId();
                    r.indent();
                    r.addDesc(swarmer);
                    r.add("3d6");
                    addReport(r);
                    addReport(gameManager.damageEntity(swarmer,
                            swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT),
                            Compute.d6(3)));
                    addNewLines();
                    swarmer.setPosition(curPos);
                }
                gameManager.entityUpdate(swarmerId);
            } // End successful-PSR

        } // End try-to-dislodge-swarmers

        // but the danger isn't over yet! landing from a jump can be risky!
        if ((overallMoveType == EntityMovementType.MOVE_JUMP) && !entity.isMakingDfa()) {
            final Hex curHex = getGame().getBoard().getHex(curPos);

            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage(overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                gameManager.doSkillCheckInPlace(entity, rollTarget);
            }

            // check for prototype JJs
            rollTarget = entity.checkLandingWithPrototypeJJ(overallMoveType);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                gameManager.doSkillCheckInPlace(entity, rollTarget);
            }

            // check for jumping into heavy woods
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_PSR_JUMP_HEAVY_WOODS)) {
                rollTarget = entity.checkLandingInHeavyWoods(overallMoveType, curHex);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    gameManager.doSkillCheckInPlace(entity, rollTarget);
                }
            }

            // Mechanical jump boosters fall damage
            if (md.shouldMechanicalJumpCauseFallDamage()) {
                gameManager.getvPhaseReport().addAll(gameManager.doEntityFallsInto(entity,
                        entity.getElevation(), md.getJumpPathHighestPoint(),
                        curPos, entity.getBasePilotingRoll(overallMoveType),
                        false, entity.getJumpMP()));
            }

            // jumped into water?
            int waterLevel = curHex.terrainLevel(Terrains.WATER);
            if (curHex.containsTerrain(Terrains.ICE) && (waterLevel > 0)) {
                if (!(entity instanceof Infantry)) {
                    // check for breaking ice
                    Roll diceRoll = Compute.rollD6(1);
                    r = new Report(2122);
                    r.add(entity.getDisplayName(), true);
                    r.add(diceRoll);
                    r.subject = entity.getId();
                    addReport(r);

                    if (diceRoll.getIntValue() >= 4) {
                        // oops!
                        entity.setPosition(curPos);
                        addReport(gameManager.resolveIceBroken(curPos));
                        curPos = entity.getPosition();
                    } else {
                        // TacOps: immediate PSR with +4 for terrain. If you
                        // fall then may break the ice after all
                        rollTarget = entity.checkLandingOnIce(overallMoveType, curHex);
                        if (!gameManager.doSkillCheckInPlace(entity, rollTarget)) {
                            // apply damage now, or it will show up as a
                            // possible breach, if ice is broken
                            entity.applyDamage();
                            Roll diceRoll2 = Compute.rollD6(1);
                            r = new Report(2118);
                            r.addDesc(entity);
                            r.add(diceRoll2);
                            r.subject = entity.getId();
                            addReport(r);

                            if (diceRoll2.getIntValue() == 6) {
                                entity.setPosition(curPos);
                                addReport(gameManager.resolveIceBroken(curPos));
                                curPos = entity.getPosition();
                            }
                        }
                    }
                }
            } else if (!(prevStep.climbMode() && curHex.containsTerrain(Terrains.BRIDGE))
                    && !(entity.getMovementMode().isHoverOrWiGE())) {
                rollTarget = entity.checkWaterMove(waterLevel, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    // For falling elevation, Entity must not on hex surface
                    int currElevation = entity.getElevation();
                    entity.setElevation(0);
                    boolean success = gameManager.doSkillCheckInPlace(entity, rollTarget);
                    if (success) {
                        entity.setElevation(currElevation);
                    }
                }
                if (waterLevel > 1) {
                    // Any swarming infantry will be destroyed.
                    gameManager.drownSwarmer(entity, curPos);
                }
            }

            // check for black ice
            boolean useBlackIce = getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE);
            boolean goodTemp = getGame().getPlanetaryConditions()
                    .getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP;
            boolean goodWeather = getGame().getPlanetaryConditions().getWeather().isIceStorm();
            if ((useBlackIce && goodTemp) || goodWeather) {
                if (ServerHelper.checkEnteringBlackIce(gameManager, curPos, curHex, useBlackIce, goodTemp,
                        goodWeather)) {
                    rollTarget = entity.checkLandingOnBlackIce(overallMoveType, curHex);
                    if (!gameManager.doSkillCheckInPlace(entity, rollTarget)) {
                        entity.applyDamage();
                    }
                }
            }

            // check for building collapse
            Building bldg = getGame().getBoard().getBuildingAt(curPos);
            if (bldg != null) {
                gameManager.checkForCollapse(bldg, getGame().getPositionMap(), curPos, true,
                        gameManager.getvPhaseReport());
            }

            // Don't interact with terrain when jumping onto a building or a bridge
            if (entity.getElevation() == 0) {
                ServerHelper.checkAndApplyMagmaCrust(curHex, entity.getElevation(), entity, curPos, true,
                        gameManager.getvPhaseReport(), gameManager);
                ServerHelper.checkEnteringMagma(curHex, entity.getElevation(), entity, gameManager);

                // jumped into swamp? maybe stuck!
                if (curHex.getBogDownModifier(entity.getMovementMode(),
                        entity instanceof LargeSupportTank) != TargetRoll.AUTOMATIC_SUCCESS) {
                    if (entity instanceof Mek) {
                        entity.setStuck(true);
                        r = new Report(2121);
                        r.add(entity.getDisplayName(), true);
                        r.subject = entity.getId();
                        addReport(r);
                        // check for quicksand
                        addReport(gameManager.checkQuickSand(curPos));
                    } else if (!entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                        rollTarget = new PilotingRollData(entity.getId(),
                                5, "entering boggy terrain");
                        rollTarget.append(new PilotingRollData(entity.getId(),
                                curHex.getBogDownModifier(entity.getMovementMode(),
                                        entity instanceof LargeSupportTank),
                                "avoid bogging down"));
                        if (0 < gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(), curPos, curPos,
                                rollTarget, false)) {
                            entity.setStuck(true);
                            r = new Report(2081);
                            r.add(entity.getDisplayName());
                            r.subject = entity.getId();
                            addReport(r);
                            // check for quicksand
                            addReport(gameManager.checkQuickSand(curPos));
                        }
                    }
                }
            }

            // If the entity is being swarmed, jumping may dislodge the fleas.
            if (Entity.NONE != swarmerId) {
                final Entity swarmer = getGame().getEntity(swarmerId);
                rollTarget = entity.getBasePilotingRoll(overallMoveType);

                entity.addPilotingModifierForTerrain(rollTarget);

                // Add a +4 modifier.
                rollTarget.addModifier(4, "dislodge swarming infantry");

                // If the swarmer has Assault claws, give a 1 modifier.
                // We can stop looking when we find our first match.
                if (swarmer.hasWorkingMisc(MiscType.F_MAGNET_CLAW, -1)) {
                    rollTarget.addModifier(1, "swarmer has magnetic claws");
                }

                // okay, print the info
                r = new Report(2125);
                r.subject = entity.getId();
                r.addDesc(entity);
                addReport(r);

                // roll
                final Roll diceRoll = Compute.rollD6(2);
                r = new Report(2130);
                r.subject = entity.getId();
                r.add(rollTarget.getValueAsString());
                r.add(rollTarget.getDesc());
                r.add(diceRoll);

                if (diceRoll.getIntValue() < rollTarget.getValue()) {
                    r.choose(false);
                    addReport(r);
                } else {
                    // Dislodged swarmers don't get turns.
                    getGame().removeTurnFor(swarmer);
                    gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

                    // Update the report and the swarmer's status.
                    r.choose(true);
                    addReport(r);
                    entity.setSwarmAttackerId(Entity.NONE);
                    swarmer.setSwarmTargetId(Entity.NONE);

                    // Did the infantry fall into water?
                    if (curHex.terrainLevel(Terrains.WATER) > 0) {
                        // Swarming infantry die.
                        swarmer.setPosition(curPos);
                        r = new Report(2135);
                        r.subject = entity.getId();
                        r.indent();
                        r.addDesc(swarmer);
                        addReport(r);
                        addReport(gameManager.destroyEntity(swarmer, "a watery grave", false));
                    } else {
                        // Swarming infantry take a 3d6 point hit.
                        // ASSUMPTION : damage should not be doubled.
                        r = new Report(2140);
                        r.subject = entity.getId();
                        r.indent();
                        r.addDesc(swarmer);
                        r.add("3d6");
                        addReport(r);
                        addReport(gameManager.damageEntity(swarmer,
                                swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT),
                                Compute.d6(3)));
                        addNewLines();
                        swarmer.setPosition(curPos);
                    }
                    gameManager.entityUpdate(swarmerId);
                } // End successful-PSR

            } // End try-to-dislodge-swarmers

            // one more check for inferno wash-off
            gameManager.checkForWashedInfernos(entity, curPos);

            // a jumping tank needs to roll for movement damage
            if (entity instanceof Tank) {
                int modifier = 0;
                if (curHex.containsTerrain(Terrains.ROUGH)
                        || curHex.containsTerrain(Terrains.WOODS)
                        || curHex.containsTerrain(Terrains.JUNGLE)) {
                    modifier = 1;
                }
                r = new Report(2126);
                r.subject = entity.getId();
                r.addDesc(entity);
                gameManager.getvPhaseReport().add(r);
                gameManager.getvPhaseReport().addAll(gameManager.vehicleMotiveDamage((Tank) entity, modifier,
                        false, -1, true));
                Report.addNewline(gameManager.getvPhaseReport());
            }

        } // End entity-is-jumping

        // If converting to another mode, set the final movement mode and report it
        if (entity.isConvertingNow()) {
            r = new Report(1210);
            r.subject = entity.getId();
            r.addDesc(entity);
            if (entity instanceof QuadVee && entity.isProne()
                    && entity.getConversionMode() == QuadVee.CONV_MODE_MEK) {
                // Fall while converting to vehicle mode cancels conversion.
                entity.setConvertingNow(false);
                r.messageId = 2454;
            } else {
                // LAMs converting from fighter mode need to have the elevation set properly.
                if (entity.isAero()) {
                    if (md.getFinalConversionMode() == EntityMovementMode.WIGE
                            && entity.getAltitude() > 0 && entity.getAltitude() <= 3) {
                        entity.setElevation(entity.getAltitude() * 10);
                        entity.setAltitude(0);
                    } else {
                        Hex hex = getGame().getBoard().getHex(entity.getPosition());
                        if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                            entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
                        } else {
                            entity.setElevation(0);
                        }
                    }
                }
                entity.setMovementMode(md.getFinalConversionMode());
                if (entity instanceof Mek && ((Mek) entity).hasTracks()) {
                    r.messageId = 2455;
                    r.choose(entity.getMovementMode() == EntityMovementMode.TRACKED);
                } else if (entity.getMovementMode() == EntityMovementMode.TRACKED
                        || entity.getMovementMode() == EntityMovementMode.WHEELED) {
                    r.messageId = 2451;
                } else if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                    r.messageId = 2452;
                } else if (entity.getMovementMode() == EntityMovementMode.AERODYNE) {
                    r.messageId = 2453;
                } else {
                    r.messageId = 2450;
                }
                if (entity.isAero()) {
                    int altitude = entity.getAltitude();
                    if (altitude == 0 && md.getFinalElevation() >= 8) {
                        altitude = 1;
                    }
                    if (altitude == 0) {
                        ((IAero) entity).land();
                    } else {
                        ((IAero) entity).liftOff(altitude);
                    }
                }
            }
            addReport(r);
        }

        // update entity's locations' exposure
        gameManager.addReport(gameManager.doSetLocationsExposure(entity,
                getGame().getBoard().getHex(curPos), false, entity.getElevation()));

        // Check the falls_end_movement option to see if it should be able to
        // move on.
        // Need to check here if the 'Mek actually went from non-prone to prone
        // here because 'fellDuringMovement' is sometimes abused just to force
        // another turn and so doesn't reliably tell us.
        boolean continueTurnFromFall = !(getGame().getOptions()
                .booleanOption(OptionsConstants.ADVGRNDMOV_FALLS_END_MOVEMENT)
                && (entity instanceof Mek) && !wasProne && entity.isProne())
                && (fellDuringMovement && !entity.isCarefulStand()) // Careful standing takes up the whole turn
                && !turnOver && (entity.mpUsed < entity.getRunMP())
                && (overallMoveType != EntityMovementType.MOVE_JUMP);
        if ((continueTurnFromFall || continueTurnFromPBS || continueTurnFromFishtail || continueTurnFromLevelDrop
                || continueTurnFromCliffAscent
                || detectedHiddenHazard)
                && entity.isSelectableThisTurn() && !entity.isDoomed()) {
            entity.applyDamage();
            entity.setDone(false);
            entity.setTurnInterrupted(true);

            GameTurn newTurn = new SpecificEntityTurn(entity.getOwner().getId(), entity.getId());
            // Need to set the new turn's multiTurn state
            newTurn.setMultiTurn(true);
            getGame().insertNextTurn(newTurn);
            // brief everybody on the turn update
            gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

            // let everyone know about what just happened
            if (gameManager.getvPhaseReport().size() > 1) {
                gameManager.send(entity.getOwner().getId(), gameManager.createSpecialReportPacket());
            }
        } else {
            if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                Hex hex = getGame().getBoard().getHex(curPos);
                if (md.automaticWiGELanding(false)) {
                    // try to land safely; LAMs require a psr when landing with gyro or leg actuator
                    // damage and ProtoMeks always require a roll
                    int elevation = (null == prevStep) ? entity.getElevation() : prevStep.getElevation();
                    if (entity.hasETypeFlag(Entity.ETYPE_LAND_AIR_MEK)) {
                        addReport(gameManager.landAirMek((LandAirMek) entity, entity.getPosition(), elevation,
                                entity.delta_distance));
                    } else if (entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                        gameManager.getvPhaseReport()
                                .addAll(gameManager.landGliderPM((ProtoMek) entity, entity.getPosition(),
                                        elevation, entity.delta_distance));
                    } else {
                        r = new Report(2123);
                        r.addDesc(entity);
                        r.subject = entity.getId();
                        addReport(r);
                    }

                    if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
                        Building bldg = getGame().getBoard().getBuildingAt(entity.getPosition());
                        entity.setElevation(hex.terrainLevel(Terrains.BLDG_ELEV));
                        gameManager.addAffectedBldg(bldg, gameManager.checkBuildingCollapseWhileMoving(bldg,
                                entity, entity.getPosition()));
                    } else if (entity.isLocationProhibited(entity.getPosition(), 0)
                            && !hex.hasPavement()) {
                        // crash
                        r = new Report(2124);
                        r.addDesc(entity);
                        r.subject = entity.getId();
                        addReport(r);
                        addReport(gameManager.crashVTOLorWiGE((Tank) entity));
                    } else {
                        entity.setElevation(0);
                    }

                    // Check for stacking violations in the target hex
                    Entity violation = Compute.stackingViolation(getGame(),
                            entity.getId(), entity.getPosition(), entity.climbMode());
                    if (violation != null) {
                        PilotingRollData prd = new PilotingRollData(
                                violation.getId(), 2, "fallen on");
                        if (violation instanceof Dropship) {
                            violation = entity;
                            prd = null;
                        }
                        Coords targetDest = Compute.getValidDisplacement(getGame(),
                                violation.getId(), entity.getPosition(), 0);
                        if (targetDest != null) {
                            addReport(gameManager.doEntityDisplacement(violation,
                                    entity.getPosition(), targetDest, prd));
                            // Update the violating entity's position on the
                            // client.
                            gameManager.entityUpdate(violation.getId());
                        } else {
                            // ack! automatic death! Tanks
                            // suffer an ammo/power plant hit.
                            // TODO : a Mek suffers a Head Blown Off crit.
                            addReport(gameManager.destroyEntity(violation,
                                    "impossible displacement",
                                    violation instanceof Mek,
                                    violation instanceof Mek));
                        }
                    }
                } else if (!entity.hasETypeFlag(Entity.ETYPE_LAND_AIR_MEK)
                        && !entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {

                    // we didn't land, so we go to elevation 1 above the terrain
                    // features
                    // it might have been higher than one due to the extra MPs
                    // it can spend to stay higher during movement, but should
                    // end up at one

                    entity.setElevation(Math.min(entity.getElevation(),
                            1 + hex.maxTerrainFeatureElevation(
                                    getGame().getBoard().inAtmosphere())));
                }
            }

            // If we've somehow gotten here as an airborne LAM with a destroyed side torso
            // (such as conversion while dropping), crash now.
            if (entity instanceof LandAirMek
                    && (entity.isLocationBad(Mek.LOC_RT) || entity.isLocationBad(Mek.LOC_LT))) {
                r = new Report(9710);
                r.subject = entity.getId();
                r.addDesc(entity);
                if (entity.isAirborneVTOLorWIGE()) {
                    addReport(r);
                    gameManager.crashAirMek(entity, new PilotingRollData(entity.getId(), TargetRoll.AUTOMATIC_FAIL,
                            "side torso destroyed"), gameManager.getvPhaseReport());
                } else if (entity.isAirborne() && entity.isAero()) {
                    addReport(r);
                    addReport(gameManager.processCrash(entity, ((IAero) entity).getCurrentVelocity(),
                            entity.getPosition()));
                }
            }

            entity.setDone(true);
        }

        if (dropshipStillUnloading) {
            // turns should have already been inserted but we need to set the
            // entity as not done
            entity.setDone(false);
        }

        // If the entity is being swarmed, update the attacker's position.
        if (Entity.NONE != swarmerId) {
            final Entity swarmer = getGame().getEntity(swarmerId);
            swarmer.setPosition(curPos);
            // If the hex is on fire, and the swarming infantry is
            // *not* Battle Armor, it drops off.
            if (!(swarmer instanceof BattleArmor) && getGame().getBoard()
                    .getHex(curPos).containsTerrain(Terrains.FIRE)) {
                swarmer.setSwarmTargetId(Entity.NONE);
                entity.setSwarmAttackerId(Entity.NONE);
                r = new Report(2145);
                r.subject = entity.getId();
                r.indent();
                r.add(swarmer.getShortName(), true);
                addReport(r);
            }
            gameManager.entityUpdate(swarmerId);
        }

        // Update the entity's position,
        // unless it is off the game map.
        if (!getGame().isOutOfGame(entity)) {
            gameManager.entityUpdate(entity.getId(), movePath, true, losCache);
            if (entity.isDoomed()) {
                gameManager.send(gameManager.createRemoveEntityPacket(entity.getId(),
                        entity.getRemovalCondition()));
            }
        }

        // If the entity is towing trailers, update the position of those trailers
        if (!entity.getAllTowedUnits().isEmpty()) {
            List<Integer> reversedTrailers = new ArrayList<>(entity.getAllTowedUnits()); // initialize with a copy (no
                                                                                         // need to initialize to an
                                                                                         // empty list first)
            Collections.reverse(reversedTrailers); // reverse in-place
            List<Coords> trailerPath = gameManager.initializeTrailerCoordinates(entity, reversedTrailers); // no need to
                                                                                                           // initialize
                                                                                                           // to an
                                                                                                           // empty list
                                                                                                           // first
            gameManager.processTrailerMovement(entity, trailerPath);
        }

        // recovered units should now be recovered and dealt with
        if (entity.isAero() && recovered && (loader != null)) {

            if (loader.isCapitalFighter()) {
                if (!(loader instanceof FighterSquadron)) {
                    // this is a solo capital fighter so we need to add a new
                    // squadron and load both the loader and loadee
                    FighterSquadron fs = new FighterSquadron();
                    fs.setDeployed(true);
                    fs.setId(getGame().getNextEntityId());
                    fs.setCurrentVelocity(((Aero) loader).getCurrentVelocity());
                    fs.setNextVelocity(((Aero) loader).getNextVelocity());
                    fs.setVectors(loader.getVectors());
                    fs.setFacing(loader.getFacing());
                    fs.setOwner(entity.getOwner());
                    // set velocity and heading the same as parent entity
                    getGame().addEntity(fs);
                    gameManager.send(gameManager.createAddEntityPacket(fs.getId()));
                    // make him not get a move this turn
                    fs.setDone(true);
                    // place on board
                    fs.setPosition(loader.getPosition());
                    gameManager.loadUnit(fs, loader, -1);
                    loader = fs;
                    gameManager.entityUpdate(fs.getId());
                }
                loader.load(entity);
            } else {
                loader.recover(entity);
                entity.setRecoveryTurn(5);
            }

            // The loaded unit is being carried by the loader.
            entity.setTransportId(loader.getId());

            // Remove the loaded unit from the screen.
            entity.setPosition(null);

            // Update the loaded unit.
            gameManager.entityUpdate(entity.getId());
        }

        // even if load was unsuccessful, I may need to update the loader
        if (null != loader) {
            gameManager.entityUpdate(loader.getId());
        }

        // if using double blind, update the player on new units he might see
        if (gameManager.doBlind()) {
            gameManager.send(entity.getOwner().getId(),
                    gameManager.createFilteredFullEntitiesPacket(entity.getOwner(), losCache));
        }

        // if we generated a charge attack, report it now
        if (charge != null) {
            gameManager.send(gameManager.getPacketHelper().createChargeAttackPacket(charge));
        }

        // if we generated a ram attack, report it now
        if (ram != null) {
            gameManager.send(gameManager.getPacketHelper().createChargeAttackPacket(ram));
        }
        if ((entity instanceof Mek) && entity.hasEngine() && ((Mek) entity).isIndustrial()
                && !entity.hasEnvironmentalSealing()
                && (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            if ((!entity.isProne()
                    && (getGame().getBoard().getHex(entity.getPosition())
                            .terrainLevel(Terrains.WATER) >= 2))
                    || (entity.isProne()
                            && (getGame().getBoard().getHex(entity.getPosition())
                                    .terrainLevel(Terrains.WATER) == 1))) {
                ((Mek) entity).setJustMovedIntoIndustrialKillingWater(true);

            } else {
                ((Mek) entity).setJustMovedIntoIndustrialKillingWater(false);
                ((Mek) entity).setShouldDieAtEndOfTurnBecauseOfWater(false);
            }
        }
    }

    /**
     * Iterate through the steps of the movement path and handle each step.
     */
    private void processSteps() {
        for (final Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            EntityMovementType stepMoveType = step.getMovementType(md.isEndStep(step));
            wasProne = entity.isProne();
            boolean isPavementStep = step.isPavementStep();
            entity.inReverse = step.isThisStepBackwards();
            boolean entityFellWhileAttemptingToStand = false;
            boolean isOnGround = !i.hasMoreElements();
            isOnGround |= stepMoveType != EntityMovementType.MOVE_JUMP;
            isOnGround &= step.getElevation() < 1;

            // Check for hidden units point blank shots
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                for (Entity e : hiddenEnemies) {
                    int dist = e.getPosition().distance(step.getPosition());
                    // Checking for same hex and stacking violation
                    if ((dist == 0) && !continueTurnFromPBS
                            && (Compute.stackingViolation(getGame(), entity.getId(),
                                    step.getPosition(), entity.climbMode()) != null)) {
                        // Moving into hex of a hidden unit detects the unit
                        e.setHidden(false);
                        gameManager.entityUpdate(e.getId());
                        r = new Report(9960);
                        r.addDesc(entity);
                        r.subject = entity.getId();
                        r.add(e.getPosition().getBoardNum());
                        addReport(r);
                        // Report the block
                        if (gameManager.doBlind()) {
                            r = new Report(9961);
                            r.subject = e.getId();
                            r.addDesc(e);
                            r.addDesc(entity);
                            r.add(step.getPosition().getBoardNum());
                            addReport(r);
                        }
                        // Report halted movement
                        r = new Report(9962);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        r.add(step.getPosition().getBoardNum());
                        addReport(r);
                        addNewLines();
                        addNewLines();
                        // If we aren't at the end, send a special report
                        if ((getGame().getTurnIndex() + 1) < getGame().getTurnsList().size()) {
                            gameManager.send(e.getOwner().getId(), gameManager.createSpecialReportPacket());
                            gameManager.send(entity.getOwner().getId(), gameManager.createSpecialReportPacket());
                        }
                        entity.setDone(true);
                        gameManager.entityUpdate(entity.getId(), movePath, true, losCache);
                        return;
                        // Potential point-blank shot
                    } else if ((dist == 1) && !e.madePointblankShot()) {
                        entity.setPosition(step.getPosition());
                        entity.setFacing(step.getFacing());
                        // If not set, BV icons could have wrong facing
                        entity.setSecondaryFacing(step.getFacing());
                        // Update entity position on client
                        gameManager.send(e.getOwnerId(), gameManager.createEntityPacket(entity.getId(), null));
                        boolean tookPBS = gameManager.processPointblankShotCFR(e, entity);
                        // Movement should be interrupted
                        if (tookPBS) {
                            // Attacking reveals hidden unit
                            e.setHidden(false);
                            gameManager.entityUpdate(e.getId());
                            r = new Report(9960);
                            r.addDesc(entity);
                            r.subject = entity.getId();
                            r.add(e.getPosition().getBoardNum());
                            gameManager.getvPhaseReport().addElement(r);
                            continueTurnFromPBS = true;

                            curFacing = entity.getFacing();
                            curPos = entity.getPosition();
                            mpUsed = step.getMpUsed();
                            break;
                        }
                    }
                }
            }

            // stop for illegal movement
            if (stepMoveType == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            // Extra damage if first and last hex are magma
            if (firstStep) {
                firstHex = getGame().getBoard().getHex(curPos);
            }
            // stop if the entity already killed itself
            if (entity.isDestroyed() || entity.isDoomed()) {
                break;
            }

            if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                if (step.getType() == MovePath.MoveStepType.UP && !entity.isAirborneVTOLorWIGE()) {
                    entity.setWigeLiftoffHover(true);
                } else if (step.getType() == MovePath.MoveStepType.HOVER) {
                    entity.setWigeLiftoffHover(true);
                    entity.setAssaultDropInProgress(false);
                } else if (step.getType() == MovePath.MoveStepType.DOWN && step.getClearance() == 0) {
                    // If this is the first step, use the Entity's starting elevation
                    int elevation = (prevStep == null) ? entity.getElevation() : prevStep.getElevation();
                    if (entity instanceof LandAirMek) {
                        addReport(gameManager.landAirMek((LandAirMek) entity, step.getPosition(), elevation,
                                distance));
                    } else if (entity instanceof ProtoMek) {
                        addReport(gameManager.landGliderPM((ProtoMek) entity, step.getPosition(), elevation,
                                distance));
                    }
                    // landing always ends movement whether successful or not
                }
            }

            // check for MASC failure on first step
            // also check Tanks because they can have superchargers that act
            // like MASc
            if (firstStep) {
                if (entity instanceof VTOL) {
                    // No roll for failure, but +3 on rolls to avoid sideslip.
                    entity.setMASCUsed(md.hasActiveMASC());
                } else if ((entity instanceof Mek) || (entity instanceof Tank)) {
                    // Not necessarily a fall, but we need to give them a new turn to plot movement
                    // with
                    // likely reduced MP.
                    fellDuringMovement = gameManager.checkMASCFailure(entity, md)
                            || gameManager.checkSuperchargerFailure(entity, md);
                }
            }

            if (firstStep) {
                rollTarget = entity.checkGunningIt(overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                            curPos, rollTarget, false);
                    if (mof > 0) {
                        // Since this is the first step, we don't have a previous step so we'll pass
                        // this one in case it's needed to process a skid.
                        if (gameManager.processFailedVehicleManeuver(entity, curPos, 0, step,
                                step.isThisStepBackwards(), lastStepMoveType, distance, 2, mof)) {
                            if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                                mpUsed = entity.getRunMP();
                            } else {
                                mpUsed = entity.getRunMPwithoutMASC();
                            }

                            turnOver = true;
                            distance = entity.delta_distance;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        } else if (entity.getFacing() != curFacing) {
                            // If the facing doesn't change we had a minor fishtail that doesn't require
                            // stopping movement.
                            continueTurnFromFishtail = true;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        }
                    }
                }
            }

            // Check for failed maneuver for overdrive on first step. The rules for
            // overdrive do not
            // state this explicitly, but since combining overdrive with gunning it requires
            // two rolls
            // and gunning does state explicitly that the roll is made before movement, this
            // implies the same for overdrive.
            if (firstStep && (overallMoveType == EntityMovementType.MOVE_SPRINT
                    || overallMoveType == EntityMovementType.MOVE_VTOL_SPRINT)) {
                rollTarget = entity.checkUsingOverdrive(EntityMovementType.MOVE_SPRINT);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                            curPos, rollTarget, false);
                    if (mof > 0) {
                        if (gameManager.processFailedVehicleManeuver(entity, curPos, 0, step,
                                step.isThisStepBackwards(),
                                lastStepMoveType, distance, 2, mof)) {
                            if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                                mpUsed = entity.getRunMP();
                            } else {
                                mpUsed = entity.getRunMPwithoutMASC();
                            }

                            turnOver = true;
                            distance = entity.delta_distance;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        } else if (entity.getFacing() != curFacing) {
                            // If the facing doesn't change we had a minor fishtail that doesn't require
                            // stopping movement.
                            continueTurnFromFishtail = true;
                            curFacing = entity.getFacing();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        }
                    }
                }
            }

            if (step.getType() == MovePath.MoveStepType.CONVERT_MODE) {
                entity.setConvertingNow(true);

                // Non-omni QuadVees converting to vehicle mode dump any riding BA in the
                // starting hex if they fail to make an anti-mek check.
                // http://bg.battletech.com/forums/index.php?topic=55263.msg1271423#msg1271423
                if (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_MEK
                        && !entity.isOmni()) {
                    for (Entity rider : entity.getExternalUnits()) {
                        addReport(gameManager.checkDropBAFromConverting(entity, rider, curPos, curFacing,
                                false, false, false));
                    }
                } else if ((entity.getEntityType() & Entity.ETYPE_LAND_AIR_MEK) != 0) {
                    // External units on LAMs, including swarmers, fall automatically and take
                    // damage,
                    // and the LAM itself may take one or more criticals.
                    for (Entity rider : entity.getExternalUnits()) {
                        addReport(gameManager.checkDropBAFromConverting(entity, rider, curPos, curFacing, true, true,
                                true));
                    }
                    final int swarmerId = entity.getSwarmAttackerId();
                    if (Entity.NONE != swarmerId) {
                        addReport(gameManager.checkDropBAFromConverting(entity, getGame().getEntity(swarmerId),
                                curPos, curFacing, true, true, true));
                    }
                }

                continue;
            }

            // did the entity move?
            didMove = step.getDistance() > distance;

            // check for aero stuff
            if (entity.isAirborne() && entity.isAero()) {
                IAero a = (IAero) entity;
                j++;

                // increment straight moves (can't do it at end, because not all
                // steps may be processed)
                a.setStraightMoves(step.getNStraight());

                // TODO : change the way this check is made
                if (!didMove && (md.length() != j)) {
                    thrustUsed += step.getMp();
                } else {
                    // if this was the last move and distance was zero, then add
                    // thrust
                    if (!didMove && (md.length() == j)) {
                        thrustUsed += step.getMp();
                    }
                    // then we moved to a new hex or the last step so check
                    // conditions
                    // structural damage
                    rollTarget = a.checkThrustSI(thrustUsed, overallMoveType);
                    if ((rollTarget.getValue() != TargetRoll.CHECK_FALSE)
                            && !(entity instanceof FighterSquadron) && !getGame().useVectorMove()) {
                        if (!gameManager.doSkillCheckInSpace(entity, rollTarget)) {
                            a.setSI(a.getSI() - 1);
                            if (entity instanceof LandAirMek) {
                                addReport(gameManager.criticalEntity(entity, Mek.LOC_CT, false, 0, 1));
                            }
                            // check for destruction
                            if (a.getSI() == 0) {
                                // Lets auto-eject if we can!
                                if (a instanceof LandAirMek) {
                                    // LAMs eject if the CT destroyed switch is on
                                    LandAirMek lam = (LandAirMek) a;
                                    if (lam.isAutoEject()
                                            && (!getGame().getOptions()
                                                    .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                    || (getGame().getOptions()
                                                            .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                            && lam.isCondEjectCTDest()))) {
                                        addReport(gameManager.ejectEntity(entity, true, false));
                                    }
                                } else {
                                    // Aeros eject if the SI Destroyed switch is on
                                    Aero aero = (Aero) a;
                                    if (aero.isAutoEject()
                                            && (!getGame().getOptions()
                                                    .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                    || (getGame().getOptions()
                                                            .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                                                            && aero.isCondEjectSIDest()))) {
                                        addReport(gameManager.ejectEntity(entity, true, false));
                                    }
                                }
                                addReport(gameManager.destroyEntity(entity, "Structural Integrity Collapse",
                                        false));
                            }
                        }
                    }

                    // check for pilot damage
                    int hits = entity.getCrew().getHits();
                    int health = 6 - hits;

                    if ((thrustUsed > (2 * health)) && !getGame().useVectorMove()
                            && !(entity instanceof TeleMissile)) {
                        int targetRoll = 2 + (thrustUsed - (2 * health))
                                + (2 * hits);
                        gameManager.resistGForce(entity, targetRoll);
                    }

                    thrustUsed = 0;
                }

                if (step.getType() == MovePath.MoveStepType.RETURN) {
                    a.setCurrentVelocity(md.getFinalVelocity());
                    entity.setAltitude(curAltitude);
                    gameManager.processLeaveMap(md, true, Compute.roundsUntilReturn(getGame(), entity));
                    return;
                }

                if (step.getType() == MovePath.MoveStepType.OFF) {
                    a.setCurrentVelocity(md.getFinalVelocity());
                    entity.setAltitude(curAltitude);
                    gameManager.processLeaveMap(md, true, -1);
                    return;
                }

                rollTarget = a.checkRolls(step, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    getGame().addControlRoll(new PilotingRollData(entity.getId(), 0, "excess roll"));
                }

                rollTarget = a.checkManeuver(step, overallMoveType);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    if (!gameManager.doSkillCheckManeuver(entity, rollTarget)) {
                        a.setFailedManeuver(true);
                        int forward = Math.max(step.getVelocityLeft() / 2, 1);
                        if (forward < step.getVelocityLeft()) {
                            fellDuringMovement = true;
                        }
                        // multiply forward by 16 when on ground hexes
                        if (getGame().getBoard().onGround()) {
                            forward *= 16;
                        }
                        while (forward > 0) {
                            curPos = curPos.translated(step.getFacing());
                            forward--;
                            distance++;
                            a.setStraightMoves(a.getStraightMoves() + 1);
                            // make sure it didn't fly off the map
                            if (!getGame().getBoard().contains(curPos)) {
                                a.setCurrentVelocity(md.getFinalVelocity());
                                gameManager.processLeaveMap(md, true, Compute.roundsUntilReturn(getGame(), entity));
                                return;
                                // make sure it didn't crash
                            } else if (gameManager.checkCrash(entity, curPos, step.getAltitude())) {
                                addReport(gameManager.processCrash(entity, step.getVelocity(), curPos));
                                forward = 0;
                                fellDuringMovement = false;
                                crashedDuringMovement = true;
                            }
                        }
                        break;
                    }
                }

                // if out of control, check for possible collision
                if (didMove && a.isOutControlTotal()) {
                    Iterator<Entity> targets = getGame().getEntities(step.getPosition());
                    if (targets.hasNext()) {
                        // Somebody here so check to see if there is a collision
                        int checkRoll = Compute.d6(2);
                        // TODO : change this to 11 for Large Craft
                        int targetRoll = 11;
                        if ((a instanceof Dropship) || (entity instanceof Jumpship)) {
                            targetRoll = 10;
                        }
                        if (checkRoll >= targetRoll) {
                            // this gets complicated, I need to check for each
                            // unit type
                            // by order of movement sub-phase
                            Vector<Integer> potentialSpaceStation = new Vector<>();
                            Vector<Integer> potentialWarShip = new Vector<>();
                            Vector<Integer> potentialJumpShip = new Vector<>();
                            Vector<Integer> potentialDropShip = new Vector<>();
                            Vector<Integer> potentialSmallCraft = new Vector<>();
                            Vector<Integer> potentialASF = new Vector<>();

                            while (targets.hasNext()) {
                                int id = targets.next().getId();
                                Entity ce = getGame().getEntity(id);
                                // if we are in atmosphere and not the same altitude
                                // then skip
                                if (!getGame().getBoard().inSpace() && (ce.getAltitude() != curAltitude)) {
                                    continue;
                                }
                                // you can't collide with yourself
                                if (ce.equals(a)) {
                                    continue;
                                }
                                if (ce instanceof SpaceStation) {
                                    potentialSpaceStation.addElement(id);
                                } else if (ce instanceof Warship) {
                                    potentialWarShip.addElement(id);
                                } else if (ce instanceof Jumpship) {
                                    potentialJumpShip.addElement(id);
                                } else if (ce instanceof Dropship) {
                                    potentialDropShip.addElement(id);
                                } else if (ce instanceof SmallCraft) {
                                    potentialSmallCraft.addElement(id);
                                } else {
                                    // ASF can actually include anything,
                                    // because we might
                                    // have combat dropping troops
                                    potentialASF.addElement(id);
                                }
                            }

                            // ok now go through and see if these have anybody in them
                            int chosen;
                            Entity target;
                            Coords destination;

                            if (!potentialSpaceStation.isEmpty()) {
                                chosen = Compute.randomInt(potentialSpaceStation.size());
                                target = getGame().getEntity(potentialSpaceStation.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialWarShip.isEmpty()) {
                                chosen = Compute.randomInt(potentialWarShip.size());
                                target = getGame().getEntity(potentialWarShip.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialJumpShip.isEmpty()) {
                                chosen = Compute.randomInt(potentialJumpShip.size());
                                target = getGame().getEntity(potentialJumpShip.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialDropShip.isEmpty()) {
                                chosen = Compute.randomInt(potentialDropShip.size());
                                target = getGame().getEntity(potentialDropShip.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialSmallCraft.isEmpty()) {
                                chosen = Compute.randomInt(potentialSmallCraft.size());
                                target = getGame().getEntity(potentialSmallCraft.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            } else if (!potentialASF.isEmpty()) {
                                chosen = Compute.randomInt(potentialASF.size());
                                target = getGame().getEntity(potentialASF.elementAt(chosen));
                                destination = target.getPosition();
                                if (gameManager.processCollision(entity, target, lastPos)) {
                                    curPos = destination;
                                    break;
                                }
                            }
                        }
                    }
                }

                // if in the atmosphere, check for a potential crash
                if (gameManager.checkCrash(entity, step.getPosition(), step.getAltitude())) {
                    addReport(gameManager.processCrash(entity, md.getFinalVelocity(), curPos));
                    crashedDuringMovement = true;
                    // don't do the rest
                    break;
                }

                // handle fighter launching
                if (step.getType() == MovePath.MoveStepType.LAUNCH) {
                    TreeMap<Integer, Vector<Integer>> launched = step.getLaunched();
                    Set<Integer> bays = launched.keySet();
                    Iterator<Integer> bayIter = bays.iterator();
                    Bay currentBay;
                    while (bayIter.hasNext()) {
                        int bayId = bayIter.next();
                        currentBay = entity.getFighterBays().elementAt(bayId);
                        Vector<Integer> launches = launched.get(bayId);
                        int nLaunched = launches.size();
                        // need to make some decisions about how to handle the
                        // distribution
                        // of fighters to doors beyond the launch rate. The most
                        // sensible thing
                        // is probably to distribute them evenly.
                        int doors = currentBay.getCurrentDoors();
                        int[] distribution = new int[doors];
                        for (int l = 0; l < nLaunched; l++) {
                            distribution[l % doors] = distribution[l % doors] + 1;
                        }
                        // ok, now lets launch them
                        r = new Report(9380);
                        r.add(entity.getDisplayName());
                        r.subject = entity.getId();
                        r.add(nLaunched);
                        r.add("bay " + currentBay.getBayNumber() + " (" + doors + " doors)");
                        addReport(r);
                        int currentDoor = 0;
                        int fighterCount = 0;
                        boolean doorDamage = false;
                        for (int fighterId : launches) {
                            // check to see if we are in the same door
                            fighterCount++;

                            // check for door damage
                            Report doorReport = null;
                            if (!doorDamage && (distribution[currentDoor] > 2) && (fighterCount > 2)) {
                                doorReport = new Report(9378);
                                doorReport.subject = entity.getId();
                                doorReport.indent(2);
                                Roll diceRoll = Compute.rollD6(2);
                                doorReport.add(diceRoll);

                                if (diceRoll.getIntValue() == 2) {
                                    doorDamage = true;
                                    doorReport.choose(true);
                                    currentBay.destroyDoorNext();
                                } else {
                                    doorReport.choose(false);
                                }
                                doorReport.newlines++;
                            }

                            if (fighterCount > distribution[currentDoor]) {
                                // move to a new door
                                currentDoor++;
                                fighterCount = 0;
                                doorDamage = false;
                            }
                            int bonus = Math.max(0,
                                    distribution[currentDoor] - 2);

                            Entity fighter = getGame().getEntity(fighterId);
                            if (!gameManager.launchUnit(entity, fighter, curPos, curFacing, step.getVelocity(),
                                    step.getAltitude(), step.getVectors(), bonus)) {
                                logger.error("Server was told to unload "
                                        + fighter.getDisplayName() + " from " + entity.getDisplayName()
                                        + " into " + curPos.getBoardNum());
                            }
                            if (doorReport != null) {
                                addReport(doorReport);
                            }
                        }
                    }
                    // now apply any damage to bay doors
                    entity.resetBayDoors();
                }

                // handle DropShip undocking
                if (step.getType() == MovePath.MoveStepType.UNDOCK) {
                    TreeMap<Integer, Vector<Integer>> launched = step.getLaunched();
                    Set<Integer> collars = launched.keySet();
                    Iterator<Integer> collarIter = collars.iterator();
                    while (collarIter.hasNext()) {
                        int collarId = collarIter.next();
                        Vector<Integer> launches = launched.get(collarId);
                        int nLaunched = launches.size();
                        // ok, now lets launch them
                        r = new Report(9380);
                        r.add(entity.getDisplayName());
                        r.subject = entity.getId();
                        r.add(nLaunched);
                        r.add("collar " + collarId);
                        addReport(r);
                        for (int dropShipId : launches) {
                            // check to see if we are in the same door
                            Entity ds = getGame().getEntity(dropShipId);
                            if (!gameManager.launchUnit(entity, ds, curPos, curFacing,
                                    step.getVelocity(), step.getAltitude(),
                                    step.getVectors(), 0)) {
                                logger.error("Error! Server was told to unload "
                                        + ds.getDisplayName() + " from "
                                        + entity.getDisplayName() + " into "
                                        + curPos.getBoardNum());
                            }
                        }
                    }
                }

                // handle combat drops
                if (step.getType() == MovePath.MoveStepType.DROP) {
                    TreeMap<Integer, Vector<Integer>> dropped = step.getLaunched();
                    Set<Integer> bays = dropped.keySet();
                    Iterator<Integer> bayIter = bays.iterator();
                    Bay currentBay;
                    while (bayIter.hasNext()) {
                        int bayId = bayIter.next();
                        currentBay = entity.getTransportBays().elementAt(bayId);
                        Vector<Integer> drops = dropped.get(bayId);
                        int nDropped = drops.size();
                        // ok, now lets drop them
                        r = new Report(9386);
                        r.add(entity.getDisplayName());
                        r.subject = entity.getId();
                        r.add(nDropped);
                        addReport(r);
                        for (int unitId : drops) {
                            if (Compute.d6(2) == 2) {
                                r = new Report(9390);
                                r.subject = entity.getId();
                                r.indent(1);
                                r.add(currentBay.getType());
                                addReport(r);
                                currentBay.destroyDoorNext();
                            }
                            Entity drop = getGame().getEntity(unitId);
                            gameManager.dropUnit(drop, entity, curPos, step.getAltitude());
                        }
                    }
                    // now apply any damage to bay doors
                    entity.resetBayDoors();
                }
            }

            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step, overallMoveType);

            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Unless we're an ICE- or fuel cell-powered IndustrialMek,
                // standing up builds heat.
                if ((entity instanceof Mek) && entity.hasEngine() && !(((Mek) entity).isIndustrial()
                        && ((entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)
                                || (entity.getEngine().getEngineType() == Engine.FUEL_CELL)))) {
                    entity.heatBuildup += 1;
                }
                entity.setProne(false);
                // entity.setHullDown(false);
                wasProne = false;
                getGame().resetPSRs(entity);
                entityFellWhileAttemptingToStand = !gameManager.doSkillCheckInPlace(entity, rollTarget);
            }
            // did the entity just fall?
            if (entityFellWhileAttemptingToStand) {
                moveType = stepMoveType;
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                if (!entity.isCarefulStand()) {
                    break;
                }
            } else if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                entity.setHullDown(false);
            }

            if (step.getType() == MovePath.MoveStepType.UNJAM_RAC) {
                entity.setUnjammingRAC(true);
                getGame().addAction(new UnjamAction(entity.getId()));

                // for Aeros this will end movement prematurely
                // if we break
                if (!(entity.isAirborne())) {
                    break;
                }
            }

            if (step.getType() == MovePath.MoveStepType.LAY_MINE) {
                gameManager.layMine(entity, step.getMineToLay(), step.getPosition());
                continue;
            }

            if (step.getType() == MovePath.MoveStepType.CLEAR_MINEFIELD) {
                ClearMinefieldAction cma = new ClearMinefieldAction(entity.getId(), step.getMinefield());
                entity.setClearingMinefield(true);
                getGame().addAction(cma);
                break;
            }

            if ((step.getType() == MovePath.MoveStepType.SEARCHLIGHT)
                    && entity.hasSearchlight()) {
                final boolean SearchOn = !entity.isUsingSearchlight();
                entity.setSearchlightState(SearchOn);
                if (gameManager.doBlind()) { // if double blind, we may need to filter the
                    // players that receive this message
                    List<Player> playersVector = getGame().getPlayersList();
                    Vector<Player> vCanSee = gameManager.whoCanSee(entity);
                    for (Player p : playersVector) {
                        if (vCanSee.contains(p)) { // Player sees the unit
                            gameManager.sendServerChat(p.getId(),
                                    entity.getDisplayName()
                                            + " switched searchlight "
                                            + (SearchOn ? "on" : "off") + '.');
                        } else {
                            gameManager.sendServerChat(p.getId(),
                                    "An unseen unit" + " switched searchlight "
                                            + (SearchOn ? "on" : "off") + '.');
                        }
                    }
                } else { // No double blind, everyone can see this
                    gameManager.sendServerChat(
                            entity.getDisplayName() + " switched searchlight "
                                    + (SearchOn ? "on" : "off") + '.');
                }
            }

            // set most step parameters
            moveType = stepMoveType;
            distance = step.getDistance();
            mpUsed = step.getMpUsed();

            if (cachedGravityLimit < 0) {
                cachedGravityLimit = EntityMovementType.MOVE_JUMP == moveType
                        ? entity.getJumpMP(MPCalculationSetting.NO_GRAVITY)
                        : entity.getRunningGravityLimit();
            }
            // check for charge
            if (step.getType() == MovePath.MoveStepType.CHARGE) {
                if (entity.canCharge()) {
                    gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType,
                            curPos, cachedGravityLimit);
                    Targetable target = step.getTarget(getGame());
                    if (target != null) {
                        ChargeAttackAction caa = new ChargeAttackAction(
                                entity.getId(), target.getTargetType(),
                                target.getId(), target.getPosition());
                        entity.setDisplacementAttack(caa);
                        getGame().addCharge(caa);
                        charge = caa;
                    } else {
                        String message = "Illegal charge!! " + entity.getDisplayName() +
                                " is attempting to charge a null target!";
                        logger.info(message);
                        gameManager.sendServerChat(message);
                        return;
                    }
                } else if (entity.isAirborneVTOLorWIGE() && entity.canRam()) {
                    gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType,
                            curPos, cachedGravityLimit);
                    Targetable target = step.getTarget(getGame());
                    if (target != null) {
                        AirMekRamAttackAction raa = new AirMekRamAttackAction(
                                entity.getId(), target.getTargetType(),
                                target.getId(), target.getPosition());
                        entity.setDisplacementAttack(raa);
                        entity.setRamming(true);
                        getGame().addCharge(raa);
                        charge = raa;
                    } else {
                        String message = "Illegal charge!! " + entity.getDisplayName()
                                + " is attempting to charge a null target!";
                        logger.info(message);
                        gameManager.sendServerChat(message);
                        return;
                    }
                } else {
                    gameManager.sendServerChat("Illegal charge!! I don't think "
                            + entity.getDisplayName()
                            + " should be allowed to charge,"
                            + " but the client of "
                            + entity.getOwner().getName() + " disagrees.");
                    gameManager.sendServerChat("Please make sure "
                            + entity.getOwner().getName()
                            + " is running MegaMek " + MMConstants.VERSION
                            + ", or if that is already the case, submit a bug report at https://github.com/MegaMek/megamek/issues");
                    return;
                }
                break;
            }

            // check for dfa
            if (step.getType() == MovePath.MoveStepType.DFA) {
                if (entity.canDFA()) {
                    gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType, curPos, cachedGravityLimit);
                    Targetable target = step.getTarget(getGame());

                    int targetType;
                    int targetID;

                    // if it's a valid target, then simply pass along the type and ID
                    if (target != null) {
                        targetID = target.getId();
                        targetType = target.getTargetType();
                        // if the target has become invalid somehow, or was incorrectly declared in the
                        // first place
                        // log the error, then put some defaults in for the DFA and proceed as if the
                        // target had been moved/destroyed
                    } else {
                        String errorMessage = "Illegal DFA by " + entity.getDisplayName()
                                + " against non-existent entity at " + step.getTargetPosition();
                        gameManager.sendServerChat(errorMessage);
                        logger.error(errorMessage);
                        targetID = Entity.NONE;
                        // doesn't really matter, DFA processing will cut out early if target resolves
                        // as null
                        targetType = Targetable.TYPE_ENTITY;
                    }

                    DfaAttackAction daa = new DfaAttackAction(entity.getId(),
                            targetType, targetID,
                            step.getPosition());
                    entity.setDisplacementAttack(daa);
                    entity.setElevation(step.getElevation());
                    getGame().addCharge(daa);
                    charge = daa;

                } else {
                    gameManager.sendServerChat("Illegal DFA!! I don't think "
                            + entity.getDisplayName()
                            + " should be allowed to DFA,"
                            + " but the client of "
                            + entity.getOwner().getName() + " disagrees.");
                    gameManager.sendServerChat("Please make sure "
                            + entity.getOwner().getName()
                            + " is running MegaMek " + MMConstants.VERSION
                            + ", or if that is already the case, submit a bug report at https://github.com/MegaMek/megamek/issues");
                    return;
                }

                break;
            }

            // check for ram
            if (step.getType() == MovePath.MoveStepType.RAM) {
                if (entity.canRam()) {
                    Targetable target = step.getTarget(getGame());
                    RamAttackAction raa = new RamAttackAction(entity.getId(),
                            target.getTargetType(), target.getId(),
                            target.getPosition());
                    entity.setRamming(true);
                    getGame().addRam(raa);
                    ram = raa;
                } else {
                    gameManager.sendServerChat("Illegal ram!! I don't think "
                            + entity.getDisplayName()
                            + " should be allowed to charge,"
                            + " but the client of "
                            + entity.getOwner().getName() + " disagrees.");
                    gameManager.sendServerChat("Please make sure "
                            + entity.getOwner().getName()
                            + " is running MegaMek " + MMConstants.VERSION
                            + ", or if that is already the case, submit a bug report at https://github.com/MegaMek/megamek/issues");
                    return;
                }
                break;
            }

            if (step.isVTOLBombingStep()) {
                ((IBomber) entity).setVTOLBombTarget(step.getTarget(getGame()));
            } else if (step.isStrafingStep() && (entity instanceof VTOL)) {
                ((VTOL) entity).getStrafingCoords().add(step.getPosition());
            }

            if ((step.getType() == MovePath.MoveStepType.ACC) || (step.getType() == MovePath.MoveStepType.ACCN)) {
                if (entity.isAero()) {
                    IAero a = (IAero) entity;
                    if (step.getType() == MovePath.MoveStepType.ACCN) {
                        a.setAccLast(true);
                    } else {
                        a.setAccDecNow(true);
                        a.setCurrentVelocity(a.getCurrentVelocity() + 1);
                    }
                    a.setNextVelocity(a.getNextVelocity() + 1);
                }
            }

            if ((step.getType() == MovePath.MoveStepType.DEC) || (step.getType() == MovePath.MoveStepType.DECN)) {
                if (entity.isAero()) {
                    IAero a = (IAero) entity;
                    if (step.getType() == MovePath.MoveStepType.DECN) {
                        a.setAccLast(true);
                    } else {
                        a.setAccDecNow(true);
                        a.setCurrentVelocity(a.getCurrentVelocity() - 1);
                    }
                    a.setNextVelocity(a.getNextVelocity() - 1);
                }
            }

            if (step.getType() == MovePath.MoveStepType.EVADE) {
                entity.setEvading(true);
            }

            if (step.getType() == MovePath.MoveStepType.BRACE) {
                entity.setBraceLocation(step.getBraceLocation());
            }

            if (step.getType() == MovePath.MoveStepType.SHUTDOWN) {
                entity.performManualShutdown();
                gameManager.sendServerChat(entity.getDisplayName() + " has shutdown.");
            }

            if (step.getType() == MovePath.MoveStepType.STARTUP) {
                entity.performManualStartup();
                gameManager.sendServerChat(entity.getDisplayName() + " has started up.");
            }

            if (step.getType() == MovePath.MoveStepType.SELF_DESTRUCT) {
                entity.setSelfDestructing(true);
            }

            if (step.getType() == MovePath.MoveStepType.ROLL) {
                if (entity.isAero()) {
                    IAero a = (IAero) entity;
                    a.setRolled(!a.isRolled());
                }
            }

            // check for dig in or fortify
            if (entity instanceof Infantry) {
                Infantry inf = (Infantry) entity;
                if (step.getType() == MovePath.MoveStepType.DIG_IN) {
                    inf.setDugIn(Infantry.DUG_IN_WORKING);
                    continue;
                } else if (step.getType() == MovePath.MoveStepType.FORTIFY) {
                    if (!inf.hasWorkingMisc(MiscType.F_TRENCH_CAPABLE)) {
                        gameManager.sendServerChat(entity.getDisplayName()
                                + " failed to fortify because it is missing suitable equipment");
                    }
                    inf.setDugIn(Infantry.DUG_IN_FORTIFYING1);
                    continue;
                } else if ((step.getType() != MovePath.MoveStepType.TURN_LEFT)
                        && (step.getType() != MovePath.MoveStepType.TURN_RIGHT)) {
                    // other movement clears dug in status
                    inf.setDugIn(Infantry.DUG_IN_NONE);
                }

                if (step.getType() == MovePath.MoveStepType.TAKE_COVER) {
                    if (Infantry.hasValidCover(getGame(), step.getPosition(),
                            step.getElevation())) {
                        inf.setTakingCover(true);
                    } else {
                        gameManager.sendServerChat(entity.getDisplayName()
                                + " failed to take cover: "
                                + "no valid unit found in "
                                + step.getPosition());
                    }
                }
            }

            // check for tank fortify
            if (entity instanceof Tank) {
                Tank tnk = (Tank) entity;
                if (step.getType() == MovePath.MoveStepType.FORTIFY) {
                    if (!tnk.hasWorkingMisc(MiscType.F_TRENCH_CAPABLE)) {
                        gameManager.sendServerChat(entity.getDisplayName()
                                + " failed to fortify because it is missing suitable equipment");
                    }
                    tnk.setDugIn(Tank.DUG_IN_FORTIFYING1);
                }
            }

            // If we have turned, check whether we have fulfilled any turn mode
            // requirements.
            if ((step.getType() == MovePath.MoveStepType.TURN_LEFT
                    || step.getType() == MovePath.MoveStepType.TURN_RIGHT)
                    && entity.usesTurnMode()) {
                int straight = 0;
                if (prevStep != null) {
                    straight = prevStep.getNStraight();
                }
                rollTarget = entity.checkTurnModeFailure(overallMoveType, straight,
                        md.getMpUsed(), step.getPosition());
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                            curPos, rollTarget, false);
                    if (mof > 0) {
                        if (gameManager.processFailedVehicleManeuver(entity, curPos,
                                step.getFacing() - curFacing,
                                (null == prevStep) ? step : prevStep,
                                step.isThisStepBackwards(),
                                lastStepMoveType, distance, mof, mof)) {
                            if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                                mpUsed = entity.getRunMP();
                            } else {
                                mpUsed = entity.getRunMPwithoutMASC();
                            }

                            turnOver = true;
                            distance = entity.delta_distance;
                        } else {
                            continueTurnFromFishtail = true;
                        }
                        curFacing = entity.getFacing();
                        curPos = entity.getPosition();
                        entity.setSecondaryFacing(curFacing);
                        break;
                    }
                }
            }

            if (step.getType() == MovePath.MoveStepType.BOOTLEGGER) {
                rollTarget = entity.getBasePilotingRoll();
                entity.addPilotingModifierForTerrain(rollTarget);
                rollTarget.addModifier(0, "bootlegger maneuver");
                int mof = gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                        curPos, curPos, rollTarget, false);
                if (mof > 0) {
                    // If the bootlegger maneuver fails, we treat it as a turn in a random
                    // direction.
                    gameManager.processFailedVehicleManeuver(entity, curPos, Compute.d6() < 4 ? -1 : 1,
                            (null == prevStep) ? step : prevStep,
                            step.isThisStepBackwards(), lastStepMoveType, distance, 2, mof);
                    curFacing = entity.getFacing();
                    curPos = entity.getPosition();
                    break;
                }
            }

            // set last step parameters
            curPos = step.getPosition();
            if (!((entity.getJumpType() == Mek.JUMP_BOOSTER) && step.isJumping())) {
                curFacing = step.getFacing();
            }
            // check if a building PSR will be needed later, before setting the
            // new elevation
            int buildingMove = entity.checkMovementInBuilding(step, prevStep, curPos, lastPos);
            curVTOLElevation = step.getElevation();
            curAltitude = step.getAltitude();
            curElevation = step.getElevation();
            curClimbMode = step.climbMode();
            // set elevation in case of collapses
            entity.setElevation(step.getElevation());
            // set climb mode in case of skid
            entity.setClimbMode(curClimbMode);

            Hex curHex = getGame().getBoard().getHex(curPos);

            // when first entering a building, we need to roll what type
            // of basement it has
            if (isOnGround && curHex.containsTerrain(Terrains.BUILDING)) {
                Building bldg = getGame().getBoard().getBuildingAt(curPos);
                if (bldg.rollBasement(curPos, getGame().getBoard(), gameManager.getvPhaseReport())) {
                    gameManager.sendChangedHex(curPos);
                    Vector<Building> buildings = new Vector<>();
                    buildings.add(bldg);
                    gameManager.sendChangedBuildings(buildings);
                }
            }

            // check for automatic unstick
            if (entity.canUnstickByJumping() && entity.isStuck()
                    && (moveType == EntityMovementType.MOVE_JUMP)) {
                entity.setStuck(false);
                entity.setCanUnstickByJumping(false);
            }

            // check for leap
            if (!lastPos.equals(curPos)
                    && (stepMoveType != EntityMovementType.MOVE_JUMP) && (entity instanceof Mek)
                    && !entity.isAirborne() && (step.getClearance() <= 0) // Don't check airborne LAMs
                    && getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEAPING)) {
                int leapDistance = (lastElevation
                        + getGame().getBoard().getHex(lastPos).getLevel())
                        - (curElevation + curHex.getLevel());
                if (leapDistance > 2) {
                    // skill check for leg damage
                    rollTarget = entity.getBasePilotingRoll(stepMoveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos);
                    rollTarget.append(new PilotingRollData(entity.getId(),
                            2 * leapDistance, "leaping (leg damage)"));
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                            lastPos, curPos, rollTarget, false)) {
                        // do leg damage
                        addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_LLEG), leapDistance));
                        addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_RLEG), leapDistance));
                        addNewLines();
                        addReport(gameManager.criticalEntity(entity, Mek.LOC_LLEG, false, 0, 0));
                        addNewLines();
                        addReport(gameManager.criticalEntity(entity, Mek.LOC_RLEG, false, 0, 0));
                        if (entity instanceof QuadMek) {
                            addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_LARM), leapDistance));
                            addReport(gameManager.damageEntity(entity, new HitData(Mek.LOC_RARM), leapDistance));
                            addNewLines();
                            addReport(gameManager.criticalEntity(entity, Mek.LOC_LARM, false, 0, 0));
                            addNewLines();
                            addReport(gameManager.criticalEntity(entity, Mek.LOC_RARM, false, 0, 0));
                        }
                    }
                    // skill check for fall
                    rollTarget = entity.getBasePilotingRoll(stepMoveType);
                    entity.addPilotingModifierForTerrain(rollTarget, curPos);
                    rollTarget.append(new PilotingRollData(entity.getId(),
                            leapDistance, "leaping (fall)"));
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                            lastPos, curPos, rollTarget, false)) {
                        entity.setElevation(lastElevation);
                        addReport(gameManager.doEntityFallsInto(entity, lastElevation,
                                lastPos, curPos,
                                entity.getBasePilotingRoll(overallMoveType), false));
                    }
                }
            }

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                    prevStep, step, prevFacing, curFacing, lastPos, curPos,
                    isInfantry, distance - 1);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                boolean psrFailed;
                int startingFacing = entity.getFacing();
                if (entity instanceof Mek) {
                    // We need to ensure that falls will happen from the proper
                    // facing
                    entity.setFacing(curFacing);
                    psrFailed = (0 < gameManager.doSkillCheckWhileMoving(entity,
                            lastElevation, lastPos, lastPos, rollTarget, true));
                } else {
                    psrFailed = (0 < gameManager.doSkillCheckWhileMoving(entity,
                            lastElevation, lastPos, lastPos, rollTarget, false));
                }

                // Does the entity skid?
                if (psrFailed) {

                    if (entity instanceof Tank) {
                        addReport(gameManager.vehicleMotiveDamage((Tank) entity, 0));
                    }

                    curPos = lastPos;
                    int skidDistance = (int) Math.round((double) (distance - 1) / 2);
                    int skidDirection = prevFacing;

                    // All charge damage is based upon
                    // the pre-skid move distance.
                    entity.delta_distance = distance - 1;

                    // Attacks against a skidding target have additional +2.
                    moveType = EntityMovementType.MOVE_SKID;

                    // What is the first hex in the skid?
                    if (step.isThisStepBackwards()) {
                        skidDirection = (skidDirection + 3) % 6;
                    }

                    if (gameManager.processSkid(entity, curPos, prevStep.getElevation(),
                            skidDirection, skidDistance, prevStep,
                            lastStepMoveType)) {
                        return;
                    }

                    // set entity parameters
                    curFacing = entity.getFacing();
                    curPos = entity.getPosition();
                    entity.setSecondaryFacing(curFacing);

                    // skid consumes all movement
                    if (md.hasActiveMASC() || md.hasActiveSupercharger()) {
                        mpUsed = entity.getRunMP();
                    } else {
                        mpUsed = entity.getRunMPwithoutMASC();
                    }

                    entity.moved = moveType;
                    fellDuringMovement = true;
                    turnOver = true;
                    distance = entity.delta_distance;
                    break;

                } else { // End failed-skid-psr
                    // If the check succeeded, restore the facing we had before
                    // if it failed, the fall will have changed facing
                    entity.setFacing(startingFacing);
                }

            } // End need-skid-psr

            // check sideslip
            if ((entity instanceof VTOL)
                    || (entity.getMovementMode() == EntityMovementMode.HOVER)
                    || (entity.getMovementMode() == EntityMovementMode.WIGE
                            && step.getClearance() > 0)) {
                rollTarget = entity.checkSideSlip(moveType, prevHex,
                        overallMoveType, prevStep, prevFacing, curFacing,
                        lastPos, curPos, distance, md.hasActiveMASC());
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    int moF = gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                            lastPos, curPos, rollTarget, false);
                    if (moF > 0) {
                        int elev;
                        int sideslipDistance;
                        int skidDirection;
                        Coords start;
                        if (step.getType() == MovePath.MoveStepType.LATERAL_LEFT
                                || step.getType() == MovePath.MoveStepType.LATERAL_RIGHT
                                || step.getType() == MovePath.MoveStepType.LATERAL_LEFT_BACKWARDS
                                || step.getType() == MovePath.MoveStepType.LATERAL_RIGHT_BACKWARDS) {
                            // A failed controlled sideslip always results in moving one additional hex
                            // in the direction of the intentional sideslip.
                            elev = step.getElevation();
                            sideslipDistance = 1;
                            skidDirection = lastPos.direction(curPos);
                            start = curPos;
                        } else {
                            elev = (null == prevStep) ? curElevation : prevStep.getElevation();
                            // maximum distance is hexes moved / 2
                            sideslipDistance = Math.min(moF, distance / 2);
                            skidDirection = prevFacing;
                            start = lastPos;
                        }
                        if (sideslipDistance > 0) {
                            sideslipped = true;
                            r = new Report(2100);
                            r.subject = entity.getId();
                            r.addDesc(entity);
                            r.add(sideslipDistance);
                            addReport(r);

                            if (gameManager.processSkid(entity, start, elev, skidDirection,
                                    sideslipDistance, (null == prevStep) ? step : prevStep,
                                    lastStepMoveType)) {
                                return;
                            }

                            if (!entity.isDestroyed() && !entity.isDoomed()
                                    && (mpUsed < entity.getRunMP())) {
                                fellDuringMovement = true; // No, but it should
                                // work...
                            }

                            if ((entity.getElevation() == 0)
                                    && ((entity.getMovementMode() == EntityMovementMode.VTOL)
                                            || (entity.getMovementMode() == EntityMovementMode.WIGE))) {
                                turnOver = true;
                            }
                            // set entity parameters
                            curFacing = step.getFacing();
                            curPos = entity.getPosition();
                            entity.setSecondaryFacing(curFacing);
                            break;
                        }
                    }
                }
            }

            // check if we've moved into rubble
            boolean isLastStep = step.equals(md.getLastStep());
            rollTarget = entity.checkRubbleMove(step, overallMoveType, curHex,
                    lastPos, curPos, isLastStep, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos, curPos,
                        rollTarget, true);
            }

            // check if we are using reckless movement
            rollTarget = entity.checkRecklessMove(step, overallMoveType, curHex,
                    lastPos, curPos, prevHex);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                if (entity instanceof Mek) {
                    gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                            curPos, rollTarget, true);
                } else if (entity instanceof Tank) {
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                            lastPos, curPos, rollTarget, false)) {
                        // assume VTOLs in flight are always in clear terrain
                        if ((0 == curHex.terrainsPresent())
                                || (step.getClearance() > 0)) {
                            if (entity instanceof VTOL) {
                                r = new Report(2208);
                            } else {
                                r = new Report(2206);
                            }
                            r.addDesc(entity);
                            r.subject = entity.getId();
                            addReport(r);
                            mpUsed = step.getMpUsed() + 1;
                            fellDuringMovement = true;
                            break;
                        }
                        r = new Report(2207);
                        r.addDesc(entity);
                        r.subject = entity.getId();
                        addReport(r);
                        // until we get a rules clarification assume that the
                        // entity is both giver and taker
                        // for charge damage
                        HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                        addReport(gameManager.damageEntity(entity, hit, ChargeAttackAction
                                .getDamageTakenBy(entity, entity)));
                        turnOver = true;
                        break;
                    }
                }
            }

            // check for breaking magma crust unless we are jumping over the hex
            if (stepMoveType != EntityMovementType.MOVE_JUMP) {
                if (!curPos.equals(lastPos)) {
                    ServerHelper.checkAndApplyMagmaCrust(curHex, step.getElevation(), entity, curPos, false,
                            gameManager.getvPhaseReport(), gameManager);
                    ServerHelper.checkEnteringMagma(curHex, step.getElevation(), entity, gameManager);
                }
            }

            if (step.getType() == MovePath.MoveStepType.CHAFF) {
                List<Mounted<?>> chaffDispensers = entity.getMiscEquipment(MiscType.F_CHAFF_POD)
                        .stream().filter(dispenser -> dispenser.isReady())
                        .collect(Collectors.toList());
                if (chaffDispensers.size() > 0) {
                    chaffDispensers.get(0).setFired(true);
                    gameManager.createSmoke(curPos, SmokeCloud.SMOKE_CHAFF_LIGHT, 1);
                    Hex hex = getGame().getBoard().getHex(curPos);
                    hex.addTerrain(new Terrain(Terrains.SMOKE, SmokeCloud.SMOKE_CHAFF_LIGHT));
                    gameManager.sendChangedHex(curPos);
                    r = new Report(2512)
                            .addDesc(entity)
                            .subject(entity.getId());

                    addReport(r);
                }
            }

            // check for last move ending in magma TODO: build report for end of move
            boolean jumpedIntoMagma = false;
            if (!i.hasMoreElements() && curHex.terrainLevel(Terrains.MAGMA) == 2) {
                jumpedIntoMagma = (moveType == EntityMovementType.MOVE_JUMP);
                if (firstHex.terrainLevel(Terrains.MAGMA) == 2) {
                    r = new Report(2404);
                    r.addDesc(entity);
                    r.subject = entity.getId();
                    addReport(r);
                    gameManager.doMagmaDamage(entity, false);
                }
            }

            if (curHex.terrainLevel(Terrains.MAGMA) != 2 || jumpedIntoMagma) {
                // check if we've moved into a swamp
                rollTarget = entity.checkBogDown(step, lastStepMoveType, curHex,
                        lastPos, curPos, lastElevation, isPavementStep);
                if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                    if (0 < gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos,
                            curPos, rollTarget, false)) {
                        entity.setStuck(true);
                        entity.setCanUnstickByJumping(true);
                        r = new Report(2081);
                        r.add(entity.getDisplayName());
                        r.subject = entity.getId();
                        addReport(r);
                        // check for quicksand
                        addReport(gameManager.checkQuickSand(curPos));
                        // check for accidental stacking violation
                        Entity violation = Compute.stackingViolation(getGame(),
                                entity.getId(), curPos, entity.climbMode());
                        if (violation != null) {
                            // target gets displaced, because of low elevation
                            int direction = lastPos.direction(curPos);
                            Coords targetDest = Compute.getValidDisplacement(getGame(),
                                    entity.getId(), curPos, direction);
                            addReport(gameManager.doEntityDisplacement(violation, curPos,
                                    targetDest,
                                    new PilotingRollData(violation.getId(), 0,
                                            "domino effect")));
                            // Update the violating entity's position on the client.
                            gameManager.entityUpdate(violation.getId());
                        }
                        break;
                    }
                }
            }

            // check to see if we are a mek and we've moved OUT of fire
            Hex lastHex = getGame().getBoard().getHex(lastPos);
            if (entity.tracksHeat() && !entity.isAirborne()) {
                if (!lastPos.equals(curPos) && (prevStep != null)
                        && ((lastHex.containsTerrain(Terrains.FIRE) && (prevStep.getElevation() <= 1))
                                || (lastHex.containsTerrain(Terrains.MAGMA) && (prevStep.getElevation() == 0)))
                        && ((stepMoveType != EntityMovementType.MOVE_JUMP)
                                // Bug #828741 -- jumping bypasses fire, but not on the first step
                                // getMpUsed -- total MP used to this step
                                // getMp -- MP used in this step
                                // the difference will always be 0 on the "first step" of a jump,
                                // and >0 on a step in the midst of a jump
                                || (0 == (step.getMpUsed() - step.getMp())))) {
                    int heat = 0;
                    if (lastHex.containsTerrain(Terrains.FIRE)) {
                        heat += 2;
                    }
                    if (lastHex.terrainLevel(Terrains.MAGMA) == 1) {
                        heat += 2;
                    } else if (lastHex.terrainLevel(Terrains.MAGMA) == 2) {
                        heat += 5;
                    }
                    boolean isMekWithHeatDissipatingArmor = (entity instanceof Mek)
                            && ((Mek) entity).hasIntactHeatDissipatingArmor();
                    if (isMekWithHeatDissipatingArmor) {
                        heat /= 2;
                    }
                    entity.heatFromExternal += heat;
                    r = new Report(2115);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(heat);
                    addReport(r);
                    if (isMekWithHeatDissipatingArmor) {
                        r = new Report(5550);
                        addReport(r);
                    }
                }
            }

            // check to see if we are not a mek and we've moved INTO fire
            if (!(entity instanceof Mek)) {
                boolean underwater = getGame().getBoard().getHex(curPos)
                        .containsTerrain(Terrains.WATER)
                        && (getGame().getBoard().getHex(curPos).depth() > 0)
                        && (step.getElevation() < getGame().getBoard().getHex(curPos).getLevel());
                if (getGame().getBoard().getHex(curPos).containsTerrain(
                        Terrains.FIRE) && !lastPos.equals(curPos)
                        && (stepMoveType != EntityMovementType.MOVE_JUMP)
                        && (step.getElevation() <= 1) && !underwater) {
                    gameManager.doFlamingDamage(entity, curPos);
                }
            }

            if ((getGame().getBoard().getHex(curPos).terrainLevel(Terrains.SMOKE) == SmokeCloud.SMOKE_GREEN)
                    && !stepMoveType.equals(EntityMovementType.MOVE_JUMP) && entity.antiTSMVulnerable()) {
                addReport(gameManager.doGreenSmokeDamage(entity));
            }

            // check for extreme gravity movement
            if (!i.hasMoreElements() && !firstStep) {
                gameManager.checkExtremeGravityMovement(entity, step, lastStepMoveType, curPos, cachedGravityLimit);
            }

            // check for revealed minefields;
            // unless we get errata about it, we assume that the check is done
            // every time we enter a new hex
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP)
                    && !lastPos.equals(curPos)) {
                if (ServerHelper.detectMinefields(getGame(), entity, curPos, gameManager.getvPhaseReport(), gameManager)
                        ||
                        ServerHelper.detectHiddenUnits(getGame(), entity, curPos, gameManager.getvPhaseReport(),
                                gameManager)) {
                    detectedHiddenHazard = true;

                    if (i.hasMoreElements() && (stepMoveType != EntityMovementType.MOVE_JUMP)) {
                        md.clear();
                    }
                }
            }

            // check for minefields. have to check both new hex and new elevation
            // VTOLs may land and submarines may rise or lower into a minefield
            // jumping units may end their movement with a turn but should still check at
            // end of movement
            if (!lastPos.equals(curPos) || (lastElevation != curElevation) ||
                    ((stepMoveType == EntityMovementType.MOVE_JUMP) && !i.hasMoreElements())) {
                boolean boom = false;
                if (isOnGround) {
                    boom = gameManager.checkVibrabombs(entity, curPos, false, lastPos, curPos,
                            gameManager.getvPhaseReport());
                }
                if (getGame().containsMinefield(curPos)) {
                    // set the new position temporarily, because
                    // infantry otherwise would get double damage
                    // when moving from clear into mined woods
                    entity.setPosition(curPos);
                    if (gameManager.enterMinefield(entity, curPos, step.getElevation(),
                            isOnGround, gameManager.getvPhaseReport())) {
                        // resolve any piloting rolls from damage unless unit
                        // was jumping
                        if (stepMoveType != EntityMovementType.MOVE_JUMP) {
                            addReport(gameManager.resolvePilotingRolls(entity));
                            getGame().resetPSRs(entity);
                        }
                        boom = true;
                    }
                    if (wasProne || !entity.isProne()) {
                        entity.setPosition(lastPos);
                    }
                }
                // did anything go boom?
                if (boom) {
                    // set fell during movement so that entity will get another
                    // chance to move with any motive damage
                    // taken account of (functions the same as MASC failure)
                    // only do this if they had more steps (and they were not
                    // jumping
                    if (i.hasMoreElements() && (stepMoveType != EntityMovementType.MOVE_JUMP)) {
                        md.clear();
                        fellDuringMovement = true;
                    }
                    // reset mines if anything detonated
                    gameManager.resetMines();
                }
            }

            // infantry discovers minefields if they end their move
            // in a minefield.
            if (!lastPos.equals(curPos) && !i.hasMoreElements() && isInfantry) {
                if (getGame().containsMinefield(curPos)) {
                    Player owner = entity.getOwner();
                    for (Minefield mf : getGame().getMinefields(curPos)) {
                        if (!owner.containsMinefield(mf)) {
                            r = new Report(2120);
                            r.subject = entity.getId();
                            r.add(entity.getShortName(), true);
                            addReport(r);
                            gameManager.revealMinefield(owner, mf);
                        }
                    }
                }
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, lastStepMoveType, curHex,
                    lastPos, curPos, isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Swarmers need special handling.
                final int swarmerId = entity.getSwarmAttackerId();
                boolean swarmerDone = true;
                Entity swarmer = null;
                if (Entity.NONE != swarmerId) {
                    swarmer = getGame().getEntity(swarmerId);
                    swarmerDone = swarmer.isDone();
                }

                // Now do the skill check.
                entity.setFacing(curFacing);
                gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos, curPos, rollTarget, true);

                // Swarming infantry platoons may drown.
                if (curHex.terrainLevel(Terrains.WATER) > 1) {
                    gameManager.drownSwarmer(entity, curPos);
                }

                // Do we need to remove a game turn for the swarmer
                if (!swarmerDone && (swarmer != null)
                        && (swarmer.isDoomed() || swarmer.isDestroyed())) {
                    // We have to diddle with the swarmer's
                    // status to get its turn removed.
                    swarmer.setDone(false);
                    swarmer.setUnloaded(false);

                    // Dead entities don't take turns.
                    getGame().removeTurnFor(swarmer);
                    gameManager.send(gameManager.getPacketHelper().createTurnListPacket());

                    // Return the original status.
                    swarmer.setDone(true);
                    swarmer.setUnloaded(true);
                }

                // check for inferno wash-off
                gameManager.checkForWashedInfernos(entity, curPos);
            }

            // In water, may or may not be a new hex, necessary to
            // check during movement, for breach damage, and always
            // set dry if appropriate
            // TODO : possibly make the locations local and set later
            addReport(gameManager.doSetLocationsExposure(entity, curHex,
                    stepMoveType == EntityMovementType.MOVE_JUMP,
                    step.getElevation()));

            // check for breaking ice by breaking through from below
            if ((lastElevation < 0) && (step.getElevation() == 0)
                    && lastHex.containsTerrain(Terrains.ICE)
                    && lastHex.containsTerrain(Terrains.WATER)
                    && (stepMoveType != EntityMovementType.MOVE_JUMP)
                    && !lastPos.equals(curPos)) {
                // need to temporarily reset entity's position so it doesn't
                // fall in the ice
                entity.setPosition(curPos);
                r = new Report(2410);
                r.addDesc(entity);
                addReport(r);
                addReport(gameManager.resolveIceBroken(lastPos));
                // ok now set back
                entity.setPosition(lastPos);
            }
            // check for breaking ice by stepping on it
            if (curHex.containsTerrain(Terrains.ICE)
                    && curHex.containsTerrain(Terrains.WATER)
                    && (stepMoveType != EntityMovementType.MOVE_JUMP)
                    && !lastPos.equals(curPos) && !(entity instanceof Infantry)
                    && !(isPavementStep && curHex.containsTerrain(Terrains.BRIDGE))) {
                if (step.getElevation() == 0) {
                    Roll diceRoll = Compute.rollD6(1);
                    r = new Report(2118);
                    r.addDesc(entity);
                    r.add(diceRoll);
                    r.subject = entity.getId();
                    addReport(r);

                    if (diceRoll.getIntValue() == 6) {
                        entity.setPosition(curPos);
                        addReport(gameManager.resolveIceBroken(curPos));
                        curPos = entity.getPosition();
                    }
                }
                // or intersecting it
                else if ((step.getElevation() + entity.height()) == 0) {
                    r = new Report(2410);
                    r.addDesc(entity);
                    addReport(r);
                    addReport(gameManager.resolveIceBroken(curPos));
                }
            }

            // Check for black ice
            int minTemp = -30;
            boolean useBlackIce = getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE);
            boolean goodTemp = getGame().getPlanetaryConditions().getTemperature() <= minTemp;
            boolean goodWeather = getGame().getPlanetaryConditions().getWeather().isIceStorm();
            if (isPavementStep && ((useBlackIce && goodTemp) || goodWeather)) {
                if (!curHex.containsTerrain(Terrains.BLACK_ICE)) {
                    int blackIceChance = Compute.d6(1);
                    if (blackIceChance > 4) {
                        curHex.addTerrain(new Terrain(Terrains.BLACK_ICE, 1));
                        gameManager.sendChangedHex(curPos);
                    }
                }
            }

            // Handle loading units.
            if (step.getType() == MovePath.MoveStepType.LOAD) {

                // Find the unit being loaded.
                Entity loaded = null;
                Iterator<Entity> entities = getGame().getEntities(curPos);
                while (entities.hasNext()) {

                    // Is the other unit friendly and not the current entity?
                    loaded = entities.next();

                    // This should never ever happen, but just in case...
                    if (loaded.equals(null)) {
                        continue;
                    }

                    if (!entity.isEnemyOf(loaded) && !entity.equals(loaded)) {
                        // The moving unit should be able to load the other
                        // unit and the other should be able to have a turn.
                        if (!entity.canLoad(loaded) || !loaded.isLoadableThisTurn()) {
                            // Something is fishy in Denmark.
                            logger
                                    .error(entity.getShortName() + " can not load " + loaded.getShortName());
                            loaded = null;
                        } else {
                            // Have the deployed unit load the indicated unit.
                            gameManager.loadUnit(entity, loaded, loaded.getTargetBay());

                            // Stop looking.
                            break;
                        }

                    } else {
                        // Nope. Discard it.
                        loaded = null;
                    }

                } // Handle the next entity in this hex.

                // We were supposed to find someone to load.
                if (loaded == null) {
                    logger
                            .error("Could not find unit for " + entity.getShortName() + " to load in " + curPos);
                }

            } // End STEP_LOAD

            // Handle towing units.
            if (step.getType() == MovePath.MoveStepType.TOW) {

                // Find the unit being loaded.
                Entity loaded;
                loaded = getGame().getEntity(entity.getTowing());

                // This should never ever happen, but just in case...
                if (loaded == null) {
                    logger.error("Could not find unit for " + entity.getShortName() + " to tow.");
                    continue;
                }

                // The moving unit should be able to tow the other
                // unit and the other should be able to have a turn.
                // FIXME: I know this check duplicates functions already performed when enabling
                // the Tow button.
                // This code made more sense as borrowed from "Load" where we actually rechecked
                // the hex for the target unit.
                // Do we need it here for safety, client/server sync or can this be further
                // streamlined?
                if (!entity.canTow(loaded.getId())) {
                    // Something is fishy in Denmark.
                    logger.error(entity.getShortName() + " can not tow " + loaded.getShortName());
                } else {
                    // Have the deployed unit load the indicated unit.
                    gameManager.towUnit(entity, loaded);
                }
            } // End STEP_TOW

            // Handle mounting units to small craft/DropShip
            if (step.getType() == MovePath.MoveStepType.MOUNT) {
                Targetable mountee = step.getTarget(getGame());
                if (mountee instanceof Entity) {
                    Entity dropShip = (Entity) mountee;
                    if (!dropShip.canLoad(entity)) {
                        // Something is fishy in Denmark.
                        logger
                                .error(dropShip.getShortName() + " can not load " + entity.getShortName());
                    } else {
                        // Have the indicated unit load this unit.
                        entity.setDone(true);
                        gameManager.loadUnit(dropShip, entity, entity.getTargetBay());
                        Bay currentBay = dropShip.getBay(entity);
                        if ((null != currentBay) && (Compute.d6(2) == 2)) {
                            r = new Report(9390);
                            r.subject = entity.getId();
                            r.indent(1);
                            r.add(currentBay.getType());
                            addReport(r);
                            currentBay.destroyDoorNext();
                        }
                        // Stop looking.
                        gameManager.entityUpdate(dropShip.getId());
                        return;
                    }
                }
            } // End STEP_MOUNT

            if (step.getType() == MovePath.MoveStepType.PICKUP_CARGO) {
                var groundObjects = getGame().getGroundObjects(step.getPosition());
                Integer cargoPickupIndex;

                // if there's only one object on the ground, let's just get that one and ignore
                // any parameters
                if (groundObjects.size() == 1) {
                    cargoPickupIndex = 0;
                } else {
                    cargoPickupIndex = step.getAdditionalData(MoveStep.CARGO_PICKUP_KEY);
                }

                Integer cargoPickupLocation = step.getAdditionalData(MoveStep.CARGO_LOCATION_KEY);

                // there have to be objects on the ground and we have to be trying to pick up
                // one of them
                if ((groundObjects.size() > 0) &&
                        (cargoPickupIndex != null) && (cargoPickupIndex >= 0)
                        && (cargoPickupIndex < groundObjects.size())) {

                    ICarryable pickupTarget = groundObjects.get(cargoPickupIndex);
                    if (entity.maxGroundObjectTonnage() >= pickupTarget.getTonnage()) {
                        getGame().removeGroundObject(step.getPosition(), pickupTarget);
                        entity.pickupGroundObject(pickupTarget, cargoPickupLocation);

                        r = new Report(2513);
                        r.subject = entity.getId();
                        r.add(entity.getDisplayName());
                        r.add(pickupTarget.specificName());
                        r.add(step.getPosition().toFriendlyString());
                        addReport(r);

                        // a pickup should be the last step. Send an update for the overall ground
                        // object list.
                        gameManager.sendGroundObjectUpdate();
                        break;
                    } else {
                        logger.warn(entity.getShortName()
                                + " attempted to pick up object but it is too heavy. Carry capacity: "
                                + entity.maxGroundObjectTonnage() + ", object weight: " + pickupTarget.getTonnage());
                    }
                } else {
                    logger
                            .warn(entity.getShortName() + " attempted to pick up non existent object at coords "
                                    + step.getPosition() + ", index " + cargoPickupIndex);
                }
            }

            if (step.getType() == MovePath.MoveStepType.DROP_CARGO) {
                Integer cargoLocation = step.getAdditionalData(MoveStep.CARGO_LOCATION_KEY);
                ICarryable cargo;

                // if we're not supplied a specific location, then the assumption is we only
                // have one
                // piece of cargo and we're going to just drop that one
                if (cargoLocation == null) {
                    cargo = entity.getDistinctCarriedObjects().get(0);
                } else {
                    cargo = entity.getCarriedObject(cargoLocation);
                }

                entity.dropGroundObject(cargo, isLastStep);
                boolean cargoDestroyed = false;

                if (!isLastStep) {
                    cargoDestroyed = gameManager.damageCargo(step.isFlying() || step.isJumping(), entity, cargo);
                }

                // note that this should not be moved into the "!isLastStep" block above
                // as cargo may be either unloaded peacefully or dumped on the move
                if (!cargoDestroyed) {
                    getGame().placeGroundObject(step.getPosition(), cargo);

                    r = new Report(2514);
                    r.subject = entity.getId();
                    r.add(entity.getDisplayName());
                    r.add(cargo.generalName());
                    r.add(step.getPosition().toFriendlyString());
                    addReport(r);

                    // a drop changes board state. Send an update for the overall ground object
                    // list.
                    gameManager.sendGroundObjectUpdate();
                }
            }

            // handle fighter recovery, and also DropShip docking with another large craft
            if (step.getType() == MovePath.MoveStepType.RECOVER) {

                loader = getGame().getEntity(step.getRecoveryUnit());
                boolean isDS = (entity instanceof Dropship);

                rollTarget = entity.getBasePilotingRoll(overallMoveType);
                if (loader.mpUsed > 0) {
                    rollTarget.addModifier(5, "carrier used thrust");
                }
                if (entity.getPartialRepairs().booleanOption("aero_collar_crit")) {
                    rollTarget.addModifier(2, "misrepaired docking collar");
                }
                if (isDS && (((Dropship) entity).getCollarType() == Dropship.COLLAR_PROTOTYPE)) {
                    rollTarget.addModifier(2, "prototype kf-boom");
                }
                Roll diceRoll = Compute.rollD6(2);

                if (isDS) {
                    r = new Report(9388);
                } else {
                    r = new Report(9381);
                }

                r.subject = entity.getId();
                r.add(entity.getDisplayName());
                r.add(loader.getDisplayName());
                r.add(rollTarget);
                r.add(diceRoll);
                r.newlines = 0;
                r.indent(1);

                if (diceRoll.getIntValue() < rollTarget.getValue()) {
                    r.choose(false);
                    addReport(r);
                    // damage unit
                    HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                    addReport(gameManager.damageEntity(entity, hit,
                            2 * (rollTarget.getValue() - diceRoll.getIntValue())));
                } else {
                    r.choose(true);
                    addReport(r);
                    recovered = true;
                }
                // check for door damage
                if (diceRoll.getIntValue() == 2) {
                    loader.damageDoorRecovery(entity);
                    r = new Report(9384);
                    r.subject = entity.getId();
                    r.indent(0);
                    r.add(loader.getDisplayName());
                    addReport(r);
                }
            }

            // handle fighter squadron joining
            if (step.getType() == MovePath.MoveStepType.JOIN) {
                loader = getGame().getEntity(step.getRecoveryUnit());
                recovered = true;
            }

            // Handle unloading units.
            if (step.getType() == MovePath.MoveStepType.UNLOAD) {
                Targetable unloaded = step.getTarget(getGame());
                Coords unloadPos = curPos;
                int unloadFacing = curFacing;
                if (null != step.getTargetPosition()) {
                    unloadPos = step.getTargetPosition();
                    unloadFacing = curPos.direction(unloadPos);
                }
                if (!gameManager.unloadUnit(entity, unloaded, unloadPos, unloadFacing,
                        step.getElevation())) {
                    logger.error("Server was told to unload "
                            + unloaded.getDisplayName() + " from "
                            + entity.getDisplayName() + " into "
                            + curPos.getBoardNum());
                }
                // some additional stuff to take care of for small
                // craft/DropShip unloading
                if ((entity instanceof SmallCraft) && (unloaded instanceof Entity)) {
                    Bay currentBay = entity.getBay((Entity) unloaded);
                    if ((null != currentBay) && (Compute.d6(2) == 2)) {
                        r = new Report(9390);
                        r.subject = entity.getId();
                        r.indent(1);
                        r.add(currentBay.getType());
                        addReport(r);
                        currentBay.destroyDoorNext();
                    }
                    // now apply any damage to bay doors
                    entity.resetBayDoors();
                    gameManager.entityUpdate(entity.getId());
                    // ok now add another turn for the transport so it can
                    // continue to unload units
                    if (!entity.getUnitsUnloadableFromBays().isEmpty()) {
                        dropshipStillUnloading = true;
                        GameTurn newTurn = new SpecificEntityTurn(
                                entity.getOwner().getId(), entity.getId());
                        // Need to set the new turn's multiTurn state
                        newTurn.setMultiTurn(true);
                        getGame().insertNextTurn(newTurn);
                    }
                    // ok add another turn for the unloaded entity so that it can move
                    if (!(unloaded instanceof Infantry)) {
                        GameTurn newTurn = new SpecificEntityTurn(
                                ((Entity) unloaded).getOwner().getId(),
                                ((Entity) unloaded).getId());
                        // Need to set the new turn's multiTurn state
                        newTurn.setMultiTurn(true);
                        getGame().insertNextTurn(newTurn);
                    }
                    // brief everybody on the turn update
                    gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
                }
            }

            // Handle disconnecting trailers.
            if (step.getType() == MovePath.MoveStepType.DISCONNECT) {
                Targetable unloaded = step.getTarget(getGame());
                Coords unloadPos = curPos;
                if (null != step.getTargetPosition()) {
                    unloadPos = step.getTargetPosition();
                }
                if (!gameManager.disconnectUnit(entity, unloaded, unloadPos)) {
                    logger.error(String.format(
                            "Server was told to disconnect %s from %s into %s",
                            unloaded.getDisplayName(), entity.getDisplayName(), curPos.getBoardNum()));
                }
            }

            // moving backwards over elevation change
            if (((step.getType() == MovePath.MoveStepType.BACKWARDS)
                    || (step.getType() == MovePath.MoveStepType.LATERAL_LEFT_BACKWARDS)
                    || (step.getType() == MovePath.MoveStepType.LATERAL_RIGHT_BACKWARDS))
                    && !(md.isJumping()
                            && (entity.getJumpType() == Mek.JUMP_BOOSTER))
                    && (lastHex.getLevel() + lastElevation != curHex.getLevel() + step.getElevation())
                    && !(entity instanceof VTOL)
                    && !(curClimbMode
                            && curHex.containsTerrain(Terrains.BRIDGE)
                            && ((curHex.terrainLevel(Terrains.BRIDGE_ELEV) + curHex.getLevel()) == (prevHex.getLevel()
                                    + (prevHex.containsTerrain(Terrains.BRIDGE)
                                            ? prevHex.terrainLevel(Terrains.BRIDGE_ELEV)
                                            : 0))))) {

                // per TacOps, if the mek is walking backwards over an elevation change and
                // falls
                // it falls into the lower hex. The caveat is if it already fell from some other
                // PSR in this
                // invocation of processMovement, then it can't fall again.
                if ((entity instanceof Mek)
                        && (curHex.getLevel() < getGame().getBoard().getHex(lastPos).getLevel())
                        && !entity.hasFallen()) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);
                    rollTarget.addModifier(0, "moving backwards over an elevation change");
                    gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(),
                            curPos, curPos, rollTarget, true);
                } else if ((entity instanceof Mek) && !entity.hasFallen()) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);
                    rollTarget.addModifier(0, "moving backwards over an elevation change");
                    gameManager.doSkillCheckWhileMoving(entity, lastElevation, lastPos, lastPos, rollTarget, true);
                } else if (entity instanceof Tank) {
                    rollTarget = entity.getBasePilotingRoll(overallMoveType);
                    rollTarget.addModifier(0, "moving backwards over an elevation change");
                    if (gameManager.doSkillCheckWhileMoving(entity, entity.getElevation(), curPos, lastPos,
                            rollTarget, false) < 0) {
                        curPos = lastPos;
                    }
                }
            }

            // Handle non-infantry moving into a building.
            if (buildingMove > 0) {
                // Get the building being exited.
                Building bldgExited = null;
                if ((buildingMove & 1) == 1) {
                    bldgExited = getGame().getBoard().getBuildingAt(lastPos);
                }

                // Get the building being entered.
                Building bldgEntered = null;
                if ((buildingMove & 2) == 2) {
                    bldgEntered = getGame().getBoard().getBuildingAt(curPos);
                }

                // ProtoMeks changing levels within a building cause damage
                if (((buildingMove & 8) == 8) && (entity instanceof ProtoMek)) {
                    Building bldg = getGame().getBoard().getBuildingAt(curPos);
                    Vector<Report> vBuildingReport = gameManager.damageBuilding(bldg, 1, curPos);
                    for (Report report : vBuildingReport) {
                        report.subject = entity.getId();
                    }
                    addReport(vBuildingReport);
                }

                boolean collapsed = false;
                if ((bldgEntered != null)) {
                    String reason;
                    if (bldgExited == null) {
                        // If we're not leaving a building, just handle the "entered".
                        reason = "entering";
                    } else if (bldgExited.equals(bldgEntered) && !(entity instanceof ProtoMek)
                            && !(entity instanceof Infantry)) {
                        // If we're moving within the same building, just handle the "within".
                        reason = "moving in";
                    } else {
                        // If we have different buildings, roll for each.
                        reason = "entering";
                    }

                    gameManager.passBuildingWall(entity, bldgEntered, lastPos, curPos, distance, reason,
                            step.isThisStepBackwards(), lastStepMoveType, true);
                    gameManager.addAffectedBldg(bldgEntered, collapsed);
                }

                // Clean up the entity if it has been destroyed.
                if (entity.isDoomed()) {
                    entity.setDestroyed(true);
                    getGame().moveToGraveyard(entity.getId());
                    gameManager.send(gameManager.createRemoveEntityPacket(entity.getId()));

                    // The entity's movement is completed.
                    return;
                }

                // TODO : what if a building collapses into rubble?
            }

            if (stepMoveType != EntityMovementType.MOVE_JUMP
                    && (step.getClearance() == 0
                            || (entity.getMovementMode().isWiGE() && (step.getClearance() == 1))
                            || curElevation == curHex.terrainLevel(Terrains.BLDG_ELEV)
                            || curElevation == curHex.terrainLevel(Terrains.BRIDGE_ELEV))) {
                Building bldg = getGame().getBoard().getBuildingAt(curPos);
                if ((bldg != null) && (entity.getElevation() >= 0)) {
                    boolean wigeFlyingOver = entity.getMovementMode() == EntityMovementMode.WIGE
                            && ((curHex.containsTerrain(Terrains.BLDG_ELEV)
                                    && curElevation > curHex.terrainLevel(Terrains.BLDG_ELEV)) ||
                                    (curHex.containsTerrain(Terrains.BRIDGE_ELEV)
                                            && curElevation > curHex.terrainLevel(Terrains.BRIDGE_ELEV)));
                    boolean collapse = gameManager.checkBuildingCollapseWhileMoving(bldg, entity, curPos);
                    gameManager.addAffectedBldg(bldg, collapse);
                    // If the building is collapsed by a WiGE flying over it, the WiGE drops one
                    // level of elevation.
                    // This could invalidate the remainder of the movement path, so we will send it
                    // back to the client.
                    if (collapse && wigeFlyingOver) {
                        curElevation--;
                        r = new Report(2378);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        addReport(r);
                        continueTurnFromLevelDrop = true;
                        entity.setPosition(curPos);
                        entity.setFacing(curFacing);
                        entity.setSecondaryFacing(curFacing);
                        entity.setElevation(curElevation);
                        break;
                    }
                }
            }

            // Sheer Cliffs, TO p.39
            boolean vehicleAffectedByCliff = entity instanceof Tank
                    && !entity.isAirborneVTOLorWIGE();
            boolean quadveeVehMode = entity instanceof QuadVee
                    && ((QuadVee) entity).getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
            boolean mekAffectedByCliff = (entity instanceof Mek || entity instanceof ProtoMek)
                    && moveType != EntityMovementType.MOVE_JUMP
                    && !entity.isAero();
            // Cliffs should only exist towards 1 or 2 level drops, check just to make sure
            // Everything that does not have a 1 or 2 level drop shouldn't be handled as a
            // cliff
            int stepHeight = curElevation + curHex.getLevel()
                    - (lastElevation + prevHex.getLevel());
            boolean isUpCliff = !lastPos.equals(curPos)
                    && curHex.hasCliffTopTowards(prevHex)
                    && (stepHeight == 1 || stepHeight == 2);
            boolean isDownCliff = !lastPos.equals(curPos)
                    && prevHex.hasCliffTopTowards(curHex)
                    && (stepHeight == -1 || stepHeight == -2);

            // Vehicles (exc. WIGE/VTOL) moving down a cliff
            if (vehicleAffectedByCliff && isDownCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(stepMoveType);
                rollTarget.append(new PilotingRollData(entity.getId(), 0, "moving down a sheer cliff"));
                if (gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                        lastPos, curPos, rollTarget, false) > 0) {
                    addReport(gameManager.vehicleMotiveDamage((Tank) entity, 0));
                    addNewLines();
                    turnOver = true;
                    break;
                }
            }

            // Meks and Protomeks moving down a cliff
            // Quadvees in vee mode ignore PSRs to avoid falls, IO p.133
            if (mekAffectedByCliff && !quadveeVehMode && isDownCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), -stepHeight - 1, "moving down a sheer cliff"));
                if (gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                        lastPos, curPos, rollTarget, true) > 0) {
                    addNewLines();
                    turnOver = true;
                    break;
                }
            }

            // Meks moving up a cliff
            if (mekAffectedByCliff && !quadveeVehMode && isUpCliff && !isPavementStep) {
                rollTarget = entity.getBasePilotingRoll(moveType);
                rollTarget.append(new PilotingRollData(entity.getId(), stepHeight, "moving up a sheer cliff"));
                if (gameManager.doSkillCheckWhileMoving(entity, lastElevation,
                        lastPos, lastPos, rollTarget, false) > 0) {
                    r = new Report(2209);
                    r.addDesc(entity);
                    r.subject = entity.getId();
                    addReport(r);
                    addNewLines();
                    curPos = entity.getPosition();
                    mpUsed = step.getMpUsed();
                    continueTurnFromCliffAscent = true;
                    break;
                }
            }

            // did the entity just fall?
            if (!wasProne && entity.isProne()) {
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }

            // dropping prone intentionally?
            if (step.getType() == MovePath.MoveStepType.GO_PRONE) {
                mpUsed = step.getMpUsed();
                rollTarget = entity.checkDislodgeSwarmers(step, overallMoveType);
                if (rollTarget.getValue() == TargetRoll.CHECK_FALSE) {
                    // Not being swarmed
                    entity.setProne(true);
                    // check to see if we washed off infernos
                    gameManager.checkForWashedInfernos(entity, curPos);
                } else {
                    // Being swarmed
                    entity.setPosition(curPos);
                    if (gameManager.doDislodgeSwarmerSkillCheck(entity, rollTarget, curPos)) {
                        // Entity falls
                        curFacing = entity.getFacing();
                        curPos = entity.getPosition();
                        fellDuringMovement = true;
                        break;
                    }
                    // roll failed, go prone but don't dislodge swarmers
                    entity.setProne(true);
                    // check to see if we washed off infernos
                    gameManager.checkForWashedInfernos(entity, curPos);
                    break;
                }
            }

            // going hull down
            if (step.getType() == MovePath.MoveStepType.HULL_DOWN) {
                mpUsed = step.getMpUsed();
                entity.setHullDown(true);
            }

            // Check for crushing buildings by Dropships/Mobile Structures
            for (Coords pos : step.getCrushedBuildingLocs()) {
                Building bldg = getGame().getBoard().getBuildingAt(pos);
                Hex hex = getGame().getBoard().getHex(pos);

                r = new Report(3443);
                r.subject = entity.getId();
                r.addDesc(entity);
                r.add(bldg.getName());
                gameManager.getvPhaseReport().add(r);

                final int cf = bldg.getCurrentCF(pos);
                final int numFloors = Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
                gameManager.getvPhaseReport().addAll(gameManager.damageBuilding(bldg, 150, " is crushed for ", pos));
                int damage = (int) Math.round((cf / 10.0) * numFloors);
                HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                gameManager.getvPhaseReport().addAll(gameManager.damageEntity(entity, hit, damage));
            }

            // Track this step's location.
            movePath.addElement(new UnitLocation(entity.getId(), curPos,
                    curFacing, step.getElevation()));

            // if the lastpos is not the same as the current position
            // then add the current position to the list of places passed
            // through
            if (!curPos.equals(lastPos)) {
                passedThrough.add(curPos);
                passedThroughFacing.add(curFacing);
            }

            // update lastPos, prevStep, prevFacing & prevHex
            if (!curPos.equals(lastPos)) {
                prevFacing = curFacing;
            }
            lastPos = curPos;
            lastElevation = curElevation;
            prevStep = step;
            prevHex = curHex;

            firstStep = false;

            // if we moved at all, we are no longer bracing "for free", except for when
            // the current step IS bracing
            if ((mpUsed > 0) && (step.getType() != MovePath.MoveStepType.BRACE)) {
                entity.setBraceLocation(Entity.LOC_NONE);
            }
        }

    }
}
