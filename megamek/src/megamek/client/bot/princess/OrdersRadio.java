/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.bot.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.force.Force;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import org.apache.logging.log4j.Level;

/**
 * How the bot tells its teammates what its units are doing with their orders. With radio chatter on (the default) the
 * replies are short radio calls with callsigns: Inner Sphere forces talk like a modern military ("Command One: Set at
 * 1508."), Clan forces use Stars and Points and no contractions ("Alpha Star, Point One: In position at 1508."). With
 * it off, the plain replies are used.
 *
 * <p>A lance speaks once per kind of event per round, not once per unit, so an order to thirty lances does not bury
 * the chat. The same event always goes to the log in plain words, whatever the voice.</p>
 */
public class OrdersRadio {

    private static final MMLogger LOGGER = MMLogger.create(OrdersRadio.class);

    private static final List<String> NUMBER_WORDS = List.of("One", "Two", "Three", "Four", "Five", "Six", "Seven",
          "Eight", "Nine", "Ten", "Eleven", "Twelve");

    /** How the replies sound. */
    public enum RadioVoice {
        /** Plain replies, no radio talk. */
        PLAIN,
        /** Modern military radio talk with lance callsigns. */
        INNER_SPHERE,
        /** Clan radio talk: Stars and Points, no contractions. */
        CLAN
    }

    private final Princess owner;
    private boolean isRadioChatterOn = true;
    private final Set<String> spokenThisRound = new HashSet<>();
    private int spokenRound = -1;

    /**
     * @param owner the bot that talks
     */
    OrdersRadio(Princess owner) {
        this.owner = owner;
    }

    /**
     * @param radioChatterOn {@code true} for radio calls, {@code false} for plain replies
     */
    public void setRadioChatter(boolean radioChatterOn) {
        isRadioChatterOn = radioChatterOn;
    }

    /**
     * @return {@code true} if the bot answers with radio calls
     */
    public boolean isRadioChatterOn() {
        return isRadioChatterOn;
    }

    /**
     * @return the voice the bot uses: plain when radio chatter is off, else Clan when most of its units are Clan
     */
    RadioVoice voice() {
        if (!isRadioChatterOn) {
            return RadioVoice.PLAIN;
        }
        int clanUnits = 0;
        int allUnits = 0;
        for (Entity entity : owner.getEntitiesOwned()) {
            allUnits++;
            if (entity.isClan()) {
                clanUnits++;
            }
        }
        return ((allUnits > 0) && ((clanUnits * 2) > allUnits)) ? RadioVoice.CLAN : RadioVoice.INNER_SPHERE;
    }

    /**
     * Reports an event about one unit's orders, once per lance and kind of event per round.
     *
     * @param entity the unit
     * @param event  the kind of event: {@code arrived}, {@code unreachable}, {@code followingOrders} or {@code fold}
     * @param detail the hex or edge the event is about
     */
    void report(Entity entity, String event, String detail) {
        String plain = Messages.getString("Princess.radio.PLAIN." + event, entity.getDisplayName(), detail);
        LOGGER.info("[BotOrders] {}", plain);
        int round = owner.getGame().getCurrentRound();
        if (round != spokenRound) {
            spokenThisRound.clear();
            spokenRound = round;
        }
        Force lance = owner.getGame().getForces().getForce(entity);
        String speaker = (lance == null) ? ("unit" + entity.getId()) : ("force" + lance.getId());
        if (!spokenThisRound.add(speaker + '|' + event)) {
            return;
        }
        RadioVoice voice = voice();
        if (voice == RadioVoice.PLAIN) {
            owner.sendChat(plain, Level.INFO);
            return;
        }
        owner.sendChat(callsign(entity, lance, voice) + ": "
              + Messages.getString("Princess.radio." + voice.name() + '.' + event, detail), Level.INFO);
    }

    /**
     * @return the unit's callsign: its lance's name and its place in the lance ("Command One"), or for a Clan force
     *       the Star and Point ("Alpha Star, Point One"); a unit in no lance uses its own name
     */
    static String callsign(Entity entity, @Nullable Force lance, RadioVoice voice) {
        if (lance == null) {
            return entity.getShortName();
        }
        int place = lance.entityIndex(entity);
        String placeWord = ((place >= 0) && (place < NUMBER_WORDS.size())) ? NUMBER_WORDS.get(place)
              : String.valueOf(place + 1);
        if (voice == RadioVoice.CLAN) {
            return lance.getName() + ", Point " + placeWord;
        }
        String base = lance.getName().replaceAll("(?i)\\s+(lance|company|star|binary|level ii)$", "");
        return base + ' ' + placeWord;
    }
}
