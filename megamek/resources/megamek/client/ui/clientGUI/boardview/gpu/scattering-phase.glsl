// Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later
// Henyey-Greenstein relative to isotropic scattering (4*pi*p). Both inputs are unit vectors:
// view points from the camera into the scene, sun from the sample toward the light.
// Positive cosine therefore means looking toward the sun, where forward scattering is strongest.
float scatteringPhase(float cosine, float g) {
    float denominator = max(0.001, 1.0 + g * g - 2.0 * g * clamp(cosine, -1.0, 1.0));
    return (1.0 - g * g) / (denominator * sqrt(denominator));
}
