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

package megamek.common;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import megamek.common.rolls.Roll;
import megamek.logging.MMLogger;

/**
 * RNG that asks the host to type individual d6 faces for combat dice. Non-d6 {@link #randomInt(int)} and
 * {@link #randomFloat()} calls stay computer-generated so bots, maps, and other engine noise are not prompted.
 */
public class ManualRandom extends MMRandom {
    private static final MMLogger logger = MMLogger.create(ManualRandom.class);

    /**
     * Supplies a batch of d6 faces (values 1-6). Empty means the caller should fall back to a PRNG for this roll.
     *
     * @param purpose human-readable reason for this combat roll, such as "To-hit" or "Hit location"
     */
    @FunctionalInterface
    public interface DieFaceSource {
        Optional<List<Integer>> requestFaces(int nDice, String purpose);
    }

    private final DieFaceSource source;
    private final Random fallback = new Random();
    private final Deque<Integer> pendingFaces = new ArrayDeque<>();

    public ManualRandom() {
        this(new SwingDieFaceSource());
    }

    public ManualRandom(DieFaceSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public Roll d6(int nDice) {
        ensureFaces(nDice);
        return super.d6(nDice);
    }

    @Override
    public Roll d6(int nDice, int keep) {
        ensureFaces(nDice);
        return super.d6(nDice, keep);
    }

    @Override
    public int randomInt(int maxValue) {
        if ((maxValue == 6) && !pendingFaces.isEmpty()) {
            return pendingFaces.removeFirst() - 1;
        }
        return fallback.nextInt(maxValue);
    }

    @Override
    public float randomFloat() {
        return fallback.nextFloat();
    }

    /**
     * Parses a player-typed string of individual d6 faces. Faces must be 1-6 and there must be exactly {@code nDice}
     * of them, separated by spaces, commas, or semicolons.
     */
    public static List<Integer> parseFaces(String input, int nDice) {
        if (nDice <= 0) {
            throw new IllegalArgumentException("Must ask for a positive number of rolls, not " + nDice);
        }
        if ((input == null) || input.isBlank()) {
            throw new IllegalArgumentException("Enter " + nDice + " d6 face(s) from 1 to 6.");
        }
        String[] parts = input.trim().split("[,;\\s]+");
        if (parts.length != nDice) {
            throw new IllegalArgumentException("Expected " + nDice + " die face(s), got " + parts.length + ".");
        }
        List<Integer> faces = new ArrayList<>(nDice);
        for (String part : parts) {
            final int face;
            try {
                face = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Not a number: " + part);
            }
            if ((face < 1) || (face > 6)) {
                throw new IllegalArgumentException("Die faces must be 1-6, not " + face + ".");
            }
            faces.add(face);
        }
        return faces;
    }

    private void ensureFaces(int nDice) {
        if (pendingFaces.size() >= nDice) {
            return;
        }
        if (!ManualDice.shouldPrompt()) {
            fillFromFallback(nDice);
            return;
        }
        Optional<List<Integer>> supplied;
        try {
            supplied = source.requestFaces(nDice, ManualDice.purpose());
        } catch (RuntimeException ex) {
            logger.error(ex, "Manual dice source failed; using computer RNG for this roll.");
            supplied = Optional.empty();
        }
        if (supplied.isPresent()) {
            pendingFaces.addAll(supplied.get());
            return;
        }
        logger.info("No manual dice entered; generating {} d6 with the computer RNG.", nDice);
        fillFromFallback(nDice);
    }

    private void fillFromFallback(int nDice) {
        while (pendingFaces.size() < nDice) {
            pendingFaces.add(fallback.nextInt(6) + 1);
        }
    }

    /**
     * Host-only Swing prompt. Marshals onto the EDT because combat dice are rolled on the server thread.
     */
    static final class SwingDieFaceSource implements DieFaceSource {
        @Override
        public Optional<List<Integer>> requestFaces(int nDice, String purpose) {
            if (GraphicsEnvironment.isHeadless()) {
                return Optional.empty();
            }
            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    return promptOnEdt(nDice, purpose);
                }
                AtomicReference<Optional<List<Integer>>> result = new AtomicReference<>(Optional.empty());
                SwingUtilities.invokeAndWait(() -> result.set(promptOnEdt(nDice, purpose)));
                return result.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.warn("Manual dice prompt interrupted; using computer RNG.");
                return Optional.empty();
            } catch (InvocationTargetException ex) {
                logger.error(ex.getCause(), "Manual dice prompt failed; using computer RNG.");
                return Optional.empty();
            }
        }

        private static Optional<List<Integer>> promptOnEdt(int nDice, String purpose) {
            String heading = headingFromPurpose(purpose);
            String diceHelp = (nDice == 1)
                  ? "Enter 1d6 (a number from 1 to 6):"
                  : "Enter " + nDice + " d6 faces (1-6), separated by spaces:";
            String details = ((purpose == null) || purpose.isBlank()) ? heading : purpose;
            String message = details + "\n\n" + diceHelp;
            String title = "Manual dice — " + heading;
            while (true) {
                String input = (String) JOptionPane.showInputDialog(null,
                      message,
                      title,
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      null,
                      "");
                if (input == null) {
                    return Optional.empty();
                }
                try {
                    return Optional.of(parseFaces(input, nDice));
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(null,
                          ex.getMessage(),
                          title,
                          JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        private static String headingFromPurpose(String purpose) {
            if ((purpose == null) || purpose.isBlank()) {
                return "To-hit";
            }
            int newline = purpose.indexOf('\n');
            return (newline < 0) ? purpose : purpose.substring(0, newline);
        }
    }
}
