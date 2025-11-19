/*
 * Copyright (C) 2001-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions;

import static megamek.common.units.QuadVee.CONV_MODE_VEHICLE;

import java.io.Serial;

import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;

public class PhysicalAttackAction extends AbstractAttackAction {
    @Serial
    private static final long serialVersionUID = -4702357516725749181L;

    public PhysicalAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public PhysicalAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Common checking whether is it possible to physically attack the target
     *
     * @param game   The current {@link Game}
     * @param ae     the attacking {@link Entity}, which may be null
     * @param target the attack's target
     *
     * @return reason the attack is impossible, or null if it is possible
     */
    protected static @Nullable String toHitIsImpossible(Game game, @Nullable Entity ae,
          Targetable target) {
        if (target == null) {
            return "target is null";
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                  && ((target.getOwnerId() == ae.getOwnerId())
                  || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                  && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                  && (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return "A friendly unit can never be the target of a direct attack.";
            }
        }

        if (!game.onTheSameBoard(ae, target)) {
            return "Attacker and target are not on the same board.";
        }

        // check range
        if (Compute.effectiveDistance(game, ae, target) > 1) {
            return "Target not in range";
        }

        // can't make a physical attack if you are evading
        if (ae.isEvading()) {
            return "Attacker is evading.";
        }

        // can't make physical attacks if loading/unloading cargo
        if (ae.endOfTurnCargoInteraction()) {
            return Messages.getString("WeaponAttackAction.CantFireWhileLoadingUnloadingCargo");
        }

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            // Checks specific to entity targets
            Entity te = (Entity) target;

            // Can't target a transported entity.
            if (Entity.NONE != te.getTransportId()) {
                return "Target is a passenger.";
            }

            // can't target yourself
            if (ae.equals(te)) {
                return "You can't target yourself";
            }

            // can't target airborne aero's
            if (te.isAirborne()) {
                return "can't target airborne units";
            }

            // Can't target an entity conducting a swarm attack.
            if (Entity.NONE != te.getSwarmTargetId()) {
                return "Target is swarming a Mek.";
            }

            if ((ae.getGrappled() != Entity.NONE) && (ae.getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                return "Locked in Grapple";

            }

            // target unit in building checks
            final boolean targetInBuilding = Compute.isInBuilding(game, te);
            if (targetInBuilding) {
                IBuilding TargBldg = game.getBoard(target).getBuildingAt(te.getPosition());

                // Can't target units in buildings (from the outside).
                if (!Compute.isInBuilding(game, ae)) {
                    return "Target is inside building";
                } else if (!game.getBoard(target).getBuildingAt(ae.getPosition()).equals(TargBldg)) {
                    return "Target is inside different building";
                }
            }

            // can't physically attack meks making dfa attacks
            if (te.isMakingDfa()) {
                return "Target is making a DFA attack";
            }
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
              || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
              || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return "Invalid attack";
        }

        return null;
    }

    protected static void setCommonModifiers(ToHitData toHit, Game game, Entity ae, Targetable target) {
        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, target);
        int attackerId = ae.getId();
        int targetId = target.getId();
        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (target instanceof BattleArmor) {
            toHit.addModifier(1, "battle armor target");
        }

        // Infantry squads are also hard to hit -- including for other infantry,
        // it seems (the rule is "all attacks"). However, this only applies to
        // proper squads deployed as such.
        if (target.isConventionalInfantry() && ((Infantry) target).isSquad()) {
            toHit.addModifier(1, "infantry squad target");
        }

        // Ejected MekWarriors are also more difficult targets.
        if (target instanceof MekWarrior) {
            toHit.addModifier(2, "ejected Pilot target");
        }
        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, target, 0, inSameBuilding));

        if (ae.hasModularArmor()) {
            toHit.addModifier(1, "Modular Armor");
        }

        if ((ae instanceof Mek) && ae.isSuperHeavy()) {
            toHit.addModifier(1, "attacker is superheavy mek");
        }

        if ((ae instanceof TripodMek) && ae.getCrew().hasDedicatedPilot()) {
            toHit.addModifier(-1, "attacker is tripod with dedicated pilot");
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if ((ae instanceof Mek attackingMek) && attackingMek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = attackingMek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_HEAD);
            int sensorHits2 = attackingMek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_CENTER_TORSO);
            if ((sensorHits + sensorHits2) == 3) {
                toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Sensors Completely Destroyed for Torso-Mounted Cockpit");
                return;
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()
              && game.getTagInfo().stream().noneMatch(inf -> inf.attackerId == ae.getId())) {
            toHit.addModifier(+1, "attacker is spotting for indirect LRM fire");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(target));

        toHit.append(nightModifiers(game, target, null, ae, false));

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            // Checks specific to entity targets
            Entity te = (Entity) target;

            // target movement
            toHit.append(Compute.getTargetMovementModifier(game, targetId));

            // target prone
            if (te.isProne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
            }

            if ((te.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) && !te.isAirborne()
                  && !te.isSpaceborne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeLargeSupportUnit"));
            }

            if (te instanceof SmallCraft) {
                if (te instanceof Dropship) {
                    toHit.addModifier(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
                } else {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeGroundedSmallCraft"));
                }
            }

            Hex targHex = game.getHexOf(te);
            // water partial cover?
            if ((te.height() > 0) && (te.getElevation() == -1)
                  && (targHex.terrainLevel(Terrains.WATER) == te.height())) {
                toHit.addModifier(1, "target has partial cover");
            }

            // Pilot skills
            Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

            // Attacking Weight Class Modifier.
            if (game.getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_ATTACK_PSR)) {
                if (ae.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                    toHit.addModifier(-2, "Weight Class Attack Modifier");
                } else if (ae.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                    toHit.addModifier(-1, "Weight Class Attack Modifier");
                }
            }

            // evading bonuses
            if (te.isEvading()) {
                toHit.addModifier(te.getEvasionBonus(), "target is evading");
            }

            if (te.isStealthActive()) {
                toHit.append(te.getStealthModifier(RangeType.RANGE_MINIMUM, ae));
            }
        }

        if ((ae instanceof Mek) && ((Mek) ae).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }
    }

    //Returns true if QuadVee is in Vehicle mode.  QuadVees in this mode are treated as having 1 less height in
    // physical attacks
    static protected boolean isConvertedQuadVee(Targetable target, Game game) {
        if (!target.isQuadMek()) {
            return false;
        }
        return game.getEntityOrThrow(target.getId()).getConversionMode() == CONV_MODE_VEHICLE;
    }
}
