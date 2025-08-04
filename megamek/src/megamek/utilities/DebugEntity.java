/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.utilities;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.Infantry;
import megamek.common.Mek;
import megamek.common.ProtoMek;
import megamek.common.Transporter;
import megamek.common.equipment.WeaponMounted;

/**
 * This class is for debugging Entity with respect to the internal state of equipment.
 */
public final class DebugEntity {

    /**
     * Gets a full listing of the internal representation of the unit's equipment and crit slots with most of the
     * internal state of each ({@link #getEquipmentState(Entity)}) and copies it to the clipboard.
     *
     * @param entity The entity to debug
     */
    public static void copyEquipmentState(Entity entity) {
        copyToClipboard(getEquipmentState(entity));
    }

    /** Copies the given text to the Clipboard. */
    public static void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * Returns a full listing of the internal representation of the unit's equipment and crit slots with most of the
     * internal state of each.
     *
     * @param entity The entity to debug
     *
     * @return A String describing the internal state of the Entity's equipment
     */
    public static String getEquipmentState(Entity entity) {
        StringBuilder result = new StringBuilder();
        try {
            result.append("Chassis: >").append(entity.getChassis()).append("<\n");
            result.append("Model: >").append(entity.getModel()).append("<\n");
            result.append("Game ID: ").append(entity.getId()).append("\n");

            result.append("Equipment:\n");
            for (int i = 0; i < entity.getEquipment().size(); i++) {
                result.append("[").append(i).append("] ").append(entity.getEquipment(i)).append("\n");
                if (entity != entity.getEquipment(i).getEntity()) {
                    result.append("Different Entity!");
                }
            }
            result.append("\n");

            if (entity.isConventionalInfantry()) {
                result.append("Weapons:\n");
                for (WeaponMounted weapon : entity.getTotalWeaponList()) {
                    result.append(weapon).append("\n");
                }
                result.append("\n");

                Infantry infantry = (Infantry) entity;
                result.append("Infantry weapons:\n");
                if (infantry.getPrimaryWeapon() != null) {
                    result.append("Primary: ").append(infantry.getPrimaryWeapon()).append("\n");
                }
                if (infantry.getSecondaryWeapon() != null) {
                    result.append("Secondary: ").append(infantry.getSecondaryWeapon()).append("\n");
                }
                result.append("\n");
            }

            if (!entity.getTransports().isEmpty()) {
                result.append("Transports:\n");
                List<Transporter> transports = entity.getTransports();
                for (int i = 0; i < transports.size(); i++) {
                    result.append("[").append(i).append("] ").append(transports.get(i)).append("\n");
                }
                result.append("\n");
            }

            result.append("Locations:\n");
            for (int location = 0; location < entity.locations(); location++) {
                result.append(entity.getLocationAbbr(location)).append(":\n");
                for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
                    CriticalSlot criticalSlot = entity.getCritical(location, slot);
                    if (criticalSlot != null) {
                        result.append("[").append(slot).append("] ").append(criticalSlot);
                        if (criticalSlot.getType() == 0) {
                            result.append(" (");
                            if (entity instanceof Mek) {
                                result.append(((Mek) entity).getSystemName(criticalSlot.getIndex()));
                            } else if (entity instanceof ProtoMek) {
                                result.append(ProtoMek.SYSTEM_NAMES[criticalSlot.getIndex()]);
                            }
                            result.append(")");
                        }
                        result.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            result.append("\nAn exception was encountered here. ").append(e.getMessage());
        }

        if (entity instanceof FighterSquadron fighterSquadron) {
            for (Entity fighter : fighterSquadron.getLoadedUnits()) {
                result.append("\n\n");
                result.append(getEquipmentState(fighter));
            }
        }

        return result.toString();
    }

    private DebugEntity() {
    }
}
