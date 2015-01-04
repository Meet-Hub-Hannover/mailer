/**
    This file is part of Meet-Hub-Hannover.

    Meet-Hub-Hannover is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Meet-Hub-Hannover is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Meet-Hub-Hannover. If not, see <http://www.gnu.org/licenses/>.
 */

package de.meethub.mailer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;

import com.ecwid.mailchimp.MailChimpClient;
import com.ecwid.mailchimp.MailChimpException;
import com.ecwid.mailchimp.MailChimpObject;
import com.ecwid.mailchimp.method.v1_3.campaign.CampaignCreateMethod;
import com.ecwid.mailchimp.method.v1_3.campaign.CampaignSendTestMethod;
import com.ecwid.mailchimp.method.v1_3.campaign.CampaignType;

public class Mailer {

    private static final String FROM_ADDRESS = "noreply@meet-hub-hannover.de";
    private static final String TEST_ADDRESS = "webmaster@meet-hub-hannover.de";

    public static void main(final String[] args) throws MalformedURLException {
        System.out.println("command line: <apikey> <list-id> <calendar-url>\n");

        final String apiKey = args[0];
        final String listId = args[1];
        final URL calendarUrl = new URL(args[2]);

        final MailChimpClient mc = new MailChimpClient();
        try {
            System.out.println("Creating campaign...\n");
            final String cid = mc.execute(createCampaign(apiKey, listId, calendarUrl));
            System.out.println("Campaign ID: " + cid);

            System.out.println("Test sending...");
            final CampaignSendTestMethod sendMethod = new CampaignSendTestMethod();
            sendMethod.apikey = apiKey;
            sendMethod.cid = cid;
            sendMethod.test_emails = Arrays.asList(TEST_ADDRESS);
            mc.execute(sendMethod);

            final String time = getScheduleTime();
            System.out.println("Scheduling real sending for " + time + " ...");
            final CampaignScheduleMethod schedule = new CampaignScheduleMethod();
            schedule.apikey = apiKey;
            schedule.cid = cid;
            schedule.schedule_time = time;
            mc.execute(schedule);

            System.out.println("Finished.");
        } catch (final IOException | MailChimpException | ParserException e) {
            System.out.println("Fehler beim Senden");
            e.printStackTrace(System.out);
        } finally {
            mc.close();
        }
    }

    private static String getScheduleTime() {
        final Date inTwoHours = new Date(System.currentTimeMillis() + 2L * 60 * 60 * 1000);
        final SimpleDateFormat jsonFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        jsonFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return jsonFormat.format(inTwoHours);
    }

    private static CampaignCreateMethod createCampaign(final String apikey, final String listId, final URL calendarUrl)
        throws IOException, ParserException {

        final CampaignCreateMethod create = new CampaignCreateMethod();
        create.apikey = apikey;
        create.type = CampaignType.plaintext;
        final MailChimpObject options = new MailChimpObject();
        options.put("list_id", listId);
        options.put("subject", createSubject());
        options.put("from_email", FROM_ADDRESS);
        options.put("from_name", "Meet-Hub-Hannover");
        create.options = options;
        final MailChimpObject content = new MailChimpObject();
        content.put("text", createMailText(calendarUrl));
        create.content = content;
        return create;
    }

    private static String createSubject() {
        final Date tomorrow = new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000);
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(tomorrow);
        final int kw = cal.get(GregorianCalendar.WEEK_OF_YEAR);
        final int year = cal.get(GregorianCalendar.YEAR);
        return "Meet-Hub-Hannover Veranstaltungs-Newsletter KW " + kw + "/" + year;
    }

    private static String createMailText(final URL calendarUrl) throws IOException, ParserException {
        final Calendar mergedCalendar = loadCalendar(calendarUrl);
        final Period nextPeriod = new Period(new DateTime(), new Dur(1));
        return "== Meet-Hub-Hannover Veranstaltungs-Newsletter ==\n" +
               "\n" +
               "Nächste Woche:\n" +
               formatNextEvents(mergedCalendar, nextPeriod) +
               "\n" +
               "Danach:\n" +
               formatLaterEvents(mergedCalendar, nextPeriod) +
               "\n" +
               "==============================================\n" +
               "*|LIST:DESCRIPTION|*\n" +
               "\n" +
               "Die E-Mail-Adresse *|EMAIL|* vom Newsletter abmelden:\n" +
               "*|UNSUB|*\n";
    }

    public static Calendar loadCalendar(final URL url) throws IOException, ParserException {
        final CalendarBuilder b = new CalendarBuilder();
        final URLConnection conn = url.openConnection();
        try (InputStream in = conn.getInputStream()) {
            return b.build(in);
        }
    }

    private static String formatNextEvents(final Calendar c, final Period nextPeriod) {
        final List<Event> filtered = Event.getEventsInPeriod(c, nextPeriod);
        return filtered.isEmpty() ? " Keine Termine in der nächsten Woche\n" : formatEvents(filtered);
    }

    private static String formatLaterEvents(final Calendar c, final Period nextPeriod) {
        final List<Event> filtered = Event.getEventsInPeriod(c, new Period(nextPeriod.getEnd(), new Dur(7)));
        return filtered.isEmpty() ? " Keine Termine in der nächsten Zeit danach\n" : formatEvents(filtered);
    }

    private static String formatEvents(final List<Event> filteredAndSortedEvents) {
        final StringBuilder b = new StringBuilder();
        for (final Event e : filteredAndSortedEvents) {
            b.append(" ");
            b.append(formatDow(e.getStart()));
            b.append(", ");
            b.append(formatDate(e.getStart()));
            b.append(": ");
            b.append(e.getTitle());
            b.append("\n");
            if (e.getURL() != null) {
                b.append(" ");
                b.append(e.getURL());
                b.append("\n");
            }
            b.append("\n");
        }
        return b.toString();
    }

    private static String formatDow(final Date date) {
        switch (date.getDay()) {
        case 0:
            return "So";
        case 1:
            return "Mo";
        case 2:
            return "Di";
        case 3:
            return "Mi";
        case 4:
            return "Do";
        case 5:
            return "Fr";
        case 6:
            return "Sa";
        default:
            throw new AssertionError(date.getDay());
        }
    }

    private static String formatDate(final Date date) {
        final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        return df.format(date);
    }

}
