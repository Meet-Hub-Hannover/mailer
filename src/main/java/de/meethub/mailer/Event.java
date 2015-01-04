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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.filter.Rule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

public class Event implements Comparable<Event> {

    private final String title;
    private final String url;
    private final Date start;
    private final Date end;

    private Event(final String title, final String url, final Period period) {
        this.title = title;
        this.url = url;
        this.start = period.getRangeStart();
        this.end = period.getRangeEnd();
    }

    public static List<Event> getEventsInPeriod(final Calendar cal, final Period period) {
        final Filter filter = new Filter(new Rule[] {new PeriodRule(period)}, Filter.MATCH_ALL);
        final List<Component> filtered = (List<Component>) filter.filter(cal.getComponents());
        final List<Event> ret = new ArrayList<>();
        for (final Component comp : filtered) {
            if  (!(comp instanceof VEvent)) {
                continue;
            }
            final PeriodList recurrenceSet = comp.calculateRecurrenceSet(period);
            for (final Object p : recurrenceSet) {
                ret.add(new Event(
                        getValueIfExists(comp, Property.SUMMARY),
                        getValueIfExists(comp, Property.URL),
                        (Period) p));
            }
        }
        Collections.sort(ret);
        return ret;
    }

    private static String getValueIfExists(final Component comp, final String key) {
        final Property p = comp.getProperty(key);
        return p == null ? null : p.getValue();
    }

    public String getTitle() {
        return this.title;
    }

    public String getURL() {
        return this.url;
    }

    public Date getStart() {
        return this.start;
    }

    @Override
    public int compareTo(final Event o) {
        int cmp;
        cmp = this.start.compareTo(o.start);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.end.compareTo(o.end);
        if (cmp != 0) {
            return cmp;
        }
        return this.title.compareTo(o.title);
    }

    @Override
    public int hashCode() {
        return this.title.hashCode() ^ this.start.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Event)) {
            return false;
        }
        final Event e = (Event) o;
        return this.compareTo(e) == 0;
    }

}
