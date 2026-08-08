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

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import megamek.common.annotations.Nullable;

/** A localized numeric spinner for settings forms. */
public class SettingsSpinner extends JSpinner implements SettingsHelpProvider {
    private static final List<Class<? extends Number>> SUPPORTED_NUMBER_TYPES = List.of(
          Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class);

    private final String settingsHelpText;

    public SettingsSpinner(SettingsTextProvider textProvider, String keyBase, Number defaultValue,
          Number minimum, Number maximum, Number stepSize) {
        this(textProvider, keyBase, defaultValue, minimum, maximum, stepSize, false, List.of());
    }

    public SettingsSpinner(SettingsTextProvider textProvider, String keyBase, Number defaultValue,
          Number minimum, Number maximum, Number stepSize, boolean noTooltip, Collection<SettingsBadge> badges) {
        super(createSpinnerModel(defaultValue, minimum, maximum, stepSize));
        String tooltipText = noTooltip ? null : SettingsControlSupport.tooltipText(textProvider, keyBase);
        settingsHelpText = tooltipText == null ? null : appendBadges(wordWrap(tooltipText), badges);
        if (settingsHelpText != null) {
            setToolTipText(settingsHelpText);
        }
        configureSpinner(keyBase);
    }

    @Override
    public @Nullable String getSettingsHelpText() {
        return settingsHelpText;
    }

    private static String appendBadges(String wrappedText, Collection<SettingsBadge> badges) {
        String badgeHtml = SettingsBadge.formatHtml(badges);
        return badgeHtml.isBlank() ? wrappedText : wrappedText.replace("</html>", badgeHtml + "</html>");
    }

    private static SpinnerNumberModel createSpinnerModel(Number defaultValue, Number minimum,
          Number maximum, Number stepSize) {
        Number value = Objects.requireNonNull(defaultValue);
        Number lowerBound = Objects.requireNonNull(minimum);
        Number upperBound = Objects.requireNonNull(maximum);
        Number increment = Objects.requireNonNull(stepSize);
        int numericType = Math.max(Math.max(numericType(value), numericType(lowerBound)),
              Math.max(numericType(upperBound), numericType(increment)));
        return switch (numericType) {
            case 5 -> new SpinnerNumberModel(value.doubleValue(), lowerBound.doubleValue(),
                  upperBound.doubleValue(), increment.doubleValue());
            case 4 -> new SpinnerNumberModel(Float.valueOf(value.floatValue()), Float.valueOf(lowerBound.floatValue()),
                  Float.valueOf(upperBound.floatValue()), Float.valueOf(increment.floatValue()));
            case 3 -> new SpinnerNumberModel(Long.valueOf(value.longValue()), Long.valueOf(lowerBound.longValue()),
                  Long.valueOf(upperBound.longValue()), Long.valueOf(increment.longValue()));
            case 2 -> new SpinnerNumberModel(value.intValue(), lowerBound.intValue(),
                  upperBound.intValue(), increment.intValue());
            case 1 -> new SpinnerNumberModel(Short.valueOf(value.shortValue()), Short.valueOf(lowerBound.shortValue()),
                  Short.valueOf(upperBound.shortValue()), Short.valueOf(increment.shortValue()));
            default -> new SpinnerNumberModel(Byte.valueOf(value.byteValue()), Byte.valueOf(lowerBound.byteValue()),
                  Byte.valueOf(upperBound.byteValue()), Byte.valueOf(increment.byteValue()));
        };
    }

    private static int numericType(Number number) {
        int numericType = SUPPORTED_NUMBER_TYPES.indexOf(number.getClass());
        if (numericType < 0) {
            throw new IllegalArgumentException(
                  "Unsupported settings spinner number type: " + number.getClass().getName());
        }
        return numericType;
    }

    private void configureSpinner(String keyBase) {
        setName("spn" + keyBase);
        setFontScaling(this, false, 1);
        configureEditor(this);
    }

    /** Applies the standard settings alignment and focus behavior to any numeric spinner. */
    public static void configureEditor(JSpinner spinner) {
        if (spinner.getEditor() instanceof DefaultEditor editor) {
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        }
        installSelectAllOnFocus(spinner);
    }

    /** Selects the complete spinner value whenever its editor gains focus. */
    public static void installSelectAllOnFocus(JSpinner spinner) {
        if (spinner.getEditor() instanceof DefaultEditor editor) {
            JTextField textField = editor.getTextField();
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    SwingUtilities.invokeLater(textField::selectAll);
                }
            });
        }
    }
}
