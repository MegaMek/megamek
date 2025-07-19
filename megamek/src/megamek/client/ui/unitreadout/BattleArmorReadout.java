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
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.Infantry;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponTypeFlag;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.LegAttack;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.SwarmAttack;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static megamek.client.ui.unitreadout.TableElement.JUSTIFIED_CENTER;
import static megamek.client.ui.unitreadout.TableElement.JUSTIFIED_LEFT;

class BattleArmorReadout extends GeneralEntityReadout {

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
                String[] row = createEquipmentTableRow(mounted);

                if (mounted.isDestroyed()) {
                    if (mounted.isRepairable()) {
                        wpnTable.addRowWithColor("yellow", row);
                    } else {
                        wpnTable.addRowWithColor("red", row);
                    }
                } else {
                    wpnTable.addRow(row);
                }
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
        if (otherAttacks.length()>0) {
            if (!result.isEmpty()) {
                result.add(new PlainLine());
            }
            result.add(new LabeledElement("Other Attacks", otherAttacks.toString()));
        }
        return result;
    }

    private String[] createEquipmentTableRow(Mounted<?> mounted) {
        String location = BattleArmor.getBaMountLocAbbr(mounted.getBaMountLoc());
        if (mounted.isDWPMounted()) {
            location = "DWP";
        }
        if (mounted.isAPMMounted()) {
            Mounted<?> apMount = mounted.getLinkedBy();
            if (apMount != null) {
                location = BattleArmor.getBaMountLocAbbr(apMount.getBaMountLoc());
            }
            location += " (APM)";
        }
        if (mounted.isSquadSupportWeapon()) {
            location = "SSWM";
        }

        String name = sanitizeMountedDesc(mounted) + quirkMarker(mounted);
        if (battleArmor.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.IS)) {
            name += Messages.getString("MekView.IS");
        }
        if (!battleArmor.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.CLAN)) {
            name += Messages.getString("MekView.Clan");
        }

        return new String[] { name, location };
    }

    private String sanitizeMountedDesc(Mounted<?> mounted) {
        String toRemove = " (%s)".formatted(BattleArmor.getBaMountLocAbbr(mounted.getBaMountLoc()));
        String name = mounted.getType().hasFlag(MiscType.F_BA_MANIPULATOR) ? mounted.getShortName() : mounted.getDesc();
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

            String[] row = { mounted.getName(), String.valueOf(mounted.getBaseShotsLeft()) };

            if (mounted.isDestroyed() || (mounted.getUsableShotsLeft() < 1)) {
                ammoTable.addRowWithColor("red", row);
            } else if (mounted.getUsableShotsLeft() < mounted.getOriginalShots()) {
                ammoTable.addRowWithColor("yellow", row);
            } else {
                ammoTable.addRow(row);
            }
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

            String[] row = createEquipmentTableRow(mounted);

            if (mounted.isDestroyed()) {
                miscTable.addRowWithColor("red", row);
            } else {
                miscTable.addRow(row);
            }
        }

        return miscTable;
    }

    @Override
    protected ViewElement createTotalInternalElement() {
        return new EmptyElement();
    }

    @Override
    protected String createMovementString() {
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

        return movement.toString();
    }
}
