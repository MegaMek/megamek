/**
 * 
 */
package megamek.client.commands;

import java.util.Enumeration;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.Messages;
import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IAimingModes;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.AbstractEntityAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.WeaponAttackAction;

/**
 * @author dirk
 *
 */
public class FireCommand extends ClientCommand {

    private Entity entity;

    private Vector<AbstractEntityAction> attacks;
    
    /**
     * @param client
     * @param name
     * @param helpText
     */
    public FireCommand(Client client) {
        super(client, "fire", "used to shoot.");
        attacks = new Vector<AbstractEntityAction>();
    }

    /* (non-Javadoc)
     * @see megamek.client.commands.ClientCommand#run(java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        if(args.length > 1) {
            if(args[1].equalsIgnoreCase("ABORT")) {
                clearAttacks();
                return "Move aborted, all movement data cleared.";
            } else if(args[1].equalsIgnoreCase("SELECT")) {
                try {
                    clearAttacks();
                    int id = Integer.parseInt(args[2]);
                    entity = client.getEntity(id);
                    
                    return "Entity " + entity.toString() + " selected for firing.";
                } catch(Exception e) {
                    return "Not an entity ID or valid number." + e.toString();
                }
            } else if(entity != null) {
                if(args[1].equalsIgnoreCase("COMMIT")) {
                    commit();
                    return "Attacks send to the server";
                } else if(args.length > 2) {
                    if(args[1].equalsIgnoreCase("TARGET")) {
                        String str = "";
                        try {
                            Targetable target = client.getEntity(Integer.parseInt(args[2]));
                            if(args.length == 4 && args[3].equalsIgnoreCase("ALL")) {
                                for(Mounted weapon : entity.getWeaponList()) {
                                    if(weapon.canFire() && !weapon.isFired()) {
                                        fire(entity.getEquipmentNum(weapon), target);
                                    }
                                }
                                return "Fireing all remaining weapons at " + target.toString() + ".";
                            } else {
                                for(int i = 3; i < args.length; i++) {
                                    fire(Integer.parseInt(args[i]), target);
                                    str += "Firing weapon " + args[i] + " at " + target.toString() + "\n";
                                }
                            }
                        } catch(NumberFormatException nfe) {
                        }
                        
                        return str + " Invalid arguments.";
                    } else if(args[1].equalsIgnoreCase("LIST")) {
                        try {
                            Targetable target = client.getEntity(Integer.parseInt(args[2]));
                            if(target != null) {
                                String str = " Weapons for " + entity + " at " + target.toString() + ":\n";
                                
                                for(Mounted weapon : entity.getWeaponList()) {
                                    str += "(" + entity.getEquipmentNum(weapon) + ") " + weapon.getName() + " = " + calculateToHit(entity.getEquipmentNum(weapon), target) + "\n";
                                }
                                
                                return str;
                            }
                        } catch(NumberFormatException nfe) {
                        }
                        
                        return "Invalid Target ID.";
                    } else if(args[1].equalsIgnoreCase("TWIST")) {
                        return "Not implemented yet.";
                    }
                }
            } else {
                return "No entity selected, first select an entity to shoot from.";
            }
        }
        clearAttacks();
        return "No arguments given, or there was an error parsing the arguments. All attack data cleared.";
    }
    
    /**
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet
        if (entity == null) {
            return;
        }
        
        // remove attacks, set weapons available again
        Enumeration<AbstractEntityAction> i = attacks.elements();
        while (i.hasMoreElements()) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                entity.getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        attacks.removeAllElements();
        
        // remove temporary attacks from game & board
        client.game.removeActionsFor(entity.getId());
        
        // restore any other movement to default
        entity.setSecondaryFacing(entity.getFacing());
        entity.setArmsFlipped(false);
        entity = null;
    }
    
    private void fire(int weaponNum, Targetable target) {
        // get the selected weaponnum
        Mounted mounted = entity.getEquipment(weaponNum);
        
        // validate
        if (entity == null || target == null || mounted == null
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid"); //$NON-NLS-1$
        }
        
        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight(target);
        }

        WeaponAttackAction waa = new WeaponAttackAction(entity.getId(), target.getTargetType(),
                target.getTargetId(), weaponNum);

        if (mounted.getLinked() != null &&
                ((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA) {
            Mounted ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(entity.getEquipmentNum(ammoMount));
            if (((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)
                    && (ammoType.getAmmoType() == AmmoType.T_LRM
                            || ammoType.getAmmoType() == AmmoType.T_MML))
                    || ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
                
                waa.setOtherAttackInfo(50); ///hardcode vibrobomb setting for now.
            }
        }

        waa.setAimedLocation(Entity.LOC_NONE);
        waa.setAimingMode(IAimingModes.AIM_MODE_NONE);

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        client.game.addAction(waa);
        
        // set the weapon as used
        mounted.setUsedThisRound(true);
    }
    
    private void doSearchlight(Targetable target) {
        // validate
        if (entity == null || target == null) {
            throw new IllegalArgumentException("current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(client.game, entity.getId(), target, null))
            return;

        //create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(entity.getId(), target.getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        client.game.addAction(saa);
    }
    
    private String calculateToHit(int weaponId, Targetable target) {
        ToHitData toHit;
        String str = "No Data";
        if (target != null && weaponId != -1 && entity != null) {
            str = "";
            toHit = WeaponAttackAction.toHit(client.game, entity.getId(), target, weaponId, Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE);
            //str += "Target: " + target.toString();
            
            str += " Range: " + entity.getPosition().distance(target.getPosition());
            
            Mounted m = entity.getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                str += " Can't shoot: " + Messages.getString("FiringDisplay.alreadyFired");
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                str += " Can't shoot: " + Messages.getString("FiringDisplay.autoFiringWeapon");
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                str += " Can't shoot: " + toHit.getValueAsString();
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                str += " Automatic Failure: " + toHit.getValueAsString();
            } else {
                str += " To hit: " + toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)";
            }
            str += " To Hit modifiers: " + toHit.getDesc();
        }
        return str;
    }
    
    /**
     * Called when the current entity is done firing.  Send out our attack
     * queue to the server.
     */
    private void commit() {
        // For bug 1002223
        // Re-compute the to-hit numbers by adding in correct order.
        Vector<EntityAction> newAttacks = new Vector<EntityAction>();
        for (Enumeration<AbstractEntityAction> e = attacks.elements(); e.hasMoreElements();) {
            AbstractEntityAction o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), Compute.ARC_FORWARD);
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(), waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            } else {
                newAttacks.addElement(o);
            }
        }
        for (Enumeration<AbstractEntityAction> e = attacks.elements(); e.hasMoreElements();) {
            Object o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), Compute.ARC_FORWARD);
                if (!curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(), waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            }
        }
        
        // send out attacks
        client.sendAttackData(entity.getId(), newAttacks);
        
        // clear queue
        attacks.removeAllElements();
    }

}
