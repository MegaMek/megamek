/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of Megamek.
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

package megamek.client.ui.entityreadout;

import megamek.client.ui.Messages;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.HandheldWeapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity Readout specialties for Handheld Weapons
 */
class HhwReadout extends GeneralEntityReadout {
    
    private final HandheldWeapon hhw;

    protected HhwReadout(HandheldWeapon entity, boolean showDetail, boolean useAlternateCost, boolean ignorePilotBV) {
        super(entity, showDetail, useAlternateCost, ignorePilotBV);
        hhw = entity;
    }

    @Override
    protected ViewElement createTotalArmorElement() {
        return new LabeledLine(Messages.getString("MekView.Armor"), String.valueOf(hhw.getTotalArmor()));
    }

    @Override
    protected List<ViewElement> createArmorElements() {
        List<ViewElement> result = new ArrayList<>();
        result.add(createTotalArmorElement());
        return result;
    }

    @Override
    protected List<ViewElement> createMovementElements() {
        return Collections.emptyList();
    }

    @Override
    protected ViewElement createEngineElement() {
        return new EmptyElement();
    }

    @Override
    protected TableElement getAmmo() {
        TableElement ammoTable = new TableElement(2);
        ammoTable.setColNames("Ammo", "Shots");
        ammoTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER);

        for (AmmoMounted mounted : hhw.getAmmo()) {
            if (hideAmmo(mounted)) {
                continue;
            }

            ViewElement[] row = new ViewElement[2];
            row[0] = new PlainElement(mounted.getName());

            if (mounted.isDestroyed() || (mounted.getUsableShotsLeft() < 1)) {
                row[1] = new DestroyedElement(mounted.getBaseShotsLeft());
            } else if (mounted.getUsableShotsLeft() < mounted.getOriginalShots()) {
                row[1] = new DamagedElement(mounted.getBaseShotsLeft());
            } else {
                row[1] = new PlainElement(mounted.getBaseShotsLeft());
            }
            ammoTable.addRow(row);
        }

        return ammoTable;
    }

    @Override
    protected List<ViewElement> getWeapons(boolean showDetail) {
        return ReadoutUtils.getSimpleWeaponsList(hhw);
    }
}
