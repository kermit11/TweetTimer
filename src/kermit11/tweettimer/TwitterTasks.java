package kermit11.tweettimer;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterTasks
{
	private Twitter twitterConnection = null;
	public static final String USER_NAME = "UserName";
	public static final String SCREEN_NAME = "ScreenName";
	public static final String AVATAR = "Avatar";
	public static final int ERROR_CODE_RATE_LIMIT_REACHED = -88;
	public static final int ERROR_CODE_USER_DOES_NOT_EXIST = -34;
	public static final int ERROR_CODE_CONNECTION_FAILURE = -1;
	//Yes, these should be hidden, but honestly, I don't give a damn. Go ahead and impersonate my app
	public static final String APIKEY = "hiGFm94ms4q07I8kje3Gg";
	public static final String APISECRET = "dIee1TIv32kKaIs3oAlP5PV5IqefmTb4n0RuM2BcsiQ";
	
	public TwitterTasks(String authToken, String authTokenSecret)
	{
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
          .setOAuthConsumerKey(APIKEY)
          .setOAuthConsumerSecret(APISECRET)
          .setOAuthAccessToken(authToken)
          .setOAuthAccessTokenSecret(authTokenSecret)
          .setUseSSL(true);
        twitterConnection = new TwitterFactory(cb.build()).getInstance();
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
			if (errCode == TwitterTasks.ERROR_CODE_CONNECTION_FAILURE && !e.isCausedByNetworkIssue())
			{
				errCode = TwitterTasks.ERROR_CODE_USER_DOES_NOT_EXIST;
			}
	   		//Use negative numbers for error codes
			return (errCode<0)?errCode:-1*errCode;
		}
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
			return new String[0];
		}
	
		return allFollowers;
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
			user.put(TwitterTasks.USER_NAME, twitterUser.getName());
			user.put(TwitterTasks.SCREEN_NAME, twitterUser.getScreenName());
			user.put(TwitterTasks.AVATAR, twitterUser.getBiggerProfileImageURL());
			return user;
		} catch (TwitterException e)
		{
			return null;
		}
	}

}
