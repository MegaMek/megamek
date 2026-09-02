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

package megamek.common.battleArmor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.TechConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests which battle armor squads are sealed by HarJel without mounting a HarJel system.
 * <p>
 * Clan power armor and battle armor of 401 kilograms or more incorporate HarJel automatically, as do Clan
 * exoskeletons built on a Clan chassis (TechManual 8th printing, p.256). This matters wherever a rule asks whether a damaged suit reseals itself - a
 * playtest found Elemental suits, which are Clan medium battle armor, being treated as unsealed because the only
 * check available looked for a mounted HarJel system.
 */
class BattleArmorHarJelTest {

    private BattleArmor squad(int techLevel, boolean isExoskeleton, boolean declinedHarJel) {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setTechLevel(techLevel);
        battleArmor.setIsExoskeleton(isExoskeleton);
        battleArmor.setClanExoWithoutHarJel(declinedHarJel);
        return battleArmor;
    }

    @Test
    @DisplayName("Clan battle armor is sealed by construction, as an Elemental is")
    void clanBattleArmorIsSealedByConstruction() {
        BattleArmor elemental = squad(TechConstants.T_CLAN_TW, false, false);

        assertTrue(elemental.hasHarJelByConstruction(),
              "Clan battle armor of 401kg or more incorporates HarJel automatically");
        assertTrue(elemental.hasHarJelProtection(BattleArmor.LOC_TROOPER_1),
              "so its troopers count as HarJel-sealed without mounting the system");
    }

    @Test
    @DisplayName("Inner Sphere battle armor is not sealed unless it mounts HarJel")
    void innerSphereBattleArmorIsNotSealedByConstruction() {
        BattleArmor innerSphereSquad = squad(TechConstants.T_IS_TW_NON_BOX, false, false);

        assertFalse(innerSphereSquad.hasHarJelByConstruction(),
              "the automatic HarJel rule is a Clan construction rule only");
        assertFalse(innerSphereSquad.hasHarJelProtection(BattleArmor.LOC_TROOPER_1),
              "and with no HarJel system mounted the suit is not sealed");
    }

    @Test
    @DisplayName("A Clan exoskeleton on a Clan chassis is sealed; one that took the lighter chassis is not")
    void clanExoskeletonDependsOnItsChassis() {
        BattleArmor clanChassisExoskeleton = squad(TechConstants.T_CLAN_TW, true, false);
        assertTrue(clanChassisExoskeleton.hasHarJelByConstruction(),
              "a Clan exoskeleton on a Clan chassis incorporates HarJel");

        BattleArmor innerSphereChassisExoskeleton = squad(TechConstants.T_CLAN_TW, true, true);
        assertFalse(innerSphereChassisExoskeleton.hasHarJelByConstruction(),
              "an exoskeleton that declined HarJel to take the lighter chassis is not sealed");
    }

    @Test
    @DisplayName("Clan battle armor at every rules level counts, since all of them are Clan tech")
    void everyClanRulesLevelCounts() {
        for (int techLevel : new int[] { TechConstants.T_CLAN_TW, TechConstants.T_CLAN_ADVANCED,
              TechConstants.T_CLAN_EXPERIMENTAL, TechConstants.T_CLAN_UNOFFICIAL }) {
            assertTrue(squad(techLevel, false, false).hasHarJelByConstruction(),
                  "tech level " + techLevel + " is Clan and so is sealed by construction");
        }
    }
}
