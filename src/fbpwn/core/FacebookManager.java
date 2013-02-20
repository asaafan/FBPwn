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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import fbpwn.plugins.core.*;
import fbpwn.ui.FacebookGUI;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the manager for Facebook communication
 */
public class FacebookManager {

    private ArrayList<AuthenticatedAccount> authenticatedAccounts;
    private ArrayList<TaskQueue> Queues = new ArrayList<TaskQueue>();
    private static FacebookManager facebookManagerInstance = null;
    private FacebookGUI facebookGUI;
    private ArrayList<Class<?>> allClasses = new ArrayList<Class<?>>();
    private BrowserVersion[] browsers = new BrowserVersion[]{BrowserVersion.FIREFOX_3,
	BrowserVersion.FIREFOX_3_6,
	//BrowserVersion.INTERNET_EXPLORER_6, //Causing troubles !
	BrowserVersion.INTERNET_EXPLORER_7,
	BrowserVersion.INTERNET_EXPLORER_8};
    private Random rand = new Random(System.currentTimeMillis());

    private FacebookManager() {
	authenticatedAccounts = new ArrayList<AuthenticatedAccount>();
	reloadPlugins();
    }

    /**
     * Gets an instance of FacebookManager, and creates one if none existed
     *
     * @return The only instance of FacebookManager
     */
    public static FacebookManager getInstance() {
	if (facebookManagerInstance == null) {
	    facebookManagerInstance = new FacebookManager();
	}
	return facebookManagerInstance;
    }

    /**
     * Log in to the attacker's account using the given username and password
     *
     * @param userEmail The e-mail used for logging in
     * @param userPassword The password used for logging in
     * @return The AuthenticatedAccount representing the attackers account
     * @throws IOException If failed to reach Facebook.com
     * @throws FacebookException If a Facebook error occurred
     */
    public AuthenticatedAccount logIn(String userEmail, String userPassword) throws IOException, FacebookException {

	Logger.getLogger(FacebookManager.class.getName()).log(Level.INFO, "Trying to login");

	WebClient webClient = null;
	if (SettingsManager.useProxy()) {
	    webClient = new WebClient(browsers[rand.nextInt(browsers.length)],
		    SettingsManager.getProxyHost(),
		    Integer.parseInt(SettingsManager.getProxyPort()));
	    ;
	} else {
	    webClient = new WebClient(browsers[rand.nextInt(browsers.length)]);
	}

	Logger.getLogger(FacebookManager.class.getName()).log(Level.INFO, "Using " + webClient.getBrowserVersion().getUserAgent());

	webClient.setCssEnabled(false);
	webClient.setJavaScriptEnabled(false);
	HtmlPage loginPage = webClient.getPage("http://www.facebook.com");
	if (loginPage.getForms().isEmpty()) {
	    throw new IOException();
	}
	HtmlForm loginForm = loginPage.getForms().get(0);
	HtmlSubmitInput button = (HtmlSubmitInput) loginForm.getInputsByValue("Log In").get(0);
	HtmlTextInput textField = loginForm.getInputByName("email");
	textField.setValueAttribute(userEmail);
	HtmlPasswordInput textField2 = loginForm.getInputByName("pass");
	textField2.setValueAttribute(userPassword);
	HtmlPage homePage = button.click();
	String homePageAsText = homePage.asText();
	if (!homePageAsText.contains("News Feed") && !homePage.getTitleText().equals("Find Friends")) {

	    throw new FacebookException(homePage, "Error occured while logging in");
	}


	DomNodeList<HtmlElement> elementsByTagName = homePage.getElementsByTagName("a");
	String profileURL = "";

	for (HtmlElement element : elementsByTagName) {
	    if (element.getAttribute("title") != null
		    && element.getAttribute("title").equals("Profile")) {
		HtmlAnchor profilebutton = (HtmlAnchor) element;
		profileURL = profilebutton.getHrefAttribute();
		break;
	    }
	}

	String accountID;
	try {
	    if (profileURL.contains("&")) {
		int index = profileURL.indexOf('=') + 1;
		accountID = profileURL.substring(index, profileURL.indexOf("&"));
	    } else {
		int index = profileURL.indexOf("facebook.com/") + "facebook.com/".length();
		if (profileURL.contains("?")) {
		    accountID = profileURL.substring(index, profileURL.indexOf("?"));
		} else {
		    accountID = profileURL.substring(index);
		}
	    }
	} catch (Exception ex) {
	    accountID = "Couldn't get ID";
	    Logger.getLogger(FacebookManager.class.getName()).log(Level.WARNING, "Failed to get User ID while logging in");
	}

	AuthenticatedAccount myAccount = new AuthenticatedAccount(accountID, webClient, profileURL, userEmail, userPassword);
	return myAccount;
    }

    /**
     * Adds a new AuthenticatedAccount
     *
     * @param newAccount The account to be added
     */
    public void addAuthenticatedProfile(AuthenticatedAccount newAccount) {
	authenticatedAccounts.add(newAccount);
    }

    /**
     * Deletes AuthenticatedAccount
     *
     * @param DeleteAccount The account to be deleted
     */
    public void removeAuthenticatedProfile(AuthenticatedAccount DeleteAccount) {
	authenticatedAccounts.remove(DeleteAccount);
    }

    /**
     * Returns the GUI associated with the FacebookManager
     *
     * @return The GUI associated with the FacebookManager
     */
    public FacebookGUI getFacebookGUI() {
	return facebookGUI;
    }

    /**
     * Sets the Facebook GUI associated with the FacebookManager
     *
     * @param facebookGUI The Facebook GUI associated with the FacebookManager
     */
    public void setFacebookGUI(FacebookGUI facebookGUI) {
	this.facebookGUI = facebookGUI;
    }

