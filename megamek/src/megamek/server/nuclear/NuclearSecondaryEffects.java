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

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Applies the secondary effects of a nuclear detonation to the units around Ground Zero: the roll on the Secondary
 * Nuclear Effects Table and the electromagnetic interference that follows (IO:AE pp. 165-166).
 */
public class NuclearSecondaryEffects {
    private static final MMLogger LOGGER = MMLogger.create(NuclearSecondaryEffects.class);

    /** Added to the roll for combat vehicles, Meks, battle armor and aerospace craft (IO:AE p.166). */
    static final int UNIT_TYPE_BONUS = 2;
    /** Added to the roll for a unit inside a hardened structure (IO:AE p.166). */
    static final int HARDENED_STRUCTURE_BONUS = 2;
    /** The Shutdown Avoid Check target for Meks and fighters on a 5-6 (IO:AE p.166). */
    static final int SEVERE_SHUTDOWN_TARGET = 10;
    /** The Shutdown Avoid Check target for Meks and fighters on a 7-10 (IO:AE p.166). */
    static final int MODERATE_SHUTDOWN_TARGET = 6;
    /** A Mek that shuts down rolls to stay standing at +3, as for a heat shutdown. */
    private static final int SHUTDOWN_PILOTING_MODIFIER = 3;
    private static final String DESTROYED_REASON = "nuclear explosion secondary effects";

    /** The rows of the Secondary Nuclear Effects Table (IO:AE p.166). */
    enum SecondaryEffectBand {
        /** 2-4: the unit is destroyed. */
        DESTROYED(0, 0, 0),
        /** 5-6: heavy casualties, two critical hits, Shutdown Avoid Check at 10+, four pilot hits. */
        SEVERE(2, SEVERE_SHUTDOWN_TARGET, 4),
        /** 7-10: lighter casualties, one critical hit, Shutdown Avoid Check at 6+, two pilot hits. */
        MODERATE(1, MODERATE_SHUTDOWN_TARGET, 2),
        /** 11+: no damage. */
        NONE(0, 0, 0);

        private final int criticalHits;
        private final int shutdownTarget;
        private final int pilotHits;

        SecondaryEffectBand(int criticalHits, int shutdownTarget, int pilotHits) {
            this.criticalHits = criticalHits;
            this.shutdownTarget = shutdownTarget;
            this.pilotHits = pilotHits;
        }

        /**
         * @param modifiedRoll the 2d6 roll plus its bonuses
         *
         * @return the table row for that roll: a Mek rolling 5 + 2 = 7 lands on {@link #MODERATE}
         */
        static SecondaryEffectBand forRoll(int modifiedRoll) {
            if (modifiedRoll <= 4) {
                return DESTROYED;
            } else if (modifiedRoll <= 6) {
                return SEVERE;
            } else if (modifiedRoll <= 10) {
                return MODERATE;
            }
            return NONE;
        }
    }

    private final TWGameManager gameManager;
    private final Game game;

