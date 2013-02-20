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
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import fbpwn.plugins.ui.DictionaryBuilderDialog;
import fbpwn.ui.FacebookGUI;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;


public class DictionaryBuilderTask extends FacebookTask {

    private boolean SearchPhotos = false;
    private boolean SearchWallPosts = false;
    private int WallPages = 0;
    private int minimumWordLength = 3;
    private DictionaryBuilderTask task;
    private ArrayList<String> Dictionary;

    public DictionaryBuilderTask(FacebookGUI FacebookGUI,
	    FacebookAccount facebookProfile,
	    AuthenticatedAccount authenticatedProfile,
	    File workingDirectory) {
	super(FacebookGUI, facebookProfile, authenticatedProfile, workingDirectory);
	task = this;
    }

    public void setSearchPhotos(boolean SearchPhotos) {
	this.SearchPhotos = SearchPhotos;
    }

    public void setSearchWallPosts(boolean SearchWallPosts) {
	this.SearchWallPosts = SearchWallPosts;
    }

    @Override
    public boolean run() {
	try {
	    setMessage("Initiating Module");
	    setTaskState(FacebookTaskState.Running);
	    getFacebookGUI().updateTaskProgress(this);
	    Dictionary = new ArrayList<String>();
	    WebClient tempWebClient = getAuthenticatedProfile().getBrowser();

	    try {
		SwingUtilities.invokeAndWait(new Runnable() {

		    @Override
		    public void run() {
			DictionaryBuilderDialog dialog = new DictionaryBuilderDialog(null, true, task);
			WallPages = dialog.ShowDictionaryBuilderSelectionDialog();
		    }
		});
	    } catch (InterruptedException ex) {
		Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
	    } catch (InvocationTargetException ex) {
		Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
	    }
	    if (SearchPhotos) {
		setMessage("Searching under Photos");
		getFacebookGUI().updateTaskProgress(this);
		ArrayList<String> albums = getFacebookTargetProfile().getAlbumsURLs();
		for (int i = 0; i < albums.size(); i++) {
		    setMessage("Searching in Album " + (i + 1) + "/" + albums.size());
		    getFacebookGUI().updateTaskProgress(this);
		    HtmlPage albumPage = tempWebClient.getPage(albums.get(i));
		    processAlbum(albumPage);
		}
	    }
	    if (SearchWallPosts) {
		String MobileWallPage = getFacebookTargetProfile().getProfilePageURL().replace("www", "m");
		HtmlPage VictimWallPage = tempWebClient.getPage(MobileWallPage);
		int index = 0;
		while (index != WallPages) {
		    HtmlElement storiesArea = VictimWallPage.getElementById("m_stream_stories");
		    DomNodeList<HtmlElement> stories = storiesArea.getElementsByTagName("span");
		    for (int i = 0; i < stories.size(); i++) {
			if (stories.get(i).getAttribute("dir").equals("rtl")) {
			    StringTokenizer token = new StringTokenizer(stories.get(i).getTextContent());
			    while (token.hasMoreTokens()) {
				String word = token.nextToken();
				if (!Dictionary.contains(word)) {
				    Dictionary.add(word);
				}
			    }
			}
		    }
                    DomNodeList<HtmlElement> anchors = storiesArea.getElementsByTagName("a");
                    for(int i=0;i<anchors.size();i++)
                    {
                        if(anchors.get(i).getAttribute("class").equals("sec") && anchors.get(i).getTextContent().contains("Comments"))
                        {
                            HtmlPage postPage = tempWebClient.getPage("http://m.facebook.com"+anchors.get(i).getAttribute("href"));
                            processPosts(postPage);
                        }
                    }
		    try {
			VictimWallPage = VictimWallPage.getAnchorByText("See More Posts").click();
		    } catch (Exception ex) {
			break;
		    }

		    setMessage("Searched in " + (index + 1) + "/"
			    + (WallPages == -1 ? "all" : WallPages)
			    + " Wall page(s)");

		    if (WallPages != -1) {
			setPercentage(((double) (index + 1) / WallPages) * 100);
		    }

		    getFacebookGUI().updateTaskProgress(this);
		    if (checkForCancel()) {
			return true;
		    }
		    index++;
		}
	    }	    
	    PrintWriter pw = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "Dictionary.txt"), "UTF-8");
	    setMessage("Writing password dictionary");
	    setPercentage(0.0);
	    getFacebookGUI().updateTaskProgress(this);
	    for (int i = 0; i < Dictionary.size(); i++) {
		if(Dictionary.get(i).length() >= minimumWordLength)
		{
		pw.println(Dictionary.get(i));
		}
	    }
	    pw.flush();
	    pw.close();
	} catch (IOException ex) {
	    Logger.getLogger(DictionaryBuilderTask.class.getName()).log(Level.SEVERE, null, ex);
	} catch (FailingHttpStatusCodeException ex) {
	    Logger.getLogger(DictionaryBuilderTask.class.getName()).log(Level.SEVERE, null, ex);
	}
	setMessage("Finished");
	setTaskState(FacebookTaskState.Finished);
	setPercentage(100.0);
	getFacebookGUI().updateTaskProgress(this);
	return true;
    }
    private void processPosts(HtmlPage postPage)
    {
        DomNodeList<HtmlElement> divisions = postPage.getElementsByTagName("div");
            for (int i = 0; i < divisions.size(); i++) {
                if (divisions.get(i).getAttribute("class").equals("row aclb apl")) {
                    String textContent = divisions.get(i).getFirstChild().getTextContent();
                    String profileName = divisions.get(i).getFirstChild().getFirstChild().getTextContent();
                    StringTokenizer token = new StringTokenizer(textContent.replace(profileName, ""));
                    while (token.hasMoreTokens()) {
                        String word = token.nextToken();
                        if (!Dictionary.contains(word)) {
                            Dictionary.add(word);
                        }
                    }
                }


            }
    }
    private void processAlbum(HtmlPage albumPage) throws FileNotFoundException, UnsupportedEncodingException, IOException {
	ArrayList<String> photos = new ArrayList<String>();  //Array of Images links
	DomNodeList<HtmlElement> anchors;  //All anchors in album page
	while (true) {
	    // Extracting images links
	    HtmlElement body = albumPage.getElementById("contentArea");
	    anchors = body.getElementsByTagName("a");
	    for (int j = 0; j < anchors.size(); j++) {
		if (anchors.get(j).getAttribute("href").contains("fbid")) {
		    photos.add(anchors.get(j).getAttribute("href").replace("www", "m"));
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
	//harvesting comments under images in dictionary
	for (int j = 0; j < photos.size(); j++) {
	    //opening Image Page that containg it's comments
	    HtmlPage photoPage = getAuthenticatedProfile().getBrowser().getPage(photos.get(j));
	    DomNodeList<HtmlElement> divisions = photoPage.getElementsByTagName("div");
	    for (int i = 0; i < divisions.size(); i++) {
		if (divisions.get(i).getAttribute("class").equals("row aclb apl")) {
		    String textContent = divisions.get(i).getFirstChild().getTextContent();
		    String profileName = divisions.get(i).getFirstChild().getFirstChild().getTextContent();
		    StringTokenizer token = new StringTokenizer(textContent.replace(profileName, ""));
		    while (token.hasMoreTokens()) {
			String word = token.nextToken();
			if (!Dictionary.contains(word)) {
			    Dictionary.add(word);
			}
		    }
		}


	    }

	    setPercentage((double) (j + 1) / photos.size() * 100);
	    getFacebookGUI().updateTaskProgress(this);

	    if (checkForCancel()) {
		return;
	    }
	}

    }

    @Override
    public void init() {
    }

    @Override
    public String toString() {
	return "Build Password Dictionary";
    }
}
