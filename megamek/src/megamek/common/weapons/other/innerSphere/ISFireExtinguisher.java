/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.other.innerSphere;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.actions.compute.FirefightingSupport;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.FireExtinguisherHandler;
import megamek.common.weapons.handlers.FirefightingSupportHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISFireExtinguisher extends Weapon {
    @Serial
    private static final long serialVersionUID = -5387366609676650747L;

    public ISFireExtinguisher() {
        super();
        name = "Fire Extinguisher";
        setInternalName(name);
        addLookupName("IS Fire Extinguisher");
        // The IS and Clan fire extinguishers are mechanically identical, so they are merged into this single
        // TechBase.ALL weapon. Keep the Clan lookup name so existing units/saves still resolve to it.
        addLookupName("Clan Fire Extinguisher");
        heat = 0;
        damage = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        tonnage = 0.0;
        criticalSlots = 0;
        flags = flags.or(F_NO_FIRES).or(F_SOLO_ATTACK).or(F_EXTINGUISHER);
        // Firefighting engineers fighting one blaze together may either roll separately or pool into a single
        // roll at -1 per extra platoon (TO:AuE p.153). The mode picks which: FIREFIGHT rolls on its own, SUPPORT
        // yields its roll and lends -1 to the lead platoon. FIREFIGHT is first so it is the default.
        setModes(new String[] { FirefightingSupport.MODE_FIREFIGHT, FirefightingSupport.MODE_SUPPORT });
        setInstantModeSwitch(true);
        techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setISAdvancement(DATE_NONE, DATE_NONE, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_NONE, 2820, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            // A firefighting platoon in Support mode yields its roll and only lends -1 to the lead platoon
            // fighting the same hex (TO:AuE p.153), so it resolves through the lightweight support handler.
            Entity attacker = game.getEntity(waa.getEntityId());
            Targetable target = waa.getTarget(game);
            if ((attacker != null) && FirefightingSupport.isYieldingSupporter(game, attacker, target)) {
                return new FirefightingSupportHandler(toHit, waa, game, manager);
            }
            return new FireExtinguisherHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}
