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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.client.ui.util.ViewFormatting;
import megamek.common.*;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;

/**
 * The Entity information shown in the unit selector and many other places in MM, MML and MHQ.
 *
 */
class InfantryReadout2 extends GeneralEntityReadout2 {

    protected final Infantry infantry;

    protected InfantryReadout2(Infantry infantry, boolean showDetail, boolean useAlternateCost,
                            boolean ignorePilotBV, ViewFormatting formatting) {

        super(infantry, showDetail, useAlternateCost, ignorePilotBV, formatting);
        this.infantry = infantry;
    }

    @Override
    protected ViewElement createWeightElement() {
        return new EmptyElement();
    }

    @Override
    protected List<ViewElement> createLoadoutBlock() {
        List<ViewElement> result = super.createLoadoutBlock();

        if (infantry.getSpecializations() > 0) {
            ItemList specList = new ItemList("Infantry Specializations");
            for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                int spec = 1 << i;
                if (infantry.hasSpecialization(spec)) {
                    specList.addItem(Infantry.getSpecializationName(spec));
                }
            }
            result.add(new PlainLine());
            result.add(specList);
        }

        if (infantry.getCrew() != null) {
            ArrayList<String> augmentations = new ArrayList<>();
            for (Enumeration<IOption> e = infantry.getCrew().getOptions(PilotOptions.MD_ADVANTAGES);
                  e.hasMoreElements();) {
                final IOption o = e.nextElement();
                if (o.booleanValue()) {
                    augmentations.add(o.getDisplayableName());
                }
            }

            if (!augmentations.isEmpty()) {
                ItemList augList = new ItemList("Augmentations");
                for (String aug : augmentations) {
                    augList.addItem(aug);
                }
                result.add(new PlainLine());
                result.add(augList);
            }
        }
        return result;
    }
}
