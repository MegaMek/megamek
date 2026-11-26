/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.plasma;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.AmmoBayWeaponHandler;
import megamek.common.weapons.ppc.clan.CLPlasmaCannon;
import megamek.server.totalWarfare.TWGameManager;

/** Plasma bays apply their firing weapons' heat at the location struck (TW p. 238). */
public class PlasmaBayWeaponHandler extends AmmoBayWeaponHandler {
    @Serial
    private static final long serialVersionUID = -4718048077136686433L;

    /** Rebuilt when the attack spends its ammunition. A cannon can fire and contribute zero physical damage. */
    private transient Map<WeaponMounted, Double> firedWeapons;
    private transient @Nullable WeaponMounted groundWeapon;

    public PlasmaBayWeaponHandler(ToHitData toHit, WeaponAttackAction attack, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, attack, game, manager);
    }

    @Override
    protected int calcAttackValue() {
        firedWeapons = new LinkedHashMap<>();
        groundWeapon = null;
        return super.calcAttackValue();
    }

    @Override
    protected double firedWeaponAttackValue(WeaponMounted member, double ammoAV) {
        firedWeapons.put(member, ammoAV);
        return isHeatOnlyHit(member, target) ? 0 : ammoAV;
    }

    @Override
    protected boolean prepareGroundWeaponHit(WeaponMounted member, int range) {
        Double damage = firedWeapons.get(member);
        if (damage == null) {
            return false;
        }
        groundWeapon = member;
        nDamPerHit = (int) Math.ceil(damage);
        return true;
    }

    @Override
    protected int damagePerHitAgainst(Entity entityTarget) {
        return groundWeapon != null && isHeatOnlyHit(groundWeapon, entityTarget) ? 0 : nDamPerHit;
    }

    private boolean isHeatOnlyHit(WeaponMounted member, Targetable struck) {
        return member.getType() instanceof CLPlasmaCannon && struck.tracksHeat();
    }

    @Override
    protected @Nullable Report applyHeatOnStrike(Entity entityTarget, int damageThatStrikes) {
        if (!entityTarget.tracksHeat() || firedWeapons.isEmpty()) {
            return null;
        }
        // Ground hits each have a location. In space or against an airborne target the bay strikes one location.
        int heat = 0;
        if (groundWeapon != null) {
            heat = PlasmaRifleHandler.rollPlasmaHeat(groundWeapon.getType());
        } else {
            for (WeaponMounted member : firedWeapons.keySet()) {
                heat += PlasmaRifleHandler.rollPlasmaHeat(member.getType());
            }
        }
        return applyExternalHeat(entityTarget, heat);
    }
}
