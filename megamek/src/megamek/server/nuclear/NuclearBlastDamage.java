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
package megamek.server.nuclear;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.MekWarrior;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Deals a nuclear detonation's blast damage to the buildings and units around Ground Zero, and the heat and piloting
 * rolls that follow it (IO:AE pp. 164-165).
 */
class NuclearBlastDamage {
    private static final MMLogger LOGGER = MMLogger.create(NuclearBlastDamage.class);

    /** Nuclear blast damage is applied in 5-point clusters (IO:AE p.165). */
    static final int BLAST_CLUSTER_SIZE = 5;
    /** Units that survive the blast roll to keep their footing or control at +2 (IO:AE p.165). */
    static final int BLAST_ROLL_MODIFIER = 2;
    /** A unit that tracks heat gains 1 heat per this much blast damage taken (IO:AE p.165). */
    static final int DAMAGE_PER_HEAT = 10;
    /** The most heat the blast can add (IO:AE p.165). */
    static final int MAXIMUM_BLAST_HEAT = 15;
    /** An airborne unit that fails its roll falls this many altitudes per point of failure (IO:AE p.165). */
    static final int ALTITUDES_LOST_PER_POINT = 2;
    private static final int BOARD_ID = 0;

    private final TWGameManager gameManager;
    private final Game game;

