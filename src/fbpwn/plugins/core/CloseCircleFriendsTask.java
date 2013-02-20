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
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import fbpwn.plugins.ui.CloseCircleFriendsDialog;
import fbpwn.ui.FacebookGUI;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

public class CloseCircleFriendsTask extends FacebookTask {

    private boolean searchPhotos = false;
    private boolean searchWallPosts = false;
    private int wallPages = 0;
    private int likePoint, commentPoint, tagPoint;
    private CloseCircleFriendsTask task;
    private String victimName;
    private ArrayList<CloseFriend> closeFriends;
    private ArrayList<String> checkedPosts;

    public CloseCircleFriendsTask(FacebookGUI FacebookGUI,
	    FacebookAccount facebookProfile,
	    AuthenticatedAccount authenticatedProfile,
	    File workingDirectory) {
	super(FacebookGUI, facebookProfile, authenticatedProfile, workingDirectory);
	task = this;
    }

    public void setSearchPhotos(boolean SearchPhotos) {
	this.searchPhotos = SearchPhotos;
    }

    public void setSearchWallPosts(boolean SearchWallPosts) {
	this.searchWallPosts = SearchWallPosts;
    }

    public void setCommentPoint(int CommentPoint) {
	this.commentPoint = CommentPoint;
    }

    public void setLikePoint(int LikePoint) {
	this.likePoint = LikePoint;
    }

    public void setTagPoint(int TagPoint) {
	this.tagPoint = TagPoint;
    }

