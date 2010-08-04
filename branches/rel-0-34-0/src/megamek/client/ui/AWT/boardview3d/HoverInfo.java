/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.boardview3d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IAimingModes;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.LosEffects;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.WeaponAttackAction;

class HoverInfo implements IDisplayable {

    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
    private static final int TOP = 5;
    private static final int LEFT = 5;
    private static final int PADDING = 5;
    private static final int WIDTH = 350;

    private FontMetrics fm;

    IGame game;
    Entity entity;
    Mounted equipment;
    Player localPlayer;
    Coords coords, los;

    HashMap<Integer, Vector<String>> sources = new HashMap<Integer, Vector<String>>();
    HashMap<Integer, Vector<String>> destinations = new HashMap<Integer, Vector<String>>();

    public HoverInfo(IGame g, BoardView3D bv) {
        game = g;
        coords = new Coords(0,0);
        fm = bv.getFontMetrics(FONT);
    }

    public void draw(Graphics g, Point drawRelativeTo, Dimension size) {
        if (!(g instanceof Graphics2D)) {
            System.err.println("Warning: HoverInfo is meant to be used with Graphics2D");
            return;
        }
        Graphics2D gr = (Graphics2D)g;

        Vector<String> info = getTipText();
        if (info == null) {
            return;
        }

        for (int i = 0; i < info.size(); i++) {
            String s = info.elementAt(i);
            int len = s.length();
            while (fm.stringWidth(s.substring(0, len)) > WIDTH-2*PADDING) {
                len--;
            }
            if (len != s.length()) {
                int len2 = len;
                while ((len2 > 0) && (" \t\r\n".indexOf(s.charAt(len2)) < 0)) {
                    len2--;
                }
                if (len2 <= 0) {
                    len2 = len;
                }
                info.removeElementAt(i);
                len = len2;
                while ((len > 0) && (" \t\r\n".indexOf(s.charAt(len)) >= 0)) {
                    len--;
                }
                info.insertElementAt(s.substring(0, len+1), i);
                len = s.length();
                while ((len2 < len) && (" \t\r\n".indexOf(s.charAt(len2)) >= 0)) {
                    len2++;
                }
                if (len2 < len) {
                    info.insertElementAt("    "+s.substring(len2, len), i+1);
                }
            }
        }

        gr.setFont(FONT);
        int height = info.size()*fm.getHeight()+PADDING*2;

        gr.setColor(new Color(Color.DARK_GRAY.getRed(), Color.DARK_GRAY.getGreen(), Color.DARK_GRAY.getBlue(), 128));
        gr.fillRect(TOP, LEFT, WIDTH+2, height);
        gr.setColor(Color.LIGHT_GRAY);
        gr.draw3DRect(TOP, LEFT, WIDTH+2, height, false);
        gr.draw3DRect(TOP+1, LEFT+1, WIDTH, height-2, true);
        int ypos = TOP+PADDING+fm.getAscent();
        for (String line : info) {
            gr.drawString(line, LEFT+PADDING, ypos);
            ypos += fm.getHeight();
        }
    }

    void setPosition(Coords c) {
        coords = c;
    }

    void setLOS(Coords c1) {
        los = c1;
    }

    Coords getLOS() {
        return los;
    }

