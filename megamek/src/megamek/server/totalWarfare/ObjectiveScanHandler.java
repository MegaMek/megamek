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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.ui.Messages;
import megamek.common.LosEffects;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.TargetRollModifier;
import megamek.common.actions.ScanAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.BankedScan;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveScoringScheme.ScanPayout;
import megamek.common.equipment.ObjectiveScoringScheme.SchemePreset;
import megamek.common.equipment.ScanMission;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesScanning;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryPointTracker;
import megamek.server.victory.VictoryPointTracker.ScanOutcome;
import megamek.server.victory.VictoryPointTracker.ScanRecord;

/**
 * Resolves the scans units ordered this turn, and settles the readings of units that have left the battlefield.
 * Runs in the End Phase before the control resolution, so any points it awards are in the tally the victory check
 * reads (Objectives series, part 4; Core Rulebook p.233 Scanning and p.217 the Sensor Check mission, and the
 * optional TacOps: Advanced Rules p.187 Scanning).
 *
 * <p>A unit may order one scan per turn, at any hex, building or unit in range and line of sight. The scan is a
 * sensor check under whichever scanning rules the game has in force. On a success the handler looks at what is there: a Scan point the
 * scanner's own side placed, or an enemy unit while the Sensor Check mission is on, banks a reading on the
 * scanning unit; anything else reports nothing of interest, so a player cannot tell a hidden objective from an
 * empty hex by the answer alone. A reading is worth nothing until the unit that carries it leaves over its home
 * edge on the exit turn or later: then a Scan point pays its value to the owner once, and every enemy-unit reading
 * pays one point. A unit that leaves earlier, or is destroyed or captured, loses its readings, which is the whole
 * of the defender's counterplay.</p>
 */
class ObjectiveScanHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveScanHandler.class);

    static final int REPORT_SCAN_SUCCESS = 7121;
    static final int REPORT_SCAN_POINTS_AWARDED = 7122;
    static final int REPORT_READINGS_LOST_EARLY_EXIT = 7123;
    static final int REPORT_SCAN_FAILED = 7124;
    static final int REPORT_NOTHING_OF_INTEREST = 7125;
    static final int REPORT_READINGS_LOST_WITH_UNIT = 7126;
    static final int REPORT_CANNOT_SCAN = 7127;
    static final int REPORT_SCAN_AUTOMATIC = 7128;
    static final int REPORT_SCAN_EDGE_REROLL = 7129;
    static final int REPORT_SCAN_POINT_SCORED = 7130;
    static final int REPORT_READINGS_CARRIED = 7153;
    static final int REPORT_READINGS_LOST_WRONG_EDGE = 7132;
    static final int REPORT_READING_BANKED = 7150;
    static final int REPORT_POINTS_HELD_WHILE_ALIVE = 7154;
    static final int REPORT_SCAN_RECORD_HEADER = 7155;
    static final int REPORT_SCAN_RECORD_LINE = 7156;
    static final int REPORT_SCAN_RECORD_OUTCOME_LINE = 7157;
    /** Says a successful scan needs no line of its own beyond the scoring line above it. */
    static final int NO_PLAIN_LINE = 0;
    static final int REPORT_SCAN_REVEALS = 7151;
    static final int REPORT_SCAN_POINTS_TAKEN_BACK = 7152;

    /** Worth of one reading of an enemy unit in the Sensor Check mission (Core Rules p.217). */
    static final int VICTORY_POINTS_PER_UNIT_READING = 1;

    ObjectiveScanHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Receives a unit's scan order from its player, in the pre-End declarations phase. The order is checked the way
     * the End Phase will check it - the unit is the sender's, it can scan, the target is in range and in line of
     * sight - and stored on the unit; a later order from the same unit replaces it. Refusals are logged and told to
     * the sender, and change nothing.
     *
     * @param payload the packet's payload, expected to be a {@link ScanAction}
     * @param connId  the sending connection, which must own the unit
     */
    void receiveScanOrder(Object payload, int connId) {
        if (!(payload instanceof ScanAction order)) {
            LOGGER.warn("[Scan] connection {} sent a scan order that is not a ScanAction - ignored", connId);
            return;
        }
        Entity scanner = getGame().getEntity(order.getEntityId());
        Player sender = getGame().getPlayer(connId);
        if ((scanner == null) || (sender == null) || !sender.equals(scanner.getOwner())) {
            LOGGER.warn("[Scan] connection {} ordered a scan for unit {}, which it does not own - ignored", connId,
                  order.getEntityId());
            return;
        }
        if (!getGame().getPhase().isPreEndDeclarations()) {
            LOGGER.warn("[Scan] {} ordered a scan outside the pre-End declarations phase ({}) - ignored",
                  scanner.getShortName(), getGame().getPhase());
            return;
        }
        String refusal = orderRefusal(scanner, order);
        if (refusal != null) {
            LOGGER.info("[Scan] {} may not scan {}: {}", scanner.getShortName(), describeOrderTarget(order), refusal);
            gameManager.sendServerChat(connId, Messages.getString("ObjectiveScan.orderRefused",
                  scanner.getShortName(), describeOrderTarget(order), refusal));
            return;
        }
        scanner.setPendingScan(order);
        LOGGER.info("[Scan] {} will scan {} in the End Phase", scanner.getShortName(), describeOrderTarget(order));
        gameManager.entityUpdate(scanner.getId());
    }

    /**
     * @return why the order cannot be given, or {@code null} when it can: the same range, line of sight and ruleset
     *       questions the End Phase asks, so an order that is accepted here resolves there
     */
    private @Nullable String orderRefusal(Entity scanner, ScanAction order) {
        if (!ScanMission.canOrderScan(scanner)) {
            return Messages.getString("ObjectiveScan.cannotScanNow");
        }
        Targetable target = order.resolveTarget(getGame());
        if ((target == null) || (target.getPosition() == null) || (scanner.getPosition() == null)) {
            return Messages.getString("ObjectiveScan.targetGone");
        }
        RulesScanning rules = ScanMission.scanningRules(getGame());
        TargetRoll targetRoll = rules.scanTargetRoll(scanner, target);
        if (targetRoll.getValue() == TargetRoll.IMPOSSIBLE) {
            return targetRoll.getDesc();
        }
        int distance = scanner.getPosition().distance(target.getPosition());
        int range = rules.scanningRange(scanner, target);
        if (distance > range) {
            return Messages.getString("ObjectiveScan.outOfRange", distance, range);
        }
        if (!hasLineOfSight(scanner, target)) {
            return Messages.getString("ObjectiveScan.noLineOfSight");
        }
        return null;
    }

    private String describeOrderTarget(ScanAction order) {
        Coords position = order.resolveTargetPosition(getGame());
        return (position == null) ? Messages.getString("ObjectiveScan.unitNoLongerThere") : position.getBoardNum();
    }

    /**
     * The End Phase pass: every ordered scan is resolved and cleared, then the readings of units that have left the
     * battlefield are paid out or forfeited. Nothing happens while objectives are switched off.
     */
    void resolveScans() {
        if (!getGame().getOptions().booleanOption(OptionsConstants.VICTORY_USE_OBJECTIVES)) {
            clearOrdersQuietly();
            return;
        }
        for (Entity scanner : getGame().getEntitiesVector()) {
            ScanAction order = scanner.getPendingScan();
            if (order == null) {
                continue;
            }
            scanner.setPendingScan(null);
            resolveScan(scanner, order);
            // Tell the clients the order is spent. Without this a scan that banked nothing left every client
            // believing one was still pending, so the Scan button went on offering to cancel a scan that had
            // already happened.
            gameManager.entityUpdate(scanner.getId());
        }
        settleReadingsOfUnitsThatLeft();
    }

    /** With objectives off a scan order means nothing; say so once per order in the log and drop it. */
    private void clearOrdersQuietly() {
        for (Entity scanner : getGame().getEntitiesVector()) {
            if (scanner.getPendingScan() != null) {
                LOGGER.info("[Scan] {} ordered a scan but use_objectives is off - the order is dropped",
                      scanner.getShortName());
                scanner.setPendingScan(null);
            }
        }
    }

    private void resolveScan(Entity scanner, ScanAction order) {
        Targetable target = order.resolveTarget(getGame());
        if ((target == null) || (target.getPosition() == null) || (scanner.getPosition() == null)) {
            reportCannotScan(scanner, order, Messages.getString("ObjectiveScan.targetGone"));
            return;
        }
        String targetName = describeTarget(target);
        RulesScanning rules = ScanMission.scanningRules(getGame());
        int distance = scanner.getPosition().distance(target.getPosition());
        int range = rules.scanningRange(scanner, target);
        if (distance > range) {
            reportCannotScan(scanner, order, Messages.getString("ObjectiveScan.outOfRange", distance, range));
            return;
        }
        if (!hasLineOfSight(scanner, target)) {
            reportCannotScan(scanner, order, Messages.getString("ObjectiveScan.noLineOfSight"));
            return;
        }
        TargetRoll targetRoll = rules.scanTargetRoll(scanner, target);
        if (targetRoll.getValue() == TargetRoll.IMPOSSIBLE) {
            reportCannotScan(scanner, order, targetRoll.getDesc());
            return;
        }
        boolean succeeded = targetRoll.needsRoll() ? rollTheCheck(scanner, targetName, targetRoll) : true;
        if (!targetRoll.needsRoll()) {
            Report report = new Report(REPORT_SCAN_AUTOMATIC, Report.PUBLIC);
            report.addDesc(scanner);
            report.add(targetName);
            report.add(targetRoll.getDesc());
            addReport(report);
        }
        if (!succeeded) {
            logScan(scanner, target, targetName, ScanOutcome.FAILED, false, 0);
            return;
        }
        bankWhatIsThere(scanner, target, targetName);
    }

    /**
     * Rolls the sensor check, spending Edge on a failure when the pilot has it and the trigger is on.
     *
     * @return {@code true} on a success
     */
    private boolean rollTheCheck(Entity scanner, String targetName, TargetRoll targetRoll) {
        Roll roll = rollScanCheck();
        boolean succeeded = roll.getIntValue() >= targetRoll.getValue();
        LOGGER.debug("[Scan] {} scans {}: needs {} ({}), rolled {} - {}", scanner.getShortName(), targetName,
              targetRoll.getValue(), targetRoll.getDesc(), roll.getIntValue(), succeeded ? "success" : "failure");
        if (!succeeded && scanner.shouldUseEdge(OptionsConstants.EDGE_WHEN_SCAN_FAILS)) {
            scanner.getCrew().decreaseEdge();
            Report edgeReport = new Report(REPORT_SCAN_EDGE_REROLL, Report.PUBLIC);
            edgeReport.addDesc(scanner);
            edgeReport.add(roll.getIntValue());
            edgeReport.add(scanner.getCrew().getOptions().intOption(OptionsConstants.EDGE));
            addReport(edgeReport);
            roll = rollScanCheck();
            succeeded = roll.getIntValue() >= targetRoll.getValue();
            LOGGER.debug("[Scan] {} rerolls the scan with Edge: rolled {} - {}", scanner.getShortName(),
                  roll.getIntValue(), succeeded ? "success" : "failure");
        }
        Report report = new Report(succeeded ? REPORT_SCAN_SUCCESS : REPORT_SCAN_FAILED, Report.PUBLIC);
        report.addDesc(scanner);
        report.add(targetName);
        report.add(targetRoll.getValue());
        // the breakdown, so a player can see whether the probe counted and what jammed it
        report.add(breakdownOf(targetRoll));
        report.add(roll.getIntValue());
        addReport(report);
        return succeeded;
    }

    /**
     * @param targetRoll the check
     *
     * @return the check's parts in reading order: the base first as "Piloting skill 5", then each modifier with its
     *       sign, "+3 scanning", "-2 active probe level 2"
     */
    static String breakdownOf(TargetRoll targetRoll) {
        List<String> parts = new ArrayList<>();
        boolean isBase = true;
        for (TargetRollModifier modifier : targetRoll.getModifiers()) {
            if (isBase) {
                parts.add(modifier.description() + " " + modifier.value());
                isBase = false;
            } else {
                String sign = (modifier.value() < 0) ? "-" : "+";
                parts.add(sign + Math.abs(modifier.value()) + " " + modifier.description());
            }
        }
        return String.join(", ", parts);
    }

    /**
     * After a successful check, decides what the scanner found: a Scan point of its own side, an enemy unit in a
     * Sensor Check mission, or nothing of interest.
     */
    private void bankWhatIsThere(Entity scanner, Targetable target, String targetName) {
        int round = getGame().getCurrentRound();
        ObjectiveMarker scanPoint = scanPointOwnedBySideAt(scanner, target.getPosition());
        if (scanPoint != null) {
            switch (scanPoint.getScoringScheme().getScanPayout()) {
                case ON_EXIT -> {
                    scanner.bankScan(BankedScan.ofObjective(round, target.getPosition(), scanPoint.generalName()));
                    LOGGER.info("[Scan] {} banked a reading of {} - {} reading(s) carried", scanner.getShortName(),
                          scanPoint.generalName(), scanner.getBankedScans().size());
                    gameManager.entityUpdate(scanner.getId());
                }
                case ON_SCAN -> scoreScanPoint(scanPoint, scanner, sideOf(scanner.getOwner()));
                default -> { /* every payout is handled; the lines below add what each one tells the player */ }
                case ON_SCAN_UNTIL_LOST -> {
                    // paid now, and the reading stays on the unit so the points can be taken back if it is lost
                    scoreScanPoint(scanPoint, scanner, sideOf(scanner.getOwner()));
                    scanner.bankScan(BankedScan.ofObjectivePaidOnScan(round, target.getPosition(),
                          scanPoint.generalName()));
                    gameManager.entityUpdate(scanner.getId());
                }
            }
            reportWhatTheScanGave(scanner, scanPoint.getScanRevealsNote(),
                  plainLineFor(scanPoint.getScoringScheme().getScanPayout()));
            boolean paidNow = scanPoint.getScoringScheme().getScanPayout() != ScanPayout.ON_EXIT;
            logScan(scanner, target, scanPoint.generalName(), ScanOutcome.SUCCEEDED, true,
                  paidNow ? scanPoint.getVictoryPointValue() : 0);
            return;
        }
        boolean isSensorCheckMission = getGame().getOptions().booleanOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK);
        if (isSensorCheckMission && (target instanceof Entity targetUnit) && isScorableEnemy(scanner, targetUnit)) {
            scanner.bankScan(BankedScan.ofEnemyUnit(round, targetUnit.getId(), targetUnit.getShortName()));
            LOGGER.info("[Scan] {} banked a reading of enemy {} - {} reading(s) carried", scanner.getShortName(),
                  targetUnit.getShortName(), scanner.getBankedScans().size());
            gameManager.entityUpdate(scanner.getId());
            reportWhatTheScanGave(scanner, "", REPORT_READING_BANKED);
            logScan(scanner, target, targetUnit.getShortName(), ScanOutcome.SUCCEEDED, false, 0);
            return;
        }
        LOGGER.info("[Scan] nothing of interest for {} scanning {}: {}", scanner.getShortName(), targetName,
              whyNothingOfInterest(scanner, target));
        Report report = new Report(REPORT_NOTHING_OF_INTEREST, Report.PUBLIC);
        report.addDesc(scanner);
        report.add(targetName);
        addReport(report);
        logScan(scanner, target, targetName, ScanOutcome.NOTHING_FOUND, false, 0);
    }

    /**
     * The line after a successful scan. With nothing to reveal, everyone reads "The reading is banked."; with a note
     * on the point, the scanning unit's player reads the note instead, and nobody else reads anything, since the
     * note is the mission's secret and the other side has not earned it.
     *
     * @param scanner the unit that scanned
     * @param note    what the point reveals, or empty
     */
    private void reportWhatTheScanGave(Entity scanner, String note, int plainLineId) {
        if (note.isEmpty() || (scanner.getOwner() == null)) {
            if (plainLineId != NO_PLAIN_LINE) {
                Report report = new Report(plainLineId, Report.PUBLIC);
                report.indent();
                addReport(report);
            }
            return;
        }
        LOGGER.info("[Scan] the point reveals its note to {}", scanner.getOwner().getName());
        Report report = new Report(REPORT_SCAN_REVEALS, Report.PLAYER);
        report.player = scanner.getOwnerId();
        report.indent();
        report.add(note);
        addReport(report);
    }

    /**
     * Prints what every unit scanned this game, once, in the end of mission report. Without this the scan log is
     * only in the game's saved state, and nobody can see whether it is right.
     */
    void reportTheScanRecord() {
        List<ScanRecord> scanLog = VictoryPointTracker.getTracker(getGame()).getScanLog();
        if (scanLog.isEmpty()) {
            return;
        }
        addReport(new Report(REPORT_SCAN_RECORD_HEADER, Report.PUBLIC));
        for (ScanRecord record : scanLog) {
            boolean isTheScanItself = (record.outcome() == ScanOutcome.SUCCEEDED)
                  || (record.outcome() == ScanOutcome.FAILED)
                  || (record.outcome() == ScanOutcome.NOTHING_FOUND);
            Report line = new Report(isTheScanItself ? REPORT_SCAN_RECORD_LINE : REPORT_SCAN_RECORD_OUTCOME_LINE,
                  Report.PUBLIC);
            line.indent();
            line.add(record.gameRound());
            line.add(record.scannerName());
            if (isTheScanItself) {
                line.add(record.targetName());
            }
            line.add(outcomeWording(record));
            addReport(line);
        }
    }

    /**
     * @param record one logged scan
     *
     * @return how that scan ended, in the words the end of mission report uses
     */
    private static String outcomeWording(ScanRecord record) {
        return switch (record.outcome()) {
            case FAILED -> Messages.getString("ObjectiveScan.record.failed");
            case NOTHING_FOUND -> Messages.getString("ObjectiveScan.record.nothing");
            case SUCCEEDED -> (record.victoryPointsAwarded() > 0)
                  ? Messages.getString("ObjectiveScan.record.scored", record.victoryPointsAwarded())
                  : Messages.getString("ObjectiveScan.record.read");
            case CARRIED_HOME -> Messages.getString("ObjectiveScan.record.carriedHome", record.targetName(),
                  record.victoryPointsAwarded());
            case LOST_EARLY -> Messages.getString("ObjectiveScan.record.lostEarly", record.targetName());
            case LOST_WRONG_EDGE -> Messages.getString("ObjectiveScan.record.lostWrongEdge", record.targetName());
            case LOST_WITH_UNIT -> Messages.getString("ObjectiveScan.record.lostWithUnit", record.targetName());
            case POINTS_TAKEN_BACK -> Messages.getString("ObjectiveScan.record.takenBack",
                  -record.victoryPointsAwarded(), record.targetName());
        };
    }

    /**
     * Withdraws the scan a unit ordered this turn. Guarded the same way as the order itself: only the unit's owner,
     * and only while the pre-End declarations phase is running, because after that the End Phase has already read
     * the order.
     *
     * @param packet the packet: the unit's id
     * @param connId the sending connection
     */
    public void receiveScanWithdraw(Packet packet, int connId) {
        if (!(packet.getObject(0) instanceof Integer entityId)) {
            LOGGER.warn("[Scan] connection {} sent a scan withdrawal without a unit - ignored", connId);
            return;
        }
        Entity scanner = getGame().getEntity(entityId);
        Player sender = getGame().getPlayer(connId);
        if ((scanner == null) || (sender == null) || !sender.equals(scanner.getOwner())) {
            LOGGER.warn("[Scan] connection {} withdrew the scan of unit {}, which it does not own - ignored", connId,
                  entityId);
            return;
        }
        if (!getGame().getPhase().isPreEndDeclarations()) {
            LOGGER.warn("[Scan] {} withdrew a scan outside the pre-End declarations phase ({}) - ignored",
                  scanner.getShortName(), getGame().getPhase());
            return;
        }
        if (scanner.getPendingScan() == null) {
            LOGGER.debug("[Scan] {} had no scan order to withdraw", scanner.getShortName());
            return;
        }
        scanner.setPendingScan(null);
        LOGGER.info("[Scan] {} withdrew its scan order and will scan nothing this turn", scanner.getShortName());
        gameManager.entityUpdate(scanner.getId());
    }

    /**
     * A game master's marking of a unit as one the mission wants scanned, at any time in the game. Once any unit
     * on the board is marked, only marked units are worth reading in a Sensor Check mission, so a convoy can be
     * made the objective while the escort is not. Anyone who is not a game master is refused and logged.
     *
     * @param packet the packet: the unit's id, then whether it is wanted
     * @param connId the sending connection
     */
    public void receiveScanDesignation(Packet packet, int connId) {
        Player sender = getGame().getPlayer(connId);
        if ((sender == null) || !sender.isGameMaster()) {
            LOGGER.warn("[Scan] Dropping a scan designation from {}: only a game master may set the mission's targets",
                  (sender == null) ? "an unknown connection" : sender.getName());
            return;
        }
        if (!(packet.getObject(0) instanceof Integer entityId) || !(packet.getObject(1) instanceof Boolean wanted)) {
            LOGGER.warn("[Scan] Dropping a scan designation from {}: the packet does not name a unit",
                  sender.getName());
            return;
        }
        Entity unit = getGame().getEntity(entityId);
        if (unit == null) {
            LOGGER.warn("[Scan] Dropping a scan designation from {}: unit {} is not on the battlefield",
                  sender.getName(), entityId);
            return;
        }
        unit.setDesignatedScanTarget(wanted);
        LOGGER.info("[Scan] Game master {} {} {} as a scan target", sender.getName(),
              wanted ? "marked" : "unmarked", unit.getShortName());
        gameManager.entityUpdate(unit.getId());
        gameManager.sendServerChat(Messages.getString(
              wanted ? "ObjectiveScan.designated" : "ObjectiveScan.undesignated",
              sender.getName(), unit.getShortName()));
    }

    /**
     * Spells out why a successful scan found nothing worth banking. Every condition is named rather than just the
     * result, because "nothing of interest found" on its own leaves a playtester with nowhere to look.
     *
     * @param scanner the unit that scanned
     * @param target  what it scanned
     *
     * @return the reasons, for the log
     */
    private String whyNothingOfInterest(Entity scanner, Targetable target) {
        boolean isSensorCheckMission = getGame().getOptions().booleanOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK);
        boolean targetIsAUnit = target instanceof Entity;
        if (!targetIsAUnit) {
            return "the target is a hex or building, not a unit, and no scan point of this side is there"
                  + " (sensor check mission=" + isSensorCheckMission + ")";
        }
        Entity targetUnit = (Entity) target;
        return "target is a unit; sensor check mission=" + isSensorCheckMission
              + ", enemy of the scanner=" + isEnemyOfTheScanner(scanner, targetUnit)
              + ", this side has marked targets=" + missionMarksTargetsFor(scanner)
              + ", this one marked wanted=" + targetUnit.isDesignatedScanTarget();
    }

    /**
     * Adds what became of a unit's readings to the after-action record, so the end of mission report says how each
     * scan ended rather than stopping at the moment it was made.
     *
     * @param unit       the unit carrying the readings
     * @param outcome    what happened to them
     * @param concerning the detail the line needs: the number of readings, or the edge the unit left by
     * @param points     victory points paid, or taken back as a negative number, or 0
     */
    private void recordReadingOutcome(Entity unit, ScanOutcome outcome, String concerning, int points) {
        VictoryPointTracker.getTracker(getGame())
              .recordScan(new ScanRecord(getGame().getCurrentRound(), unit.getId(), unit.getShortName(),
                    unit.getOwnerId(), outcome, concerning, "", Entity.NONE, false, points));
    }

    /**
     * Writes one resolved scan into the game's after-action record, so a campaign can ask afterwards which unit
     * read which objective on which turn. Kept on the victory point tracker, which already rides the game's
     * victory context into savegames.
     *
     * @param scanner    the unit that scanned
     * @param target     what it scanned
     * @param targetName the name to record, which is the objective's name when it found one
     * @param outcome    how the scan ended
     * @param wasObjective {@code true} when the scan found an objective of the scanner's own side
     * @param pointsPaid the victory points paid at once, 0 when the reading must be carried home first
     */
    private void logScan(Entity scanner, Targetable target, String targetName, ScanOutcome outcome,
          boolean wasObjective, int pointsPaid) {
        Coords position = target.getPosition();
        int targetUnitId = (target instanceof Entity targetUnit) ? targetUnit.getId() : Entity.NONE;
        VictoryPointTracker.getTracker(getGame())
              .recordScan(new ScanRecord(getGame().getCurrentRound(), scanner.getId(), scanner.getShortName(),
                    scanner.getOwnerId(), outcome, targetName,
                    (position == null) ? "" : position.getBoardNum(), targetUnitId, wasObjective, pointsPaid));
    }

    /**
     * @param payout how the point pays
     *
     * @return the report line that follows a successful scan when the point reveals nothing: what the reading is
     *       worth now. A point that simply pays needs no line, because the scoring line already said so.
     */
    private static int plainLineFor(ScanPayout payout) {
        return switch (payout) {
            case ON_EXIT -> REPORT_READING_BANKED;
            case ON_SCAN_UNTIL_LOST -> REPORT_POINTS_HELD_WHILE_ALIVE;
            case ON_SCAN -> NO_PLAIN_LINE;
        };
    }

    /**
     * @return the Scan point at the hex that belongs to the scanner's side and has not been scored yet, or
     *       {@code null}; a point another side placed is not this side's to read and looks like nothing
     */
    private @Nullable ObjectiveMarker scanPointOwnedBySideAt(Entity scanner, Coords position) {
        for (ICarryable groundObject : getGame().getGroundObjects(position)) {
            if (!(groundObject instanceof ObjectiveMarker marker)) {
                continue;
            }
            ObjectiveScoringScheme scheme = marker.getScoringScheme();
            boolean isOpenScanPoint = (scheme.getPreset() == SchemePreset.SCAN) && !scheme.isDecided()
                  && !marker.isDestroyed();
            if (isOpenScanPoint && isSameSide(scanner.getOwner(), getGame().getPlayer(marker.getOwnerId()))) {
                return marker;
            }
        }
        return null;
    }

    /**
     * @return {@code true} when the unit is an enemy of the scanner that counts in the Sensor Check mission: any
     *       enemy, or only the marked ones when the mission marked some for this side to read
     */
    private boolean isScorableEnemy(Entity scanner, Entity target) {
        if (!isEnemyOfTheScanner(scanner, target)) {
            return false;
        }
        return !missionMarksTargetsFor(scanner) || target.isDesignatedScanTarget();
    }

    /**
     * Whether the mission has marked the units this side is meant to read. The question is asked about the
     * scanner's side rather than about the game as a whole, because a mark only ever sits on a unit somebody else
     * was sent to read. Asking globally meant that in a mission where both sides have something to read, marking
     * one side's targets made every ordinary enemy worthless to the other side as well.
     *
     * @param scanner the unit doing the scanning
     *
     * @return {@code true} when at least one enemy of this scanner is marked
     */
    private boolean missionMarksTargetsFor(Entity scanner) {
        for (Entity unit : getGame().getEntitiesVector()) {
            if (unit.isDesignatedScanTarget() && isEnemyOfTheScanner(scanner, unit)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param scanner the unit doing the scanning
     * @param unit    the unit to weigh up
     *
     * @return {@code true} when the two are on opposing sides
     */
    private boolean isEnemyOfTheScanner(Entity scanner, Entity unit) {
        return (scanner.getOwner() != null) && (unit.getOwner() != null)
              && unit.getOwner().isEnemyOf(scanner.getOwner());
    }

    /**
     * Pays out or forfeits the readings of every unit that has left the battlefield and has not been settled yet.
     * A unit that fled over its home edge on the exit turn or later scores; one that fled earlier, or died, loses
     * its readings.
     */
    private void settleReadingsOfUnitsThatLeft() {
        List<Entity> departed = new ArrayList<>();
        departed.addAll(Collections.list(getGame().getRetreatedEntities()));
        departed.addAll(Collections.list(getGame().getGraveyardEntities()));
        for (Entity unit : departed) {
            if (unit.isBankedScansRedeemed() || unit.getBankedScans().isEmpty()) {
                continue;
            }
            unit.setBankedScansRedeemed(true);
            boolean fled = unit.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT;
            if (!fled) {
                takeBackPointsPaidOnScan(unit);
                int unpaidReadings = countUnpaidReadings(unit);
                if (unpaidReadings > 0) {
                    LOGGER.info("[Scan] {} was lost with {} reading(s) - they are lost with it", unit.getShortName(),
                          unpaidReadings);
                    Report report = new Report(REPORT_READINGS_LOST_WITH_UNIT, Report.PUBLIC);
                    report.addDesc(unit);
                    report.add(unpaidReadings);
                    addReport(report);
                    recordReadingOutcome(unit, ScanOutcome.LOST_WITH_UNIT, String.valueOf(unpaidReadings), 0);
                }
                continue;
            }
            // a scout that left the battlefield by any exit keeps what it was paid on the scan
            int readings = countUnpaidReadings(unit);
            if (readings == 0) {
                LOGGER.info("[Scan] {} left with nothing still to be paid", unit.getShortName());
                continue;
            }
            int exitTurn = exitTurn();
            if (getGame().getCurrentRound() < exitTurn) {
                LOGGER.info("[Scan] {} left before turn {} - its {} reading(s) are lost", unit.getShortName(),
                      exitTurn, readings);
                Report report = new Report(REPORT_READINGS_LOST_EARLY_EXIT, Report.PUBLIC);
                report.addDesc(unit);
                report.add(exitTurn);
                report.add(readings);
                addReport(report);
                recordReadingOutcome(unit, ScanOutcome.LOST_EARLY, String.valueOf(readings), 0);
                continue;
            }
            if (!leftOverHomeEdge(unit)) {
                LOGGER.info("[Scan] {} left over the {} edge, not its side's home edge - its {} reading(s) are lost",
                      unit.getShortName(), unit.getRetreatedDirection(), readings);
                Report report = new Report(REPORT_READINGS_LOST_WRONG_EDGE, Report.PUBLIC);
                report.addDesc(unit);
                report.add(String.valueOf(unit.getRetreatedDirection()).toLowerCase(java.util.Locale.ROOT));
                report.add(readings);
                addReport(report);
                recordReadingOutcome(unit, ScanOutcome.LOST_WRONG_EDGE,
                      String.valueOf(unit.getRetreatedDirection()).toLowerCase(java.util.Locale.ROOT), 0);
                continue;
            }
            payOutReadings(unit);
        }
    }

    /**
     * @return how many of the unit's readings are still waiting to be paid on exit; readings paid on the scan are
     *       not counted
     */
    private static int countUnpaidReadings(Entity unit) {
        int unpaidReadings = 0;
        for (BankedScan reading : unit.getBankedScans()) {
            if (!reading.isPointsPaidOnScan()) {
                unpaidReadings++;
            }
        }
        return unpaidReadings;
    }

    /**
     * The take-back for a Scan point that paid on the scan but only while the scout lived: the points come off
     * the side's total and the point is open to be scanned again.
     */
    private void takeBackPointsPaidOnScan(Entity unit) {
        Side side = sideOf(unit.getOwner());
        if (side == null) {
            return;
        }
        for (BankedScan reading : unit.getBankedScans()) {
            if (!reading.isPointsPaidOnScan() || (reading.getObjectivePosition() == null)) {
                continue;
            }
            ObjectiveMarker scanPoint = scanPointAt(reading.getObjectivePosition());
            if ((scanPoint == null) || !scanPoint.getScoringScheme().isDecided()) {
                continue;
            }
            int points = scanPoint.getVictoryPointValue();
            ObjectiveScoringScheme scheme = scanPoint.getScoringScheme();
            scheme.setSecuredBy(ObjectiveScoringScheme.NO_SIDE, ObjectiveScoringScheme.NO_SIDE);
            scheme.setVictoryPointsAwarded(false);
            creditVictoryPoints(side, -points, "lost with " + unit.getShortName() + " before it left: "
                  + scanPoint.generalName());
            LOGGER.info("[Scan] {} was lost - the {} point(s) paid for scanning {} are taken back from {}",
                  unit.getShortName(), points, scanPoint.generalName(), displayName(side));
            Report report = new Report(REPORT_SCAN_POINTS_TAKEN_BACK, Report.PUBLIC);
            report.addDesc(unit);
            report.add(points);
            report.add(scanPoint.generalName());
            addReport(report);
            recordReadingOutcome(unit, ScanOutcome.POINTS_TAKEN_BACK, scanPoint.generalName(), -points);
        }
        gameManager.sendGroundObjectUpdate();
    }

    /** @return the Scan point at the hex, whatever its owner or state, or {@code null} */
    private @Nullable ObjectiveMarker scanPointAt(Coords position) {
        for (ICarryable groundObject : getGame().getGroundObjects(position)) {
            boolean isScanPoint = (groundObject instanceof ObjectiveMarker marker)
                  && (marker.getScoringScheme().getPreset() == SchemePreset.SCAN);
            if (isScanPoint) {
                return (ObjectiveMarker) groundObject;
            }
        }
        return null;
    }

    /** Turns a home-bound unit's readings into points: each Scan point once for the side, each enemy reading one. */
    private void payOutReadings(Entity unit) {
        Side side = sideOf(unit.getOwner());
        if (side == null) {
            LOGGER.warn("[Scan] {} carried readings home but has no owner - nothing awarded", unit.getShortName());
            return;
        }
        int pointsAwarded = 0;
        Set<Coords> pointsAlreadyPaid = new HashSet<>();
        for (BankedScan reading : unit.getBankedScans()) {
            if (reading.isPointsPaidOnScan()) {
                continue;
            }
            if (reading.isObjectiveReading()) {
                Coords position = reading.getObjectivePosition();
                if (!pointsAlreadyPaid.add(position)) {
                    continue;
                }
                ObjectiveMarker scanPoint = scanPointOwnedBySideAt(unit, position);
                if (scanPoint != null) {
                    pointsAwarded += scoreScanPoint(scanPoint, unit, side);
                } else {
                    LOGGER.info("[Scan] {} carried a reading of {} home, but that point is already scored or gone",
                          unit.getShortName(), reading.getDescription());
                }
            } else if (getGame().getOptions().booleanOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK)) {
                creditVictoryPoints(side, VICTORY_POINTS_PER_UNIT_READING,
                      "reading of " + reading.getDescription() + " carried home by " + unit.getShortName());
                pointsAwarded += VICTORY_POINTS_PER_UNIT_READING;
            }
        }
        LOGGER.info("[Scan] {} carried {} reading(s) home - {} receives {} victory point(s)", unit.getShortName(),
              unit.getBankedScans().size(), displayName(side), pointsAwarded);
        Report report = new Report(REPORT_SCAN_POINTS_AWARDED, Report.PUBLIC);
        report.add(displayName(side));
        report.add(pointsAwarded);
        report.addDesc(unit);
        addReport(report);
        recordReadingOutcome(unit, ScanOutcome.CARRIED_HOME, String.valueOf(countUnpaidReadings(unit)),
              pointsAwarded);
    }

    /**
     * Decides a Scan point for the side and pays its value once.
     *
     * @return the points paid
     */
    private int scoreScanPoint(ObjectiveMarker scanPoint, Entity scanner, @Nullable Side side) {
        if (side == null) {
            return 0;
        }
        ObjectiveScoringScheme scheme = scanPoint.getScoringScheme();
        scheme.setSecuredBy(side.isTeam() ? side.id() : ObjectiveScoringScheme.NO_SIDE,
              side.isTeam() ? ObjectiveScoringScheme.NO_SIDE : side.id());
        scheme.setVictoryPointsAwarded(true);
        VictoryPointTracker.getTracker(getGame()).setPointDecided();
        int points = scanPoint.getVictoryPointValue();
        creditVictoryPoints(side, points, "scanned " + scanPoint.generalName());
        LOGGER.info("[Scan] {} scored {} for {} (+{} VP)", scanner.getShortName(), scanPoint.generalName(),
              displayName(side), points);
        Report report = new Report(REPORT_SCAN_POINT_SCORED, Report.PUBLIC);
        report.add(displayName(side));
        report.add(scanPoint.generalName());
        report.add(points);
        addReport(report);
        gameManager.sendGroundObjectUpdate();
        return points;
    }

    /**
     * One line per side in the initiative standings saying how many readings its units are carrying, so a player
     * can weigh a run for the edge. Called by the control resolution's standings report.
     */
    void reportReadingsCarried() {
        List<Side> sides = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Entity unit : getGame().getEntitiesVector()) {
            if (unit.getBankedScans().isEmpty()) {
                continue;
            }
            Side side = sideOf(unit.getOwner());
            if (side == null) {
                continue;
            }
            int index = sides.indexOf(side);
            if (index < 0) {
                sides.add(side);
                counts.add(unit.getBankedScans().size());
            } else {
                counts.set(index, counts.get(index) + unit.getBankedScans().size());
            }
        }
        for (int index = 0; index < sides.size(); index++) {
            Report report = new Report(REPORT_READINGS_CARRIED, Report.PUBLIC);
            report.indent();
            report.add(displayName(sides.get(index)));
            report.add(counts.get(index));
            addReport(report);
        }
    }

    private void reportCannotScan(Entity scanner, ScanAction order, String reason) {
        String targetName = (order.getTargetPosition() != null)
              ? order.getTargetPosition().getBoardNum()
              : Messages.getString("ObjectiveScan.unitNoLongerThere");
        LOGGER.info("[Scan] {} cannot scan {}: {}", scanner.getShortName(), targetName, reason);
        Report report = new Report(REPORT_CANNOT_SCAN, Report.PUBLIC);
        report.addDesc(scanner);
        report.add(targetName);
        report.add(reason);
        addReport(report);
    }

    private static String describeTarget(Targetable target) {
        if (target instanceof Entity unit) {
            return unit.getShortName();
        }
        return target.getPosition().getBoardNum();
    }

    private void creditVictoryPoints(Side side, int points, String reason) {
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(getGame());
        if (side.isTeam()) {
            tracker.awardToTeam(side.id(), points, getGame().getCurrentRound(), reason);
        } else {
            tracker.awardToPlayer(side.id(), points, getGame().getCurrentRound(), reason);
        }
    }

    private int exitTurn() {
        return getGame().getOptions().intOption(OptionsConstants.VICTORY_SCAN_EXIT_TURN);
    }

    /**
     * Core Rules p.217: the readings score when the unit leaves "via its home edge". A lobby game lets a unit flee
     * over any edge, so the home edge is read from where the side deployed: north means the north edge, a corner
     * means either of its two edges. A side deployed anywhere, on any edge or in the centre has no single home
     * edge, and any edge counts; a scenario that wants a particular edge sets the player's flee zone, which the
     * flee itself already enforces.
     *
     * @param unit the unit that fled
     *
     * @return {@code true} when the unit left over an edge that counts as home
     */
    static boolean leftOverHomeEdge(Entity unit) {
        Player owner = unit.getOwner();
        if (owner == null) {
            return true;
        }
        Set<OffBoardDirection> homeEdges = homeEdgesOf(owner.getStartingPos());
        OffBoardDirection fledOver = unit.getRetreatedDirection();
        boolean directionUnknown = (fledOver == null) || (fledOver == OffBoardDirection.NONE);
        boolean noHomeEdge = homeEdges.isEmpty();
        boolean countsAsHome = noHomeEdge || directionUnknown || homeEdges.contains(fledOver);
        // At INFO because this is the line that answers "why did that score?" in a playtest log, and it is
        // written once per unit that leaves the battlefield.
        LOGGER.info("[Scan] {} left over {}; {} start position {} gives home edge(s) {}; counted as home: {}{}",
              unit.getShortName(), directionUnknown ? "an unknown edge" : fledOver, owner.getName(),
              owner.getStartingPos(), noHomeEdge ? "none" : homeEdges, countsAsHome,
              countsAsHome && (noHomeEdge || directionUnknown)
                    ? (noHomeEdge ? " (no home edge set, so any edge counts)" : " (edge unknown, so it counts)")
                    : "");
        return countsAsHome;
    }

    /**
     * @param startingPosition a {@link Board} starting position
     *
     * @return the edges that count as home for a side deployed there; empty when no single edge does
     */
    static Set<OffBoardDirection> homeEdgesOf(int startingPosition) {
        return switch (startingPosition) {
            case Board.START_N -> Set.of(OffBoardDirection.NORTH);
            case Board.START_S -> Set.of(OffBoardDirection.SOUTH);
            case Board.START_E -> Set.of(OffBoardDirection.EAST);
            case Board.START_W -> Set.of(OffBoardDirection.WEST);
            case Board.START_NE -> Set.of(OffBoardDirection.NORTH, OffBoardDirection.EAST);
            case Board.START_NW -> Set.of(OffBoardDirection.NORTH, OffBoardDirection.WEST);
            case Board.START_SE -> Set.of(OffBoardDirection.SOUTH, OffBoardDirection.EAST);
            case Board.START_SW -> Set.of(OffBoardDirection.SOUTH, OffBoardDirection.WEST);
            default -> Set.of();
        };
    }

    private static boolean isSameSide(@Nullable Player first, @Nullable Player second) {
        if ((first == null) || (second == null)) {
            return false;
        }
        if (first.getId() == second.getId()) {
            return true;
        }
        return (first.getTeam() != Player.TEAM_NONE) && (first.getTeam() == second.getTeam());
    }

    /** The scoring side of a player: its team when it has one, else the player itself. */
    private static @Nullable Side sideOf(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        boolean isTeamed = player.getTeam() != Player.TEAM_NONE;
        return new Side(isTeamed, isTeamed ? player.getTeam() : player.getId());
    }

    private String displayName(Side side) {
        if (side.isTeam()) {
            return Messages.getString("ObjectiveScan.teamName", side.id());
        }
        Player player = getGame().getPlayer(side.id());
        return (player == null) ? String.valueOf(side.id()) : player.getName();
    }

    /** Seam for tests: line of sight from the scanner to the target. */
    boolean hasLineOfSight(Entity scanner, Targetable target) {
        return LosEffects.calculateLOS(getGame(), scanner, target).canSee();
    }

    /** Seam for tests: the 2D6 of the sensor check. */
    Roll rollScanCheck() {
        return Compute.rollD6(2);
    }

    /**
     * A side identified the way the tally keys it: a team, or an unteamed player.
     *
     * @param isTeam {@code true} when {@code id} is a team id, {@code false} when it is a player id
     * @param id     the team or player id
     */
    record Side(boolean isTeam, int id) {}
}
