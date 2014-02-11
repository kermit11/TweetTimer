package kermit11.tweettimer;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import kermit11.tweettimer.util.IntHolder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;

public class FavoritesLoader extends AsyncTaskLoader<List<String>> 
{ 
	private List<String> mData;
	private WeakReference<MainActivity> mActivity;
	
	private boolean loadInProgress = false;
	
	private SharedPreferences prefs;
	
	private IntHolder errorCode = new IntHolder(0);
	public IntHolder getErrorCode()
	{
		return errorCode;
	}

	public FavoritesLoader(MainActivity context)
	{
		super(context);
		mActivity = new WeakReference<MainActivity>(context);
	}

	@Override
	public List<String> loadInBackground() 
	{
		prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		
		//Prepare time parameter
		int period = Integer.parseInt(prefs.getString(SettingsActivity.PREF_PERIOD, SettingsActivity.DEFAULT_PERIOD));
        Date earlyDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(earlyDate);
        cal.add(Calendar.MINUTE, -period);
        
        //Prepare threshold parameter
        int threshold = Integer.parseInt(prefs.getString(SettingsActivity.PREF_THRESHOLD, SettingsActivity.DEFAULT_THRESHOLD));
        
        //Prepare user list parameter
        String[] users = SettingsActivity.readSettingArray(prefs);
                
        //Call check method
        TwitterBridge twitter = TwitterBridge.getInstance(prefs);
		List<String> activeUsers = twitter.getActiveUsers(users, threshold, cal.getTime(), errorCode, mActivity.get());
 
        return activeUsers;
	}

	@Override
	public void deliverResult(List<String> data) 
	{
		loadInProgress = false;
		
		if (isReset()) 
		{
			onReleaseResources(data);
			return;
		}

		List<String> oldData = mData;
		mData = data;

		if (isStarted()) 
		{
			super.deliverResult(data);
		}

		// Invalidate the old data as we don't need it any more.
		if (oldData != null && oldData != data) 
		{
			onReleaseResources(oldData);
		}
	}

	@Override
	protected void onStartLoading() 
	{
		if (mData != null) 
		{
			deliverResult(mData);
		}

		if (!loadInProgress && (takeContentChanged() || mData == null)) 
		{
			loadInProgress = true;
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() 
	{
		//Do not stop loaders unless they actually finished (to stop multiple loaders from being created).
		if (!loadInProgress)
		{
			cancelLoad();
		}
	}


	@Override
	protected void onReset() 
	{
		onStopLoading();

		// At this point we can release the resources associated with 'mData'.
		if (mData != null) 
		{
			onReleaseResources(mData);
			mData = null;
			
		}

	}

	@Override
	public void onCanceled(List<String> data) 
	{
		super.onCanceled(data);

		onReleaseResources(data);
	}


	protected void onReleaseResources(List<String> data) 
	{
		mActivity = null;
	}
	
}
