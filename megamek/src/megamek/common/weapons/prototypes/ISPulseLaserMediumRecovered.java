/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on Sep 8, 2005
 *
 */
package megamek.common.weapons.prototypes;

import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeLaserHandler;
import megamek.common.weapons.lasers.PulseLaserWeapon;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public class ISPulseLaserMediumRecovered extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -8402915088560062495L;

    /**
     *
     */
    public ISPulseLaserMediumRecovered() {
        super();
        name = "Medium Pulse Laser (Recovered Prototype)";
        setInternalName("ISMediumPulseLaserRecovered");
        addLookupName("IS Pulse Med Laser Recovered");
        shortName = "Medium Pulse Laser (P)";
        flags = flags.or(F_PROTOTYPE);
        heat = 4;
        damage = 6;
        toHitModifier = -2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 2;
        waterMediumRange = 3;
        waterLongRange = 4;
        waterExtremeRange = 6;
        tonnage = 2.0;
        criticals = 1;
        bv = 48;
        cost = 240000;
        rulesRefs = "103, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
            .setISAdvancement(3031, DATE_NONE, DATE_NONE, 3037, DATE_NONE)
            .setISApproximate(false, false, false, true, false)
            .setPrototypeFactions(F_FS,F_DC)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new PrototypeLaserHandler(toHit, waa, game, manager);
    }

    @Override
    public int getAlphaStrikeHeat() {
        return 7;
    }
}
