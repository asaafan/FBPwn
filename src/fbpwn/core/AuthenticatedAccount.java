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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.List;

/**
 * Represents a Facebook authenticated account.
 * Used as the attacker's account.
 */
public class AuthenticatedAccount extends FacebookAccount {

    private String accountEmail, password, accoundID;  //AuthenticatedAccount info

    /**
     * Create a new authenticated account
     * @param AccoundID Facebook account ID
     * @param Browser The headless browser used for logging in with this account
     * @param profilePageUrl URL for the profile
     * @param Email E-mail used in logging in to this authenticated account
     * @param pass the authenticated account's password
     */
    public AuthenticatedAccount(String AccoundID, WebClient Browser, String profilePageUrl, String Email, String pass) {
        super(profilePageUrl, Browser);
        accoundID = AccoundID;
        accountEmail = Email;
        password = pass;
    }

    /**
     * Send friend request to a specific Facebook account using this Authenticated account
     * @param profileURL The victim's profile URL     
     * @throws IOException If failed to reach Facebook.com
     * @throws FacebookException If a Facebook error occurs
     */
    public void sendFriendRequest(String profileURL) throws IOException, FacebookException {
        HtmlPage friendPage = super.getBrowser().getPage(profileURL);
        // parsing the friend page
        List<HtmlAnchor> allAnchors = friendPage.getAnchors();
        //extracting AddFriend Href
        for (int i = 0; i < allAnchors.size(); i++) {
            if (allAnchors.get(i).getHrefAttribute().contains("/addfriend.php")) {
                HtmlPage SendRequest = super.getBrowser().getPage("http://www.facebook.com" + allAnchors.get(i).getHrefAttribute());
                System.out.println(SendRequest.getUrl().toString());
                if (!SendRequest.asXml().toString().contains("Sorry, this user already has too many friend requests")) {
                    HtmlElement AddFriendButton = SendRequest.getElementByName("add");
                    AddFriendButton.click();
                    return;
                }
            }
        }
    }

    /**
     * This function determine the state of the sent friend request
     * @param profileURL Returns the request state for the given profile
     * @return The state of the friend request
     * @throws IOException If failed to reach Facebook.com
     */
    public RequestState getFriendRequestState(String profileURL) throws IOException {
        HtmlPage friendProfilePage = super.getBrowser().getPage(profileURL);
        //Friend Request was sent and Declined
        if (friendProfilePage.asXml().contains("Friend Request Sent")) {
            return RequestState.RequestPending;
        } else
        if (friendProfilePage.asXml().contains("FriendRequestAdd hidden_elem addButton uiButton")) {
            return RequestState.RequestAccepted;
        } else
        if (friendProfilePage.asXml().contains("Add Friend") && !friendProfilePage.asXml().contains("Cancel Friend Request")) {
            return RequestState.RequestDeclined;
            //Friend Request is still pending
        }  else {
            return RequestState.ErrorOccured;
        }
    }

    /**
     * Gets the e-mail associated with this authenticated account
     * @return String representing the e-mail used for this authenticated account
     */
    public String getEmail() {
        return accountEmail;
    }

    /**
     * Gets the account's ID
     * @return String representing the account's Facebook ID
     */
    public String getAccountID() {
        return accoundID;
    }
    
    /**
     * Gets the password associated with this authenticated account
     * @return String representing the pasword used for this authenticated account
     */
    public String getPassword() {
        return password;
    }
    
    @Override
    public String toString() {
        return accountEmail;
    }
}
