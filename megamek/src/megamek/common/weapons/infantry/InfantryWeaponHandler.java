/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.InfantryMount;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 24, 2004
 */
public class InfantryWeaponHandler extends WeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(InfantryWeaponHandler.class);

    @Serial
    private static final long serialVersionUID = 1425176802065536326L;

    /**
     *
     */
    public InfantryWeaponHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
        bSalvo = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calculateNumCluster() {
        return 2;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int nHitMod = 0;

        if (bGlancing) {
            nHitMod -= 4;
        }

        if (this.bLowProfileGlancing) {
            nHitMod -= 4;
        }

        int troopersHit;
        // when swarming all troopers hit
        if (attackingEntity.getSwarmTargetId() == target.getId()) {
            troopersHit = ((Infantry) attackingEntity).getShootingStrength();
        } else if (!(attackingEntity instanceof Infantry)) {
            troopersHit = 1;
        } else {
            troopersHit = Compute.missilesHit(((Infantry) attackingEntity)
                  .getShootingStrength(), nHitMod);
        }
        double damage = calculateBaseDamage(attackingEntity, weapon, weaponType);

        if ((attackingEntity instanceof Infantry) && (nRange == 0)
              && attackingEntity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            damage += 0.14;
        }
        int damageDealt = (int) Math.round(damage * troopersHit);

        // beast-mounted infantry get range 0 bonus damage per platoon
        if ((attackingEntity instanceof Infantry) && (nRange == 0)) {
            InfantryMount mount = ((Infantry) attackingEntity).getMount();
            if (mount != null) {
                if (!target.isConventionalInfantry()) {
                    damageDealt += mount.vehicleDamage();
                } else if (mount.getBurstDamageDice() > 0) {
                    damageDealt += Compute.d6(mount.getBurstDamageDice());
                }
            }
        }

        // conventional infantry weapons with high damage get treated as if they have
        // the infantry burst mod
        if (target.isConventionalInfantry() &&
              (weaponType.hasFlag(WeaponType.F_INF_BURST) ||
                    (attackingEntity.isConventionalInfantry()
                          && ((Infantry) attackingEntity).primaryWeaponDamageCapped()))) {
            damageDealt += Compute.d6();
        }
        if ((target instanceof Infantry) && ((Infantry) target).isMechanized()) {
            damageDealt /= 2;
        }
        // this doesn't work...
        if ((target instanceof IBuilding) && (weaponType.hasFlag(WeaponType.F_INF_NONPENETRATING))) {
            damageDealt = 0;
        }
        if (weaponType.hasFlag(WeaponType.F_INF_NONPENETRATING)) {
            damageType = DamageType.NONPENETRATING;
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        if (attackingEntity instanceof Infantry) {
            r.add(troopersHit);
            r.add(" troopers ");
        } else { // Needed for support tanks with infantry weapons
            r.add("");
            r.add("");
        }
        r.add(toHit.getTableDesc() + ", causing " + damageDealt
              + " damage.");
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (target.isConventionalInfantry()) {
            // this is a little strange, but I can't just do this in calcDamagePerHit
            // because
            // that is called up before misses are determined and will lead to weird
            // reporting
            nDamPerHit = damageDealt;
            return 1;
        }
        return damageDealt;
    }

    // we need to figure out AV damage to aerospace for AA weapons
    @Override
    protected int calculateNumClusterAero(Entity entityTarget) {
        return 5;
    }

    @Override
    protected int calcAttackValue() {
        int av;
        // Sigh, another rules oversight - nobody bothered to figure this out
        // To be consistent with other cluster weapons we will assume 60% hit
        if (attackingEntity.isConventionalInfantry()) {
            double damage = ((Infantry) attackingEntity).getDamagePerTrooper();
            av = (int) Math.round(damage * 0.6 * ((Infantry) attackingEntity).getShootingStrength());
        } else {
            // Small fixed wing support
            av = super.calcAttackValue();
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        return av;
    }

    @Override
    public void useAmmo() {
        if (attackingEntity.isSupportVehicle()) {
            Mounted<?> ammo = weapon.getLinked();
            if (ammo == null) {
                attackingEntity.loadWeapon(weapon);
                ammo = weapon.getLinked();
            }
            if (ammo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                LOGGER.error("Handler can't find any ammo for {} firing {}",
                      attackingEntity.getShortName(),
                      weapon.getName());
                return;
            }

            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            // Swap between standard and inferno if the unit has some left of the other type
            if ((ammo.getUsableShotsLeft() <= 0)
                  && (ammo.getLinked() != null)
                  && (ammo.getLinked().getUsableShotsLeft() > 0)) {
                weapon.setLinked(ammo.getLinked());
                weapon.getLinked().setLinked(ammo);
            }
            super.useAmmo();
        }
    }

    /**
     * Utility function to calculate variable damage based only on the firing entity.
     */
    public static double calculateBaseDamage(Entity ae, Mounted<?> weapon, WeaponType weaponType) {
        if (ae.isConventionalInfantry()) {
            // for conventional infantry, we have to calculate primary and secondary weapons
            // to get damage per trooper
            return ((Infantry) ae).getDamagePerTrooper();
        } else if (ae.isSupportVehicle()) {
            // Damage for some weapons depends on what type of ammo is being used
            if ((weapon.getLinked() != null)
                  && ((AmmoType) weapon.getLinked().getType()).getMunitionType()
                  .contains(AmmoType.Munitions.M_INFERNO)) {
                return ((InfantryWeapon) weaponType).getInfernoVariant().getInfantryDamage();
            } else {
                return ((InfantryWeapon) weaponType).getNonInfernoVariant().getInfantryDamage();
            }
        } else {
            return ((InfantryWeapon) weaponType).getInfantryDamage();
        }
    }

    @Override
    protected void initHit(Entity entityTarget) {
        if ((entityTarget instanceof BattleArmor) && attackingEntity.isConventionalInfantry()) {
            // TacOps crits against BA do not happen for infantry weapon attacks
            hit = ((BattleArmor) entityTarget).rollHitLocation(toHit.getSideTable(),
                  weaponAttackAction.getAimedLocation(), weaponAttackAction.getAimingMode(), true);
            hit.setGeneralDamageType(generalDamageType);
            hit.setCapital(weaponType.isCapital());
            hit.setBoxCars(roll.getIntValue() == 12);
            hit.setCapMisCritMod(getCapMisMod());
            hit.setFirstHit(firstHit);
            hit.setAttackerId(getAttackerId());
            if (weapon.isWeaponGroup()) {
                hit.setSingleAV(attackValue);
            }
        } else {
            super.initHit(entityTarget);
        }
    }
}
