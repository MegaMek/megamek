/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * UnitStatusFormatter.java - Copyright (C) 2002 Joshua Yockey
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

import megamek.common.*;
import java.util.*;

public abstract class UnitStatusFormatter
{
    /**
     * Much of the layout for the status string is heavily inspired by
     * the Battletech MUSE/MUX code
     */
    public static String format(Entity e)
    {
        StringBuffer sb = new StringBuffer(2048);
        sb.append("=============================================================\n");
        sb.append(formatHeader(e));
        sb.append("-------------------------------------------------------------\n");
        sb.append(formatArmor(e));
        if (e instanceof Mech) {
            sb.append("-------------------------------------------------------------\n");
            sb.append(formatCrits(e));
        }
        sb.append("-------------------------------------------------------------\n");
        sb.append(formatAmmo(e));
        sb.append("=============================================================\n");
        return sb.toString();
    }

    private static String formatHeader(Entity e)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("Model: ").append(e.getChassis()).append(" - ").append(e.getModel()).append("\n");
        sb.append("Pilot: ").append(e.crew.getName());
        sb.append(" (").append(e.crew.getGunnery()).append("/");
        sb.append(e.crew.getPiloting()).append(")\n");
        return sb.toString();
    }

    private static String formatAmmo(Entity e)
    {
        StringBuffer sb = new StringBuffer(1024);
        Mounted weap;

        for (Enumeration en = e.getAmmo(); en.hasMoreElements(); )
        {
            weap = (Mounted)en.nextElement();
            sb.append(weap.getName());
            sb.append(": ").append(weap.getShotsLeft()).append("\n");
        }

        return sb.toString();
    }
      

    private static String formatCrits(Entity e)
    {
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < e.locations(); x++) {
            sb.append(makeLength(e.getLocationName(x), 12)).append(": ");
            int nCount = 0;
            for (int y = 0; y < e.getNumberOfCriticals(x); y++) {
                CriticalSlot cs = e.getCritical(x, y);
                if (cs == null) continue;
                nCount++;
                if (nCount == 7) {
                    sb.append("\n              ");
                }
                else if (nCount > 1) {
                    sb.append(",");
                }

                if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                    if (cs.isHit() || cs.isDestroyed() || cs.isMissing()) {
                        sb.append("*");
                    }
                    if (e instanceof Mech) {
                        sb.append(((Mech)e).systemNames[cs.getIndex()]);
                    }
                }
                else if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted m = e.getEquipment(cs.getIndex());
                    if (m.isHit()) {
                        sb.append("*");
                    }
                    sb.append(m.getName());
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    private static String formatArmor(Entity e)
    {
        if (e instanceof Mech) {
            return formatArmorMech((Mech)e);
        } else if (e instanceof Tank) {
            return formatArmorTank((Tank)e);
        } else if (e instanceof BattleArmor) {
            return formatArmorBattleArmor((BattleArmor)e);
        } else if (e instanceof Infantry) {
            return formatArmorInfantry((Infantry)e);
        }
        return "";
    }
    
    private static String formatArmorTank(Tank t)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("      ARMOR               INTERNAL\n    __________           __________\n    |\\      /|           |\\      /|\n");
        // front
        sb.append("    | \\ ").append(renderArmor(t.getArmor(Tank.LOC_FRONT))).append(" / |           | \\ ");
        sb.append(renderArmor(t.getInternal(Tank.LOC_FRONT))).append(" / |\n    |  \\__/  |           |  \\__/  |\n");
        // left, turret and right
        sb.append("    |").append(renderArmor(t.getArmor(Tank.LOC_LEFT))).append("/");
        if (t.hasTurret())
        {
            sb.append(renderArmor(t.getArmor(Tank.LOC_TURRET))).append("\\");
        } else {
            sb.append("  \\");
        }
        sb.append(renderArmor(t.getArmor(Tank.LOC_RIGHT))).append("|           |");
        sb.append(renderArmor(t.getInternal(Tank.LOC_LEFT))).append("/");
        if (t.hasTurret())
        {
            sb.append(renderArmor(t.getInternal(Tank.LOC_TURRET))).append("\\");
        } else {
            sb.append("  \\");
        }    
        sb.append(renderArmor(t.getInternal(Tank.LOC_RIGHT))).append("|\n");
        // rear
        sb.append("    | /____\\ |           | /____\\ |\n    | / ").append(renderArmor(t.getArmor(Tank.LOC_REAR))).append(" \\ |           | / ");
        sb.append(renderArmor(t.getInternal(Tank.LOC_REAR))).append(" \\ |\n    |/______\\|           |/______\\|\n");

        sb.append("\n");
        return sb.toString();
    }

    private static String formatArmorMech(Mech m)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("         FRONT                REAR                INTERNAL\n");
        if (m.getWeight() < 70) {
            // head
            sb.append("         (").append(renderArmor(m.getArmor(Mech.LOC_HEAD))).append(")                 (**)                  (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD))).append(")\n");
            // torsos
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LT))).append("|");
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
            sb.append("/  \\").append(renderArmor(m.getInternal(Mech.LOC_RLEG))).append(")\n");
        }
        else {
            // head
            sb.append("      .../").append(renderArmor(m.getArmor(Mech.LOC_HEAD))).append("\\...           .../**\\...            .../");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD))).append("\\...\n");
            // torsos
            sb.append("     /").append(renderArmor(m.getArmor(Mech.LOC_LT))).append("| ");
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
            sb.append("). -- .(").append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")       (   |    |   )        (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append("). -- .(");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")\n");
            // legs
            sb.append("       /  /\\  \\             /      \\              /  /\\  \\\n");
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append(".\\/.").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append("\\           /        \\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append(".\\/.").append(renderArmor(m.getInternal(Mech.LOC_RLEG))).append("\\\n");
        }
        sb.append("\n");
        return sb.toString();
    }
    
    private static String formatArmorInfantry(Infantry i) {
        StringBuffer sb = new StringBuffer(32);
        sb.append("Surviving troopers: ")
            .append( renderArmor(i.getInternal(0)) )
            .append('\n');
        return sb.toString();
    }
    
    private static String formatArmorBattleArmor(BattleArmor b) {
        StringBuffer sb = new StringBuffer(32);
        for (int i = 1; i < b.locations(); i++) {
            sb.append("Trooper ").append(i).append(": ")
                .append( renderArmor(b.getArmor(i)) )
                .append(" / ")
                .append( renderArmor(b.getInternal(i)) );
            sb.append('\n');
        }
        return sb.toString();
    }
    
    private static String renderArmor(int nArmor)
    {
        if (nArmor <= 0) {
            return "xx";
        }
        else {
            return makeLength(String.valueOf(nArmor), 2, true);
        }
    }
    
    private static final String SPACES = "                                   ";
    private static String makeLength(String s, int n) {
        return makeLength(s, n, false);
    }
    
    private static String makeLength(String s, int n, boolean bRightJustify)
    {
        int l = s.length();
        if (l == n) {
            return s;
        }
        else if (l < n) {
            if (bRightJustify) {
                return SPACES.substring(0, n - l) + s;
            }
            else {
                return s + SPACES.substring(0, n - l);
            }
        }
        else {
            return s.substring(0, n - 2) + "..";
        }
    }
    
    public static void main(String[] ARGS)
        throws Exception
    {
        MechSummary ms = MechSummaryCache.getInstance().getMech(ARGS[0]);
        Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        System.out.println(format(e));
    }
}
