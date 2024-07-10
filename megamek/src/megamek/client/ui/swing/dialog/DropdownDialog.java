package megamek.client.ui.swing.dialog;

import java.awt.Container;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.baseComponents.MMComboBox;

/**
 * This class displays a single dropdown from which we can make a single choice
 * @param <T>
 */
public class DropdownDialog<T> extends AbstractDialog {

	List<T> dataSource;
	MMComboBox comboBox;
	
	public DropdownDialog(JFrame frame, String name, String title, List<T> data) {
		super(frame, name, title);
		dataSource = data;
		initialize();
	}

	@Override
	protected Container createCenterPane() {
		comboBox = new MMComboBox(getName(), dataSource);
		comboBox.setSelectedIndex(0);
		
		JPanel result = new JPanel();
		result.add(comboBox);
		return result;
	}

	/**
	 * Returns the item that is currently selected in the combobox.
	 */
	public T getSelectedItem() {
		return (T) comboBox.getSelectedItem();
	}
}
