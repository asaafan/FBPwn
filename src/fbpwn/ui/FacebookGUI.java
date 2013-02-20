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

import fbpwn.core.FacebookTask;

/**
 * Represents Facebook GUI used for updating the status of each task 
 */
public interface FacebookGUI {

    /**
     * Updates a given task
     * @param task The task to be updated
     */
    public void updateTaskProgress(FacebookTask task);

    /**
     * Adds a new task to the GUI
     * @param task The task to be added
     */
    public void addTask(FacebookTask task);

    /**
     * Removes a given task from the GUI
     * @param task The task to be removed
     */
    public void removeTask(FacebookTask task);
}
