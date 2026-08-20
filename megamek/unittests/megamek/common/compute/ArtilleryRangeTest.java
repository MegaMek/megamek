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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Oblique Artilleryman range extension (CamOps p.78, 5th printing): the ability adds ten percent, rounded
 * up, to the range of the artillery weapon it is used with. An Inner Sphere Arrow IV is rated at 8 map sheets, which is
 * 136 hexes, so an Oblique Artilleryman reaches 150 hexes with it.
 *
 * @see ArtilleryRange#maximumIndirectRangeInHexes(Game, megamek.common.units.Entity,
 *       megamek.common.equipment.WeaponType, WeaponMounted)
 */
class ArtilleryRangeTest {

    private static final String ARROW_IV = "ISArrowIV";
    private static final String THUMPER_CANNON = "ISThumperCannon";

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    @Test
    @DisplayName("an ordinary crew reaches the artillery weapon's rated range")
    void ratedRangeWithoutTheAbility() throws Exception {
        Tank artillery = createArtilleryTank(false);
        WeaponMounted arrowFour = arrowFourMount(artillery);

        assertEquals(136,
              ArtilleryRange.maximumIndirectRangeInHexes(game, artillery, arrowFour.getType(),
                    arrowFour),
              "an Arrow IV is rated at 8 map sheets, which is 136 hexes");
    }

    @Test
    @DisplayName("Oblique Artilleryman adds ten percent of the artillery weapon's range, rounded up")
    void extendedRangeWithTheAbility() throws Exception {
        Tank artillery = createArtilleryTank(true);
        WeaponMounted arrowFour = arrowFourMount(artillery);

        assertEquals(150,
              ArtilleryRange.maximumIndirectRangeInHexes(game, artillery, arrowFour.getType(),
                    arrowFour),
              "136 hexes plus ten percent is 149.6 hexes, rounded up to 150");
    }

    @Test
    @DisplayName("the ten percent is taken after gravity has shortened the artillery weapon's range")
    void extendedRangeInHighGravity() throws Exception {
        game.getPlanetaryConditions().setGravity(1.2f);
        Tank ordinaryCrew = createArtilleryTank(false);
        Tank obliqueArtilleryman = createArtilleryTank(true);
        WeaponMounted ordinaryArrowFour = arrowFourMount(ordinaryCrew);
        WeaponMounted obliqueArrowFour = arrowFourMount(obliqueArtilleryman);

        assertEquals(102,
              ArtilleryRange.maximumIndirectRangeInHexes(game, ordinaryCrew, ordinaryArrowFour.getType(),
                    ordinaryArrowFour),
              "1.2 gravity cuts the Arrow IV to 6 map sheets, which is 102 hexes");
        assertEquals(113,
              ArtilleryRange.maximumIndirectRangeInHexes(game, obliqueArtilleryman,
                    obliqueArrowFour.getType(), obliqueArrowFour),
              "102 hexes plus ten percent is 112.2 hexes, rounded up to 113");
    }

    @Test
    @DisplayName("an artillery cannon is not an artillery piece, so the ability does not reach it")
    void artilleryCannonIsNotExtended() throws Exception {
        Tank artillery = createArtilleryTank(true);
        WeaponMounted thumperCannon = mountWeapon(artillery, THUMPER_CANNON);

        assertFalse(ArtilleryRange.isExtendedByObliqueArtilleryman(artillery, thumperCannon.getType()),
              "artillery cannons are a separate weapon class and are not on the artillery range table");
    }

    @Test
    @DisplayName("the range shown against a weapon's rating keeps the fraction of a map sheet")
    void extendedRangeForDisplayKeepsTheFraction() {
        assertEquals(8.8, ArtilleryRange.extendedRangeInMapSheets(8), 0.0001,
              "an Arrow IV rated at 8 map sheets is shown as reaching 8.8");
        assertEquals(19.8, ArtilleryRange.extendedRangeInMapSheets(18), 0.0001,
              "a Sniper rated at 18 map sheets is shown as reaching 19.8");
    }

    private Tank createArtilleryTank(boolean hasObliqueArtilleryman) {
        Tank artillery = new Tank();
        artillery.setGame(game);
        artillery.setId(game.getNextEntityId());
        artillery.setChassis("Test Battery");
        artillery.setOwner(game.getPlayer(0));
        artillery.setCrew(new Crew(CrewType.CREW));
        artillery.getCrew()
              .getOptions()
              .getOption(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY)
              .setValue(hasObliqueArtilleryman);
        game.addEntity(artillery);
        return artillery;
    }

    private WeaponMounted arrowFourMount(Tank artillery) throws Exception {
        return mountWeapon(artillery, ARROW_IV);
    }

    private WeaponMounted mountWeapon(Tank artillery, String internalName) throws Exception {
        WeaponMounted weapon = (WeaponMounted) Mounted.createMounted(artillery, EquipmentType.get(internalName));
        artillery.addEquipment(weapon, Tank.LOC_FRONT, false);
        return weapon;
    }
}
