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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Vector;

/**
 * @author dirk
 */
public class FireCommand extends ClientCommand {
    private int cen = Entity.NONE;

    private Vector<AbstractEntityAction> attacks;

    /**
     * @param client
     */
    public FireCommand(Client client) {
        super(client, "fire", "used to shoot. See #fire HELP for more details.");
        attacks = new Vector<>();
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

                    return "Entity " + ce().toString() + " selected for firing.";
                } catch (Exception e) {
                    return "Not an entity ID or valid number." + e;
                }
            } else if (args[1].equalsIgnoreCase("HELP")) {
                return "Available commands:\n"
                       + "#fire ABORT = aborts planed firing and deselect unit.\n"
                       + "#fire SELECT unitID = Selects the unit named unit ID for firing. This is a prerequisite " +
                       "for all commands listed after this.\n"
                       + "#fire COMMIT = executes the current firing plan.\n"
                       + "#fire LIST unitID = List targeting information for all weapons at the specified target. " +
                       "This is currently the only way to get weapon IDs.\n"
                       + "#fire TWIST heading = used for torso twisting, the heading being to which direction (N, " +
                       "NE, SE, etc) to try and turn.\n"
                       + "#fire TARGET unitID weaponID1 weaponID2 ... = fires all specified weapons at the specified" +
                       " target. Any number of weapons may be specified.\n"
                       + "#fire TARGET unitID ALL = fires all remaining weapons at the specified target.\n";
            } else if (ce() != null) {
                if (args[1].equalsIgnoreCase("COMMIT")) {
                    commit();
                    return "Attacks send to the server";
                } else if (args.length > 2) {
                    if (args[1].equalsIgnoreCase("TARGET")) {
                        String str = "";
                        try {
                            Targetable target = getClient().getEntity(Integer.parseInt(args[2]));
                            if ((args.length == 4) && args[3].equalsIgnoreCase("ALL")) {
                                for (Mounted weapon : ce().getWeaponList()) {
                                    if (weapon.canFire() && !weapon.isFired()) {
                                        fire(ce().getEquipmentNum(weapon), target);
                                    }
                                }
                                return "Firing all remaining weapons at " + target.toString() + ".";
                            } else {
                                for (int i = 3; i < args.length; i++) {
                                    fire(Integer.parseInt(args[i]), target);
                                    str += "Firing weapon " + args[i] + " at "
                                           + target.toString() + "\n";
                                }
                            }
                        } catch (Exception ignored) {

                        }

                        return str + " Invalid arguments.";
                    } else if (args[1].equalsIgnoreCase("LIST")) {
                        try {
                            Targetable target = getClient().getEntity(Integer.parseInt(args[2]));
                            if (target != null) {
                                String str = " Weapons for " + ce() + " at " + target + ":\n";

                                for (Mounted weapon : ce().getWeaponList()) {
                                    str += "("
                                           + ce().getEquipmentNum(weapon)
                                           + ") "
                                           + weapon.getName()
                                           + " = "
                                           + calculateToHit(ce().getEquipmentNum(weapon), target) + "\n";
                                }

                                return str;
                            }
                        } catch (Exception ignored) {

                        }

                        return "Invalid Target ID.";
                    } else if (args[1].equalsIgnoreCase("TWIST")) {
                        torsoTwist(getDirection(args[2]));
                        return "Torso-twisted (or rotated turret). All attacks planned until now have been cleared.";
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
        getClient().getGame().removeActionsFor(cen);

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
        getClient().getGame().removeActionsFor(cen);

        // restore any other movement to default
        if (!ce().getAlreadyTwisted()) {
            ce().setSecondaryFacing(ce().getFacing());
            ce().setArmsFlipped(false);

            int direction = ce().clipSecondaryFacing(target);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
        }
    }

    private void fire(int weaponNum, Targetable target) {
        // get the selected weaponnum
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if (ce() == null || target == null || mounted == null
            || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight(target);
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen, target
                .getTargetType(), target.getId(), weaponNum);

        if (mounted.getLinked() != null && ((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA) {
            Mounted ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
            EnumSet<AmmoType.Munitions> ammoMunitionType = ammoType.getMunitionType();
            waa.setAmmoMunitionType(ammoMunitionType);
            waa.setAmmoCarrier(ammoMount.getEntity().getId());
            if (((ammoMunitionType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB))
                    && (ammoType.getAmmoType() == AmmoType.T_LRM
                    || ammoType.getAmmoType() == AmmoType.T_MML
                    || ammoType.getAmmoType() == AmmoType.T_LRM_IMP))
                    || ammoType.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV)) {

                waa.setOtherAttackInfo(50); // /hardcode vibrobomb setting for
                // now.
            }
        }

        waa.setAimedLocation(Entity.LOC_NONE);
        waa.setAimingMode(AimingMode.NONE);

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        getClient().getGame().addAction(waa);

        // set the weapon as used
        mounted.setUsedThisRound(true);
    }

    private void doSearchlight(Targetable target) {
        // validate
        if (ce() == null || target == null) {
            throw new IllegalArgumentException("current searchlight parameters are invalid");
        }

        if (!SearchlightAttackAction.isPossible(getClient().getGame(), cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target.getTargetType(),
                target.getId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        getClient().getGame().addAction(saa);
    }

    private String calculateToHit(int weaponId, Targetable target) {
        ToHitData toHit;
        String str = "No Data";
        if (target != null && weaponId != -1 && ce() != null) {
            str = "";
            toHit = WeaponAttackAction.toHit(getClient().getGame(), cen, target, weaponId,
                    Entity.LOC_NONE, AimingMode.NONE, false);

            str += " Range: " + ce().getPosition().distance(target.getPosition());

            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                str += " Can't shoot: "
                       + Messages.getString("FiringDisplay.alreadyFired");
            } else if ((m.getType().hasFlag(WeaponType.F_AUTO_TARGET) && !m.curMode().equals(Weapon.MODE_AMS_MANUAL))
                    || (m.hasModes() && m.curMode().equals("Point Defense"))) {
                str += " Can't shoot: "
                       + Messages.getString("FiringDisplay.autoFiringWeapon");
            } else if (getClient().getGame().getPhase().isFiring() && m.isInBearingsOnlyMode()) {
                str += " Can't shoot: "
                        + Messages.getString("FiringDisplay.bearingsOnlyWrongPhase");
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                str += " Automatic Failure: " + toHit.getValueAsString();
            } else if (toHit.getValue() > 12) {
                str += " Can't hit: " + toHit.getValueAsString();
            } else {
                str += " To hit: " + toHit.getValueAsString() + " ("
                       + Compute.oddsAbove(toHit.getValue(),
                                           ce().hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY)) + "%)";
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
        Vector<EntityAction> newAttacks = new Vector<>();
        for (Enumeration<AbstractEntityAction> e = attacks.elements(); e.hasMoreElements(); ) {
            AbstractEntityAction o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(getClient().getGame());
                Targetable target = waa.getTarget(getClient().getGame());
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                                                        attacker.getSecondaryFacing(), target,
                                                        attacker.getForwardArc());
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa.getEntityId(),
                            waa.getTargetType(), waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            } else {
                newAttacks.addElement(o);
            }
        }
        for (Enumeration<AbstractEntityAction> e = attacks.elements(); e.hasMoreElements(); ) {
            Object o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(getClient().getGame());
                Targetable target = waa.getTarget(getClient().getGame());
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                                                        attacker.getSecondaryFacing(), target,
                                                        attacker.getForwardArc());
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
        getClient().sendAttackData(cen, newAttacks);

        // clear queue
        attacks.removeAllElements();
    }

    /**
     * Returns the current Entity.
     */
    public Entity ce() {
        return getClient().getGame().getEntity(cen);
    }
}
