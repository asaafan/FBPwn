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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Facebook Exception 
 */
public class FacebookException extends Exception {

    private File errorFile = null;
    private String message = "";

    /**
     * Creates a new exception using the error page and a custom error message
     * @param errorPage The error page
     * @param message A custom error message
     */
    public FacebookException(HtmlPage errorPage, String message) {
        this.message = message;

        PrintWriter errorWriter = null;
        try {
            String errorPath = "Error-"
                    + new SimpleDateFormat("dd-MMMMM-yyyy-HH-mm-ss").format(Calendar.getInstance().getTime());
            errorFile = new File(errorPath + ".html");
            if (errorFile.exists()) {
                errorPath += "-";
            }
            int i = 1;
            while (errorFile.exists()) {
                errorFile = new File(errorPath + (i) + ".html");
                i++;
            }
            errorWriter = new PrintWriter(errorFile);
            String errorPageXml = new StringBuffer(errorPage.asXml()).insert(errorPage.asXml().indexOf("onloadRegister(function (){window.location.href"), "//").toString();
            errorWriter.print(errorPageXml);
        } catch (IOException ex) {
            Logger.getLogger(FacebookException.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            errorFile = null;
        } finally {
            errorWriter.flush();
            errorWriter.close();
        }
    }

    /**
     * Gets the error file's path
     * @return The path of the error file
     */
    public String getErrorFilePath() {
        try {
            return errorFile.getCanonicalPath();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Gets the error file
     * @return The error file
     */
    public File getErrorFile() {
        return errorFile;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
