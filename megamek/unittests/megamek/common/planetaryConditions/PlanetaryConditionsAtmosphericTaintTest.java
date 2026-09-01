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

package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import megamek.common.game.Game;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests which units a tainted or toxic atmosphere refuses to let onto the field (TO:AR p.54), and that the setting
 * survives being sent to a client.
 */
class PlanetaryConditionsAtmosphericTaintTest {

    private final Game game = mock(Game.class);

    private ConvInfantry platoon(boolean isXCTForTainted, boolean isXCTForToxic) {
        ConvInfantry infantry = mock(ConvInfantry.class);
        lenient().when(infantry.isConventionalInfantry()).thenReturn(true);
        lenient().when(infantry.isXCTForTaintedAtmosphere()).thenReturn(isXCTForTainted);
        lenient().when(infantry.isXCTForToxicAtmosphere()).thenReturn(isXCTForToxic);
        lenient().when(infantry.getMovementMode()).thenReturn(EntityMovementMode.INF_LEG);
        return infantry;
    }

    private Tank vehicle(boolean hasEnvironmentalSealing) {
        Tank tank = mock(Tank.class);
        lenient().when(tank.isConventionalInfantry()).thenReturn(false);
        lenient().when(tank.hasEnvironmentalSealing()).thenReturn(hasEnvironmentalSealing);
        lenient().when(tank.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        return tank;
    }

    private Mek battleMek() {
        Mek mek = mock(Mek.class);
        lenient().when(mek.isConventionalInfantry()).thenReturn(false);
        lenient().when(mek.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        return mek;
    }

    private Mek industrialMek(boolean hasEnvironmentalSealing) {
        Mek mek = mock(Mek.class);
        lenient().when(mek.isConventionalInfantry()).thenReturn(false);
        lenient().when(mek.isIndustrial()).thenReturn(true);
        lenient().when(mek.hasEnvironmentalSealing()).thenReturn(hasEnvironmentalSealing);
        lenient().when(mek.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        return mek;
    }

    private PlanetaryConditions conditionsWith(AtmosphericTaint atmosphericTaint) {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setAtmosphericTaint(atmosphericTaint);
        return conditions;
    }

    @Test
    @DisplayName("Breathable air lets everything onto the field")
    void breathableAirDoomsNobody() {
        PlanetaryConditions conditions = conditionsWith(AtmosphericTaint.BREATHABLE);

        assertNull(conditions.whyDoomed(platoon(false, false), game));
        assertNull(conditions.whyDoomed(vehicle(false), game));
        assertNull(conditions.whyDoomed(battleMek(), game));
    }

    @Test
    @DisplayName("Tainted air turns away infantry without the XCT gear for it")
    void taintedAirDoomsInfantryWithoutTaintedGear() {
        PlanetaryConditions conditions = conditionsWith(AtmosphericTaint.TAINTED_CAUSTIC);

        assertNotNull(conditions.whyDoomed(platoon(false, false), game),
              "A platoon with no tainted-atmosphere gear cannot be fielded");
        assertNull(conditions.whyDoomed(platoon(true, false), game),
              "A platoon with tainted-atmosphere XCT gear can be fielded");
    }

    @Test
    @DisplayName("Toxic air needs the stricter XCT gear; tainted-only gear is not enough")
    void toxicAirNeedsToxicGear() {
        PlanetaryConditions conditions = conditionsWith(AtmosphericTaint.TOXIC_POISON);

        assertNotNull(conditions.whyDoomed(platoon(true, false), game),
              "A Light Environment Suit is not enough for toxic air");
        assertNull(conditions.whyDoomed(platoon(true, true), game),
              "A platoon with toxic-atmosphere XCT gear can be fielded");
    }

    @Test
    @DisplayName("Tainted air leaves vehicles alone; toxic air turns away the unsealed ones")
    void toxicAirDoomsUnsealedVehicles() {
        PlanetaryConditions taintedConditions = conditionsWith(AtmosphericTaint.TAINTED_CAUSTIC);
        assertNull(taintedConditions.whyDoomed(vehicle(false), game),
              "A tainted atmosphere does not bar an unsealed vehicle");

        PlanetaryConditions toxicConditions = conditionsWith(AtmosphericTaint.TOXIC_CAUSTIC);
        assertNotNull(toxicConditions.whyDoomed(vehicle(false), game),
              "A toxic atmosphere bars a vehicle without Environmental Sealing");
        assertNull(toxicConditions.whyDoomed(vehicle(true), game),
              "A sealed vehicle can still be fielded in a toxic atmosphere");
    }

    @Test
    @DisplayName("Hovercraft, WiGEs and VTOLs are judged on their sealing, not on how they move")
    void liftingVehiclesAreNotBarredByTheirMovementMode() {
        // The footnote that bars hover, WiGE and VTOL vehicles belongs to vacuum and trace atmospheres. Official
        // ruling: "WIGEs, VTOLs, and hovercraft would work fine in uninhabitable atmospheres of sufficient
        // thickness." A tainted or toxic atmosphere has air, so only the Environmental Sealing requirement applies.
        PlanetaryConditions conditions = conditionsWith(AtmosphericTaint.TOXIC_CAUSTIC);

        for (EntityMovementMode movementMode : List.of(EntityMovementMode.HOVER,
              EntityMovementMode.WIGE,
              EntityMovementMode.VTOL)) {
            Tank sealedLifter = vehicle(true);
            lenient().when(sealedLifter.getMovementMode()).thenReturn(movementMode);
            assertNull(conditions.whyDoomed(sealedLifter, game),
                  "a sealed " + movementMode + " vehicle should be allowed in toxic air");

            Tank unsealedLifter = vehicle(false);
            lenient().when(unsealedLifter.getMovementMode()).thenReturn(movementMode);
            assertNotNull(conditions.whyDoomed(unsealedLifter, game),
                  "an unsealed " + movementMode + " vehicle should still be barred, for want of sealing");
        }
    }

    @Test
    @DisplayName("A BattleMek is sealed by construction and is never turned away by the air")
    void meksAreNeverDoomedByTaint() {
        for (AtmosphericTaint atmosphericTaint : AtmosphericTaint.values()) {
            PlanetaryConditions conditions = conditionsWith(atmosphericTaint);
            assertNull(conditions.whyDoomed(battleMek(), game),
                  "A BattleMek should survive " + atmosphericTaint);
        }
    }

    @Test
    @DisplayName("A flammable atmosphere bars nobody; it burns the ground, not the crews")
    void flammableAirDoomsNobody() {
        PlanetaryConditions taintedConditions = conditionsWith(AtmosphericTaint.TAINTED_FLAME);
        PlanetaryConditions toxicConditions = conditionsWith(AtmosphericTaint.TOXIC_FLAME);

        assertNull(taintedConditions.whyDoomed(vehicle(false), game));
        assertNull(toxicConditions.whyDoomed(vehicle(false), game));
    }

    @Test
    @DisplayName("A flammable atmosphere makes fires easier to start, on top of the weather")
    void flammableAirLowersTheIgnitionTarget() {
        PlanetaryConditions breathable = conditionsWith(AtmosphericTaint.BREATHABLE);
        PlanetaryConditions flammableTainted = conditionsWith(AtmosphericTaint.TAINTED_FLAME);
        PlanetaryConditions flammableToxic = conditionsWith(AtmosphericTaint.TOXIC_FLAME);

        int baseModifier = breathable.getIgniteModifiers();
        assertEquals(baseModifier - 2, flammableTainted.getIgniteModifiers());
        assertEquals(baseModifier - 4, flammableToxic.getIgniteModifiers());
    }

    @Test
    @DisplayName("The taint setting survives being copied to a client")
    void taintSurvivesCopying() {
        PlanetaryConditions original = conditionsWith(AtmosphericTaint.TAINTED_POISON);

        PlanetaryConditions copy = new PlanetaryConditions(original);
        assertEquals(AtmosphericTaint.TAINTED_POISON, copy.getAtmosphericTaint());

        PlanetaryConditions altered = new PlanetaryConditions();
        altered.alterConditions(original);
        assertEquals(AtmosphericTaint.TAINTED_POISON, altered.getAtmosphericTaint());
    }

    @Test
    @DisplayName("The taint setting survives the Java serialization used to send conditions to clients")
    void taintSurvivesSerialization() throws IOException, ClassNotFoundException {
        PlanetaryConditions original = conditionsWith(AtmosphericTaint.TOXIC_FLAME);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(bytes)) {
            objectOutput.writeObject(original);
        }
        PlanetaryConditions restored;
        try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (PlanetaryConditions) objectInput.readObject();
        }

        assertEquals(AtmosphericTaint.TOXIC_FLAME, restored.getAtmosphericTaint());
    }

    @Test
    @DisplayName("Conditions written before this setting existed come back as breathable air")
    void defaultIsBreathable() {
        assertEquals(AtmosphericTaint.BREATHABLE, new PlanetaryConditions().getAtmosphericTaint());
    }

    @Test
    @DisplayName("Toxic air turns away unsealed vehicles but not Meks of any kind")
    void toxicAirBarsVehiclesOnly() {
        PlanetaryConditions conditions = conditionsWith(AtmosphericTaint.TOXIC_CAUSTIC);

        assertNotNull(conditions.whyDoomed(vehicle(false), game),
              "an unsealed vehicle may not be fielded in toxic air");
        assertNull(conditions.whyDoomed(vehicle(true), game),
              "the sealing chassis modification is what lets a vehicle be here");
        assertNull(conditions.whyDoomed(battleMek(), game),
              "a BattleMek is sealed as part of its basic construction");
        assertNull(conditions.whyDoomed(industrialMek(false), game),
              "TO:AR p.54 bars vehicles, not Meks - an IndustrialMek is fielded and dies to a cockpit breach");
    }
}
