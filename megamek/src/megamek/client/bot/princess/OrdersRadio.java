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
import java.util.Locale;
import java.util.Set;

import megamek.client.bot.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.force.Force;
import megamek.common.force.ForceNames;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.commands.RadioCommand;
import org.apache.logging.log4j.Level;

/**
 * How the bot tells its teammates what its units are doing with their orders. With radio chatter on (the default) the
 * replies are short radio calls with callsigns and nav points: Inner Sphere forces talk like a modern military
 * ("Charlie Lance, proceeding to Nav Point Gamma (1617)."), Clan forces use Stars and Points and no contractions
 * ("Alpha Star proceeds to Nav Point Gamma (1617)."). With it off, the plain replies are used.
 *
 * <p>Whole-lance events - an order taken, a shape formed, a waypoint reached - are called by the lance ("Charlie
 * Lance, ..."); a single unit's trouble by the unit ("Charlie Three, taking damage..."). A lance speaks once per kind of
 * event per round, not once per unit, so an order to thirty lances does not bury the chat. The same event always goes
 * to the log in plain words, whatever the voice.</p>
 */
public class OrdersRadio {

    /** The events a bot reports on its units' orders; each names its calls in the bot's messages. */
    enum RadioEvent {
        /** The lance, or a unit on its own, took a new route: the call names its first nav point. */
        ORDERED("ordered", true),
        /** The lance reached the end of its route and holds there. */
        ARRIVED("arrived", true),
        /** The lance holds at a waypoint as ordered. */
        HOLDING("holding", true),
        /** The lance formed up at a waypoint and moves on: the call names the waypoint and the next nav point. */
        FORMED("formed", true),
        /** The lance leaves the board at the end of its route. */
        EXITING("exiting", true),
        /** The lance folds into a Column to get through a gap. */
        FOLD("fold", true),
        /** A unit cannot reach its waypoint and skips it. */
        UNREACHABLE("unreachable", false),
        /** A unit follows the player's orders over its own withdrawal. */
        FOLLOWING_ORDERS("followingOrders", false);

        private final String key;
        private final boolean isLanceCall;

        RadioEvent(String key, boolean isLanceCall) {
            this.key = key;
            this.isLanceCall = isLanceCall;
        }

        /** @return the event's part of the message key, e.g. {@code arrived} */
        String key() {
            return key;
        }

