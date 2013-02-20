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
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

/**
 * Represents the dump thumbnail images task  
 */
public class DumpThumbnailImagesTask extends FacebookTask {

    /**
     * Creates a new dump thumbnail images task
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public DumpThumbnailImagesTask(FacebookGUI FacebookGUI,
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

    private void processPhotos(HtmlPage PhotosPage, String FileDirectory, String Process) throws IOException {
        Pattern PhotoURL = Pattern.compile("url\\(http.*\\)");
        Matcher MatchedURL = PhotoURL.matcher(PhotosPage.asXml());
        ArrayList<String> images = new ArrayList<String>();
        while (MatchedURL.find()) {
            images.add(MatchedURL.group().substring(4, MatchedURL.group().length() - 1));
        }
        for (int i = 0; i < images.size(); i++) {

            if (checkForCancel()) {
                return;
            }

            FileUtils.copyURLToFile(new URL(images.get(i)), new File(FileDirectory + System.getProperty("file.separator") + "Image" + (i + 1) + ".jpg"));
            setMessage(Process);
            setPercentage(((double) (i + 1) / images.size()) * 100);
            getFacebookGUI().updateTaskProgress(this);
        }
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

            HtmlPage taggedPhotosPage = getFacebookTargetProfile().getBrowser().getPage(getFacebookTargetProfile().getTaggedPhotosURL());
            processPhotos(taggedPhotosPage, getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "TaggedPhotos", "Dumping tagged photos");
            ArrayList<String> albumsURLs = getFacebookTargetProfile().getAlbumsURLs();
            for (int i = 0; i < albumsURLs.size(); i++) {
                processPhotos((HtmlPage) getFacebookTargetProfile().getBrowser().getPage(albumsURLs.get(i)), getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Album" + (i + 1), "Dumping Album: " + (i + 1) + "/" + albumsURLs.size());
                if (checkForCancel()) {
                    return true;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DumpThumbnailImagesTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        }
        setTaskState(FacebookTaskState.Finished);
        setMessage("Finished");
        setPercentage(100.0);
        getFacebookGUI().updateTaskProgress(this);
        return true;
    }

    @Override
    public String toString() {
        return "Dump all photos";
    }
}
