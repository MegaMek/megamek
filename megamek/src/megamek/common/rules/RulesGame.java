package megamek.common.rules;
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


import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.Player;
import megamek.common.battlefieldSupport.BFSSpecialType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.equipment.enums.BombType;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;

public abstract class RulesGame {

    /**
     * Allow ammo dumping.
     *
     * @return true if ammo dumping is allowed
     */
    public abstract boolean ammoDumping();

    /**
     * Is an entity eligible for a phase
     *
     * @param entity the unit being considered
     * @param phase  what phase it is in
     * @return is it eligible
     */
    public abstract boolean eligibleForPhase(Entity entity,
                                             @Nullable GamePhase phase);

    /**
     * Return the number of units to move.
     *
     * @param num_normal_turns array of normal turns
     * @param index            the current index
     * @param min              the minimum value
     * @param frontLoadOption  true if front load option is enabled
     * @return the initiative order
     */
    public abstract int getInitiativeOrder(int[] num_normal_turns,
                                           int index,
                                           int min,
                                           boolean frontLoadOption);

    /**
     * Is there a BV bump for tag?
     *
     * @param entity     The entity being considered
     * @param bvReport   the report
     * @param adjustedBV the adjusted BV so far
     * @param tagCount   how many working TAGs the unit has
     * @param onlyRearTag true if the unit's only TAG is rear-mounted, which pays half (TechManual p.315)
     * @return adjusted BV value with bump if applicable
     */
    public abstract double tagBVBump(Entity entity,
                                     CalculationReport bvReport,
                                     double adjustedBV,
                                     long tagCount,
                                     boolean onlyRearTag);

    /**
     * Adds the cost of a friendly unit's Homing bombs, chosen in the lobby or mounted and not yet dropped. They are
     * "dropped as bombs, not fired as artillery" (TO:AUE p.171) and cost the bomb's BV per TAG whether mounted or not.
     *
     * @param otherEntity the friendly unit that may carry Homing bombs
     * @param bvReport    the report
     * @param adjustedBV  the adjusted BV so far
     * @param tags        the TAGs paying, halved for a rear-only TAG
     * @return the adjusted BV
     */
    protected static double addHomingBombBV(Entity otherEntity, CalculationReport bvReport, double adjustedBV,
          double tags) {
        int homingBombs = (otherEntity instanceof IBomber asBomber)
              ? asBomber.getBombChoices().getCount(BombType.BombTypeEnum.HOMING) : 0;
        for (Mounted<?> ammo : otherEntity.getAmmo()) {
            if ((ammo.getType() instanceof BombType bomb) && (bomb.getBombType() == BombType.BombTypeEnum.HOMING)
                  && (ammo.getUsableShotsLeft() > 0)) {
                homingBombs++;
            }
        }
        BombType bomb = BombType.createBombByType(BombType.BombTypeEnum.HOMING);
        if ((homingBombs == 0) || (bomb == null)) {
            return adjustedBV;
        }
        adjustedBV += bomb.getBV(otherEntity) * homingBombs * tags;
        bvReport.addLine("- " + bomb.getName(),
              "+ " + formatForReport(tags) + " x " + homingBombs + " x " + formatForReport(bomb.getBV(otherEntity))
                    + " (" + otherEntity.getShortName() + ")",
              "= " + formatForReport(adjustedBV));
        return adjustedBV;
    }

    /**
     * Allow minefields or not
     *
     * @param toMinefields OptionsConstants.ADVANCED_MINEFIELDS
     * @return Allow in core or TO
     */
    public abstract boolean allowMinefields(boolean toMinefields);

    /**
     * Helped function for tagBVBump to get the equipment descriptor for a mounted item.
     *
     * @param mounted
     * @param entity
     * @return
     */
    public String equipmentDescriptor(Mounted<?> mounted,
                                      Entity entity) {
        if (mounted.getType() instanceof WeaponType) {
            String descriptor = mounted.getType().getShortName() +
                                " (" +
                                entity.getLocationAbbr(mounted.getLocation()) +
                                ")";
            if (mounted.isMekTurretMounted()) {
                descriptor += " (T)";
            }
            if (mounted.isRearMounted() || (mounted.getType().hasFlag(WeaponType.F_VGL)
                                            && (mounted.getFacing() >= 2)
                                            && (mounted.getFacing() <= 4))) {
                descriptor += " (R)";
            }
            return descriptor;
        } else if ((mounted.getType() instanceof MiscType) && ((MiscType) mounted.getType()).isVibroblade()) {
            return mounted.getType().getShortName() + " (" + entity.getLocationAbbr(mounted.getLocation()) + ")";
        } else if (mounted.getType() instanceof AmmoType) {
            String shortName = mounted.getType().getShortName();
            return shortName + (shortName.contains("Ammo") ? "" : " Ammo");
        } else {
            return mounted.getType().getShortName();
        }
    }

    /**
     * Is walk on deployment supported?
     *
     * @return Can they walk ok
     */
    public abstract boolean isWalkOnDeployment();

