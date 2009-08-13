/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.actions;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.Player;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;

/**
 * The attacker pushes the target.
 */
public class PushAttackAction extends DisplacementAttackAction {
    /**
     * Static Serial.
     */
    private static final long serialVersionUID = 6878038939232914083L;

    public PushAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public PushAttackAction(int entityId, int targetType, int targetId,
            Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()));
    }

    /**
     * pushes are impossible when physical attacks are impossible, or a
     * retractable blade is extended
     *
     * @param game
     * @param ae
     * @param target
     * @return
     */
    protected static String toHitIsImpossible(IGame game, Entity ae,
            Targetable target) {
        String physicalImpossible = PhysicalAttackAction.toHitIsImpossible(
                game, ae, target);
        String extendedBladeImpossible = null;
        if ((ae instanceof Mech) && ((Mech) ae).hasExtendedRetractableBlade()) {
            extendedBladeImpossible = "Extended retractable blade";
        }
        if (physicalImpossible != null) {
            return physicalImpossible;
        }

        if ( ae.getGrappled() != Entity.NONE ) {
            return "Unit Grappled";
        }

        if(ae.isEvading()) {
            return "attacker is evading.";
        }

        if (!game.getOptions().booleanOption("friendly_fire")) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                    && ((((Entity)target).getOwnerId() == ae.getOwnerId())
                            || ((((Entity)target).getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() == ((Entity)target).getOwner().getTeam())))) {
                return "A friendly unit can never be the target of a direct attack.";
            }
        }

        return extendedBladeImpossible;
    }

    /**
     * To-hit number for the mech to push another mech
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);

        int targetId = Entity.NONE;
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }

        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't attack from a null entity!");
        }
        if (te == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't target a null entity!");
        }

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(te.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        final int targetElevation = target.getElevation()
                + targHex.getElevation();

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);
        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        Building bldg = null;
        if (targetInBuilding) {
            bldg = game.getBoard().getBuildingAt(te.getPosition());
        }
        ToHitData toHit = null;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't target yourself");
        }

        // non-mechs can't push
        if (!(ae instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-mechs can't push");
        }

        // Quads can't push
        if (ae.entityIsQuad()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is a quad");
        }

        // Can only push mechs
        if (!(te instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is not a mech");
        }

        // Can't push with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Arms are flipped to the rear. Can not push.");
        }

        // Can't target a transported entity.
        if (Entity.NONE != te.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if (Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is swarming a Mek.");
        }

        // check if both arms are present
        if (ae.isLocationBad(Mech.LOC_RARM) || ae.isLocationBad(Mech.LOC_LARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(Mech.LOC_RARM)
                || ae.weaponFiredFrom(Mech.LOC_LARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Weapons fired from arm this turn");
        }

        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // target must be at same elevation
        if (attackerElevation != targetElevation) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target not at same elevation");
        }

        // can't push mech making non-pushing displacement attack
        if (te.hasDisplacementAttack() && !te.isPushing()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is making a charge/DFA attack");
        }

        // can't push mech pushing another, different mech
        if (te.isPushing()
                && (te.getDisplacementAttack().getTargetId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is pushing another mech");
        }

        // can't do anything but counter-push if the target of another attack
        if (ae.isTargetOfDisplacementAttack()
                && (ae.findTargetedDisplacement().getEntityId() != target
                        .getTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Attacker is the target of another push/charge/DFA");
        }

        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()
                && (te.findTargetedDisplacement().getEntityId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is the target of another push/charge/DFA");
        }

        // check facing
        if (!target.getPosition().equals(
                ae.getPosition().translated(ae.getFacing()))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target not directly ahead of feet");
        }

        // can't push while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // can't push prone mechs
        if (te.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is prone");
        }

        // Can't target units in buildings (from the outside).
        if (targetInBuilding) {
            if (!Compute.isInBuilding(game, ae)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Target is inside building");
            } else if (!game.getBoard().getBuildingAt(ae.getPosition()).equals(
                    bldg)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Target is inside differnt building");
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

        // Set the base BTH
        int base = ae.getCrew().getPiloting() - 1;

        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te, 0, inSameBuilding));

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right Shoulder destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left Shoulder destroyed");
        }

        // attacker is spotting
        if (ae.isSpotting()) {
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

        //evading
        if(te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        toHit.append(nightModifiers(game, target, null, ae, false));
        // side and elevation shouldn't matter

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4,
                        "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        //Attacking Weight Class Modifier.
        if ( game.getOptions().booleanOption("tacops_attack_physical_psr") ) {
            if ( ae.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT ) {
                toHit.addModifier(-2, "Weight Class Attack Modifier");
            }else if ( ae.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM ) {
                toHit.addModifier(-1, "Weight Class Attack Modifier");
            }
        }

        if (((Mech)ae).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }

        // done!
        return toHit;
    }
}
