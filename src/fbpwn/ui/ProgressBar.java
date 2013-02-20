/*
 * FBPwn
 * 
 * http://code.google.com/p/fbpwn
 * 
 * Copyright (C) 2011 - FBPwn
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fbpwn.ui;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * A class represents a custom progress bar, to be inserted in a JTable
 */
public class ProgressBar extends JProgressBar implements TableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JComponent) {
            return (JComponent) value;
        } else {
            return null;
        }
    }

    @Override
    public void setValue(int n) {
        super.setValue(n);
    }
}

class MyDefaultTableModel extends DefaultTableModel {

    public MyDefaultTableModel(Object[] obj, int i) {
        super(obj, i);

    }

    public boolean isCellEditable(int row, int column) {
        if (row == 0 && column == 1) {
            return false;
        } else {
            return true;
        }
    }
}