    private void checkLOS(Vector<String> out) {
        Coords src = los;
        if (los == null) {
            if (entity == null) {
                return;
            }
            src = entity.getPosition();
        }

        Entity s = entity;
        Entity t = null;

        if (s != null) {
            t = game.getFirstEnemyEntity(coords, s);
        }

        // temporarily move the chosen source entity to the source hex and adjust
        // it to get the target into the front arc at the correct elevation
        Coords ocoords = null;
        int ofacing = 0, osfacing = 0, oelevation = 0;
        if ((s != null) && (los != null)) {
            ocoords = s.getPosition();
            ofacing = s.getFacing();
            osfacing = s.getSecondaryFacing();
            oelevation = s.getElevation();
            s.setGame(null);
            s.setPosition(los);
            int dir = src.direction(coords);
            s.setFacing(dir);
            s.setSecondaryFacing(dir);
            int sdepth = game.getBoard().getHex(ocoords).depth();
            int ddepth = game.getBoard().getHex(src).depth();
            if (ddepth > 0) {
                s.setElevation(oelevation == -sdepth? -ddepth : oelevation > 0 ? oelevation : oelevation <= -ddepth? oelevation : -ddepth);
            } else if (oelevation < 0) {
                s.setElevation(0);
            }
            s.setGame(game);
        }

        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = src;
        ai.targetPos = coords;
        boolean mechInFirst  = (s != null? s.height() == 1 : GUIPreferences.getInstance().getMechInFirst());
        boolean mechInSecond = (t != null? t.height() == 1 : GUIPreferences.getInstance().getMechInSecond());
        ai.attackHeight = s != null? s.height() : mechInFirst?  1 : 0;
        ai.targetHeight = t != null? t.height() : mechInSecond? 1 : 0;
        if ((los == null) && (s != null)) {
            // no need to mention the attacker: it's the unit showing in the MechDisplay
            ai.attackAbsHeight = game.getBoard().getHex(src).floor() + s.absHeight();
            ai.targetAbsHeight = game.getBoard().getHex(coords).floor() + (t == null? 0 : t.absHeight());
        } else {
            // show source unit
            ai.attackAbsHeight = game.getBoard().getHex(src).floor() + ai.attackHeight;
            ai.targetAbsHeight = game.getBoard().getHex(coords).floor() + ai.targetHeight;
            out.add(Messages.getString("BoardView1.Attacker", new Object[]{ //$NON-NLS-1$
                (s != null ? s.getDisplayName() : mechInFirst  ? Messages.getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech")), //$NON-NLS-1$ //$NON-NLS-2$
                src.getBoardNum()
            }));
        }

        if (t == null) {
            // show out hypothetical target
            out.add(Messages.getString("BoardView1.Target", new Object[]{ //$NON-NLS-1$
                    ai.targetHeight == 1 ? Messages.getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                    coords.getBoardNum()
            }));
        }

        if ((t != null) && (s != null)) {
            // If we have a source and a target, use exact calculation...
            int i = 0;
            Mounted eq;
            HashMap<String, Boolean> done = new HashMap<String, Boolean>();
            while ((eq = s.getEquipment(i++)) != null) {
                if ((eq.getType() instanceof WeaponType) && !done.containsKey(eq.getName())) {
                    ToHitData toHit = WeaponAttackAction.toHit(game, s.getId(), t, s.getEquipmentNum(eq),
                        Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE);
                    out.add(eq.getType().getName() + Messages.getString("BoardView1.needs") + toHit.getValueAsString() + " " + toHit.getTableDesc() //$NON-NLS-1$
                        + " ["+toHit.getDesc()+"]");
                    done.put(eq.getName(), null);
                }
            }
        } else {
            // ...otherwise, use the generic LOS tool
            LosEffects le = LosEffects.calculateLos(game, ai);
            if (le.isBlocked()) {
                out.add(Messages.getString("BoardView1.LOSBlocked", new Object[]{ //$NON-NLS-1$
                    new Integer(src.distance(coords))}));
            } else {
                out.add(Messages.getString("BoardView1.LOSNotBlocked", new Object[]{ //$NON-NLS-1$
                        new Integer(src.distance(coords))}));
                if (le.getHeavyWoods() > 0) {
                    out.add(Messages.getString("BoardView1.HeavyWoods", new Object[]{ //$NON-NLS-1$
                            new Integer(le.getHeavyWoods())}));
                }
                if (le.getLightWoods() > 0) {
                    out.add(Messages.getString("BoardView1.LightWoods", new Object[]{ //$NON-NLS-1$
                            new Integer(le.getLightWoods())}));
                }
                if (le.getLightSmoke() > 0) {
                    out.add(Messages.getString("BoardView1.LightSmoke", new Object[]{ //$NON-NLS-1$
                            new Integer(le.getLightSmoke())}));
                }
                if (le.getHeavySmoke() > 0) {
                    if (game.getOptions().booleanOption("maxtech_fire")) { //$NON-NLS-1$
                        out.add(Messages.getString("BoardView1.HeavySmoke", new Object[]{ //$NON-NLS-1$
                                new Integer(le.getHeavySmoke())}));
                    }
                    else {
                        out.add(Messages.getString("BoardView1.Smoke", new Object[]{ //$NON-NLS-1$
                                new Integer(le.getHeavySmoke())}));
                    }
                }
                if (le.isTargetCover()) {
                    out.add(Messages.getString("BoardView1.TargetPartialCover")); //$NON-NLS-1$
                }
                if (le.isAttackerCover()) {
                    out.add(Messages.getString("BoardView1.AttackerPartialCover")); //$NON-NLS-1$
                }
            }
        }

        // restore entity position
        if (ocoords != null) {
            s.setGame(null);
            s.setPosition(ocoords);
            s.setFacing(ofacing);
            s.setSecondaryFacing(osfacing);
            s.setElevation(oelevation);
            s.setGame(game);
        }
    }

    void setSelected(Entity en, Mounted eq, Player pl) {
        if ((en != null) && !game.getBoard().contains(en.getPosition())) {
            en = null;
        }
        entity = en;
        equipment = eq;
        localPlayer = pl;
        setLOS(los);
    }

    /**
     * The text to be displayed when the mouse is at a certain point
     */
    private Vector<String> getTipText() {
        IHex mhex = game.getBoard().getHex(coords);
        if (mhex == null) {
            return null;
        }

        Vector<String> out = new Vector<String>();

        out.add(Messages.getString("BoardView1.Hex") + coords.getBoardNum() //$NON-NLS-1$
                    + Messages.getString("BoardView1.level") + mhex.getElevation()); //$NON-NLS-1$

        if (mhex.containsTerrain(Terrains.JUNGLE)) {
            int ttl = mhex.getTerrain(Terrains.JUNGLE).getLevel();
            int tf = mhex.getTerrain(Terrains.JUNGLE).getTerrainFactor();
            if (ttl == 1) {
                out.add(Messages.getString("BoardView1.TipLightJungle", new Object[] { tf }));
            } else if (ttl == 2) {
                out.add(Messages.getString("BoardView1.TipHeavyJungle", new Object[] { tf }));
            } else if (ttl == 3) {
                out.add(Messages.getString("BoardView1.TipUltraJungle", new Object[] { tf }));
            } else {
                out.add(Messages.getString("BoardView1.TipJungle", new Object[] { tf }));
            }
        } else if (mhex.containsTerrain(Terrains.WOODS)) {
            int ttl = mhex.getTerrain(Terrains.WOODS).getLevel();
            int tf = mhex.getTerrain(Terrains.WOODS).getTerrainFactor();
            if (ttl == 1) {
                out.add(Messages.getString("BoardView1.TipLightWoods", new Object[] { tf }));
            } else if (ttl == 2) {
                out.add(Messages.getString("BoardView1.TipHeavyWoods", new Object[] { tf }));
            } else if (ttl == 3) {
                out.add(Messages.getString("BoardView1.TipUltraWoods", new Object[] { tf }));
            } else {
                out.add(Messages.getString("BoardView1.TipWoods", new Object[] { tf }));
            }
        }

        if (mhex.containsTerrain(Terrains.ICE)) {
            int tf = mhex.getTerrain(Terrains.ICE).getTerrainFactor();
            out.add(Messages.getString("BoardView1.TipIce", new Object[] { tf }));
        }

        if (mhex.containsTerrain(Terrains.RUBBLE)) {
            out.add(Messages.getString("BoardView1.Rubble")); //$NON-NLS-1$
        }

        if (mhex.containsTerrain(Terrains.SWAMP)) {
            out.add(Messages.getString("BoardView1.TipSwamp"));
        }

        if (mhex.containsTerrain(Terrains.FUEL_TANK)) {
            Building bldg = game.getBoard().getBuildingAt(coords);
            StringBuffer buf = new StringBuffer(Messages.getString("BoardView1.Height")); //$NON-NLS-1$
            buf.append(mhex.terrainLevel(Terrains.FUEL_TANK_ELEV));
            buf.append(" "); //$NON-NLS-1$
            buf.append(bldg.toString());
            buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
            buf.append(bldg.getCurrentCF(coords));
            out.add(buf.toString());
        }

        if (mhex.containsTerrain(Terrains.BUILDING)) {
            Building bldg = game.getBoard().getBuildingAt(coords);
            StringBuffer buf = new StringBuffer(Messages.getString("BoardView1.Height")); //$NON-NLS-1$
            buf.append(mhex.terrainLevel(Terrains.BLDG_ELEV));
            buf.append(" "); //$NON-NLS-1$
            buf.append(bldg.toString());
            buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
            buf.append(bldg.getCurrentCF(coords));
            out.add(buf.toString());
        }

        if (mhex.containsTerrain(Terrains.BRIDGE)) {
            Building bldg = game.getBoard().getBuildingAt(coords);
            StringBuffer buf = new StringBuffer(Messages.getString("BoardView1.Height")); //$NON-NLS-1$
            buf.append(mhex.terrainLevel(Terrains.BRIDGE_ELEV));
            buf.append(" "); //$NON-NLS-1$
            buf.append(bldg.toString());
            buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
            buf.append(bldg.getCurrentCF(coords));
            out.add(buf.toString());
        }

        if (game.containsMinefield(coords)) {
            Vector<Minefield> minefields = game.getMinefields(coords);
            for (int i = 0; i < minefields.size(); i++){
                Minefield mf =  minefields.elementAt(i);
                String owner =  " (" + game.getPlayer(mf.getPlayerId()).getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$

                switch (mf.getType()) {
                case (Minefield.TYPE_CONVENTIONAL) :
                    out.add(mf.getName()+Messages.getString("BoardView1.minefield") + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                    break;
                case (Minefield.TYPE_COMMAND_DETONATED) :
                    out.add(mf.getName()+Messages.getString("BoardView1.minefield") + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                    break;
                case (Minefield.TYPE_VIBRABOMB) :
                    if (mf.getPlayerId() == localPlayer.getId()) {
                        out.add(mf.getName()+Messages.getString("BoardView1.minefield")+"(" + mf.getSetting() + ") " + owner); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    } else {
                        out.add(mf.getName()+Messages.getString("BoardView1.minefield") + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    break;
                case (Minefield.TYPE_ACTIVE) :
                    out.add(mf.getName()+Messages.getString("BoardView1.minefield")+"(" + mf.getDensity() + ")" + owner); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    break;
                case (Minefield.TYPE_INFERNO) :
                    out.add(mf.getName()+Messages.getString("BoardView1.minefield")+"(" + mf.getDensity() + ")" + owner); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    break;
                }
            }
        }

        for (Enumeration<Entity> e = game.getEntities(coords); e.hasMoreElements(); ) {
            Entity en = e.nextElement();
            addEntityText(out, en);
        }

        for (Enumeration<ArtilleryAttackAction> e = game.getArtilleryAttacks(); e.hasMoreElements(); ) {
            final ArtilleryAttackAction aaa = e.nextElement();
            final Entity ae = game.getEntity(aaa.getEntityId());
            String s = null;
            if (ae != null) {
                if (aaa.getWeaponId() > -1) {
                    Mounted weap = ae.getEquipment(aaa.getWeaponId());
                    s = weap.getName();
                    if (aaa.getAmmoId() > -1) {
                        Mounted ammo = ae.getEquipment(aaa.getAmmoId());
                        s += "(" + ammo.getName() + ")";
                    }
                }
            }
            if (s == null) {
                s = Messages.getString("BoardView1.Artillery");
            }
            out.add(Messages.getString("BoardView1.ArtilleryAttack", new Object[] {
                s,
                new Integer(aaa.turnsTilHit),
                aaa.toHit(game).getValueAsString()
            }));

        }

        if ((equipment != null) && (entity != null) && equipment.getType().hasFlag(WeaponType.F_ARTILLERY)) {
            int amod = 0;
            if (entity.getOwner().getArtyAutoHitHexes().contains(coords)) {
                amod = TargetRoll.AUTOMATIC_SUCCESS;
            } else {
                amod = entity.aTracker.getModifier(equipment, coords);
            }

            if (amod == TargetRoll.AUTOMATIC_SUCCESS) {
                out.add(Messages.getString("BoardView1.ArtilleryAutohit"));
            } else {
                out.add(Messages.getString("BoardView1.ArtilleryAdjustment", new Object[] { new Integer(amod) }));
            }
        }
        checkLOS(out);
        return out;
    }

    private void addEntityText(Vector<String> out, Entity e) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(e.getChassis()).append(" (") //$NON-NLS-1$
                .append(e.getOwner().getName()).append("); ") //$NON-NLS-1$
                .append(e.getCrew().getGunnery()).append("/") //$NON-NLS-1$
                .append(e.getCrew().getPiloting()).append(
                        Messages.getString("BoardView1.pilot")); //$NON-NLS-1$
        int numAdv = e.getCrew().countAdvantages();
        boolean isMD = e.getCrew().countMDImplants() > 0;
        if (numAdv > 0) {
            buffer.append(" <") //$NON-NLS-1$
                    .append(numAdv).append(
                            Messages.getString("BoardView1.advs")); //$NON-NLS-1$
        }
        if (isMD) {
            buffer.append(Messages.getString("BoardView1.md")); //$NON-NLS-1$
        }
        out.add(buffer.toString());

        GunEmplacement ge = null;
        if (e instanceof GunEmplacement) {
            ge = (GunEmplacement) e;
        }

        buffer = new StringBuffer();
        if (ge == null) {
            buffer.append(Messages.getString("BoardView1.move")) //$NON-NLS-1$
                    .append(e.getMovementAbbr(e.moved)).append(
                            ":") //$NON-NLS-1$
                    .append(e.delta_distance).append(" (+") //$NON-NLS-1$
                    .append(
                            Compute.getTargetMovementModifier(game,
                                    e.getId()).getValue())
                    .append(");") //$NON-NLS-1$
                    .append(Messages.getString("BoardView1.Heat")) //$NON-NLS-1$
                    .append(e.heat);
            if (e.isCharging()) {
                buffer.append(" ") //$NON-NLS-1$
                        .append(Messages.getString("BoardView1.charge1")); //$NON-NLS-1$
            }
            if (e.isMakingDfa()) {
                buffer.append(" ") //$NON-NLS-1$
                        .append(Messages.getString("BoardView1.DFA1")); //$NON-NLS-1$
            }
        } else {
            if (ge.hasTurret() && ge.isTurretLocked()) {
                buffer
                        .append(Messages
                                .getString("BoardView1.TurretLocked"));
                if (ge.getFirstWeapon() == -1) {
                    buffer.append(",");
                    buffer.append(Messages
                            .getString("BoardView1.WeaponsDestroyed"));
                }
            } else if (ge.getFirstWeapon() == -1) {
                buffer.append(Messages
                        .getString("BoardView1.WeaponsDestroyed"));
            } else {
                buffer.append(Messages.getString("BoardView1.Operational"));
            }
        }
        if (e.isDone()) {
            buffer.append(" (").append(
                    Messages.getString("BoardView1.done")).append(")");
        }
        out.add(buffer.toString());

        buffer = new StringBuffer();
        if (ge == null) {
            buffer.append(Messages.getString("BoardView1.Armor")) //$NON-NLS-1$
                    .append(e.getTotalArmor()).append(
                            Messages.getString("BoardView1.internal")) //$NON-NLS-1$
                    .append(e.getTotalInternal());
        } else {
            buffer.append(Messages.getString("BoardView1.cf")) //$NON-NLS-1$
                    .append(ge.getCurrentCF()).append(
                            Messages.getString("BoardView1.turretArmor")) //$NON-NLS-1$
                    .append(ge.getCurrentTurretArmor());
        }
        out.add(buffer.toString());

        Vector<String> strs = sources.get(new Integer(e.getId()));
        if (strs != null) {
            out.addAll(strs);
        }
        strs = destinations.get(new Integer(e.getId()));
        if (strs != null) {
            out.add("Incoming:");
            out.addAll(strs);
        }
    }

    public void add(AttackAction aa) {
        int targetType = aa.getTargetType();
        int targetId = aa.getTargetId();
        Entity ae = game.getEntity(aa.getEntityId());
        Targetable target = game.getTarget(targetType, targetId);

        String out = null;

        if (aa instanceof WeaponAttackAction) {
            WeaponAttackAction attack = (WeaponAttackAction)aa;
            final WeaponType wtype = (WeaponType)ae.getEquipment(attack.getWeaponId()).getType();
            final String roll = attack.toHit(game).getValueAsString();
            final String table = attack.toHit(game).getTableDesc();
            out = wtype.getName() + Messages.getString("BoardView1.needs") + roll + " " + table; //$NON-NLS-1$
        }

        if (aa instanceof KickAttackAction) {
            KickAttackAction attack = (KickAttackAction)aa;
            String rollLeft = ""; //$NON-NLS-1$
            String rollRight = ""; //$NON-NLS-1$
            final int leg = attack.getLeg();
            switch (leg) {
            case KickAttackAction.BOTH:
                rollLeft = KickAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), KickAttackAction.LEFT)
                        .getValueAsString();
                rollRight = KickAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), KickAttackAction.RIGHT)
                        .getValueAsString();
                out = Messages.getString("BoardView1.kickBoth", new Object[] { rollLeft, rollRight }); //$NON-NLS-1$
                break;
            case KickAttackAction.LEFT:
                rollLeft = KickAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), KickAttackAction.LEFT)
                        .getValueAsString();
                out = Messages.getString("BoardView1.kickLeft", new Object[] { rollLeft }); //$NON-NLS-1$
                break;
            case KickAttackAction.RIGHT:
                rollRight = KickAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), KickAttackAction.RIGHT)
                        .getValueAsString();
                out = Messages.getString("BoardView1.kickRight", new Object[] { rollRight }); //$NON-NLS-1$
                break;
            }
        }

        if (aa instanceof PunchAttackAction) {
            PunchAttackAction attack = (PunchAttackAction)aa;
            String rollLeft = ""; //$NON-NLS-1$
            String rollRight = ""; //$NON-NLS-1$
            final int arm = attack.getArm();
            switch (arm) {
            case PunchAttackAction.BOTH:
                rollLeft = PunchAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), PunchAttackAction.LEFT)
                        .getValueAsString();
                rollRight = PunchAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), PunchAttackAction.RIGHT)
                        .getValueAsString();
                out = Messages.getString("BoardView1.punchBoth", new Object[] { rollLeft, rollRight }); //$NON-NLS-1$
                break;
            case PunchAttackAction.LEFT:
                rollLeft = PunchAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), PunchAttackAction.LEFT)
                        .getValueAsString();
                out = Messages.getString("BoardView1.punchLeft", new Object[] { rollLeft }); //$NON-NLS-1$
                break;
            case PunchAttackAction.RIGHT:
                rollRight = PunchAttackAction.toHit(
                        game,
                        attack.getEntityId(),
                        game.getTarget(attack.getTargetType(), attack
                                .getTargetId()), PunchAttackAction.RIGHT)
                        .getValueAsString();
                out = Messages.getString("BoardView1.punchRight", new Object[] { rollRight }); //$NON-NLS-1$
                break;
            }
        }

        if (aa instanceof PushAttackAction) {
            PushAttackAction attack = (PushAttackAction)aa;
            final String roll = attack.toHit(game).getValueAsString();
            out = Messages.getString("BoardView1.push", new Object[] { roll }); //$NON-NLS-1$
        }

        if (aa instanceof ClubAttackAction) {
            ClubAttackAction attack = (ClubAttackAction)aa;
            final String roll = attack.toHit(game).getValueAsString();
            final String club = attack.getClub().getName();
            out = Messages.getString("BoardView1.hit", new Object[] { club, roll }); //$NON-NLS-1$
        }

        if (aa instanceof ChargeAttackAction) {
            ChargeAttackAction attack = (ChargeAttackAction)aa;
            final String roll = attack.toHit(game).getValueAsString();
            out = Messages.getString("BoardView1.charge", new Object[] { roll }); //$NON-NLS-1$
        }

        if (aa instanceof DfaAttackAction) {
            DfaAttackAction attack = (DfaAttackAction)aa;
            final String roll = attack.toHit(game).getValueAsString();
            out = Messages.getString("BoardView1.DFA", new Object[] { roll }); //$NON-NLS-1$
        }

        if (aa instanceof ProtomechPhysicalAttackAction) {
            ProtomechPhysicalAttackAction attack = (ProtomechPhysicalAttackAction)aa;
            final String roll = attack.toHit(game).getValueAsString();
            out = Messages.getString("BoardView1.proto", new Object[] { roll }); //$NON-NLS-1$
        }

        if (aa instanceof SearchlightAttackAction) {
            out = Messages.getString("BoardView1.Searchlight");
        }

        if (out != null) {
            Integer id = new Integer(ae.getId());
            Vector<String> strs = sources.get(id);
            if (strs == null) {
                strs = new Vector<String>();
            }
            strs.add(out + " " + Messages.getString("BoardView1.on") + " " + target.getDisplayName());
            sources.put(id, strs);

            id = new Integer(targetId);
            strs = destinations.get(id);
            if (strs == null) {
                strs = new Vector<String>();
            }
            strs.add(out + " [" + ae.getDisplayName()+"]");
            destinations.put(id, strs);
        }
    }

    public void remove(Entity entity) {
        Integer id = new Integer(entity.getId());
        sources.remove(id);
        destinations.remove(id);
    }

    public void clear() {
        sources.clear();
        destinations.clear();
    }

    public void update() {
        clear();
        for (EntityAction ea : game.getActionsVector()) {
            if (ea instanceof AttackAction) {
                add((AttackAction)ea);
            }
        }
        for (EntityAction ea : game.getChargesVector()) {
            if (ea instanceof AttackAction) {
                add((AttackAction)ea);
            }
        }
    }

    public void setIdleTime(long timeIdle, boolean add) {
    }

    public boolean isHit(Point p, Dimension size) {
        return false;
    }

    public boolean isMouseOver(Point p, Dimension size) {
        return false;
    }

    public boolean isSliding() {
        return false;
    }

    public boolean slide() {
        return false;
    }

    public boolean isDragged(Point p, Dimension size) {
        return false;
    }

    public boolean isBeingDragged() {
        return false;
    }

    public boolean isReleased() {
        return false;
    }
}
