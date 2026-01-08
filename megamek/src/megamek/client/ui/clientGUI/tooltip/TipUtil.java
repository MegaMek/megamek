/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.tooltip;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.enums.ProstheticEnhancementType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/** Provides static helper functions for creating entity and crew tooltips. */
public final class TipUtil {

    final static boolean BR = true;
    final static boolean NOBR = false;

    /**
     * Returns a List wherein each element consists of an option group of the given optGroups, which is e.g.
     * crew.getOptions().getGroups() or entity.getQuirks().getGroups() as well as the count of active options within
     * that group, e.g. "Manei Domini (2)". A counter function for the options of a group must be supplied, in the form
     * of e.g. e -&gt; crew.countOptions(e) or e -&gt; entity.countQuirks(e). A namer function for the group names must
     * be supplied, e.g. (e) -&gt; weapon.getDesc().
     */
    public static List<String> getOptionListArray(Enumeration<IOptionGroup> optGroups,
          Function<String, Integer> counter, Function<IOptionGroup, String> namer) {

        List<String> result = new ArrayList<>();
        while (optGroups.hasMoreElements()) {
            IOptionGroup advGroup = optGroups.nextElement();
            int numOpts = counter.apply(advGroup.getKey());
            if (numOpts > 0) {
                result.add(namer.apply(advGroup) + " (" + numOpts + ")");
            }
        }
        return result;
    }

    /**
     * Returns an HTML String listing the options given as optGroups, which is e.g. crew.getOptions().getGroups() or
     * entity.getQuirks().getGroups(). A counter function for the options of a group must be supplied, in the form of
     * e.g. e -&gt; crew.countOptions(e) or e -&gt; entity.countQuirks(e). A namer function for the group names must be
     * supplied, e.g. (e) -&gt; weapon.getDesc(). The group names are italicized. The list is 40 characters wide with
     * \u2B1D as option separator.
     */
    public static String getOptionList(Enumeration<IOptionGroup> optGroups, Function<String, Integer> counter,
          Function<IOptionGroup, String> namer, boolean detailed) {
        if (detailed) {
            return optionListFull(optGroups, counter, namer, null);
        } else {
            return optionListShort(optGroups, counter, namer);
        }
    }

    /**
     * Returns an HTML String listing the options given as optGroups, which is e.g. crew.getOptions().getGroups() or
     * entity.getQuirks().getGroups(). A counter function for the options of a group must be supplied, in the form of
     * e.g. e -&gt; crew.countOptions(e) or e -&gt; entity.countQuirks(e). The list is 40 characters wide with \u2B1D as
     * option separator.
     */
    public static String getOptionList(Enumeration<IOptionGroup> optGroups,
          Function<String, Integer> counter, boolean detailed) {
        return getOptionList(optGroups, counter, detailed, null);
    }

    /**
     * Returns an HTML String listing the options given as optGroups, with optional entity context for enhanced display
     * of entity-specific options like prosthetic enhancements.
     *
     * @param optGroups the option groups to list
     * @param counter   function to count options by name
     * @param detailed  if true, returns full option list; if false, returns short summary
     * @param entity    optional entity context for enhanced display (may be null)
     * @return HTML formatted string listing the options
     */
    public static String getOptionList(Enumeration<IOptionGroup> optGroups,
          Function<String, Integer> counter, boolean detailed, @Nullable Entity entity) {
        if (detailed) {
            return optionListFull(optGroups, counter, IOptionGroup::getDisplayableName, entity);
        } else {
            return optionListShort(optGroups, counter, IOptionGroup::getDisplayableName);
        }
    }

    static String htmlSpacer(int unscaledSize) {
        return "<P><IMG SRC=FILE:" + Configuration.widgetsDir() + "/Tooltip/TT_Spacer.png "
              + "WIDTH=" + unscaledSize + " HEIGHT=" + unscaledSize + "></P>";
    }

    // PRIVATE

