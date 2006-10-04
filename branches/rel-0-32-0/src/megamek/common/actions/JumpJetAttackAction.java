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

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.ToHitData;

/**
 * The attacker kicks the target.
 */
public class JumpJetAttackAction extends PhysicalAttackAction
{
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    
    private int leg;
    
    public JumpJetAttackAction(int entityId, int targetId, int leg) {
        super(entityId, targetId);
        this.leg = leg;
    }
    
    public JumpJetAttackAction(int entityId, int targetType, int targetId, int leg) {
        super(entityId, targetType, targetId);
        this.leg = leg;
    }
    
    public int getLeg() {
        return leg;
    }
    
    public void setLeg(int leg) {
        this.leg = leg;
    }
    
    /**
     * Damage that the specified mech does with a JJ attack
     */
    public static int getDamageFor(Entity entity, int leg) {
        
        if(leg == BOTH)
            return getDamageFor(entity,LEFT) + getDamageFor(entity,RIGHT);
        
        int[] kickLegs = new int[2];
        if ( entity.entityIsQuad() && !entity.isProne()) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }
        
        final int legLoc = (leg == RIGHT) ? kickLegs[0] : kickLegs[1];

        // underwater damage is 0
        if (entity.getLocationStatus(legLoc) == ILocationExposureStatus.WET) {
            return 0;
        }

        int damage = 0;
        for(Mounted m : entity.getMisc()) {
            if(m.getType().hasFlag(MiscType.F_JUMP_JET) 
                    && m.isReady()
                    && m.getLocation() == legLoc) {
                damage += 3 * m.getType().getCriticals(entity); //assumption: IJJ do 2 heat 6 damage
            }
        }

        return damage;
    }
    
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()), getLeg());
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                      Targetable target, int leg) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null)
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't attack from a null entity!");

        if(!game.getOptions().booleanOption("maxtech_new_physicals"))
            return new ToHitData(ToHitData.IMPOSSIBLE, "no MaxTech physicals");

        String impossible = toHitIsImpossible(game, ae, target);
        if(impossible != null) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "impossible");
        }

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        final int attackerHeight = attackerElevation + ae.getHeight();
        final int targetElevation = target.getElevation() + targHex.getElevation();
        final int targetHeight = targetElevation + target.getHeight();

        int[] kickLegs = new int[2];
        if ( ae.entityIsQuad() && !ae.isProne() ) {
            kickLegs[0] = Mech.LOC_RARM;
            kickLegs[1] = Mech.LOC_LARM;
        } else {
            kickLegs[0] = Mech.LOC_RLEG;
            kickLegs[1] = Mech.LOC_LLEG;
        }

        ToHitData toHit;

        // arguments legal?
        if (leg != RIGHT && leg != LEFT && leg != BOTH) {
            throw new IllegalArgumentException("Leg must be LEFT or RIGHT");
        }

        // non-mechs can't kick
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't kick");
        }
        
        if(leg == BOTH && !ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Only prone mechs can attack with both legs");
        }

        // check if legs are present & working
        if ((ae.isLocationBad(kickLegs[0]) && (leg == BOTH || leg == LEFT))
            || (ae.isLocationBad(kickLegs[1]) && (leg == BOTH || leg == RIGHT))) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Leg missing");
        }
        
        // check if attacker even has jump jets!
        for(Mounted m : ae.getMisc()) {
            boolean hasJJ=false;
            int loc = m.getLocation();
            if(m.getType().hasFlag(MiscType.F_JUMP_JET) 
                    && m.isReady()
                    && ((loc == kickLegs[0] && (leg == BOTH || leg == LEFT))
                      ||(loc == kickLegs[1] && (leg == BOTH || leg == RIGHT)))) {
                hasJJ = true;
                break;
            }
            if(!hasJJ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Jump jets missing or destroyed");
            }
        }

        // check if attacker has fired leg-mounted weapons
        for (Mounted mounted : ae.getWeaponList()) {
            if (mounted.isUsedThisRound()) {
                int loc = mounted.getLocation();
                if(((leg == BOTH || leg == LEFT) && loc == kickLegs[0]) 
                        || ((leg == BOTH || leg == RIGHT) && loc == kickLegs[1])) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from leg this turn");
                }
            }
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if ( 1 != range ) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Enemy must be at range 1");
        }

        // check elevation
        if (!ae.isProne() && attackerHeight - targetHeight != 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        if (ae.isProne() && (attackerHeight > targetHeight || attackerHeight < targetElevation)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // check facing
        if(!ae.isProne()) {
            if (!target.getPosition().equals(ae.getPosition().translated(ae.getFacing()))) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target not directly ahead of feet");
            }
        } else {
            if (!target.getPosition().equals(ae.getPosition().translated(( 3 + ae.getFacing()) % 6 ))) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target not directly behind of feet");
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target instanceof GunEmplacement) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        //Set the base BTH
        int base = 5;

        // Level 3 rule: the BTH is PSR + 2
        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() + 2;
        }

        // Start the To-Hit
        toHit = new ToHitData(base, "base");
        
        setCommonModifiers(toHit, game, ae, target);

        // +2 for prone
        if ( ae.isProne() ) {
            toHit.addModifier( 2, "Attacker is prone" );
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae,target));

        // done!
        return toHit;
    }
}
