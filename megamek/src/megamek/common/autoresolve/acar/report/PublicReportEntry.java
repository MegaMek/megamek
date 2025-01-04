/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.report;

import megamek.common.Report;
import megamek.common.ReportEntry;
import megamek.common.Roll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class PublicReportEntry implements ReportEntry {

    record DataEntry(String data, boolean isObscured) implements Serializable { }

    private final int messageId;
    private final List<DataEntry> data = new ArrayList<>();
    private boolean endLine = true;
    private boolean endSpace = false;
    private int indentation = 0;

    public PublicReportEntry(int messageId) {
        this.messageId = messageId;
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(int data) {
        return add(String.valueOf(data), true);
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data    the int to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *                sensitive
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(int data, boolean obscure) {
        return add(String.valueOf(data), obscure);
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(String data) {
        return add(data, true);
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data    the String to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *                sensitive
     * @return This Report to allow chaining
     */
    public PublicReportEntry add(String data, boolean obscure) {
        this.data.add(new DataEntry(data, obscure));
        return this;
    }

    @Override
    public final String text() {
        return " ".repeat(indentation) + reportText() + lineEnd();
    }

    @Override
    public ReportEntry addRoll(Roll roll) {
        return this;
    }

    /**
     * Indent the report. Equivalent to calling {@link #indent(int)} with a
     * parameter of 1.
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
        return (endSpace ? " " : "") + (endLine ? "<BR>" : "");
    }

    protected String reportText() {
        return ReportMessages.getString(String.valueOf(messageId), data.stream().map(d -> (Object) d.data).toList());
    }
}
