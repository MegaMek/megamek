package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import megamek.client.ui.swing.GUIPreferences;

public class ConstructionFactorWarningTest {

	@Test
	public void testDefaultPreferenceForCFWarningIndicator() {
		// The default setting for Construction Factor warning indicators should be true.
		GUIPreferences GUIP = GUIPreferences.getInstance();
		assertTrue(GUIP.getDefaultBoolean(GUIPreferences.CONSTRUCTOR_FACTOR_WARNING));
	}

	@Test
	public void testSetPreferenceForCFWarningIndicator() {
		// Set the preference for CF warning indicator to false and true.
		GUIPreferences GUIP = GUIPreferences.getInstance();

		// Set preference to false and test that is retrieved as false.
		GUIP.setShowCFWarnings(false);
		assertFalse(GUIP.getShowCFWarnings());

		// Set back to true and test again.
		GUIP.setShowCFWarnings(true);
		assertTrue(GUIP.getShowCFWarnings());
	}
}