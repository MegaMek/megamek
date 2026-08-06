/*
 * Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.primitive;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.srm.SRMHandler;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISSRM2Primitive extends SRMWeapon {
    @Serial
    private static final long serialVersionUID = 8488734998051278663L;

    public ISSRM2Primitive() {
        super();

        name = "Primitive Prototype SRM 2";
        setInternalName(name);
        addLookupName("IS SRM-2 Primitive");
        addLookupName("ISSRM2p");
        addLookupName("IS SRM 2 Primitive");
        shortName = "SRM/2p";
        sortingName = "SRM Proto 2";
        flags = flags.or(F_PROTOTYPE).andNot(F_ARTEMIS_COMPATIBLE);
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.0;
        criticalSlots = 1;
        bv = 21;
        flags = flags.or(F_NO_FIRES);
        cost = 10000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        ammoType = AmmoType.AmmoTypeEnum.SRM_PRIMITIVE;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This to cover some of the back worlds in the Periphery.
        rulesRefs = "112, IO:AE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new SRMHandler(toHit, waa, game, manager, -2);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}
