package megamek.client.ui.swing.table;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


public class MegamekTable extends JTable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final static int KEY_TIMEOUT = 1000;
    
    private long lastSearch;
    
    StringBuffer searchBuffer;
    
    /**
     * Determines which column in the table model will be used for searches.
     */
    protected int searchColumn;
    
    
    public MegamekTable()
    {
        super();
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }
    
    public MegamekTable(int numRows, int numColumns) {
        super(numRows,numColumns);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public MegamekTable(Object[][] rowData, Object[] columnNames) {
        super(rowData,columnNames);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public MegamekTable(TableModel dm) {
        super(dm);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }
    
    public MegamekTable(TableModel dm, int sc) {
        super(dm);
        lastSearch = 0;
        searchColumn = sc; 
        searchBuffer = new StringBuffer();
    }

    public MegamekTable(TableModel dm, TableColumnModel cm) {
        super(dm,cm);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public MegamekTable(TableModel dm, TableColumnModel cm,
            ListSelectionModel sm) {
        super(dm,cm,sm);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }

    public MegamekTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        lastSearch = 0;
        searchColumn = 0;
        searchBuffer = new StringBuffer();
    }
    
        
    public int getSearchColumn() {
        return searchColumn;
    }

    public void setSearchColumn(int searchColumn) {
        this.searchColumn = searchColumn;
    }

    
    
    /**
     * getToolTipText method that implements cell tooltips.  This is useful for
     * displaying cells that are larger than the column width
     */
    @Override
    public String getToolTipText(MouseEvent e) 
    {
        String tip = null;
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        tip = getValueAt(rowIndex, colIndex).toString();                
        return tip;
    }
    
    public void keyTyped(KeyEvent ke) 
    {
        long curTime = System.currentTimeMillis();
        if ((curTime - lastSearch) > KEY_TIMEOUT) {
            searchBuffer = new StringBuffer();
        }
        lastSearch = curTime;
        searchBuffer.append(ke.getKeyChar());
        searchFor(searchBuffer.toString().toLowerCase());
    }
    
    /**
     * When keys are pressed with focus on this table, they are added to a
     * search buffer, which is then used to search on a predetrmined column for
     * selection.
     * 
     * @param search
     */
    protected void searchFor(String search)
    {
        int rows = getRowCount();
        for (int row = 0; row < rows; row++) 
        {
            String name = (String)getValueAt(row,searchColumn);
            if (name.toLowerCase().startsWith(search)) 
            {
                changeSelection(row, 0, false, false);
                break;
            }
        }
    }
    

}
