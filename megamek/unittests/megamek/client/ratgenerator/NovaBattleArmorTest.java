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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Covers the battle armor half of a Clan Nova, which came out empty in some Clusters.
 *
 * <p>Two faults combined. When a Nova's formation could not be filled, its base units were left out of the count of
 * omnis, so every battle armor slot asked for magnetic clamps. Then the fallback ladder dropped that role only after
 * it had moved off the slot's weight class, so the medium Elemental, the only suit many Clans field, was never tried
 * without it.</p>
 *
 * <p>The Force Generator's tables and unit records are mocked, so only the generation logic is under test.</p>
 */
class NovaBattleArmorTest {
    private static final String OMNI_MEK = "Black Hawk (Nova) Prime";
    private static final String ELEMENTAL = "Elemental Battle Armor [Laser](Sqd5)";
    private static final int POINTS = 5;

    private MockedStatic<RATGenerator> ratGeneratorMock;
    private MockedStatic<UnitTable> unitTableMock;
    private Field rulesetTable;
    private Object previousRulesets;

    @BeforeEach
    void setUp() throws Exception {
        RATGenerator ratGenerator = mock(RATGenerator.class);
        ModelRecord omniMek = new ModelRecord("Black Hawk (Nova)", "Prime");
        omniMek.setOmni(true);
        when(ratGenerator.getModelRecord(OMNI_MEK)).thenReturn(omniMek);
        ModelRecord elemental = mock(ModelRecord.class);
        when(elemental.getKey()).thenReturn(ELEMENTAL);
        when(ratGenerator.getModelRecord(ELEMENTAL)).thenReturn(elemental);
        ratGeneratorMock = mockStatic(RATGenerator.class);
        ratGeneratorMock.when(RATGenerator::getInstance).thenReturn(ratGenerator);

        // No rulesets are loaded, so rating lookups fall back to the descriptor's own rating
        rulesetTable = Ruleset.class.getDeclaredField("rulesets");
        rulesetTable.setAccessible(true);
        previousRulesets = rulesetTable.get(null);
        rulesetTable.set(null, new HashMap<String, Ruleset>());

        unitTableMock = mockStatic(UnitTable.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        unitTableMock.close();
        ratGeneratorMock.close();
        rulesetTable.set(null, previousRulesets);
    }

    @Test
    @DisplayName("A Nova whose formation fails still counts its omnis, so its battle armor needs no clamps")
    void failedFormationStillCountsOmniBaseUnits() throws Exception {
        FormationType battleFormation = mock(FormationType.class);
        when(battleFormation.generateFormation(anyList(), anyList(), anyInt(), anyBoolean(), anyInt(), anyInt()))
              .thenReturn(new ArrayList<>());
        ForceDescriptor nova = spy(new ForceDescriptor());
        nova.setFaction("CHH");
        nova.setYear(3049);
        nova.setAugmented(true);
        nova.setFormationType(battleFormation);
        // The ordinary-lance fallback picks an OmniMek for every base slot and leaves the battle armor alone
        doAnswer(invocation -> {
            List<ForceDescriptor> slots = invocation.getArgument(0);
            for (ForceDescriptor slot : slots) {
                if (slot.getUnitType() == UnitType.MEK) {
                    slot.getModels().add(OMNI_MEK);
                }
            }
            return null;
        }).when(nova).generateLance(anyList());

        List<ForceDescriptor> mekSlots = slots(UnitType.MEK);
        List<ForceDescriptor> battleArmorSlots = slots(UnitType.BATTLE_ARMOR);
        List<ForceDescriptor> allSlots = new ArrayList<>(mekSlots);
        allSlots.addAll(battleArmorSlots);

        List<?> baseUnits = generateNovaFormation(nova, allSlots);

        assertEquals(POINTS, baseUnits.size(), "the base units picked by the fallback are handed back");
        assertNull(nova.getFormation(), "a Nova whose formation was not met must not carry its name");
        for (ForceDescriptor battleArmor : battleArmorSlots) {
            assertFalse(battleArmor.getRoles().contains(MissionRole.MAG_CLAMP),
                  "five OmniMeks carry five battle armor Points, so none of them needs magnetic clamps");
            assertTrue(battleArmor.getRoles().contains(MissionRole.MECHANIZED_BA),
                  "each Point rides an OmniMek, so it must be able to ride one");
        }
    }

    @Test
    @DisplayName("A slot whose role no unit meets is tried at its own weight class without the role")
    void unmetRoleIsDroppedAtTheRequestedWeight() {
        // The faction's only suit is the medium Elemental, which has no magnetic clamps
        UnitTable emptyTable = mock(UnitTable.class);
        UnitTable elementalTable = mock(UnitTable.class);
        MekSummary elemental = mock(MekSummary.class);
        when(elemental.getName()).thenReturn(ELEMENTAL);
        when(elementalTable.generateUnit(any())).thenReturn(elemental);
        unitTableMock.when(() -> UnitTable.findTable(any(), anyInt(), anyInt(), anyString(), any(), anyInt(), any(),
              any(), anyInt())).thenAnswer(invocation -> {
                  Collection<Integer> weightClasses = invocation.getArgument(4);
                  Collection<MissionRole> roles = invocation.getArgument(7);
                  boolean isMedium = weightClasses.contains(EntityWeightClass.WEIGHT_MEDIUM);
                  return (isMedium && roles.isEmpty()) ? elementalTable : emptyTable;
              });

        ForceDescriptor battleArmor = new ForceDescriptor();
        battleArmor.setFaction("CHH");
        battleArmor.setYear(3049);
        battleArmor.setRating("FL");
        battleArmor.setUnitType(UnitType.BATTLE_ARMOR);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        battleArmor.getRoles().add(MissionRole.MAG_CLAMP);

        ModelRecord picked = battleArmor.generate();

        assertNotNull(picked, "the slot must not be left empty while a medium suit exists");
        assertEquals(ELEMENTAL, picked.getKey());
    }

    private static List<ForceDescriptor> slots(int unitType) {
        List<ForceDescriptor> slots = new ArrayList<>();
        for (int i = 0; i < POINTS; i++) {
            ForceDescriptor slot = new ForceDescriptor();
            slot.setFaction("CHH");
            slot.setYear(3049);
            slot.setUnitType(unitType);
            slots.add(slot);
        }
        return slots;
    }

    private static List<?> generateNovaFormation(ForceDescriptor nova, List<ForceDescriptor> slots) throws Exception {
        Method method = ForceDescriptor.class.getDeclaredMethod("generateNovaFormation", List.class, int.class,
              int.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(nova, slots, ModelRecord.NETWORK_NONE, 0);
    }
}
