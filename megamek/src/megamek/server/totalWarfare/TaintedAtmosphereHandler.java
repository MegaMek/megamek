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
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.TaintedAtmosphereRules;
import megamek.common.planetaryConditions.TaintedAtmosphereRules.VehicleBreachEffect;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.logging.MMLogger;

/**
 * Resolves the Tainted and Toxic Atmospheres rules, TO:AR 6th printing p.54: what a hull breach does to a crew when
 * the air outside is fouled, the slow poisoning of troops and crews left in the open too long, and the hexes a
 * flammable atmosphere sets alight on its own.
 * <p>
 * Extracted from {@link TWGameManager} so that already very large class does not also carry these rules; it keeps only
 * thin delegators.
 */
class TaintedAtmosphereHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(TaintedAtmosphereHandler.class);

    TaintedAtmosphereHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    private AtmosphericTaint atmosphericTaint() {
        return getGame().getPlanetaryConditions().getAtmosphericTaint();
    }

    /**
     * Resolves a breach of a vehicle location that is open to a tainted or toxic atmosphere. Caustic tainted air stuns
     * the crew; caustic or radiological toxic air kills them outright, which destroys the vehicle. The location is
     * marked breached either way so the same hole is not rolled for again.
     *
     * @param tank     the vehicle whose armor was breached
     * @param location the breached location
     *
     * @return the reports describing what the air did to the crew
     */
    Vector<Report> resolveVehicleBreach(Tank tank, int location) {
        Vector<Report> reports = new Vector<>();
        VehicleBreachEffect effect = TaintedAtmosphereRules.getVehicleBreachEffect(atmosphericTaint());
        tank.setLocationStatus(location, ILocationExposureStatus.BREACHED);

        switch (effect) {
            case CREW_KILLED -> {
                LOGGER.debug("[TaintedAtmosphere] {}: {} breached in {} air - crew killed",
                      tank.getShortName(), tank.getLocationAbbr(location), atmosphericTaint());
                reports.add(new Report(7700).subject(tank.getId()).addDesc(tank));
                reports.addAll(gameManager.applyCriticalHit(tank,
                      0,
                      new CriticalSlot(0, Tank.CRIT_CREW_KILLED),
                      false,
                      0,
                      false));
            }
            case CREW_STUNNED -> {
                LOGGER.debug("[TaintedAtmosphere] {}: {} breached in {} air - crew stunned",
                      tank.getShortName(), tank.getLocationAbbr(location), atmosphericTaint());
                reports.add(new Report(7701).subject(tank.getId()).addDesc(tank));
                reports.addAll(gameManager.applyCriticalHit(tank,
                      0,
                      new CriticalSlot(0, Tank.CRIT_CREW_STUNNED),
                      false,
                      0,
                      false));
            }
            case NONE -> LOGGER.debug("[TaintedAtmosphere] {}: {} breached but {} air has no crew effect",
                  tank.getShortName(), tank.getLocationAbbr(location), atmosphericTaint());
        }
        return reports;
    }

    /**
     * Rolls to see whether a battle armor trooper whose suit was damaged is killed by the air getting in, TO:AR p.54.
     * The trooper dies on a 2D6 roll of 9 or more, or 10 or more if the suit mounts HarJel, whatever armor the suit
     * has left.
     *
     * @param battleArmor the squad whose suit took damage
     * @param location    the trooper location that was damaged
     *
     * @return the reports describing the roll and its result
     */
    Vector<Report> resolveBattleArmorSuitBreach(BattleArmor battleArmor, int location) {
        Vector<Report> reports = new Vector<>();
        if (!TaintedAtmosphereRules.killsBattleArmorInDamagedSuits(atmosphericTaint())) {
            return reports;
        }
        if (battleArmor.getInternal(location) <= 0) {
            // The trooper is already dead; the air has nothing left to kill.
            return reports;
        }
        boolean hasHarJel = battleArmor.hasHarJelIn(location);
        int target = TaintedAtmosphereRules.getBattleArmorSuitBreachTarget(hasHarJel);
        Roll diceRoll = Compute.rollD6(2);

        Report report = new Report(7702);
        report.subject = battleArmor.getId();
        report.indent(3);
        report.add(battleArmor.getLocationAbbr(location));
        report.add(target);
        report.add(diceRoll);
        report.choose(diceRoll.getIntValue() >= target);
        reports.add(report);

        if (diceRoll.getIntValue() >= target) {
            LOGGER.debug("[TaintedAtmosphere] {}: trooper {} killed by {} air after suit damage (rolled {} vs {})",
                  battleArmor.getShortName(), battleArmor.getLocationAbbr(location), atmosphericTaint(),
                  diceRoll.getIntValue(), target);
            battleArmor.destroyLocation(location);
        }
        return reports;
    }

    /**
     * The END-phase pass for a tainted atmosphere: advances the exposure clocks of anyone left out in the open in
     * radiological or poisonous air, and gives hot units in flammable air the chance to set fire to their own hex.
     */
    void checkTaintedAtmosphereEffects() {
        AtmosphericTaint atmosphericTaint = atmosphericTaint();
        LOGGER.debug("[TaintedAtmosphere] End Phase pass running in {} air", atmosphericTaint);
        if (atmosphericTaint.isBreathable()) {
            return;
        }
        if (atmosphericTaint == AtmosphericTaint.RADIOLOGICAL_TAINTED) {
            checkAtmosphericExposure();
        }
        if (TaintedAtmosphereRules.causesSpontaneousIgnition(atmosphericTaint)) {
            checkSpontaneousIgnition();
        }
    }

    /**
     * Advances the exposure clock of every conventional infantry platoon and unsealed vehicle standing in radiological
     * or poisonous tainted air, TO:AR p.54. A platoon that has been out in it for more than 30 turns takes 1D6 damage
     * every round after that; a vehicle without Environmental Sealing loses its crew after 90 turns.
     */
    private void checkAtmosphericExposure() {
        for (Entity entity : getGame().inGameTWEntities()) {
            if (entity.isDestroyed() || entity.isDoomed() || entity.isOffBoard()) {
                continue;
            }
            if (TaintedAtmosphereRules.isShelteredFromAtmosphere(entity)) {
                continue;
            }
            if (entity.isConventionalInfantry()) {
                advanceInfantryExposure(entity);
            } else if ((entity instanceof Tank tank) && !tank.hasEnvironmentalSealing()) {
                advanceVehicleExposure(tank);
            }
        }
    }

    /**
     * Advances one platoon's exposure clock and applies the per-round damage once it has run out, TO:AR p.54. The
     * damage is applied as though it came from another infantry unit, so it is neither reduced by the Non-Infantry
     * Weapon Damage Against Infantry Table nor doubled again by the atmosphere.
     *
     * @param infantry the platoon standing in the open
     */
    private void advanceInfantryExposure(Entity infantry) {
        int turnsExposed = infantry.advanceTaintedAtmosphereExposure();
        if (turnsExposed <= TaintedAtmosphereRules.INFANTRY_FIELD_EXPOSURE_LIMIT_TURNS) {
            return;
        }
        int damage = Compute.d6(TaintedAtmosphereRules.INFANTRY_EXPOSURE_DAMAGE_DICE);
        LOGGER.info("[TaintedAtmosphere] {}: {} turns in the open, taking {} damage from the air",
              infantry.getShortName(), turnsExposed, damage);

        Report report = new Report(7705);
        report.subject = infantry.getId();
        report.addDesc(infantry);
        report.add(turnsExposed);
        report.add(damage);
        addReport(report);

        HitData hit = infantry.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
        hit.setIgnoreInfantryDoubleDamage(true);
        addReport(gameManager.damageEntity(infantry, hit, damage));
    }

    /**
     * Advances one unsealed vehicle's exposure clock and kills its crew once it has run out, TO:AR p.54.
     *
     * @param tank the vehicle standing in the open
     */
    private void advanceVehicleExposure(Tank tank) {
        int turnsExposed = tank.advanceTaintedAtmosphereExposure();
        if (turnsExposed <= TaintedAtmosphereRules.VEHICLE_FIELD_EXPOSURE_LIMIT_TURNS) {
            return;
        }
        if (tank.getCrew().isDead()) {
            return;
        }
        LOGGER.info("[TaintedAtmosphere] {}: {} turns in the open without Environmental Sealing, crew killed",
              tank.getShortName(), turnsExposed);

        Report report = new Report(7706);
        report.subject = tank.getId();
        report.addDesc(tank);
        report.add(turnsExposed);
        addReport(report);
        addReport(gameManager.applyCriticalHit(tank,
              0,
              new CriticalSlot(0, Tank.CRIT_CREW_KILLED),
              false,
              0,
              false));
    }

    /**
     * Gives every heat-tracking unit at heat 15 or higher a chance to set fire to the hex it is standing in, on a 2D6
     * roll of 10 or more, TO:AR p.54.
     */
    private void checkSpontaneousIgnition() {
        if (!getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE)) {
            LOGGER.debug("[TaintedAtmosphere] spontaneous ignition skipped: the fire game option is switched off");
            return;
        }
        int heatTrackingUnits = 0;
        int unitsHotEnough = 0;
        int unitsRolled = 0;
        for (Entity entity : getGame().inGameTWEntities()) {
            if (entity.tracksHeat()) {
                heatTrackingUnits++;
                if (entity.getHeat() >= TaintedAtmosphereRules.SPONTANEOUS_IGNITION_HEAT_THRESHOLD) {
                    unitsHotEnough++;
                }
            }
            if (!isSpontaneousIgnitionCandidate(entity)) {
                continue;
            }
            unitsRolled++;
            Roll diceRoll = Compute.rollD6(2);
            Report report = new Report(7707);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(entity.getHeat());
            report.add(TaintedAtmosphereRules.SPONTANEOUS_IGNITION_TARGET);
            report.add(entity.getPosition().getBoardNum());
            report.add(diceRoll);
            report.choose(diceRoll.getIntValue() >= TaintedAtmosphereRules.SPONTANEOUS_IGNITION_TARGET);
            addReport(report);

            if (diceRoll.getIntValue() >= TaintedAtmosphereRules.SPONTANEOUS_IGNITION_TARGET) {
                LOGGER.info("[TaintedAtmosphere] {} sets its own hex {} alight at {} heat",
                      entity.getShortName(), entity.getPosition(), entity.getHeat());
                gameManager.ignite(entity.getPosition(),
                      entity.getBoardId(),
                      Terrains.FIRE_LVL_NORMAL,
                      gameManager.getMainPhaseReport());
            }
        }
        // Summarised after the loop rather than logged per unit, so a quiet End Phase can still be explained without
        // a line for every cool Mek on the board.
        LOGGER.debug("[TaintedAtmosphere] spontaneous ignition: {} heat-tracking unit(s), {} at {}+ heat, {} rolled",
              heatTrackingUnits, unitsHotEnough, TaintedAtmosphereRules.SPONTANEOUS_IGNITION_HEAT_THRESHOLD,
              unitsRolled);
    }

    /**
     * Whether one unit could set fire to its own hex this End Phase. Each failing condition is logged separately so a
     * playtest log can answer why a given unit was or was not rolled for.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if this unit should roll for spontaneous ignition
     */
    private boolean isSpontaneousIgnitionCandidate(Entity entity) {
        if (entity.isDestroyed() || entity.isDoomed() || entity.isOffBoard()) {
            return false;
        }
        if (!entity.tracksHeat()) {
            return false;
        }
        if (entity.getHeat() < TaintedAtmosphereRules.SPONTANEOUS_IGNITION_HEAT_THRESHOLD) {
            return false;
        }
        Coords position = entity.getPosition();
        if (position == null) {
            return false;
        }
        if (entity.isAirborne() || (entity.getElevation() > 0)) {
            LOGGER.debug("[TaintedAtmosphere] {}: not rolling for spontaneous ignition - it is not on the ground",
                  entity.getShortName());
            return false;
        }
        Hex hex = getGame().getHex(position, entity.getBoardId());
        if ((hex == null) || !hex.isIgnitable()) {
            LOGGER.debug("[TaintedAtmosphere] {}: not rolling for spontaneous ignition - hex {} will not burn",
                  entity.getShortName(), position);
            return false;
        }
        return true;
    }

    /**
     * Applies the extra crew hit a caustic tainted atmosphere inflicts when the Cockpit or Crew location is damaged in
     * combat, TO:AR p.54. The air gets into the damaged compartment and burns whoever is inside it.
     *
     * @param entity the unit whose cockpit or crew location was hit
     *
     * @return the reports describing the extra hit, empty when the air does not cause one
     */
    Vector<Report> resolveExtraCockpitCrewHit(Entity entity) {
        Vector<Report> reports = new Vector<>();
        if (!TaintedAtmosphereRules.causesExtraCockpitCrewHit(atmosphericTaint())) {
            return reports;
        }
        if (entity.getCrew().isDead()) {
            return reports;
        }
        LOGGER.debug("[TaintedAtmosphere] {}: cockpit damaged in caustic air - one extra crew hit",
              entity.getShortName());
        reports.add(new Report(7710).subject(entity.getId()).addDesc(entity));
        reports.addAll(gameManager.damageCrew(entity, 1));
        return reports;
    }

    /**
     * Rolls to set fire to a hex a unit jumps out of or lands in, TO:AR p.54. In flammable toxic air the jump exhaust
     * always lights the hex; in flammable tainted air it does so on a 2D6 roll of 7 or more, or 9 or more when the
     * jumping unit is a conventional infantry platoon.
     *
     * @param entity      the jumping unit
     * @param coords      the hex it lifts off from or lands in
     * @param boardId     the board that hex is on
     * @param phaseReport the report vector for this phase
     */
    void checkJumpIgnition(Entity entity, Coords coords, int boardId, Vector<Report> phaseReport) {
        AtmosphericTaint atmosphericTaint = atmosphericTaint();
        if (!atmosphericTaint.isFlammable()) {
            return;
        }
        if (!getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE)) {
            LOGGER.debug("[TaintedAtmosphere] {}: jump ignition skipped - the fire game option is switched off",
                  entity.getShortName());
            return;
        }
        Hex hex = getGame().getHex(coords, boardId);
        if ((hex == null) || !hex.isIgnitable()) {
            LOGGER.debug("[TaintedAtmosphere] {}: jump does not ignite {} - the hex will not burn",
                  entity.getShortName(), coords);
            return;
        }

        if (TaintedAtmosphereRules.jumpJetsAlwaysIgnite(atmosphericTaint)) {
            LOGGER.info("[TaintedAtmosphere] {}: jump exhaust sets {} alight automatically",
                  entity.getShortName(), coords);
            phaseReport.add(new Report(7711).subject(entity.getId())
                  .addDesc(entity)
                  .add(coords.getBoardNum()));
            gameManager.ignite(coords, boardId, Terrains.FIRE_LVL_NORMAL, phaseReport);
            return;
        }

        int target = TaintedAtmosphereRules.getJumpIgnitionTarget(entity.isConventionalInfantry());
        Roll diceRoll = Compute.rollD6(2);
        Report report = new Report(7712);
        report.subject = entity.getId();
        report.addDesc(entity);
        report.add(target);
        report.add(coords.getBoardNum());
        report.add(diceRoll);
        report.choose(diceRoll.getIntValue() >= target);
        phaseReport.add(report);

        if (diceRoll.getIntValue() >= target) {
            LOGGER.info("[TaintedAtmosphere] {}: jump sets {} alight (rolled {} vs {})",
                  entity.getShortName(), coords, diceRoll.getIntValue(), target);
            gameManager.ignite(coords, boardId, Terrains.FIRE_LVL_NORMAL, phaseReport);
        }
    }

    /**
     * Rolls for the wash of exhaust a jet-propelled craft leaves behind it when it takes off or lands, TO:AR p.54.
     * In a flammable atmosphere one 2D6 roll is made - at the start of a takeoff or the end of a landing - and on a 6
     * or better every hex in the craft's rear arc out to two hexes catches fire.
     * <p>
     * In toxic air the craft cannot take off at all, so in practice this is a landing rule there; see
     * {@link TaintedAtmosphereRules#causesExhaustWashIgnition} for why it applies at both strengths.
     *
     * @param aeroEntity the craft taking off or landing
     * @param coords     the hex it takes off from or comes to rest in
     * @param boardId    the board that hex is on
     * @param facing     the facing the craft has at that moment, which decides where its rear arc lies
     */
    void checkExhaustWashIgnition(Entity aeroEntity, Coords coords, int boardId, int facing) {
        if (!TaintedAtmosphereRules.causesExhaustWashIgnition(atmosphericTaint())) {
            return;
        }
        if (!getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE)) {
            LOGGER.debug("[TaintedAtmosphere] {}: exhaust wash skipped - the fire game option is switched off",
                  aeroEntity.getShortName());
            return;
        }
        if (coords == null) {
            LOGGER.debug("[TaintedAtmosphere] {}: exhaust wash skipped - the craft has no position",
                  aeroEntity.getShortName());
            return;
        }
        List<Coords> washedHexes = exhaustWashHexes(coords, boardId, facing);
        if (washedHexes.isEmpty()) {
            LOGGER.debug("[TaintedAtmosphere] {}: exhaust wash at {} finds nothing behind it that will burn",
                  aeroEntity.getShortName(), coords);
            return;
        }

        Roll diceRoll = Compute.rollD6(2);
        Report report = new Report(7720);
        report.subject = aeroEntity.getId();
        report.addDesc(aeroEntity);
        report.add(TaintedAtmosphereRules.EXHAUST_WASH_IGNITION_TARGET);
        report.add(coords.getBoardNum());
        report.add(diceRoll);
        report.choose(diceRoll.getIntValue() >= TaintedAtmosphereRules.EXHAUST_WASH_IGNITION_TARGET);
        gameManager.getMainPhaseReport().add(report);

        if (diceRoll.getIntValue() < TaintedAtmosphereRules.EXHAUST_WASH_IGNITION_TARGET) {
            return;
        }
        LOGGER.info("[TaintedAtmosphere] {}: exhaust wash at {} sets {} hex(es) in its rear arc alight",
              aeroEntity.getShortName(), coords, washedHexes.size());
        for (Coords washedHex : washedHexes) {
            gameManager.ignite(washedHex, boardId, Terrains.FIRE_LVL_NORMAL, gameManager.getMainPhaseReport());
        }
    }

    /**
     * The hexes a craft's exhaust washes over: everything in its rear arc within two hexes that is on the board, is
     * not already burning and will take a fire at all.
     *
     * @param coords  the hex the craft occupies
     * @param boardId the board that hex is on
     * @param facing  the craft's facing
     *
     * @return the hexes that should be set alight, which may be empty
     */
    private List<Coords> exhaustWashHexes(Coords coords, int boardId, int facing) {
        List<Coords> washedHexes = new ArrayList<>();
        for (Coords candidate : TaintedAtmosphereRules.getExhaustWashCoords(coords, facing)) {
            Hex hex = getGame().getHex(candidate, boardId);
            boolean isOnTheBoard = hex != null;
            boolean isAlreadyBurning = isOnTheBoard && hex.containsTerrain(Terrains.FIRE);
            if (isOnTheBoard && !isAlreadyBurning && hex.isIgnitable()) {
                washedHexes.add(candidate);
            }
        }
        return washedHexes;
    }

    /**
     * Rolls for an accidental fire in the hex a weapon attack was aimed at. A flammable atmosphere is close enough to
     * catching light that every weapon attack on a non-water hex risks setting it alight, whether the shot hit or
     * missed (TO:AR p.54).
     *
     * @param attackHandler the attack that was just resolved
     * @param reports       the report vector for this phase
     */
    void checkAccidentalWeaponFire(AttackHandler attackHandler, Vector<Report> reports) {
        if (!atmosphericTaint().isFlammable()) {
            return;
        }
        WeaponAttackAction weaponAttackAction = attackHandler.getWeaponAttackAction();
        if (weaponAttackAction == null) {
            return;
        }
        Targetable target = weaponAttackAction.getTarget(getGame());
        if ((target == null) || (target.getPosition() == null)) {
            return;
        }
        if (target.isAirborne() || target.isAirborneVTOLorWIGE()) {
            return;
        }
        Hex hex = getGame().getHex(target.getPosition(), target.getBoardId());
        if (hex == null) {
            return;
        }
        if (hex.containsTerrain(Terrains.WATER)) {
            LOGGER.debug("[TaintedAtmosphere] no accidental fire check at {}: the hex is water", target.getPosition());
            return;
        }
        WeaponType weaponType = getAttackWeaponType(attackHandler);
        TargetRoll fireRoll = getWeaponFireTargetRoll(attackHandler);
        if ((weaponType == null) || (fireRoll == null)) {
            return;
        }
        boolean startedFire = gameManager.tryIgniteHex(target.getPosition(),
              target.getBoardId(),
              attackHandler.getAttackerId(),
              false,
              false,
              fireRoll,
              TaintedAtmosphereRules.ACCIDENTAL_FIRE_CHECK_TARGET,
              reports);

        boolean isExplosiveOrdnance = weaponType.hasFlag(WeaponType.F_BALLISTIC)
              || weaponType.hasFlag(WeaponType.F_MISSILE);
        if (startedFire && isExplosiveOrdnance) {
            spreadExplosiveFire(target.getPosition(), target.getBoardId(), attackHandler.getAttackerId(), reports);
        }
    }

    /**
     * The weapon behind an attack, or {@code null} when the attack has no ordinary weapon behind it.
     *
     * @param attackHandler the attack that was just resolved
     *
     * @return the {@link WeaponType} fired, or {@code null} if there is none
     */
    private @Nullable WeaponType getAttackWeaponType(AttackHandler attackHandler) {
        Entity attacker = attackHandler.getAttacker();
        if (attacker == null) {
            return null;
        }
        Mounted<?> weapon = attacker.getEquipment(attackHandler.getWeaponAttackAction().getWeaponId());
        return (weapon instanceof WeaponMounted weaponMounted) ? weaponMounted.getType() : null;
    }

    /**
     * The ignition target roll for the weapon behind an attack, or {@code null} when that weapon cannot start fires at
     * all and so needs no accidental fire check.
     *
     * @param attackHandler the attack that was just resolved
     *
     * @return the ignition {@link TargetRoll}, or {@code null} if this weapon cannot set anything alight
     */
    private @Nullable TargetRoll getWeaponFireTargetRoll(AttackHandler attackHandler) {
        Entity attacker = attackHandler.getAttacker();
        WeaponAttackAction weaponAttackAction = attackHandler.getWeaponAttackAction();
        if (attacker == null) {
            return null;
        }
        Mounted<?> weapon = attacker.getEquipment(weaponAttackAction.getWeaponId());
        if (!(weapon instanceof WeaponMounted weaponMounted)) {
            return null;
        }
        WeaponType weaponType = weaponMounted.getType();
        int fireTargetNumber = weaponType.getFireTN();
        Mounted<?> ammo = attacker.getEquipment(weaponAttackAction.getAmmoId());
        if ((ammo != null) && (ammo.getType() instanceof AmmoType ammoType)) {
            fireTargetNumber = Math.min(fireTargetNumber, ammoType.getFireTN());
        }
        if (fireTargetNumber == TargetRoll.IMPOSSIBLE) {
            return null;
        }
        return new TargetRoll(fireTargetNumber, weaponType.getName());
    }

    /**
     * Spreads a fire started by an inferno round or explosive ordnance into every adjacent hex at once, which is what
     * flammable toxic air does to such a fire the instant it is lit, TO:AR p.54.
     *
     * @param coords      the hex that was set alight
     * @param boardId     the board that hex is on
     * @param entityId    the unit that started the fire, for reporting
     * @param phaseReport the report vector for this phase
     */
    void spreadExplosiveFire(Coords coords, int boardId, int entityId, Vector<Report> phaseReport) {
        if (!TaintedAtmosphereRules.spreadsExplosiveFiresInstantly(atmosphericTaint())) {
            return;
        }
        LOGGER.info("[TaintedAtmosphere] fire at {} spreads to every adjacent hex in flammable toxic air", coords);
        phaseReport.add(new Report(7715).subject(entityId).add(coords.getBoardNum()));
        for (int direction = 0; direction < 6; direction++) {
            Coords adjacent = coords.translated(direction);
            Hex hex = getGame().getHex(adjacent, boardId);
            if ((hex == null) || hex.containsTerrain(Terrains.FIRE) || !hex.isIgnitable()) {
                continue;
            }
            gameManager.ignite(adjacent, boardId, Terrains.FIRE_LVL_NORMAL, phaseReport);
        }
    }
}
