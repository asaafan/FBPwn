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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogManager {

    private static FileHandler fh = null;

    public static void init() throws IOException {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.CONFIG);
        Handler[] handlers = logger.getHandlers();
        for (Handler h : handlers) {
            logger.removeHandler(h);
        }
        fh = new FileHandler("log.html");
        fh.setFormatter(new HTMLLogFormatter());
        logger.addHandler(fh);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.SEVERE) {

                    ExceptionHandler.reportException(Thread.currentThread(),
                            record.getThrown());

                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
    }
}

class HTMLLogFormatter extends Formatter {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");

    public String format(LogRecord rec) {

        StringBuilder buffer = new StringBuilder(1000);

        buffer.append("<tr>");
        buffer.append("<td>");

        if (rec.getLevel().intValue() >= Level.WARNING.intValue()) {
            buffer.append("<b>");
            buffer.append(rec.getLevel());
            buffer.append("</b>");
        } else {
            buffer.append(rec.getLevel());
        }        
        buffer.append("</td>");
        buffer.append("<td>");
        buffer.append(calcDate(rec.getMillis()));
        buffer.append("</td>");
        
        
        buffer.append("<td>");
        buffer.append(rec.getSourceClassName());        
        buffer.append("</td>");
        
        buffer.append("<td>");
        buffer.append(rec.getSourceMethodName());        
        buffer.append("</td>");
        
        
        buffer.append("<td>");
        Throwable ex = rec.getThrown();
        if (ex != null) {
            buffer.append("<b>" + ex.toString() + "</b>\n<br><br>");
            StackTraceElement[] stacktrace = ex.getStackTrace();
            for (StackTraceElement element : stacktrace) {
                buffer.append(element.toString() + "<br>");
            }
        } else {
            buffer.append(formatMessage(rec));
        }
        buffer.append("</td>");
        buffer.append("</tr>\n");
        return buffer.toString();
    }

    private String calcDate(long millisecs) {
        Date resultdate = new Date(millisecs);

        return dateFormat.format(resultdate);
    }

    public String getHead(Handler h) {
        return "<HTML>\n<HEAD>\n" + "<h1> FBPwn log </h1> " + (new Date()) + "\n</HEAD>\n<BODY>\n<PRE>\n"
                + "<table border>\n  "
                + "<tr><th>Type</th><th>Time</th><th>Class</th><th>Method</th><th>Message</th></tr>\n";
    }

    public String getTail(Handler h) {
        return "</table>\n  </PRE></BODY>\n</HTML>\n";
    }
}
