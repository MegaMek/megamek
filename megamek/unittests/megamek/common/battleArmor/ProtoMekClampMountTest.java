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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the issue #8638 bug class in {@link ProtoMekClampMount}: its
 * {@code getCargoMpReduction()} resolves the carried ProtoMek through the transient {@code game} field, and its
 * {@code isWeaponBlockedAt()} returns {@code true} without reading {@code game} at all - so
 * {@code getExteriorUnitAt()} in the parent class must guard {@code game} itself rather than rely on the callee.
 * Both paths crashed with a {@link NullPointerException} for a unit not yet added to a game.
 *
 * @author Claude Code (Fable 5)
 */
class ProtoMekClampMountTest {

    private static final int CARRIED_PROTOMEK_ID = 7;
    private static final double CARRIER_WEIGHT = 50.0;

    private ProtoMek mockClampedProtoMek() {
        ProtoMek protoMek = mock(ProtoMek.class);
        when(protoMek.isProtoMek()).thenReturn(true);
        when(protoMek.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP)).thenReturn(true);
        when(protoMek.getId()).thenReturn(CARRIED_PROTOMEK_ID);
        return protoMek;
    }

    @Test
    @DisplayName("with no game reference, a loaded clamp mount neither reduces MP nor exposes an exterior unit")
    void noGameReferenceIsSafeEvenWithCarriedProtoMek() {
        ProtoMekClampMount clampMount = new ProtoMekClampMount(false);
        clampMount.load(mockClampedProtoMek());

        Mek carrier = mock(Mek.class);
        when(carrier.getWeight()).thenReturn(CARRIER_WEIGHT);

        int mpReduction = assertDoesNotThrow(() -> clampMount.getCargoMpReduction(carrier),
              "MP reduction of a unit not yet added to a game must not throw");
        assertEquals(0, mpReduction, "An unresolvable carried unit cannot penalize the carrier's MP");

        // isWeaponBlockedAt() reports true here without reading the game field, so this exercises
        // getExteriorUnitAt()'s own null-guard rather than the callee's.
        assertTrue(clampMount.isWeaponBlockedAt(Mek.LOC_CENTER_TORSO, false));
        assertNull(assertDoesNotThrow(() -> clampMount.getExteriorUnitAt(Mek.LOC_CENTER_TORSO, false)));
    }

    @Test
    @DisplayName("with a game reference, a loaded clamp mount resolves the ProtoMek's weight and exposes it")
    void gameReferenceResolvesCarriedProtoMek() {
        ProtoMek protoMek = mockClampedProtoMek();
        when(protoMek.getWeight()).thenReturn(9.0);
        Game game = mock(Game.class);
        when(game.getEntity(CARRIED_PROTOMEK_ID)).thenReturn(protoMek);

        ProtoMekClampMount clampMount = new ProtoMekClampMount(false);
        clampMount.load(protoMek);
        clampMount.setGame(game);

        Mek carrier = mock(Mek.class);
        when(carrier.getWeight()).thenReturn(CARRIER_WEIGHT);
        when(carrier.getOriginalWalkMP()).thenReturn(8);

        // 9 tons is between 10% and 25% of a 50-ton carrier: reduction = min(3, walk / 2) = 3
        assertEquals(3, clampMount.getCargoMpReduction(carrier));
        assertEquals(protoMek, clampMount.getExteriorUnitAt(Mek.LOC_CENTER_TORSO, false));
    }
}
