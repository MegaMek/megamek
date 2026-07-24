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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Tests the pure unit-type filter decision function, in particular the Battlefield Support Asset handling: the Asset
 * filter matches assets and base units that have a linked asset, and a linked base unit still matches its own type.
 */
class AbstractUnitSelectorDialogFilterTest {

    private static final String ASSET_TYPE_NAME = UnitType.getTypeName(UnitType.BATTLEFIELD_SUPPORT_ASSET);
    private static final String TANK_TYPE_NAME = UnitType.getTypeName(UnitType.TANK);

    private static boolean match(int typeCode, String unitTypeName, boolean isAsset, boolean isSupport,
          boolean hasLinkedAsset) {
        return AbstractUnitSelectorDialog.matchesUnitTypeSelection(typeCode, unitTypeName, isAsset, isSupport,
              hasLinkedAsset);
    }

    @Test
    void allFilterMatchesEverything() {
        assertTrue(match(AbstractUnitSelectorDialog.UNIT_TYPE_ALL, TANK_TYPE_NAME, false, false, false));
        assertTrue(match(AbstractUnitSelectorDialog.UNIT_TYPE_ALL, ASSET_TYPE_NAME, true, false, false));
    }

    @Test
    void supportVeeFilterMatchesSupportUnits() {
        assertTrue(match(AbstractUnitSelectorDialog.UNIT_TYPE_SUPPORT_VEE, TANK_TYPE_NAME, false, true, false));
        assertFalse(match(AbstractUnitSelectorDialog.UNIT_TYPE_SUPPORT_VEE, TANK_TYPE_NAME, false, false, false));
    }

    @Test
    void typeFilterMatchesOwnType() {
        assertTrue(match(UnitType.TANK, TANK_TYPE_NAME, false, false, false));
        assertFalse(match(UnitType.TANK, ASSET_TYPE_NAME, true, false, false));
    }

    @Test
    void assetFilterMatchesStandaloneAsset() {
        assertTrue(match(UnitType.BATTLEFIELD_SUPPORT_ASSET, ASSET_TYPE_NAME, true, false, false));
    }

    @Test
    void assetFilterMatchesBaseUnitWithLinkedAsset() {
        // A Tank base unit that has a linked asset appears under the Asset filter (the condensed row).
        assertTrue(match(UnitType.BATTLEFIELD_SUPPORT_ASSET, TANK_TYPE_NAME, false, false, true));
    }

    @Test
    void assetFilterExcludesPlainBaseUnit() {
        assertFalse(match(UnitType.BATTLEFIELD_SUPPORT_ASSET, TANK_TYPE_NAME, false, false, false));
    }

    @Test
    void linkedBaseUnitStillMatchesItsOwnType() {
        // The same condensed Tank+asset row must also appear under the Tank filter.
        assertTrue(match(UnitType.TANK, TANK_TYPE_NAME, false, false, true));
    }

    @Test
    void selectAsUnitRequiresNonEmptySelection() {
        assertFalse(AbstractUnitSelectorDialog.canSelectSelectionAsUnit(0, 0));
    }

    @Test
    void selectAsUnitAllowedWhenNoAssetOnlyRows() {
        // Two plain/linked base units, none of them asset-only.
        assertTrue(AbstractUnitSelectorDialog.canSelectSelectionAsUnit(2, 0));
    }

    @Test
    void selectAsUnitDisabledWhenAnyAssetOnlyRow() {
        // Mixed selection with one standalone asset disables selecting as unit.
        assertFalse(AbstractUnitSelectorDialog.canSelectSelectionAsUnit(3, 1));
    }

    @Test
    void selectAsAssetRequiresNonEmptySelection() {
        assertFalse(AbstractUnitSelectorDialog.canSelectSelectionAsAsset(0, 0));
    }

    @Test
    void selectAsAssetAllowedWhenEveryRowHasAssetForm() {
        assertTrue(AbstractUnitSelectorDialog.canSelectSelectionAsAsset(2, 0));
    }

    @Test
    void selectAsAssetDisabledWhenAnyRowLacksAssetForm() {
        // Any TW-only row (no asset form) disables selecting as asset.
        assertFalse(AbstractUnitSelectorDialog.canSelectSelectionAsAsset(3, 1));
    }
}
