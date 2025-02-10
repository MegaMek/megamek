/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.ai.utility;

import java.util.StringJoiner;

public class DebugReporter implements IDebugReporter {
    private final StringBuilder stringBuilder;
    private static final String LINE_BREAK = "\n";
    private static final String TAB = "\t";

    public DebugReporter(int size) {
        stringBuilder = new StringBuilder(size);
    }

    @Override
    public IDebugReporter append(Consideration<?, ?> consideration) {
        stringBuilder.append(consideration.getName());
        return this;
    }

    @Override
    public IDebugReporter append(String s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public IDebugReporter newLine(int i) {
        stringBuilder.append(LINE_BREAK.repeat(Math.max(0, i)));
        return this;
    }

    @Override
    public IDebugReporter indent(int i) {
        stringBuilder.append(TAB.repeat(Math.max(0, i)));
        return this;
    }

    @Override
    public IDebugReporter newLine() {
        return newLine(1);
    }

    @Override
    public IDebugReporter indent() {
        return indent(1);
    }

    @Override
    public IDebugReporter newLineIndent(int indent) {
        return newLine().indent(indent);
    }

    @Override
    public IDebugReporter newLineIndent() {
        return newLine().indent();
    }

    @Override
    public IDebugReporter append(double s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public IDebugReporter append(int s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public IDebugReporter append(float s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public IDebugReporter append(boolean s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public IDebugReporter append(Object s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public IDebugReporter append(long s) {
        stringBuilder.append(s);
        return this;
    }

    @Override
    public String getReport() {
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DebugReporter.class.getSimpleName() + "[", "]")
            .add("stringBuilder=").add(stringBuilder).toString();
    }
}
