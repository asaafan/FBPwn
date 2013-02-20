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

import fbpwn.plugins.ui.AddVictimsFriendsDialog;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.core.FacebookException;
import fbpwn.ui.FacebookGUI;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Represents Adding his friends task 
 */
public class AddVictimsFriends extends FacebookTask {

    /**
     * Creates a new add friends task
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public AddVictimsFriends(FacebookGUI FacebookGUI,
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
            final ArrayList<FacebookAccount> victimsFriendList = getFacebookTargetProfile().getFriends();

            if (victimsFriendList == null) {
                setMessage("Failed to get friend list");
                setPercentage(0.0);
                setTaskState(FacebookTaskState.Finished);
                getFacebookGUI().updateTaskProgress(this);
                return true;
            }

            final ArrayList<FacebookAccount> choosenFriends = new ArrayList<FacebookAccount>();


            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        AddVictimsFriendsDialog dialog = new AddVictimsFriendsDialog(null, true, victimsFriendList);
                        ArrayList<FacebookAccount> choosen = dialog.showSelectionDialog();
                        for (int i = 0; i < choosen.size(); i++) {
                            choosenFriends.add(choosen.get(i));
                        }
                    }
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            }

            if (choosenFriends.size() != 0) {
                setMessage("Sending Request to " + choosenFriends.size() + " profile(s)");
                for (int i = 0; i < choosenFriends.size(); i++) {
                    if (checkForCancel()) {
                        return true;
                    }
                    getAuthenticatedProfile().sendFriendRequest(choosenFriends.get(i).getProfilePageURL());
                    setPercentage(((double) (i + 1) / choosenFriends.size()) * 100);
                    getFacebookGUI().updateTaskProgress(this);
                }
            }

            setTaskState(FacebookTaskState.Finished);
            setMessage("Finished");
            setPercentage(100.0);
            getFacebookGUI().updateTaskProgress(this);
            return true;

        } catch (IOException ex) {
            Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        } catch (FacebookException ex) {
            Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        }
    }

    @Override
    public String toString() {
        return "Add victim's friends";
    }
}
