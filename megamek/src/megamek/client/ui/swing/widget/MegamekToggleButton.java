package megamek.client.ui.swing.widget;

public class MegamekToggleButton extends MegamekButton {

    public MegamekToggleButton(String text, String component) {
        super(text, component);
    }

    @Override
    public boolean isSelected()
    {
        return super.isSelected();
    }

    @Override
    public void setSelected(boolean value)
    {
        super.setSelected(value);
        if (value) {}
    }
}
