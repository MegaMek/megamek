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

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.flamers.clan.CLHeavyFlamer;
import megamek.common.weapons.flamers.innerSphere.ISHeavyFlamer;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves an attack by a Flamer or Fluid Gun firing Inferno Fuel (TO:AUE p.173). Each hit is treated as
 * a hit by a single Inferno SRM missile; a Heavy Flamer using Inferno Fuel delivers two (to the same
 * target). Sprayers cannot load Inferno Fuel.
 *
 * @author The MegaMek Team
 */
public class InfernoFuelHandler extends AmmoWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(InfernoFuelHandler.class);

    @Serial
    private static final long serialVersionUID = -2295861876703866443L;

    public InfernoFuelHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager)
          throws EntityLoadingException {
        super(toHit, waa, game, manager);
        damageType = DamageType.INFERNO;
    }

    private int infernoMissiles() {
        return infernoMissilesFor(weaponType);
    }

    /**
     * @param weaponType the weapon firing Inferno Fuel
     *
     * @return the number of Inferno SRM missiles a hit delivers: two for a Heavy Flamer, one for a
     *       standard Vehicle Flamer or Fluid Gun (TO:AUE p.173)
     */
    static int infernoMissilesFor(WeaponType weaponType) {
        return ((weaponType instanceof ISHeavyFlamer) || (weaponType instanceof CLHeavyFlamer)) ? 2 : 1;
    }

    @Override
    protected int calcDamagePerHit() {
        // Inferno Fuel does no direct damage; its effect is delivered as Inferno SRM missile hits.
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding building,
          int hits, int nCluster, int bldgAbsorbs) {
        LOGGER.debug("[Fluid:InfernoFuel] delivered {} inferno missile(s) to {}",
              infernoMissiles(), entityTarget.getShortName());
        vPhaseReport.addAll(gameManager.deliverInfernoMissiles(attackingEntity, target, infernoMissiles(),
              weapon.getCalledShot().getCall()));
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, IBuilding bldg, int hits) {
        // Targeting a hex with Inferno Fuel sets it ablaze exactly like Inferno SRMs landing there.
        LOGGER.debug("[Fluid:InfernoFuel] delivered {} inferno missile(s) to {}",
              infernoMissiles(), target.getDisplayName());
        vPhaseReport.addAll(gameManager.deliverInfernoMissiles(attackingEntity, target, infernoMissiles(),
              weapon.getCalledShot().getCall()));
    }
}
