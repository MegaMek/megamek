package megamek.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import megamek.client.ui.swing.GUIPreferences;

public class ConstructionFactorWarningTest {

	@Test
	public void testDefaultPreferenceForCFWarningIndicator() {
		//The default setting for Construction Factor warning indicators should be true.
		GUIPreferences GUIP = GUIPreferences.getInstance();

		boolean bCFWarning = GUIP.getShowCFWarnings();
		assertTrue(bCFWarning);
	}
}
