package megamek.client.bot;

import java.util.Enumeration;
import java.util.StringTokenizer;

import com.sun.java.util.collections.Iterator;

import megamek.client.GameEvent;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Player;

public class ChatProcessor {

    protected void processChat(GameEvent ge, TestBot tb) {
        if (ge.getType() != GameEvent.GAME_PLAYER_CHAT)
            return;
        if (tb.getLocalPlayer() == null)
            return;
        StringTokenizer st = new StringTokenizer(ge.getMessage(), ":"); //$NON-NLS-1$
        if (!st.hasMoreTokens()) {
            return;
        }
            String name = st.nextToken().trim();
            //who is the message from?
            Enumeration e = tb.game.getPlayers();
            boolean found = false;
            Player p = null;
            while (e.hasMoreElements() && !found) {
                p = (Player) e.nextElement();
                if (name.equalsIgnoreCase(p.getName())) {
                    found = true;
                }
            }
            if (!found) {
                return;
            }
            try {
                if (st.hasMoreTokens() && st.nextToken().trim().equalsIgnoreCase(tb.getLocalPlayer().getName())) {
                    if (!p.isEnemyOf(tb.getLocalPlayer())) {
                        if (st.hasMoreTokens()) {
                            String command = st.nextToken().trim();
                            boolean understood = false;
                            //should create a command factory and a
                            // command object...
                            if (command.equalsIgnoreCase("echo")) { //$NON-NLS-1$
                                understood = true;
                            }
                            if (command.equalsIgnoreCase("calm down")) { //$NON-NLS-1$
                                Iterator i = tb.getEntitiesOwned().iterator();
                                while (i.hasNext()) {
                                    CEntity cen = tb.centities.get((Entity) i.next());
                                    if (cen.strategy.attack > 1) {
                                        cen.strategy.attack = 1;
                                    }
                                }
                                understood = true;
                            } else if (command.equalsIgnoreCase("be aggressive")) { //$NON-NLS-1$
                                Iterator i = tb.getEntitiesOwned().iterator();
                                while (i.hasNext()) {
                                    CEntity cen = tb.centities.get((Entity) i.next());
                                    cen.strategy.attack = Math.min(cen.strategy.attack * 1.2, 1.5);
                                }
                                understood = true;
                            } else if (command.equalsIgnoreCase("attack")) { //$NON-NLS-1$
                                int x = Integer.parseInt(st.nextToken().trim());
                                int y = Integer.parseInt(st.nextToken().trim());
                                Entity en = tb.game.getFirstEntity(new Coords(x - 1, y - 1));
                                if (en != null) {
                                    if (en.isEnemyOf((Entity) tb.getEntitiesOwned().get(0))) {
                                        CEntity cen = tb.centities.get(en);
                                        cen.strategy.target += 3;
                                        System.out.println(cen.entity.getShortName() + " " + cen.strategy.target); //$NON-NLS-1$
                                        understood = true;
                                    }
                                }
                            }
                            if (understood)
                                tb.sendChat("Understood " + p.getName()); 
                        }
                    } else {
                        tb.sendChat("I can't do that, " + p.getName()); 
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
}
