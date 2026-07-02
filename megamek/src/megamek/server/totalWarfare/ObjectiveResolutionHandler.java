/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.victory.ScanTally;
import megamek.server.victory.VictoryPointTracker;

/**
 * End-Phase resolution for objective markers (Standard Missions, Objectives): determines which side controls each
 * {@link ObjectiveMarker} on the board and awards Victory Points into the game's {@link VictoryPointTracker} per the
 * standard control mission scoring.
 *
 * <P>Control: a side controls an objective when it has strictly more eligible units within the objective's control
 * radius than any other side. Crippled, prone, immobile and transported units do not count, and flying units
 * (airborne aerospace units, VTOLs and WiGEs at altitude) cannot control - grounded units count normally. A tie, or
 * no units in range, leaves the objective uncontrolled.</P>
 *
 * <P>Scoring (standard control mission, played with four counters, two per side): each End Phase a side receives 1 VP
 * for controlling at least one friendly and at least one enemy objective, and 2 VP for controlling all objectives on
 * the board. Other mission scoring rules (Objective Raid, Sensor Check) resolve in later implementation phases.</P>
 *
 * <P>Destruction: an objective counter inside a building rides that building - when the building hex is destroyed,
 * a destructible objective is destroyed with it. Objectives cannot be destroyed unless the mission allows it
 * (scenario key {@code destructible: true}); destroyed objectives no longer score.</P>
 *
 * <P>Scanning: each End Phase, every unit may scan one target within scanning range (2 hexes, or the range of a
 * working active probe not negated by ECM) and line of sight - a sensor check (a Piloting Skill Roll ignoring
 * ordinary PSR modifiers) with a +3 TN modifier. Confirming an unconfirmed Potential Objective candidate takes
 * priority: on a successful scan, a 1D6 of {@value #CONFIRMATION_MINIMUM_ROLL}+ confirms it as a real objective,
 * otherwise it is useless and removed from the battlefield. With the
 * {@link OptionsConstants#VICTORY_USE_SENSOR_CHECK} game option (Sensor Check mission), units otherwise scan enemy
 * units; successful scans are banked in the game's {@link ScanTally} (each enemy unit once per scanner) and convert
 * to 1 VP each when the scanner exfiltrates - flees the board - from round {@value #EXFILTRATION_EARLIEST_ROUND} on;
 * fleeing earlier forfeits the banked scans.</P>
 *
 * <P>Sides are teams; a player without a team forms its own side.</P>
 */
class ObjectiveResolutionHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveResolutionHandler.class);

    private static final int REPORT_OBJECTIVE_CONTROLLED = 7117;
    private static final int REPORT_OBJECTIVE_UNCONTROLLED = 7118;
    private static final int REPORT_OBJECTIVE_POINTS_AWARDED = 7119;
    private static final int REPORT_OBJECTIVE_DESTROYED = 7120;
    private static final int REPORT_SCAN_SUCCESS = 7121;
    private static final int REPORT_SCAN_POINTS_AWARDED = 7122;
    private static final int REPORT_SCANS_LOST = 7123;

    // Scan check numbers per Standard Missions, Objectives - Scanning: a scan is a sensor check (a Piloting
    // Skill Roll ignoring ordinary PSR modifiers, p.113) with a +3 TN modifier; +2 for one sensor critical
    // hit (two prevent sensor checks entirely); -1 per active probe level (Light 1, Beagle-class 2,
    // Bloodhound 3) when not negated by hostile ECM; +2 against a target with an active stealth system.
    static final int SCAN_MODIFIER = 3;
    static final int SCAN_SENSOR_CRITICAL_MODIFIER = 2;
    static final int SCAN_BLOCKING_SENSOR_CRITICAL_HITS = 2;
    static final int SCAN_STEALTH_MODIFIER = 2;
    static final int PROBE_LEVEL_LIGHT = 1;
    static final int PROBE_LEVEL_STANDARD = 2;
    static final int PROBE_LEVEL_BLOODHOUND = 3;
    static final int DEFAULT_SCANNING_RANGE = 2;
    static final int EXFILTRATION_EARLIEST_ROUND = 5;
    // Potential Objectives: after a successful scan, 1D6 of 4+ confirms the candidate; otherwise it is
    // useless and removed from the battlefield
    static final int CONFIRMATION_MINIMUM_ROLL = 4;
    // Fragile Objectives: on a qualifying event, 1D6 of 1-4 destroys the objective
    static final int FRAGILE_DESTRUCTION_MAXIMUM_ROLL = 4;

    private static final int REPORT_OBJECTIVE_CONFIRMED = 7124;
    private static final int REPORT_OBJECTIVE_USELESS = 7125;
    private static final int REPORT_OBJECTIVE_DROPPED = 7126;
    private static final int REPORT_FORCED_DROP_CHECK = 7127;
    private static final int REPORT_OBJECTIVE_DISPLACED = 7130;

    // safety bound for the same-direction stacking displacement chain
    private static final int MAXIMUM_DISPLACEMENT_CHAIN = 20;

    /**
     * A scoring side. Normally this is a team; a player that is not on any team forms its own side.
     *
     * @param isTeam {@code true} when {@code id} is a team ID, {@code false} when it is a player ID
     * @param id     The team or player ID
     */
    record Side(boolean isTeam, int id) {}

    /**
     * An objective marker together with its board position: the key of the game's ground object map, or the
     * position of the unit carrying it (Mobile Objectives).
     *
     * @param position The board position of the marker
     * @param marker   The objective marker
     * @param carrier  The unit carrying the marker, or {@code null} when it lies on the ground
     */
    record PlacedObjective(Coords position, ObjectiveMarker marker, @Nullable Entity carrier) {
        PlacedObjective(Coords position, ObjectiveMarker marker) {
            this(position, marker, null);
        }
    }

    /**
     * The control resolution of one objective for one End Phase.
     *
     * @param placed     The objective and its position
     * @param owningSide The side that owns (placed) the objective, or {@code null} when the owner is unknown
     * @param controller The side controlling the objective this End Phase, or {@code null} when uncontrolled
     */
    record ResolvedObjective(PlacedObjective placed, @Nullable Side owningSide, @Nullable Side controller) {}

    ObjectiveResolutionHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Resolves objectives for the current End Phase: resolves the Sensor Check scan mission (when enabled), syncs
     * objective destruction (an objective inside a destroyed building is destroyed with it), then determines the
     * controller of every surviving objective marker, reports the results and awards Victory Points per the standard
     * control scoring. Does nothing when the game has neither the scan mission nor objective markers.
     */
    void resolveObjectives() {
        resolveScanMission();
        resolveCarriedObjectiveDrops();

        List<PlacedObjective> allObjectives = findAllObjectives();
        if (allObjectives.isEmpty()) {
            return;
        }

        syncObjectiveDestruction(allObjectives);

        List<PlacedObjective> activeObjectives = allObjectives.stream()
              .filter(this::isScorableObjective)
              .toList();
        if (activeObjectives.isEmpty()) {
            LOGGER.debug("[Objective] No scorable objectives (destroyed or unconfirmed candidates) - no control "
                  + "resolution");
            return;
        }

        List<Entity> entities = getGame().getEntitiesVector();
        List<ResolvedObjective> resolvedObjectives = new ArrayList<>();
        for (PlacedObjective objective : activeObjectives) {
            Side controller = determineControllingSide(objective, entities);
            Side owningSide = sideOfPlayerId(objective.marker().getOwnerId());
            resolvedObjectives.add(new ResolvedObjective(objective, owningSide, controller));
            storeControllerOnMarker(objective.marker(), controller);
            reportObjectiveControl(objective, controller);
        }
        awardStandardControlVictoryPoints(resolvedObjectives, VictoryPointTracker.getTracker(getGame()));
    }

    /**
     * Checks whether an objective participates in control resolution and scoring: destroyed objectives and
     * unconfirmed Potential Objective candidates do not (RAW: only confirmed objectives are worth Victory Points).
     * A False Objective flag has no effect with a running VP score (RAW: the variant is not used in such missions).
     */
    private boolean isScorableObjective(PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (marker.isDestroyed()) {
            return false;
        }
        if (marker.isPotential() && !marker.isConfirmed()) {
            LOGGER.debug("[Objective] {} at {} is an unconfirmed objective candidate - it cannot score until "
                  + "confirmed by a scan", marker.generalName(), objective.position());
            return false;
        }
        if (marker.isFalseObjective()) {
            LOGGER.debug("[Objective] {} at {} is flagged as a False Objective, but the variant is not used with "
                        + "a running VP score - the flag has no effect",
                  marker.generalName(), objective.position());
        }
        return true;
    }

    /**
     * @return All objective markers, including destroyed ones: those placed on the ground with their map positions,
     *       and carried Mobile Objectives at their carrier's position
     */
    private List<PlacedObjective> findAllObjectives() {
        List<PlacedObjective> objectives = new ArrayList<>();
        for (Map.Entry<Coords, List<ICarryable>> groundObjectEntry : getGame().getGroundObjects().entrySet()) {
            for (ICarryable groundObject : groundObjectEntry.getValue()) {
                if (groundObject instanceof ObjectiveMarker marker) {
                    objectives.add(new PlacedObjective(groundObjectEntry.getKey(), marker));
                }
            }
        }
        for (Entity entity : getGame().getEntitiesVector()) {
            for (ICarryable carriedObject : entity.getDistinctCarriedObjects()) {
                if ((carriedObject instanceof ObjectiveMarker marker) && (entity.getPosition() != null)) {
                    objectives.add(new PlacedObjective(entity.getPosition(), marker, entity));
                }
            }
        }
        return objectives;
    }

    /**
     * Syncs objective destruction: detects on the first pass which objectives sit inside a building, destroys
     * destructible objectives whose building hex has since been destroyed (an objective counter rides its building),
     * and reports each destroyed objective exactly once, regardless of how it was destroyed.
     *
     * @param objectives All objective markers, including already destroyed ones
     */
    private void syncObjectiveDestruction(List<PlacedObjective> objectives) {
        // Ground objects live on the game's main board (ID 0); null in board-less setups
        Board board = getGame().getBoard();
        for (PlacedObjective objective : objectives) {
            if (board != null) {
                if (objective.carrier() == null) {
                    checkBuildingDestruction(board, objective);
                } else {
                    // A carried objective rides no building; re-detect the link when it is set down
                    objective.marker().setBuildingLinkInitialized(false);
                    objective.marker().setInsideBuilding(false);
                }
                // The fire check applies wherever the objective is - on the ground or in a carrier's hex
                checkFragileFireDestruction(board, objective);
            }
            reportDestructionOnce(objective);
        }
    }

    /**
     * Fragile Objectives: an objective in a burning hex risks destruction, checked in the End Phase - roll 1D6, on
     * 1-{@value #FRAGILE_DESTRUCTION_MAXIMUM_ROLL} the objective is destroyed. This runs before control resolution,
     * so an objective destroyed this way is destroyed before it provides the VP it would have granted this turn
     * (RAW). The other Fragile triggers (forced drops, falling or destroyed carriers, heat-causing weapon hits,
     * area-effect damage, forced level changes) resolve with the Mobile Objective carry rules in a later phase.
     */
    private void checkFragileFireDestruction(Board board, PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (!marker.isFragile() || marker.isDestroyed() || marker.isInvulnerable()) {
            return;
        }
        Hex hex = board.getHex(objective.position());
        if ((hex == null) || !hex.containsTerrain(Terrains.FIRE)) {
            return;
        }
        int roll = rollFragileCheck();
        boolean destroyed = roll <= FRAGILE_DESTRUCTION_MAXIMUM_ROLL;
        LOGGER.info("[Objective] Fragile objective {} at {} is in a burning hex: rolled {} - {}",
              marker.generalName(), objective.position(), roll, destroyed ? "destroyed" : "survives");
        if (destroyed) {
            marker.setDestroyed(true);
        }
    }

    private void checkBuildingDestruction(Board board, PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        initializeBuildingLink(board, objective);
        boolean ridesDestroyedBuilding = !marker.isDestroyed() && marker.isInsideBuilding()
              && isBuildingHexGone(board, objective.position());
        if (!ridesDestroyedBuilding) {
            return;
        }
        if (marker.isInvulnerable()) {
            LOGGER.debug("[Objective] {} at {}: its building was destroyed, but objectives cannot be destroyed "
                  + "in this mission", marker.generalName(), objective.position());
            return;
        }
        marker.setDestroyed(true);
        LOGGER.info("[Objective] {} at {}: destroyed with its building", marker.generalName(), objective.position());
    }

    /**
     * Detects once, on the first End Phase, whether the objective sits inside a building. From then on the objective
     * rides that building: when the building hex is destroyed, so is a destructible objective.
     */
    private void initializeBuildingLink(Board board, PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (marker.isBuildingLinkInitialized()) {
            return;
        }
        boolean insideBuilding = board.getBuildingAt(objective.position()) != null;
        marker.setInsideBuilding(insideBuilding);
        marker.setBuildingLinkInitialized(true);
        if (insideBuilding) {
            LOGGER.debug("[Objective] {} at {} sits inside a building and is destroyed with it (if destructible)",
                  marker.generalName(), objective.position());
        }
    }

    private boolean isBuildingHexGone(Board board, Coords position) {
        IBuilding building = board.getBuildingAt(position);
        return (building == null) || (building.getCurrentCF(position) <= 0);
    }

    // --- Mobile Objective carrying ---

    /**
     * End-Phase auto-drops for carried Mobile Objectives: a carrier that has become immobile drops its objective
     * (with the Fragile destruction roll), and a carrier that went prone by choice drops it without a Fragile roll
     * (carriers that fell already dropped their cargo when the fall was resolved).
     */
    private void resolveCarriedObjectiveDrops() {
        for (Entity carrier : getGame().getEntitiesVector()) {
            for (ICarryable carriedObject : List.copyOf(carrier.getDistinctCarriedObjects())) {
                if (!(carriedObject instanceof ObjectiveMarker marker)) {
                    continue;
                }
                if (carrier.isImmobile()) {
                    LOGGER.info("[Objective] {} is immobile and drops {}", carrier.getShortName(),
                          marker.generalName());
                    dropObjective(carrier, marker, true);
                } else if (carrier.isProne()) {
                    // still carrying while prone at the End Phase = went prone by choice; the drop does
                    // not trigger a Fragile Objective roll
                    LOGGER.info("[Objective] {} is prone and drops {} (no Fragile roll for going prone "
                          + "by choice)", carrier.getShortName(), marker.generalName());
                    dropObjective(carrier, marker, false);
                }
            }
        }
    }

    /**
     * Forced-drop check after a phase with damage: a unit that took any damage in the phase while carrying a Mobile
     * Objective must make a Piloting Skill Roll at the end of that phase to avoid dropping it. Failure does not
     * cause a fall. A Mek with two intact hand actuators applies a -2 target number modifier (two claws do not).
     * Called at the end of each combat phase, alongside the damage PSR checks.
     */
    void resolveForcedObjectiveDrops() {
        for (Entity carrier : getGame().getEntitiesVector()) {
            if (carrier.damageThisPhase <= 0) {
                continue;
            }
            List<ObjectiveMarker> carriedMarkers = new ArrayList<>();
            for (ICarryable carriedObject : carrier.getDistinctCarriedObjects()) {
                if (carriedObject instanceof ObjectiveMarker marker) {
                    carriedMarkers.add(marker);
                }
            }
            if (carriedMarkers.isEmpty()) {
                continue;
            }
            resolveForcedDropCheck(carrier, carriedMarkers);
        }
    }

    private void resolveForcedDropCheck(Entity carrier, List<ObjectiveMarker> carriedMarkers) {
        PilotingRollData rollData = carrier.getBasePilotingRoll();
        if (hasTwoIntactHandActuators(carrier)) {
            rollData.addModifier(-2, "two intact hand actuators");
        }
        int targetNumber = rollData.getValue();
        int roll = rollForcedDropCheck();
        boolean holdsOn = roll >= targetNumber;
        LOGGER.debug("[Objective] {} took damage while carrying an objective - forced-drop Piloting Skill Roll: "
              + "needs {}, rolled {} - {}", carrier.getShortName(), targetNumber, roll,
              holdsOn ? "keeps hold" : "drops");

        Report report = new Report(REPORT_FORCED_DROP_CHECK, Report.PUBLIC);
        report.add(carrier.getDisplayName());
        report.add(targetNumber);
        report.add(roll);
        report.choose(holdsOn);
        addReport(report);

        if (!holdsOn) {
            for (ObjectiveMarker marker : carriedMarkers) {
                // a forced drop is a Fragile Objective destruction trigger
                dropObjective(carrier, marker, true);
            }
        }
    }

    /**
     * @return {@code true} if the unit is a Mek with a working hand actuator in both arms; claws function as hand
     *       actuators for carrying, but two claws do not provide the forced-drop bonus
     */
    private boolean hasTwoIntactHandActuators(Entity carrier) {
        return (carrier instanceof Mek)
              && carrier.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)
              && carrier.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM);
    }

    /**
     * Drops a carried Mobile Objective into the carrier's hex, rolling the Fragile Objective destruction check when
     * the drop is a Fragile trigger, and updates the clients.
     *
     * @param carrier            The unit carrying the objective
     * @param marker             The carried objective
     * @param fragileRollApplies Whether this drop triggers the Fragile destruction roll (forced drops do; setting
     *                           down or going prone by choice does not)
     */
    private void dropObjective(Entity carrier, ObjectiveMarker marker, boolean fragileRollApplies) {
        carrier.dropCarriedObject(marker, false);
        if (fragileRollApplies) {
            rollFragileEventDestruction(marker, "dropped");
        }
        if (carrier.getPosition() != null) {
            placeDroppedObjective(carrier, carrier.getPosition(), marker);
        }
        gameManager.sendGroundObjectUpdate();
        Report report = new Report(REPORT_OBJECTIVE_DROPPED, Report.PUBLIC);
        report.add(carrier.getDisplayName());
        report.add(marker.generalName());
        addReport(report);
    }

    /**
     * Resolves whether a dropped or carrier-lost objective is destroyed: never, unless it is a Fragile Objective -
     * then 1D6, destroyed on 1-{@value #FRAGILE_DESTRUCTION_MAXIMUM_ROLL}. Called for objective markers instead of
     * the generic cargo destruction roll when a carrier is destroyed or falls.
     *
     * @param carrier The carrier dropping the objective (for reporting)
     * @param marker  The objective being dropped
     *
     * @return {@code true} if the objective is destroyed by the drop
     */
    boolean resolveObjectiveDropDamage(Entity carrier, ObjectiveMarker marker) {
        boolean destroyed = rollFragileEventDestruction(marker, "carrier fell or was destroyed");
        if (destroyed) {
            reportDestructionOnce(new PlacedObjective(carrier.getPosition(), marker, carrier));
        }
        return destroyed;
    }

    /**
     * Rolls the Fragile Objective destruction check for a qualifying event, marking the objective destroyed on
     * 1-{@value #FRAGILE_DESTRUCTION_MAXIMUM_ROLL}. Non-Fragile and indestructible objectives always survive.
     *
     * @param marker           The objective at risk
     * @param eventDescription The triggering event, for the log
     *
     * @return {@code true} if the objective was destroyed
     */
    private boolean rollFragileEventDestruction(ObjectiveMarker marker, String eventDescription) {
        if (!marker.isFragile() || marker.isInvulnerable() || marker.isDestroyed()) {
            return marker.isDestroyed();
        }
        int roll = rollFragileCheck();
        boolean destroyed = roll <= FRAGILE_DESTRUCTION_MAXIMUM_ROLL;
        LOGGER.info("[Objective] Fragile objective {} takes a destruction roll ({}): rolled {} - {}",
              marker.generalName(), eventDescription, roll, destroyed ? "destroyed" : "survives");
        if (destroyed) {
            marker.setDestroyed(true);
        }
        return destroyed;
    }

    /**
     * Fragile Objective check for every objective lying in the given hex, triggered by area-effect damage or a
     * heat-causing weapon hitting the hex (RAW triggers, rolled at the time of the event). Destroyed objectives are
     * reported immediately.
     *
     * @param position         The hex that was hit
     * @param eventDescription The triggering event, for the log
     */
    void checkFragileObjectivesInHex(Coords position, String eventDescription) {
        for (ICarryable groundObject : List.copyOf(getGame().getGroundObjects(position))) {
            if (groundObject instanceof ObjectiveMarker marker) {
                boolean destroyed = rollFragileEventDestruction(marker, eventDescription);
                if (destroyed) {
                    reportDestructionOnce(new PlacedObjective(position, marker));
                }
            }
        }
    }

    // --- Objective stacking (one objective per hex) ---

    /**
     * Places a dropped objective in the given hex, enforcing the stacking rule: only one objective can be in a
     * single hex. An objective already in the hex is displaced one hex in the direction of the dropping unit's
     * facing (chaining in the same direction while hexes are occupied); a displacement off the battlefield leaves
     * the objective in its last in-play hex, and a forced level change on a Fragile objective triggers its
     * destruction roll.
     *
     * @param droppingUnit The unit dropping the objective (its facing sets the displacement direction)
     * @param position     The hex the objective is dropped into
     * @param droppedMarker The objective being dropped
     */
    void placeDroppedObjective(Entity droppingUnit, Coords position, ObjectiveMarker droppedMarker) {
        ObjectiveMarker displacedMarker = findOtherObjectiveAt(position, droppedMarker);
        getGame().placeGroundObject(position, droppedMarker);
        if (displacedMarker != null) {
            displaceObjective(displacedMarker, position, droppingUnit.getFacing(), 0);
        }
    }

    @Nullable
    private ObjectiveMarker findOtherObjectiveAt(Coords position, ObjectiveMarker excludedMarker) {
        for (ICarryable groundObject : getGame().getGroundObjects(position)) {
            if ((groundObject instanceof ObjectiveMarker marker) && (marker != excludedMarker)) {
                return marker;
            }
        }
        return null;
    }

    private void displaceObjective(ObjectiveMarker marker, Coords from, int direction, int chainDepth) {
        if ((direction < 0) || (chainDepth > MAXIMUM_DISPLACEMENT_CHAIN)) {
            LOGGER.warn("[Objective] {} at {} cannot be displaced (direction {} / chain depth {}) - it stays, "
                        + "sharing the hex", marker.generalName(), from, direction, chainDepth);
            return;
        }
        Board board = getGame().getBoard();
        Coords destination = from.translated(direction);
        if ((board == null) || !board.contains(destination)) {
            // RAW: an objective displaced off the battlefield is placed in the last in-play hex occupied
            LOGGER.info("[Objective] {} would be displaced off the battlefield - it stays in its last in-play "
                  + "hex {}", marker.generalName(), from);
            return;
        }

        ObjectiveMarker nextDisplacedMarker = findOtherObjectiveAt(destination, marker);
        getGame().removeGroundObject(from, marker);
        getGame().placeGroundObject(destination, marker);
        LOGGER.info("[Objective] {} is displaced from {} to {} (stacking - one objective per hex)",
              marker.generalName(), from, destination);
        Report report = new Report(REPORT_OBJECTIVE_DISPLACED, Report.PUBLIC);
        report.add(marker.generalName());
        report.add(destination.toFriendlyString());
        addReport(report);

        Hex fromHex = board.getHex(from);
        Hex destinationHex = board.getHex(destination);
        boolean levelChanged = (fromHex != null) && (destinationHex != null)
              && (fromHex.getLevel() != destinationHex.getLevel());
        if (levelChanged) {
            boolean destroyed = rollFragileEventDestruction(marker, "forced level change from displacement");
            if (destroyed) {
                reportDestructionOnce(new PlacedObjective(destination, marker));
            }
        }

        if (nextDisplacedMarker != null) {
            displaceObjective(nextDisplacedMarker, destination, direction, chainDepth + 1);
        }
    }

    private void reportDestructionOnce(PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (!marker.isDestroyed() || marker.isDestructionProcessed()) {
            return;
        }
        marker.setDestructionProcessed(true);
        Report report = new Report(REPORT_OBJECTIVE_DESTROYED, Report.PUBLIC);
        report.add(marker.generalName());
        report.add(objective.position().toFriendlyString());
        addReport(report);
        LOGGER.info("[Objective] {} at {} has been destroyed", marker.generalName(), objective.position());
    }

    /**
     * Determines the side controlling the given objective: the side with strictly more eligible units within the
     * objective's control radius than any other side.
     *
     * @param objective The objective to evaluate
     * @param entities  The game's entities to consider
     *
     * @return The controlling side, or {@code null} when the objective is uncontrolled (tie or no units in range)
     */
    @Nullable
    Side determineControllingSide(PlacedObjective objective, List<Entity> entities) {
        // A unit carrying a Mobile Objective automatically controls it, regardless of how many enemy
        // units are nearby or what the objective's control radius normally is
        if (objective.carrier() != null) {
            Side carrierSide = sideOfPlayer(objective.carrier().getOwner());
            LOGGER.debug("[Objective] {} is carried by {} - automatically controlled by {}",
                  objective.marker().generalName(), objective.carrier().getShortName(),
                  (carrierSide == null) ? "no one (ownerless carrier)" : displayName(carrierSide));
            return carrierSide;
        }

        Map<Side, Integer> unitCountsBySide = new HashMap<>();
        for (Entity entity : entities) {
            if (!isEligibleToControl(entity, objective)) {
                continue;
            }
            Side side = sideOfPlayer(entity.getOwner());
            if (side != null) {
                unitCountsBySide.merge(side, 1, Integer::sum);
            }
        }

        Side leadingSide = null;
        int leadingCount = 0;
        boolean tie = false;
        for (Map.Entry<Side, Integer> countEntry : unitCountsBySide.entrySet()) {
            if (countEntry.getValue() > leadingCount) {
                leadingSide = countEntry.getKey();
                leadingCount = countEntry.getValue();
                tie = false;
            } else if (countEntry.getValue() == leadingCount) {
                tie = true;
            }
        }

        if (leadingSide == null) {
            LOGGER.debug("[Objective] {} at {}: uncontrolled - no eligible units in control radius {}",
                  objective.marker().generalName(), objective.position(), objective.marker().getControlRadius());
            return null;
        }
        if (tie) {
            LOGGER.debug("[Objective] {} at {}: uncontrolled - tied unit counts {}",
                  objective.marker().generalName(), objective.position(), unitCountsBySide);
            return null;
        }
        LOGGER.debug("[Objective] {} at {}: controlled by {} with unit counts {}",
              objective.marker().generalName(), objective.position(), displayName(leadingSide), unitCountsBySide);
        return leadingSide;
    }

    /**
     * Checks whether a unit counts toward controlling the given objective. Only deployed, on-board units within the
     * control radius count; crippled, prone, immobile and transported units are excluded, and flying units cannot
     * control (grounded units count normally).
     *
     * @param entity    The unit to check
     * @param objective The objective being evaluated
     *
     * @return {@code true} if the unit counts toward control of the objective
     */
    boolean isEligibleToControl(Entity entity, PlacedObjective objective) {
        Coords entityPosition = entity.getPosition();
        boolean isOnBoard = (entityPosition != null) && entity.isDeployed() && !entity.isOffBoard()
              && !entity.isDestroyed();
        if (!isOnBoard) {
            return false;
        }
        if (entityPosition.distance(objective.position()) > objective.marker().getControlRadius()) {
            return false;
        }
        // The unit is in range; log the excluded ones so a playtest can tell why a unit does not count.
        // TRACE because this runs in a loop over all entities each End Phase.
        if (entity.getTransportId() != Entity.NONE) {
            LOGGER.trace("[Objective] {} does not count for {}: being transported",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isCrippled()) {
            LOGGER.trace("[Objective] {} does not count for {}: crippled",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isProne()) {
            LOGGER.trace("[Objective] {} does not count for {}: prone",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isImmobile()) {
            LOGGER.trace("[Objective] {} does not count for {}: immobile",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            LOGGER.trace("[Objective] {} does not count for {}: flying units cannot control objectives",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.getMovementMode().isVTOL()) {
            // RAW (Control Radius - Assets): air and VTOL vehicle Assets can never control objectives,
            // even when landed
            LOGGER.trace("[Objective] {} does not count for {}: VTOL units cannot control objectives",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        return true;
    }

    /**
     * Awards Victory Points per the standard control mission scoring: each End Phase, a side receives 1 VP for
     * controlling at least one friendly and at least one enemy objective, and 2 VP for controlling all objectives on
     * the board (with more than one objective in play).
     *
     * @param resolvedObjectives The control resolution of all active objectives this End Phase
     * @param tracker            The victory point tracker to award into
     */
    void awardStandardControlVictoryPoints(List<ResolvedObjective> resolvedObjectives, VictoryPointTracker tracker) {
        Set<Side> controllers = new LinkedHashSet<>();
        for (ResolvedObjective resolvedObjective : resolvedObjectives) {
            if (resolvedObjective.controller() != null) {
                controllers.add(resolvedObjective.controller());
            }
        }
        if (controllers.isEmpty()) {
            LOGGER.debug("[Objective] No objective is controlled this round - no victory points awarded");
            return;
        }

        int totalObjectives = resolvedObjectives.size();
        for (Side side : controllers) {
            int controlledFriendly = 0;
            int controlledEnemy = 0;
            for (ResolvedObjective resolvedObjective : resolvedObjectives) {
                if (!side.equals(resolvedObjective.controller())) {
                    continue;
                }
                // An objective with an unknown owner is not friendly to anyone and counts as enemy
                if (side.equals(resolvedObjective.owningSide())) {
                    controlledFriendly++;
                } else {
                    controlledEnemy++;
                }
            }

            int points = 0;
            String reason = "";
            if ((controlledFriendly + controlledEnemy == totalObjectives) && (totalObjectives > 1)) {
                points = 2;
                reason = "controls all " + totalObjectives + " objectives";
            } else if ((controlledFriendly >= 1) && (controlledEnemy >= 1)) {
                points = 1;
                reason = "controls " + controlledFriendly + " friendly and " + controlledEnemy
                      + " enemy objective(s)";
            }

            if (points > 0) {
                awardVictoryPoints(side, points, reason, tracker);
            } else {
                LOGGER.debug("[Objective] {} controls friendly: {}, enemy: {} objective(s) - standard control "
                            + "scoring requires at least one of each, no victory points awarded",
                      displayName(side), controlledFriendly, controlledEnemy);
            }
        }
    }

    private void awardVictoryPoints(Side side, int points, String reason, VictoryPointTracker tracker) {
        creditVictoryPoints(side, points, reason, tracker);
        Report report = new Report(REPORT_OBJECTIVE_POINTS_AWARDED, Report.PUBLIC);
        report.add(displayName(side));
        report.add(points);
        addReport(report);
    }

    /** Credits victory points to the side's running tally without reporting; callers add their own report. */
    private void creditVictoryPoints(Side side, int points, String reason, VictoryPointTracker tracker) {
        int gameRound = getGame().getCurrentRound();
        if (side.isTeam()) {
            tracker.awardToTeam(side.id(), points, gameRound, reason);
        } else {
            tracker.awardToPlayer(side.id(), points, gameRound, reason);
        }
    }

    // --- Sensor Check mission (scanning) ---

    /**
     * Resolves scanning for the current End Phase: every eligible unit may attempt one scan (a sensor check needs no
     * optional rules - it is a Piloting Skill Roll). Confirming an unconfirmed Potential Objective candidate takes
     * priority; with the {@link OptionsConstants#VICTORY_USE_SENSOR_CHECK} game option on, units otherwise scan
     * enemy units and scanners that exfiltrated convert their banked scans to Victory Points. Does nothing when
     * neither applies.
     */
    private void resolveScanMission() {
        boolean sensorCheckMission = getGame().getOptions().booleanOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK);
        List<PlacedObjective> objectiveCandidates = findUnconfirmedCandidates();
        if (!sensorCheckMission && objectiveCandidates.isEmpty()) {
            return;
        }
        performScans(sensorCheckMission, objectiveCandidates);
        if (sensorCheckMission) {
            awardExfiltrationVictoryPoints(ScanTally.getTally(getGame()));
        }
    }

    /** @return All unconfirmed Potential Objective candidates on the board (RAW: scanned to confirm them) */
    private List<PlacedObjective> findUnconfirmedCandidates() {
        List<PlacedObjective> candidates = new ArrayList<>();
        for (PlacedObjective objective : findAllObjectives()) {
            ObjectiveMarker marker = objective.marker();
            if (marker.isPotential() && !marker.isConfirmed() && !marker.isDestroyed()) {
                candidates.add(objective);
            }
        }
        return candidates;
    }

    private void performScans(boolean sensorCheckMission, List<PlacedObjective> objectiveCandidates) {
        List<Entity> entities = getGame().getEntitiesVector();
        boolean designatedTargetsOnly = sensorCheckMission && hasDesignatedScanTargets(entities);
        ScanTally tally = sensorCheckMission ? ScanTally.getTally(getGame()) : null;
        for (Entity scanner : entities) {
            if (!isEligibleScanner(scanner)) {
                continue;
            }
            // Confirming an objective candidate takes priority and uses up the one scan per unit per turn
            PlacedObjective candidate = findCandidateInRange(scanner, objectiveCandidates);
            if (candidate != null) {
                attemptConfirmationScan(scanner, candidate);
                continue;
            }
            if (tally == null) {
                continue;
            }
            Entity target = findScanTarget(scanner, entities, tally, designatedTargetsOnly);
            if (target != null) {
                attemptScan(scanner, target, tally);
            }
        }
    }

    /**
     * @return The closest unconfirmed Potential Objective candidate within the scanner's scanning range and line of
     *       sight, or {@code null} when there is none (candidates resolved earlier this phase are skipped)
     */
    @Nullable
    private PlacedObjective findCandidateInRange(Entity scanner, List<PlacedObjective> objectiveCandidates) {
        int scanningRange = scanningRange(scanner);
        PlacedObjective closestCandidate = null;
        int closestDistance = Integer.MAX_VALUE;
        for (PlacedObjective candidate : objectiveCandidates) {
            ObjectiveMarker marker = candidate.marker();
            if (marker.isConfirmed() || marker.isDestroyed()) {
                continue;
            }
            int distance = scanner.getPosition().distance(candidate.position());
            if ((distance > scanningRange) || !hasLineOfSightToHex(scanner, candidate.position())) {
                continue;
            }
            if (distance < closestDistance) {
                closestDistance = distance;
                closestCandidate = candidate;
            }
        }
        return closestCandidate;
    }

    /**
     * Scans an unconfirmed Potential Objective candidate. On a successful sensor check, roll 1D6: on
     * {@value #CONFIRMATION_MINIMUM_ROLL}+ the candidate is a confirmed objective (known to all players); otherwise
     * it is useless and immediately removed from the battlefield.
     */
    private void attemptConfirmationScan(Entity scanner, PlacedObjective candidate) {
        ObjectiveMarker marker = candidate.marker();
        int targetNumber = baseScanTargetNumber(scanner);
        int roll = rollScanCheck();
        boolean scanSucceeded = roll >= targetNumber;
        LOGGER.debug("[Scan] {} scans objective candidate {} at {}: TN {} (piloting {} + scan {}, sensor hits {}, "
                    + "probe level {}), rolled {} - {}",
              scanner.getShortName(), marker.generalName(), candidate.position(), targetNumber,
              scanner.getCrew().getPiloting(), SCAN_MODIFIER, sensorCriticalHits(scanner),
              activeProbeLevel(scanner), roll, scanSucceeded ? "success" : "failure");
        if (!scanSucceeded) {
            return;
        }

        int confirmationRoll = rollObjectiveConfirmation();
        if (confirmationRoll >= CONFIRMATION_MINIMUM_ROLL) {
            marker.setConfirmed(true);
            LOGGER.info("[Objective] {} at {} is confirmed as a real objective (confirmation roll {})",
                  marker.generalName(), candidate.position(), confirmationRoll);
            Report report = new Report(REPORT_OBJECTIVE_CONFIRMED, Report.PUBLIC);
            report.add(scanner.getDisplayName());
            report.add(marker.generalName());
            report.add(candidate.position().toFriendlyString());
            addReport(report);
        } else {
            getGame().removeGroundObject(candidate.position(), marker);
            gameManager.sendGroundObjectUpdate();
            LOGGER.info("[Objective] {} at {} is useless and removed from the battlefield (confirmation roll {})",
                  marker.generalName(), candidate.position(), confirmationRoll);
            Report report = new Report(REPORT_OBJECTIVE_USELESS, Report.PUBLIC);
            report.add(scanner.getDisplayName());
            report.add(marker.generalName());
            report.add(candidate.position().toFriendlyString());
            addReport(report);
        }
    }

    /**
     * @return {@code true} when the mission designates specific units as scan targets - then only those units can
     *       be scanned; without designations, every enemy unit is a valid scan target
     */
    private boolean hasDesignatedScanTargets(List<Entity> entities) {
        boolean designatedTargetsOnly = false;
        for (Entity entity : entities) {
            if (entity.isDesignatedScanTarget()) {
                designatedTargetsOnly = true;
                break;
            }
        }
        if (designatedTargetsOnly) {
            LOGGER.debug("[Scan] The mission designates specific scan targets - only designated units can be "
                  + "scanned");
        }
        return designatedTargetsOnly;
    }

    /**
     * Checks whether a unit can attempt scans: it must be deployed on the board, not transported, have a crew and a
     * working sensor ({@link Entity#getActiveSensor()} not {@code null} - covers destroyed sensors), and have fewer
     * than {@value #SCAN_BLOCKING_SENSOR_CRITICAL_HITS} sensor critical hits (two sensor hits prevent sensor
     * checks). An immobile unit can still scan - sensor checks are not movement.
     */
    private boolean isEligibleScanner(Entity scanner) {
        boolean isOnBoard = (scanner.getPosition() != null) && scanner.isDeployed() && !scanner.isOffBoard()
              && !scanner.isDestroyed() && (scanner.getTransportId() == Entity.NONE);
        if (!isOnBoard) {
            return false;
        }
        // TRACE below because this runs in a loop over all entities each End Phase
        if ((scanner.getActiveSensor() == null) || (scanner.getCrew() == null)) {
            LOGGER.trace("[Scan] {} cannot scan: no working sensor or no crew", scanner.getShortName());
            return false;
        }
        if (sensorCriticalHits(scanner) >= SCAN_BLOCKING_SENSOR_CRITICAL_HITS) {
            LOGGER.trace("[Scan] {} cannot scan: {} or more sensor critical hits prevent sensor checks",
                  scanner.getShortName(), SCAN_BLOCKING_SENSOR_CRITICAL_HITS);
            return false;
        }
        return true;
    }

    /**
     * Picks the scan target for this scanner: the closest enemy unit within scanning range and line of sight that
     * the scanner has not already banked. One scan per unit per turn. When the mission designates scan targets,
     * only designated units qualify.
     *
     * @return The chosen target, or {@code null} when there is no valid scan target
     */
    @Nullable
    private Entity findScanTarget(Entity scanner, List<Entity> entities, ScanTally tally,
          boolean designatedTargetsOnly) {
        int scanningRange = scanningRange(scanner);
        Entity closestTarget = null;
        int closestDistance = Integer.MAX_VALUE;
        for (Entity target : entities) {
            if (!isValidScanTarget(scanner, target, scanningRange, tally, designatedTargetsOnly)) {
                continue;
            }
            int distance = scanner.getPosition().distance(target.getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTarget = target;
            }
        }
        return closestTarget;
    }

    private boolean isValidScanTarget(Entity scanner, Entity target, int scanningRange, ScanTally tally,
          boolean designatedTargetsOnly) {
        boolean isOnBoard = (target.getPosition() != null) && target.isDeployed() && !target.isOffBoard()
              && !target.isDestroyed();
        boolean isEnemy = (scanner.getOwner() != null) && (target.getOwner() != null)
              && target.getOwner().isEnemyOf(scanner.getOwner());
        if (!isOnBoard || !isEnemy) {
            return false;
        }
        if (designatedTargetsOnly && !target.isDesignatedScanTarget()) {
            LOGGER.trace("[Scan] {} is not a designated scan target - skipped", target.getShortName());
            return false;
        }
        if (tally.hasScanned(scanner.getId(), target.getId())) {
            return false;
        }
        if (scanner.getPosition().distance(target.getPosition()) > scanningRange) {
            return false;
        }
        return hasLineOfSight(scanner, target);
    }

    /**
     * @return The scanning range of the unit: {@value #DEFAULT_SCANNING_RANGE} hexes, or the range of its active
     *       probe when it has a working probe that is not negated by hostile ECM
     */
    int scanningRange(Entity scanner) {
        if (scanner.hasBAP(true)) {
            int probeRange = scanner.getBAPRange();
            if (probeRange > DEFAULT_SCANNING_RANGE) {
                return probeRange;
            }
        }
        return DEFAULT_SCANNING_RANGE;
    }

    /**
     * Computes the target number for the scan sensor check: the pilot's Piloting Skill (a sensor check is a Piloting
     * Skill Roll that ignores ordinary PSR modifiers) + {@value #SCAN_MODIFIER} scan modifier,
     * + {@value #SCAN_SENSOR_CRITICAL_MODIFIER} with a sensor critical hit, - the active probe level with a working
     * non-negated probe, + {@value #SCAN_STEALTH_MODIFIER} against a target with an active stealth system.
     *
     * @param scanner The scanning unit; must have a crew
     * @param target  The unit being scanned
     *
     * @return The 2d6 target number for the scan
     */
    int computeScanTargetNumber(Entity scanner, Entity target) {
        int targetNumber = baseScanTargetNumber(scanner);
        if (target.isStealthActive()) {
            targetNumber += SCAN_STEALTH_MODIFIER;
        }
        return targetNumber;
    }

    /**
     * @return The scan target number without target-dependent modifiers, as used for scanning objective markers:
     *       Piloting + {@value #SCAN_MODIFIER}, + {@value #SCAN_SENSOR_CRITICAL_MODIFIER} with a sensor critical
     *       hit, - the active probe level
     */
    int baseScanTargetNumber(Entity scanner) {
        int targetNumber = scanner.getCrew().getPiloting() + SCAN_MODIFIER;
        if (sensorCriticalHits(scanner) > 0) {
            targetNumber += SCAN_SENSOR_CRITICAL_MODIFIER;
        }
        return targetNumber - activeProbeLevel(scanner);
    }

    /**
     * @return The number of sensor critical hits on the unit: head (and center torso for torso-mounted cockpits)
     *       sensor slots for Meks, the sensor hit counters for vehicles and aerospace units, 0 for other types
     */
    int sensorCriticalHits(Entity scanner) {
        if (scanner instanceof Mek mek) {
            int sensorHits = mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            if (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                sensorHits += mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS,
                      Mek.LOC_CENTER_TORSO);
            }
            return sensorHits;
        }
        if (scanner instanceof Tank tank) {
            return tank.getSensorHits();
        }
        if (scanner instanceof IAero aero) {
            return aero.getSensorHits();
        }
        return 0;
    }

    /**
     * Determines the unit's active probe level for the scan TN bonus: Light Active Probe = 1, Beagle-class probes
     * (Beagle, Clan Active Probe, Watchdog, Nova) = 2, Bloodhound = 3. Probe capability without probe equipment
     * (implants, quirks) counts as a light probe. A probe negated by hostile ECM gives no level (and no range).
     *
     * @param scanner The scanning unit
     *
     * @return The probe level, or 0 without a working non-negated probe
     */
    int activeProbeLevel(Entity scanner) {
        if (!scanner.hasBAP(true)) {
            return 0;
        }
        int probeLevel = 0;
        for (MiscMounted miscEquipment : scanner.getMisc()) {
            if (!miscEquipment.getType().hasFlag(MiscType.F_BAP) || miscEquipment.isInoperable()) {
                continue;
            }
            String probeName = miscEquipment.getType().getInternalName().toLowerCase();
            if (probeName.contains("bloodhound")) {
                probeLevel = Math.max(probeLevel, PROBE_LEVEL_BLOODHOUND);
            } else if (probeName.contains("light")) {
                probeLevel = Math.max(probeLevel, PROBE_LEVEL_LIGHT);
            } else {
                probeLevel = Math.max(probeLevel, PROBE_LEVEL_STANDARD);
            }
        }
        return (probeLevel == 0) ? PROBE_LEVEL_LIGHT : probeLevel;
    }

    private void attemptScan(Entity scanner, Entity target, ScanTally tally) {
        int targetNumber = computeScanTargetNumber(scanner, target);
        int roll = rollScanCheck();
        boolean success = roll >= targetNumber;
        LOGGER.debug("[Scan] {} scans {}: TN {} (piloting {} + scan {}, sensor hits {}, probe level {}, "
                    + "target stealth {}), rolled {} - {}",
              scanner.getShortName(), target.getShortName(), targetNumber, scanner.getCrew().getPiloting(),
              SCAN_MODIFIER, sensorCriticalHits(scanner), activeProbeLevel(scanner), target.isStealthActive(),
              roll, success ? "success" : "failure");
        if (!success) {
            return;
        }
        tally.recordScan(scanner.getId(), target.getId());
        Report report = new Report(REPORT_SCAN_SUCCESS);
        report.subject = scanner.getId();
        report.add(scanner.getDisplayName());
        report.add(target.getDisplayName());
        addReport(report);
    }

    /**
     * Converts banked scans to Victory Points for scanners that exfiltrated (fled the board): 1 VP per banked scan
     * when the game round is at least {@value #EXFILTRATION_EARLIEST_ROUND}; fleeing earlier forfeits the scans.
     * Each scanner's exfiltration is processed exactly once.
     */
    private void awardExfiltrationVictoryPoints(ScanTally tally) {
        for (Entity retreatedEntity : Collections.list(getGame().getRetreatedEntities())) {
            boolean isFled = retreatedEntity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT;
            int scanCount = tally.getScanCount(retreatedEntity.getId());
            if (!isFled || (scanCount == 0) || tally.isExfiltrationProcessed(retreatedEntity.getId())) {
                continue;
            }
            tally.markExfiltrationProcessed(retreatedEntity.getId());

            if (getGame().getCurrentRound() < EXFILTRATION_EARLIEST_ROUND) {
                LOGGER.info("[Scan] {} fled before round {} - its {} banked scan(s) are lost",
                      retreatedEntity.getShortName(), EXFILTRATION_EARLIEST_ROUND, scanCount);
                Report report = new Report(REPORT_SCANS_LOST, Report.PUBLIC);
                report.add(retreatedEntity.getDisplayName());
                report.add(EXFILTRATION_EARLIEST_ROUND);
                addReport(report);
                continue;
            }

            Side side = sideOfPlayer(retreatedEntity.getOwner());
            if (side == null) {
                LOGGER.warn("[Scan] Exfiltrated scanner {} has no owner - cannot award scan victory points",
                      retreatedEntity.getShortName());
                continue;
            }
            creditVictoryPoints(side, scanCount,
                  "exfiltrated with " + scanCount + " scan(s)", VictoryPointTracker.getTracker(getGame()));
            LOGGER.info("[Scan] {} exfiltrated with {} banked scan(s) - {} receives {} VP",
                  retreatedEntity.getShortName(), scanCount, displayName(side), scanCount);
            Report report = new Report(REPORT_SCAN_POINTS_AWARDED, Report.PUBLIC);
            report.add(displayName(side));
            report.add(scanCount);
            report.add(retreatedEntity.getDisplayName());
            addReport(report);
        }
    }

    /** Seam for tests: line of sight between scanner and target. */
    boolean hasLineOfSight(Entity scanner, Entity target) {
        return LosEffects.calculateLOS(getGame(), scanner, target).canSee();
    }

    /** Seam for tests: line of sight from the scanner to a board hex (for scanning objective markers). */
    boolean hasLineOfSightToHex(Entity scanner, Coords position) {
        HexTarget hexTarget = new HexTarget(position, Targetable.TYPE_HEX_CLEAR);
        return LosEffects.calculateLOS(getGame(), scanner, hexTarget).canSee();
    }

    /** Seam for tests: the 2d6 scan check roll. */
    int rollScanCheck() {
        return Compute.d6(2);
    }

    /** Seam for tests: the 1D6 Potential Objective confirmation roll ({@value #CONFIRMATION_MINIMUM_ROLL}+ confirms). */
    int rollObjectiveConfirmation() {
        return Compute.d6();
    }

    /** Seam for tests: the 1D6 Fragile Objective destruction roll (1-{@value #FRAGILE_DESTRUCTION_MAXIMUM_ROLL} destroys). */
    int rollFragileCheck() {
        return Compute.d6();
    }

    /** Seam for tests: the 2d6 forced-drop Piloting Skill Roll. */
    int rollForcedDropCheck() {
        return Compute.d6(2);
    }

    /**
     * Records the resolved controller on the marker itself, so state-based victory triggers
     * (objective control conditions) can read it without re-running the control algorithm.
     */
    private void storeControllerOnMarker(ObjectiveMarker marker, @Nullable Side controller) {
        if (controller == null) {
            marker.setController(ObjectiveMarker.NO_CONTROLLER, ObjectiveMarker.NO_CONTROLLER);
        } else if (controller.isTeam()) {
            marker.setController(controller.id(), ObjectiveMarker.NO_CONTROLLER);
        } else {
            marker.setController(ObjectiveMarker.NO_CONTROLLER, controller.id());
        }
    }

    private void reportObjectiveControl(PlacedObjective objective, @Nullable Side controller) {
        Report report;
        if (controller == null) {
            report = new Report(REPORT_OBJECTIVE_UNCONTROLLED, Report.PUBLIC);
            report.add(objective.marker().generalName());
            report.add(objective.position().toFriendlyString());
        } else {
            report = new Report(REPORT_OBJECTIVE_CONTROLLED, Report.PUBLIC);
            report.add(objective.marker().generalName());
            report.add(objective.position().toFriendlyString());
            report.add(displayName(controller));
        }
        addReport(report);
    }

    /**
     * @param player A player, or {@code null} when unknown
     *
     * @return The scoring side of the player: their team, or the player itself when not on a team; {@code null} when
     *       the player is {@code null}
     */
    @Nullable
    private Side sideOfPlayer(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        if (player.getTeam() != Player.TEAM_NONE) {
            return new Side(true, player.getTeam());
        }
        return new Side(false, player.getId());
    }

    @Nullable
    private Side sideOfPlayerId(int playerId) {
        Player player = getGame().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("[Objective] Objective owner player ID {} does not exist in the game - the objective "
                  + "counts as an enemy objective for every side", playerId);
        }
        return sideOfPlayer(player);
    }

    private String displayName(Side side) {
        if (side.isTeam()) {
            return "Team " + side.id();
        }
        Player player = getGame().getPlayer(side.id());
        return (player == null) ? "Player " + side.id() : player.getName();
    }
}