    /**
     * Gets all authenticated accounts
     *
     * @return A list of authenticated accounts
     */
    public List<AuthenticatedAccount> getAuthenticatedAccounts() {
	return this.authenticatedAccounts;
    }

    /**
     * Creates a new task queue
     *
     * @param authenticatedAccount The account to be used in attacking
     * @param targetAccountURL The target's profile URL
     * @param outputDirectory The directory to save all the dumped data in
     * @param selectedPlugins The list of selected plugins and modules to be
     * used for attacking
     * @param pollingTime The delay between each retry in case of a plugin
     * failure
     * @param useVictimID Use Victim's ID as the output folder
     */
    public void createTaskQueue(AuthenticatedAccount authenticatedAccount,
	    String targetAccountURL,
	    File outputDirectory,
	    Class<?>[] selectedPlugins,
	    double pollingTime, boolean useVictimID) {
	createTaskQueue(authenticatedAccount,
		new FacebookAccount(targetAccountURL,
		authenticatedAccount.getBrowser()),
		outputDirectory, selectedPlugins, pollingTime, useVictimID);
    }

    /**
     * Creates a new task queue
     *
     * @param authenticatedAccount The account to be used in attacking
     * @param targetAccount The target's account
     * @param outputDirectory The directory to save all the dumped data in
     * @param selectedPlugins The list of selected plugins and modules to be
     * used for attacking
     * @param pollingTime The delay between each retry in case of a plugin
     * failure
     * @param useVictimID Use Victim's ID as the output folder
     */
    public void createTaskQueue(AuthenticatedAccount authenticatedAccount,
	    FacebookAccount targetAccount,
	    File outputDirectory,
	    Class<?>[] selectedPlugins,
	    double pollingTime, boolean useVictimID) {

	if (useVictimID) {
	    String newPath = outputDirectory.getAbsolutePath() + System.getProperty("file.separator") + targetAccount.getProfileId();
	    outputDirectory = new File(newPath);
	    outputDirectory.mkdir();
	}

	TaskQueue queue = new TaskQueue(pollingTime);
	for (int i = 0; i < selectedPlugins.length; i++) {
	    try {
		queue.addTask((FacebookTask) selectedPlugins[i].getConstructor(
			FacebookGUI.class,
			FacebookAccount.class,
			AuthenticatedAccount.class,
			File.class).newInstance(
			facebookGUI,
			targetAccount,
			authenticatedAccount,
			outputDirectory));
	    } catch (Exception ex) {
		Logger.getLogger(FacebookManager.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
	    }

	}
	Queues.add(queue);
	Thread thread = new Thread(queue);
	thread.setName("Task queue");
	thread.start();
    }

    private ArrayList<Class<?>> reloadPlugins() {

	Logger.getLogger(FacebookManager.class.getName()).log(Level.INFO, "Loading plugins");

	allClasses.clear();

	allClasses.add(DumpFriendsTask.class);
	allClasses.add(AddVictimsFriends.class);
	allClasses.add(CheckFriendRequestTask.class);
	allClasses.add(DumpImagesTask.class);
	allClasses.add(DumpInfoTask.class);
	allClasses.add(DumpThumbnailImagesTask.class);
	allClasses.add(DumpWallTask.class);
	allClasses.add(ProfileClonerTask.class);
	allClasses.add(DictionaryBuilderTask.class);
	allClasses.add(CloseCircleFriendsTask.class);

	//allClasses.add(MobileDeviceReconTask.class); //Unstable still under development
	try {
	    Class<?> myclass = null;
	    File Directory = new File(
		    System.getProperty("user.dir")
		    + System.getProperty("file.separator") + "fbpwn"
		    + System.getProperty("file.separator") + "plugins"
		    + System.getProperty("file.separator") + "core");
	    File allFiles[] = Directory.listFiles();
	    URLClassLoader urlclassloader = null;
	    try {
		urlclassloader = new URLClassLoader(new URL[]{new File(System.getProperty("user.dir")).toURI().toURL()});
	    } catch (MalformedURLException ex) {
		Logger.getLogger(FacebookManager.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
	    }
	    for (int i = 0; i < allFiles.length; i++) {
		try {
		    myclass = urlclassloader.loadClass("fbpwn.plugins.core." + allFiles[i].getName().
			    substring(0, allFiles[i].getName().indexOf('.')));

		} catch (ClassNotFoundException ex) {
		    continue;
		}
		Constructor<?>[] cnList = myclass.getDeclaredConstructors();
		try {
		    if (myclass.getSuperclass().getName().equals(FacebookTask.class.getName())) {
			for (int j = 0; j < cnList.length; j++) {
			    Constructor cnTemp = cnList[j];
			    Class pmList[] =
				    cnTemp.getParameterTypes();
			    if (pmList.length == 4) {
				if (pmList[0].toString().equals("interface fbpwn.ui.FacebookGUI")
					&& pmList[1].toString().equals("class fbpwn.core.FacebookAccount")
					&& pmList[2].toString().equals("class fbpwn.core.AuthenticatedAccount")
					&& pmList[3].toString().equals("class java.io.File")) {
				    allClasses.add(myclass);
				}
			    }
			}
		    }
		} catch (Exception ex) {
		}
	    }
	} catch (Exception ex) {
	}
	Logger.getLogger(FacebookManager.class.getName()).log(Level.INFO, "Loaded " + allClasses.size() + " plugins");
	return allClasses;
    }

    /**
     * Returns a list of all available plugins
     *
     * @return A list of all available plugins
     */
    public ArrayList<Class<?>> getPlugins() {
	return allClasses;
    }

    /**
     * Removes a task from task queue
     *
     * @param removedTask The task to be canceled
     */
    public void removeTask(FacebookTask removedTask) {
	removedTask.deleteTask();
    }
}
