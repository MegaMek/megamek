/**
 * MegaMek - Copyright (C) 2000,2001,2002,2005 Ben Mazur (bmazur@sev.org)
 * UnitStatusFormatter.java - Copyright (C) 2002,2004 Joshua Yockey
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.server;

import megamek.common.BattleArmor;
import megamek.common.CommonConstants;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.util.StringUtil;

public abstract class UnitStatusFormatter {
    /**
     * Much of the layout for the status string is heavily inspired by the
     * Battletech MUSE/MUX code
     */
    public static String format(Entity e) {
        StringBuffer sb = new StringBuffer(2048);
        sb
                .append(
                        "=============================================================")
                .append(CommonConstants.NL);
        sb.append(formatHeader(e));
        sb.append("--- Armor: ").append(e.getTotalArmor()).append("/").append(
                e.getTotalOArmor()).append(
                "-------------------------------------------").append(
                CommonConstants.NL);
        sb.append("--- Internal: ").append(e.getTotalInternal()).append("/")
                .append(e.getTotalOInternal()).append(
                        "----------------------------------------").append(
                        CommonConstants.NL);
        sb.append(formatArmor(e));
        if (e instanceof Mech || e instanceof Protomech) {
            sb
                    .append(
                            "-------------------------------------------------------------")
                    .append(CommonConstants.NL);
            sb.append(formatCrits(e));
        }
        sb
                .append(
                        "-------------------------------------------------------------")
                .append(CommonConstants.NL);
        sb.append(formatAmmo(e));
        sb
                .append(
                        "=============================================================")
                .append(CommonConstants.NL);
        return sb.toString();
    }

    private static String formatHeader(Entity e) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("Model: ").append(e.getChassis()).append(" - ").append(
                e.getModel()).append(CommonConstants.NL);
        sb.append("Pilot: ").append(e.crew.getName());
        sb.append(" (").append(e.crew.getGunnery()).append("/");
        sb.append(e.crew.getPiloting()).append(")").append(CommonConstants.NL);
        if (e.isCaptured()) {
            sb.append("  *** CAPTURED BY THE ENEMY ***");
            sb.append(CommonConstants.NL);
        }
        return sb.toString();
    }

    private static String formatAmmo(Entity e) {
        StringBuffer sb = new StringBuffer(1024);
        for (Mounted ammo : e.getAmmo()) {
            sb.append(ammo.getName());
            sb.append(": ").append(ammo.getShotsLeft()).append(
                    CommonConstants.NL);
        }
        return sb.toString();
    }

    private static String formatCrits(Entity e) {
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < e.locations(); x++) {
            sb.append(StringUtil.makeLength(e.getLocationName(x), 12)).append(
                    ": ");
            int nCount = 0;
            for (int y = 0; y < e.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = e.getCritical(x, y);
                if (cs == null)
                    continue;
                nCount++;
                if (nCount == 7) {
                    sb.append(CommonConstants.NL);
                    sb.append("              ");
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
                    Mounted m = e.getEquipment(cs.getIndex());
                    sb
                            .append(cs.isHit() ? "*" : "").append(cs.isDestroyed() ? "*" : "").append(cs.isBreached() ? "x" : "").append(m.getDesc()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                }
            }
            sb.append(CommonConstants.NL);
        }
        return sb.toString();
    }

    private static String formatArmor(Entity e) {
        if (e instanceof Mech) {
            return formatArmorMech((Mech) e);
        } else if (e instanceof Tank) {
            return formatArmorTank((Tank) e);
        } else if (e instanceof BattleArmor) {
            return formatArmorBattleArmor((BattleArmor) e);
        } else if (e instanceof Infantry) {
            return formatArmorInfantry((Infantry) e);
        } else if (e instanceof Protomech) {
            return formatArmorProtomech((Protomech) e);
        } else if (e instanceof GunEmplacement) {
            return formatArmorGunEmplacement((GunEmplacement) e);
        }
        return "";
    }

    private static String formatArmorTank(Tank t) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("      ARMOR               INTERNAL").append(
                CommonConstants.NL).append(
                "    __________           __________").append(
                CommonConstants.NL).append(
                "    |\\      /|           |\\      /|").append(
                CommonConstants.NL);
        // front
        sb.append("    | \\ ").append(renderArmor(t.getArmor(Tank.LOC_FRONT)))
                .append(" / |           | \\ ");
        sb.append(renderArmor(t.getInternal(Tank.LOC_FRONT))).append(" / |")
                .append(CommonConstants.NL).append(
                        "    |  \\__/  |           |  \\__/  |").append(
                        CommonConstants.NL);
        // left, turret and right
        sb.append("    |").append(renderArmor(t.getArmor(Tank.LOC_LEFT)))
                .append("/");
        if (!t.hasNoTurret()) {
            sb.append(renderArmor(t.getArmor(Tank.LOC_TURRET))).append("\\");
        } else {
            sb.append("  \\");
        }
        sb.append(renderArmor(t.getArmor(Tank.LOC_RIGHT))).append(
                "|           |");
        sb.append(renderArmor(t.getInternal(Tank.LOC_LEFT))).append("/");
        if (t.hasNoTurret()) {
            sb.append(renderArmor(t.getInternal(Tank.LOC_TURRET))).append("\\");
        } else {
            sb.append("  \\");
        }
        sb.append(renderArmor(t.getInternal(Tank.LOC_RIGHT))).append("|")
                .append(CommonConstants.NL);
        // rear
        sb.append("    | /____\\ |           | /____\\ |").append(
                CommonConstants.NL).append("    | / ").append(
                renderArmor(t.getArmor(Tank.LOC_REAR))).append(
                " \\ |           | / ");
        sb.append(renderArmor(t.getInternal(Tank.LOC_REAR))).append(" \\ |")
                .append(CommonConstants.NL).append(
                        "    |/______\\|           |/______\\|").append(
                        CommonConstants.NL);

        sb.append(CommonConstants.NL);
        return sb.toString();
    }

    private static String formatArmorMech(Mech m) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("         FRONT                REAR                INTERNAL");
        sb.append(CommonConstants.NL);
        if (m.getWeight() < 70) {
            // head
            sb.append("         (").append(
                    renderArmor(m.getArmor(Mech.LOC_HEAD))).append(
                    ")                 (**)                  (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD))).append(")");
            sb.append(CommonConstants.NL);
            // torsos
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LT)))
                    .append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT))).append(
                    "\\           /");
            sb.append(renderArmor(m.getArmor(Mech.LOC_LT, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT, true))).append(
                    "\\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LT))).append("|");
            sb.append(renderArmor(m.getInternal(Mech.LOC_CT))).append("|");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RT))).append("\\");
            sb.append(CommonConstants.NL);
            // arms
            sb.append("     (").append(renderArmor(m.getArmor(Mech.LOC_LARM)));
            sb.append("/ || \\").append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")         (   |  |   )          (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append(
                    "/ || \\");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")");
            sb.append(CommonConstants.NL);
            // legs
            sb
                    .append("       /  /\\  \\               /  \\                /  /\\  \\");
            sb.append(CommonConstants.NL);
            sb.append("      (").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append("/  \\").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append(")             /    \\              (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append("/  \\")
                    .append(renderArmor(m.getInternal(Mech.LOC_RLEG))).append(
                            ")");
            sb.append(CommonConstants.NL);
        } else {
            // head
            sb.append("      .../").append(
                    renderArmor(m.getArmor(Mech.LOC_HEAD))).append(
                    "\\...           .../**\\...            .../");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD)))
                    .append("\\...");
            sb.append(CommonConstants.NL);
            // torsos
            sb.append("     /").append(renderArmor(m.getArmor(Mech.LOC_LT)))
                    .append("| ");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT))).append(" |");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT))).append(
                    "\\         /");
            sb.append(renderArmor(m.getArmor(Mech.LOC_LT, true))).append("| ");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT, true))).append(" |");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT, true))).append(
                    "\\          /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LT))).append("| ");
            sb.append(renderArmor(m.getInternal(Mech.LOC_CT))).append(" |");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RT))).append("\\");
            sb.append(CommonConstants.NL);
            // arms
            sb.append("    (").append(renderArmor(m.getArmor(Mech.LOC_LARM)));
            sb.append("). -- .(")
                    .append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")       (   |    |   )        (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append(
                    "). -- .(");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")");
            sb.append(CommonConstants.NL);
            // legs
            sb
                    .append("       /  /\\  \\             /      \\              /  /\\  \\");
            sb.append(CommonConstants.NL);
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append(".\\/.").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append("\\           /        \\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append(".\\/.")
                    .append(renderArmor(m.getInternal(Mech.LOC_RLEG))).append(
                            "\\");
            sb.append(CommonConstants.NL);
        }
        sb.append(CommonConstants.NL);
        return sb.toString();
    }

    private static String formatArmorInfantry(Infantry i) {
        StringBuffer sb = new StringBuffer(32);
        sb.append("Surviving troopers: ").append(renderArmor(i.getInternal(0)))
                .append(CommonConstants.NL);
        return sb.toString();
    }

    private static String formatArmorBattleArmor(BattleArmor b) {
        StringBuffer sb = new StringBuffer(32);
        for (int i = 1; i < b.locations(); i++) {
            sb.append("Trooper ").append(i).append(": ").append(
                    renderArmor(b.getArmor(i))).append(" / ").append(
                    renderArmor(b.getInternal(i)));
            sb.append(CommonConstants.NL);
        }
        return sb.toString();
    }

    private static String formatArmorProtomech(Protomech m) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("         FRONT                INTERNAL");
        sb.append(CommonConstants.NL);

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
                .append(")");
        sb.append(CommonConstants.NL);
        if (m.hasMainGun()) {
            sb.append("         \\/ \\                   \\/ \\");
            sb.append(CommonConstants.NL);
        } else {
            sb.append("          / \\                    / \\");
            sb.append(CommonConstants.NL);
        }
        // arms & torso
        sb.append("      (").append(
                renderArmor(m.getArmor(Protomech.LOC_LARM), 1));
        sb.append(" /").append(renderArmor(m.getArmor(Protomech.LOC_TORSO)))
                .append(" \\").append(
                        renderArmor(m.getArmor(Protomech.LOC_RARM)));
        sb.append(")            (");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_LARM), 1)).append(
                " /").append(renderArmor(m.getInternal(Protomech.LOC_TORSO)))
                .append(" \\");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_RARM))).append(")");
        sb.append(CommonConstants.NL);
        // legs
        sb.append("         | | |                  | | |");
        sb.append(CommonConstants.NL);
        sb.append("        ( ").append(
                renderArmor(m.getArmor(Protomech.LOC_LEG)));
        sb.append("  )                ( ");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_LEG))).append("  )");
        sb.append(CommonConstants.NL);
        sb.append("");
        sb.append(CommonConstants.NL);
        return sb.toString();
    }

    private static String formatArmorGunEmplacement(GunEmplacement ge) {
        StringBuffer sb = new StringBuffer(1024);
        if (ge.hasTurret()) {
            sb
                    .append("             --------")
                    .append(CommonConstants.NL)
                    .append(" TURRET     /   ")
                    .append(renderArmor(ge.getArmor(GunEmplacement.LOC_TURRET)))
                    .append("   \\").append(CommonConstants.NL);
        }
        sb.append("            ----------").append(CommonConstants.NL).append(
                "           |          |").append(CommonConstants.NL).append(
                "  CF       |    ").append(
                renderArmor(ge.getArmor(GunEmplacement.LOC_TURRET))).append(
                "    |").append(CommonConstants.NL).append(
                "           |          |").append(CommonConstants.NL).append(
                "         -----------------").append(CommonConstants.NL);
        return sb.toString();
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
