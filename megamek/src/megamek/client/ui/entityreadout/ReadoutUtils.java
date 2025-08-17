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
package megamek.client.ui.entityreadout;

import static megamek.client.ui.entityreadout.TableElement.JUSTIFIED_CENTER;
import static megamek.client.ui.entityreadout.TableElement.JUSTIFIED_LEFT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.bays.Bay;
import megamek.common.equipment.*;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.common.units.InfantryCompartment;
import megamek.common.units.Mek;
import megamek.common.weapons.bayweapons.BayWeapon;

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

    static List<ViewElement> getWeapons(Entity entity, boolean showDetail) {
        List<ViewElement> result = new ArrayList<>();

        if (entity.getWeaponList().isEmpty()) {
            return result;
        }

        TableElement wpnTable = new TableElement(4);
        wpnTable.setColNames("Weapons", "Loc", "Heat", entity.isOmni() ? "Omni" : "");
        wpnTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER, JUSTIFIED_CENTER);
        for (WeaponMounted mounted : entity.getWeaponList()) {
            if (mounted.getType().hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)) {
                continue;
            }

            ViewElement[] row = createWeaponTableRow(mounted, entity, true);

            WeaponType weaponType = mounted.getType();
            if (weaponType instanceof BayWeapon) {
                BayInfo bayInfo = getBayInfo(mounted);
                row[2] = new PlainElement(bayInfo.totalHeat());
                if ((bayInfo.damagedWeapons() > 0) && !showDetail) {
                    row[3] = new DestroyedElement(Messages.getString("MekView.WeaponDamage") + ")");
                }
            } else {
                row[2] = new PlainElement(weaponType.getHeat());
                if (entity.isOmni()) {
                    row[3] = new PlainElement(Messages.getString(mounted.isOmniPodMounted() ?
                          "MekView.Pod" :
                          "MekView.Fixed"));
                }
            }
            wpnTable.addRow(row);

            if ((weaponType instanceof BayWeapon) && showDetail) {
                addBayWeaponList(mounted, wpnTable, entity, true);
                addBayAmmoList(mounted, wpnTable);
            }
        }
        result.add(wpnTable);
        return result;
    }

    static List<ViewElement> getWeaponsNoHeat(Entity entity, boolean showDetail) {
        List<ViewElement> result = new ArrayList<>();

        if (entity.getWeaponList().isEmpty()) {
            return result;
        }

        TableElement wpnTable = new TableElement(3);
        wpnTable.setColNames("Weapons", "Loc", entity.isOmni() ? "Omni" : "");
        wpnTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER);
        for (WeaponMounted mounted : entity.getWeaponList()) {
            if (mounted.getType().hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)) {
                continue;
            }

            ViewElement[] row = createWeaponTableRow(mounted, entity, false);

            WeaponType weaponType = mounted.getType();
            if (weaponType instanceof BayWeapon) {
                BayInfo bayInfo = getBayInfo(mounted);
                if (bayInfo.damagedWeapons() > 0 && !showDetail) {
                    row[2] = new DamagedElement(Messages.getString("MekView.WeaponDamage") + ")");
                }
            } else if (entity.isOmni()) {
                row[2] = new PlainElement(Messages.getString(mounted.isOmniPodMounted() ?
                      "MekView.Pod" :
                      "MekView.Fixed"));
            }

            wpnTable.addRow(row);

            if ((weaponType instanceof BayWeapon) && showDetail) {
                addBayWeaponList(mounted, wpnTable, entity, false);
                addBayAmmoList(mounted, wpnTable);
            }
        }
        result.add(wpnTable);
        return result;
    }

    private record BayInfo(int damagedWeapons, int totalHeat) {}

    /**
     * Collects and returns the total weapon heat and the number of damaged weapons in the given bayWeapon.
     */
    private static BayInfo getBayInfo(WeaponMounted bayWeapon) {
        WeaponType weaponType = bayWeapon.getType();
        int heat = weaponType.getHeat();
        int bWeaponDamaged = 0;
        if (weaponType instanceof BayWeapon) {
            // loop through weapons in bay and add up heat
            heat = 0;
            for (WeaponMounted m : bayWeapon.getBayWeapons()) {
                heat = heat + m.getType().getHeat();
                if (m.isDestroyed()) {
                    bWeaponDamaged++;
                }
            }
        }
        return new BayInfo(bWeaponDamaged, heat);
    }

    private static ViewElement[] createWeaponTableRow(Mounted<?> mounted, Entity entity, boolean withHeatColumn) {
        String name = mounted.getDesc() + GeneralEntityReadout.quirkMarker(mounted);
        if (entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.IS)) {
            name += Messages.getString("MekView.IS");
        }
        if (!entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.CLAN)) {
            name += Messages.getString("MekView.Clan");
        }
        ViewElement nameElement = new PlainElement(name);
        if (mounted.isDestroyed() && mounted.isRepairable()) {
            nameElement = new DamagedElement(name);
        } else if (mounted.isDestroyed()) {
            nameElement = new DestroyedElement(name);
        }
        ViewElement location = new PlainElement(entity.joinLocationAbbr(mounted.allLocations(), 3));
        if (withHeatColumn) {
            return new ViewElement[] { nameElement, location, new EmptyElement(), new EmptyElement() };
        } else {
            return new ViewElement[] { nameElement, location, new EmptyElement() };
        }
    }

    private static void addBayWeaponList(WeaponMounted mounted, TableElement wpnTable, Entity entity,
          boolean withHeatColumn) {
        for (WeaponMounted m : mounted.getBayWeapons()) {

            String name = "- " + m.getDesc();
            if (entity.isClan() && (m.getType().getTechBase() == ITechnology.TechBase.IS)) {
                name += Messages.getString("MekView.IS");
            }
            if (!entity.isClan() && (m.getType().getTechBase() == ITechnology.TechBase.CLAN)) {
                name += Messages.getString("MekView.Clan");
            }
            ViewElement nameElement = new PlainElement(name);
            if (mounted.isDestroyed() && mounted.isRepairable()) {
                nameElement = new DamagedElement(name);
            } else if (mounted.isDestroyed()) {
                nameElement = new DestroyedElement(name);
            }
            ViewElement location = new PlainElement(entity.joinLocationAbbr(mounted.allLocations(), 3));
            ViewElement[] row = new ViewElement[] { nameElement, location, new EmptyElement() };
            if (withHeatColumn) {
                row = new ViewElement[] { nameElement, location, new EmptyElement(), new EmptyElement() };
            }
            wpnTable.addRow(row);
        }
    }

    private static void addBayAmmoList(WeaponMounted mounted, TableElement wpnTable) {
        for (AmmoMounted m : mounted.getBayAmmo()) {
            // Ignore ammo for one-shot launchers
            if ((m.getLinkedBy() != null) && m.getLinkedBy().isOneShot()) {
                continue;
            }
            if (mounted.getLocation() != Entity.LOC_NONE) {
                var row = new ViewElement[] { new PlainElement("- " + m.getName()),
                                              new PlainElement(m.getBaseShotsLeft()),
                                              new EmptyElement(),
                                              new EmptyElement() };
                if (m.isDestroyed()) {
                    row[0] = new DestroyedElement("- " + m.getName());
                } else if (m.getUsableShotsLeft() < 1) {
                    row[0] = new DamagedElement("- " + m.getName());
                }
                wpnTable.addRow(row);
            }
        }
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

    static ViewElement renderArmorAsViewElement(int currentArmor, int fullArmor) {
        double percentRemaining = ((double) currentArmor) / ((double) fullArmor);
        String armor = Integer.toString(currentArmor);

        if (percentRemaining < 0) {
            return new DestroyedElement("X");
        } else if (percentRemaining <= .25) {
            return new DestroyedElement(armor);
        } else if (percentRemaining < 1.00) {
            return new DamagedElement(armor);
        } else {
            return new PlainElement(armor);
        }
    }


    /**
     * @return An item list of the given entity's transports.
     */
    static ItemList createTransporterList(Entity entity) {
        var transportsList = new ItemList(Messages.getString("MekView.CarryingCapacity"));
        for (Transporter transporter : entity.getTransports()) {
            if ((transporter instanceof Bay bay) && bay.isQuarters()) {
                continue;
            }
            if ((transporter instanceof DockingCollar dockingCollar) && dockingCollar.isDamaged()) {
                continue;
            }
            String transporterStatus = transporter.getUnusedString();
            if (entity.isOmni() && ((transporter instanceof InfantryCompartment) || (transporter instanceof Bay))) {
                transporterStatus += entity.isPodMountedTransport(transporter) ? " (Pod)" : " (Fixed)";
            }
            ViewElement transporterElement = new PlainElement(transporterStatus);
            if ((transporter instanceof Bay bay) && (bay.getBayDamage() > 0)) {
                transporterElement = new DamagedElement(transporterStatus);
            }
            transportsList.addItem(transporterElement);
        }

        return transportsList;
    }

    private ReadoutUtils() {}
}
