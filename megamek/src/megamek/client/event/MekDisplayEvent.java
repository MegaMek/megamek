/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.event;

import megamek.common.Entity;
import megamek.common.Mounted;

/**
 * Instances of this class are sent as a result of changes in MekDisplay
 *
 * @see MekDisplayListener
 */
public class MekDisplayEvent extends java.util.EventObject {
    /**
     *
     */
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
     * @return the entity ID associated with this event, if applicable; -1
     *         otherwise.
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * @return the weapon ID associated with this event, if applicable; -1
     *         otherwise.
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
