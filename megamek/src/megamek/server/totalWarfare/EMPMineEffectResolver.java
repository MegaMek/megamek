/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import java.util.Vector;

import megamek.common.EMPEffectFormatter;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.TemporaryECMField;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EMPEffectResult;
import megamek.common.equipment.EMPMineEffectsTable;
import megamek.common.equipment.Minefield;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.MobileStructure;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

/**
 * Resolves EMP mine detonation effects per Tactical Operations: Advanced Rules (Experimental).
 * <p>
 * EMP mines affect all units in the hex with:
 * </p>
 * <ul>
 *   <li>Infantry: 1D6 area-effect damage only</li>
 *   <li>Other units: Roll on EMP Effects Table for Interference or Shutdown</li>
 *   <li>All units: Temporary ECM bubble created in the hex</li>
 * </ul>
 */
public class EMPMineEffectResolver {
    private static final MMLogger logger = MMLogger.create(EMPMineEffectResolver.class);

    private final TWGameManager gameManager;
    private final Game game;

    /**
     * Creates a new EMP mine effect resolver.
     *
     * @param gameManager The game manager handling the game
     */
    public EMPMineEffectResolver(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Resolves the detonation of an EMP mine.
     *
     * @param minefield     The EMP minefield that detonated
     * @param triggerEntity The entity that triggered the mine
     * @param coords        The coordinates of the detonation
     *
     * @return Vector of reports describing the effects
     */
    public Vector<Report> resolveEMPMineDetonation(Minefield minefield, Entity triggerEntity, Coords coords) {
        Vector<Report> reports = new Vector<>();

        // Report the mine hit
        Report hitReport = new Report(2560);
        hitReport.subject = triggerEntity.getId();
        hitReport.add(triggerEntity.getShortName(), true);
        hitReport.add(coords.getBoardNum(), true);
        hitReport.indent();
        reports.add(hitReport);

        // Report EMP pulse
        Report pulseReport = new Report(2561);
        pulseReport.add(coords.getBoardNum());
        pulseReport.indent(2);
        reports.add(pulseReport);

        // Find all entities in the hex (including the trigger entity which may not have
        // its position updated yet during movement)
        List<Entity> entitiesInHex = getEntitiesInHex(coords, triggerEntity);

        // Apply effects to each entity
        for (Entity entity : entitiesInHex) {
            reports.addAll(applyEMPEffects(entity));
        }

        // Create temporary ECM bubble
        createTemporaryECMBubble(minefield, coords);
        Report ecmReport = new Report(2572);
        ecmReport.add(coords.getBoardNum());
        ecmReport.indent(2);
        reports.add(ecmReport);

        // TODO: When Remote Sensors, C3 Remote Sensors, and Collapsible Command Modules are
        // implemented, add code here to destroy any such equipment in the affected hex.
        // Per TO:AR, these electronics are destroyed by the EMP pulse.

        return reports;
    }

    /**
     * Gets all entities at the given coordinates, ensuring the trigger entity is included.
     *
     * @param coords        The coordinates to check
     * @param triggerEntity The entity that triggered the mine (always included if not destroyed)
     *
     * @return List of entities in the hex
     */
    private List<Entity> getEntitiesInHex(Coords coords, Entity triggerEntity) {
        List<Entity> entities = new ArrayList<>();

        // Always include the trigger entity first (its position may not be updated yet during movement)
        if ((triggerEntity != null) && !triggerEntity.isDestroyed() && !triggerEntity.isDoomed()) {
            entities.add(triggerEntity);
        }

        // Add any other entities in the hex
        for (Entity entity : game.getEntitiesVector()) {
            if (coords.equals(entity.getPosition()) && !entity.isDestroyed() && !entity.isDoomed()) {
                // Don't add the trigger entity twice
                if ((triggerEntity == null) || (entity.getId() != triggerEntity.getId())) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }

    /**
     * Applies EMP effects to a single entity.
     */
    private Vector<Report> applyEMPEffects(Entity entity) {
        Vector<Report> reports = new Vector<>();

        // Check for immunity
        if (isImmuneToEMP(entity)) {
            Report immuneReport = createImmunityReport(entity);
            immuneReport.indent(2);
            reports.add(immuneReport);
            return reports;
        }

        // Infantry only takes area-effect damage
        if (entity.isConventionalInfantry()) {
            reports.addAll(applyInfantryDamage(entity));
            return reports;
        }

        // Roll for EMP effect
        EMPEffectResult result = EMPMineEffectsTable.rollForEffect(entity);
        reports.addAll(reportEffectRoll(entity, result));

        // Apply the effect
        if (result.isInterference()) {
            applyInterferenceEffect(entity, result.durationTurns());
            reports.add(createInterferenceReport(entity, result.durationTurns()));
        } else if (result.isShutdown()) {
            reports.addAll(applyShutdownEffect(entity, result.durationTurns()));
        }

        return reports;
    }

    /**
     * Checks if an entity is immune to EMP effects.
     * <p>
     * Immune units:
     * </p>
     * <ul>
     *   <li>Mobile Structures</li>
     *   <li>Units over 200 tons</li>
     *   <li>Airborne aerospace units</li>
     * </ul>
     */
    private boolean isImmuneToEMP(Entity entity) {
        // Mobile Structures are immune
        if (entity instanceof MobileStructure) {
            return true;
        }

        // Units over 200 tons are immune
        if (entity.getWeight() > 200) {
            return true;
        }

        // Airborne aerospace units are immune (grounded aerospace can be affected)
        if (entity.isAero() && entity.isAirborne()) {
            return true;
        }

        return false;
    }

    /**
     * Creates an immunity report for the given entity.
     */
    private Report createImmunityReport(Entity entity) {
        if ((entity instanceof MobileStructure) || entity.getWeight() > 200) {
            Report report = new Report(2562);
            report.subject = entity.getId();
            report.add(entity.getShortName(), true);
            return report;
        } else if (entity.isAero() && entity.isAirborne()) {
            Report report = new Report(2563);
            report.subject = entity.getId();
            report.add(entity.getShortName(), true);
            return report;
        }
        // Fallback
        Report report = new Report(2562);
        report.subject = entity.getId();
        report.add(entity.getShortName(), true);
        return report;
    }

    /**
     * Applies area-effect damage to conventional infantry.
     */
    private Vector<Report> applyInfantryDamage(Entity entity) {
        Vector<Report> reports = new Vector<>();

        int damage = Compute.d6();
        Report damageReport = new Report(2570);
        damageReport.subject = entity.getId();
        damageReport.add(damage);
        damageReport.indent(2);
        reports.add(damageReport);

        // Apply the damage
        HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
        reports.addAll(gameManager.damageEntity(entity, hit, damage));

        return reports;
    }

    /**
     * Reports the EMP effect roll result.
     */
    private Vector<Report> reportEffectRoll(Entity entity, EMPEffectResult result) {
        Vector<Report> reports = new Vector<>();

        // Use shared formatter for consistent effect display
        String formattedEffect = EMPEffectFormatter.formatEffect(result.effect());
        String formattedRoll = EMPEffectFormatter.formatRoll(result.rollValue(), result.modifier(), "drone");

        Report rollReport = new Report(2565);
        rollReport.subject = entity.getId();
        rollReport.add(formattedRoll);
        rollReport.add(formattedEffect);
        rollReport.indent(2);
        reports.add(rollReport);

        return reports;
    }

    /**
     * Applies interference effect to an entity.
     */
    private void applyInterferenceEffect(Entity entity, int durationTurns) {
        // Interference causes +2 to-hit and +5 heat/turn for Meks/Aero
        boolean causesHeat = entity.isMek() || entity.isAero();
        entity.setEMPInterference(durationTurns, causesHeat);
    }

    /**
     * Creates an interference effect report.
     */
    private Report createInterferenceReport(Entity entity, int durationTurns) {
        Report report = new Report(2567);
        report.subject = entity.getId();
        report.add(entity.getShortName(), true);
        report.add(durationTurns);
        report.indent(2);
        return report;
    }

    /**
     * Applies shutdown effect to an entity.
     */
    private Vector<Report> applyShutdownEffect(Entity entity, int durationTurns) {
        Vector<Report> reports = new Vector<>();

        Report shutdownReport = new Report(2568);
        shutdownReport.subject = entity.getId();
        shutdownReport.add(entity.getShortName(), true);
        shutdownReport.add(durationTurns);
        shutdownReport.indent(2);
        reports.add(shutdownReport);

        // Apply shutdown with duration tracking
        entity.setShutDown(true);
        entity.setShutDownThisPhase(true);
        entity.setEMPShutdownRounds(durationTurns);

        // Check for skid on hover/WiGE vehicles
        if (isHoverOrWiGE(entity)) {
            addHoverWiGESkidCheck(entity);
        }

        return reports;
    }

    /**
     * Checks if entity is a hover or WiGE vehicle.
     */
    private boolean isHoverOrWiGE(Entity entity) {
        if (entity instanceof Tank tank) {
            return tank.getMovementMode().isHover() || tank.getMovementMode().isWiGE();
        }
        return false;
    }

    /**
     * Adds a skid check for hover/WiGE vehicle shutdown.
     * <p>
     * Per TO:AR, hover/WiGE must make Drive Skill check at +3 or skid.
     * </p>
     */
    private void addHoverWiGESkidCheck(Entity entity) {
        // Per TO:AR, hover/WiGE must make Drive Skill check at +3 or skid
        PilotingRollData psr = new PilotingRollData(entity.getId(), 3, "EMP shutdown");

        // Add control roll for the vehicle - will be processed in the movement phase
        game.addControlRoll(psr);
    }

    /**
     * Creates a temporary ECM bubble at the mine location.
     */
    private void createTemporaryECMBubble(Minefield minefield, Coords coords) {
        TemporaryECMField ecmField = TemporaryECMField.fromEMPMine(
              coords,
              game.getRoundCount(),
              minefield.getPlayerId()
        );
        game.addTemporaryECMField(ecmField);
        // Send to all clients so they can display the ECM field
        try {
            gameManager.sendTemporaryECMFieldAdded(ecmField);
        } catch (Exception e) {
            logger.error("Failed to send temporary ECM field to clients", e);
        }
        logger.debug("Created temporary ECM field at {} from EMP mine", coords);
    }
}
