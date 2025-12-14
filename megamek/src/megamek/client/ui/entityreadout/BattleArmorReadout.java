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
import java.util.List;
import java.util.StringJoiner;

import megamek.client.ui.Messages;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.TechBase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.attacks.LegAttack;
import megamek.common.weapons.attacks.StopSwarmAttack;
import megamek.common.weapons.attacks.SwarmAttack;

class BattleArmorReadout extends GeneralEntityReadout {

    BattleArmor battleArmor;

    protected BattleArmorReadout(BattleArmor battleArmor, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV) {

        super(battleArmor, showDetail, useAlternateCost, ignorePilotBV);
        this.battleArmor = battleArmor;
    }

    @Override
    protected ViewElement createTotalArmorElement() {
        String armor = battleArmor.getTotalArmor() + " "
              + EquipmentType.getArmorTypeName(battleArmor.getArmorType(1)).trim();
        return new LabeledLine(Messages.getString("MekView.Armor"), armor);
    }

    @Override
    protected ViewElement createEngineElement() {
        return new EmptyElement();
    }

    @Override
    protected List<ViewElement> createMiscMovementElements() {
        List<ViewElement> result = new ArrayList<>();
        if (battleArmor.isBurdened()) {
            result.add(new PlainLine(Messages.getString("MekView.Burdened")));
        }
        if (battleArmor.hasDWP()) {
            result.add(new PlainLine(Messages.getString("MekView.DWPBurdened")));
        }
        if (battleArmor.canDoMechanizedBA()) {
            result.add(new PlainLine("Can ride as Mechanized BA"));
        }
        return result;
    }

    @Override
    protected List<ViewElement> getWeapons(boolean showDetail) {
        List<ViewElement> result = new ArrayList<>();

        if (!battleArmor.getWeaponList().isEmpty()) {
            TableElement wpnTable = new TableElement(4);
            wpnTable.setColNames("Weapons", "Loc");
            wpnTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER);
            for (WeaponMounted mounted : battleArmor.getWeaponList()) {
                if (mounted.getType().hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)) {
                    continue;
                }
                wpnTable.addRow(createEquipmentTableRow(mounted));
            }
            result.add(wpnTable);
        }

        StringJoiner otherAttacks = new StringJoiner(", ");
        if (canLegAttack(battleArmor)) {
            otherAttacks.add("Leg Attack");
        }
        if (canSwarm(battleArmor)) {
            otherAttacks.add("Swarm Attack");
        }
        if (otherAttacks.length() > 0) {
            if (!result.isEmpty()) {
                result.add(new PlainLine());
            }
            result.add(new LabeledLine("Other Attacks", otherAttacks.toString()));
        }
        return result;
    }

    private ViewElement[] createEquipmentTableRow(Mounted<?> mounted) {
        String location = getLocation(mounted);

        String name = sanitizeMountedDesc(mounted) + quirkMarker(mounted);
        if (battleArmor.isClan() && (mounted.getType().getTechBase() == TechBase.IS)) {
            name += Messages.getString("MekView.IS");
        }
        if (!battleArmor.isClan() && (mounted.getType().getTechBase() == TechBase.CLAN)) {
            name += Messages.getString("MekView.Clan");
        }
        ViewElement nameElement = new PlainElement(name);
        if (mounted.isDestroyed() && mounted.isRepairable()) {
            nameElement = new DamagedElement(name);
        } else if (mounted.isDestroyed()) {
            nameElement = new DestroyedElement(name);
        }
        return new ViewElement[] { nameElement, new PlainElement(location) };
    }

    private static String getLocation(Mounted<?> mounted) {
        String location = BattleArmor.getBaMountLocName(mounted.getBaMountLoc());
        if (mounted.isDWPMounted()) {
            location = "DWP";
        }
        if (mounted.isAPMMounted()) {
            Mounted<?> apMount = mounted.getLinkedBy();
            if (apMount != null) {
                location = BattleArmor.getBaMountLocName(apMount.getBaMountLoc());
            }
            location += " (APM)";
        }
        if (mounted.isSquadSupportWeapon()) {
            location = "SSWM";
        }
        return location;
    }

    private String sanitizeMountedDesc(Mounted<?> mounted) {
        String toRemove = " (%s)".formatted(BattleArmor.getBaMountLocName(mounted.getBaMountLoc()));
        String name = mounted.getDesc();
        EquipmentType type = mounted.getType();
        if (type instanceof MiscType && type.hasFlag(MiscType.F_BA_MANIPULATOR)) {
            name = mounted.getShortName();
        }
        return name
              .replace(toRemove, "")
              .replace(" (DWP)", "")
              .replace(" (SSWM)", "")
              .replace(" (APM)", "");
    }

    public static boolean canSwarm(Infantry ba) {
        return ba.getEquipment()
              .stream()
              .map(Mounted::getType)
              .anyMatch(type -> (type instanceof SwarmAttack) || (type instanceof StopSwarmAttack));
    }

    public static boolean canLegAttack(Infantry ba) {
        return ba.getEquipment()
              .stream()
              .map(Mounted::getType)
              .anyMatch(type -> (type instanceof LegAttack));
    }

    @Override
    protected TableElement getAmmo() {
        TableElement ammoTable = new TableElement(2);
        ammoTable.setColNames("Ammo", "Shots");
        ammoTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER);

        for (Mounted<?> mounted : battleArmor.getAmmo()) {
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
    protected TableElement createMiscTable() {
        TableElement miscTable = new TableElement(2);
        miscTable.setColNames("Equipment", "Loc");
        miscTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER);
        for (Mounted<?> mounted : battleArmor.getMisc()) {
            String name = mounted.getName();
            if ((mounted.getLocation() == Entity.LOC_NONE)
                  || name.contains("Jump Jet")
                  || (name.contains("CASE") && !name.contains("II") && battleArmor.isClan())
                  || EquipmentType.isArmorType(mounted.getType())
                  || EquipmentType.isStructureType(mounted.getType())) {
                // These items are displayed elsewhere, so skip them here.
                continue;
            }

            miscTable.addRow(createEquipmentTableRow(mounted));
        }

        return miscTable;
    }

    @Override
    protected ViewElement createTotalInternalElement() {
        return new EmptyElement();
    }

    @Override
    protected ViewElement createMovementString() {
        int walkMP = battleArmor.getWalkMP();
        int runMP = battleArmor.getRunMP();
        int jumpMP = battleArmor.getJumpMP();
        int umuMP = battleArmor.getAllUMUCount();
        StringJoiner movement = new StringJoiner("/");
        movement.add(walkMP + "");
        if (runMP > walkMP) {
            // Infantry fast movement option; otherwise run mp = walk mp
            movement.add("%d (Fast)".formatted(runMP));
        }
        if (jumpMP > 0) {
            String modeLetter = battleArmor.getMovementMode().isVTOL() ? "V" : "J";
            movement.add("%d (%s)".formatted(jumpMP, modeLetter));
        }
        if (umuMP > 0) {
            movement.add("%d (U)".formatted(umuMP));
        }

        return new PlainElement(movement.toString());
    }
}
