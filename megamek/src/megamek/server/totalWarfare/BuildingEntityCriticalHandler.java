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

import java.util.List;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.MobileStructure;
import megamek.logging.MMLogger;

/**
 * Resolves a critical hit against an Advanced Building entity using the Advanced Building Critical Hits Table
 * (TO:AR p. 119). The check itself (a single attack exceeding a tenth of the hex's start-of-turn CF) is made by
 * {@link TWGameManager#damageBuilding}; this handler only rolls and applies the effect to the equipment in the hex
 * that was hit.
 *
 * <p>Effects are applied per hex, as the table describes.</p>
 */
class BuildingEntityCriticalHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(BuildingEntityCriticalHandler.class);

    /** On the Turret Jammed/Turret Locked result a 1D6 of this value or less jams; higher locks (TO:AR p. 119). */
    private static final int TURRET_JAM_MAX_ROLL = 3;
    private int targetLevel = -1;
    private int criticalModifier;

    BuildingEntityCriticalHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Rolls 2D6 on the Advanced Building Critical Hits Table and applies the result to the given hex.
     *
     * @param building the building that exceeded its damage threshold
     * @param coords   the board hex that was hit
     *
     * @return the reports describing the roll and its effect
     */
    Vector<Report> resolveCriticalHit(AbstractBuildingEntity building, Coords coords, int level, Entity attacker, int modifier) {
        targetLevel = building.usesExpandedCF() ? level : -1;
        criticalModifier = modifier;
        try {
            return applyCriticalResult(building, coords, Math.min(12, Compute.d6(2) + modifier), Compute.d6(), attacker);
        } finally {
            targetLevel = -1;
            criticalModifier = 0;
        }
    }

    private <T extends Mounted<?>> List<T> atTargetLevel(AbstractBuildingEntity building, List<T> equipment) {
        return equipment.stream().filter(item -> targetLevel < 0 || building.getLocationLevel(item.getLocation()) == targetLevel)
              .toList();
    }

    /**
     * Applies an already-rolled result. Exposed for tests so every row of the table can be exercised without dice.
     *
     * @param building     the building that exceeded its damage threshold
     * @param coords       the board hex that was hit
     * @param criticalRoll the 2D6 result on the Advanced Building Critical Hits Table
     * @param turretRoll   the 1D6 used only by the Turret Jammed/Turret Locked result
     *
     * @return the reports describing the roll and its effect
     */
    Vector<Report> applyCriticalResult(AbstractBuildingEntity building, Coords coords, int criticalRoll,
          int turretRoll) {
        return applyCriticalResult(building, coords, criticalRoll, turretRoll, null);
    }

    Vector<Report> applyCriticalResult(AbstractBuildingEntity building, Coords coords, int criticalRoll,
          int turretRoll, Entity attacker) {
        Vector<Report> reports = new Vector<>();
        reports.add(publicReport(3800, 0));
        if (building instanceof MobileStructure) {
            // TO:AUE p.40 rerolls an absent/destroyed category, rather than wasting the critical.
            boolean hasItem = atTargetLevel(building, building.getWeaponsAt(coords)).stream().anyMatch(this::isWorking)
                  || atTargetLevel(building, building.getAmmoAt(coords)).stream().anyMatch(this::isWorking)
                  || atTargetLevel(building, building.getMiscAt(coords)).stream().anyMatch(this::isWorking);
            if (!hasItem) {
                criticalRoll = 2;
            }
            while (criticalRoll >= 6 && !hasMobileCriticalTarget(building, coords, criticalRoll, turretRoll)) {
                criticalRoll = Math.min(12, Compute.d6(2) + criticalModifier);
                turretRoll = Compute.d6();
            }
            if (criticalRoll == 6 && turretRoll > TURRET_JAM_MAX_ROLL) {
                criticalRoll = 10;
                turretRoll = 1;
            } else if (criticalRoll == 8 && turretRoll > TURRET_JAM_MAX_ROLL) {
                criticalRoll = 10;
                turretRoll = 6;
            }
        }
        // A critical roll only happens when a single hit beat the hex's damage threshold, so this stays low-volume
        LOGGER.info("[BuildingCrit] {} hex {}: critical roll {} (turret roll {})",
              building.getShortName(), coords, criticalRoll, turretRoll);
        switch (criticalRoll) {
            case 6 -> weaponMalfunction(building, coords, reports, attacker);
            case 7 -> gunnersStunned(building, coords, reports);
            case 8 -> weaponDestroyed(building, coords, reports, attacker);
            case 9 -> gunnersKilled(building, coords, reports);
            case 10 -> turretHit(building, coords, turretRoll, reports);
            case 11 -> ammunitionExplosion(building, coords, reports);
            case 12 -> otherEquipmentHit(building, coords, reports);
            default -> reports.add(publicReport(3805, 1));
        }
        return reports;
    }

    private boolean hasMobileCriticalTarget(AbstractBuildingEntity building, Coords coords, int roll, int subroll) {
        List<WeaponMounted> weapons = atTargetLevel(building, building.getWeaponsAt(coords)).stream()
              .filter(this::isWorking).toList();
        if (roll == 10 || ((roll == 6 || roll == 8) && subroll > TURRET_JAM_MAX_ROLL)) {
            return weapons.stream().anyMatch(building::isTurretMounted);
        }
        return switch (roll) {
            case 6, 8 -> !weapons.isEmpty();
            case 7, 9 -> weapons.stream().anyMatch(weapon -> building.requiresGunner(weapon)
                  && !building.hasDeadGunners(weapon.getLocation()));
            case 11 -> atTargetLevel(building, building.getAmmoAt(coords)).stream().anyMatch(this::isWorking);
            case 12 -> atTargetLevel(building, building.getMiscAt(coords)).stream().anyMatch(this::isWorking);
            default -> true;
        };
    }

    /** Result 6: one working weapon in the hex jams until the gunners clear it. */
    private void weaponMalfunction(AbstractBuildingEntity building, Coords coords, Vector<Report> reports, Entity attacker) {
        List<WeaponMounted> candidates = atTargetLevel(building, building.getWeaponsAt(coords)).stream()
              .filter(weapon -> isWorking(weapon) && !weapon.isJammed() && !weapon.jammedThisPhase())
              .toList();
        if (candidates.isEmpty()) {
            reports.add(publicReport(3846, 1));
            return;
        }
        WeaponMounted weapon = chooseWeapon(building, candidates, attacker);
        weapon.setJammed(true);
        Report report = publicReport(3845, 1);
        report.add(weapon.getDesc());
        reports.add(report);
        LOGGER.debug("[BuildingCrit] {}: weapon malfunction, {} jammed", building.getShortName(), weapon.getName());
    }

    /**
     * Result 7: the gunners are disoriented and the hex takes no actions next turn. A hex whose gunners are
     * already dead has nobody to stun, so the result has no effect (TO:AR p. 118, Critical Hit Effects).
     */
    private void gunnersStunned(AbstractBuildingEntity building, Coords coords, Vector<Report> reports) {
        boolean gunnersDead = atTargetLevel(building, building.getWeaponsAt(coords)).stream()
              .noneMatch(weapon -> building.requiresGunner(weapon) && !building.hasDeadGunners(weapon.getLocation()));
        if (gunnersDead) {
            reports.add(publicReport(3811, 1));
            LOGGER.debug("[BuildingCrit] {}: gunners stunned in hex {} has no effect, gunners already dead",
                  building.getShortName(), coords);
            return;
        }
        building.stunGunnersAt(coords, targetLevel);
        reports.add(publicReport(3810, 1));
        LOGGER.debug("[BuildingCrit] {}: gunners stunned for {} turns", building.getShortName(),
              building.getStunnedTurns());
    }

    /** Result 8: one working weapon in the hex stops working for the rest of the scenario. */
    private void weaponDestroyed(AbstractBuildingEntity building, Coords coords, Vector<Report> reports, Entity attacker) {
        List<WeaponMounted> candidates = atTargetLevel(building, building.getWeaponsAt(coords)).stream()
              .filter(this::isWorking)
              .toList();
        if (candidates.isEmpty()) {
            reports.add(publicReport(3841, 1));
            return;
        }
        WeaponMounted weapon = chooseWeapon(building, candidates, attacker);
        int explosionDamage = weapon.getType().isExplosive(weapon) ? weapon.getExplosionDamage() : 0;
        weapon.setHit(true);
        Report report = publicReport(3840, 1);
        report.add(weapon.getDesc());
        reports.add(report);
        LOGGER.debug("[BuildingCrit] {}: weapon destroyed, {}", building.getShortName(), weapon.getName());
        if (explosionDamage > 0) {
            applyExplosion(building, coords, reports, explosionDamage);
        }
    }

    private WeaponMounted chooseWeapon(AbstractBuildingEntity building, List<WeaponMounted> candidates, Entity attacker) {
        boolean hasTurret = candidates.stream().anyMatch(building::isTurretMounted);
        boolean hasFixed = candidates.stream().anyMatch(weapon -> !building.isTurretMounted(weapon));
        if (hasTurret && hasFixed) {
            boolean turret = Compute.d6() <= 3;
            candidates = candidates.stream().filter(weapon -> building.isTurretMounted(weapon) == turret).toList();
        }
        int playerId = Compute.d6() <= 3 || attacker == null ? building.getOwnerId() : attacker.getOwnerId();
        return gameManager.chooseBuildingCriticalWeapon(building, playerId, candidates);
    }

    /** Result 9: nothing in the hex fires again this scenario. */
    private void gunnersKilled(AbstractBuildingEntity building, Coords coords, Vector<Report> reports) {
        List<WeaponMounted> weapons = atTargetLevel(building, building.getWeaponsAt(coords));
        if (weapons.stream().noneMatch(weapon -> building.requiresGunner(weapon)
              && !building.hasDeadGunners(weapon.getLocation()))) {
            reports.add(publicReport(3811, 1));
            return;
        }
        if (targetLevel < 0) {
            building.killGunnersAt(coords);
        } else {
            weapons.forEach(weapon -> building.setGunnersKilledAtLocation(weapon.getLocation(), true));
        }
        reports.add(publicReport(3815, 1));
        LOGGER.debug("[BuildingCrit] {}: gunners killed in hex {}; all gunners dead = {}", building.getShortName(),
              coords, building.allGunnersDead());
    }

    /** Result 10: turreted weapons in the hex jam (1D6 of 1 to 3) or lock in their current facing (4 to 6). */
    private void turretHit(AbstractBuildingEntity building, Coords coords, int turretRoll, Vector<Report> reports) {
        List<WeaponMounted> turretWeapons = atTargetLevel(building, building.getWeaponsAt(coords)).stream()
              .filter(weapon -> building.isTurretMounted(weapon) && isWorking(weapon) && !building.isTurretLocked(weapon))
              .toList();
        if (turretWeapons.isEmpty()) {
            reports.add(publicReport(3826, 1));
            return;
        }
        if (turretRoll <= TURRET_JAM_MAX_ROLL) {
            turretWeapons.forEach(building::jamTurretWeapon);
            reports.add(publicReport(turretWeapons.stream().anyMatch(building::isTurretLocked) ? 3820 : 3825, 1));
        } else {
            turretWeapons.forEach(building::lockTurretWeapon);
            reports.add(publicReport(3820, 1));
        }
        LOGGER.debug("[BuildingCrit] {}: turret {} in hex {} ({} weapons)", building.getShortName(),
              (turretRoll <= TURRET_JAM_MAX_ROLL) ? "jammed" : "locked", coords, turretWeapons.size());
    }

    /** Result 11: every ammunition bin in the hex explodes and the total is applied to the hex's CF. */
    private void ammunitionExplosion(AbstractBuildingEntity building, Coords coords, Vector<Report> reports) {
        int explosionDamage = 0;
        for (AmmoMounted ammo : atTargetLevel(building, building.getAmmoAt(coords))) {
            if (!isWorking(ammo)) {
                continue;
            }
            ammo.setHit(true);
            if (ammo.getType().isExplosive(ammo)) {
                AmmoType ammoType = ammo.getType();
                explosionDamage += ammo.getHittableShotsLeft() * ammoType.getDamagePerShot() * ammoType.getRackSize();
            }
        }
        applyExplosion(building, coords, reports, explosionDamage);
    }

    private void applyExplosion(AbstractBuildingEntity building, Coords coords, Vector<Report> reports, int explosionDamage) {
        boolean hasCase = atTargetLevel(building, building.getMiscAt(coords)).stream()
              .anyMatch(misc -> isWorking(misc) && misc.getType().hasFlag(MiscType.F_CASE));
        if (hasCase) {
            explosionDamage /= 10;
        }
        explosionDamage = building.scaleDamageToCF(explosionDamage);
        if (explosionDamage == 0) {
            reports.add(publicReport(3831, 1));
            return;
        }
        int level = Math.max(0, targetLevel);
        int currentCF = building.getCurrentCF(coords, level);
        currentCF -= Math.min(currentCF, explosionDamage);
        building.setCurrentCF(currentCF, coords, level);
        Report report = publicReport(3830, 1);
        report.add(building.getShortName());
        report.add(explosionDamage);
        report.add(currentCF);
        reports.add(report);
        LOGGER.info("[BuildingCrit] {}: ammunition explosion for {} in hex {}, CF now {}", building.getShortName(),
              explosionDamage, coords, currentCF);
    }

    /** Result 12: one other piece of equipment in the hex is rendered inoperative. */
    private void otherEquipmentHit(AbstractBuildingEntity building, Coords coords, Vector<Report> reports) {
        List<MiscMounted> candidates = atTargetLevel(building, building.getMiscAt(coords)).stream()
              .filter(this::isWorking)
              .toList();
        if (candidates.isEmpty()) {
            reports.add(publicReport(3835, 1));
            return;
        }
        MiscMounted equipment = candidates.get(Compute.randomInt(candidates.size()));
        equipment.setHit(true);
        Report report = publicReport(3836, 1);
        report.add(equipment.getDesc());
        reports.add(report);
        LOGGER.debug("[BuildingCrit] {}: other equipment hit, {}", building.getShortName(), equipment.getName());
    }

    /**
     * A critical result only lands on equipment that still works. A mount that took a hit this phase is out even
     * though it is not yet marked destroyed, and a mount can be destroyed, missing or useless without the hit flag,
     * for example when its hex collapsed.
     */
    private boolean isWorking(Mounted<?> mounted) {
        return !mounted.isHit() && mounted.isOperable();
    }

    private Report publicReport(int messageId, int indent) {
        Report report = new Report(messageId);
        report.type = Report.PUBLIC;
        report.indent(indent);
        return report;
    }
}