    private static String optionListFull(Enumeration<IOptionGroup> advGroups, Function<String, Integer> counter,
          Function<IOptionGroup, String> namer, Entity entity) {
        StringBuilder result = new StringBuilder();

        // Get prosthetic enhancement details if this is an infantry entity
        String regularProstheticDetails = getRegularProstheticDetails(entity);
        String extraneousLimbDetails = getExtraneousLimbDetails(entity);

        while (advGroups.hasMoreElements()) {
            IOptionGroup advGroup = advGroups.nextElement();
            if (counter.apply(advGroup.getKey()) > 0) {
                // Group title
                result.append("<I>").append(namer.apply(advGroup)).append(":</I><BR>");

                // Gather the group options
                List<String> origList = new ArrayList<>();
                for (Enumeration<IOption> advantages = advGroup.getOptions(); advantages.hasMoreElements(); ) {
                    IOption advantage = advantages.nextElement();
                    if (advantage != null && advantage.booleanValue()) {
                        String displayText = advantage.getDisplayableNameWithValue();

                        // Append prosthetic enhancement details for Enhanced/Improved Enhanced options
                        if (!regularProstheticDetails.isEmpty()
                              && (OptionsConstants.MD_PL_ENHANCED.equals(advantage.getName())
                              || OptionsConstants.MD_PL_I_ENHANCED.equals(advantage.getName()))) {
                            displayText += " (" + regularProstheticDetails + ")";
                        }

                        // Append extraneous limb details for Extraneous Limbs option
                        if (!extraneousLimbDetails.isEmpty()
                              && OptionsConstants.MD_PL_EXTRA_LIMBS.equals(advantage.getName())) {
                            displayText += " (" + extraneousLimbDetails + ")";
                        }

                        origList.add(displayText);
                    }
                }

                // Arrange the options in lines according to length
                List<String> advLines = UIUtil.arrangeInLines(origList, 40, " \u2B1D ", false);
                for (String line : advLines) {
                    result.append("&nbsp;&nbsp;").append(line).append("<BR>");
                }
            }
        }
        return result.toString();
    }

    /**
     * Gets regular prosthetic enhancement details for an infantry entity (slot 1 and 2 only).
     *
     * @param entity The entity to check (may be null or non-Infantry)
     *
     * @return String like "Laser x2, Grappler x1" or empty string if not applicable
     */
    private static String getRegularProstheticDetails(Entity entity) {
        if (!(entity instanceof Infantry infantry)) {
            return "";
        }

        StringBuilder details = new StringBuilder();
        if (infantry.hasProstheticEnhancement1()) {
            ProstheticEnhancementType type1 = infantry.getProstheticEnhancement1();
            details.append(type1.getDisplayName()).append(" x").append(infantry.getProstheticEnhancement1Count());
        }
        if (infantry.hasProstheticEnhancement2()) {
            if (details.length() > 0) {
                details.append(", ");
            }
            ProstheticEnhancementType type2 = infantry.getProstheticEnhancement2();
            details.append(type2.getDisplayName()).append(" x").append(infantry.getProstheticEnhancement2Count());
        }
        return details.toString();
    }

    /**
     * Gets extraneous limb details for an infantry entity (pair 1 and 2 only).
     *
     * @param entity The entity to check (may be null or non-Infantry)
     *
     * @return String like "Laser x2, Grappler x2" or empty string if not applicable
     */
    private static String getExtraneousLimbDetails(Entity entity) {
        if (!(entity instanceof Infantry infantry)) {
            return "";
        }

        StringBuilder details = new StringBuilder();
        if (infantry.hasExtraneousPair1()) {
            ProstheticEnhancementType pair1Type = infantry.getExtraneousPair1();
            details.append(pair1Type.getDisplayName()).append(" x2");
        }
        if (infantry.hasExtraneousPair2()) {
            if (details.length() > 0) {
                details.append(", ");
            }
            ProstheticEnhancementType pair2Type = infantry.getExtraneousPair2();
            details.append(pair2Type.getDisplayName()).append(" x2");
        }
        return details.toString();
    }

    /**
     * Gets all prosthetic enhancement details for an infantry entity (regular + extraneous). Used for combined tooltip
     * display.
     *
     * @param entity The entity to check (may be null or non-Infantry)
     *
     * @return String like "Laser x2, Grappler x1; Extra: Blade x2" or empty string if not applicable
     */
    private static String getProstheticEnhancementDetails(Entity entity) {
        String regular = getRegularProstheticDetails(entity);
        String extraneous = getExtraneousLimbDetails(entity);

        if (regular.isEmpty() && extraneous.isEmpty()) {
            return "";
        } else if (regular.isEmpty()) {
            return "Extra: " + extraneous;
        } else if (extraneous.isEmpty()) {
            return regular;
        } else {
            return regular + "; Extra: " + extraneous;
        }
    }

    private static String optionListShort(Enumeration<IOptionGroup> advGroups,
          Function<String, Integer> counter, Function<IOptionGroup, String> namer) {
        StringBuilder result = new StringBuilder();

        // Gather the option groups and option count per group
        List<String> origList = new ArrayList<>();
        while (advGroups.hasMoreElements()) {
            IOptionGroup advGroup = advGroups.nextElement();
            int numOpts = counter.apply(advGroup.getKey());
            if (numOpts > 0) {
                origList.add(namer.apply(advGroup) + " (" + numOpts + ")");
            }
        }

        // Arrange the option groups in lines according to length
        for (String line : UIUtil.arrangeInLines(origList, 40, "; ", true)) {
            result.append(line).append("<BR>");
        }
        return result.toString();
    }

}
