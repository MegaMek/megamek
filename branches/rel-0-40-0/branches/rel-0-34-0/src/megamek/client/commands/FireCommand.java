/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.commands;

import java.util.Enumeration;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
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
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;

/**
 * @author dirk
 */
public class FireCommand extends ClientCommand {
    private int cen = Entity.NONE;

    private Vector<AbstractEntityAction> attacks;

    /**
     * @param client
     * @param name
     * @param helpText
     */
    public FireCommand(Client client) {
        super(client, "fire", "used to shoot. See #fire HELP for more details.");
        attacks = new Vector<AbstractEntityAction>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.client.commands.ClientCommand#run(java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("ABORT")) {
                clearAttacks();
                return "Move aborted, all movement data cleared.";
            } else if (args[1].equalsIgnoreCase("SELECT")) {
                try {
                    clearAttacks();
                    cen = Integer.parseInt(args[2]);

                    return "Entity " + ce().toString()
                            + " selected for firing.";
                } catch (Exception e) {
                    return "Not an entity ID or valid number." + e.toString();
                }
            } else if (args[1].equalsIgnoreCase("HELP")) {
                return "Available commands:\n"
                        + "#fire ABORT = aborts planed fireing and deselect unit.\n"
                        + "#fire SELECT unitID = Selects the unit named unit ID for fireing. This is a prerequisite for all commands listed after this.\n"
                        + "#fire COMMIT = executs the current fireing plan.\n"
                        + "#fire LIST unitID = List targeting information for all weapons at the specified target. This is currently the only way to get weapon IDs.\n"
                        + "#fire TWIST heading = used for torso twisitng, the heading being to which direction (N, NE, SE, etc) to try and turn.\n"
                        + "#fire TARGET unitID weaponID1 weaponID2 ... = fires all specified weapons at the specified target. Any number of weapons may be specified.\n"
                        + "#fire TARGET unitID ALL = fires all remaining weapons at the specified target.\n";
            } else if (ce() != null) {
                if (args[1].equalsIgnoreCase("COMMIT")) {
                    commit();
                    return "Attacks send to the server";
                } else if (args.length > 2) {
                    if (args[1].equalsIgnoreCase("TARGET")) {
                        String str = "";
                        try {
                            Targetable target = client.getEntity(Integer
                                    .parseInt(args[2]));
                            if (args.length == 4
                                    && args[3].equalsIgnoreCase("ALL")) {
                                for (Mounted weapon : ce().getWeaponList()) {
                                    if (weapon.canFire() && !weapon.isFired()) {
                                        fire(ce().getEquipmentNum(weapon),
                                                target);
                                    }
                                }
                                return "Fireing all remaining weapons at "
                                        + target.toString() + ".";
                            } else {
                                for (int i = 3; i < args.length; i++) {
                                    fire(Integer.parseInt(args[i]), target);
                                    str += "Firing weapon " + args[i] + " at "
                                            + target.toString() + "\n";
                                }
                            }
                        } catch (NumberFormatException nfe) {
                        }

                        return str + " Invalid arguments.";
                    } else if (args[1].equalsIgnoreCase("LIST")) {
                        try {
                            Targetable target = client.getEntity(Integer
                                    .parseInt(args[2]));
                            if (target != null) {
                                String str = " Weapons for " + ce() + " at "
                                        + target.toString() + ":\n";

                                for (Mounted weapon : ce().getWeaponList()) {
                                    str += "("
                                            + ce().getEquipmentNum(weapon)
                                            + ") "
                                            + weapon.getName()
                                            + " = "
                                            + calculateToHit(ce()
                                                    .getEquipmentNum(weapon),
                                                    target) + "\n";
                                }

                                return str;
                            }
                        } catch (NumberFormatException nfe) {
                        }

                        return "Invalid Target ID.";
                    } else if (args[1].equalsIgnoreCase("TWIST")
                            && args.length > 2) {
                        torsoTwist(getDirection(args[2]));
                        return "Torso-twisted (or rotated turret). All attacks planned until now have been clearned.";
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
        // We may not have an ce() selected yet
        if (ce() == null) {
            return;
        }

        // remove attacks, set weapons available again
        Enumeration<AbstractEntityAction> i = attacks.elements();
        while (i.hasMoreElements()) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        attacks.removeAllElements();

        // remove temporary attacks from game & board
        client.game.removeActionsFor(cen);

        // restore any other movement to default
        ce().setSecondaryFacing(ce().getFacing());
        ce().setArmsFlipped(false);
        cen = Entity.NONE;
    }

    private void torsoTwist(int target) {
        Enumeration<AbstractEntityAction> i = attacks.elements();
        while (i.hasMoreElements()) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        attacks.removeAllElements();

        // remove temporary attacks from game & board
        client.game.removeActionsFor(cen);

        // restore any other movement to default
        ce().setSecondaryFacing(ce().getFacing());
        ce().setArmsFlipped(false);

        int direction = ce().clipSecondaryFacing(target);
        attacks.addElement(new TorsoTwistAction(cen, direction));
        ce().setSecondaryFacing(direction);
    }

    private void fire(int weaponNum, Targetable target) {
        // get the selected weaponnum
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if (ce() == null || target == null || mounted == null
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException(
                    "current fire parameters are invalid"); //$NON-NLS-1$
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight(target);
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen, target
                .getTargetType(), target.getTargetId(), weaponNum);

        if (mounted.getLinked() != null
                && ((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA) {
            Mounted ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(ce().getEquipmentNum(ammoMount));
            if (((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) && (ammoType
                    .getAmmoType() == AmmoType.T_LRM || ammoType.getAmmoType() == AmmoType.T_MML))
                    || ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV) {

                waa.setOtherAttackInfo(50); // /hardcode vibrobomb setting for
                                            // now.
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
        if (ce() == null || target == null) {
            throw new IllegalArgumentException(
                    "current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(client.game, cen, target, null))
            return;

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target
                .getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        client.game.addAction(saa);
    }

    private String calculateToHit(int weaponId, Targetable target) {
        ToHitData toHit;
        String str = "No Data";
        if (target != null && weaponId != -1 && ce() != null) {
            str = "";
            toHit = WeaponAttackAction.toHit(client.game, cen, target,
                    weaponId, Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE);
            // str += "Target: " + target.toString();

            str += " Range: "
                    + ce().getPosition().distance(target.getPosition());

            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                str += " Can't shoot: "
                        + Messages.getString("FiringDisplay.alreadyFired");
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                str += " Can't shoot: "
                        + Messages.getString("FiringDisplay.autoFiringWeapon");
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                str += " Can't shoot: " + toHit.getValueAsString();
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                str += " Automatic Failure: " + toHit.getValueAsString();
            } else {
                str += " To hit: " + toHit.getValueAsString() + " ("
                        + Compute.oddsAbove(toHit.getValue()) + "%)";
            }
            str += " To Hit modifiers: " + toHit.getDesc();
        }
        return str;
    }

    /**
     * Called when the current ce() is done firing. Send out our attack queue to
     * the server.
     */
    private void commit() {
        // For bug 1002223
        // Re-compute the to-hit numbers by adding in correct order.
        Vector<EntityAction> newAttacks = new Vector<EntityAction>();
        for (Enumeration<AbstractEntityAction> e = attacks.elements(); e
                .hasMoreElements();) {
            AbstractEntityAction o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target.getPosition(),
                        Compute.ARC_FORWARD);
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa
                            .getEntityId(), waa.getTargetType(), waa
                            .getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            } else {
                newAttacks.addElement(o);
            }
        }
        for (Enumeration<AbstractEntityAction> e = attacks.elements(); e
                .hasMoreElements();) {
            Object o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target.getPosition(),
                        Compute.ARC_FORWARD);
                if (!curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa
                            .getEntityId(), waa.getTargetType(), waa
                            .getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            }
        }

        // send out attacks
        client.sendAttackData(cen, newAttacks);

        // clear queue
        attacks.removeAllElements();
    }

    /**
     * Returns the current Entity.
     */
    public Entity ce() {
        return client.game.getEntity(cen);
    }
}
