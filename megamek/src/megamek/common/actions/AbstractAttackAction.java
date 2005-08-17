/**
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

import java.util.Enumeration;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.Mech;
import megamek.common.WeaponType;
import megamek.common.actions.SearchlightAttackAction;

/**
 * Abstract superclass for any action where an entity is attacking another
 * entity.
 */
public abstract class AbstractAttackAction
    extends AbstractEntityAction
    implements AttackAction
{
    private int targetType;
    private int targetId;
    
    // default to attacking an entity, since this is what most of them are
    public AbstractAttackAction(int entityId, int targetId) {
        super(entityId);
        this.targetType = Targetable.TYPE_ENTITY;
        this.targetId = targetId;
    }
    
    public AbstractAttackAction(int entityId, int targetType, int targetId) {
        super(entityId);
        this.targetType = targetType;
        this.targetId = targetId;
    }
    
    public int getTargetType() {
        return targetType;
    }
    
    public int getTargetId() {
        return targetId;
    }
    
    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }
    
    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
    
    public Targetable getTarget(IGame g) {
        return g.getTarget(getTargetType(), getTargetId());
    }
    
    public Entity getEntity(IGame g) {
        return g.getEntity(getEntityId());
    }
    
    /**
     * used by the toHit of derived classes
     * atype may be null if not using an ammo based weapon
    */
    public static ToHitData nightModifiers(IGame game, Targetable target, AmmoType atype, Entity attacker) {
        ToHitData toHit = null;
        if(game.getOptions().booleanOption("night_battle")) {
            Entity te = null;
            if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
                te = (Entity) target;
            }
            toHit = new ToHitData();

            //The base night penalty
            int night_modifier;
            if(game.getOptions().booleanOption("dusk")) {
                night_modifier = 1;
                toHit.addModifier(night_modifier, "Dusk");
            } else {
                night_modifier = 2;
                toHit.addModifier(night_modifier, "Night");
            }

            boolean illuminated = false;
            if(te!=null) {
                illuminated = te.isIlluminated();
                //hack for unresolved actions so client shows right BTH
                if(!illuminated) {
                    for(Enumeration actions=game.getActions();actions.hasMoreElements();) {
                        Object a = actions.nextElement();
                        if(a instanceof SearchlightAttackAction) {
                            SearchlightAttackAction saa = (SearchlightAttackAction)a;
                            if(saa.willIlluminate(game, te)) {
                                illuminated = true;
                                break;
                            }
                        }
                    }
                }
            }
            //Searchlights reduce the penalty to zero
            if(te!=null && te.isUsingSpotlight()) {
                toHit.addModifier(-night_modifier, "target using searchlight");
                night_modifier = 0;
            }
            else if(illuminated) {
                toHit.addModifier(-night_modifier, "target illuminated by searchlight");
                night_modifier = 0;
            }
            //Ignored with EI system & implants
            else if(attacker.hasActiveEiCockpit()) {
                toHit.addModifier(-night_modifier, "EI system");
                night_modifier = 0;
            }
            //So do flares
            else if(game.isPositionIlluminated(target.getPosition())) {
                toHit.addModifier(-night_modifier, "target illuminated by flare");
                night_modifier = 0;
            }
            //Certain ammunitions reduce the penalty
            else if(atype != null) {
                if(atype.getAmmoType() == AmmoType.T_AC &&
                   (atype.getMunitionType() == AmmoType.M_INCENDIARY_AC || 
                   atype.getMunitionType() == AmmoType.M_TRACER)) {
                    toHit.addModifier(-1, "incendiary/tracer ammo");
                    night_modifier--;
                }
            }
            //Laser heatsinks
            if(night_modifier > 0 && te != null && te instanceof Mech && ((Mech)te).hasLaserHeatSinks()) {
                boolean lhsused=false;
                if(te.heat > 0) {
                    toHit.addModifier(-night_modifier, "target overheated with laser heatsinks");
                    night_modifier=0;
                }
                //actions that generate heat give a -1 modifier
                else if(te.heatBuildup > 0 || te.isStealthActive()) {
                    lhsused=true;
                }
                else {
                    //Unfortunately, we can't just check weapons fired by the target
                    //because isUsedThisRound() is not valid if the attacker declared first.
                    //therefore, enumerate WeaponAttackActions...
                    for(Enumeration actions=game.getActions();actions.hasMoreElements();) {
                        Object a = actions.nextElement();
                        if(a instanceof WeaponAttackAction) {
                            WeaponAttackAction waa = (WeaponAttackAction)a;
                            if(waa.getEntityId() == te.getId()) {
                                Mounted weapon = te.getEquipment(waa.getWeaponId());
                                WeaponType wtype = (WeaponType)weapon.getType();
                                if(wtype.getHeat() != 0 ||
                                   weapon.isRapidfire()) {
                                    //target fired a weapon that generates heat
                                    lhsused = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if(lhsused) {
                    toHit.addModifier(-1, "target uses laser heatsinks");
                }
            }
        }
        return toHit;
    }
    
}