    @Override
    public boolean run() {
	try {
	    setMessage("Initializing Module");
	    setTaskState(FacebookTaskState.Running);
	    getFacebookGUI().updateTaskProgress(this);
	    WebClient tempPage = getAuthenticatedProfile().getBrowser();
	    //determining search parameters
	    try {
		SwingUtilities.invokeAndWait(new Runnable() {

		    @Override
		    public void run() {
			CloseCircleFriendsDialog dialog = new CloseCircleFriendsDialog(null, true, task);
			wallPages = dialog.ShowCloseCircleFriendsSelectionDialog();
		    }
		});
	    } catch (InterruptedException ex) {
		Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
	    } catch (InvocationTargetException ex) {
		Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
	    }
	    String MobileWallPage = getFacebookTargetProfile().getProfilePageURL().replace("www", "m");
	    HtmlPage VictimWallPage = tempPage.getPage(MobileWallPage);
	    //extracting victime's name
	    victimName = VictimWallPage.getTitleText();
	    closeFriends = new ArrayList<CloseFriend>();
	    checkedPosts = new ArrayList<String>();
	    if (searchPhotos) {
		setMessage("Searching under Photos");
		getFacebookGUI().updateTaskProgress(this);
		//getting albums links
		ArrayList<String> albums = getFacebookTargetProfile().getAlbumsURLs();
		for (int i = 0; i < albums.size(); i++) {
		    //searching in comments/likes/tags in each albums
		    setMessage("Searching in Album " + (i + 1) + "/" + albums.size());
		    getFacebookGUI().updateTaskProgress(this);
		    HtmlPage albumPage = tempPage.getPage(albums.get(i));
		    processAlbum(albumPage, tempPage);
		}
	    }
	    if (checkForCancel()) {
		for (int i = 0; i < closeFriends.size(); i++) {
		    closeFriends.get(i).calculatePoints(commentPoint, tagPoint, likePoint);
		}
		SortCloseFriends();
		//saving close friends to html file
		writeOutput();
		return true;
	    }
	    if (searchWallPosts) {

		int index = 0;
		while (index != wallPages) {
		    HtmlElement storiesArea = VictimWallPage.getElementById("m_stream_stories");
		    DomNodeList<HtmlElement> anchors = storiesArea.getElementsByTagName("a");
		    for (int i = 0; i < anchors.size(); i++) {
			if (anchors.get(i).getAttribute("class").equals("sec") && anchors.get(i).getTextContent().contains("Comments") && !checkedPosts.contains("http://m.facebook.com" + anchors.get(i).getAttribute("href"))) {
			    HtmlPage postPage = tempPage.getPage("http://m.facebook.com" + anchors.get(i).getAttribute("href"));
			    processPost(postPage, tempPage);
			    checkedPosts.add("http://m.facebook.com" + anchors.get(i).getAttribute("href"));
			}
		    }
		    try {
			VictimWallPage = VictimWallPage.getAnchorByText("See More Posts").click();
		    } catch (Exception ex) {
			break;
		    }

		    setMessage("Searching in " + (index + 1) + "/"
			    + (wallPages == -1 ? "all" : wallPages)
			    + " Wall page(s)");

		    if (wallPages != -1) {
			setPercentage(((double) (index + 1) / wallPages) * 100);
		    }

		    getFacebookGUI().updateTaskProgress(this);
		    if (checkForCancel()) {
			for (int i = 0; i < closeFriends.size(); i++) {
			    closeFriends.get(i).calculatePoints(commentPoint, tagPoint, likePoint);
			}
			SortCloseFriends();
			//saving close friends to html file
			writeOutput();
			return true;
		    }
		    index++;
		}
	    }
	    //calculating each close friend points
	    setMessage("Calculating each friend points");
	    setPercentage(0.0);
	    getFacebookGUI().updateTaskProgress(this);
	    for (int i = 0; i < closeFriends.size(); i++) {
		closeFriends.get(i).calculatePoints(commentPoint, tagPoint, likePoint);
	    }
	    SortCloseFriends();
	    //saving close friends to html file
	    writeOutput();
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

    private void writeOutput() {
	PrintWriter pw = null;
	try {
	    pw = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "CloseFriends.html"), "UTF-8");
	    setMessage("Writing close friends to HTML file");
	    setPercentage(0.0);
	    getFacebookGUI().updateTaskProgress(this);
	    pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 3.2//EN\">");
	    pw.println("<html>");
	    pw.println("<head>");
	    pw.println();
	    pw.println("<meta http-equiv = \"Content-Type\" content = \"text/html; charset=UTF-8\">");
	    pw.println();
	    pw.println("</head>");
	    pw.println("<body>");
	    pw.println("<div align=\"center\">");
	    pw.println("<table border=\"1\">");
	       for (int i = 0; i < closeFriends.size(); i++) {		
		pw.println("<tr>");
		pw.println("<td>");
		pw.println("<a href=\"" +closeFriends.get(i).getAccount().getProfilePageURL() + "\">" + closeFriends.get(i).getAccount().getName() + "<//a>");
		pw.println("</td>");		
		pw.println("<td>");
		pw.println("Comments = " + closeFriends.get(i).getComments() + "<br>Tags = " + closeFriends.get(i).getTags() + "<br>Likes = " + closeFriends.get(i).getLikes() + "<br>Total Points = " + closeFriends.get(i).getPoints());
		pw.println("</td>");
		pw.println("</tr>");				
	    }	  
	    pw.println("</table>");
	    pw.println("</div>");
	    pw.println("</body>");
	    pw.println("</html>");
	    pw.flush();
	    pw.close();
	} catch (FileNotFoundException ex) {
	    Logger.getLogger(CloseCircleFriendsTask.class.getName()).log(Level.SEVERE, null, ex);
	} catch (UnsupportedEncodingException ex) {
	    Logger.getLogger(CloseCircleFriendsTask.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    pw.close();
	}
    }

    private void processAlbum(HtmlPage albumPage, WebClient webClient) throws FileNotFoundException, UnsupportedEncodingException, IOException {
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
		//calculating and sorting
		for (int i = 0; i < closeFriends.size(); i++) {
		    closeFriends.get(i).calculatePoints(commentPoint, tagPoint, likePoint);
		}
		SortCloseFriends();
		//saving close friends to text file
		PrintWriter pw = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "CloseFriends.txt"), "UTF-8");
		for (int i = 0; i < closeFriends.size(); i++) {
		    pw.println(closeFriends.get(i).getAccount().getName() + "|Comments = " + closeFriends.get(i).getComments() + "|Tags = " + closeFriends.get(i).getTags() + "|Likes = " + closeFriends.get(i).getLikes() + "|Total Points = " + closeFriends.get(i).getPoints());
		}
		pw.flush();
		pw.close();
		return;
	    }
	}
	//harvesting comments under images in dictionary
	for (int j = 0; j < photos.size(); j++) {
	    //opening Image Page that containg it's comments
	    HtmlPage photoPage = getAuthenticatedProfile().getBrowser().getPage(photos.get(j));

	    processPost(photoPage, webClient);
	    checkedPosts.add(photos.get(j));
	    setPercentage((double) (j + 1) / photos.size() * 100);
	    getFacebookGUI().updateTaskProgress(this);

	    if (checkForCancel()) {
		//calculating and sorting
		for (int i = 0; i < closeFriends.size(); i++) {
		    closeFriends.get(i).calculatePoints(commentPoint, tagPoint, likePoint);
		}
		SortCloseFriends();
		//saving close friends to text file
		PrintWriter pw = new PrintWriter(new File(getDirectory().getAbsolutePath() + System.getProperty("file.separator") + "CloseFriends.txt"), "UTF-8");
		for (int i = 0; i < closeFriends.size(); i++) {
		    pw.println(closeFriends.get(i).getAccount().getName() + "|Comments = " + closeFriends.get(i).getComments() + "|Tags = " + closeFriends.get(i).getTags() + "|Likes = " + closeFriends.get(i).getLikes() + "|Total Points = " + closeFriends.get(i).getPoints());
		}
		pw.flush();
		pw.close();
		return;
	    }
	}

    }

    private void processPost(HtmlPage postPage, WebClient webClient) throws IOException {
	DomNodeList<HtmlElement> divisions = postPage.getElementsByTagName("div");
	for (int i = 0; i < divisions.size(); i++) {
	    //checking for comments division
	    if (divisions.get(i).getAttribute("class").equals("row aclb apl")) {
		String profileName = divisions.get(i).getFirstChild().getFirstChild().getTextContent();
		String profileURL = "http://www.facebook.com" + divisions.get(i).getElementsByTagName("a").get(0).getAttribute("href");
		//saving new friend to victim's close friends
		if (profileName.equals(victimName)) {
		    continue;
		}
		CloseFriend closeFriend = searchForFriend(profileName);
		if (closeFriend != null) {
		    closeFriend.incrementComments();
		} else {
		    FacebookAccount friend = new FacebookAccount(profileURL, webClient);
		    friend.setName(profileName);
		    closeFriend = new CloseFriend(friend);
		    closeFriend.incrementComments();
		    closeFriends.add(closeFriend);
		}
	    }
	    //checking for photo tags division
	    if (divisions.get(i).getAttribute("class").equals("acw apl")) {
		if (divisions.get(i).getFirstChild().getTextContent().contains("with")) {
		    DomNodeList<HtmlElement> tagsDivision = divisions.get(i).getElementsByTagName("div");
		    DomNodeList<HtmlElement> tagsProfiles = tagsDivision.get(0).getElementsByTagName("span");
		    //extracting all friends tagged with victim
		    for (int k = 1; k < tagsProfiles.size(); k++) {
			String profileName = tagsProfiles.get(k).getTextContent();
			String ProfileURL = "http://www.facebook.com" + tagsProfiles.get(k).getElementsByTagName("a").get(0).getAttribute("href");
			if (profileName.equals(victimName)) {
			    continue;
			}
			CloseFriend closeFriend = searchForFriend(profileName);
			if (closeFriend != null) {
			    closeFriend.incrementTags();
			} else {
			    FacebookAccount friend = new FacebookAccount(ProfileURL, webClient);
			    friend.setName(profileName);
			    closeFriend = new CloseFriend(friend);
			    closeFriend.incrementTags();
			    closeFriends.add(closeFriend);
			}
		    }
		}
	    }
	    // checking for likes on photo division
	    if (divisions.get(i).getAttribute("class").equals("likes row aclb apm")) {
		if (!divisions.get(i).getTextContent().contains("people") && !divisions.get(i).getTextContent().contains("and")) {
		    String profileName = divisions.get(i).getTextContent().replace(" likes this.", "");
		    String profileURL = "http://www.facebook.com" + divisions.get(i).getElementsByTagName("a").get(0).getAttribute("href");
		    if (profileName.equals(victimName)) {
			continue;
		    }
		    CloseFriend closeFriend = searchForFriend(profileName);
		    if (closeFriend != null) {
			closeFriend.incrementLikes();
		    } else {
			FacebookAccount friend = new FacebookAccount(profileURL, webClient);
			friend.setName(profileName);
			closeFriend = new CloseFriend(friend);
			closeFriend.incrementLikes();
			closeFriends.add(closeFriend);
		    }
		}
	    }
	}
	//checking for likes on each comments on photo division
	List<HtmlAnchor> photoAnchors = postPage.getAnchors();
	for (int i = 0; i < photoAnchors.size(); i++) {
	    //extracting likes links
	    if (photoAnchors.get(i).getHrefAttribute().contains("/browse/likes/")) {
		HtmlPage likesPage = photoAnchors.get(i).click();
		List<HtmlAnchor> likesAnchors = likesPage.getAnchors();
		for (int k = 1; k < likesAnchors.size(); k++) {
		    //extracting profiles with likes on comments
		    if (!likesAnchors.get(k).getTextContent().contains("Add")) {
			String profileName = likesAnchors.get(k).getTextContent();
			String profileURL = "http://www.facebook.com" + likesAnchors.get(k).getHrefAttribute();
			if (profileName.equals(victimName)) {
			    continue;
			}
			CloseFriend closeFriend = searchForFriend(profileName);
			if (closeFriend != null) {
			    closeFriend.incrementLikes();
			} else {
			    FacebookAccount friend = new FacebookAccount(profileURL, webClient);
			    friend.setName(profileName);
			    closeFriend = new CloseFriend(friend);
			    closeFriend.incrementLikes();
			    closeFriends.add(closeFriend);
			}
		    }
		}
	    }
	}
    }
    /*
     * search for friends inside the list of close friends by name
     */

    private CloseFriend searchForFriend(String friendName) {
	for (int i = 0; i < closeFriends.size(); i++) {
	    if (closeFriends.get(i).getAccount().getName().equals(friendName)) {
		return closeFriends.get(i);
	    }
	}
	return null;
    }

    private void SortCloseFriends() {

	Collections.sort(closeFriends, new Comparator<CloseFriend>() {

	    @Override
	    public int compare(CloseFriend arg0, CloseFriend arg1) {
		if (arg0.getPoints() < arg1.getPoints()) {
		    return 1;
		} else if (arg0.getPoints() == arg1.getPoints()) {
		    return 0;
		} else {
		    return -1;
		}
	    }
	});
    }

    @Override
    public void init() {
    }

    @Override
    public String toString() {
	return "Find close friends";
    }
}

class CloseFriend {

    private FacebookAccount account;
    private int Likes;
    private int Comments;
    private int Tags;
    private int Points;

    public CloseFriend(FacebookAccount account) {
	this.account = account;
	Likes = 0;
	Comments = 0;
	Tags = 0;
	Points = 0;
    }

    public void incrementLikes() {
	Likes++;
    }

    public void incrementComments() {
	Comments++;
    }

    public void incrementTags() {
	Tags++;
    }

    public void setPoints(int Points) {
	this.Points = Points;
    }

    public int getComments() {
	return Comments;
    }

    public int getLikes() {
	return Likes;
    }

    public int getPoints() {
	return Points;
    }

    public int getTags() {
	return Tags;
    }

    public FacebookAccount getAccount() {
	return account;
    }

    public void calculatePoints(int CommentPoint, int TagPoint, int LikePoint) {
	Points = (Comments * CommentPoint) + (Tags * TagPoint) + (Likes * LikePoint);
    }
}