    /**
     * @param gameManager the game manager running the game the detonation happens in
     */
    public NuclearSecondaryEffects(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Rolls on the Secondary Nuclear Effects Table for one unit, applies the result and leaves the unit suffering
     * electromagnetic interference for the rest of the scenario (IO:AE pp. 165-166). The caller decides whether the
     * unit is within the secondary radius.
     *
     * @param entity     the unit
     * @param groundZero the Ground Zero hex; critical hits roll on the side facing it
     * @param vDesc      the report the results are added to
     */
    public void apply(Entity entity, Coords groundZero, Vector<Report> vDesc) {
        if (entity.isDestroyed()) {
            LOGGER.info("[Nukes] {} is already destroyed: no secondary effects roll", entity.getDisplayName());
            return;
        }

        // A carried unit rolls where its carrier is, and a transport is not a structure (HammerGS, 2026-10-26)
        boolean carried = entity.getTransportId() != Entity.NONE;
        boolean insideBuilding = !carried && Compute.isInBuilding(game, entity);
        boolean inHardenedBuilding = insideBuilding && isHardenedBuilding(entity);
        boolean unitTypeBonus = hasUnitTypeBonus(entity);
        int side = carried ? ToHitData.SIDE_FRONT : entity.sideTable(groundZero);

        Roll diceRoll = Compute.rollD6(2);
        int modifiedRoll = diceRoll.getIntValue() + rollBonus(unitTypeBonus, inHardenedBuilding);

        Report report = new Report(6555);
        report.subject = entity.getId();
        report.add(entity.getDisplayName());
        report.add(diceRoll);
        report.add(inHardenedBuilding ? " + " + HARDENED_STRUCTURE_BONUS + " (in a hardened building)" : "");
        report.add(unitTypeBonus ? " + " + UNIT_TYPE_BONUS + " (Mek, combat vehicle, battle armor or aerospace)" : "");
        report.indent(2);
        vDesc.add(report);

        SecondaryEffectBand band = SecondaryEffectBand.forRoll(modifiedRoll);
        LOGGER.info("[Nukes] {} rolls {} on the Secondary Nuclear Effects Table (unit type bonus: {}, inside a "
                    + "building: {}, hardened building: {}, carried: {}) = {}: {}", entity.getDisplayName(),
              diceRoll.getIntValue(), unitTypeBonus, insideBuilding, inHardenedBuilding, carried, modifiedRoll, band);
        applyBand(entity, band, side, insideBuilding, vDesc);

        entity.setEMI(true);
        LOGGER.info("[Nukes] {} suffers electromagnetic interference for the rest of the scenario",
              entity.getDisplayName());
    }

    /**
     * Airborne units suffer the secondary effects only at or below the weapon's Secondary Effect Height (IO:AE
     * p.165). A fighter at altitude 10 over an Alamo (height 9) escapes them; at altitude 9 it does not. VTOLs and
     * WiGEs fly below the Low-Altitude Map's first altitude, so they are always within it.
     *
     * @param entity  the unit, or the carrier of a carried unit
     * @param profile the detonation's numbers
     *
     * @return {@code true} if the unit is low enough to suffer the secondary effects
     */
    static boolean isWithinSecondaryHeight(Entity entity, NuclearBlastProfile profile) {
        // isAirborne() is only true for aerospace units, so a flying VTOL or WiGE needs its own check
        if (!entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            return true;
        }
        boolean within = entity.isAirborneAeroOnGroundMap() && (entity.getAltitude() <= profile.secondaryHeight());
        LOGGER.info("[Nukes] {} is airborne at altitude {} (ground map: {}), secondary effect height {}: {}",
              entity.getDisplayName(), entity.getAltitude(), entity.isAirborneAeroOnGroundMap(),
              profile.secondaryHeight(), within ? "affected" : "out of reach");
        return within;
    }

    /**
     * The +2 goes to combat vehicles, Meks, battle armor and aerospace craft (IO:AE p.166). Support vehicles,
     * ProtoMeks, conventional infantry and gun emplacements roll without it: a Locust gets it, a support APC does not.
     *
     * @param entity the unit
     *
     * @return {@code true} if the unit adds the unit type bonus to its roll
     */
    static boolean hasUnitTypeBonus(Entity entity) {
        if (entity.isSupportVehicle() || (entity instanceof GunEmplacement)) {
            return false;
        }
        return entity.isMek()
              || entity.isCombatVehicle()
              || (entity instanceof BattleArmor)
              || (entity instanceof Aero);
    }

    /**
     * @param unitTypeBonus      whether the unit gets the unit type bonus
     * @param inHardenedBuilding whether the unit is inside a hardened building
     *
     * @return the total added to the 2d6 roll
     */
    static int rollBonus(boolean unitTypeBonus, boolean inHardenedBuilding) {
        int bonus = 0;
        if (unitTypeBonus) {
            bonus += UNIT_TYPE_BONUS;
        }
        if (inHardenedBuilding) {
            bonus += HARDENED_STRUCTURE_BONUS;
        }
        return bonus;
    }

    private boolean isHardenedBuilding(Entity entity) {
        IBuilding building = game.getBoard(entity.getBoardId()).getBuildingAt(entity.getPosition());
        return (building != null) && (building.getBuildingType() == BuildingType.HARDENED);
    }

    /**
     * Applies one row of the table to the unit.
     *
     * @param entity         the unit
     * @param band           the table row
     * @param side           the side of the unit facing Ground Zero
     * @param insideBuilding whether the unit is inside a building
     * @param vDesc          the report the results are added to
     */
    void applyBand(Entity entity, SecondaryEffectBand band, int side, boolean insideBuilding, Vector<Report> vDesc) {
        switch (band) {
            case DESTROYED -> destroy(entity, vDesc);
            case SEVERE, MODERATE -> applyDamagingBand(entity, band, side, insideBuilding, vDesc);
            case NONE -> LOGGER.info("[Nukes] {} takes no damage from the secondary effects",
                  entity.getDisplayName());
        }
    }

    private void destroy(Entity entity, Vector<Report> vDesc) {
        vDesc.addAll(gameManager.destroyEntity(entity, DESTROYED_REASON, false, false));
        entity.getCrew().setDoomed(true);
        // The table adds 1D6 critical hits, but the unit is already destroyed and unsalvageable (HammerGS, 2026-10-26)
        LOGGER.info("[Nukes] {} is destroyed and its crew killed; the 1D6 critical hits are skipped on a destroyed "
              + "unit", entity.getDisplayName());
    }

    private void applyDamagingBand(Entity entity, SecondaryEffectBand band, int side, boolean insideBuilding,
          Vector<Report> vDesc) {
        boolean severe = band == SecondaryEffectBand.SEVERE;
        if (entity instanceof BattleArmor battleArmor) {
            // Battle armor is armored infantry: 50% or 25% casualties, rounded up
            int activeTroopers = battleArmor.getNumberActiveTroopers();
            int troopersKilled = (int) Math.ceil(activeTroopers / (severe ? 2.0 : 4.0));
            LOGGER.info("[Nukes] {} loses {} of {} troopers", entity.getDisplayName(), troopersKilled,
                  activeTroopers);
            for (int trooper = 0; trooper < troopersKilled; trooper++) {
                vDesc.addAll(gameManager.applyCriticalHit(entity, 0, null, false, 0, false));
            }
        } else if (entity instanceof Infantry infantry) {
            applyInfantryCasualties(infantry, severe, insideBuilding, vDesc);
        } else if (entity instanceof Tank) {
            // Conventional and support vehicles: critical hits plus Crew Killed or Crew Stunned
            LOGGER.info("[Nukes] {} takes {} critical hit(s) and a Crew {} critical", entity.getDisplayName(),
                  band.criticalHits, severe ? "Killed" : "Stunned");
            rollCriticalHits(entity, side, band.criticalHits, vDesc);
            int crewCritical = severe ? Tank.CRIT_CREW_KILLED : Tank.CRIT_CREW_STUNNED;
            vDesc.addAll(gameManager.applyCriticalHit(entity, 0, new CriticalSlot(0, crewCritical), false, 0, false));
        } else if ((entity instanceof Mek) || (entity instanceof ProtoMek) || (entity instanceof Aero)) {
            // Meks and fighter units; ProtoMeks take the Mek row (HammerGS, 2026-10-26)
            LOGGER.info("[Nukes] {} takes {} critical hit(s) and {} pilot hit(s)", entity.getDisplayName(),
                  band.criticalHits, band.pilotHits);
            rollCriticalHits(entity, side, band.criticalHits, vDesc);
            rollShutdownAvoid(entity, band.shutdownTarget, Compute.rollD6(2), vDesc);
            vDesc.addAll(gameManager.damageCrew(entity, band.pilotHits));
        } else {
            LOGGER.info("[Nukes] {} has no row on the table and takes no damage", entity.getDisplayName());
        }
    }

    /**
     * Unarmored infantry (damage divisor 1) outside buildings are killed on a 5-6 and lose 50% on a 7-10. Armored
     * infantry and infantry inside buildings lose 50% on a 5-6 and 25% on a 7-10 (IO:AE p.166). Casualties round up:
     * 28 troopers in flak armor lose 14 on a 5-6.
     */
    private void applyInfantryCasualties(Infantry infantry, boolean severe, boolean insideBuilding,
          Vector<Report> vDesc) {
        // Armored means a damage divisor above 1: fatigues are an armor kit with divisor 1 and count as unarmored
        // (HammerGS, 2026-10-26)
        boolean armored = (infantry instanceof ConvInfantry convInfantry) && (convInfantry.calcDamageDivisor() > 1.0);
        boolean protectedColumn = armored || insideBuilding;
        if (severe && !protectedColumn) {
            LOGGER.info("[Nukes] {} is unarmored infantry outside a building: killed", infantry.getDisplayName());
            destroy(infantry, vDesc);
            return;
        }
        double divisor = (severe || !protectedColumn) ? 2.0 : 4.0;
        int troopers = infantry.getInternal(ConvInfantry.LOC_INFANTRY);
        int casualties = (int) Math.ceil(troopers / divisor);
        int remaining = Math.max(0, troopers - casualties);
        LOGGER.info("[Nukes] {} (armored: {}, inside a building: {}) loses {} of {} troopers",
              infantry.getDisplayName(), armored, insideBuilding, casualties, troopers);
        // The casualties are already troopers, so they come straight off the trooper count. The weapon damage
        // routine would divide them by the armor divisor, which the table has already allowed for.
        infantry.setInternal(remaining, ConvInfantry.LOC_INFANTRY);
        infantry.damageThisPhase += casualties;
        Report report = new Report(6557);
        report.subject = infantry.getId();
        report.indent(3);
        report.addDesc(infantry);
        report.add(casualties);
        report.add(remaining);
        vDesc.add(report);
        if (remaining == 0) {
            destroy(infantry, vDesc);
        }
    }

    /** Critical hit locations roll on the side of the unit facing Ground Zero (IO:AE p.166). */
    private void rollCriticalHits(Entity entity, int side, int criticalHits, Vector<Report> vDesc) {
        for (int hit = 0; hit < criticalHits; hit++) {
            HitData hitData = entity.rollHitLocation(ToHitData.HIT_NORMAL, side);
            if (entity instanceof Aero aero) {
                // Rolling the location sets the aerospace critical for it, as for a takeoff critical
                vDesc.addAll(gameManager.applyCriticalHit(entity, hitData.getLocation(),
                      new CriticalSlot(0, aero.getPotCrit()), true, 0, false));
            } else {
                vDesc.addAll(gameManager.oneCriticalEntity(entity, hitData.getLocation(), hitData.isRear(), 0));
            }
        }
    }

    /**
     * The Shutdown Avoid Check for Meks and fighters (IO:AE p.166). A unit that fails restarts in a later heat phase
     * like any other shutdown (HammerGS, 2026-10-26). ProtoMeks have no shutdown rules. DropShips, JumpShips and
     * capital fighters skip it: the heat phase never restarts them, so a failed check would keep them down for the
     * rest of the game.
     *
     * @param entity   the unit
     * @param target   the number to roll to avoid shutting down
     * @param diceRoll the 2d6 roll
     * @param vDesc    the report the results are added to
     */
    void rollShutdownAvoid(Entity entity, int target, Roll diceRoll, Vector<Report> vDesc) {
        if (entity instanceof ProtoMek) {
            LOGGER.info("[Nukes] {} is a ProtoMek: no Shutdown Avoid Check", entity.getDisplayName());
            return;
        }
        if (entity.isLargeCraft() || entity.isCapitalFighter()) {
            LOGGER.info("[Nukes] {} is a large craft or capital fighter: no Shutdown Avoid Check, as nothing would "
                  + "restart it", entity.getDisplayName());
            return;
        }
        if (entity.isShutDown()) {
            LOGGER.info("[Nukes] {} is already shut down: no Shutdown Avoid Check", entity.getDisplayName());
            return;
        }
        boolean avoided = diceRoll.getIntValue() >= target;
        Report report = new Report(6556);
        report.subject = entity.getId();
        report.indent(3);
        report.addDesc(entity);
        report.add(target);
        report.add(diceRoll);
        report.choose(avoided);
        vDesc.add(report);
        LOGGER.info("[Nukes] {} needs {}+ to avoid shutdown, rolls {}: {}", entity.getDisplayName(), target,
              diceRoll.getIntValue(), avoided ? "stays running" : "shuts down");
        if (avoided) {
            return;
        }
        entity.setShutDown(true);
        if (entity.canFall()) {
            game.addPSR(new PilotingRollData(entity.getId(), SHUTDOWN_PILOTING_MODIFIER, "reactor shutdown"));
            LOGGER.info("[Nukes] {} must make a piloting roll at +{} or fall", entity.getDisplayName(),
                  SHUTDOWN_PILOTING_MODIFIER);
        }
    }
}
