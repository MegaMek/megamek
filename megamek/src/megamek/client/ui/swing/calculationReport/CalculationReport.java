/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.calculationReport;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.Locale;

/**
 * This represents a report for any typical MegaMek suite calculation such as BV, cost or AS conversion.
 * It assumes that each line will have at most three entries, a sort of header text (Damage:), a calculation
 * (5 + 5 + 7 * 0.2) and a result on the right side (11.4).
 * <BR><BR>
 * The result can be given as a String or as a double value with a prefix String. When a double value is
 * given, it will be rounded to one decimal digit. Since the second entry (the calculation text) can
 * be more complicated than a single number, it can only be passed as a String and rounding must be
 * performed by the caller if needed.
 * <BR><BR>
 * Classes that implement this interface ideally provide a runtime representation of the resulting report when
 * overriding toJComponent(). This should be displayable in a Swing Panel and not be null. At the worst,
 * a text explaining that the report is not available in this format should be given.
 *
 * @author Simon (Juliez)
 */
public interface CalculationReport {

    enum LineType {
        LINE, HEADER, SUBHEADER, RESULT_LINE, EMPTY
    }

    /**
     * Adds a single line to the CalculationReport.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param calculation A calculation or other info, displayed after the type
     * @param result A result or other info, displayed on the right side
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    CalculationReport addLine(@Nullable String type, @Nullable String calculation, @Nullable String result);

    /**
     * Adds a single line to the CalculationReport. This method performs rounding to
     * a single decimal digit on the result and writes resultPrefix in front of it.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param calculation A calculation or other info, displayed after the type
     * @param resultPrefix A text to be display immediately in front of the result, such as "= "
     * @param result A numerical result which will be rounded to a single decimal, e.g. 25.1
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addLine(@Nullable String type, @Nullable String calculation,
                              @Nullable String resultPrefix, double result) {
        return addLine(type, calculation, getRoundedResultString(resultPrefix, result));
    }

    /**
     * Adds a single line to the CalculationReport. This method performs rounding to
     * a single decimal digit on the result and writes resultPrefix in front of it.
     * This line has only two elements, the type ("Damage: ") and the result displayed
     * on the right side.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param resultPrefix A text to be display immediately in front of the result, such as "= "
     * @param result A numerical result which will be rounded to a single decimal, e.g. 25.1
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addLine(@Nullable String type,
                                      @Nullable String resultPrefix, double result) {
        return addLine(type, "", getRoundedResultString(resultPrefix, result));
    }

    /**
     * Adds a single line to the CalculationReport in the way addLine() does, except
     * the result has a line above it that may e.g. indicate a summary of previous values.
     * This method performs rounding to a single decimal digit on the result and
     * writes resultPrefix in front of it.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param calculation A calculation or other info, displayed after the type
     * @param resultPrefix A text to be display immediately in front of the result, such as "= "
     * @param result A numerical result which will be rounded to a single decimal, e.g. 25.1
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addResultLine(@Nullable String type, @Nullable String calculation,
                                      @Nullable String resultPrefix, double result) {
        return addResultLine(type, calculation, getRoundedResultString(resultPrefix, result));
    }

    /**
     * Adds a single line to the CalculationReport in the way addLine() does, except
     * the result has a line above it that may e.g. indicate a summary of previous values.
     * This line has only two elements, the type ("Damage: ") and the result displayed
     * on the right side. This method performs rounding to a single decimal digit on the
     * result and writes resultPrefix in front of it.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param resultPrefix A text to be display immediately in front of the result, such as "= "
     * @param result A numerical result which will be rounded to a single decimal, e.g. 25.1
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addResultLine(@Nullable String type,
                                            @Nullable String resultPrefix, double result) {
        return addResultLine(type, "", getRoundedResultString(resultPrefix, result));
    }

    /**
     * Adds a single line to the CalculationReport in the way addLine() does, except
     * the result has a line above it that may e.g. indicate a summary of previous values.
     * This line has only one element, the result displayed on the right side. This method
     * performs rounding to a single decimal digit on the result and writes resultPrefix
     * in front of it.
     *
     * @param resultPrefix A text to be display immediately in front of the result, such as "= "
     * @param result A numerical result which will be rounded to a single decimal, e.g. 25.1
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addResultLine(@Nullable String resultPrefix, double result) {
        return addResultLine("", "", getRoundedResultString(resultPrefix, result));
    }

    /**
     * Adds an empty line to the CalculationReport.
     *
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addEmptyLine() {
        return addLine("", "", "");
    }

    /**
     * Adds a single line to the CalculationReport. This line has only two elements, the type
     * ("Damage: ") and a result displayed on the right side.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param result A result or other info, displayed on the right side
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addLine(@Nullable String type, @Nullable String result) {
        return addLine(type, "", result);
    }

    /**
     * Adds a single line to the CalculationReport. This line only has one element displayed
     * on the right side.
     *
     * @param result A result or other info, displayed on the right side
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    default CalculationReport addLine(@Nullable String result) {
        return addLine("", "", result);
    }

    /**
     * Adds a single line to the CalculationReport containing a sub-header.
     *
     * @param text The header text
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    CalculationReport addSubHeader(String text);

    /**
     * Adds a single line to the CalculationReport containing the header for the CalculationReport.
     * This would typically be used as the first line but can be used anywhere in the CalculationReport
     * and multiple times.
     *
     * @param text The header text
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    CalculationReport addHeader(String text);

    /**
     * Adds a single line to the CalculationReport in the way addLine() does, except
     * the result has a line above it that may e.g. indicate a summary of previous values.
     *
     * @param type The first element of this line, such as "Damage: "
     * @param calculation A calculation or other info, displayed after the type
     * @param result A result or other info, displayed on the right side
     * @return The CalculationReport itself. Enables multiple stringed addLine calls
     */
    CalculationReport addResultLine(@Nullable String type, @Nullable String calculation, @Nullable String result);