        /** @return {@code true} if the lance makes the call, rather than the unit it happened to */
        boolean isLanceCall() {
            return isLanceCall;
        }
    }

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
        CLAN,
        /** ComStar and Word of Blake radio talk: Level IIs and Adepts, formal. */
        COMSTAR
    }

    /** The voice a player has set for the bot, or AUTO to let the bot work it out. */
    public enum RadioSetting {
        /** Work the voice out from the bot's name and units. */
        AUTO,
        /** Plain replies. */
        OFF,
        /** Always the Inner Sphere voice. */
        INNER_SPHERE,
        /** Always the Clan voice. */
        CLAN,
        /** Always the ComStar voice. */
        COMSTAR
    }

    private static final List<String> COMSTAR_NAMES = List.of("comstar", "word of blake", "com guard", "wob");

    private final Princess owner;
    private RadioSetting setting = RadioSetting.AUTO;
    private final Set<String> spokenThisRound = new HashSet<>();
    private int spokenRound = -1;

    /**
     * @param owner the bot that talks
     */
    OrdersRadio(Princess owner) {
        this.owner = owner;
    }

    /**
     * @param radioSetting the voice to use, or AUTO to let the bot work it out
     */
    public void setRadioSetting(RadioSetting radioSetting) {
        setting = radioSetting;
    }

    /**
     * @return the voice setting
     */
    public RadioSetting getRadioSetting() {
        return setting;
    }

    /**
     * The voice the bot uses. A voice the player set wins. Otherwise a bot named for ComStar, the Word of Blake or the
     * Com Guards talks like ComStar and one named for a Clan like a Clan - MegaMek players carry no faction, so the
     * name is the best hint there is - and failing both, the bot talks like a Clan when most of its units are Clan.
     *
     * @return the voice
     */
    RadioVoice voice() {
        RadioVoice chosenVoice = switch (setting) {
            case OFF -> RadioVoice.PLAIN;
            case INNER_SPHERE -> RadioVoice.INNER_SPHERE;
            case CLAN -> RadioVoice.CLAN;
            case COMSTAR -> RadioVoice.COMSTAR;
            case AUTO -> null;
        };
        if (chosenVoice != null) {
            return chosenVoice;
        }
        String botName = owner.getName().toLowerCase(Locale.ROOT);
        for (String comstarName : COMSTAR_NAMES) {
            if (botName.contains(comstarName)) {
                return RadioVoice.COMSTAR;
            }
        }
        if (botName.startsWith("clan ") || botName.contains(" clan ")) {
            return RadioVoice.CLAN;
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
     * Reports an event about a unit's orders, once per lance and kind of event per round. A whole-lance event is
     * called by the lance, anything else by the unit.
     *
     * @param entity  the unit
     * @param event   the kind of event
     * @param details what the event is about, usually a nav point such as {@code Nav Point Gamma (1617)}, or an edge;
     *                {@link RadioEvent#FORMED} takes the waypoint and then the next nav point
     */
    void report(Entity entity, RadioEvent event, String... details) {
        Force lance = owner.getGame().getForces().getForce(entity);
        String lanceName = (lance == null) ? null : lanceName(lance);
        String plainSpeaker = (event.isLanceCall() && (lanceName != null)) ? lanceName : entity.getDisplayName();
        String plain = Messages.getString("Princess.radio.PLAIN." + event.key(), withSpeaker(plainSpeaker, details));
        LOGGER.info("[BotOrders] {}", plain);
        int round = owner.getGame().getCurrentRound();
        if (round != spokenRound) {
            spokenThisRound.clear();
            spokenRound = round;
        }
        String speaker = (lance == null) ? ("unit" + entity.getId()) : ("force" + lance.getId());
        if (!spokenThisRound.add(speaker + '|' + event)) {
            return;
        }
        RadioVoice voice = voice();
        String call = plain;
        if (voice != RadioVoice.PLAIN) {
            String callsign = (event.isLanceCall() && (lanceName != null)) ? lanceName
                  : callsign(entity, lance, lanceName, voice);
            call = Messages.getString("Princess.radio." + voice.name() + '.' + event.key(),
                  withSpeaker(callsign, details));
        }
        // the server relays the call to the unit's own side only, as a toast with the unit's icon and a chat line;
        // it names the hex the unit is heading for, so the other side must not hear it
        owner.sendChat(RadioCommand.commandText(entity.getId(), call), Level.INFO);
    }

    private static Object[] withSpeaker(String speaker, String... details) {
        Object[] arguments = new Object[details.length + 1];
        arguments[0] = speaker;
        System.arraycopy(details, 0, arguments, 1, details.length);
        return arguments;
    }

    /**
     * @param lance one of the bot's lances
     *
     * @return the name the radio calls the lance by: its own name, or, when that says nothing (blank, the bot's own
     *       name, "Force"), a callsign name by its place among the bot's forces with no usable name, Alpha Lance
     *       first (Alpha Star for a Clan force)
     */
    String lanceName(Force lance) {
        String botName = owner.getName();
        if (!ForceNames.isGeneric(lance.getName(), botName)) {
            return lance.getName();
        }
        // the place counts the older forces, by id, so each lance keeps its callsign name all game
        int place = 0;
        for (Force force : owner.getGame().getForces().getAllForces()) {
            if ((force.getId() < lance.getId()) && (force.getOwnerId() == lance.getOwnerId())
                  && ForceNames.isGeneric(force.getName(), botName)) {
                place++;
            }
        }
        return ForceNames.callsignName(place, styleOf(voice()));
    }

    private static ForceNames.Style styleOf(RadioVoice voice) {
        return switch (voice) {
            case CLAN -> ForceNames.Style.CLAN;
            case COMSTAR -> ForceNames.Style.COMSTAR;
            case INNER_SPHERE, PLAIN -> ForceNames.Style.INNER_SPHERE;
        };
    }

    /**
     * @return the unit's callsign: its lance's name and its place in the lance ("Command One"), or for a Clan force
     *       the Star and Point ("Alpha Star, Point One"); a unit in no lance uses its own name
     */
    static String callsign(Entity entity, @Nullable Force lance, RadioVoice voice) {
        return callsign(entity, lance, (lance == null) ? null : lance.getName(), voice);
    }

    /**
     * @param lanceName the name the lance is called by, which for a lance with no usable name of its own is a callsign
     *                  name such as {@code Charlie Lance}
     *
     * @return the unit's callsign, as {@link #callsign(Entity, Force, RadioVoice)} but under the given lance name
     */
    static String callsign(Entity entity, @Nullable Force lance, @Nullable String lanceName, RadioVoice voice) {
        if ((lance == null) || (lanceName == null)) {
            return entity.getShortName();
        }
        int place = lance.entityIndex(entity);
        String placeWord = ((place >= 0) && (place < NUMBER_WORDS.size())) ? NUMBER_WORDS.get(place)
              : String.valueOf(place + 1);
        if (voice == RadioVoice.CLAN) {
            return lanceName + ", Point " + placeWord;
        }
        if (voice == RadioVoice.COMSTAR) {
            return lanceName + ", Adept " + placeWord;
        }
        String base = lanceName.replaceAll("(?i)\\s+(lance|company|star|binary|level ii)$", "");
        return base + ' ' + placeWord;
    }
}
