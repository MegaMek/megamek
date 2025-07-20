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
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.Mek;
import megamek.common.MiscType;
import megamek.common.WeaponType;
import megamek.common.WeaponTypeFlag;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static megamek.client.ui.unitreadout.TableElement.JUSTIFIED_CENTER;
import static megamek.client.ui.unitreadout.TableElement.JUSTIFIED_LEFT;

/**
 * This class contains some readout modules that are used by Entity types that are not in the same hierarchy (e.g.
 * support vehicles that can be Aero and Tank) but still have only limited use.
 */
final class ReadoutUtils {

    static List<ViewElement> createChassisModList(Entity entity) {

        List<MiscMounted> chassisMods = entity.getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION))
              .toList();

        if (chassisMods.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ViewElement> result = new ArrayList<>();
            ItemList list = new ItemList("Chassis Modifications");
            chassisMods.forEach(mod -> list.addItem(mod.getShortName()));
            result.add(new PlainLine());
            result.add(list);
            return result;
        }
    }

    static List<ViewElement> getWeaponsNoHeat(Entity entity, boolean showDetail, ViewFormatting formatting) {
        List<ViewElement> retVal = new ArrayList<>();

        if (entity.getWeaponList().isEmpty()) {
            return retVal;
        }

        TableElement wpnTable = new TableElement(3);
        wpnTable.setColNames("Weapons", "Loc", entity.isOmni() ? "Omni" : "");
        wpnTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER);
        for (WeaponMounted mounted : entity.getWeaponList()) {
            if (mounted.getType().hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)) {
                continue;
            }
            String[] row = { mounted.getDesc() + GeneralEntityReadout.quirkMarker(mounted),
                             entity.joinLocationAbbr(mounted.allLocations(), 3), "" };
            WeaponType wtype = mounted.getType();

            if (entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.IS)) {
                row[0] += Messages.getString("MekView.IS");
            }
            if (!entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.CLAN)) {
                row[0] += Messages.getString("MekView.Clan");
            }

            int bWeapDamaged = 0;
            if (wtype instanceof BayWeapon) {
                // loop through weapons in bay and add up heat
                for (WeaponMounted m : mounted.getBayWeapons()) {
                    if (m.isDestroyed()) {
                        bWeapDamaged++;
                    }
                }
            }

            if (entity.isOmni()) {
                row[2] = Messages.getString(mounted.isOmniPodMounted() ? "MekView.Pod" : "MekView.Fixed");
            } else if (wtype instanceof BayWeapon && bWeapDamaged > 0 && !showDetail) {
                row[2] = ViewElement.warningStart(formatting) + Messages.getString("MekView.WeaponDamage")
                      + ")" + ViewElement.warningEnd(formatting);
            }
            if (mounted.isDestroyed()) {
                if (mounted.isRepairable()) {
                    wpnTable.addRowWithColor("yellow", row);
                } else {
                    wpnTable.addRowWithColor("red", row);
                }
            } else {
                wpnTable.addRow(row);
            }

            // if this is a weapon bay, then cycle through weapons and ammo
            if ((wtype instanceof BayWeapon) && showDetail) {
                for (WeaponMounted m : mounted.getBayWeapons()) {
                    row = new String[] { m.getDesc(), "", "", "" };

                    if (entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.IS)) {
                        row[0] += Messages.getString("MekView.IS");
                    }
                    if (!entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.CLAN)) {
                        row[0] += Messages.getString("MekView.Clan");
                    }
                    if (m.isDestroyed()) {
                        if (m.isRepairable()) {
                            wpnTable.addRowWithColor("yellow", row);
                        } else {
                            wpnTable.addRowWithColor("red", row);
                        }
                    } else {
                        wpnTable.addRow(row);
                    }
                }
                for (AmmoMounted m : mounted.getBayAmmo()) {
                    // Ignore ammo for one-shot launchers
                    if ((m.getLinkedBy() != null) && m.getLinkedBy().isOneShot()) {
                        continue;
                    }
                    if (mounted.getLocation() != Entity.LOC_NONE) {
                        row = new String[] { m.getName(), String.valueOf(m.getBaseShotsLeft()), "" };
                        if (m.isDestroyed()) {
                            wpnTable.addRowWithColor("red", row);
                        } else if (m.getUsableShotsLeft() < 1) {
                            wpnTable.addRowWithColor("yellow", row);
                        } else {
                            wpnTable.addRow(row);
                        }
                    }
                }
            }
        }
        retVal.add(wpnTable);
        return retVal;
    }

    static boolean hideMisc(MiscMounted mounted, Entity entity) {
        String name = mounted.getName();
        return (((mounted.getLocation() == Entity.LOC_NONE)
              // Meks can have zero-slot equipment in LOC_NONE that needs to be shown.
              && (!(entity instanceof Mek) || mounted.getCriticals() > 0)))
              || name.contains("Jump Jet")
              || (name.contains("CASE") && !name.contains("II") && entity.isClan())
              || (name.contains("Heat Sink") && !name.contains("Radical"))
              || EquipmentType.isArmorType(mounted.getType())
              || EquipmentType.isStructureType(mounted.getType())
              || mounted.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)
              || mounted.getType().hasFlag(MiscType.F_ARMOR_KIT);
    }

    private ReadoutUtils() { }
}
