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
 * but WITHOUT ANY   WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fbpwn.core;

import fbpwn.ui.FacebookGUI;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Facebook task 
 */
public abstract class FacebookTask {

    private FacebookGUI facebookGUI; //Facebook GUI required for updating interface
    private FacebookAccount facebookTargetProfile;  //FacebookAccount on which the tasks are performed
    private AuthenticatedAccount authenticatedProfile; //AuthenticatedAccount which performs the given tasks
    private File directory; // Directorgy for dumping informations 
    // Update Task status
    private String message = "Pending";
    private Double percentage = 0.0;
    private FacebookTaskState taskState = FacebookTaskState.Waiting;
    private boolean isCancelled = false;
    private boolean isDeleted = false;

    /**
     * Creates a new task
     * @param facebookGUI The GUI used for updating the status of this task
     * @param facebookTargetProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param directory The directory used in dumping the information
     */
    public FacebookTask(FacebookGUI facebookGUI, FacebookAccount facebookTargetProfile,
            AuthenticatedAccount authenticatedProfile, File directory) {
        this.facebookGUI = facebookGUI;
        this.facebookTargetProfile = facebookTargetProfile;
        this.authenticatedProfile = authenticatedProfile;
        this.directory = directory;
        facebookGUI.addTask((FacebookTask)this);
        taskState = FacebookTaskState.Waiting;        
    }

    /**
     * Run this task
     * @return true on success, false to retry after some delay
     */
    public abstract boolean run();

    /**
     * Initialize this task, called once the task is added to the queue 
     */
    public abstract void init();

    @Override
    public abstract String toString();

    /**
     * Gets the Facebook GUI associated with this task.
     * Used for updating the status
    @return The FacebookGUI Used for updating the status of this task
     */
    public FacebookGUI getFacebookGUI() {
        return facebookGUI;
    }

    /**
     * Gets the victim's profile
     * @return the victim's profile    
     */
    public FacebookAccount getFacebookTargetProfile() {
        return facebookTargetProfile;
    }

    /**
     * Gets the attacker's profile
     * @return The attackers profile
     */
    public AuthenticatedAccount getAuthenticatedProfile() {
        return authenticatedProfile;
    }

    /**
     * Checks if the task is finished or canceled
     * @return true - Task is finished or canceled
     */
    public boolean isFinished() {
        if (taskState == FacebookTaskState.Finished || taskState == FacebookTaskState.Cancelled) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the output directory
     * @return The output directory used in dumping all the information
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Gets the current status of this task
     * @return The status message of this task
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the status message for this task
     * @param message The status message to be set for this task
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the current progress of this task
     * @return The current progress of this task
     */
    public Double getPercentage() {
        return percentage;
    }

    /**
     * Sets the current progress of this task
     * @param percentage The progress to set for this task
     */
    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    /**
     * Returns the current state of the task
     * @return the taskState
     */
    public FacebookTaskState getTaskState() {
        return taskState;
    }

    /**
     * Sets the current state of the task
     * @param taskState the taskState to set
     */
    public void setTaskState(FacebookTaskState taskState) {
        this.taskState = taskState;
    }

    /**
     * Sends the cancel signal
     */
    public void sendCancelSignal() {
        if (taskState == FacebookTaskState.Running) {
            isCancelled = true;
            setTaskState(FacebookTaskState.Cancelling);
            facebookGUI.updateTaskProgress(this);
        }
    }

    /**
     * Checks for the cancel signal, if cancelled, updates the state of
     * the task properly in the GUI
     * @return True if cancelled, false otherwise
     */
    protected boolean checkForCancel() {
        if (isCancelled) {
            setTaskState(FacebookTaskState.Cancelled);
            facebookGUI.updateTaskProgress(this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks to see if the current task is in the running state
     * @return True if running, false otherwise
     */
    public boolean isRunning() {
        if (taskState == FacebookTaskState.Running) {
            return true;
        }
        return false;
    }

    /**
     * Marks this task as removed
     */
    public void deleteTask() {
        isDeleted = true;
    }

    /**
     * Checks to see if this task is marked as deleted
     * @return true if marked as deleted, false otherwise
     */
    public boolean isDeleted() {
        return isDeleted;
    }
}
