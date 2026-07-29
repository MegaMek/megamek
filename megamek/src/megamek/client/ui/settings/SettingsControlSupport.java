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
package megamek.client.ui.settings;

import java.util.Collection;

import megamek.common.annotations.Nullable;

final class SettingsControlSupport {
    static final int DEFAULT_TOOLTIP_WRAP_SIZE = 100;

    private SettingsControlSupport() {
    }

    static String displayText(SettingsTextProvider textProvider, String keyBase,
          @Nullable Collection<SettingsBadge> badges) {
        String baseText = textProvider.getText(keyBase + ".text");
        String badgeHtml = SettingsBadge.formatHtml(badges);
        return baseText.contains("{0}")
              ? textProvider.getFormattedText(keyBase + ".text", badgeHtml)
              : baseText + badgeHtml;
    }

    static @Nullable String tooltipText(SettingsTextProvider textProvider, String keyBase) {
        String tooltipKey = keyBase + ".tooltip";
        if (textProvider.containsKey(tooltipKey)) {
            return textProvider.getText(tooltipKey);
        }
        String legacyTooltipKey = keyBase + ".toolTipText";
        return textProvider.containsKey(legacyTooltipKey) ? textProvider.getText(legacyTooltipKey) : null;
    }

    static int tooltipWrapSize(@Nullable Integer customWrapSize) {
        return customWrapSize == null ? DEFAULT_TOOLTIP_WRAP_SIZE : customWrapSize;
    }
}
