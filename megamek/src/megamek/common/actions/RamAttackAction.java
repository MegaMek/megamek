/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */

/*
 * RamAttackAction.java
 * 
 * Created on May 28, 2008
 */

package megamek.common.actions;

import java.util.Enumeration;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.FighterSquadron;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.Jumpship;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.SpaceStation;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.Warship;

/**
 * Represents one unit charging another. Stores information about where the
 * target is supposed to be for the charge to be successful, as well as normal
 * attack info.
 * 
 * @author Ben Mazur
 */
public class RamAttackAction extends AbstractAttackAction {

    /**
     * 
     */
    private static final long serialVersionUID = -3549351664290057785L;

    public RamAttackAction(Entity attacker, Targetable target) {
        this(attacker.getId(), target.getTargetType(), target.getTargetId(),
                target.getPosition());
    }

    public RamAttackAction(int entityId, int targetType, int targetId,
            Coords targetPos) {
        super(entityId, targetType, targetId);
    }

    /**
     * To-hit number for a ram, assuming that movement has been handled
     */
    public ToHitData toHit(IGame game) {
        final Entity entity = game.getEntity(getEntityId());
        return toHit(game, game.getTarget(getTargetType(), getTargetId()),
                     entity.getPosition(), entity.getElevation(), 
                     entity.getPriorPosition(), entity.moved);
    }
    
    /**
     * To-hit number for a ram, assuming that movement has been handled
     */
    public ToHitData toHit(IGame game, Targetable target, Coords src,
            int elevation, Coords priorSrc, EntityMovementType movement) {
        final Entity ae = getEntity(game);

        // arguments legal?
        if (ae == null) {
            throw new IllegalStateException("Attacker is null");
        }

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is null");
        }
        
        if(!(ae instanceof Aero)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is not Aero");
        }
        
        if(!(target instanceof Aero)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is not Aero");
        }   
        