    NuclearBlastDamage(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Applies the blast: buildings first, then the units, then heat and the rolls of the units still standing.
     *
     * @param groundZero   the Ground Zero hex
     * @param profile      the detonation's numbers
     * @param vDesc        the report the results are added to
     * @param blastedUnits filled with the ids of the units the blast struck
     */
    void apply(Coords groundZero, NuclearBlastProfile profile, Vector<Report> vDesc, Vector<Integer> blastedUnits) {
        int[] damages = profile.blastDamageByDistance();
        // Units inside a building only take what the building did not absorb (IO:AE p.165), so its strength is
        // read before the blast hits it.
        Map<Coords, Integer> buildingStrengthBefore = damageBuildings(groundZero, damages, vDesc);

        List<Entity> carriedUnits = new ArrayList<>();
        for (Entity entity : game.inGameTWEntities()) {
            if (entity.isDestroyed() || !entity.isDeployed()) {
                continue;
            }
            if ((entity instanceof MekWarrior mekWarrior) && !mekWarrior.hasLanded()) {
                // Still in the air after ejecting
                continue;
            }
            Coords entityPosition = entity.getPosition();
            if (entityPosition == null) {
                Entity transport = game.getEntity(entity.getTransportId());
                if ((transport != null) && !transport.isAirborne()) {
                    carriedUnits.add(entity);
                }
                continue;
            }
            // An Advanced Building entity is the building at its hexes and was already damaged as a building
            if (entity instanceof AbstractBuildingEntity) {
                continue;
            }
            int range = groundZero.distance(entityPosition);
            if (range >= damages.length) {
                continue;
            }
            if (!isInBlastHeight(entity, profile)) {
                continue;
            }
            if ((range > 0) && isSheltered(entity, entityPosition, entity.relHeight(), groundZero, vDesc)) {
                continue;
            }
            int damage = damageAfterBuilding(entity, entityPosition, damages[range], buildingStrengthBefore, vDesc);
            if ((range == 0) && isAnnihilatedAtGroundZero(entity, damage, vDesc)) {
                blastedUnits.add(entity.getId());
                continue;
            }
            if (damage == 0) {
                continue;
            }
            blastedUnits.add(entity.getId());
            int damageTaken = applyClusters(entity, damage, entity.sideTable(groundZero), groundZero, vDesc);
            addBlastHeat(entity, damageTaken, vDesc);
            Report.addNewline(vDesc);
        }

        for (Entity entity : carriedUnits) {
            damageCarriedUnit(entity, groundZero, damages, vDesc, blastedUnits);
        }

        for (int unitId : blastedUnits) {
            Entity entity = game.getEntity(unitId);
            if ((entity != null) && !entity.isDoomed() && !entity.isDestroyed()) {
                rollToWithstandBlast(entity, vDesc);
            }
        }
    }

    /**
     * Damages every building hex in the blast.
     *
     * @return each damaged building hex with its CF before the blast
     */
    private Map<Coords, Integer> damageBuildings(Coords groundZero, int[] damages, Vector<Report> vDesc) {
        Map<Coords, Integer> strengthBefore = new HashMap<>();
        Enumeration<IBuilding> buildings = game.getBoard(BOARD_ID).getBuildings();
        while (buildings.hasMoreElements()) {
            IBuilding building = buildings.nextElement();
            Enumeration<Coords> buildingHexes = building.getCoords();
            while (buildingHexes.hasMoreElements()) {
                Coords coords = buildingHexes.nextElement();
                int distance = groundZero.distance(coords);
                if (distance < damages.length) {
                    strengthBefore.put(coords, building.getCurrentCF(coords));
                    Vector<Report> buildingReport = gameManager.damageBuilding(building, damages[distance], coords);
                    for (Report report : buildingReport) {
                        report.type = Report.PUBLIC;
                    }
                    vDesc.addAll(buildingReport);
                }
            }
        }
        return strengthBefore;
    }

    /**
     * Airborne units on the ground map are struck by the blast like ground units, by their ground distance from Ground
     * Zero, as long as they fly at or below the weapon's Blast Height (IO:AE p.165). VTOLs and WiGEs fly below the
     * Low-Altitude Map's first altitude, so they are always low enough.
     */
    private boolean isInBlastHeight(Entity entity, NuclearBlastProfile profile) {
        if (!entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            return true;
        }
        return entity.isAirborneAeroOnGroundMap() && (entity.getAltitude() <= profile.blastHeight());
    }

    /**
     * For units inside a building, the blast strikes the building first and the units take what is left (IO:AE
     * p.165). A Mek inside a CF 40 building hit for 60 takes 20.
     *
     * @return the damage that reaches the unit
     */
    private int damageAfterBuilding(Entity entity, Coords entityPosition, int damage,
          Map<Coords, Integer> buildingStrengthBefore, Vector<Report> vDesc) {
        Integer buildingStrength = buildingStrengthBefore.get(entityPosition);
        if ((buildingStrength == null) || !Compute.isInBuilding(game, entity)) {
            return damage;
        }
        int remaining = Math.max(0, damage - buildingStrength);
        LOGGER.info("[Nukes] {} at hex {} is inside a building of CF {}: {} of {} blast damage reaches it",
              entity.getDisplayName(), entityPosition.getBoardNum(), buildingStrength, remaining, damage);
        Report report = new Report((remaining == 0) ? 6547 : 6548);
        report.subject = entity.getId();
        report.indent(2);
        report.addDesc(entity);
        if (remaining > 0) {
            report.add(Math.min(damage, buildingStrength));
        }
        vDesc.addElement(report);
        return remaining;
    }

    /**
     * A unit at Ground Zero is annihilated unless it has enough armor in a single location to withstand the damage
     * that reaches it (IO:AE p.164). A Davy Crockett (100) annihilates a Mek, whose best location holds far less
     * than 100 armor.
     *
     * @return {@code true} if the unit was annihilated
     */
    private boolean isAnnihilatedAtGroundZero(Entity entity, int damage, Vector<Report> vDesc) {
        int bestArmor = bestLocationArmor(entity);
        if (damage <= bestArmor) {
            LOGGER.info("[Nukes] {} at Ground Zero survives: {} armor in one location withstands {} damage",
                  entity.getDisplayName(), bestArmor, damage);
            Report report = new Report(6549);
            report.subject = entity.getId();
            report.indent(2);
            report.addDesc(entity);
            report.add(damage);
            vDesc.addElement(report);
            return false;
        }
        LOGGER.info("[Nukes] {} at Ground Zero is annihilated: best location armor {} is below {} damage",
              entity.getDisplayName(), bestArmor, damage);
        vDesc.addAll(gameManager.destroyEntity(entity, "nuclear explosion ground zero", false, false));
        entity.getCrew().setDoomed(true);
        return true;
    }

    /**
     * @return the most armor the unit has in any single location, front or rear, 0 for a unit without armor
     */
    static int bestLocationArmor(Entity entity) {
        int bestArmor = 0;
        for (int location = 0; location < entity.locations(); location++) {
            bestArmor = Math.max(bestArmor, entity.getArmor(location, false));
            if (entity.hasRearArmor(location)) {
                bestArmor = Math.max(bestArmor, entity.getArmor(location, true));
            }
        }
        return bestArmor;
    }

    /**
     * Deals the damage in 5-point clusters (infantry take it as one hit) and stops once the unit is destroyed, so a
     * large blast does not keep hitting a wreck.
     *
     * @return the damage dealt before the unit was destroyed
     */
    private int applyClusters(Entity entity, int damage, int side, Coords groundZero, Vector<Report> vDesc) {
        Report report = new Report(6175);
        report.subject = entity.getId();
        report.indent(2);
        report.addDesc(entity);
        report.add(damage);
        vDesc.addElement(report);

        // Reveal hidden entity caught in the blast (per TW pg. 259)
        if (entity.isHidden()) {
            entity.setHidden(false);
            Report revealReport = new Report(9963);
            revealReport.subject = entity.getId();
            revealReport.addDesc(entity);
            vDesc.addElement(revealReport);
        }

        int table = (entity instanceof ProtoMek) ? ToHitData.HIT_SPECIAL_PROTO : ToHitData.HIT_NORMAL;
        int damageDealt = 0;
        while ((damageDealt < damage) && !entity.isDoomed() && !entity.isDestroyed()) {
            int cluster = (entity instanceof Infantry) ? damage : Math.min(BLAST_CLUSTER_SIZE, damage - damageDealt);
            HitData hit = entity.rollHitLocation(table, side);
            vDesc.addAll(gameManager.damageEntity(entity, hit, cluster, false, DamageType.IGNORE_PASSENGER, false,
                  true));
            damageDealt += cluster;
        }
        LOGGER.info("[Nukes] {} at hex {} ({} hexes from Ground Zero) takes {} of {} blast damage{}",
              entity.getDisplayName(), (entity.getPosition() == null) ? "-" : entity.getPosition().getBoardNum(),
              (entity.getPosition() == null) ? "-" : groundZero.distance(entity.getPosition()), damageDealt,
              damage, (entity.isDoomed() || entity.isDestroyed()) ? " and is destroyed" : "");
        return damageDealt;
    }

    /**
     * A unit that tracks heat gains 1 heat per 10 blast damage taken, up to 15 (IO:AE p.165). A Mek that takes 85
     * damage gains 8 heat.
     */
    private void addBlastHeat(Entity entity, int damageTaken, Vector<Report> vDesc) {
        int heat = blastHeat(entity, damageTaken);
        if (heat == 0) {
            return;
        }
        entity.changeHeatBuildup(heat, "Nuclear blast");
        Report report = new Report(6551);
        report.subject = entity.getId();
        report.indent(2);
        report.addDesc(entity);
        report.add(heat);
        vDesc.addElement(report);
    }

    /**
     * @return the heat a unit gains from taking this much blast damage, 0 for a unit that does not track heat
     */
    static int blastHeat(Entity entity, int damageTaken) {
        if (!entity.tracksHeat() || entity.isDoomed() || entity.isDestroyed()) {
            return 0;
        }
        return Math.min(MAXIMUM_BLAST_HEAT, damageTaken / DAMAGE_PER_HEAT);
    }

    /**
     * A unit carried outside its transport, or whose transport is gone, takes the blast at the transport's hex.
     */
    private void damageCarriedUnit(Entity entity, Coords groundZero, int[] damages, Vector<Report> vDesc,
          Vector<Integer> blastedUnits) {
        Entity transporter = game.getEntity(entity.getTransportId());
        if ((transporter != null) && !transporter.getExternalUnits().contains(entity)) {
            return;
        }
        Coords entityPosition = (transporter == null) ? entity.getPosition() : transporter.getPosition();
        int range = groundZero.distance(entityPosition);
        if (range >= damages.length) {
            return;
        }
        int height = (transporter == null) ? entity.relHeight() : transporter.relHeight();
        if (isSheltered(entity, entityPosition, height, groundZero, vDesc)) {
            return;
        }
        blastedUnits.add(entity.getId());
        int damageTaken = applyClusters(entity, damages[range], ToHitData.SIDE_FRONT, groundZero, vDesc);
        addBlastHeat(entity, damageTaken, vDesc);
        Report.addNewline(vDesc);
    }

    /**
     * The shelter roll: a unit behind a taller hex toward Ground Zero escapes the blast on 9+ on 2d6. Reports either
     * outcome. Unchanged from before IO:AE; the Protecting Terrain rule (IO:AE p.165) replaces it in a later change.
     *
     * @return {@code true} if the unit escapes the blast
     */
    private boolean isSheltered(Entity entity, Coords entityPosition, int height, Coords groundZero,
          Vector<Report> vDesc) {
        if (!gameManager.canShelter(entityPosition, BOARD_ID, groundZero, height)) {
            return false;
        }
        boolean sheltered = Compute.d6(2) >= 9;
        LOGGER.info("[Nukes] {} at hex {} rolls for shelter from the blast: {}", entity.getDisplayName(),
              entityPosition.getBoardNum(), sheltered ? "sheltered" : "not sheltered");
        Report report = new Report(sheltered ? 6545 : 6546);
        report.addDesc(entity);
        report.subject = entity.getId();
        vDesc.addElement(report);
        return sheltered;
    }

    /**
     * Meks, vehicles and airborne units that survive the blast roll at +2 (IO:AE p.165). A Mek that fails falls; the
     * roll joins the end-of-phase piloting rolls. A ground vehicle that fails takes motive damage, a VTOL or WiGE
     * crashes, and an aerospace unit falls 2 altitudes per point of failure and crashes if that reaches the ground.
     * Units without a Piloting Skill, such as infantry, do not roll.
     */
    private void rollToWithstandBlast(Entity entity, Vector<Report> vDesc) {
        if (entity.isAirborne()) {
            rollAirborne(entity, vDesc);
        } else if (entity.isMek() && entity.canFall()) {
            game.addPSR(new PilotingRollData(entity.getId(), BLAST_ROLL_MODIFIER, "hit by nuclear blast"));
            LOGGER.info("[Nukes] {} must make a piloting roll at +{} or fall", entity.getDisplayName(),
                  BLAST_ROLL_MODIFIER);
        } else if (entity instanceof Tank tank) {
            int marginOfFailure = rollAgainstBlast(entity, vDesc);
            if (marginOfFailure > 0) {
                vDesc.addAll(gameManager.vehicleMotiveDamage(tank, 0));
            }
        }
    }

    private void rollAirborne(Entity entity, Vector<Report> vDesc) {
        int marginOfFailure = rollAgainstBlast(entity, vDesc);
        if (marginOfFailure <= 0) {
            return;
        }
        if (entity.isAirborneVTOLorWIGE()) {
            // A VTOL or WiGE flies below the Low-Altitude Map's first altitude, so any fall puts it on the ground
            if (entity instanceof Tank tank) {
                vDesc.addAll(gameManager.crashVTOLorWiGE(tank));
            } else {
                gameManager.crashAirMek(entity, blastRoll(entity), vDesc);
            }
        } else if (entity instanceof IAero aero) {
            int altitudeLost = ALTITUDES_LOST_PER_POINT * marginOfFailure;
            entity.setAltitude(entity.getAltitude() - altitudeLost);
            Report report = new Report(6554);
            report.subject = entity.getId();
            report.indent(3);
            report.addDesc(entity);
            report.add(altitudeLost);
            vDesc.addElement(report);
            LOGGER.info("[Nukes] {} falls {} altitude(s) to altitude {}", entity.getDisplayName(), altitudeLost,
                  entity.getAltitude());
            if (gameManager.checkCrash(entity)) {
                vDesc.addAll(gameManager.processCrash(entity, aero.getCurrentVelocity(), entity.getPosition()));
            }
        }
    }

    private PilotingRollData blastRoll(Entity entity) {
        PilotingRollData target = entity.getBasePilotingRoll();
        target.addModifier(BLAST_ROLL_MODIFIER, "hit by nuclear blast");
        return target;
    }

    /**
     * Makes the roll now and reports it.
     *
     * @return the margin of failure, or 0 if the roll succeeded
     */
    private int rollAgainstBlast(Entity entity, Vector<Report> vDesc) {
        PilotingRollData target = blastRoll(entity);
        int roll = Compute.d6(2);
        boolean succeeded = roll >= target.getValue();
        Report report = new Report(succeeded ? 6552 : 6553);
        report.subject = entity.getId();
        report.indent(2);
        report.addDesc(entity);
        report.add(target.getValueAsString());
        report.add(roll);
        vDesc.addElement(report);
        LOGGER.info("[Nukes] {} rolls {} against {} to withstand the blast: {}", entity.getDisplayName(), roll,
              target.getValueAsString(), succeeded ? "succeeds" : "fails");
        if (succeeded) {
            return 0;
        }
        // An automatic failure or an impossible roll has no real margin; count it as a fall of one step
        return Math.max(1, (target.getValue() >= TargetRoll.AUTOMATIC_FAIL) ? 1 : target.getValue() - roll);
    }
}
