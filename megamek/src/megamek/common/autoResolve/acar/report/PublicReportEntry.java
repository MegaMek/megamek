/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.autoResolve.acar.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Report;
import megamek.common.interfaces.ReportEntry;
import megamek.common.rolls.Roll;


public class PublicReportEntry implements ReportEntry {

    record DataEntry(String data, boolean isObscured) implements Serializable {}

    private final String messageId;
    private final List<DataEntry> data = new ArrayList<>();
    private boolean endLine = true;
    private boolean endSpace = false;
    private int indentation = 0;
    protected static final String EMPTY = "";

    public PublicReportEntry(String messageId) {
        this.messageId = messageId;
    }

    protected PublicReportEntry() {
        this(null);
    }

    /**
     * Add the given int to the list of data that will be substituted for the &lt;data&gt; tags in the report. The order
     * in which items are added must match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     *
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(int data) {
        return add(String.valueOf(data), true);
    }

    /**
     * Add the given int to the list of data that will be substituted for the &lt;data&gt; tags in the report, and mark
     * it as double-blind sensitive information if <code>obscure</code> is true. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data    the int to be substituted
     * @param obscure boolean indicating whether the data is double-blind sensitive
     *
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(int data, boolean obscure) {
        return add(String.valueOf(data), obscure);
    }

    /**
     * Add the given String to the list of data that will be substituted for the &lt;data&gt; tags in the report. The
     * order in which items are added must match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     *
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(String data) {
        return add(data, true);
    }

    /**
     * Add the given String to the list of data that will be substituted for the &lt;data&gt; tags in the report, and
     * mark it as double-blind sensitive information if <code>obscure</code> is true. The order in which items are added
     * must match the order of the tags in the report text.
     *
     * @param data    the String to be substituted
     * @param obscure boolean indicating whether the data is double-blind sensitive
     *
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(String data, boolean obscure) {
        this.data.add(new DataEntry(data, obscure));
        return this;
    }

    @Override
    public final String text() {
        return "&ensp;".repeat(indentation) + reportText() + lineEnd();
    }

    @Override
    public ReportEntry addRoll(Roll roll) {
        return this;
    }

    /**
     * Indent the report. Equivalent to calling {@link #indent(int)} with a parameter of 1.
     *
     * @return This Report to allow chaining
     */
    public PublicReportEntry indent() {
        return indent(1);
    }

    /**
     * Indent the report n times.
     *
     * @param n the number of times to indent the report
     *
     * @return This Report to allow chaining
     */
    public PublicReportEntry indent(int n) {
        indentation += (n * Report.DEFAULT_INDENTATION);
        return this;
    }

    public PublicReportEntry noNL() {
        endLine = false;
        return this;
    }

    public PublicReportEntry endSpace() {
        endSpace = true;
        return this;
    }

    public PublicReportEntry addNL() {
        endLine = true;
        return this;
    }

    private String lineEnd() {
        return (endSpace ? "&nbsp;" : EMPTY) + (endLine ? "<br>" : EMPTY);
    }

    protected String reportText() {
        return ReportMessages.getString(messageId, data.stream().map(DataEntry::data).toArray());
    }
}