        if(ae instanceof FighterSquadron || target instanceof FighterSquadron) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "fighter squadrons may not ram nor be the target of a ramming attc");
        }

        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        } else {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid Target");
        }
        
        if (!game.getOptions().booleanOption("friendly_fire")) {
            // a friendly unit can never be the target of a direct attack.
            if (target.getTargetType() == Targetable.TYPE_ENTITY
                    && (((Entity)target).getOwnerId() == ae.getOwnerId()
                            || (((Entity)target).getOwner().getTeam() != IPlayer.TEAM_NONE
                                    && ae.getOwner().getTeam() != IPlayer.TEAM_NONE
                                    && ae.getOwner().getTeam() == ((Entity)target).getOwner().getTeam())))
                return new ToHitData(TargetRoll.IMPOSSIBLE, "A friendly unit can never be the target of a direct attack.");
        }
        IHex attHex = game.getBoard().getHex(src);
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = elevation + attHex.getLevel();
        final int targetElevation = target.getElevation()
                + targHex.getLevel();
        ToHitData toHit = null;
 
        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't target yourself");
        }

        // Can't target a transported entity.
        if (Entity.NONE != te.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is a passenger.");
        }

        // check range
        if (src.distance(target.getPosition()) > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // target must be at same elevation level
        if (attackerElevation != targetElevation) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target must be at the same elevation level");
        }

        // can't attack Aero making a different ramming attack
        if (te.isRamming()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is already making a ramming attack");
        }

        //attacker 
        
        // target must have moved already
        if (!te.isDone()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target must be done with movement");
        }
        
        //Set the base BTH       
        int base = 6 + te.getCrew().getPiloting() - ae.getCrew().getPiloting(); 
        
        toHit = new ToHitData(base, "base");

        Aero a = (Aero)ae;
        
        //target type
        if(target instanceof SpaceStation) {
            toHit.addModifier(-1,"target is a space station");
        } else if(target instanceof Warship) {
            toHit.addModifier(+1,"target is a warship");
        } else if(target instanceof Jumpship) {
            toHit.addModifier(+0,"target is a jumpship");
        } else if(target instanceof Dropship) {
            toHit.addModifier(+2,"target is a dropship");
        } else {
            toHit.addModifier(+4,"target is a fighter/small craft");
        }
        
        //attacker type
        if(a instanceof SpaceStation) {
            toHit.addModifier(+0,"attacker is a space station");
        } else if(a instanceof Warship) {
            toHit.addModifier(+1,"attacker is a warship");
        } else if(a instanceof Jumpship) {
            toHit.addModifier(+0,"attacker is a jumpship");
        } else if(a instanceof Dropship) {
            toHit.addModifier(-1,"attacker is a dropship");
        } else {
            toHit.addModifier(-2,"attacker is a fighter/small craft");
        }
        
        //can the target unit move
        if(target.isImmobile() || te.getWalkMP() == 0)
            toHit.addModifier(-2,"target cannot spend thrust");
            
        //sensor damage
        if(a.getSensorHits() > 0) 
            toHit.addModifier(+1, "sensor damage");
            
        //avionics damage
        int avionics = a.getAvionicsHits();
        if(avionics > 3) 
            avionics = 3;
        if(avionics > 0)
            toHit.addModifier(avionics, "avionics damage");
        
        //evading bonuses
        if (target.getTargetType() == Targetable.TYPE_ENTITY && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }
        
        //determine hit direction
        toHit.setSideTable(te.sideTable(priorSrc));
        
        toHit.setHitTable(ToHitData.HIT_NORMAL);

        // done!
        return toHit;
    }

    /**
     * Checks if a ram can hit the target, taking account of movement
     */
    public ToHitData toHit(IGame game, MovePath md) {
        final Entity ae = game.getEntity(getEntityId());
        final Targetable target = getTarget(game);
        Coords ramSrc = ae.getPosition();
        int ramEl = ae.getElevation();
        Coords priorSrc = md.getSecondFinalPosition(ae.getPosition());
        MoveStep ramStep = null;

        // let's just check this
        if (!md.contains(MoveStepType.RAM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Ram action not found in movement path");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }
            if (step.getType() == MoveStepType.RAM) {
                ramStep = step;
                ramSrc = step.getPosition();
                ramEl = step.getElevation();
            } 
        }

        // need to reach target
        if (ramStep == null
                || !target.getPosition().equals(ramStep.getPosition())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Could not reach target with movement");
        }
        
        return toHit(game, target, ramSrc, ramEl, priorSrc, ramStep.getMovementType(true));
    }

    /**
     * Damage that an Aero does on a successful ramming attack
     * 
     */
   public static int getDamageFor(Aero entity, Entity target) {
       int avel = entity.getCurrentVelocity();
       int tvel = 0;
       if(target instanceof Aero) {
           tvel = ((Aero)target).getCurrentVelocity();
       }
       return getDamageFor(entity, target, entity.getPriorPosition(), avel, tvel);
   }
   
   public static int getDamageFor(Aero entity, Entity target, Coords atthex, int avel, int tvel) {
       int netv = Compute.getNetVelocity(atthex, target, avel, tvel);
       return (int) Math.ceil(
               (entity.getWeight() / 10.0) * netv);
   }
     
   /**
    * Damage that an Aero suffers after a successful charge.
    */
   public static int getDamageTakenBy(Aero entity, Entity target) {
       int avel = entity.getCurrentVelocity();
       int tvel = 0;
       if(target instanceof Aero) {
           tvel = ((Aero)target).getCurrentVelocity();
       }
       return getDamageTakenBy(entity, target, entity.getPriorPosition(), avel, tvel);
   }
   
   public static int getDamageTakenBy(Aero entity, Entity target, Coords atthex, int avel, int tvel) {
       int netv = Compute.getNetVelocity(atthex, target, avel, tvel);
       return (int) Math.ceil(
               (target.getWeight() / 10.0) * netv);
   }

}
