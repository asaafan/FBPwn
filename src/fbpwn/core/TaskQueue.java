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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a queue of tasks 
 */
public class TaskQueue implements Runnable {

    private ArrayList<FacebookTask> taskQueue = new ArrayList<FacebookTask>();
    private double pollingTime;

    /**
     * Creates a new queue
     * @param pollingTime the delay between retrying on task failure
     */
    public TaskQueue(double pollingTime) {
        this.pollingTime = pollingTime * 1000;
    }

    /**
     * Adds a new task to this queue
     * @param newTask the task to be added
     */
    public void addTask(FacebookTask newTask) {        
        taskQueue.add(newTask);
        newTask.init();
    }

    /**
     * Removes a task from this queue
     * @param deletedTask the task to be removed
     */
    public void removeTask(FacebookTask deletedTask) {
        taskQueue.remove(deletedTask);
    }

    @Override
    public void run() {
        for (FacebookTask task : taskQueue) {
            while (true) {
                if (task.isDeleted()) {
                    break;
                }

                Logger.getLogger(TaskQueue.class.getName()).log(Level.INFO, "Running module: " + task.toString());
                if (task.run() == true) {
                    break;
                }
                try {
                    Thread.sleep((long) pollingTime);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TaskQueue.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
                }
            }
        }
    }
}
