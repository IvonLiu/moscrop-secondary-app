package com.moscrop.official.rss;

import android.content.Context;
import android.content.SharedPreferences;

import com.moscrop.official.util.DateUtil;
import com.moscrop.official.util.JsonUtil;
import com.moscrop.official.util.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ivon on 20/10/14.
 */
public class RSSParser {

    public static final int PAGE_SIZE = Preferences.Default.LOAD_LIMIT;

    private Context mContext;
    private RSSTagCriteria[] mCriteria;

    public RSSParser(Context context) throws JSONException, IOException {
        mContext = context;
        mCriteria = RSSTagCriteria.getCriteriaList(context);
    }

    /**
     * Extract from JSONObject the time the RSS feed was last updated
     *
     * @throws JSONException
     */
    private String getUpdatedTimeFromJsonObject(JSONObject jsonObject) throws JSONException {
        JSONObject feed = jsonObject.getJSONObject("feed");
        JSONObject updated = feed.getJSONObject("updated");
        return updated.getString("$t");
    }

    /**
     * Extract from JSONObject the number of events in the RSS feed
     *
     * @throws JSONException
     */
    private int getTotalResultsCount(JSONObject jsonObject) throws JSONException {
        JSONObject feed = jsonObject.getJSONObject("feed");
        JSONObject totalResults = feed.getJSONObject("openSearch$totalResults");
        return Integer.parseInt(totalResults.getString("$t"));
    }

