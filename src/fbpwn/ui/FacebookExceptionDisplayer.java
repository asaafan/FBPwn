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

import fbpwn.core.FacebookException;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author hussein
 */
public class FacebookExceptionDisplayer {

    /**
     * 
     * @param facebookException
     */
    public static void displayException(final FacebookException facebookException) {
        final File errorFileFinal = facebookException.getErrorFile();

        int choice = 0;
        String errorMessage = facebookException.getMessage();

        if (errorFileFinal == null) {
            errorMessage += "\nFailed to dump error file";

            JOptionPane.showMessageDialog(null, errorMessage, "Error Occurred", JOptionPane.ERROR_MESSAGE);

        } else {
            try {
                errorMessage += "\nError was dumped to:\n" + errorFileFinal.getAbsolutePath() + "\nDo you want to view this error";

                choice = JOptionPane.showConfirmDialog(null, errorMessage, "Error Occurred", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (choice == 0) {
                    Desktop.getDesktop().open(errorFileFinal);
                }
            } catch (IOException ex) {
                Logger.getLogger(FacebookExceptionDisplayer.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            }

        }
    }
}
