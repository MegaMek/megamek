/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.unitreadout;

import megamek.client.ui.Messages;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.BattleArmor;
import megamek.common.EquipmentType;

import java.util.ArrayList;
import java.util.List;

class BattleArmorReadout extends GeneralEntityReadout2 {

    BattleArmor battleArmor;

    protected BattleArmorReadout(BattleArmor battleArmor, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV, ViewFormatting formatting) {

        super(battleArmor, showDetail, useAlternateCost, ignorePilotBV, formatting);
        this.battleArmor = battleArmor;
    }

    @Override
    protected ViewElement createTotalArmorElement() {
        String armor = battleArmor.getTotalArmor() + " "
              + EquipmentType.getArmorTypeName(battleArmor.getArmorType(1)).trim();
        return new LabeledElement(Messages.getString("MekView.Armor"), armor);
    }

    @Override
    protected ViewElement createEngineElement() {
        return new EmptyElement();
    }

    @Override
    protected List<ViewElement> createMiscMovementElements() {
        List<ViewElement> result = new ArrayList<>();
        if (battleArmor.isBurdened()) {
            result.add(new PlainLine(ViewElement.italicize(Messages.getString("MekView.Burdened"), formatting)));
        }
        if (battleArmor.hasDWP()) {
            result.add(new PlainLine(ViewElement.italicize(Messages.getString("MekView.DWPBurdened"), formatting)));
        }
        return result;
    }
}
