/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions.compute;

import static megamek.common.equipment.AmmoType.AmmoTypeEnum.*;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Vector;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.LosEffects;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CrossBoardAttackHelper;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.attacks.DiveBombAttack;
import megamek.common.weapons.bayWeapons.LaserBayWeapon;
import megamek.common.weapons.bayWeapons.PPCBayWeapon;
import megamek.common.weapons.bayWeapons.PulseLaserBayWeapon;
import megamek.common.weapons.bayWeapons.ScreenLauncherBayWeapon;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;
import megamek.common.weapons.gaussRifles.GaussWeapon;

class ComputeToHitIsImpossible {

    /**
     * Method that tests each attack to see if it's impossible. If so, a reason string will be returned. A null return
     * means we can continue processing the attack
     * <p>
     * TODO: replace 40-ish parameters with an attack info record of some kind.
     *
     * @param game                  The current {@link Game}
     * @param weaponEntity              The Entity making this attack
     * @param attackerId            The ID number of the attacking entity
     * @param target                The Targetable object being attacked
     * @param targetType            The targetable object type
     * @param los                   The calculated LOS between attacker and target
     * @param losMods               ToHitData calculated from the spotter for indirect fire scenarios
     * @param toHit                 The running total ToHitData for this WeaponAttackAction
     * @param distance              The distance in hexes from attacker to target
     * @param spotter               The spotting entity for indirect fire, if present
     * @param weaponType            The WeaponType of the weapon being used
     * @param weapon                The Mounted weapon being used
     * @param ammoType              The AmmoType being used for this attack
     * @param ammo                  The Mounted ammo being used
     * @param munition              Long indicating the munition type flag being used, if applicable
     * @param isFlakAttack          Whether the attack itself counts as a flak attack
     * @param isArtilleryDirect     flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryFLAK       flag that indicates whether this is an artillery flak attack against an entity
     * @param isArtilleryIndirect   flag that indicates whether this is an indirect-fire artillery attack
     * @param isAttackerInfantry    flag that indicates whether the attacker is an infantry/BA unit
     * @param isBearingsOnlyMissile flag that indicates whether this is a bearings-only capital missile attack
     * @param isCruiseMissile       flag that indicates whether this is a cruise missile artillery attack
     * @param exchangeSwarmTarget   flag that indicates whether this is the secondary target of Swarm LRMs
     * @param isHoming              flag that indicates whether this is a homing artillery attack
     * @param isIndirect            flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isInferno             flag that indicates whether this is an inferno munition attack
     * @param isStrafing            flag that indicates whether this is an aero strafing attack
     * @param isTAG                 flag that indicates whether this is a TAG attack
     * @param targetInBuilding      flag that indicates whether the target occupies a building hex
     * @param usesAmmo              flag that indicates whether the WeaponType being used is ammo-fed
     * @param underWater            flag that indicates whether the weapon being used is underwater
     */
    static String toHitIsImpossible(Game game, Entity weaponEntity, int attackerId, Targetable target, int targetType,
          LosEffects los, ToHitData losMods, ToHitData toHit, int distance, Entity spotter, WeaponType weaponType,
          WeaponMounted weapon, int weaponId, AmmoType ammoType, AmmoMounted ammo, EnumSet<AmmoType.Munitions> munition,
          boolean isFlakAttack, boolean isArtilleryDirect, boolean isArtilleryFLAK, boolean isArtilleryIndirect,
          boolean isAttackerInfantry, boolean isBearingsOnlyMissile, boolean isCruiseMissile,
          boolean exchangeSwarmTarget, boolean isHoming, boolean isInferno, boolean isIndirect, boolean isStrafing,
          boolean isTAG, boolean targetInBuilding, boolean usesAmmo, boolean underWater, boolean evenIfAlreadyFired) {

        //Block if the weapon entity is null
        if (weaponEntity == null) {
            return Messages.getString("WeaponAttackAction.NoAttacker");
        }
        Entity attacker = weaponEntity.getAttackingEntity();
        // Block the shot if the attacker is null
        if (attacker == null) {
            return Messages.getString("WeaponAttackAction.NoAttacker");
        }
        // Or if the target is null
        if (target == null) {
            return Messages.getString("WeaponAttackAction.NoTarget");
        }
        // Without valid toHit data, the rest of this will fail
        if (toHit == null) {
            toHit = new ToHitData();
        }

        if (!game.onConnectedBoards(attacker, target)) {
            return Messages.getString("WeaponAttackAction.UnconnectedBoards");
        }

        Entity entityTarget = target instanceof Entity ? (Entity) target : null;

        // If the attacker and target are in the same building & hex, they can
        // always attack each other, TW pg 175.
        if ((los.getThruBldg() != null) && los.getTargetPosition().equals(attacker.getPosition())) {
            return null;
        }

        // got ammo?
        if (usesAmmo && ((ammo == null) || (ammo.getUsableShotsLeft() == 0))) {
            return Messages.getString("WeaponAttackAction.OutOfAmmo");
        }

        // are we bracing a location that's not where the weapon is located?
        if (attacker.isBracing() && weapon != null && (attacker.braceLocation() != weapon.getLocation())) {
            return String.format(Messages.getString("WeaponAttackAction.BracingOtherLocation"),
                  attacker.getLocationName(attacker.braceLocation()),
                  weaponEntity.getLocationName(weapon.getLocation()));
        }

        // Ammo-specific Reasons
        if (ammoType != null) {
            // Are we dumping that ammo?
            if (usesAmmo && ammo != null && ammo.isDumping()) {
                attacker.loadWeaponWithSameAmmo(weapon);
                if ((ammo.getUsableShotsLeft() == 0) || ammo.isDumping()) {
                    return Messages.getString("WeaponAttackAction.DumpingAmmo");
                }
            }

            // make sure weapon can deliver flares
            if ((target.getTargetType() == Targetable.TYPE_FLARE_DELIVER)
                  && !(usesAmmo && ammoType.getAmmoType().isAnyOf(LRM, MML, LRM_IMP, MEK_MORTAR)
                  && munition.contains(AmmoType.Munitions.M_FLARE))) {
                return Messages.getString("WeaponAttackAction.NoFlares");
            }

            // These ammo types can only target hexes for flare delivery
            if (ammoType.getAmmoType().isAnyOf(LRM, LRM_IMP, MML)
                  && ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLARE)
                  && (target.getTargetType() != Targetable.TYPE_FLARE_DELIVER)) {
                return Messages.getString("WeaponAttackAction.OnlyFlare");
            }

            // Aeros must have enough ammo for the maximum rate of fire because they cannot lower it
            if (attacker.isAero()
                  && usesAmmo
                  && ammo != null
                  && weapon != null
                  && (attacker.getTotalAmmoOfType(ammo.getType()) < weapon.getCurrentShots())) {
                return Messages.getString("WeaponAttackAction.InsufficientAmmo");
            }

            // Some Mek mortar ammo types can only be aimed at a hex
            if (weaponType != null &&
                  weaponType.hasFlag(WeaponType.F_MEK_MORTAR) &&
                  ((ammoType.getMunitionType().contains(AmmoType.Munitions.M_AIRBURST)) ||
                        (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLARE)) ||
                        (ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)))) {
                if (!(target instanceof HexTarget)) {
                    return String.format(Messages.getString("WeaponAttackAction.AmmoAtHexOnly"),
                          ammoType.getSubMunitionName());
                }
            }

            // make sure weapon can deliver minefield
            if ((target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER)
                  && !AmmoType.canDeliverMinefield(ammoType)) {
                return Messages.getString("WeaponAttackAction.NoMinefields");
            }

            // These ammo types can only target hexes for minefield delivery
            if (ammoType.getAmmoType().isAnyOf(LRM, LRM_IMP, MML, MEK_MORTAR) &&
                  ((ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER)) ||
                        (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_ACTIVE)) ||
                        (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_INFERNO)) ||
                        (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) ||
                        (ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_AUGMENTED))) &&
                  (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER)) {
                return Messages.getString("WeaponAttackAction.OnlyMinefields");
            }
        }

        // Attacker Action Reasons

        // If the attacker is actively using a shield, weapons in the same location are blocked
        if (weapon != null
              && attacker.hasShield()
              && attacker.hasActiveShield(weapon.getLocation(), weapon.isRearMounted())) {
            return Messages.getString("WeaponAttackAction.ActiveShieldBlocking");
        }

        // is the attacker even active?
        if (attacker.isShutDown() || !attacker.getCrew().isActive()) {
            return Messages.getString("WeaponAttackAction.AttackerNotReady");
        }

        // If the attacker is involved in a grapple
        if (attacker.getGrappled() != Entity.NONE) {
            int grappleOpponent = attacker.getGrappled();
            // It can only target the unit it is grappling with
            if (grappleOpponent != target.getId()) {
                return Messages.getString("WeaponAttackAction.MustTargetGrappled");
            }
            if (weapon != null) {
                int loc = weapon.getLocation();
                // Can't fire arm and leg-mounted weapons while grappling
                if (((attacker instanceof Mek mek)
                      && (attacker.getGrappleSide() == Entity.GRAPPLE_BOTH)
                      && (!mek.locationIsTorso(loc) && (loc != Mek.LOC_HEAD)))
                      || weapon.isRearMounted()) {
                    return Messages.getString("WeaponAttackAction.CantFireWhileGrappled");
                }
                // If caught by a chain whip, can't use weapons in the affected arm
                if ((attacker instanceof Mek) && (attacker.getGrappleSide() == Entity.GRAPPLE_LEFT)
                      && (loc == Mek.LOC_LEFT_ARM)) {
                    return Messages.getString("WeaponAttackAction.CantShootWhileChained");
                }
                if ((attacker instanceof Mek) && (attacker.getGrappleSide() == Entity.GRAPPLE_RIGHT)
                      && (loc == Mek.LOC_RIGHT_ARM)) {
                    return Messages.getString("WeaponAttackAction.CantShootWhileChained");
                }
            }
        }

        // can't fire weapons if loading/unloading cargo
        if (attacker.endOfTurnCargoInteraction()) {
            return Messages.getString("WeaponAttackAction.CantFireWhileLoadingUnloadingCargo");
        }

        // can't fire arm/forward facing torso weapons if carrying cargo in hands
        if ((weapon != null)) {
            if ((attacker instanceof Mek) && !weapon.isRearMounted() && !attacker.canFireWeapon(weapon.getLocation())) {
                return Messages.getString("WeaponAttackAction.CantFireWhileCarryingCargo");
            }
        }

        // Only large spacecraft can shoot while evading
        if (attacker.isEvading() && !(attacker instanceof Dropship) && !(attacker instanceof Jumpship)) {
            return Messages.getString("WeaponAttackAction.AeEvading");
        }

        // If we're laying mines, we can't shoot.
        if (attacker.isLayingMines()) {
            return Messages.getString("WeaponAttackAction.BusyLayingMines");
        }

        // Attacker prone and unable to fire?
        ToHitData ProneMods = Compute.getProneMods(game, attacker, weaponId);
        if ((ProneMods != null) && ProneMods.getValue() == ToHitData.IMPOSSIBLE) {
            return ProneMods.getDesc();
        }

        // WiGE vehicles cannot fire at 0-range targets as they fly overhead
        if ((attacker.getMovementMode() == EntityMovementMode.WIGE)
              && game.onTheSameBoard(attacker, target)
              && (attacker.getPosition().equals(target.getPosition()))) {
            return Messages.getString("WeaponAttackAction.ZeroRangeTarget");
        }

        // Crew Related Reasons

        // Stunned vehicle crews can't make attacks
        if (attacker instanceof Tank tank && tank.getStunnedTurns() > 0) {
            return Messages.getString("WeaponAttackAction.CrewStunned");
        }

        // Vehicles with a single crewman can't shoot and unjam a RAC in the same turn (like meks...)
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_TANK_CREWS)
              && (attacker instanceof Tank)
              && attacker.isUnjammingRAC()
              && (attacker.getCrew().getSize() == 1)) {
            return Messages.getString("WeaponAttackAction.VeeSingleCrew");
        }

        // Critical Damage Reasons

        // Aerospace units can't fire if the FCS/CIC is destroyed
        if (attacker instanceof Aero aero) {
            if (aero.getFCSHits() > 2) {
                return Messages.getString("WeaponAttackAction.FCSDestroyed");
            }
            // JS/WS/SS have CIC instead of FCS
            if (aero instanceof Jumpship jumpship) {
                if (jumpship.getCICHits() > 2) {
                    return Messages.getString("WeaponAttackAction.CICDestroyed");
                }
            }
        }
        // Are the sensors operational?
        // BattleMek sensors are destroyed after 2 hits, unless they have a
        // torso-mounted cockpit
        int sensorHits = attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
        if ((attacker instanceof Mek mek) && (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)) {
            sensorHits += attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_CENTER_TORSO);
            if (sensorHits > 2) {
                return Messages.getString("WeaponAttackAction.SensorsDestroyed");
            }
            // Vehicles Sensor Hits
        } else if (attacker instanceof Tank tank) {
            sensorHits = tank.getSensorHits();
            if (sensorHits >= Tank.CRIT_SENSOR) {
                return Messages.getString("WeaponAttackAction.SensorsDestroyed");
            }
            // IndustrialMeks and other unit types have destroyed sensors with 2 or more hits
        } else if ((sensorHits > 1) || ((attacker instanceof Mek mek) && (mek.isIndustrial() && (sensorHits == 1)))) {
            return Messages.getString("WeaponAttackAction.SensorsDestroyed");
        }

        // Invalid Target Reasons

        // a friendly unit can never be the target of a direct attack.
        // but we do allow vehicle flamers to cool. Also swarm missile secondary targets
        // and strafing are exempt.
        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE) &&
              !isStrafing &&
              !exchangeSwarmTarget) {
            if (entityTarget != null && !entityTarget.getOwner().isEnemyOf(attacker.getOwner())) {
                if (!(usesAmmo && ammoType != null && (ammoType.getMunitionType()
                      .contains(AmmoType.Munitions.M_COOLANT)))) {
                    return Messages.getString("WeaponAttackAction.NoFriendlyTarget");
                }
            }
        }

        // Can't fire at hidden targets
        if ((target instanceof Entity) && ((Entity) target).isHidden()) {
            return Messages.getString("WeaponAttackAction.NoFireAtHidden");
        }

        // Infantry can't clear woods.
        if (isAttackerInfantry && (Targetable.TYPE_HEX_CLEAR == target.getTargetType())) {
            Hex hexTarget = game.getHexOf(target);
            if ((hexTarget != null) && hexTarget.containsTerrain(Terrains.WOODS)) {
                return Messages.getString("WeaponAttackAction.NoInfantryWoodsClearing");
            }
        }

        // Can't target infantry with Inferno rounds (BMRr, pg. 141).
        // Also, enforce options for keeping vehicles and proto's safe
        // if those options are checked.
        if (isInferno &&
              (((entityTarget instanceof Tank) &&
                    game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_VEHICLES_SAFE_FROM_INFERNOS)) ||
                    ((entityTarget instanceof ProtoMek) &&
                          game.getOptions()
                                .booleanOption(OptionsConstants.ADVANCED_COMBAT_PROTOMEKS_SAFE_FROM_INFERNOS)))) {
            return Messages.getString("WeaponAttackAction.CantShootWithInferno");
        }

        // Only weapons allowed to clear minefields can target a hex for minefield clearance
        if ((target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR) && !AmmoType.canClearMinefield(ammoType)) {
            return Messages.getString("WeaponAttackAction.CantClearMines");
        }

        // Mine Clearance munitions can only target hexes for minefield clearance
        if (!(target instanceof HexTarget) &&
              (ammoType != null) &&
              ammoType.getMunitionType().contains(AmmoType.Munitions.M_MINE_CLEARANCE)) {
            return Messages.getString("WeaponAttackAction.MineClearHexOnly");
        }

        // Only screen launchers may target a hex for screen launch
        if (Targetable.TYPE_HEX_SCREEN == target.getTargetType()) {
            if (weaponType != null &&
                  (!((weaponType.getAmmoType() == SCREEN_LAUNCHER) ||
                        (weaponType instanceof ScreenLauncherBayWeapon)))) {
                return Messages.getString("WeaponAttackAction.ScreenLauncherOnly");
            }
        }

        // Screen Launchers can only target hexes
        if ((Targetable.TYPE_HEX_SCREEN != target.getTargetType()) &&
              (weaponType != null &&
                    ((weaponType.getAmmoType() == SCREEN_LAUNCHER) ||
                          (weaponType instanceof ScreenLauncherBayWeapon)))) {
            return Messages.getString("WeaponAttackAction.ScreenHexOnly");
        }

        // Can't target an entity conducting a swarm attack.
        if ((entityTarget != null) && (Entity.NONE != entityTarget.getSwarmTargetId())) {
            return Messages.getString("WeaponAttackAction.TargetSwarming");
        }

        // Tasers must target units and can't target flying units
        if (weaponType != null && weaponType.hasFlag(WeaponType.F_TASER)) {
            if (entityTarget != null) {
                if (entityTarget.isAirborne() || entityTarget.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.NoTaserAtAirborne");
                }
            } else {
                return Messages.getString("WeaponAttackAction.TaserOnlyAtUnit");
            }
        }

        // can't target yourself intentionally, but swarm missiles can come back to bite
        // you
        if (!exchangeSwarmTarget && attacker.equals(entityTarget)) {
            return Messages.getString("WeaponAttackAction.NoSelfTarget");
        }

        // Line of Sight and Range Reasons

        // attacker partial cover means no leg weapons
        if (los.isAttackerCover()
              && weapon != null
              && weaponEntity.locationIsLeg(weapon.getLocation())
              && !underWater) {
            return Messages.getString("WeaponAttackAction.LegBlockedByTerrain");
        }

        // Must target infantry in buildings from the inside.
        if (targetInBuilding && (entityTarget instanceof Infantry) && (null == los.getThruBldg())) {
            return Messages.getString("WeaponAttackAction.CantShootThruBuilding");
        }

        if (game.getPhase().isFiring() && !game.onTheSameBoard(attacker, target)
              && !CrossBoardAttackHelper.isCrossBoardAttackPossible(attacker, target, game)) {
            return Messages.getString("WeaponAttackAction.CantAttackOtherBoard");
        }

        // if LOS is blocked, block the shot except in the case of artillery fire
        // fall through in the targeting phase to show a more specific reason than "no LOS"
        if ((losMods.getValue() == TargetRoll.IMPOSSIBLE) && !isArtilleryIndirect && !isArtilleryDirect
              && !game.getPhase().isTargeting()) {
            return losMods.getDesc();
        }

        // If using SO advanced sensors, the firing unit or one on its NC3 network must
        // have a valid firing solution
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS) &&
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
              attacker.isSpaceborne()) {
            boolean networkFiringSolution = false;
            // Check to see if the attacker has a firing solution. Naval C3 networks share targeting data
            if (attacker.hasNavalC3()) {
                for (Entity en : game.getC3NetworkMembers(attacker)) {
                    if (entityTarget != null && en.hasFiringSolutionFor(entityTarget.getId())) {
                        networkFiringSolution = true;
                        break;
                    }
                }
            }
            if (!networkFiringSolution) {
                // If we don't check for target type here, we can't fire screens and missiles at hexes...
                if (target.getTargetType() == Targetable.TYPE_ENTITY &&
                      (entityTarget != null && !attacker.hasFiringSolutionFor(entityTarget.getId()))) {
                    return Messages.getString("WeaponAttackAction.NoFiringSolution");
                }
            }
        }

        // http://www.classicbattletech.com/forums/index.php/topic,47618.0.html
        // anything outside visual range requires a "sensor lock" in order to
        // direct fire. Note that this is for ground combat with TacOps sensors rules
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
              !attacker.isSpaceborne() &&
              !Compute.inVisualRange(game, attacker, target) &&
              !(Compute.inSensorRange(game, attacker, target, null)
                    // Can shoot at something in sensor range if it has been spotted by another unit
                    && (entityTarget != null) && entityTarget.hasSeenEntity(attacker.getOwner())) &&
              !isArtilleryIndirect &&
              !isIndirect &&
              !isBearingsOnlyMissile &&
              !isStrafing &&
              !target.isHexBeingBombed()) {
            boolean networkSee = false;
            if (attacker.hasC3() || attacker.hasC3i() || attacker.hasActiveNovaCEWS()) {
                // c3 units can fire if any other unit in their network is in visual or sensor range
                for (Entity en : game.getEntitiesVector()) {
                    // We got here because attackingEntity can't see the target, so we need a C3 buddy that _can_
                    // or there's no shot.
                    if (!en.isEnemyOf(attacker) &&
                          en.onSameC3NetworkAs(attacker) &&
                          Compute.canSee(game, en, target, false, null, null)) {
                        networkSee = true;
                        break;
                    }
                }
            }
            if (!networkSee) {
                if (!Compute.inSensorRange(game, attacker, target, null)) {
                    return Messages.getString("WeaponAttackAction.NoSensorTarget");
                } else {
                    return Messages.getString("WeaponAttackAction.TargetNotSpotted");
                }
            }
        }

        // Torpedoes must remain in the water over their whole path to the target
        if ((ammoType != null) &&
              ((ammoType.getAmmoType() == LRM_TORPEDO) ||
                    (ammoType.getAmmoType() == SRM_TORPEDO) ||
                    (((ammoType.getAmmoType() == SRM) ||
                          (ammoType.getAmmoType() == SRM_IMP) ||
                          (ammoType.getAmmoType() == MRM) ||
                          (ammoType.getAmmoType() == LRM) ||
                          (ammoType.getAmmoType() == LRM_IMP) ||
                          (ammoType.getAmmoType() == MML)) &&
                          (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)))) &&
              (los.getMinimumWaterDepth() < 1)) {
            return Messages.getString("WeaponAttackAction.TorpOutOfWater");
        }

        // arty fire from fully submerged units, TO:AR p.193
        if ((isArtilleryIndirect || isArtilleryDirect)
              && attacker.getElevation() < -attacker.getHeight()
              && game.getHexOf(attacker).hasDepth1WaterOrDeeper()
              && (weapon == null || !weapon.getType().hasFlag(WeaponTypeFlag.F_CRUISE_MISSILE))) {
            return Messages.getString("WeaponAttackAction.ArtyOutOfWater");
        }

        // Is the weapon blocked by a passenger?
        if (weapon != null && (attacker.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted()))) {
            return Messages.getString("WeaponAttackAction.PassengerBlock");
        }

        // Is the weapon blocked by a tractor/trailer?
        if (weapon != null && (attacker.getTowing() != Entity.NONE || attacker.getTowedBy() != Entity.NONE)) {
            if (attacker.isWeaponBlockedByTowing(weapon.getLocation(),
                  attacker.getSecondaryFacing(),
                  weapon.isRearMounted())) {
                return Messages.getString("WeaponAttackAction.TrailerBlock");
            }
        }

        // HHW Specific
        if ((weapon != null) && (weapon.getEntity() != attacker) && weapon.getEntity() instanceof HandheldWeapon hhw) {
            // Are any other attacks from this HHW at different targets?
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if ((ea instanceof WeaponAttackAction prevWeaponAttackAction) &&
                      (attackerId == prevWeaponAttackAction.getEntityId()) &&
                      (weaponId != prevWeaponAttackAction.getWeaponId())) {
                    WeaponMounted prevWeapon =
                          (WeaponMounted) prevWeaponAttackAction.getEntity(game)
                                .getEquipment(prevWeaponAttackAction.getWeaponId());
                    if ((prevWeapon != null)
                          && hhw.equals(prevWeapon.getEntity())
                          && prevWeaponAttackAction.getTargetId() != target.getId()) {
                        return Messages.getString("WeaponAttackAction.HandheldWeaponsMultipleTargets");
                    }
                }
            }
        }

        // Phase Reasons

        // Only bearings-only capital missiles and indirect fire artillery or equivalent (Cap and SubCap atmospheric
        // fire) can be fired in the targeting phase
        boolean isCapitalOrSubCapital = (weaponType != null) && (weaponType.isCapital() || weaponType.isSubCapital());
        if (game.getPhase().isTargeting() && (isArtilleryFLAK
              || !(isArtilleryIndirect || isBearingsOnlyMissile || isCapitalOrSubCapital))) {
            return Messages.getString("WeaponAttackAction.NotValidForTargPhase");
        }
        // Only TAG can be fired in the offboard phase
        if (game.getPhase().isOffboard() && !isTAG) {
            return Messages.getString("WeaponAttackAction.OnlyTagInOffboard");
        }
        // TAG can't be fired in any phase but offboard
        if (!game.getPhase().isOffboard() && isTAG) {
            return Messages.getString("WeaponAttackAction.TagOnlyInOffboard");
        }

        // Unit-specific Reasons

        // Airborne units cannot tag and attack
        // http://bg.battletech.com/forums/index.php?topic=17613.new;topicseen#new
        // Rules seem to allow multiple TAG attempts but not TAG + any other attacks
        if (attacker.isAirborne() && !isTAG && attacker.usedTag()) {
            return Messages.getString("WeaponAttackAction.AeroCantTAGAndShoot");
        }

        // Hull Down

        // Hull down meks cannot fire any leg weapons
        if (attacker.isHullDown()
              && attacker instanceof Mek mek
              && weapon != null
              && mek.locationIsLeg(weapon.getLocation())) {
            return Messages.getString("WeaponAttackAction.NoLegHullDown");
        }

        // hull down vees can't fire front weapons unless indirect
        if ((attacker instanceof Tank)
              && attacker.isHullDown()
              && (weapon != null)
              && (weapon.getLocation() == Tank.LOC_FRONT)
              && !isIndirect) {
            return Messages.getString("WeaponAttackAction.FrontBlockedByTerrain");
        }

        // LAMs in fighter mode are restricted to only the ammo types that Aeros can use
        if ((attacker instanceof LandAirMek)
              && (attacker.getConversionMode() == LandAirMek.CONV_MODE_FIGHTER)
              && usesAmmo
              && (ammo != null)
              && !ammo.getType().canAeroUse(
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_ARTILLERY_MUNITIONS))) {
            return Messages.getString("WeaponAttackAction.InvalidAmmoForFighter");
        }

        // LAMs carrying certain types of bombs that require a weapon have attacks that cannot be used in mek mode
        if ((attacker instanceof LandAirMek)
              && (attacker.getConversionMode() == LandAirMek.CONV_MODE_MEK)
              && weaponType != null
              && weaponType.hasFlag(WeaponType.F_BOMB_WEAPON)
              && weaponType.getAmmoType() != RL_BOMB
              && !weaponType.hasFlag(WeaponType.F_TAG)) {
            return Messages.getString("WeaponAttackAction.NoBombInMekMode");
        }

        // limit large craft to zero net heat and to heat by arc
        final int heatCapacity = attacker.getHeatCapacity();
        if (attacker.isLargeCraft() && (weapon != null)) {
            int totalHeat = 0;

            // first check to see if there are any usable bay weapons
            if (!weapon.getBayWeapons().isEmpty()) {
                boolean usable = false;
                for (WeaponMounted m : weapon.getBayWeapons()) {
                    WeaponType bayWType = m.getType();
                    boolean bayWUsesAmmo = (bayWType.getAmmoType() != NA);
                    if (m.canFire()) {
                        if (bayWUsesAmmo) {
                            if ((m.getLinked() != null) && (m.getLinked().getUsableShotsLeft() > 0)) {
                                usable = true;
                                break;
                            }
                        } else {
                            usable = true;
                            break;
                        }
                    }
                }
                if (!usable) {
                    return Messages.getString("WeaponAttackAction.BayNotReady");
                }
            }

            // create an array of booleans of locations
            boolean[] usedFrontArc = new boolean[attacker.locations()];
            boolean[] usedRearArc = new boolean[attacker.locations()];
            for (int i = 0; i < attacker.locations(); i++) {
                usedFrontArc[i] = false;
                usedRearArc[i] = false;
            }

            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction prevAttack)) {
                    continue;
                }
                // Strafing attacks only count heat for first shot
                if (prevAttack.isStrafing() && !prevAttack.isStrafingFirstShot()) {
                    continue;
                }
                if ((prevAttack.getEntityId() == attackerId) && (weaponId != prevAttack.getWeaponId())) {
                    WeaponMounted prevWeapon = (WeaponMounted) attacker.getEquipment(prevAttack.getWeaponId());
                    if (prevWeapon != null) {
                        int loc = prevWeapon.getLocation();
                        boolean rearMount = prevWeapon.isRearMounted();
                        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                            totalHeat += prevWeapon.getHeatByBay();
                        } else {
                            if (!rearMount) {
                                if (!usedFrontArc[loc]) {
                                    totalHeat += attacker.getHeatInArc(loc, rearMount);
                                    usedFrontArc[loc] = true;
                                }
                            } else {
                                if (!usedRearArc[loc]) {
                                    totalHeat += attacker.getHeatInArc(loc, rearMount);
                                    usedRearArc[loc] = true;
                                }
                            }
                        }
                    }
                }
            }

            // now check the current heat
            int loc = weapon.getLocation();
            boolean rearMount = weapon.isRearMounted();
            int currentHeat = attacker.getHeatInArc(loc, rearMount);
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                currentHeat = 0;
                currentHeat += weapon.getHeatByBay();
            }
            // check to see if this is currently the only arc being fired
            boolean onlyArc = true;
            for (int nLoc = 0; nLoc < attacker.locations(); nLoc++) {
                if (nLoc == loc) {
                    continue;
                }
                if (usedFrontArc[nLoc] || usedRearArc[nLoc]) {
                    onlyArc = false;
                    break;
                }
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                if ((totalHeat + currentHeat) > heatCapacity) {
                    // FIXME: This is causing weird problems (try firing all the Suffen's nose weapons)
                    return Messages.getString("WeaponAttackAction.HeatOverCap");
                }
            } else {
                if (!rearMount) {
                    if (!usedFrontArc[loc] && ((totalHeat + currentHeat) > heatCapacity) && !onlyArc) {
                        return Messages.getString("WeaponAttackAction.HeatOverCap");
                    }
                } else {
                    if (!usedRearArc[loc] && ((totalHeat + currentHeat) > heatCapacity) && !onlyArc) {
                        return Messages.getString("WeaponAttackAction.HeatOverCap");
                    }
                }
            }
        } else if (attacker instanceof Dropship) {
            int totalHeat = 0;

            for (EntityAction action : game.getActionsVector()) {
                if (action instanceof WeaponAttackAction otherAttack) {
                    if ((otherAttack.getEntityId() == attackerId) && (weaponId != otherAttack.getWeaponId())) {
                        Mounted<?> prevWeapon = attacker.getEquipment(otherAttack.getWeaponId());
                        totalHeat += prevWeapon.getCurrentHeat();
                    }
                }
            }

            if (weapon != null && ((totalHeat + weapon.getCurrentHeat()) > heatCapacity)) {
                return Messages.getString("WeaponAttackAction.HeatOverCap");
            }
        }

        // ProtoMeks can't fire energy weapons while charging EDP armor
        if ((attacker instanceof ProtoMek protoMek)
              && protoMek.isEDPCharging()
              && (weaponType != null)
              && weaponType.hasFlag(WeaponType.F_ENERGY)) {
            return Messages.getString("WeaponAttackAction.ChargingEDP");
        }

        // for spheroid dropships in atmosphere (and on ground), the rules about
        // firing arcs are more complicated
        // TW errata 2.1

        if ((Compute.useSpheroidAtmosphere(game, attacker)
              || (attacker.isAero() && attacker.isSpheroid() && (attacker.getAltitude() == 0)
              && game.isOnGroundMap(attacker)))
              && (weapon != null)) {
            int range = Compute.effectiveDistance(game, attacker, target, false);
            // Only aft-mounted weapons can be fired at range 0 (targets directly underneath)
            if (!attacker.isAirborne() && (range == 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                return Messages.getString("WeaponAttackAction.OnlyAftAtZero");
            }

            int altDif = target.getAltitude() - attacker.getAltitude();

            // Nose-mounted weapons can only be fired at targets at least 1 altitude higher
            if ((weapon.getLocation() == Aero.LOC_NOSE) &&
                  (altDif < 1) &&
                  weaponType != null
                  // Unless the weapon is used as artillery
                  &&
                  (!(weaponType instanceof ArtilleryWeapon ||
                        weaponType.hasFlag(WeaponType.F_ARTILLERY) ||
                        (attacker.getAltitude() == 0 && weaponType instanceof CapitalMissileWeapon) ||
                        isIndirect))) {
                return Messages.getString("WeaponAttackAction.TooLowForNose");
            }
            // Front-side-mounted weapons can only be fired at targets at the same altitude or higher
            if ((!weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT)) &&
                  (altDif < 0) &&
                  weaponType != null &&
                  !((weaponType instanceof ArtilleryWeapon) || weaponType.hasFlag(WeaponType.F_ARTILLERY))) {
                return Messages.getString("WeaponAttackAction.TooLowForFrontSide");
            }
            // Aft-mounted weapons can only be fired at targets at least 1 altitude lower
            // For grounded spheroids, weapons can only be fired at targets in occupied hexes, but it's not actually
            // possible for a unit to occupy the same hex as a grounded spheroid so we simplify the calculation a bit
            if (weapon.getLocation() == Aero.LOC_AFT) {
                if (altDif > -1) {
                    return Messages.getString("WeaponAttackAction.TooHighForAft");
                }

                // if both targets are on the ground and the target is below the attacker and the attacker is in one
                // of the target's occupied hexes then we can shoot aft weapons at it note that this cannot actually
                // happen in MegaMek currently but is left here for the possible eventuality that overhanging
                // dropships are implemented
                if (!attacker.isAirborne() && !target.isAirborne()) {
                    boolean targetInAttackerHex = attacker.getOccupiedCoords().contains(target.getPosition()) ||
                          attacker.getPosition().equals(target.getPosition());
                    boolean targetBelowAttacker = game.getHexOf(attacker).getLevel() >
                          game.getHexOf(target).getLevel() +
                                target.getElevation();

                    if (!targetInAttackerHex || !targetBelowAttacker) {
                        return Messages.getString("WeaponAttackAction.GroundedSpheroidDropshipAftWeaponRestriction");
                    }
                }
            }

            // and aft-side-mounted weapons can only be fired at targets at the same or lower altitude
            if ((weapon.isRearMounted()) && (altDif > 0)) {
                return Messages.getString("WeaponAttackAction.TooHighForAftSide");
            }

            if (Compute.inDeadZone(game, attacker, target)) {
                // Only nose weapons can fire at targets in the dead zone at higher altitude
                if ((altDif > 0) && (weapon.getLocation() != Aero.LOC_NOSE)) {
                    return Messages.getString("WeaponAttackAction.OnlyNoseInDeadZone");
                }
                // and only aft weapons can fire at targets in the dead zone at lower altitude
                if ((altDif < 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                    return Messages.getString("WeaponAttackAction.OnlyAftInDeadZone");
                }
            }

        }

        // Weapon-specific Reasons

        if (weapon != null && weaponType != null) {
            // Variable setup

            // "Cool" mode for vehicle flamer requires coolant ammo
            boolean vf_cool = ammoType != null &&
                  ammo != null &&
                  (ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_COOLANT));

            // Anti-Infantry weapons can only target infantry
            if (weaponType.hasFlag(WeaponType.F_INFANTRY_ONLY)) {
                if ((entityTarget != null) && !(entityTarget instanceof Infantry)) {
                    return Messages.getString("WeaponAttackAction.TargetOnlyInf");
                }
            }

            // Air-to-ground attacks
            if (Compute.isAirToGround(attacker, target) && !isArtilleryIndirect && !attacker.isDropping()) {
                if (attacker.isBomber()
                      && weapon.isInternalBomb()
                      && ((IBomber) attacker).getUsedInternalBombs() >= 6) {
                    return Messages.getString("WeaponAttackAction.AlreadyUsedMaxInternalBombs");
                }
                // Can't strike from above altitude 5. Dive-bombing uses a different test below
                if ((attacker.getAltitude() > 5) &&
                      !weaponType.hasAnyFlag(WeaponType.F_DIVE_BOMB, WeaponType.F_ALT_BOMB)) {
                    return Messages.getString("WeaponAttackAction.AttackerTooHigh");
                }
                // Can't strafe from above altitude 3
                if ((attacker.getAltitude() > 3) && isStrafing) {
                    return Messages.getString("WeaponAttackAction.AttackerTooHigh");
                }
                // Additional Nap-of-Earth restrictions for strafing
                if ((attacker.isNOE()) && isStrafing) {
                    Vector<Coords> passedThrough = attacker.getPassedThrough();
                    if (passedThrough.isEmpty() || passedThrough.get(0).equals(target.getPosition())) {
                        // TW pg 243 says units flying at NOE have a harder time establishing LoS while strafing and
                        // hence have to consider the adjacent hex along the flight place in the direction of the
                        // attack. What if there is no adjacent hex? The rules don't address this. We could
                        // theoretically consider last turns movement, but that's cumbersome, so we'll just assume
                        // it's impossible - Arlith
                        return Messages.getString("WeaponAttackAction.TooCloseForStrafe");
                    }

                    // Strafing dead-zone, TW pg 243
                    Coords prevCoords = attacker.passedThroughPrevious(target.getPosition());
                    Hex prevHex = game.getHex(prevCoords, attacker.getPassedThroughBoardId());
                    Hex currHex = game.getHexOf(target);
                    int prevElev = prevHex.getLevel();
                    int currElev = currHex.getLevel();
                    if ((prevElev - currElev - target.relHeight()) > 2) {
                        return Messages.getString("WeaponAttackAction.DeadZone");
                    }
                }

                // Per TW p. 243: "The unit may fire one, some, or all of its non-ammo-dependent
                // direct-fire energy and pulse weapons when strafing."
                // This excludes plasma weapons since they require ammo.
                boolean isDirectFireEnergy = (weaponType.hasFlag(WeaponType.F_DIRECT_FIRE) &&
                      (weaponType.hasFlag(WeaponType.F_LASER) ||
                            weaponType.hasFlag(WeaponType.F_PPC))) ||
                      weaponType.hasFlag(WeaponType.F_FLAMER);
                // Note: flamers are direct fire energy, but don't have the flag,
                // so they won't work with targeting computers
                boolean isEnergyBay = (weaponType instanceof LaserBayWeapon) ||
                      (weaponType instanceof PPCBayWeapon) ||
                      (weaponType instanceof PulseLaserBayWeapon);
                if (isStrafing && !isDirectFireEnergy && !isEnergyBay) {
                    return Messages.getString("WeaponAttackAction.StrafeDirectEnergyOnly");
                }

                // only certain weapons can be used for air to ground attacks
                if (attacker.isAero()) {
                    // Spheroids can't strafe
                    if (isStrafing && attacker.isSpheroid()) {
                        return Messages.getString("WeaponAttackAction.NoSpheroidStrafing");
                    }
                    // Spheroid craft can only use aft or aft-side mounted weapons for strike attacks
                    if (attacker.isSpheroid()) {
                        if ((weapon.getLocation() != Aero.LOC_AFT) && !weapon.isRearMounted()) {
                            return Messages.getString("WeaponAttackAction.InvalidDSAtgArc");
                        }

                    } else if (attacker instanceof LandAirMek) {
                        // LAMs can't use leg or rear-mounted weapons
                        if ((weapon.getLocation() == Mek.LOC_LEFT_LEG) ||
                              (weapon.getLocation() == Mek.LOC_RIGHT_LEG) ||
                              weapon.isRearMounted()) {
                            return Messages.getString("WeaponAttackAction.InvalidAeroDSAtgArc");
                        }

                    } else {
                        // and other types of aero can't use aft or rear-mounted weapons
                        if ((weapon.getLocation() == Aero.LOC_AFT) || weapon.isRearMounted()) {
                            return Messages.getString("WeaponAttackAction.InvalidAeroDSAtgArc");
                        }
                    }
                }

                // for air to ground attacks, the target's position must be within
                // the flight path, unless it is an artillery weapon in the nose.
                // http://www.classicbattletech.com/forums/index.php?topic=65110.0
                if (!attacker.passedOver(target)) {
                    if (!weaponType.hasFlag(WeaponType.F_ARTILLERY)) {
                        return Messages.getString("WeaponAttackAction.NotOnFlightPath");
                    } else if (weapon.getLocation() != Aero.LOC_NOSE) {
                        return Messages.getString("WeaponAttackAction.NotOnFlightPath");
                    }
                }

                // Strike attacks cost the attacker 1 altitude
                int altitudeLoss = 1;
                // Dive-bombing costs 2 altitude
                if (weaponType.hasFlag(WeaponType.F_DIVE_BOMB)) {
                    altitudeLoss = 2;
                }
                // Altitude bombing and strafing cost nothing
                if (weaponType.hasFlag(WeaponType.F_ALT_BOMB) || isStrafing) {
                    altitudeLoss = 0;
                }
                int altLossThisRound = 0;
                if (attacker.isAero()) {
                    altLossThisRound = ((IAero) attacker).getAltLossThisRound();
                }
                // You can't make attacks that would lower you to zero altitude
                if (altitudeLoss >= (attacker.getAltitude() + altLossThisRound)) {
                    return Messages.getString("WeaponAttackAction.TooMuchAltLoss");
                }

                // can only make a strike attack against a single target
                if (!isStrafing) {
                    for (EntityAction action : game.getActionsVector()) {
                        if (action instanceof WeaponAttackAction otherAttack) {
                            if ((otherAttack.getEntityId() == attacker.getId())
                                  && (otherAttack.getTargetId() != target.getId())
                                  && !weaponType.hasFlag(WeaponType.F_ALT_BOMB)) {
                                return Messages.getString("WeaponAttackAction.CantSplitFire");
                            }
                        }
                    }
                }

            } else if ((attacker instanceof VTOL) && isStrafing) {
                // VTOL Strafing
                if (!(weaponType.hasFlag(WeaponType.F_DIRECT_FIRE) &&
                      (weaponType.hasAnyFlag(WeaponType.F_LASER, WeaponType.F_PPC, WeaponType.F_PLASMA,
                            WeaponType.F_PLASMA_MFUK))) || weaponType.hasFlag(WeaponType.F_FLAMER)) {
                    return Messages.getString("WeaponAttackAction.StrafeDirectEnergyOnly");
                }
                if (weapon.getLocation() != VTOL.LOC_FRONT &&
                      weapon.getLocation() != VTOL.LOC_TURRET &&
                      weapon.getLocation() != VTOL.LOC_TURRET_2) {
                    return Messages.getString("WeaponAttackAction.InvalidStrafingArc");
                }
            }

            // Artillery

            // Arty shots have to be with arty, non-arty shots with non arty.
            if (weaponType.hasFlag(WeaponType.F_ARTILLERY)) {

                // Don't allow Artillery Flak attacks by off-board artillery.
                if (entityTarget != null && entityTarget.isAirborne() && attacker.isOffBoard()) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }

                // check artillery is targeted appropriately for its ammo
                // Artillery only targets hexes unless making a direct fire flak shot or using
                // homing ammo.
                if ((targetType != Targetable.TYPE_HEX_ARTILLERY) &&
                      (targetType != Targetable.TYPE_MINEFIELD_CLEAR) &&
                      !(isArtilleryFLAK || (ammoType != null && ammoType.countsAsFlak())) &&
                      !isHoming &&
                      !target.isOffBoard()) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }
                // Airborne units can't make direct-fire artillery attacks
                if (attacker.isAirborne()) {
                    if (isArtilleryDirect) {
                        return Messages.getString("WeaponAttackAction.NoAeroDirectArty");
                    } else if (isArtilleryIndirect) {
                        // and can only make indirect artillery attacks at altitude 9 or below
                        if (attacker.getAltitude() > 9) {
                            return Messages.getString("WeaponAttackAction.TooHighForArty");
                        }
                        // and finally, can only use Arrow IV artillery
                        if (attacker.usesWeaponBays()) {
                            // For Dropships
                            for (WeaponMounted bayW : weapon.getBayWeapons()) {
                                // check the loaded ammo for the Arrow IV flag
                                AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
                                AmmoType bAType = bayWAmmo.getType();
                                if (bAType.getAmmoType() != ARROW_IV) {
                                    return Messages.getString("WeaponAttackAction.OnlyArrowArty");
                                }
                            }
                        } else if ((weaponType.getAmmoType() != ARROW_IV) &&
                              (weaponType.getAmmoType() != ARROW_IV_BOMB)) {
                            // For Fighters, LAMs, Small Craft and VTOLs
                            return Messages.getString("WeaponAttackAction.OnlyArrowArty");
                        }
                    }
                } else if ((weaponType.getAmmoType() == ARROW_IV) &&
                      ammoType != null &&
                      ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                    // Air-Defense Arrow IV can only target airborne enemy units between 1 and 51 hexes away
                    // (same ground map/Low Altitude hex, 1 LAH, or 2 Low Altitude hexes away) and below altitude 8.
                    if (!(target.isAirborne() || target.isAirborneVTOLorWIGE())) {
                        return Messages.getString("WeaponAttackAction.AaaGroundAttack");
                    }
                    if (target.getAltitude() > 8) {
                        return Messages.getString("WeaponAttackAction.OutOfRange");
                    }
                    if (distance > Board.DEFAULT_BOARD_HEIGHT * 3) {
                        return Messages.getString("WeaponAttackAction.OutOfRange");
                    }
                }

            } else if (!weapon.isInBearingsOnlyMode()) {
                if ((weaponType.isCapital() || weaponType.isSubCapital())
                      && CrossBoardAttackHelper.isOrbitToSurface(game, attacker, target)) {
                    // O2S attacks behave (correctly) as artillery attacks against hex targets, SO:AA p.91
                    if (!game.getPhase().isTargeting() || (targetType != Targetable.TYPE_HEX_ARTILLERY)) {
                        return Messages.getString("WeaponAttackAction.OnlyInTargeting");
                    }
                } else if (weaponType instanceof CapitalMissileWeapon && Compute.isGroundToGround(attacker, target)) {
                    // Grounded units firing capital missiles at ground targets must do so as artillery
                    if (targetType != Targetable.TYPE_HEX_ARTILLERY) {
                        return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                    }
                } else {
                    // weapon is not artillery
                    if (targetType == Targetable.TYPE_HEX_ARTILLERY) {
                        return Messages.getString("WeaponAttackAction.NoArtyAttacks");
                    }
                }
            }

            // Direct-fire artillery attacks.
            if (isArtilleryDirect) {
                if (attacker.isOffBoard()) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }
                // Cruise missiles cannot make direct-fire attacks
                if (isCruiseMissile) {
                    return Messages.getString("WeaponAttackAction.NoDirectCruiseMissile");
                }
                // ADA is _fired_ by artillery but is just a Flak attack, and so bypasses these
                // restrictions
                if (null != ammoType && !ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                    // Direct fire artillery cannot be fired at less than 6 hexes,
                    // except at ASFs in the air (TO:AR 6th print, p153.)
                    if (!(target.isAirborne()) && (Compute.effectiveDistance(game, attacker, target) <= 6)) {
                        return Messages.getString("WeaponAttackAction.TooShortForDirectArty");
                    }
                    // ...or more than 17 hexes
                    if (!(target.isAirborne()) && distance > Board.DEFAULT_BOARD_HEIGHT) {
                        return Messages.getString("WeaponAttackAction.TooLongForDirectArty");
                    }
                    // Artillery Flak targeting Aerospace ignores altitude when computing range
                    if (target.isAirborne() &&
                          attacker.getPosition().distance(target.getPosition()) > Board.DEFAULT_BOARD_HEIGHT) {
                        return Messages.getString("WeaponAttackAction.TooLongForDirectArtyFlak");
                    }
                }
            }

            // Indirect artillery attacks
            if (isArtilleryIndirect) {
                // Cannot make Indirect Flak attacks
                if (isFlakAttack || isArtilleryFLAK) {
                    return Messages.getString("WeaponAttackAction.FlakIndirect");
                }

                int boardRange = (int) Math.ceil(distance / 17f);
                int maxRange = weaponType.getLongRange();
                // Capital/subcapital missiles have a board range equal to their max space hex
                // range
                if (weaponType instanceof CapitalMissileWeapon) {
                    if (weaponType.getMaxRange(weapon) == WeaponType.RANGE_EXT) {
                        maxRange = 50;
                    }
                    if (weaponType.getMaxRange(weapon) == WeaponType.RANGE_LONG) {
                        maxRange = 40;
                    }
                    if (weaponType.getMaxRange(weapon) == WeaponType.RANGE_MED) {
                        maxRange = 24;
                    }
                    if (weaponType.getMaxRange(weapon) == WeaponType.RANGE_SHORT) {
                        maxRange = 12;
                    }
                }

                // Apply gravity mod here, per TO: AR pg 155
                maxRange = (int) (Math.floor((double) (maxRange * Board.DEFAULT_BOARD_HEIGHT) /
                      game.getPlanetaryConditions().getGravity()) / 17f);

                // Maximum range is measured in map sheets
                if (boardRange > maxRange) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Indirect (=targeting phase) shots cannot be made at less than 17 hexes range unless
                // the attacker is airborne
                if (((distance <= Board.DEFAULT_BOARD_HEIGHT) && !attacker.isAirborne())) {
                    return Messages.getString("WeaponAttackAction.TooShortForIndirectArty");
                }
                if (isHoming) {
                    // Homing missiles must target a hex (map sheet)
                    if (targetType != Targetable.TYPE_HEX_ARTILLERY) {
                        return Messages.getString("WeaponAttackAction.HomingMapsheetOnly");
                    }
                }
            }

            // Ballistic and Missile weapons are subject to wind conditions
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            if (conditions.getWind().isTornadoF1ToF3() &&
                  weaponType.hasFlag(WeaponType.F_MISSILE) &&
                  !attacker.isSpaceborne()) {
                return Messages.getString("WeaponAttackAction.NoMissileTornado");
            }
            boolean missileOrBallistic = weaponType.hasFlag(WeaponType.F_MISSILE)
                  || weaponType.hasFlag(WeaponType.F_BALLISTIC);
            if (conditions.getWind().isTornadoF4() && !attacker.isSpaceborne() && missileOrBallistic) {
                return Messages.getString("WeaponAttackAction.F4Tornado");
            }

            // Battle Armor

            // BA can only make one AP attack
            if ((attacker instanceof BattleArmor) && weaponType.hasFlag(WeaponType.F_INFANTRY)) {
                final int attackerWeaponId = attacker.getEquipmentNum(weapon);
                // See if this unit has made a previous AP attack
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    Object o = i.nextElement();
                    if (!(o instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    // Is this an attack from this entity
                    if (prevAttack.getEntityId() == attacker.getId()) {
                        Mounted<?> prevWeapon = attacker.getEquipment(prevAttack.getWeaponId());
                        WeaponType prevWeaponType = (WeaponType) prevWeapon.getType();
                        if (prevWeaponType.hasFlag(WeaponType.F_INFANTRY) && (prevAttack.getWeaponId()
                              != attackerWeaponId)) {
                            return Messages.getString("WeaponAttackAction.OnlyOneBAAPAttack");
                        }
                    }
                }
            }

            // BA compact narc: we have one weapon for each trooper, but you
            // can fire only at one target at a time
            if (weaponType.getName().equals("Compact Narc")) {
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    if (prevAttack.getEntityId() == attackerId) {
                        Mounted<?> prevWeapon = attacker.getEquipment(prevAttack.getWeaponId());
                        if (prevWeapon.getType().getName().equals("Compact Narc")) {
                            if (prevAttack.getTargetId() != target.getId()) {
                                return Messages.getString("WeaponAttackAction.OneTargetForCNarc");
                            }
                        }
                    }
                }
            }

            // BA Mine launchers can only target 'Mek, vehicle, or grounded fighter (TW p.229)
            if (BattleArmor.MINE_LAUNCHER.equals(weaponType.getInternalName())) {
                boolean isValidTarget = (entityTarget instanceof Mek)
                      || (entityTarget instanceof Tank)
                      || ((entityTarget instanceof Aero) && !entityTarget.isAirborne());
                if (!isValidTarget) {
                    return Messages.getString("WeaponAttackAction.PopUpMineInvalidTarget");
                }
            }

            // BA NARCs and Tasers can only fire at one target in a round
            if ((attacker instanceof BattleArmor) &&
                  (weaponType.hasFlag(WeaponType.F_TASER) || weaponType.getAmmoType() == NARC)) {
                // Go through all the current actions to see if a NARC or Taser
                // has been fired
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    Object o = i.nextElement();
                    if (!(o instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    // Is this an attack from this entity to a different target?
                    if (prevAttack.getEntityId() == attacker.getId() && prevAttack.getTargetId() != target.getId()) {
                        Mounted<?> prevWeapon = attacker.getEquipment(prevAttack.getWeaponId());
                        WeaponType prevWeaponType = (WeaponType) prevWeapon.getType();
                        if (prevWeapon.getType().hasFlag(WeaponType.F_TASER) &&
                              weapon.getType().hasFlag(WeaponType.F_TASER)) {
                            return Messages.getString("WeaponAttackAction.BATaserSameTarget");
                        }
                        if (prevWeaponType.getAmmoType() == NARC && weaponType.getAmmoType() == NARC) {
                            return Messages.getString("WeaponAttackAction.BANarcSameTarget");
                        }
                    }
                }
            }

            // BA squad support weapons require that Trooper 1 be alive to use
            if (weapon.isSquadSupportWeapon() && (attacker instanceof BattleArmor)) {
                if (!((BattleArmor) attacker).isTrooperActive(BattleArmor.LOC_TROOPER_1)) {
                    return Messages.getString("WeaponAttackAction.NoSquadSupport");
                }
            }

            // Bombs and such

            // Anti ship missiles can't be launched from altitude 3 or lower
            if (weaponType.hasFlag(WeaponType.F_ANTI_SHIP) && !attacker.isSpaceborne() && (attacker.getAltitude()
                  < 4)) {
                return Messages.getString("WeaponAttackAction.TooLowForASM");
            }

            // ASEW Missiles cannot be launched in an atmosphere
            if ((weaponType.getAmmoType() == ASEW_MISSILE) && !attacker.isSpaceborne()) {
                return Messages.getString("WeaponAttackAction.ASEWAtmo");
            }

            if (attacker.isAero()) {
                // Can't mix bombing with other attack types
                // also for altitude bombing, the target hex must either be the first in a line
                // adjacent to a prior one
                boolean adjacentAltBomb = false;
                boolean firstAltBomb = true;
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    Object o = i.nextElement();
                    if (!(o instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    if (prevAttack.getEntityId() == attackerId) {
                        // You also can't mix and match the 3 different types of bombing: Space, Dive
                        // and Altitude
                        if ((weaponId != prevAttack.getWeaponId()) &&
                              attacker.getEquipment(prevAttack.getWeaponId())
                                    .getType()
                                    .hasFlag(WeaponType.F_SPACE_BOMB)) {
                            return Messages.getString("WeaponAttackAction.BusySpaceBombing");
                        }
                        if ((weaponId != prevAttack.getWeaponId()) &&
                              attacker.getEquipment(prevAttack.getWeaponId())
                                    .getType()
                                    .hasFlag(WeaponType.F_DIVE_BOMB)) {
                            return Messages.getString("WeaponAttackAction.BusyDiveBombing");
                        }
                        if ((weaponId != prevAttack.getWeaponId()) &&
                              attacker.getEquipment(prevAttack.getWeaponId())
                                    .getType()
                                    .hasFlag(WeaponType.F_ALT_BOMB)) {
                            if (!weaponType.hasFlag(WeaponType.F_ALT_BOMB)) {
                                return Messages.getString("WeaponAttackAction.BusyAltBombing");
                            }
                            firstAltBomb = false;
                            int bombDistance = prevAttack.getTarget(game).getPosition().distance(target.getPosition());
                            if (bombDistance == 1) {
                                adjacentAltBomb = true;
                            }
                            // For altitude bombing, prevent targeting the same hex twice
                            if (bombDistance == 0) {
                                return Messages.getString("WeaponAttackAction.AlreadyBombingHex");
                            }

                        }
                    }
                }
                if (weaponType.hasFlag(WeaponType.F_ALT_BOMB) && !firstAltBomb && !adjacentAltBomb) {
                    return Messages.getString("WeaponAttackAction.BombNotInLine");
                }
            }

            // Altitude and dive-bombing attacks...
            if (weaponType.hasFlag(WeaponType.F_DIVE_BOMB) || weaponType.hasFlag(WeaponType.F_ALT_BOMB)) {
                // Can't fire if the unit is out of bombs
                if (attacker.getBombs(AmmoType.F_GROUND_BOMB).isEmpty()) {
                    return Messages.getString("WeaponAttackAction.OutOfBombs");
                }
                // Spheroid Aeros can't bomb
                if (attacker.isAero() && attacker.isSpheroid()) {
                    return Messages.getString("WeaponAttackAction.NoSpheroidBombing");
                }
                // Grounded Aeros can't bomb
                if (!attacker.isAirborne() && !attacker.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.GroundedAeroCantBomb");
                }
                // Bomb attacks can only target hexes
                if (target.getTargetType() != Targetable.TYPE_HEX_AERO_BOMB) {
                    return Messages.getString("WeaponAttackAction.BombTargetHexOnly");
                }
                // Can't target a hex that isn't on the flight path
                if (!attacker.passedOver(target)) {
                    return Messages.getString("WeaponAttackAction.CantBombOffFlightPath");
                }
                // Dive Bombing can only be conducted if starting between altitude 5 and altitude 3
                if (weaponType.hasFlag(WeaponType.F_DIVE_BOMB)) {
                    if (attacker.getAltitude() > MMConstants.DIVE_BOMB_MAX_ALTITUDE) {
                        return Messages.getString("WeaponAttackAction.TooHighForDiveBomb");
                    }
                    if (attacker.isAero()) {
                        int altLoss = ((IAero) attacker).getAltLossThisRound();
                        if ((attacker.getAltitude() + altLoss) < MMConstants.DIVE_BOMB_MIN_ALTITUDE) {
                            return Messages.getString("WeaponAttackAction.TooLowForDiveBomb");
                        }
                    }
                }
            }

            // Can't attack bomb hex targets with weapons other than alt/dive bombs
            if ((target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB) &&
                  !weaponType.hasFlag(WeaponType.F_DIVE_BOMB) &&
                  !weaponType.hasFlag(WeaponType.F_ALT_BOMB)) {
                return Messages.getString("WeaponAttackAction.InvalidForBombing");
            }

            // BA Micro bombs only when flying
            if ((ammoType != null) && (ammoType.getAmmoType() == BA_MICRO_BOMB)) {
                if (!attacker.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.MinimumAlt1");
                    // and can only target hexes
                } else if (target.getTargetType() != Targetable.TYPE_HEX_BOMB) {
                    return Messages.getString("WeaponAttackAction.BombTargetHexOnly");
                    // and can only be dropped at exactly altitude 1
                } else if (attacker.getElevation() != 1) {
                    return Messages.getString("WeaponAttackAction.ExactlyAlt1");
                }
            }

            // Can't attack a Micro Bomb hex target with other weapons
            if ((target.getTargetType() == Targetable.TYPE_HEX_BOMB) &&
                  !(usesAmmo && ammoType != null && (ammoType.getAmmoType() == BA_MICRO_BOMB))) {
                return Messages.getString("WeaponAttackAction.InvalidForBombing");
            }

            // Space bombing attacks
            if (weaponType.hasFlag(WeaponType.F_SPACE_BOMB) && entityTarget != null) {
                toHit = Compute.getSpaceBombBaseToHit(attacker, entityTarget, game);
                // Return if the attack is impossible.
                if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                    return toHit.getDesc();
                }
            }

            // B-Pods

            if (weaponType.hasFlag(WeaponType.F_B_POD)) {
                // B-Pods are only effective against infantry
                if (!(target instanceof Infantry)) {
                    return Messages.getString("WeaponAttackAction.BPodOnlyAtInf");
                }
                // Leg-mounted B-Pods can be fired at infantry in the attacker's hex, other
                // locations
                // can only be fired in response to leg/swarm attacks
                if (attacker instanceof BipedMek) {
                    if (!((weapon.getLocation() == Mek.LOC_LEFT_LEG) || (weapon.getLocation() == Mek.LOC_RIGHT_LEG))) {
                        return Messages.getString("WeaponAttackAction.OnlyLegBPod");
                    }
                } else if (attacker instanceof QuadMek) {
                    if (!((weapon.getLocation() == Mek.LOC_LEFT_LEG) ||
                          (weapon.getLocation() == Mek.LOC_RIGHT_LEG) ||
                          (weapon.getLocation() == Mek.LOC_LEFT_ARM) ||
                          (weapon.getLocation() == Mek.LOC_RIGHT_ARM))) {
                        return Messages.getString("WeaponAttackAction.OnlyLegBPod");
                    }
                }
            }

            // Called shots
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS)) {
                String reason = weapon.getCalledShot().isValid(target);
                if (reason != null) {
                    return reason;
                }
            }

            // Capital Mass Drivers can only fire at targets directly in front of the attacker
            if (entityTarget != null
                  && weaponType.hasFlag(WeaponType.F_MASS_DRIVER)
                  && (attacker instanceof SpaceStation)) {
                int range = Compute.effectiveDistance(game, attacker, target);
                if (!attacker.getPosition().translated(attacker.getFacing(), range).equals(target.getPosition())) {
                    return Messages.getString("WeaponAttackAction.MassDriverFrontOnly");
                }
            }

            // Capital missiles in bearings-only mode
            if (isBearingsOnlyMissile) {
                // Can't target anything beyond max range of 5,000 hexes
                // This is an arbitrary number. If your map size is really this large, you'll
                // probably crash the game
                if (distance > RangeType.RANGE_BEARINGS_ONLY_OUT) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Can't fire in bearings-only mode within direct-fire range (50 hexes)
                if (game.getPhase().isTargeting() && distance < RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
                    return Messages.getString("WeaponAttackAction.BoMissileMinRange");
                }
                // Can't target anything but hexes
                if (targetType != Targetable.TYPE_HEX_ARTILLERY) {
                    return Messages.getString("WeaponAttackAction.BOHexOnly");
                }
            }

            // Capital weapons fire by grounded units
            if (weaponType.isSubCapital() || weaponType.isCapital()) {
                // Can't fire any but capital/subcapital missiles surface-to-surface (but VTOL dive bombing is allowed)
                if (Compute.isGroundToGround(attacker, target) &&
                      !((attacker.getMovementMode() == EntityMovementMode.VTOL)
                            && (weaponType instanceof DiveBombAttack)) &&
                      !(weaponType instanceof CapitalMissileWeapon)) {
                    return Messages.getString("WeaponAttackAction.NoS2SCapWeapons");
                }
            }

            // Causing Fires

            // Some weapons can't cause fires, but Infernos always can.
            if ((vf_cool || (weaponType.hasFlag(WeaponType.F_NO_FIRES) && !isInferno)) &&
                  (Targetable.TYPE_HEX_IGNITE == target.getTargetType())) {
                return Messages.getString("WeaponAttackAction.WeaponCantIgnite");
            }

            // only woods and buildings can be set intentionally on fire
            if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                  && game.getOptions().booleanOption(OptionsConstants.ADVANCED_NO_IGNITE_CLEAR)
                  && !(game.getHexOf(target).containsAnyTerrainOf(Terrains.WOODS, Terrains.JUNGLE,
                  Terrains.FUEL_TANK, Terrains.BUILDING))) {
                return Messages.getString("WeaponAttackAction.CantIntentionallyBurn");
            }

            // Conventional Infantry Attacks

            if (isAttackerInfantry && !(attacker instanceof BattleArmor)) {
                // 0 MP infantry units: move or shoot, except for anti-mek attacks, those are handled above
                if ((attacker.getMovementMode() == EntityMovementMode.INF_LEG) &&
                      (attacker.getWalkMP() == 0) &&
                      (attacker.moved != EntityMovementType.MOVE_NONE)) {
                    return Messages.getString("WeaponAttackAction.0MPInf");
                }
                // Can't shoot if platoon used fast movement
                if (game.getOptions()
                      .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FAST_INFANTRY_MOVE) &&
                      (attacker.moved == EntityMovementType.MOVE_RUN)) {
                    return Messages.getString("WeaponAttackAction.CantShootAndFastMove");
                }
                // check for trying to fire field gun after moving
                if ((weapon.getLocation() == Infantry.LOC_FIELD_GUNS) && (attacker.moved
                      != EntityMovementType.MOVE_NONE)) {
                    return Messages.getString("WeaponAttackAction.CantMoveAndFieldGun");
                }
                // check for mixing infantry and field gun attacks
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    if (prevAttack.getEntityId() == attackerId) {
                        Mounted<?> prevWeapon = attacker.getEquipment(prevAttack.getWeaponId());
                        if ((prevWeapon.getType().hasFlag(WeaponType.F_INFANTRY) &&
                              (weapon.getLocation() == Infantry.LOC_FIELD_GUNS)) ||
                              (weapon.getType().hasFlag(WeaponType.F_INFANTRY) &&
                                    (prevWeapon.getLocation() == Infantry.LOC_FIELD_GUNS))) {
                            return Messages.getString("WeaponAttackAction.FieldGunOrSAOnly");
                        }
                    }
                }
            }

            // Extinguishing Fires

            // You can use certain types of flamer/sprayer ammo or infantry firefighting engineers to extinguish
            // burning hexes (and units).
            // TODO: This functionality does not appear to be implemented
            if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
                if (!weaponType.hasFlag(WeaponType.F_EXTINGUISHER) && !vf_cool) {
                    return Messages.getString("WeaponAttackAction.InvalidForFirefighting");
                }
                Hex hexTarget = game.getHexOf(target);
                if ((hexTarget != null) && !hexTarget.containsTerrain(Terrains.FIRE)) {
                    return Messages.getString("WeaponAttackAction.TargetNotBurning");
                }
            } else if (weaponType.hasFlag(WeaponType.F_EXTINGUISHER)) {
                if (!(((target instanceof Tank) && ((Tank) target).isOnFire()) ||
                      ((target instanceof Entity) && (((Entity) target).infernos.getTurnsLeftToBurn() > 0)))) {
                    return Messages.getString("WeaponAttackAction.TargetNotBurning");
                }
            }

            // Gauss weapons using the TacOps powered down rule can't fire
            if ((weaponType instanceof GaussWeapon)
                  && weapon.hasModes()
                  && weapon.curMode().equals(Weapon.MODE_GAUSS_POWERED_DOWN)) {
                return Messages.getString("WeaponAttackAction.WeaponNotReady");
            }

            // Ground-to-air attacks

            // air-2-air and air-2-ground cannot be combined by any aerospace units
            if (Compute.isAirToAir(game, attacker, target) || Compute.isAirToGround(attacker, target)) {
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    if (prevAttack.getEntityId() != attacker.getId()) {
                        continue;
                    }
                    if (Compute.isAirToAir(game, attacker, target) && prevAttack.isAirToGround(game)) {
                        return Messages.getString("WeaponAttackAction.AlreadyAtgAttack");
                    }
                    if (Compute.isAirToGround(attacker, target) && prevAttack.isAirToAir(game)) {
                        return Messages.getString("WeaponAttackAction.AlreadyAtaAttack");
                    }
                }
            }

            // Can't make ground-to-air attacks against a target above altitude 8
            if ((target.getAltitude() > 8) && Compute.isGroundToAir(attacker, target)) {
                return Messages.getString("WeaponAttackAction.AeroTooHighForGta");
            }

            // Infantry can't make ground-to-air attacks, unless using field guns,
            // specialized AA infantry weapons,
            // or direct-fire artillery flak attacks
            boolean isWeaponFieldGuns = isAttackerInfantry && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
            if ((attacker instanceof Infantry) &&
                  Compute.isGroundToAir(attacker, target) &&
                  !weaponType.hasFlag(WeaponType.F_INF_AA) &&
                  !isArtilleryFLAK &&
                  !isWeaponFieldGuns) {
                return Messages.getString("WeaponAttackAction.NoInfantryGta");
            }

            // only one ground-to-air attack allowed per turn
            // grounded spheroid dropships dont have this limitation
            if (!attacker.isAirborne() && !((attacker instanceof Dropship) && attacker.isSpheroid())) {
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    if (prevAttack.getEntityId() == attacker.getId()) {
                        if (prevAttack.isGroundToAir(game) && !Compute.isGroundToAir(attacker, target)) {
                            return Messages.getString("WeaponAttackAction.AlreadyGtaAttack");
                        }
                        // Can't mix ground-to-air and ground-to-ground attacks either
                        if (!prevAttack.isGroundToAir(game) && Compute.isGroundToAir(attacker, target)) {
                            return Messages.getString("WeaponAttackAction.AlreadyGtgAttack");
                        }
                        // Or split ground-to-air fire across multiple targets
                        if (prevAttack.isGroundToAir(game) &&
                              Compute.isGroundToAir(attacker, target) &&
                              (null != entityTarget) &&
                              (prevAttack.getTargetId() != entityTarget.getId())) {
                            return Messages.getString("WeaponAttackAction.OneTargetForGta");
                        }
                    }
                }
            }

            // Indirect Fire (LRMs)

            // Can't fire Indirect LRM with direct LOS
            if (isIndirect && Compute.indirectAttackImpossible(game, attacker, target, weaponType, weapon)) {
                return Messages.getString("WeaponAttackAction.NoIndirectWithLOS");
            }

            // Can't fire Indirect LRMs if the option is turned off
            if (isIndirect && !game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
                return Messages.getString("WeaponAttackAction.IndirectFireOff");
            }

            // Can't fire an MML indirectly when loaded with SRM munitions
            if (isIndirect &&
                  usesAmmo &&
                  ammoType != null &&
                  (ammoType.getAmmoType() == MML) &&
                  !ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                return Messages.getString("WeaponAttackAction.NoIndirectSRM");
            }

            // Can't fire anything but Mek Mortars and Artillery Cannons indirectly without a spotter unless the
            // attack has the Oblique Attacker SPA
            if (isIndirect
                  && (spotter == null)
                  && !(weaponType instanceof ArtilleryCannonWeapon)
                  && !attacker.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)
                  && !weaponType.hasFlag(WeaponType.F_MORTAR_TYPE_INDIRECT)) {
                return Messages.getString("WeaponAttackAction.NoSpotter");
            }

            // Infantry Leg attacks and Swarm attacks
            if (Infantry.LEG_ATTACK.equals(weaponType.getInternalName()) && entityTarget != null) {
                toHit = Compute.getLegAttackBaseToHit(attacker, entityTarget, game);

                // Return if the attack is impossible.
                if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                    return toHit.getDesc();
                }
                // Out of range?
                if (Compute.effectiveDistance(game, attacker, target) > 0) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Can't combine leg attacks with other attacks
                if (!isOnlyAttack(game, attacker, Infantry.LEG_ATTACK, entityTarget)) {
                    return Messages.getString("WeaponAttackAction.LegAttackOnly");
                }
            } else if (Infantry.SWARM_MEK.equals(weaponType.getInternalName()) && entityTarget != null) {
                toHit = Compute.getSwarmMekBaseToHit(attacker, entityTarget, game);

                // Return if the attack is impossible.
                if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                    return toHit.getDesc();
                }
                // Out of range?
                if (Compute.effectiveDistance(game, attacker, target) > 0) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Can't combine swarm attacks with other attacks
                if (!isOnlyAttack(game, attacker, Infantry.SWARM_MEK, entityTarget)) {
                    return Messages.getString("WeaponAttackAction.SwarmAttackOnly");
                }
            } else if (Infantry.STOP_SWARM.equals(weaponType.getInternalName())) {
                // Can't stop if we're not swarming, otherwise automatic.
                if (Entity.NONE == attacker.getSwarmTargetId()) {
                    return Messages.getString("WeaponAttackAction.NotSwarming");
                }
            } else if (Infantry.SWARM_WEAPON_MEK.equals(weaponType.getInternalName())) {
                // Can't stop if we're not swarming, otherwise automatic.
                if (Entity.NONE == attacker.getSwarmTargetId()) {
                    return Messages.getString("WeaponAttackAction.NotSwarming");
                }
            }
            // Swarming infantry always hit their target, but
            // they can only target the Mek they're swarming.
            if ((entityTarget != null) && (attacker.getSwarmTargetId() == entityTarget.getId())) {
                // Weapons that do no damage cannot be used in swarm attacks
                if (weaponType.getDamage() == 0) {
                    return Messages.getString("WeaponAttackAction.0DamageWeapon");
                }
                // Missiles and BA body-mounted weapons cannot be used when swarming
                if (weaponType.hasFlag(WeaponType.F_MISSILE)) {
                    return Messages.getString("WeaponAttackAction.NoMissileWhenSwarming");
                }
                if (weapon.isBodyMounted()) {
                    return Messages.getString("WeaponAttackAction.NoBodyWhenSwarming");
                }
            } else if (Entity.NONE != attacker.getSwarmTargetId()) {
                return Messages.getString("WeaponAttackAction.MustTargetSwarmed");
            }

            // MG arrays

            // Can't fire one if none of the component MGs are functional
            if (weaponType.hasFlag(WeaponType.F_MGA) && (weapon.getCurrentShots() == 0)) {
                return Messages.getString("WeaponAttackAction.NoWorkingMGs");
            }
            // Or if the array is off
            if (weaponType.hasFlag(WeaponType.F_MGA) && weapon.hasModes() && weapon.curMode()
                  .equals(Weapon.MODE_AMS_OFF)) {
                return Messages.getString("WeaponAttackAction.MGArrayOff");
            } else if (weaponType.hasFlag(WeaponType.F_MG)) {
                // and you can't fire an individual MG if it's in an array
                if (attacker.hasLinkedMGA(weapon)) {
                    return Messages.getString("WeaponAttackAction.MGPartOfArray");
                }
            }

            // Protomek can fire MGA only into front arc, TW page 137
            if ((attacker instanceof ProtoMek) &&
                  !ComputeArc.isInArc(attacker.getPosition(), attacker.getFacing(), target, Compute.ARC_FORWARD) &&
                  weaponType.hasFlag(WeaponType.F_MGA)
            ) {
                return Messages.getString("WeaponAttackAction.ProtoMGAOnlyFront");
            }

            // NARC and iNARC
            if ((weaponType.getAmmoType() == NARC) || (weaponType.getAmmoType() == INARC)) {
                // Cannot be used against targets inside buildings
                if (targetInBuilding) {
                    return Messages.getString("WeaponAttackAction.NoNarcInBuilding");
                }
                // and can't be fired at infantry
                if (target instanceof Infantry) {
                    return Messages.getString("WeaponAttackAction.CantNarcInfantry");
                }
            }

            // PPCs linked to capacitors can't fire while charging
            if (weapon.getType().hasFlag(WeaponType.F_PPC) &&
                  (weapon.getLinkedBy() != null) &&
                  weapon.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR) &&
                  weapon.getLinkedBy().pendingMode().equals(Weapon.MODE_PPC_CHARGE)) {
                return Messages.getString("WeaponAttackAction.PPCCharging");
            }

            // Some weapons can only be fired by themselves

            // Check to see if another solo weapon was fired
            boolean hasSoloAttack = false;
            String soloWeaponName = "";
            for (EntityAction ea : game.getActionsVector()) {
                if ((ea.getEntityId() == attackerId) && (ea instanceof WeaponAttackAction otherWAA)) {
                    final Mounted<?> otherWeapon = attacker.getEquipment(otherWAA.getWeaponId());

                    if (!(otherWeapon.getType() instanceof WeaponType otherWeaponType)) {
                        continue;
                    }
                    hasSoloAttack |= (otherWeaponType.hasFlag(WeaponType.F_SOLO_ATTACK) &&
                          otherWAA.getWeaponId() != weaponId);
                    if (hasSoloAttack) {
                        soloWeaponName = otherWeapon.getName();
                        break;
                    }
                }
            }
            if (hasSoloAttack) {
                return String.format(Messages.getString("WeaponAttackAction.CantFireWithOtherWeapons"), soloWeaponName);
            }

            // Handle solo attack weapons.
            if (weaponType.hasFlag(WeaponType.F_SOLO_ATTACK)) {
                for (EntityAction ea : game.getActionsVector()) {
                    if (!(ea instanceof WeaponAttackAction prevAttack)) {
                        continue;
                    }
                    if (prevAttack.getEntityId() == attackerId) {
                        // If the attacker fires another weapon, this attack fails.
                        if (weaponId != prevAttack.getWeaponId()) {
                            return Messages.getString("WeaponAttackAction.CantMixAttacks");
                        }
                    }
                }
            }

            // ProtoMeks cannot fire arm weapons and main gun in the same turn
            if ((attacker instanceof ProtoMek) &&
                  ((weapon.getLocation() == ProtoMek.LOC_MAIN_GUN) ||
                        (weapon.getLocation() == ProtoMek.LOC_RIGHT_ARM) ||
                        (weapon.getLocation() == ProtoMek.LOC_LEFT_ARM))) {
                final boolean firingMainGun = weapon.getLocation() == ProtoMek.LOC_MAIN_GUN;
                for (EntityAction ea : game.getActionsVector()) {
                    if ((ea.getEntityId() == attackerId) && (ea instanceof WeaponAttackAction otherWAA)) {
                        final Mounted<?> otherWeapon = attacker.getEquipment(otherWAA.getWeaponId());
                        if ((firingMainGun &&
                              ((otherWeapon.getLocation() == ProtoMek.LOC_RIGHT_ARM) ||
                                    (otherWeapon.getLocation() == ProtoMek.LOC_LEFT_ARM))) ||
                              !firingMainGun && (otherWeapon.getLocation() == ProtoMek.LOC_MAIN_GUN)) {
                            return Messages.getString("WeaponAttackAction.CantFireArmsAndMainGun");
                        }
                    }
                }
            }

            // TAG

            // The TAG system cannot target Airborne Aeros.
            if (isTAG && (entityTarget != null) && (entityTarget.isAirborne() || entityTarget.isSpaceborne())) {
                return Messages.getString("WeaponAttackAction.CantTAGAero");
            }

            // The TAG system cannot target infantry.
            if (isTAG && (entityTarget instanceof Infantry)) {
                return Messages.getString("WeaponAttackAction.CantTAGInf");
            }

            // TSEMPs

            // Can't fire a one-shot TSEMP more than once
            if (weaponType.hasFlag(WeaponType.F_TSEMP)
                  && weaponType.hasFlag(WeaponType.F_ONE_SHOT)
                  && weapon.isFired()) {
                return Messages.getString("WeaponAttackAction.OneShotTSEMP");
            }

            // Can't fire a regular TSEMP while it is recharging
            if (weaponType.hasFlag(WeaponType.F_TSEMP) && weapon.isFired()) {
                return Messages.getString("WeaponAttackAction.TSEMPRecharging");
            }

            // Weapon Bays

            // Large Craft weapon bays cannot bracket small craft at short range
            if (weapon.hasModes() &&
                  (weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_80) ||
                        weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_60) ||
                        weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_40)) &&
                  target.isAero() &&
                  entityTarget != null &&
                  !entityTarget.isLargeCraft() &&
                  (RangeType.rangeBracket(attacker.getPosition().distance(target.getPosition()),
                        weaponType.getRanges(weapon, ammo),
                        true,
                        false) == RangeType.RANGE_SHORT)) {
                return Messages.getString("WeaponAttackAction.TooCloseForSCBracket");
            }

            // you must have enough weapons in your bay to be able to use bracketing
            if (weapon.hasModes() &&
                  weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_80) &&
                  (weapon.getBayWeapons().size() < 2)) {
                return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
            }
            if (weapon.hasModes() &&
                  weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_60) &&
                  (weapon.getBayWeapons().size() < 3)) {
                return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
            }
            if (weapon.hasModes() &&
                  weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_40) &&
                  (weapon.getBayWeapons().size() < 4)) {
                return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
            }

            // If you're an aero, can't fire an AMS Bay at all or a Point Defense bay that's in PD Mode
            if (weaponType.hasFlag(WeaponType.F_AMS_BAY)) {
                return Messages.getString("WeaponAttackAction.AutoWeapon");
            } else if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_POINT_DEFENSE)) {
                return Messages.getString("WeaponAttackAction.PDWeapon");
            }

            // Weapon in arc?
            if (!ComputeArc.isInArc(game, attackerId, weaponId, target) &&
                  (!Compute.isAirToGround(attacker, target) || isArtilleryIndirect) &&
                  !attacker.isMakingVTOLGroundAttack() &&
                  !attacker.isOffBoard()) {
                return Messages.getString("WeaponAttackAction.OutOfArc");
            }

            // Weapon operational?
            // TODO move to top for early-out if possible, as this is the most common
            // reason shot is impossible
            if ((!evenIfAlreadyFired) && (!weapon.canFire(isStrafing, evenIfAlreadyFired))) {
                return Messages.getString("WeaponAttackAction.WeaponNotReady");
            }
        }

        // If we get here, the shot is possible
        return null;
    }

    /**
     * Some attacks are the only actions that a particular entity can make during its turn Also, only this unit can make
     * that particular attack.
     */
    private static boolean isOnlyAttack(Game game, Entity attacker, String attackType, Entity target) {
        // meks can only be the target of one leg or swarm attack
        for (EntityAction action : game.getActionsVector()) {
            if (action instanceof WeaponAttackAction waa) {
                Entity otherAttacker = waa.getEntity(game);
                if (otherAttacker == null) {
                    continue;
                }
                if (otherAttacker.equals(attacker)) {
                    // If there is an attack by this unit that is not the attack type, fail
                    if (!otherAttacker.getEquipment(waa.getWeaponId()).getType().is(attackType)) {
                        return false;
                    }
                }
                Targetable otherTarget = waa.getTarget(game);
                EquipmentType otherWeaponType = otherAttacker.getEquipment(waa.getWeaponId()).getType();
                if (otherWeaponType.is(attackType)
                      && (otherTarget != null)
                      && otherTarget.equals(target)
                      && !otherAttacker.equals(attacker)) {
                    // If there is an attack by another unit that has this attack type against the same target, fail
                    return false;
                }
            }
        }
        return true;
    }

    private ComputeToHitIsImpossible() {}
}
