package com.moscropsecondary.official.calendar;

import android.content.Context;
import android.content.SharedPreferences;

import com.moscropsecondary.official.util.DateUtil;
import com.moscropsecondary.official.util.JsonUtil;
import com.moscropsecondary.official.util.Logger;
import com.moscropsecondary.official.util.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Helper class that performs Google
 * Calendar specific JSON parsing.
 *
 * Created by ivon on 9/2/14.
 */
public class CalendarParser {

    public static final String API_KEY = "AIzaSyDQgD1es2FQdm4xTA1tU8vFniOglwe4HsE";

    private static class CalendarInfo {
        private final String version;

        private CalendarInfo(String version) {
            this.version = version;
        }
    }

    private static CalendarInfo getCalendarInfo(Context context, String url) throws JSONException {
        JSONObject jsonObject = JsonUtil.getJsonObjectFromUrl(context, url);
        if (jsonObject != null) {
            String timestamp = getUpdatedTimeFromJsonObject(jsonObject);
            return new CalendarInfo(timestamp);
        } else {
            return null;
        }
    }

    private static String getStoredVersion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Preferences.App.NAME, Context.MODE_MULTI_PROCESS);
        return prefs.getString(Preferences.App.Keys.GCAL_VERSION, Preferences.App.Default.GCAL_VERSION);
    }

    private static String getUpdatedTimeFromJsonObject(JSONObject root) throws JSONException {
        return root.getString("updated");
    }

    private static List<GCalEvent> getEventsListFromJsonObject(JSONObject root) throws JSONException {
        JSONArray items = root.getJSONArray("items");
        JSONObject itemObjects[] = JsonUtil.extractJsonArray(items);
        if (itemObjects == null) {
            return null;
        }

        List<GCalEvent> events = new ArrayList<GCalEvent>();
        for (JSONObject entryObject : itemObjects) {
            GCalEvent event = jsonItemToEvent(entryObject);
            if(event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private static GCalEvent jsonItemToEvent(JSONObject itemObject) {

        String title = null;
        String description = null;
        String location = null;
        long startTime = -1;
        long endTime = -1;

        try {
            title = itemObject.getString("summary");
        } catch (JSONException e) {
            //e.printStackTrace();
        }

        try {
            description = itemObject.getString("description");
        } catch (JSONException e) {
            //e.printStackTrace();
        }

        try {
            location = itemObject.getString("location");
        } catch (JSONException e) {
            //e.printStackTrace();
        }

        try {
            String s = itemObject.getJSONObject("start").getString("dateTime");
            startTime = DateUtil.parseRCF339Date(s, false).getTime();
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        if (startTime == -1) {
            try {
                String s = itemObject.getJSONObject("start").getString("date");
                startTime = DateUtil.parseRCF339Date(s, true).getTime();
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }

        try {
            String s = itemObject.getJSONObject("end").getString("dateTime");
            endTime = DateUtil.parseRCF339Date(s, false).getTime();
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        if (endTime == -1) {
            try {
                String s = itemObject.getJSONObject("end").getString("date");
                endTime = DateUtil.parseRCF339Date(s, true).getTime();
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }

        return new GCalEvent(title, description, location, startTime, endTime);
    }

    private static CalendarFeed getCalendarFeed(Context context, String url) throws JSONException {
        JSONObject jsonObject = JsonUtil.getJsonObjectFromUrl(context, url);
        if (jsonObject != null) {
            String timestamp = getUpdatedTimeFromJsonObject(jsonObject);
            List<GCalEvent> events = getEventsListFromJsonObject(jsonObject);
            return new CalendarFeed(timestamp, events);
        } else {
            return null;
        }
    }

    private static String getCalendarUrlFromId(String id, String timeMin, boolean headerInfoOnly) {
        if (!headerInfoOnly) {
            if (timeMin == null) {
                return "https://www.googleapis.com/calendar/v3/calendars/" + id + "/events?maxResults=1000&orderBy=startTime&singleEvents=true&key=" + API_KEY;
            } else {
                return "https://www.googleapis.com/calendar/v3/calendars/" + id + "/events?maxResults=1000&orderBy=startTime&singleEvents=true&timeMin=" + timeMin + "&key=" + API_KEY;
            }
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String now = sdf.format(new Date(System.currentTimeMillis()));
            return "https://www.googleapis.com/calendar/v3/calendars/" + id + "/events?maxResults=1&timeMax=" + now + "&timeMax=" + now + "&key=" + API_KEY;
        }
    }

    private static void saveUpdateInfo(Context context, String gcalVersion) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Preferences.App.NAME, Context.MODE_MULTI_PROCESS).edit();
        prefs.putLong(Preferences.App.Keys.GCAL_LAST_UPDATED, System.currentTimeMillis());
        prefs.putString(Preferences.App.Keys.GCAL_VERSION, gcalVersion);
        prefs.apply();
    }

    /**
     * Download, parse, and store data from a Google Calendar feed. This method
     * will load all data from the whole calendar. Unlike process(String, long, String),
     * this method does not check for Gcal version and will disregard any previously
     * saved data. This method will delete all previously saved data and replace it
     * with freshly downloaded data. Because this takes a long time
     * and is often unnecessary, it is only recommended to use this method
     * when loading for the first time. Afterwards it is recommeneded to keep
     * track of updates and use process(String, long, String).
     *
     * @param id
     *      ID of the Google Calendar
     */
    public static void parseAndSaveAll(Context context, String id) {

        Logger.log("Processing all");

        // Get the list of events from the URL
        CalendarFeed feed = null;
        try {
            String url = getCalendarUrlFromId(id, null, false);
            feed = getCalendarFeed(context, url);
        } catch (JSONException e) {
            Logger.error("CalendarParser.parseAndSaveAll()", e);
        }

        if (feed != null) {
            saveUpdateInfo(context, feed.version);
            CalendarDatabase db = new CalendarDatabase(context);
            db.deleteAll();
            db.saveEventsToProvider(feed.events);
            db.close();
        }
    }

    /**
     * Selectively download, parse, and store data from a Google Calendar feed
     *
     * @param id
     *      ID of the Google Calendar
     * @param timeMin
     *      Matches timeMin query param of Google Calendar API.
     *      Only process calendar events with start dates after this param.
     *      Given in milliseconds.
     * @param lastGcalVersion
     *      "Updated" string from the last processed Google Calendar feed. Used to version
     *      Google Calendar feed and determine if it is necessary to go through with processing.
     *      After all, if it's the same version, no need to do all that work again!
     *      Usually of the format "2014-09-09T12:21:08.000Z"
     */
    public static void parseAndSave(Context context, String id, long timeMin, String lastGcalVersion) {

        Logger.log("Processing selectively");

        // Determine if a full load is needed
        CalendarInfo info = null;
        try {
            String url = getCalendarUrlFromId(id, null, true);
            Logger.log("Loading info from: " + url);
            info = getCalendarInfo(context, url);
        } catch (JSONException e) {
            //Logger.error("RSSParser.parseAndSave() info", e);
        }

        if (info != null) {
            //Logger.log("Downloaded version: " + info.version);
            //Logger.log("Stored version:     " + getStoredVersion(context));
        } else {
            //Logger.log("Info is null!!!");
        }

        if (info != null && !info.version.equals(getStoredVersion(context))) {

            CalendarFeed feed = null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                String timeMinStr = sdf.format(new Date(timeMin));
                String url = getCalendarUrlFromId(id, timeMinStr, false);
                feed = getCalendarFeed(context, url);
            } catch (JSONException e) {
                Logger.error("CalendarParser.process()", e);
            }

            if (feed != null) {

                // We just updated, so update records
                // with current time and the version we
                // just downloaded regardless of whether
                // updating the database was needed

                saveUpdateInfo(context, feed.version);
                CalendarDatabase db = new CalendarDatabase(context);

                String newGcalVersion = feed.version;
                if (!newGcalVersion.equals(lastGcalVersion)) {

                    // There has been changes! We must update!
                    //
                    // The maintainer of the calendar probably
                    // won't make changes to events that have
                    // already past. Therefore we only need to
                    // update events that begin after the last
                    // update time.
                    //
                    // Begin by deleting those events

                    int deleted = db.deleteAfterTime(timeMin);

                    if (deleted != feed.events.size()) {
                        Logger.warn("Processing calendar events: deleted "
                                        + deleted + " events from database, but only inserting "
                                        + feed.events.size() + " new events."
                        );
                    }

                    // Our list of events will already only consist
                    // of events that begin after startMin, so no
                    // overlapping will occur. We can save normally.

                    db.saveEventsToProvider(feed.events);

                } else {
                    Logger.log("Existing version is already up to date.");
                }
                db.close();
            }
        }
    }


}
