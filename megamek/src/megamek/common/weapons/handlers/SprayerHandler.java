/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.HitData;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves a Sprayer attack (TM pp.248-249). A Sprayer inflicts direct damage to conventional
 * infantry only, equivalent to a battle-armor flamer (3D6 burst) at a range of 1 hex (TW p.217);
 * against any other target the spray itself does no damage. The effects of the loaded fluid
 * ammunition are applied separately.
 *
 * @author The MegaMek Team
 */
public class SprayerHandler extends AmmoWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(SprayerHandler.class);

    @Serial
    private static final long serialVersionUID = -3097044328957258071L;

    public SprayerHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    @Override
    protected void useAmmo() {
        // A Sprayer draws Fluid Gun ammunition at twice the normal rate, so a full ton yields half the
        // shots it would for a Fluid Gun (TO:AUE p.172). After the standard one-shot deduction, consume
        // one additional shot from the same bin (never going below empty).
        super.useAmmo();
        if ((ammo != null) && (ammo.getBaseShotsLeft() > 0)) {
            ammo.setShotsLeft(ammo.getBaseShotsLeft() - 1);
            LOGGER.debug("[Fluid:Sprayer] consumed 2 shots (Sprayer half-shot rate); {} shots remaining",
                  ammo.getBaseShotsLeft());
        }
    }

    @Override
    protected int calcDamagePerHit() {
        if (!target.isConventionalInfantry()) {
            // Sprayers cannot harm anything but conventional infantry; the fluid effect is resolved elsewhere.
            return 0;
        }

        // Equivalent to a battle-armor flamer: a 3D6 burst-fire attack (TW p.217).
        double damage = Compute.d6(3);
        if (bDirect) {
            damage += floor(toHit.getMoS() / 3.0);
        }
        // Pain-shunted infantry take half damage.
        if (((Entity) target).hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            damage /= 2;
        }
        damage = applyGlancingBlowModifier(damage, true);
        LOGGER.debug("[Fluid:Sprayer] {}: infantry burst-fire attack applied {} damage",
              target.getDisplayName(), (int) damage);
        return (int) damage;
    }
}
