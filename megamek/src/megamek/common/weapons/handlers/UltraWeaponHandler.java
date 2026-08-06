/*
 * Copyright (c) 2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2007-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sept 29, 2004
 */
public class UltraWeaponHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 7551194199079004134L;

    protected int howManyShots;
    private final boolean twoRollsUltra; // Tracks whether this is an
    // ultra AC using the unofficial "two rolls" rule. Can be final because
    // this isn't really going to change over the course of a game.

    public UltraWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        twoRollsUltra = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS)
              && ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA)
              || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA_THB));
    }

    @Override
    protected void useAmmo() {
        setDone();
        checkAmmo();
        howManyShots = (weapon.curMode().equals(Weapon.MODE_AC_SINGLE) ? 1 : 2);
        int total = weaponEntity.getTotalAmmoOfType(ammo.getType());
        if (total == 1) {
            howManyShots = 1;
        } else if (total < 1) {
            howManyShots = 0;
        }

        // Handle bins that are empty or will be emptied by this attack
        attemptToReloadWeapon();
        reduceShotsLeft(howManyShots);
    }

    protected void attemptToReloadWeapon() {
        // We _may_ be able to reload from another ammo source, but in case
        // a previous attack burned through all the ammo, this attack may be SOL.
        if (ammo.getUsableShotsLeft() == 0) {
            weaponEntity.loadWeapon(weapon);
            ammo = (AmmoMounted) weapon.getLinked();
        }
    }

    protected void reduceShotsLeft(int shotsNeedFiring) {
        while (shotsNeedFiring > ammo.getUsableShotsLeft()) {
            shotsNeedFiring -= ammo.getBaseShotsLeft();
            ammo.setShotsLeft(0);
            attemptToReloadWeapon();
        }
        ammo.setShotsLeft(ammo.getBaseShotsLeft() - shotsNeedFiring);
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump BAs can't mount UACS/RACs
        if (target.isConventionalInfantry()) {
            return 1;
        }

        if (howManyShots == 1 || twoRollsUltra) {
            return 1;
        }

        bSalvo = true;

        int nMod = getClusterModifiers(true);

        int shotsHit = allShotsHit() ? howManyShots : Compute.missilesHit(howManyShots, nMod);

        // report number of shots that hit only when weapon doesn't jam
        if (!weapon.isJammed()) {
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(shotsHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMod != 0) {
                if (nMod > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMod);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            r = new Report(3345);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        return shotsHit;
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (super.doChecks(vPhaseReport)) {
            return true;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            if ((roll.getIntValue() == 2) && (howManyShots == 2) && !weaponEntity.isConventionalInfantry()) {
                // Ultra ACs jam on a 2. Edge may reroll the jam check; if Edge wasn't used the jam stands, otherwise
                // the reroll decides (an Ultra AC still jams only on another 2).
                int edgeReroll = rerollJamCheckWithEdge(attackingEntity, subjectId, vPhaseReport);
                if (acStillJams(edgeReroll, 2)) {
                    Report r = new Report();
                    r.subject = subjectId;
                    weapon.setJammed(true);
                    isJammed = true;
                    if ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA)
                          || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA_THB)) {
                        r.messageId = 3160;
                    } else {
                        r.messageId = 3170;
                    }
                    vPhaseReport.addElement(r);
                }
            }
        } else {
            // PLAYTEST3 Caseless ammo support for RAC
            // Will potentially explode when rolling a 2. Can still jam if not blowing up.
            // The check above will only get to this if playtest3 is enabled
            if ((roll.getIntValue() <= 2) && !attackingEntity.isConventionalInfantry()
                  && ammoType.getMunitionType().contains(AmmoType.Munitions.M_CASELESS)) {
                Roll diceRoll = Compute.rollD6(2);

                Report r = new Report(3164);
                r.subject = subjectId;
                r.add(diceRoll);

                if (diceRoll.getIntValue() >= 8) {
                    // Round explodes destroying weapon
                    weapon.setDestroyed(true);
                    r.choose(false);
                } else {
                    // Just a jam
                    weapon.setJammed(true);
                    r.choose(true);
                }
                vPhaseReport.addElement(r);
            }
        }
        return false;
    }

    /**
     * Applies Edge to an autocannon jam check (Ultra, Rotary, or rapid-fire modes): if the attacker has the UAC-jam
     * Edge trigger enabled with Edge remaining, spends one Edge point and rerolls the jam check once. The caller
     * re-evaluates its own jam threshold against the returned value.
     *
     * @param attacker     the firing unit
     * @param subjectId    the report subject id
     * @param reportVector the report vector to append the Edge reports to
     *
     * @return the rerolled 2d6 value if Edge was spent, or -1 if Edge was not used
     */
    // package-private static for testing
    static int rerollJamCheckWithEdge(Entity attacker, int subjectId, Vector<Report> reportVector) {
        if (!attacker.shouldUseEdge(OptionsConstants.EDGE_WHEN_AC_JAMS_OR_MALFUNCTIONS)) {
            return -1;
        }
        attacker.getCrew().decreaseEdge();
        Report report = new Report(3166);
        report.subject = subjectId;
        report.add(attacker.getCrew().getOptions().intOption(OptionsConstants.EDGE));
        reportVector.addElement(report);

        Roll jamReroll = Compute.rollD6(2);
        report = new Report(3167);
        report.subject = subjectId;
        report.add(jamReroll);
        reportVector.addElement(report);
        return jamReroll.getIntValue();
    }

    /**
     * Decides whether an autocannon still jams after an Edge reroll. The weapon jams unless Edge was used
     * ({@code edgeReroll >= 0}) and the rerolled value is above the jam threshold for the current fire mode.
     *
     * @param edgeReroll   the rerolled value from {@link #rerollJamCheckWithEdge}, or -1 if Edge was not used
     * @param jamThreshold the value at or below which the weapon jams
     *
     * @return true if the weapon jams
     */
    // package-private static for testing
    static boolean acStillJams(int edgeReroll, int jamThreshold) {
        // A negative reroll means Edge was not used, so the original jam stands.
        return (edgeReroll < 0) || (edgeReroll <= jamThreshold);
    }

    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage();
        // infantry get hit by all shots
        if (target.isConventionalInfantry()) {
            if (howManyShots > 1) { // Is this a cluster attack?
                // Compute maximum damage potential for cluster weapons
                toReturn = howManyShots * weaponType.getDamage();
                toReturn = Compute.directBlowInfantryDamage(toReturn,
                      bDirect ? toHit.getMoS() / 3 : 0,
                      WeaponType.WEAPON_CLUSTER_BALLISTIC, // treat as cluster
                      ((Infantry) target).isMechanized(),
                      toHit.getThruBldg() != null, weaponEntity.getId(),
                      calcDmgPerHitReport);
            } else { // No - only one shot fired
                toReturn = Compute.directBlowInfantryDamage(weaponType.getDamage(),
                      bDirect ? toHit.getMoS() / 3 : 0,
                      weaponType.getInfantryDamageClass(),
                      ((Infantry) target).isMechanized(),
                      toHit.getThruBldg() != null, weaponEntity.getId(),
                      calcDmgPerHitReport);
            }
            // Cluster bonuses or penalties can't apply to "two rolls" UACs, so
            // if we have one, modify the damage per hit directly.
        } else if (bDirect && (howManyShots == 1 || twoRollsUltra)) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }

        if (howManyShots == 1 || twoRollsUltra) {
            toReturn = applyGlancingBlowModifier(toReturn, false);
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }
        return (int) toReturn;
    }

    @Override
    protected boolean usesClusterTable() {
        return !game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_UAC_TWO_ROLLS);
    }

    @Override
    protected int calculateNumClusterAero(Entity entityTarget) {
        if (usesClusterTable() && !weaponEntity.isCapitalFighter() && (entityTarget != null)
              && !entityTarget.isCapitalScale()) {
            return (int) Math.ceil(attackValue / 2.0);
        } else {
            return 1;
        }
    }
}
