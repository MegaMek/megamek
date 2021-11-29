/*
 * MegaMek - Copyright (C) 2000,2001,2002,2005 Ben Mazur (bmazur@sev.org)
 * UnitStatusFormatter.java - Copyright (C) 2002,2004 Joshua Yockey
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server;

import megamek.common.*;
import megamek.common.util.StringUtil;

public abstract class UnitStatusFormatter {
    /**
     * Much of the layout for the status string is heavily inspired by the
     * BattleTech MUSE/MUX code
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
        if ((e instanceof Mech) || (e instanceof Protomech)) {
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
        for (Mounted ammo : e.getAmmo()) {
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
            for (int y = 0; y < e.getNumberOfCriticals(x); y++) {
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
                    if (e instanceof Mech) {
                        sb.append(((Mech) e).getSystemName(cs.getIndex()));
                    } else if (e instanceof Protomech) {
                        sb.append(Protomech.systemNames[cs.getIndex()]);
                    }
                } else if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted m = cs.getMount();
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
        if (e instanceof Mech) {
            return formatArmorMech((Mech) e);
        } else if (e instanceof GunEmplacement) {
            return formatArmorGunEmplacement((GunEmplacement) e);
        } else if (e instanceof Tank) {
            return formatArmorTank((Tank) e);
        } else if (e instanceof BattleArmor) {
            return formatArmorBattleArmor((BattleArmor) e);
        } else if (e instanceof Infantry) {
            return formatArmorInfantry((Infantry) e);
        } else if (e instanceof Protomech) {
            return formatArmorProtomech((Protomech) e);
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

    private static String formatArmorMech(Mech m) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("         FRONT                REAR                INTERNAL\n");

        if (m.getWeight() < 70) {
            // head
            sb.append("         (")
                    .append(renderArmor(m.getArmor(Mech.LOC_HEAD)))
                    .append(")                 (**)                  (")
                    .append(renderArmor(m.getInternal(Mech.LOC_HEAD)))
                    .append(")\n");
            // torsos
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LT)))
                    .append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT))).append("\\           /");
            sb.append(renderArmor(m.getArmor(Mech.LOC_LT, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT, true))).append("\\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LT))).append("|");
            sb.append(renderArmor(m.getInternal(Mech.LOC_CT))).append("|");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RT))).append("\\\n");
            // arms
            sb.append("     (").append(renderArmor(m.getArmor(Mech.LOC_LARM)));
            sb.append("/ || \\").append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")         (   |  |   )          (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append("/ || \\");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")\n");
            // legs
            sb.append("       /  /\\  \\               /  \\                /  /\\  \\\n");
            sb.append("      (").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append("/  \\").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append(")             /    \\              (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append("/  \\")
                    .append(renderArmor(m.getInternal(Mech.LOC_RLEG)))
                    .append(")\n");
        } else {
            // head
            sb.append("      .../")
                    .append(renderArmor(m.getArmor(Mech.LOC_HEAD)))
                    .append("\\...           .../**\\...            .../");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD)))
                    .append("\\...\n");
            // torsos
            sb.append("     /").append(renderArmor(m.getArmor(Mech.LOC_LT)))
                    .append("| ");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT))).append(" |");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT))).append("\\         /");
            sb.append(renderArmor(m.getArmor(Mech.LOC_LT, true))).append("| ");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT, true))).append(" |");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT, true))).append("\\          /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LT))).append("| ");
            sb.append(renderArmor(m.getInternal(Mech.LOC_CT))).append(" |");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RT))).append("\\\n");
            // arms
            sb.append("    (").append(renderArmor(m.getArmor(Mech.LOC_LARM)));
            sb.append("). -- .(")
                    .append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")       (   |    |   )        (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append("). -- .(");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")\n");
            // legs
            sb.append("       /  /\\  \\             /      \\              /  /\\  \\\n");
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append(".\\/.").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append("\\           /        \\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append(".\\/.")
                    .append(renderArmor(m.getInternal(Mech.LOC_RLEG)))
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

    private static String formatArmorProtomech(Protomech m) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("         FRONT                INTERNAL\n");

        // head & main gun
        sb.append("        ");
        if (m.hasMainGun()) {
            sb.append(renderArmor(m.getArmor(Protomech.LOC_MAINGUN), 1));
        } else {
            sb.append(" ");
        }
        sb.append(" (").append(renderArmor(m.getArmor(Protomech.LOC_HEAD), 1))
                .append(")                  ");
        if (m.hasMainGun()) {
            sb.append(renderArmor(m.getInternal(Protomech.LOC_MAINGUN), 1));
        } else {
            sb.append(" ");
        }
        sb.append(" (");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_HEAD), 1))
                .append(")\n");
        if (m.hasMainGun()) {
            sb.append("         \\/ \\                   \\/ \\\n");
        } else {
            sb.append("          / \\                    / \\\n");
        }
        // arms & torso
        if (!m.isQuad()) {
            sb.append("      (").append(
                    renderArmor(m.getArmor(Protomech.LOC_LARM), 1));
            sb.append(" /")
                    .append(renderArmor(m.getArmor(Protomech.LOC_TORSO)))
                    .append(" \\")
                    .append(renderArmor(m.getArmor(Protomech.LOC_RARM)));
            sb.append(")            (");
            sb.append(renderArmor(m.getInternal(Protomech.LOC_LARM), 1))
                    .append(" /")
                    .append(renderArmor(m.getInternal(Protomech.LOC_TORSO)))
                    .append(" \\");
            sb.append(renderArmor(m.getInternal(Protomech.LOC_RARM)))
                    .append(")\n");
        }

        // legs
        sb.append("         | | |                  | | |\n");
        sb.append("        ( ").append(
                renderArmor(m.getArmor(Protomech.LOC_LEG)));
        sb.append("  )                ( ");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_LEG))).append("  )\n\n");
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
        MechSummary ms = MechSummaryCache.getInstance().getMech(ARGS[0]);
        Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName())
                .getEntity();
        System.out.println(format(e));
    }
}
