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
package megamek.client.ratgenerator;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * How one faction designates its formations, read from the {@code <formationNaming>} element of a
 * faction ruleset file.
 *
 * <p>The ruleset {@code <name>} elements already produce faction-correct <i>flavour</i> names -
 * "Battle Lance", "Assault Cluster", "Trinary [Battle]", "IV-alpha". What they cannot express is how
 * a consumer should <i>designate</i> those formations relative to one another: whether the companies
 * of a battalion are lettered and restart in each battalion, whether regiments take ordinals, or
 * whether a Clan galaxy takes a Greek letter. That positional vocabulary differs sharply between the
 * Inner Sphere, the Clans and ComStar, and this element is where each tradition declares it.</p>
 *
 * <p>Conventions are authored in the four root rulesets ({@code IS}, {@code CLAN}, {@code CS},
 * {@code Periphery}) and inherited by every other faction through
 * {@link Ruleset#findNamingTier(int)}, which walks the parent chain one echelon at a time. A faction
 * therefore overrides a single echelon without restating the whole convention.</p>
 *
 * <p>Echelon numbers are not unique across traditions - echelon 4 is an Inner Sphere Company, a Clan
 * Binary and a ComStar Choir. That is unambiguous here because a convention is only ever consulted
 * through the ruleset of the faction it belongs to.</p>
 *
 * @see DesignatorStyle
 */
public class FormationNamingConvention {

    private static final MMLogger LOGGER = MMLogger.create(FormationNamingConvention.class);

    /**
     * How the formations at one echelon are designated relative to their siblings.
     *
     * <p>Only {@link #ALPHABET} consults the consumer's user-facing naming preference; every other
     * value is fixed by the faction's own tradition. That split is deliberate - it lets a player
     * choose between the CCB, ICAO, plain-letter and Greek alphabets for Inner Sphere companies
     * without that choice overwriting canon Clan or ComStar designations.</p>
     */
    public enum DesignatorStyle {
        /**
         * Keep the ruleset's own name. The consumer disambiguates only when two siblings collide,
         * and never rewrites a name that is already unique among them.
         */
        ENGINE,
        /** The consumer's selected naming alphabet: Able/Baker, Alpha/Bravo, A/B, or Alpha/Beta. */
        ALPHABET,
        /** Spelled ordinals: First, Second, Third. */
        ORDINAL,
        /** Numeric ordinals: 1st, 2nd, 3rd - the form regimental designations conventionally take. */
        NUMERIC_ORDINAL,
        /** Roman numerals: I, II, III. */
        ROMAN,
        /**
         * Greek letters: Alpha, Beta, Gamma. Fixed regardless of the consumer's alphabet preference,
         * because the factions that use it (Clan galaxies) use it canonically.
         */
        GREEK,
        /** Arabic numerals: 1, 2, 3. */
        NUMBER;

        /**
         * Parses the {@code designator} attribute value.
         *
         * @param value the attribute text, in any case
         *
         * @return the matching style, or {@code null} when {@code value} is not a recognised style
         */
        static @Nullable DesignatorStyle parse(String value) {
            for (DesignatorStyle style : values()) {
                if (style.name().equalsIgnoreCase(value)) {
                    return style;
                }
            }
            return null;
        }
    }

    /**
     * The naming rule for one echelon.
     *
     * @param echelon         the echelon this rule applies to, after constant substitution
     * @param designatorStyle how formations at this echelon are designated among their siblings
     * @param qualifiedByParent whether the parent formation's designator is prepended, which is what
     *                          keeps names unique once a designator sequence restarts under each
     *                          parent ({@code 1/Alpha Company} rather than a bare
     *                          {@code Alpha Company} repeated in every battalion)
     */
    public record Tier(int echelon, DesignatorStyle designatorStyle, boolean qualifiedByParent) {}

    private final Map<Integer, Tier> tiersByEchelon = new HashMap<>();

    /**
     * Reads a {@code <formationNaming>} element. Malformed tiers are logged and skipped rather than
     * failing the whole ruleset load, so one bad attribute cannot cost a faction its entire
     * generation ruleset.
     *
     * @param node    the {@code <formationNaming>} element
     * @param faction the faction the containing ruleset declares, used for log messages
     *
     * @return the parsed convention, empty when the element declares no usable tier
     */
    static FormationNamingConvention createFromXml(Node node, String faction) {
        FormationNamingConvention convention = new FormationNamingConvention();
        NodeList children = node.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            Node child = children.item(childIndex);
            if (!"tier".equals(child.getNodeName()) || !(child instanceof Element tierElement)) {
                continue;
            }
            convention.addTier(tierElement, faction);
        }
        LOGGER.debug("[ForceGen][Naming] {} declares {} naming tier(s)", faction, convention.tiersByEchelon.size());
        return convention;
    }

    private void addTier(Element tierElement, String faction) {
        String echelonText = tierElement.getAttribute("echelon");
        if (echelonText.isBlank()) {
            LOGGER.warn("[ForceGen][Naming] {}: <tier> is missing the required echelon attribute; skipping it",
                  faction);
            return;
        }

        int echelon;
        try {
            echelon = Integer.parseInt(Ruleset.substituteConstants(echelonText).trim());
        } catch (NumberFormatException exception) {
            LOGGER.warn("[ForceGen][Naming] {}: <tier> echelon \"{}\" is not a number or known %CONSTANT%;"
                        + " skipping it", faction, echelonText);
            return;
        }

        String designatorText = tierElement.getAttribute("designator");
        DesignatorStyle designatorStyle = DesignatorStyle.parse(designatorText);
        if (designatorStyle == null) {
            LOGGER.warn("[ForceGen][Naming] {}: <tier echelon=\"{}\"> has unrecognised designator \"{}\";"
                        + " expected one of ENGINE, ALPHABET, ORDINAL, NUMERIC_ORDINAL, ROMAN, GREEK, NUMBER."
                        + " Falling back to ENGINE so the ruleset's own name is kept.",
                  faction, echelonText, designatorText);
            designatorStyle = DesignatorStyle.ENGINE;
        }

        boolean qualifiedByParent = "parent".equalsIgnoreCase(tierElement.getAttribute("qualifyWith"));

        Tier replaced = tiersByEchelon.put(echelon, new Tier(echelon, designatorStyle, qualifiedByParent));
        if (replaced != null) {
            LOGGER.warn("[ForceGen][Naming] {}: echelon {} is declared more than once; the later"
                        + " <tier> ({}) wins over the earlier one ({})",
                  faction, echelon, designatorStyle, replaced.designatorStyle());
        }
    }

    /**
     * @param echelon the echelon to look up
     *
     * @return the rule declared for {@code echelon} by this convention alone, or {@code null} when it
     *       declares none. Callers wanting inherited rules must use {@link Ruleset#findNamingTier(int)}.
     */
    public @Nullable Tier getTier(int echelon) {
        return tiersByEchelon.get(echelon);
    }

    /**
     * @return {@code true} when this convention declares no tiers at all, which is the state of every
     *       ruleset that does not carry a {@code <formationNaming>} element
     */
    public boolean isEmpty() {
        return tiersByEchelon.isEmpty();
    }
}
