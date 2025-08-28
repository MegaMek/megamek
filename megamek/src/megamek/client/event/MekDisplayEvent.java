/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.event;

import java.io.Serial;

import megamek.common.units.Entity;
import megamek.common.equipment.Mounted;

/**
 * Instances of this class are sent as a result of changes in MekDisplay
 *
 * @see MekDisplayListener
 */
public class MekDisplayEvent extends java.util.EventObject {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -932419778029797238L;

    public static final int WEAPON_SELECTED = 0;

    private final int entityId;
    private final int weaponId;
    private final Entity entity;
    private final Mounted<?> equip;
    private final int type;

    public MekDisplayEvent(Object source, Entity entity, Mounted<?> weapon) {
        super(source);
        this.entity = entity;
        this.entityId = entity.getId();
        this.type = WEAPON_SELECTED;
        this.equip = weapon;
        this.weaponId = entity.getEquipmentNum(weapon);
    }

    /**
     * Returns the type of event that this is
     */
    public int getType() {
        return type;
    }

    /**
     * @return the entity ID associated with this event, if applicable; -1 otherwise.
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * @return the weapon ID associated with this event, if applicable; -1 otherwise.
     */
    public int getWeaponId() {
        return weaponId;
    }

    /**
     * @return the entity associated with event, or null
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @return the equipment associated with the event or null
     */
    public Mounted<?> getEquip() {
        return equip;
    }
}
