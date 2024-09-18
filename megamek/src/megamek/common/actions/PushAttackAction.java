/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.actions;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

/**
 * The attacker pushes the target.
 */
public class PushAttackAction extends DisplacementAttackAction {
    private static final long serialVersionUID = 6878038939232914083L;

    public PushAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public PushAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * pushes are impossible when physical attacks are impossible, or a
     * retractable blade is extended
     *
     * @param game   The current {@link Game}
     * @param ae
     * @param target
     * @return
     */
    protected static String toHitIsImpossible(Game game, Entity ae, Targetable target) {
        String physicalImpossible = PhysicalAttackAction.toHitIsImpossible(game, ae, target);

        if (physicalImpossible != null) {
            return physicalImpossible;
        }

        if (ae.getGrappled() != Entity.NONE) {
            return "Unit Grappled";
        }

        // can't push if carrying any cargo per TW
        if ((ae instanceof Mek) &&
                !((Mek) ae).canFireWeapon(Mek.LOC_LARM) ||
                !((Mek) ae).canFireWeapon(Mek.LOC_LARM)) {
            return Messages.getString("WeaponAttackAction.CantFireWhileCarryingCargo");
        }

        if ((ae instanceof Mek) && ((Mek) ae).hasExtendedRetractableBlade()) {
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

        Hex attHex = game.getBoard().getHex(ae.getPosition());
        Hex targHex = game.getBoard().getHex(te.getPosition());

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
        Building bldg = null;
        if (targetInBuilding) {
            bldg = game.getBoard().getBuildingAt(te.getPosition());
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

        // Can't target a entity conducting a swarm attack.
        if (Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check if both arms are present
        if (ae.isLocationBad(Mek.LOC_RARM) || ae.isLocationBad(Mek.LOC_LARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
        }

        // check for no/minimal arms quirk
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No/minimal arms");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(Mek.LOC_RARM) || ae.weaponFiredFrom(Mek.LOC_LARM)) {
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
            } else if (!game.getBoard().getBuildingAt(ae.getPosition()).equals(bldg)) {
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
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RARM)) {
            toHit.addModifier(2, "Right Shoulder destroyed");
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LARM)) {
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
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // Attacking Weight Class Modifier.
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)) {
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
