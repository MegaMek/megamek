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

package megamek.client.ui.panels;

/**
 * The localized text and component names for one {@link OptionFilterBar}.
 *
 * <p>Values are resolved text, not resource keys: the owning panel looks them up in its own bundle, so the filter
 * bar needs no knowledge of message keys and the two option lists can word their prompts differently.</p>
 *
 * @param filterFieldName     component name of the search field, for tests and UI automation
 * @param filterPlaceholder   grey prompt shown in the empty search field
 * @param filterTooltip       tooltip explaining what the search matches against
 * @param toggleName          component name of the "show unimplemented" check box
 * @param toggleText          label of the check box
 * @param toggleTooltip       tooltip explaining what the check box reveals
 * @param matchCountName      component name of the match counter label
 * @param matchCountPattern   a {@link java.text.MessageFormat} pattern taking the shown and total counts, in that
 *                            order (for example {@code "{0} of {1} quirks match"})
 */
public record OptionFilterBarLabels(String filterFieldName, String filterPlaceholder, String filterTooltip,
                                    String toggleName, String toggleText, String toggleTooltip,
                                    String matchCountName, String matchCountPattern) {
}
