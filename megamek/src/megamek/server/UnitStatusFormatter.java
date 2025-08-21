/*
  Copyright (C) 2000, 2001, 2002, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002, 2004 Joshua Yockey
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

package megamek.server;

import megamek.common.CriticalSlot;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.util.StringUtil;

public abstract class UnitStatusFormatter {
    /**
     * Much of the layout for the status string is heavily inspired by the BT MUSE/MUX code
     */
    public static String format(Entity e) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("=============================================================\n")
              .append(formatHeader(e))
              .append("--- Armor: ").append(e.getTotalArmor()).append("/")
              .append(e.getTotalOArmor())
              .append("-------------------------------------------\n")
              .append("--- Internal: ").append(e.getTotalInternal()).append("/")
              .append(e.getTotalOInternal())
              .append("----------------------------------------\n")
              .append(formatArmor(e));
        if ((e instanceof Mek) || (e instanceof ProtoMek)) {
            sb.append("-------------------------------------------------------------\n")
                  .append(formatCrits(e));
        }

        return sb.append("-------------------------------------------------------------\n")
              .append(formatAmmo(e))
              .append("=============================================================\n")
              .toString();
    }

    private static String formatHeader(Entity e) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Model: ").append(e.getChassis()).append(" - ")
              .append(e.getModel()).append("\n");
        for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
            if (e.getCrew().isMissing(i)) {
                sb.append("No ").append(e.getCrew().getCrewType().getRoleName(i));
            } else {
                sb.append(e.getCrew().getCrewType().getRoleName(i)).append(": ")
                      .append(e.getCrew().getName(i));
                sb.append(" (").append(e.getCrew().getGunnery(i)).append("/")
                      .append(e.getCrew().getPiloting(i)).append(")");
            }
            sb.append("\n");
        }

        if (e.isCaptured()) {
            sb.append("  *** CAPTURED BY THE ENEMY ***\n");
        }

        return sb.toString();
    }

    private static String formatAmmo(Entity e) {
        StringBuilder sb = new StringBuilder(1024);
        for (Mounted<?> ammo : e.getAmmo()) {
            sb.append(ammo.getName());
            sb.append(": ").append(ammo.getBaseShotsLeft())
                  .append("\n");
        }
        return sb.toString();
    }

    private static String formatCrits(Entity e) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < e.locations(); x++) {
            sb.append(StringUtil.makeLength(e.getLocationName(x), 12)).append(": ");
            int nCount = 0;
            for (int y = 0; y < e.getNumberOfCriticalSlots(x); y++) {
                CriticalSlot cs = e.getCritical(x, y);
                if (cs == null) {
                    continue;
                }
                nCount++;
                if (nCount == 7) {
                    sb.append("\n              ");
                } else if (nCount > 1) {
                    sb.append(",");
                }
                if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                    if (cs.isHit() || cs.isDestroyed() || cs.isMissing()) {
                        sb.append("*");
                    }
                    if (e instanceof Mek) {
                        sb.append(((Mek) e).getSystemName(cs.getIndex()));
                    } else if (e instanceof ProtoMek) {
                        sb.append(ProtoMek.SYSTEM_NAMES[cs.getIndex()]);
                    }
                } else if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted<?> m = cs.getMount();
                    sb.append(cs.isHit() ? "*" : "")
                          .append(cs.isDestroyed() ? "*" : "")
                          .append(cs.isBreached() ? "x" : "")
                          .append(m.getDesc());
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String formatArmor(Entity e) {
        if (e instanceof Mek) {
            return formatArmorMek((Mek) e);
        } else if (e instanceof GunEmplacement) {
            return formatArmorGunEmplacement((GunEmplacement) e);
        } else if (e instanceof Tank) {
            return formatArmorTank((Tank) e);
        } else if (e instanceof BattleArmor) {
            return formatArmorBattleArmor((BattleArmor) e);
        } else if (e instanceof Infantry) {
            return formatArmorInfantry((Infantry) e);
        } else if (e instanceof ProtoMek) {
            return formatArmorProtoMek((ProtoMek) e);
        } else {
            return "";
        }
    }

    private static String formatArmorTank(Tank t) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("      ARMOR               INTERNAL\n")
              .append("    __________           __________\n")
              .append("    |\\      /|           |\\      /|\n");
        // front
        sb.append("    | \\ ").append(renderArmor(t.getArmor(Tank.LOC_FRONT)))
              .append(" / |           | \\ ")
              .append(renderArmor(t.getInternal(Tank.LOC_FRONT))).append(" / |\n")
              .append("    |  \\__/  |           |  \\__/  |\n");
        // left, turret and right
        sb.append("    |").append(renderArmor(t.getArmor(Tank.LOC_LEFT)))
              .append("/");
        if (!t.hasNoTurret()) {
            sb.append(renderArmor(t.getArmor(t.getLocTurret()))).append("\\");
        } else {
            sb.append("  \\");
        }
        sb.append(renderArmor(t.getArmor(Tank.LOC_RIGHT))).append(
              "|           |");
        sb.append(renderArmor(t.getInternal(Tank.LOC_LEFT))).append("/");
        if (t.hasNoTurret()) {
            sb.append(renderArmor(t.getInternal(t.getLocTurret())))
                  .append("\\");
        } else {
            sb.append("  \\");
        }
        sb.append(renderArmor(t.getInternal(Tank.LOC_RIGHT))).append("|\n");
        // rear
        sb.append("    | /____\\ |           | /____\\ |\n")
              .append("    | / ")
              .append(renderArmor(t.getArmor(Tank.LOC_REAR)))
              .append(" \\ |           | / ")
              .append(renderArmor(t.getInternal(Tank.LOC_REAR)))
              .append(" \\ |\n")
              .append("    |/______\\|           |/______\\|\n\n");

        return sb.toString();
    }

    private static String formatArmorMek(Mek m) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("         FRONT                REAR                INTERNAL\n");

        if (m.getWeight() < 70) {
            // head
            sb.append("         (")
                  .append(renderArmor(m.getArmor(Mek.LOC_HEAD)))
                  .append(")                 (**)                  (")
                  .append(renderArmor(m.getInternal(Mek.LOC_HEAD)))
                  .append(")\n");
            // torsos
            sb.append("      /").append(renderArmor(m.getArmor(Mek.LOC_LEFT_TORSO)))
                  .append("|");
            sb.append(renderArmor(m.getArmor(Mek.LOC_CENTER_TORSO))).append("|");
            sb.append(renderArmor(m.getArmor(Mek.LOC_RIGHT_TORSO))).append("\\           /");
            sb.append(renderArmor(m.getArmor(Mek.LOC_LEFT_TORSO, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mek.LOC_CENTER_TORSO, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mek.LOC_RIGHT_TORSO, true))).append("\\            /");
            sb.append(renderArmor(m.getInternal(Mek.LOC_LEFT_TORSO))).append("|");
            sb.append(renderArmor(m.getInternal(Mek.LOC_CENTER_TORSO))).append("|");
            sb.append(renderArmor(m.getInternal(Mek.LOC_RIGHT_TORSO))).append("\\\n");
            // arms
            sb.append("     (").append(renderArmor(m.getArmor(Mek.LOC_LEFT_ARM)));
            sb.append("/ || \\").append(renderArmor(m.getArmor(Mek.LOC_RIGHT_ARM)));
            sb.append(")         (   |  |   )          (");
            sb.append(renderArmor(m.getInternal(Mek.LOC_LEFT_ARM))).append("/ || \\");
            sb.append(renderArmor(m.getInternal(Mek.LOC_RIGHT_ARM))).append(")\n");
            // legs
            sb.append("       /  /\\  \\               /  \\                /  /\\  \\\n");
            sb.append("      (").append(renderArmor(m.getArmor(Mek.LOC_LEFT_LEG)));
            sb.append("/  \\").append(renderArmor(m.getArmor(Mek.LOC_RIGHT_LEG)));
            sb.append(")             /    \\              (");
            sb.append(renderArmor(m.getInternal(Mek.LOC_LEFT_LEG)));
            sb.append("/  \\")
                  .append(renderArmor(m.getInternal(Mek.LOC_RIGHT_LEG)))
                  .append(")\n");
        } else {
            // head
            sb.append("      .../")
                  .append(renderArmor(m.getArmor(Mek.LOC_HEAD)))
                  .append("\\...           .../**\\...            .../");
            sb.append(renderArmor(m.getInternal(Mek.LOC_HEAD)))
                  .append("\\...\n");
            // torsos
            sb.append("     /").append(renderArmor(m.getArmor(Mek.LOC_LEFT_TORSO)))
                  .append("| ");
            sb.append(renderArmor(m.getArmor(Mek.LOC_CENTER_TORSO))).append(" |");
            sb.append(renderArmor(m.getArmor(Mek.LOC_RIGHT_TORSO))).append("\\         /");
            sb.append(renderArmor(m.getArmor(Mek.LOC_LEFT_TORSO, true))).append("| ");
            sb.append(renderArmor(m.getArmor(Mek.LOC_CENTER_TORSO, true))).append(" |");
            sb.append(renderArmor(m.getArmor(Mek.LOC_RIGHT_TORSO, true))).append("\\          /");
            sb.append(renderArmor(m.getInternal(Mek.LOC_LEFT_TORSO))).append("| ");
            sb.append(renderArmor(m.getInternal(Mek.LOC_CENTER_TORSO))).append(" |");
            sb.append(renderArmor(m.getInternal(Mek.LOC_RIGHT_TORSO))).append("\\\n");
            // arms
            sb.append("    (").append(renderArmor(m.getArmor(Mek.LOC_LEFT_ARM)));
            sb.append("). -- .(")
                  .append(renderArmor(m.getArmor(Mek.LOC_RIGHT_ARM)));
            sb.append(")       (   |    |   )        (");
            sb.append(renderArmor(m.getInternal(Mek.LOC_LEFT_ARM))).append("). -- .(");
            sb.append(renderArmor(m.getInternal(Mek.LOC_RIGHT_ARM))).append(")\n");
            // legs
            sb.append("       /  /\\  \\             /      \\              /  /\\  \\\n");
            sb.append("      /").append(renderArmor(m.getArmor(Mek.LOC_LEFT_LEG)));
            sb.append(".\\/.").append(renderArmor(m.getArmor(Mek.LOC_RIGHT_LEG)));
            sb.append("\\           /        \\            /");
            sb.append(renderArmor(m.getInternal(Mek.LOC_LEFT_LEG)));
            sb.append(".\\/.")
                  .append(renderArmor(m.getInternal(Mek.LOC_RIGHT_LEG)))
                  .append("\\\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String formatArmorInfantry(Infantry i) {
        return "Surviving troopers: " + renderArmor(i.getInternal(0)) + "\n";
    }

    private static String formatArmorBattleArmor(BattleArmor b) {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 1; i < b.locations(); i++) {
            sb.append("Trooper ").append(i).append(": ")
                  .append(renderArmor(b.getArmor(i))).append(" / ")
                  .append(renderArmor(b.getInternal(i)))
                  .append("\n");
        }
        return sb.toString();
    }

    private static String formatArmorProtoMek(ProtoMek m) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("         FRONT                INTERNAL\n");

        // head & main gun
        sb.append("        ");
        if (m.hasMainGun()) {
            sb.append(renderArmor(m.getArmor(ProtoMek.LOC_MAIN_GUN), 1));
        } else {
            sb.append(" ");
        }
        sb.append(" (").append(renderArmor(m.getArmor(ProtoMek.LOC_HEAD), 1))
              .append(")                  ");
        if (m.hasMainGun()) {
            sb.append(renderArmor(m.getInternal(ProtoMek.LOC_MAIN_GUN), 1));
        } else {
            sb.append(" ");
        }
        sb.append(" (");
        sb.append(renderArmor(m.getInternal(ProtoMek.LOC_HEAD), 1))
              .append(")\n");
        if (m.hasMainGun()) {
            sb.append("         \\/ \\                   \\/ \\\n");
        } else {
            sb.append("          / \\                    / \\\n");
        }
        // arms & torso
        if (!m.isQuad()) {
            sb.append("      (").append(
                  renderArmor(m.getArmor(ProtoMek.LOC_LEFT_ARM), 1));
            sb.append(" /")
                  .append(renderArmor(m.getArmor(ProtoMek.LOC_TORSO)))
                  .append(" \\")
                  .append(renderArmor(m.getArmor(ProtoMek.LOC_RIGHT_ARM)));
            sb.append(")            (");
            sb.append(renderArmor(m.getInternal(ProtoMek.LOC_LEFT_ARM), 1))
                  .append(" /")
                  .append(renderArmor(m.getInternal(ProtoMek.LOC_TORSO)))
                  .append(" \\");
            sb.append(renderArmor(m.getInternal(ProtoMek.LOC_RIGHT_ARM)))
                  .append(")\n");
        }

        // legs
        sb.append("         | | |                  | | |\n");
        sb.append("        ( ").append(
              renderArmor(m.getArmor(ProtoMek.LOC_LEG)));
        sb.append("  )                ( ");
        sb.append(renderArmor(m.getInternal(ProtoMek.LOC_LEG))).append("  )\n\n");
        return sb.toString();
    }

    private static String formatArmorGunEmplacement(GunEmplacement ge) {
        return "            ----------\n" +
              "           |          |\n" +
              "  CF       |    " +
              renderArmor(ge.getArmor(GunEmplacement.LOC_GUNS)) +
              "    |\n" +
              "           |          |\n" +
              "         -----------------\n";
    }

    private static String renderArmor(int nArmor) {
        return renderArmor(nArmor, 2);
    }

    private static String renderArmor(int nArmor, int spaces) {
        if (nArmor <= 0) {
            if (1 == spaces) {
                return "x";
            }
            return "xx";
        }
        return StringUtil.makeLength(String.valueOf(nArmor), spaces, true);
    }

    public static void main(String[] ARGS) throws Exception {
        MekSummary ms = MekSummaryCache.getInstance().getMek(ARGS[0]);
        Entity e = new MekFileParser(ms.getSourceFile(), ms.getEntryName())
              .getEntity();
        System.out.println(format(e));
    }
}
