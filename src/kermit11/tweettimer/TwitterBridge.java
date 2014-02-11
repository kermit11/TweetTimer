package kermit11.tweettimer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import kermit11.tweettimer.intf.ProgressUpdater;
import kermit11.tweettimer.util.IntHolder;
import kermit11.tweettimer.util.Utils;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

public class TwitterBridge
{

	public static final String AVATAR = "Avatar";
	public static final String SCREEN_NAME = "ScreenName";
	public static final String USER_NAME = "UserName";
	public static final int ERROR_CODE_CONNECTION_FAILURE = -1;
	public static final int ERROR_CODE_USER_DOES_NOT_EXIST = -34;
	public static final int ERROR_CODE_RATE_LIMIT_REACHED = -88;
	//Yes, these should be hidden, but honestly, I don't give a damn. Go ahead and impersonate my app
	public static final String APIKEY = "hiGFm94ms4q07I8kje3Gg";
	public static final String APISECRET = "dIee1TIv32kKaIs3oAlP5PV5IqefmTb4n0RuM2BcsiQ";

	public static final String INTENT_FOLLOWERS_CACHED = "FollowersCached";
	public static final String INTENT_EXTRA_FOLLOWERS = "CachedFollowersExtra";
	
	private static TwitterBridge instance = null;
    private Twitter twitterConnection = null;
	private String[] cachedFollowers = null;
	
	public static TwitterBridge getInstance(SharedPreferences prefs)
	{
		if (instance == null)
		{
			instance = new TwitterBridge();
			//Initialize the connection as well
			String authToken = prefs.getString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN, "");
			String authTokenSecret = prefs.getString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN_SECRET, "");
			
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
              .setOAuthConsumerKey(APIKEY)
              .setOAuthConsumerSecret(APISECRET)
              .setOAuthAccessToken(authToken)
              .setOAuthAccessTokenSecret(authTokenSecret)
              .setUseSSL(true);
            instance.twitterConnection = new TwitterFactory(cb.build()).getInstance();
		}
		return instance;
	}
	
	/**
	 * Perform additional initializations
	 * @param broadcastManager TODO
	 */
	public void doInit(LocalBroadcastManager broadcastManager)
	{
        //Start loading user's followers in the background
		if (cachedFollowers == null)
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
    		//TODO dev mode
			int favs = countLatestFavs(users[i], threshold, earliestDate);
//    		int favs = threshold;
//			try{Thread.sleep(1000);} catch (InterruptedException e){ e.printStackTrace(); }
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
    
    public int countLatestFavs(String forUser, int maxFavs, Date earliestDate)
    {
    	try 
    	{
    		int count=0;
    		ResponseList<Status> favs = twitterConnection.getFavorites(forUser, new Paging(1, maxFavs));
    		for (Iterator<Status> iterator = favs.iterator(); iterator.hasNext();) 
    		{
				Status status = (Status) iterator.next();
				Date stDate = status.getCreatedAt();
				if (stDate.after(earliestDate))
				{
					count++;
				}
				
			}
    		return count;
    	}
    	catch (TwitterException e) 
    	{
    		int errCode = e.getErrorCode();
    		//Twitter stopped returning specific error code for user not found
    		if (errCode == ERROR_CODE_CONNECTION_FAILURE && !e.isCausedByNetworkIssue())
    		{
    			errCode = ERROR_CODE_USER_DOES_NOT_EXIST;
    		}
       		//Use negative numbers for error codes
    		return (errCode<0)?errCode:-1*errCode;
    	}
    }

    /**
     * Check if the given user-name is the name of an existing Twitter user
     * @param userName
     * @return
     */
	public HashMap<String, Object> getUserDetails(String userName)
	{
		try
		{
			HashMap<String, Object> user = new HashMap<String, Object>();
			User twitterUser = twitterConnection.showUser(userName);
			user.put(USER_NAME, twitterUser.getName());
			user.put(SCREEN_NAME, twitterUser.getScreenName());
			String avatarURL = twitterUser.getBiggerProfileImageURL();
			user.put(AVATAR, Utils.getDrawableFromURL(avatarURL));
			return user;
		} catch (TwitterException e)
		{
			return null;
		}
	}
	
	/**
	 * Get cached version of followers list if available  
	 */
	public String[] getFollowersCached()
	{
		return cachedFollowers;
	}
	
	/**
	 * 	Fetch followers of logged on user
	 */
	//TODO error handling
	public String[] getFollowers()
	{
		String[] allFollowers = null;
		try
		{
			long[] followerIDs = twitterConnection.getFollowersIDs(-1).getIDs();
			allFollowers = new String[followerIDs.length];
			long[] portion = new long[100];

			for (int startPos = 0; startPos < followerIDs.length; )
			{
				int nextPortionSize = Math.min(100, followerIDs.length-startPos);
				if (nextPortionSize<200) portion = new long[nextPortionSize];
				System.arraycopy(followerIDs, startPos, portion, 0, nextPortionSize);
				ResponseList<User> fo = twitterConnection.lookupUsers(portion);

				for (Iterator<User> iterator = fo.iterator(); iterator.hasNext();)
				{
					User user = (User) iterator.next();
					allFollowers[startPos++] = user.getScreenName();
				}

			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return allFollowers;
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
			String[] followers = TwitterBridge.this.getFollowers();
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
