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
package fbpwn.core;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static boolean appendToLog = true;

    public static void reportException(Thread t, Throwable e) {
        
        appendToLog = false;
        new ExceptionHandler().uncaughtException(t, e);
        appendToLog = true;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {

        final Thread finalT = t;
        final Throwable finalE = e;

        if (SwingUtilities.isEventDispatchThread()) {

            if (appendToLog) {
                Logger.getLogger(ExceptionHandler.class.getName()).
                        log(Level.SEVERE, "Exception in thread: " + t.getName(), e);
            }

            JOptionPane.showMessageDialog(null,
                    "An exception occurred\n"
                    + "The full exception message and stacktrace are dumped to \"log.html\" file\n"
                    + "If you are not expecting this error, kindly report it to the developers and attach the log file\n"
                    + "FBPwn will now exit",
                    "Exception occurred",
                    JOptionPane.ERROR_MESSAGE);

        } else {
            try {

                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {

                        if (appendToLog) {
                            Logger.getLogger(ExceptionHandler.class.getName()).
                                    log(Level.SEVERE, "Exception in thread: " + finalT.getName(), finalE);
                        }
                        JOptionPane.showMessageDialog(null,
                                "An exception occurred\n"
                                + "The full exception message and stacktrace are dumped to \"log.html\" file\n"
                                + "If you are not expecting this error, kindly report it to the developers and attach the log file\n"
                                + "FBPwn will now exit",
                                "Exception occurred",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(ExceptionHandler.class.getName()).log(Level.SEVERE, "Exception in thread: " + t.getName(), ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(ExceptionHandler.class.getName()).log(Level.SEVERE, "Exception in thread: " + t.getName(), ex);
            }
        }
        System.exit(1);
    }
}
