package kermit11.tweettimer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kermit11.tweettimer.intf.ProgressUpdater;
import kermit11.tweettimer.util.IntHolder;
import kermit11.tweettimer.util.StringListAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//TODO (next version) background service for alerts
//TODO (next version) automatic mode
//TODO (next version) multiple lists
//TODO (consider)keep old data in case of failure
//TODO (consider) add avatars

public class MainActivity extends ActionBarActivity implements LoaderCallbacks<List<String>>, ProgressUpdater 
{

    public static final String COLOR_ACTIONBAR = "#33336C";
	public static final String COLOR_BG = "#666699";
    
	private ArrayList<String> namesList = new ArrayList<String>();
    private StringListAdapter adapter = null;
	private SharedPreferences prefs = null;

	//Singleton applicationContext to be available for non-activity classes
	private static Context appContext = null;
	public static Context getContext()
	{
		return appContext;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	try
    	{
    		super.onCreate(savedInstanceState);
    		setContentView(R.layout.activity_main);
    		
    		ActionBar actionBar = getSupportActionBar();
        	actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(COLOR_ACTIONBAR)));

    		appContext = getApplicationContext();
    		prefs = PreferenceManager.getDefaultSharedPreferences(this);

    		//Start fetching user's data in background, to save time when we need to show it
    		TwitterBridge.getInstance(prefs).doInit(LocalBroadcastManager.getInstance(getContext()));
    		
    		//If user list is empty, assume it's the first visit (after auth) and welcome the user
        	String[] userList = SettingsActivity.readSettingArray(prefs);
        	if (userList.length == 0)
        	{
        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        		builder.setTitle(R.string.welcome_message_title);
        		builder.setMessage(R.string.welcome_message_main_desc);
        		builder.setPositiveButton(android.R.string.ok, null );
        		AlertDialog dialog = builder.create();
        		dialog.show();
        	}
        	
    		//Prepare the ListView object
        	ListView lv = (ListView)findViewById(R.id.namesListView);
    		adapter = new StringListAdapter(this, R.layout.list_item_main, namesList );
    		lv.setAdapter(adapter);
    		
    		//Edit appearance and behavior of Refresh button
    		final ImageButton refreshButton = (ImageButton) findViewById(R.id.refreshButton);
    		refreshButton.getBackground().setAlpha(60);
    		refreshButton.setOnClickListener(new OnClickListener()
    		{
    			@Override
    			public void onClick(View arg0)
    			{
    				refreshButton.setEnabled(false);
    				//Set header text while loading results
    	        	TextView namesListHeader = (TextView) findViewById(R.id.namesListHeader);
    	        	namesListHeader.setText(getString(R.string.active_users_default)+"...");
    	        	//Call loader
    				getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
    			}
    		});

    		//Edit appearance and behavior of Settings button
    		ImageButton settingsButton = (ImageButton) findViewById(R.id.settingsButton);
    		settingsButton.getBackground().setAlpha(60);
    		settingsButton.setOnClickListener(new OnClickListener()
    		{
    			@Override
    			public void onClick(View v)
    			{
    				//Launch the settings activity
    				Intent i = new Intent(MainActivity.this,SettingsActivity.class);
    				startActivity(i);
    			}
    		});

    		//Edit appearance and behavior of Info button
    		ImageButton infoButton = (ImageButton) findViewById(R.id.infoButton);
    		infoButton.getBackground().setAlpha(60);
    		infoButton.setOnClickListener(new OnClickListener()
    		{
    			@Override
    			public void onClick(View v)
    			{
    				//Launch the help activity
    				Intent i = new Intent(MainActivity.this,HelpActivity.class);
    				startActivity(i);
    			}
    		});