    /**
     * For use by TW to enable walk on deployment. Not used by Core
     *
     * @param walkOn should walk-on deployment be enabled
     */
    public abstract void setWalkOnDeployment(boolean walkOn);

    /**
     * Walk-on deployment. Skip deployment for eligible units Note: Bot does not get Walk-on deployment for now.
     * TODO bot walk-on deployment when enabled will remove the isBot check.
     *
     * @param entity The mek under consideration
     * @return true if they can walk on, false if they cannot
     */
    public boolean canWalkOnThisRound(Entity entity) {
        if (!isWalkOnDeployment() || entity.getGame() == null) {
            return false;
        }

        int deploymentRound = entity.getDeployRound();
        int currentRound = entity.getGame().getCurrentRound();
        int startingPos = entity.getStartingPos();

        return (!entity.isDeployed() && deploymentRound >= 0
                && deploymentRound <= currentRound
                && restrictDeploymentWidth(entity.getOwner(), startingPos));
    }

    /**
     * Should the entity be included in movement / initiative?
     *
     * @param phase  what phase is it
     * @param entity what entity is it
     * @return True by default
     */
    public boolean includeInMovement(GamePhase phase,
                                     Entity entity) {
        return true;
    }

    /**
     * This gets the deployment width. It is called from both Player and Entity
     *
     * @param player          What player's deployment area (used for bot check)
     * @param deploymentArea  what deployment area
     * @param deploymentWidth what deployment width
     * @return the updated deployment width (1 for walk-on)
     */
    public int getDeploymentWidth(Player player,
                                  int deploymentArea,
                                  int deploymentWidth) {
        if (isWalkOnDeployment()) {
            int width = 1;
            if (!restrictDeploymentWidth(player, deploymentArea)) {
                width = deploymentWidth;
            }
            return width;
        }
        return deploymentWidth;
    }

    /**
     * Checks if it should restrict the deployment width.
     *
     * @param player         The player to check
     * @param deploymentArea the deployment area
     * @return true if it should restrict the deployment width, false otherwise
     */
    public boolean restrictDeploymentWidth(@Nullable Player player,
                                           int deploymentArea) {
        if (isWalkOnDeployment() &&
            (deploymentArea != Board.START_CENTER &&
             deploymentArea != Board.START_ANY &&
             deploymentArea < Board.NUM_ZONES)) {
            return true;
        }
        return false;
    }

    /** Battlefield Support Asset TAG pricing in either ruleset (Core errata v0.2 p.1). */
    public int assetTagSurchargeBsp(BattlefieldSupportAsset asset, int skill) {
        if (!asset.hasSpecial(BFSSpecialType.TAG)) {
            return 0;
        }
        int bspPerLauncher = Math.max(0, 15 - 2 * skill);
        return homingLaunchers(asset).size() * bspPerLauncher;
    }

    /**
     * @return a description of every Arrow IV or artillery launcher in the unit's force on a unit with usable Homing
     *       ammo, and of every Asset with the Arrow IV Special, whose attacks need friendly TAG (Core p.148)
     */
    protected List<String> homingLaunchers(Entity entity) {
        List<String> launchers = new ArrayList<>();
        if ((entity.getGame() == null) || (entity.getOwner() == null)) {
            return launchers;
        }
        for (Entity otherEntity : entity.getGame().getEntitiesVector()) {
            // Every friendly launcher counts, as any friendly TAG designates for it (Core p.193)
            if ((otherEntity.getOwner() == null) || otherEntity.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            if (otherEntity instanceof BattlefieldSupportAsset asset) {
                // "ARROW2" is two launchers (Core p.149)
                int assetLaunchers = asset.findSpecial(BFSSpecialType.ARROW_IV)
                      .map(special -> special.intValue().orElse(1))
                      .orElse(0);
                for (int launcher = 0; launcher < assetLaunchers; launcher++) {
                    launchers.add("Arrow IV (" + asset.getShortName() + ")");
                }
                continue;
            }
            if (!hasUsableHomingAmmo(otherEntity)) {
                continue;
            }
            for (WeaponMounted weapon : otherEntity.getWeaponList()) {
                // A mounted Homing bomb is "dropped as a bomb, not fired as artillery" (TO:AUE p.171), priced above
                if (weapon.isBombMounted()) {
                    continue;
                }
                // A bay counts each launcher in it
                List<WeaponMounted> mounts = weapon.getBayWeapons().isEmpty() ? List.of(weapon)
                      : weapon.getBayWeapons();
                for (WeaponMounted mount : mounts) {
                    if (mount.getType().hasFlag(WeaponTypeFlag.F_ARROW_IV)
                          || mount.getType().hasFlag(WeaponTypeFlag.F_ARTILLERY)) {
                        launchers.add(mount.getType().getShortName() + " (" + otherEntity.getShortName() + ")");
                    }
                }
            }
        }
        return launchers;
    }

    private static boolean hasUsableHomingAmmo(Entity unit) {
        return unit.getAmmo().stream()
              .anyMatch(ammo -> (ammo.getUsableShotsLeft() > 0) && !(ammo.getType() instanceof BombType)
                    && ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_HOMING));
    }
}
