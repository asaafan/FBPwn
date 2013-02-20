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
package fbpwn.plugins.core;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.ui.FacebookGUI;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the dump information task  
 */
public class DumpInfoTask extends FacebookTask {

    /**
     * Creates a new dump information task
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public DumpInfoTask(FacebookGUI FacebookGUI,
            FacebookAccount facebookProfile,
            AuthenticatedAccount authenticatedProfile,
            File workingDirectory) {
        super(FacebookGUI, facebookProfile, authenticatedProfile, workingDirectory);
    }

    /**
     * Initialize this task, called once the task is added to the queue 
     */
    @Override
    public void init() {
    }

    /**
     * Runs this task
     * @return true if completed, false if error occurred so that it will be rerun after a small delay.
     */
    @Override
    public boolean run() {
        try {
            setTaskState(FacebookTaskState.Running);
            setMessage("Dumping information");
            setPercentage(0.0);
            getFacebookGUI().updateTaskProgress(this);

            HtmlPage infoPage = getFacebookTargetProfile().getBrowser().getPage(getFacebookTargetProfile().getInfoPageURL());
            if (checkForCancel()) {
                return true;
            }
            // Saving the info page as html file
            PrintWriter infoWriterAsXML = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "InfoPage.html"));
            infoWriterAsXML.print(infoPage.getElementById("contentArea").asXml());
            infoWriterAsXML.flush();
            infoWriterAsXML.close();
            setPercentage(50.0);

            // Saving the info page as text file
            PrintWriter infoWriterAsText = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "InfoPage.txt"));
            infoWriterAsText.print(infoPage.getElementById("contentArea").asText());
            infoWriterAsText.flush();
            infoWriterAsText.close();

        } catch (IOException ex) {
            Logger.getLogger(DumpInfoTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        }
        
        //Updating the GUI
        setTaskState(FacebookTaskState.Finished);
        setMessage("Finished");
        setPercentage(100.0);
        getFacebookGUI().updateTaskProgress(this);
        return true;
    }

    @Override
    public String toString() {
        return "Dump profile info";
    }
}
