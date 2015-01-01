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
        sb.append("=============================================================")
            .append( Settings.NL );
        sb.append(formatHeader(e));
        sb.append("--- Armor: ")
            .append(e.getTotalArmor())
            .append("/")
            .append(e.getTotalOArmor())
            .append("-------------------------------------------")
            .append( Settings.NL );
        sb.append("--- Internal: ")
            .append(e.getTotalInternal())
            .append("/")
            .append(e.getTotalOInternal())
            .append("----------------------------------------")
            .append( Settings.NL );
        sb.append(formatArmor(e));
        if ( e instanceof Mech ||
             e instanceof Protomech ) {
            sb.append("-------------------------------------------------------------")
                .append( Settings.NL );
            sb.append(formatCrits(e));
        }
        sb.append("-------------------------------------------------------------")
            .append( Settings.NL );
        sb.append(formatAmmo(e));
        sb.append("=============================================================")
            .append( Settings.NL );
        return sb.toString();
    }

    private static String formatHeader(Entity e)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("Model: ")
            .append(e.getChassis())
            .append(" - ")
            .append(e.getModel())
            .append( Settings.NL );
        sb.append("Pilot: ")
            .append(e.crew.getName());
        sb.append(" (")
            .append(e.crew.getGunnery())
            .append("/");
        sb.append(e.crew.getPiloting())
            .append(")")
            .append( Settings.NL );
        if (e.isCaptured()) {
            sb.append( "  *** CAPTURED BY THE ENEMY ***" );
            sb.append( Settings.NL );
        }
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
                sb.append(": ")
                    .append(weap.getShotsLeft())
                    .append( Settings.NL );
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
                    sb.append( Settings.NL );
                    sb.append("              ");
                }
                else if (nCount > 1) {
                    sb.append(",");
                }

                if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                    if (cs.isHit() || cs.isDestroyed() || cs.isMissing()) {
                        sb.append("*");
                    }
                    if (e instanceof Mech) {
                        sb.append( Mech.systemNames[cs.getIndex()] );
                    }
                    else if ( e instanceof Protomech ) {
                        sb.append( Protomech.systemNames[cs.getIndex()] );
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
            sb.append( Settings.NL );
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
        } else if (e instanceof Protomech) {
            return formatArmorProtomech((Protomech)e);
        }
        return "";
    }
    
    private static String formatArmorTank(Tank t)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("      ARMOR               INTERNAL")
            .append( Settings.NL )
            .append("    __________           __________")
            .append( Settings.NL )
            .append("    |\\      /|           |\\      /|")
            .append( Settings.NL );
        // front
        sb.append("    | \\ ")
            .append(renderArmor(t.getArmor(Tank.LOC_FRONT)))
            .append(" / |           | \\ ");
        sb.append(renderArmor(t.getInternal(Tank.LOC_FRONT)))
            .append(" / |")
            .append( Settings.NL )
            .append("    |  \\__/  |           |  \\__/  |")
            .append( Settings.NL );
        // left, turret and right
        sb.append("    |")
            .append(renderArmor(t.getArmor(Tank.LOC_LEFT)))
            .append("/");
        if (!t.hasNoTurret())
            {
                sb.append(renderArmor(t.getArmor(Tank.LOC_TURRET)))
                    .append("\\");
            } else {
                sb.append("  \\");
            }
        sb.append(renderArmor(t.getArmor(Tank.LOC_RIGHT)))
            .append("|           |");
        sb.append(renderArmor(t.getInternal(Tank.LOC_LEFT)))
            .append("/");
        if (t.hasNoTurret())
            {
                sb.append(renderArmor(t.getInternal(Tank.LOC_TURRET)))
                    .append("\\");
            } else {
                sb.append("  \\");
            }    
        sb.append(renderArmor(t.getInternal(Tank.LOC_RIGHT)))
            .append("|")
            .append( Settings.NL );
        // rear
        sb.append("    | /____\\ |           | /____\\ |")
            .append( Settings.NL )
            .append("    | / ")
            .append(renderArmor(t.getArmor(Tank.LOC_REAR)))
            .append(" \\ |           | / ");
        sb.append(renderArmor(t.getInternal(Tank.LOC_REAR)))
            .append(" \\ |")
            .append( Settings.NL )
            .append("    |/______\\|           |/______\\|")
            .append( Settings.NL );

        sb.append( Settings.NL );
        return sb.toString();
    }

    private static String formatArmorMech(Mech m)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("         FRONT                REAR                INTERNAL");
        sb.append( Settings.NL );
        if (m.getWeight() < 70) {
            // head
            sb.append("         (").append(renderArmor(m.getArmor(Mech.LOC_HEAD))).append(")                 (**)                  (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD))).append(")");
            sb.append( Settings.NL );
            // torsos
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LT))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT))).append("\\           /");
            sb.append(renderArmor(m.getArmor(Mech.LOC_LT, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT, true))).append("|");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT, true))).append("\\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LT))).append("|");
            sb.append(renderArmor(m.getInternal(Mech.LOC_CT))).append("|");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RT))).append("\\");
            sb.append( Settings.NL );
            // arms
            sb.append("     (").append(renderArmor(m.getArmor(Mech.LOC_LARM)));
            sb.append("/ || \\").append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")         (   |  |   )          (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append("/ || \\");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")");
            sb.append( Settings.NL );
            // legs
            sb.append("       /  /\\  \\               /  \\                /  /\\  \\");
            sb.append( Settings.NL );
            sb.append("      (").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append("/  \\").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append(")             /    \\              (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append("/  \\").append(renderArmor(m.getInternal(Mech.LOC_RLEG))).append(")");
            sb.append( Settings.NL );
        }
        else {
            // head
            sb.append("      .../").append(renderArmor(m.getArmor(Mech.LOC_HEAD))).append("\\...           .../**\\...            .../");
            sb.append(renderArmor(m.getInternal(Mech.LOC_HEAD))).append("\\...");
            sb.append( Settings.NL );
            // torsos
            sb.append("     /").append(renderArmor(m.getArmor(Mech.LOC_LT))).append("| ");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT))).append(" |");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT))).append("\\         /");
            sb.append(renderArmor(m.getArmor(Mech.LOC_LT, true))).append("| ");
            sb.append(renderArmor(m.getArmor(Mech.LOC_CT, true))).append(" |");
            sb.append(renderArmor(m.getArmor(Mech.LOC_RT, true))).append("\\          /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LT))).append("| ");
            sb.append(renderArmor(m.getInternal(Mech.LOC_CT))).append(" |");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RT))).append("\\");
            sb.append( Settings.NL );
            // arms
            sb.append("    (").append(renderArmor(m.getArmor(Mech.LOC_LARM)));
            sb.append("). -- .(").append(renderArmor(m.getArmor(Mech.LOC_RARM)));
            sb.append(")       (   |    |   )        (");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LARM))).append("). -- .(");
            sb.append(renderArmor(m.getInternal(Mech.LOC_RARM))).append(")");
            sb.append( Settings.NL );
            // legs
            sb.append("       /  /\\  \\             /      \\              /  /\\  \\");
            sb.append( Settings.NL );
            sb.append("      /").append(renderArmor(m.getArmor(Mech.LOC_LLEG)));
            sb.append(".\\/.").append(renderArmor(m.getArmor(Mech.LOC_RLEG)));
            sb.append("\\           /        \\            /");
            sb.append(renderArmor(m.getInternal(Mech.LOC_LLEG)));
            sb.append(".\\/.").append(renderArmor(m.getInternal(Mech.LOC_RLEG))).append("\\");
            sb.append( Settings.NL );
        }
        sb.append( Settings.NL );
        return sb.toString();
    }
    
    private static String formatArmorInfantry(Infantry i) {
        StringBuffer sb = new StringBuffer(32);
        sb.append("Surviving troopers: ")
            .append( renderArmor(i.getInternal(0)) )
            .append( Settings.NL );
        return sb.toString();
    }
    
    private static String formatArmorBattleArmor(BattleArmor b) {
        StringBuffer sb = new StringBuffer(32);
        for (int i = 1; i < b.locations(); i++) {
            sb.append("Trooper ").append(i).append(": ")
                .append( renderArmor(b.getArmor(i)) )
                .append(" / ")
                .append( renderArmor(b.getInternal(i)) );
            sb.append( Settings.NL );
        }
        return sb.toString();
    }
    
    private static String formatArmorProtomech(Protomech m)
    {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("         FRONT                INTERNAL");
        sb.append( Settings.NL );

        // head & main gun
        sb.append("        ");
        if ( m.hasMainGun() ) {
            sb.append(renderArmor(m.getArmor(Protomech.LOC_MAINGUN),1));
        } else {
            sb.append(" ");
        }
        sb.append(" (")
            .append(renderArmor(m.getArmor(Protomech.LOC_HEAD),1))
            .append(")                  ");
        if ( m.hasMainGun() ) {
            sb.append(renderArmor(m.getInternal(Protomech.LOC_MAINGUN),1));
        } else {
            sb.append(" ");
        }
        sb.append(" (");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_HEAD),1))
            .append(")");
        sb.append( Settings.NL );
        if ( m.hasMainGun() ) {
            sb.append("         \\/ \\                   \\/ \\");
            sb.append( Settings.NL );
        } else {
            sb.append("          / \\                    / \\");
            sb.append( Settings.NL );
        }
        // arms & torso
        sb.append("      (")
            .append(renderArmor(m.getArmor(Protomech.LOC_LARM),1));
        sb.append(" /") 
            .append(renderArmor(m.getArmor(Protomech.LOC_TORSO)))
            .append(" \\")
            .append(renderArmor(m.getArmor(Protomech.LOC_RARM)));
        sb.append(")            (");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_LARM),1))
            .append(" /")
            .append(renderArmor(m.getInternal(Protomech.LOC_TORSO)))
            .append(" \\");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_RARM)))
            .append(")");
        sb.append( Settings.NL );
        // legs
        sb.append("         | | |                  | | |");
        sb.append( Settings.NL );
        sb.append("        ( ")
            .append(renderArmor(m.getArmor(Protomech.LOC_LEG)));
        sb.append("  )                ( ");
        sb.append(renderArmor(m.getInternal(Protomech.LOC_LEG)))
            .append("  )");
        sb.append( Settings.NL );
        sb.append("");
        sb.append( Settings.NL );
        return sb.toString();
    }

    private static String renderArmor(int nArmor)
    {
        return renderArmor(nArmor, 2);
    }
    private static String renderArmor(int nArmor, int spaces)
    {
        if (nArmor <= 0) {
            if ( 1 == spaces ) {
                return "x";
            } else {
                return "xx";
            }
        }
        else {
            return makeLength(String.valueOf(nArmor), spaces, true);
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
