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

import javax.swing.*;

/**
 * This is a Calculation Report that can be used when the report that comes with a
 * calculation isn't going to be used. It saves time and memory by doing nothing with any passed parameters.
 * Its toString() and toJComponent() methods yield a short explanatory message as String and
 * JLabel, respectively, and so can be called safely.
 */
public class DummyCalculationReport implements CalculationReport {

    @Override
    public CalculationReport addLine(String type, String calculation, String result) {
        return this;
    }

    @Override
    public CalculationReport addLine(String type, String calculation, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addLine(String type, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String resultPrefix, double result) {
        return this;
    }

    @Override
    public CalculationReport addEmptyLine() {
        return this;
    }

    @Override
    public CalculationReport addLine(String type, String result) {
        return this;
    }

    @Override
    public CalculationReport addLine(String result) {
        return this;
    }

    @Override
    public CalculationReport addSubHeader(String text) {
        return this;
    }

    @Override
    public CalculationReport addHeader(String text) {
        return this;
    }

    @Override
    public CalculationReport addResultLine(String type, String calculation, String result) {
        return this;
    }

    @Override
    public JComponent toJComponent() {
        return new JLabel("This is an intentionally calculation report.");
    }

    @Override
    public void startTentativeSection() { }

    @Override
    public void endTentativeSection() { }

    @Override
    public void discardTentativeSection() { }

    @Override
    public String toString() {
        return "This is an intentionally empty calculation report.";
    }
}
