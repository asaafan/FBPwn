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

import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.ui.FacebookGUI;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the dump friends task 
 */
public class DumpFriendsTask extends FacebookTask {

    /**
     * Creates a new dump friends task
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public DumpFriendsTask(FacebookGUI FacebookGUI,
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
            getFacebookGUI().updateTaskProgress(this);
            setMessage("Getting friend list");
            setPercentage(0.0);
            getFacebookGUI().updateTaskProgress(this);
            ArrayList<FacebookAccount> allfriends = getFacebookTargetProfile().getFriends();

            if (checkForCancel()) {
                return true;
            }
            
            if (allfriends == null) {
                setMessage("Failed to get friend list");
                setPercentage(0.0);
                setTaskState(FacebookTaskState.Finished);
                getFacebookGUI().updateTaskProgress(this);
                return true;
            }
            
            
            // Saving the friends' name and image list in an html file
            PrintWriter friendsWriter = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Friends.html"), "UTF-8");
            setMessage("Dumping friend list");
            setPercentage(0.0);
            getFacebookGUI().updateTaskProgress(this);
            friendsWriter.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 3.2//EN\">");
            friendsWriter.println("<html>");
            friendsWriter.println("<head>");
            friendsWriter.println();
            friendsWriter.println("<meta http-equiv = \"Content-Type\" content = \"text/html; charset=UTF-8\">");
            friendsWriter.println();
            friendsWriter.println("</head>");
            friendsWriter.println("<body>");
            friendsWriter.println("<div align=\"center\">");
            friendsWriter.println("<table border=\"1\">");
            for (int i = 0; i < allfriends.size(); i++) {
                friendsWriter.println("<tr>");
                friendsWriter.println("<td>");
                friendsWriter.println("<a href=\"" + allfriends.get(i).getProfilePageURL() + "\">" + allfriends.get(i).getName() + "<//a>");
                friendsWriter.println("</td>");
                friendsWriter.println("<td>");
                friendsWriter.println("<img src=\"" + allfriends.get(i).getProfilePhotoURL() + "\" alt=\"\" />");
                friendsWriter.println("</td>");
                friendsWriter.println("</tr>");
                setMessage("Dumping friend list");
                setPercentage((double) i + 1 / allfriends.size());
                getFacebookGUI().updateTaskProgress(this);
            }
            friendsWriter.println("</table>");
            friendsWriter.println("</div>");
            friendsWriter.println("</body>");
            friendsWriter.println("</html>");
            friendsWriter.flush();
            friendsWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(DumpFriendsTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        }
        setMessage("Finished");
        setPercentage(100.0);
        setTaskState(FacebookTaskState.Finished);
        getFacebookGUI().updateTaskProgress(this);
        return true;
    }

    @Override
    public String toString() {
        return "Dump friend list";
    }
}