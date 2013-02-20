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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;


public class SettingsManager {

    private static Preferences prefs = 
            Preferences.userRoot().node("fbpwn.core.SettingsManager");

    public static void setProxySettings(String hostname, String port) {    
        Logger.getLogger(SettingsManager.class.getName()).log(Level.CONFIG, "Settings proxy settings");
        setParameter("proxyhost", hostname);
        setParameter("proxyport", port);
    }

    public static String getProxyHost() {
        return getParameter("proxyhost");
    }

    public static String getProxyPort() {
        return getParameter("proxyport");
    }

    public static boolean useProxy() {
        if (getParameter("useproxy").equals("true")) {            
            Logger.getLogger(SettingsManager.class.getName()).log(Level.CONFIG, "Proxy enabled");
            return true;
        }
        Logger.getLogger(SettingsManager.class.getName()).log(Level.CONFIG, "Proxy disabled");
        return false;
    }

    public static void setUseProxy(boolean useProxy) {
        if (useProxy) {
            setParameter("useproxy", "true");
            Logger.getLogger(SettingsManager.class.getName()).log(Level.CONFIG, "Setting proxy enabled");
        } else {
            setParameter("useproxy", "false");
            Logger.getLogger(SettingsManager.class.getName()).log(Level.CONFIG, "Setings proxy disabled");
        }
    }

    private static void setParameter(String paramName, String paramValue) {
        prefs.put(paramName, paramValue);
    }

    private static String getParameter(String paramName) {
        return prefs.get(paramName, "");
    }
}
