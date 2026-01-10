/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;

/**
 * The attacker pushes the target.
 */
public class PushAttackAction extends DisplacementAttackAction {
    @Serial
    private static final long serialVersionUID = 6878038939232914083L;

    public PushAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public PushAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    public ToHitData toHit(Game game) {
        Targetable target = game.getTarget(getTargetType(), getTargetId());

        if (target == null) {
            return null;
        }

        return toHit(game, getEntityId(), target);
    }

    /**
     * pushes are impossible when physical attacks are impossible, or a retractable blade is extended
     *
     * @return A String giving the reason why the push attack is impossible
     */
    protected static String toHitIsImpossible(Game game, Entity attacker, Targetable target) {
        String physicalImpossible = PhysicalAttackAction.toHitIsImpossible(game, attacker, target);

        if (physicalImpossible != null) {
            return physicalImpossible;
        }

        if (attacker.getGrappled() != Entity.NONE) {
            return "Unit Grappled";
        }

        // can't push if carrying any cargo per TW
        if ((attacker instanceof Mek mek) && !(mek.canFireWeapon(Mek.LOC_LEFT_ARM)
              || mek.canFireWeapon(Mek.LOC_RIGHT_ARM))) {
            return Messages.getString("WeaponAttackAction.CantFireWhileCarryingCargo");
        }

        if ((attacker instanceof Mek mek) && mek.hasExtendedRetractableBlade()) {
            return "Extended retractable blade";
        }

        return null;
    }

    /**
     * To-hit number for the mek to push another mek
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);

        int targetId = Entity.NONE;
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getId();
        }

        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        }
        if (te == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't target a null entity!");
        }
        if (!game.onTheSameBoard(ae, target)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker and target are not on the same board.");
        }

        Hex attHex = game.getHexOf(ae);
        Hex targHex = game.getHexOf(te);

        if (attHex == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Entity #" + ae.getId() + " does not know its position.");
        }
        if (targHex == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Entity #" + te.getId() + " does not know its position.");
        }

        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int targetElevation = target.getElevation() + targHex.getLevel();

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);
        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        IBuilding bldg = null;
        if (targetInBuilding) {
            bldg = game.getBuildingAt(te.getBoardLocation()).orElse(null);
        }
        ToHitData toHit;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't target yourself");
        }

        // non-meks can't push
        if (!(ae instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-meks can't push");
        }

        // Quads can't push
        if (ae.entityIsQuad()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is a quad");
        }

        // LAM AirMeks can only push when grounded.
        if (ae.isAirborneVTOLorWIGE()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot push while airborne");
        }

        // Can only push meks
        if (!(te instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is not a mek");
        }

        // Can't push with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Arms are flipped to the rear. Can not push.");
        }

        // Can't target a transported entity.
        if (Entity.NONE != te.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target an entity conducting a swarm attack.
        if (Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check if both arms are present
        if (ae.isLocationBad(Mek.LOC_RIGHT_ARM) || ae.isLocationBad(Mek.LOC_LEFT_ARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
        }

        // check for no/minimal arms quirk
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No/minimal arms");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(Mek.LOC_RIGHT_ARM) || ae.weaponFiredFrom(Mek.LOC_LEFT_ARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Weapons fired from arm this turn");
        }

        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // target must be at same elevation
        if (attackerElevation != targetElevation) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not at same elevation");
        }

        // can't push mek making non-pushing displacement attack
        if (te.hasDisplacementAttack() && !te.isPushing()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is making a charge/DFA attack");
        }

        // can't push mek pushing another, different mek
        if (te.isPushing() && (te.getDisplacementAttack().getTargetId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is pushing another mek");
        }

        // can't do anything but counter-push if the target of another attack
        if (ae.isTargetOfDisplacementAttack()
              && (ae.findTargetedDisplacement().getEntityId() != target.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is the target of another push/charge/DFA");
        }

        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()
              && (te.findTargetedDisplacement().getEntityId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is the target of another push/charge/DFA");
        }

        // can't push airborne targets
        if (te.isAirborne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot push an airborne target.");
        }

        // check facing
        if (!target.getPosition().equals(ae.getPosition().translated(ae.getFacing()))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not directly ahead of feet");
        }

        // Tripods can only push targets in front arc based on torso facing per IO:AE p.158
        if (ae.isTripodMek() && !ComputeArc.isInArc(ae.getPosition(), ae.getSecondaryFacing(),
              target, Compute.ARC_FORWARD)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in torso front arc");
        }

        // can't push while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // can't push prone meks
        if (te.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is prone");
        }

        // Can't target units in buildings (from the outside).
        if (targetInBuilding) {
            if (!Compute.isInBuilding(game, ae)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is inside building");
            } else if (!game.getBoard(ae).getBuildingAt(ae.getPosition()).equals(bldg)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is inside different building");
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
              || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "You can not push a building (well, you can, but it won't do anything).");
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
              || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
              || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        String otherImpossible = toHitIsImpossible(game, ae, target);
        if (otherImpossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, otherImpossible);
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");
        toHit.addModifier(-1, "Push");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te, 0, inSameBuilding));

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RIGHT_ARM)) {
            toHit.addModifier(2, "Right Shoulder destroyed");
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LEFT_ARM)) {
            toHit.addModifier(2, "Left Shoulder destroyed");
        }

        // attacker is spotting
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, "attacker is spotting");
        }

        // water partial cover?
        if ((te.height() > 0) && (te.getElevation() == -1)
              && (targHex.terrainLevel(Terrains.WATER) == te.height())) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // evading
        if (te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        toHit.append(nightModifiers(game, target, null, ae, false));
        // side and elevation shouldn't matter

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if (((Mek) ae).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = ae.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_CENTER_TORSO);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // Attacking Weight Class Modifier.
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_ATTACK_PSR)) {
            if (ae.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                toHit.addModifier(-2, "Weight Class Attack Modifier");
            } else if (ae.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                toHit.addModifier(-1, "Weight Class Attack Modifier");
            }
        }

        if (((Mek) ae).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }

        // done!
        return toHit;
    }

    @Override
    public String toSummaryString(final Game game) {
        final String roll = this.toHit(game).getValueAsString();
        return Messages.getString("BoardView1.PushAttackAction", roll);
    }
}
