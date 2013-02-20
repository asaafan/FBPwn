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

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.ui.FacebookGUI;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 * Represents the dump images task 
 */
public class DumpImagesTask extends FacebookTask {

    /**
     * Creates a new dump images task 
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public DumpImagesTask(FacebookGUI FacebookGUI,
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
        setTaskState(FacebookTaskState.Running);
        setMessage("Getting Album List");
        getFacebookGUI().updateTaskProgress(this);
        ArrayList<String> albumsURL = new ArrayList<String>();
        try {
            //Opening Mobile photo page of victime profile
            HtmlPage photosPage = getAuthenticatedProfile().getBrowser().getPage(getFacebookTargetProfile().getTaggedPhotosURL().replace("www", "m").replace("sk", "v"));
            while (true) {
                //Extracting Album links
                List<HtmlAnchor> anchors = photosPage.getAnchors();
                for (int i = 0; i < anchors.size(); i++) {
                    if (anchors.get(i).getHrefAttribute().contains("/media/set")) {

                        if (!anchors.get(i).getHrefAttribute().contains("m.facebook.com")) {
                            albumsURL.add("http://m.facebook.com" + anchors.get(i).getHrefAttribute());
                        } else {
                            albumsURL.add(anchors.get(i).getHrefAttribute());
                        }
                    }
                }
                //checking for Additional album pages
                try {
                    photosPage = getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com" + photosPage.getElementById("m_more_item").getElementsByTagName("a").get(0).getAttribute("href"));
                } catch (Exception ex) {
                    break;
                }

                if (checkForCancel()) {
                    return true;
                }
            }
            //dumping images and comments on each Album
            for (int i = 0; i < albumsURL.size(); i++) {
                setMessage("Dumping Albums " + (i + 1) + "/" + albumsURL.size());
                getFacebookGUI().updateTaskProgress(this);
                HtmlPage album = getAuthenticatedProfile().getBrowser().getPage(albumsURL.get(i));
                processAlbum(album, i + 1);

                if (checkForCancel()) {
                    return true;
                }
            }
            setMessage((albumsURL.isEmpty()) ? "No Albums or Albums are not accesible" : "Finished");
            setTaskState(FacebookTaskState.Finished);
            setPercentage(100.0);
            getFacebookGUI().updateTaskProgress(this);
        } catch (IOException ex) {
            Logger.getLogger(DumpImagesTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
        } catch (FailingHttpStatusCodeException ex) {
            Logger.getLogger(DumpImagesTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
        }
        return true;
    }

    /**
     * Dump Album Images with Comments
     * @param albumPage Mobile album page containing Images with comments
     * @param albumIndex Album index that identify it's folder
     */
    private void processAlbum(HtmlPage albumPage, int albumIndex) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        ArrayList<String> photos = new ArrayList<String>();  //Array of Images links
        DomNodeList<HtmlElement> anchors;  //All anchors in album page
        while (true) {
            // Extracting images links
            HtmlElement body = albumPage.getElementById("root");
            anchors = body.getElementsByTagName("a");
            for (int j = 0; j < anchors.size(); j++) {
                if (anchors.get(j).getAttribute("href").contains("fbid")) {
                    photos.add("http://m.facebook.com" + anchors.get(j).getAttribute("href"));
                }
            }
            //checking for Additional images in this album
            try {
                albumPage = getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com" + albumPage.getElementById("m_more_item").getElementsByTagName("a").get(0).getAttribute("href"));
            } catch (Exception ex) {
                break;
            }

            if (checkForCancel()) {
                return;
            }
        }
        //dumping Images with comments
        for (int j = 0; j < photos.size(); j++) {
            //opening Image Page that containg it's comments
            HtmlPage photoPage = getAuthenticatedProfile().getBrowser().getPage(photos.get(j));
            //writing image to file
            FileUtils.copyURLToFile(new URL(photoPage.getElementsByTagName("img").get(1).getAttribute("src")), new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Album-" + albumIndex + System.getProperty("file.separator") + "Image-" + (j + 1) + ".jpg"));
            //Initializing html file to look like facebook interface
            PrintWriter commentWriter = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Album-" + albumIndex + System.getProperty("file.separator") + "Comments-on-Image-" + (j + 1) + ".html"), "UTF-8");
            commentWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            commentWriter.println("<!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.0//EN\" \"http://www.wapforum.org/DTD/xhtml-mobile10.dtd\">");
            commentWriter.println(albumPage.getElementsByTagName("head").get(0).asXml());
            //checking for previous comments on this image
            try {
                //dumping previous comments
                dumpComments((HtmlPage) getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com" + photoPage.getElementById("see_prev").getElementsByTagName("a").get(0).getAttribute("href")), commentWriter);
            } catch (Exception ex) {
            }
            //dumping latest comments
            dumpComments(photoPage, commentWriter);
            commentWriter.flush();
            commentWriter.close();
            setPercentage((double) (j + 1) / photos.size() * 100);
            getFacebookGUI().updateTaskProgress(this);

            if (checkForCancel()) {
                return;
            }
        }

        //dumping Album name in text file
        PrintWriter nameWriter = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Album-" + albumIndex + System.getProperty("file.separator") + "Album Name.txt"), "UTF-8");
        nameWriter.println(albumPage.getTitleText());
        nameWriter.flush();
        nameWriter.close();
    }

    /**
     * Dump comments on Album images
     * @param photoPage Mobile photo page containing comments
     * @param commentWriter Writer used to dump comments
     */
    private void dumpComments(HtmlPage photoPage, PrintWriter commentWriter) {
        DomNodeList<HtmlElement> divisions = photoPage.getElementsByTagName("div");
        for (int i = 0; i < divisions.size(); i++) {
            if (divisions.get(i).getAttribute("class").equals("ufi")) {
                commentWriter.println(divisions.get(i).asXml());
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "Dump Album's photos with comments";
    }
}