    /**
     * Extract a list of RSSItems from the JSONObject
     *
     * @throws JSONException
     */
    private List<RSSItem> getEntriesListFromJsonObject(JSONObject jsonObject) throws JSONException {
        JSONObject feed = jsonObject.getJSONObject("feed");
        JSONArray entry = feed.getJSONArray("entry");
        JSONObject entryObjects[] = JsonUtil.extractJsonArray(entry);
        if (entryObjects == null) {
            return null;
        }

        List<RSSItem> items = new ArrayList<RSSItem>();
        for (JSONObject entryObject : entryObjects) {
            RSSItem item = entryObjectToRssItem(entryObject);
            if(item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Helper method that converts a JSONObject representation
     * of an RSS entry (post) to an instance of RSSItem
     *
     * @throws JSONException
     */
    private RSSItem entryObjectToRssItem(JSONObject entryObject) throws JSONException {

        String dateStr = entryObject.getJSONObject("published").getString("$t");
        long date = DateUtil.parseRCF339Date(dateStr).getTime();
        String title = entryObject.getJSONObject("title").getString("$t");
        String content = entryObject.getJSONObject("content").getString("$t");

        JSONObject[] links = JsonUtil.extractJsonArray(entryObject.getJSONArray("link"));
        String url = "";
        for (JSONObject link : links) {
            String type = link.getString("rel");
            if (type.equals("alternate")) {
                url = link.getString("href");
                break;
            }
        }

        String[] tags = extractTags(entryObject);

        return new RSSItem(date, title, content, tags, url);
    }

    /**
     * Determines which tags fit the RSS post from
     * the JSONObject representation of that post
     *
     * @return  String array of tag names that the post belongs to
     */
    private String[] extractTags(JSONObject entryObject) {

        // Get a list of categories
        List<String> categories = new ArrayList<String>();
        try {
            JSONObject[] categoryObjects = JsonUtil.extractJsonArray(entryObject.getJSONArray("category"));
            for (JSONObject o : categoryObjects) {
                categories.add(o.getString("term"));
            }
        } catch (JSONException e) {
            //Logger.error("RSSParser.extractTags(): caught JSONException while parsing categories for " + title, e);
        }

        // Get a list of authors
        List<String> authors = new ArrayList<String>();
        try {
            JSONObject[] categoryObjects = JsonUtil.extractJsonArray(entryObject.getJSONArray("author"));
            for (JSONObject o : categoryObjects) {
                authors.add(o.getJSONObject("name").getString("$t"));
            }
        } catch (JSONException e) {
            //Logger.error("RSSParser.extractTags(): caught JSONException while parsing authors for " + title, e);
        }

        RSSTagCriteria firstMatchedCriteria = null;

        // Check if it matches criteria
        ArrayList<String> tags = new ArrayList<String>();
        for (RSSTagCriteria criteria : mCriteria) {
            if ((criteria.category != null && categories.contains(criteria.category)) || (criteria.author != null && authors.contains(criteria.author))) {
                tags.add(criteria.name);
                if (firstMatchedCriteria == null) {
                    firstMatchedCriteria = criteria;
                }
            }
        }

        // Convert list to array
        String[] tagsArray = new String[tags.size()+1];
        if (firstMatchedCriteria != null) { // Prevent crash in the event there are no matched tags
            tagsArray[0] = firstMatchedCriteria.imageUrl;
        }
        for (int i=1; i<tagsArray.length; i++) {
            tagsArray[i] = tags.get(i-1);
        }

        return tagsArray;
    }

    /**
     * Get RSSFeed object for an online Blogger feed
     *
     * @param url
     *          URL to a Blogger JSON feed
     *
     * @throws JSONException
     */
    private RSSFeed getRssFeed(Context context, String url) throws JSONException {
        JSONObject jsonObject = JsonUtil.getJsonObjectFromUrl(context, url);
        if (jsonObject != null) {
            String timestamp = getUpdatedTimeFromJsonObject(jsonObject);
            List<RSSItem> items = getEntriesListFromJsonObject(jsonObject);
            return new RSSFeed(timestamp, items);
        } else {
            return null;
        }
    }

    private static class RSSInfo {
        private final String version;
        private final int postCount;

        private RSSInfo(String version, int postCount) {
            this.version = version;
            this.postCount = postCount;
        }
    }

    /**
     * Get version info and post count for an online Blogger feed
     *
     * @param url
     *          URL to a Blogger JSON feed
     *
     * @throws JSONException
     */
    private RSSInfo getRssInfo(Context context, String url) throws JSONException {
        JSONObject jsonObject = JsonUtil.getJsonObjectFromUrl(context, url);
        if (jsonObject != null) {
            String timestamp = getUpdatedTimeFromJsonObject(jsonObject);
            int resultCount = getTotalResultsCount(jsonObject);
            return new RSSInfo(timestamp, resultCount);
        } else {
            return null;
        }
    }

    /**
     * Get version info of the cached Blogger feed
     */
    private String getStoredVersion() {
        SharedPreferences prefs = mContext.getSharedPreferences(Preferences.App.NAME, Context.MODE_MULTI_PROCESS);
        return prefs.getString(Preferences.App.Keys.RSS_VERSION, Preferences.App.Default.RSS_VERSION);
    }

    /**
     * Helper method to generate URL of Blogger feed
     *
     * @param blogId
     *          Blogger ID of the blog
     * @param headerInfoOnly
     *          True to generate URL that only retrieves information for an RSSInfo object
     *          False to generate URL for a full list of posts
     */
    private String getFeedUrlFromId(String blogId, boolean headerInfoOnly) {
        if (headerInfoOnly) {
            return "http://" + blogId + ".blogspot.ca/feeds/posts/default?alt=json&max-results=0";
        } else {
            return "http://" + blogId + ".blogspot.ca/feeds/posts/default?alt=json&max-results=" + PAGE_SIZE;
        }
    }

    /**
     * Save the version info so we know
     * which version we are currently on,
     * in case future updates need to compare version info
     *
     * @param version
     *          Version string of the Blogger feed we just downloaded
     */
    private void saveUpdateInfo(String version) {
        SharedPreferences.Editor prefs = mContext.getSharedPreferences(Preferences.App.NAME, Context.MODE_MULTI_PROCESS).edit();
        prefs.putLong(Preferences.App.Keys.RSS_LAST_UPDATED, System.currentTimeMillis());
        prefs.putString(Preferences.App.Keys.RSS_VERSION, version);
        prefs.apply();
    }

    /**
     * Download, parse and save into cache a Blogger feed
     *
     * @param blogId
     *          Blogger ID of the blog
     * @param lastFeedVersion
     *          The last version of this feed that we have cached
     * @param append
     *          False to delete all existing cache and load from scratch
     *          True to only load posts older than the oldest post in cache
     *          and append them to the cache database
     */
    public void parseAndSave(String blogId, String lastFeedVersion, boolean append) {

        //Logger.log("---");

        if (!append) {

            //Logger.log("Normal load");

            // Determine if a full load is needed
            RSSInfo info = null;
            try {
                String url = getFeedUrlFromId(blogId, true);
                //Logger.log("Loading info from: " + url);
                info = getRssInfo(mContext, url);
            } catch (JSONException e) {
                //Logger.error("RSSParser.parseAndSave() info", e);
            }

            if (info != null) {
                //Logger.log("Downloaded version: " + info.version);
                //Logger.log("Stored version:     " + getStoredVersion());
            } else {
                //Logger.log("Info is null!!!");
            }

            if (info != null && !info.version.equals(getStoredVersion())) {

                // Get the list of events from the URL
                RSSFeed feed = null;
                try {
                    String url = getFeedUrlFromId(blogId, false);
                    //Logger.log("Loading feed from: " + url);
                    feed = getRssFeed(mContext, url);
                } catch (JSONException e) {
                    //Logger.error("RSSParser.parseAndSave() feed", e);
                }

                if (feed != null) {

                    // We just updated, so update records
                    // with current time and the version we
                    // just downloaded regardless of whether
                    // updating the database was needed

                    saveUpdateInfo(feed.version);

                    String newFeedVersion = feed.version;
                    if (!newFeedVersion.equals(lastFeedVersion)) {

                        // New version. All our cache is invalid.
                        // Exterminate! Exterminate the cache!
                        // Of course, then replace with new data.

                        RSSDatabase database = new RSSDatabase(mContext);
                        database.deleteAll();
                        database.save(feed.items);
                        database.close();
                    }
                }
            }

        } else {

            //Logger.log("Append load");

            RSSDatabase database = new RSSDatabase(mContext);

            // Determine if a full load is needed
            RSSInfo info = null;
            try {
                String url = getFeedUrlFromId(blogId, true);
                info = getRssInfo(mContext, url);
            } catch (JSONException e) {
                //Logger.error("RSSParser.parseAndSave()", e);
            }

            if (info != null) {
                //Logger.log("Downloaded count: " + info.postCount);
                //Logger.log("Stored count:     " + database.getCount());
            } else {
                //Logger.log("Info is null!!!");
            }

            if (info != null && info.postCount > database.getCount()) {

                RSSFeed feed = null;
                try {
                    String url = getFeedUrlFromId(blogId, false);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    String publishedMaxStr = sdf.format(new Date(database.getOldestPostDate(null)));
                    url = url + "&published-max=" + publishedMaxStr;
                    feed = getRssFeed(mContext, url);
                } catch (JSONException e) {
                    //Logger.error("RSSParser.parseAndSave()", e);
                }

                if (feed != null) {
                    // We just updated, so update records
                    // with current time and the version we
                    // just downloaded regardless of whether
                    // updating the database was needed

                    saveUpdateInfo(feed.version);


                    // New version. All our cache is invalid.
                    // Exterminate! Exterminate the cache!
                    // Of course, then replace with new data.

                    database.save(feed.items);
                }
                database.close();
            }
        }

        //Logger.log("---");
    }
}