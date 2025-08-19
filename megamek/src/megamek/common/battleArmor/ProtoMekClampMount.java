/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;

/**
 * Represents a section of a Mek torso where a protomek equipped with the magnetic clamp system can attach itself for
 * transport. A mek has two of these, one front and one rear. An ultraheavy protomek can only be carried on the front
 * mount, and if carried this way the rear cannot be used. The two mounts are not aware of each other, and it is the
 * responsibility of the code that handles loading to enforce this limitation.
 *
 * @author Neoancient
 */
public class ProtoMekClampMount extends BattleArmorHandles {
    @Serial
    private static final long serialVersionUID = 3937766099677646981L;

    private final boolean rear;

    private static final String NO_VACANCY_STRING = "A protomek is loaded";
    private static final String HAVE_VACANCY_STRING = "One protomek";

    public ProtoMekClampMount(boolean rear) {
        this.rear = rear;
    }

    public boolean isRear() {
        return rear;
    }

    @Override
    public String getUnusedString() {
        return (carriedUnit != Entity.NONE) ? NO_VACANCY_STRING : HAVE_VACANCY_STRING;
    }

    @Override
    public boolean canLoad(Entity unit) {
        return (carriedUnit == Entity.NONE) && unit.isProtoMek() && unit.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP)
              && (!rear || unit.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY);
    }

    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return (rear == isRear) && (loc == Mek.LOC_CT) && (carriedUnit != Entity.NONE);
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        double protoWeight = 0.0;
        if (carriedUnit != Entity.NONE) {
            Entity carriedEntity = game.getEntity(carriedUnit);
            if (carriedEntity != null) {
                protoWeight = carriedEntity.getWeight();
                if (carrier.isOmni()) {
                    protoWeight = Math.max(0, protoWeight - 3.0);
                }
            }
        }
        if (protoWeight < carrier.getWeight() * 0.1) {
            return 0;
        } else if (protoWeight < carrier.getWeight() * 0.25) {
            return Math.min(3, carrier.getOriginalWalkMP() / 2);
        } else {
            return carrier.getOriginalWalkMP() / 2;
        }
    }

    @Override
    public String toString() {
        return "Protomek clamp mount:" + carriedUnit;
    }
}