    		//Load the results using a background loader
    		boolean checkOnStart = prefs.getBoolean(SettingsActivity.PREF_CHECK_ON_START, SettingsActivity.DEFAULT_CHECK_ON_START);
    		if (checkOnStart)
    		{
        		getSupportLoaderManager().initLoader(0, null, this);
    		}
    		else
    		{
            	TextView namesListHeader = (TextView) findViewById(R.id.namesListHeader);
            	namesListHeader.setText(R.string.active_users_not_loaded);
    		}
    	}
    	catch (Exception e)
    	{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.general_error_title);
    		builder.setMessage(R.string.general_error_desc);
    		builder.setPositiveButton(android.R.string.ok, null);
    		AlertDialog dialog = builder.create();
    		dialog.show();
	
    	}
    }



	@Override
	public Loader<List<String>> onCreateLoader(int id, Bundle args)
	{
		return new FavoritesLoader(this);
	}


	@Override
	public void onLoadFinished(Loader<List<String>> loader, List<String> data) 
	{
		IntHolder errCode = ((FavoritesLoader)loader).getErrorCode();
		//Update the list
		if (data != null)
		{
	        namesList.clear();
	        namesList.addAll(data);
	        adapter.notifyDataSetChanged();
	        //Refresh was successful, so record the date
			Editor editor = prefs.edit();
            editor.putLong(SettingsActivity.PREF_LAST_REFRESH, new Date().getTime());
            editor.commit();
		}
		//And the header
		updateHeader();

		//Handle errors
		if (errCode.val == TwitterTasks.ERROR_CODE_USER_DOES_NOT_EXIST)
		{
			//TODO (next version) Propose to clean list automatically
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.general_error_title);
    		builder.setMessage(R.string.error_user_missing_desc);
    		builder.setPositiveButton(android.R.string.ok, null);
    		AlertDialog dialog = builder.create();
    		dialog.show();
		}
		else if (errCode.val == TwitterTasks.ERROR_CODE_RATE_LIMIT_REACHED)
		{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.general_error_title);
    		builder.setMessage(R.string.error_rate_limit_desc);
    		builder.setPositiveButton(android.R.string.ok, null);
    		AlertDialog dialog = builder.create();
    		dialog.show();
		}
		else if (errCode.val == TwitterTasks.ERROR_CODE_CONNECTION_FAILURE)
		{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.general_error_title);
    		builder.setMessage(R.string.error_connection_fail_desc);
    		builder.setPositiveButton(android.R.string.ok, null);
    		AlertDialog dialog = builder.create();
    		dialog.show();
		}
		else if (errCode.val < 0) 
		{
			Toast.makeText(this, "Error fetching favorites from twitter :/", Toast.LENGTH_LONG).show();
		}
		
		ImageButton refreshButton = (ImageButton) findViewById(R.id.refreshButton);
		refreshButton.setEnabled(true);
	
	}


	@Override
	public void onLoaderReset(Loader<List<String>> loader)
	{
		
	}

    private void updateHeader()
    {
		if (namesList.size()>0)
        {
			String minutes = prefs.getString(SettingsActivity.PREF_PERIOD, SettingsActivity.DEFAULT_PERIOD);
        	TextView namesListHeader = (TextView) findViewById(R.id.namesListHeader);
        	namesListHeader.setText(getString(R.string.active_users_prefix) + " " + minutes + " " + getString(R.string.active_users_suffix));
        }
        else
        {
        	TextView namesListHeader = (TextView) findViewById(R.id.namesListHeader);
        	namesListHeader.setText(R.string.active_users_none);
        }
    	
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_settings:
				Intent iSettings = new Intent(MainActivity.this,SettingsActivity.class);
				startActivity(iSettings);
	            return true;
	        case R.id.action_help:
				Intent iHelp = new Intent(MainActivity.this,HelpActivity.class);
				startActivity(iHelp);
	            return true;
	        case R.id.action_logout:
	        	Intent iWelcome = new Intent(MainActivity.this,WelcomeActivity.class);
	        	iWelcome.putExtra("LOGOUT", true);
				iWelcome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(iWelcome);
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback method for updating progress of loader on UI
	 */
	public void publishProgress(final int current, final int max)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
	        	TextView namesListHeader = (TextView) findViewById(R.id.namesListHeader);
	        	namesListHeader.setText(getString(R.string.active_users_default) + " (" + current + "/" + max + ") ...");
			}
		});
	}

}
