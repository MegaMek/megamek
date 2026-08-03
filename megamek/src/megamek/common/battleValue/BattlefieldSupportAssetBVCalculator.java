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
package megamek.common.battleValue;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.units.Entity;

/**
 * Battle Value calculator for Battlefield Support Assets. Assets are not built from armor/weapons/engine like standard
 * units; their Battle Value is derived directly from their Battlefield Support Point cost. This calculator therefore
 * bypasses the standard base/defensive/offensive BV machinery and selects the designer-provided Regular or Veteran
 * value from the Asset.
 */
public class BattlefieldSupportAssetBVCalculator extends BVCalculator {

    BattlefieldSupportAssetBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    public int calculateBV(boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        this.ignoreC3 = ignoreC3;
        this.ignoreSkill = ignoreSkill;
        int bv = (entity instanceof BattlefieldSupportAsset asset)
              ? (ignoreSkill ? asset.getBv() : asset.getEffectiveBv())
              : 0;
        adjustedBV = bv;
        baseBV = bv;
        return bv;
    }
}
