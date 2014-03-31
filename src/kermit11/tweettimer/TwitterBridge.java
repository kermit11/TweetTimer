package kermit11.tweettimer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kermit11.tweettimer.intf.ProgressUpdater;
import kermit11.tweettimer.util.IntHolder;
import kermit11.tweettimer.util.Utils;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

public class TwitterBridge
{

	public static final String INTENT_FOLLOWERS_CACHED = "FollowersCached";
	public static final String INTENT_EXTRA_FOLLOWERS = "CachedFollowersExtra";
	
    private String[] cachedFollowers = null;
	private TwitterTasks twitter = null;
	private static TwitterBridge instance = null;
	public static TwitterBridge getInstance(SharedPreferences prefs)
	{
		if (instance == null)
		{
			instance = new TwitterBridge();
			String authToken = prefs.getString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN, "");
			String authTokenSecret = prefs.getString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN_SECRET, "");
			instance.twitter = new TwitterTasks(authToken, authTokenSecret);
		}
		return instance;
	}
	
	/**
	 * Perform additional initializations
	 * @param broadcastManager
	 */
	public void doInit(LocalBroadcastManager broadcastManager)
	{
        //Start loading user's followers in the background
		if (cachedFollowers == null || cachedFollowers.length == 0)
		{
			LoadFollowersTask loadTask = new LoadFollowersTask();
			loadTask.execute(broadcastManager);
		}
		
	}
	
    public List<String> getActiveUsers(String[] users, int threshold, Date earliestDate, IntHolder errorCode, ProgressUpdater progress)
    {
    	List<String> activeUsers = new ArrayList<String>();
    	for (int i = 0; i < users.length; i++)
    	{
			int favs = twitter.countLatestFavs(users[i], threshold, earliestDate);
    		if (favs < 0)
    		{
    			errorCode.val = favs;
    			continue; //Skip this user, last error code will be returned
    		}
    		
			if (favs == threshold)
			{
				activeUsers.add(users[i]);
			}
			progress.publishProgress(i+1, users.length);
		}
    	return activeUsers;
    }
    
    /**
	 * Check if the given user-name is the name of an existing Twitter user
	 * @param userName
	 * @return
	 */
	public HashMap<String, Object> getUserDetails(String userName)
	{
		HashMap<String, Object> userDetails = twitter.getUserDetails(userName);
		if (userDetails != null)
		{
			//Convert avatar URL to Android image
			String avatarURL = (String) userDetails.get(TwitterTasks.AVATAR);
			userDetails.put(TwitterTasks.AVATAR, Utils.getDrawableFromURL(avatarURL));
		}
		return userDetails;
	}
	
	/**
	 * Get cached version of followers list if available  
	 */
	public String[] getFollowersCached()
	{
		return cachedFollowers;
	}
	
	/**
	 * AsyncTask to fetch followers using twitter APIs
	 */
	private class LoadFollowersTask extends AsyncTask<LocalBroadcastManager,Void,String[]>
	{
		private LocalBroadcastManager broadcastManager = null;

		protected String[] doInBackground(LocalBroadcastManager... params)
		{
			broadcastManager = params[0];
			String[] followers = TwitterBridge.this.twitter.getFollowers();
			return followers;
		}
		
		protected void onPostExecute(String[] result)
		{
			cachedFollowers = result;
			//Notify any activities waiting for followers to be cached
			Intent i = new Intent(INTENT_FOLLOWERS_CACHED);
			i.putExtra(INTENT_EXTRA_FOLLOWERS, result);
			broadcastManager.sendBroadcast(i);
		}
	}

}