    /**
     * Returns the CalculationReport as a JComponent that can be added to a dialog or other Swing component.
     *
     * @return The CalculationReport wrapped in a JComponent form.
     */
    JComponent toJComponent();

    /**
     * Returns the assembled rounded result value with the prefix applied. Uses the fixed Locale.US
     * as the Java way of converting "" + value seems to use Locale.US by default as well.
     */
    private String getRoundedResultString(String resultPrefix, double result) {
        return ((resultPrefix != null) ? resultPrefix : "")
                + String.format(Locale.US, "%1$,.1f", result);
    }

    /**
     * Formats the given double with only the necessary digits and at most three digits. Uses the fixed Locale.US
     * as the Java way of converting "" + value seems to use Locale.US by default as well.
     */
    static String formatForReport(double d) {
        long timesThousand = Math.round(1000 * d);
        if (timesThousand % 1000 == 0) {
            return String.format(Locale.US, "%1$.0f", d);
        } else if (timesThousand % 100 == 0) {
            return String.format(Locale.US, "%1$.1f", d);
        } else if (timesThousand % 10 == 0) {
            return String.format(Locale.US, "%1$.2f", d);
        } else {
            return String.format(Locale.US, "%1$.3f", d);
        }
    }

    /**
     * Starts a report section that is tentative, i.e. that may be added or discarded depending
     * on whether {@link #endTentativeSection()} or {@link #discardTentativeSection()} is called
     * at a later point.
     * All lines added to the report after calling this method are kept separate until either
     * {@link #endTentativeSection()} or {@link #discardTentativeSection()} is called.
     * When subsequently {@link #endTentativeSection()} is called, the lines of the section are
     * written to the report normally.
     * When subsequently {@link #discardTentativeSection()} is called, all lines in the section
     * are discarded (not written to the report).
     * Note that calling this method multiple times has no further effect. The first consecutive
     * call of this method stays the one that marks the start of the section. Not more than a
     * single section is maintained at any time.
     */
    void startTentativeSection();

    /**
     * End the current section of lines, writing them to the report normally.
     * Note that when a section has been ended and no new section begun, calling this method again
     * has no effect.
     */
    void endTentativeSection();

    /**
     * Discard all lines written to this section (all lines added to the report after calling
     * {@link #startTentativeSection()}).
     * Note that when a section has been ended and no new section begun, calling this method again
     * has no effect.
     */
    void discardTentativeSection();

    /**
     * End the current section of lines, keeping or discarding the section depending on the given
     * keepSection. Calls either {@link #endTentativeSection()} or {@link #discardTentativeSection()}.
     *
     * @param keepSection When true, keeps the current section, otherwise discards it.
     */
    default void finalizeTentativeSection(boolean keepSection) {
        if (keepSection) {
            endTentativeSection();
        } else {
            discardTentativeSection();
        }
    }
}