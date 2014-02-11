package kermit11.tweettimer;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

//TODO *consider* Secondary text below is for status, not description

//TODO consider moving nested preference screen to separate activity (see: http://stackoverflow.com/questions/2615528/preferenceactivity-and-theme-not-applying )
//TODO enumerate users

public class SettingsActivity extends PreferenceActivity
{
	public static final String FAV_LIST_PREFIX = "FAV_LIST_";
	public static final String PREF_FAV_LIST_SIZE = "FAV_LISTSIZE";
	public static final String PREF_THRESHOLD = "THRESHOLD";
	public static final String DEFAULT_THRESHOLD = "1";
	public static final String PREF_PERIOD = "PERIOD";
	public static final String DEFAULT_PERIOD = "15";
	public static final String PREF_ALARM_STATE = "ALARM_STATE";
	public static final boolean DEFAULT_ALARM_STATE = false;
	public static final String PREF_ALARM_TRIGGER = "ALARM_TRIGGER";
	public static final int DEFAULT_ALARM_TRIGGER = 5;
	public static final String PREF_CHECK_ON_START = "CHECK_ON_START";
	public static final boolean DEFAULT_CHECK_ON_START = true;
	
	public static final String PREF_LAST_REFRESH = "LAST_REFRESH";
	public static final String PREF_TWITTER_ACCESS_TOKEN = "ACCESS_TOKEN";
	public static final String PREF_TWITTER_ACCESS_TOKEN_SECRET = "ACCESS_TOKEN_SECRET";
	
	public static final String SCREEN_FAVLIST = "FAV_LIST";
	public static final String DUMMY_PREF_FAVLIST_ADD = "FAVLIST_ADD";
	private static final int MAX_LIST_USERS = 15;
	public static final String INTENT_EXTRA_OPENING_PREFERENCE = "OPENING_PREFERENCE";
	private SharedPreferences prefs;
	private OnPreferenceClickListener opClickLstn = null; 
	
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	
    	//Styling PreferenceActivity is a bit complex, we have to style every part in a different way instead of using one theme
    	View view = this.getWindow().getDecorView();
    	view.setBackgroundColor(Color.parseColor(MainActivity.COLOR_BG));
    	setTheme(R.style.PreferenceActivityTheme);
    	
    	//TODO action bar support for preference activity
//    	ActionBar actionBar = getSupportActionBar();
//    	actionBar.setDisplayHomeAsUpEnabled(true);
    	
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //Make sure these are numbers only
        ((EditTextPreference)findPreference(SettingsActivity.PREF_PERIOD)).getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        ((EditTextPreference)findPreference(SettingsActivity.PREF_THRESHOLD)).getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        opClickLstn = new RemoveUserOnPreferenceClickListener();
        
        populateFavList();
    }

	/**
	 * Dynamically populate the user list preference screen
	 */
	@SuppressWarnings("deprecation")
	protected void populateFavList()
	{
        final PreferenceScreen favListPref = (PreferenceScreen) findPreference(SCREEN_FAVLIST);

        //Set listener for "Add user"
        Preference favListAddPref = findPreference(DUMMY_PREF_FAVLIST_ADD);
        favListAddPref.setOnPreferenceClickListener(new AddUserOnPreferenceClickListener());

		//Create a Preference for each user in the SharedPrefs
        String[] favList = readSettingArray(prefs);
        for (int i = 0; i < favList.length; i++)
		{
			String userName = favList[i];
			
            final Preference pref = new Preference(getApplicationContext());
            pref.setTitle(userName);
            pref.setSummary(R.string.settings_list_remove);
            pref.setKey(FAV_LIST_PREFIX+i);
            pref.setOnPreferenceClickListener(opClickLstn);
            favListPref.addPreference(pref);
        }       
        
        //Disable the button if there's no room for more users
        toggleAddVisibility(true);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) 
		{
		case (UserSelectionActivity.ACTIVITY_REQUEST_CODE) : 
		{
			if (resultCode == Activity.RESULT_OK) 
			{
				String selectedVal = data.getStringExtra(UserSelectionActivity.ACTIVITY_RESULT_SELECTED_VAL);
				if (selectedVal == null || selectedVal.equals("")) return;
				checkAndAddUser(selectedVal);
			}
			break;
		} 
		}
	}

	/**
	 * Check if the given user name can be added to the list, and if so, add it
	 * @param newValue
	 * @return
	 */
	protected void checkAndAddUser(String userName)
	{
		//Check if the user is not already in the list
        String[] favList = readSettingArray(prefs);
        for (int i = 0; i < favList.length; i++)
		{
			String curUser = favList[i];
			if (curUser.equalsIgnoreCase(userName)) return;
		}

        //Block adding until we're done with current user
        toggleAddVisibility(false);

        //Perform actual connection using AsyncTask
        GetUserDetailsTask gudTask = new GetUserDetailsTask();
        gudTask.execute(userName,this);
	}

	/**
	 * Add a user to the list and save it in the preferences
	 */
	public void addUser(String userName)
	{
        //Create a new Preference and add to screen
        int listSize = prefs.getInt(PREF_FAV_LIST_SIZE, 0);
    	@SuppressWarnings("deprecation")
        PreferenceScreen favListPref = (PreferenceScreen) findPreference(SCREEN_FAVLIST);
        final Preference pref = new Preference(getApplicationContext());
        pref.setTitle(userName);
        pref.setSummary(R.string.settings_list_remove);
        pref.setKey(FAV_LIST_PREFIX+(listSize));
        pref.setOnPreferenceClickListener(opClickLstn);
        favListPref.addPreference(pref);
		        
        //Add also to SharedPrefs
        Editor editor = prefs.edit();
        editor.putInt(PREF_FAV_LIST_SIZE, ++listSize);
        editor.putString(pref.getKey(), userName);
        editor.commit();
	}

	/**
	 * Determine correct visibility for "Add Users" button and set it 
	 */
	public void toggleAddVisibility(boolean newState)
	{
    	@SuppressWarnings("deprecation")
		Preference favListAddPref =  findPreference(DUMMY_PREF_FAVLIST_ADD);
        int listSize = prefs.getInt(PREF_FAV_LIST_SIZE, 0);

        if (newState && listSize <= MAX_LIST_USERS)
        {
        	favListAddPref.setEnabled(true);
        }
        if (newState && listSize == MAX_LIST_USERS)
        {
        	favListAddPref.setEnabled(false);
        }

        if (!newState)
        {
        	favListAddPref.setEnabled(false);
        }
	}


	/**
	 * Navigate up on "home" button
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            finish();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	
	//Workaround for styling sub-PreferenceScreen (Android issue 4611).
    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
    {
    	super.onPreferenceTreeClick(preferenceScreen, preference);
    	if (preference!=null)
	    	if (preference instanceof PreferenceScreen)
	        	if (((PreferenceScreen)preference).getDialog()!=null)
	        		((PreferenceScreen)preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
    	return false;
    }

    
    /**
     * This listener will be used to remove names that have been tapped 
     */
    private class RemoveUserOnPreferenceClickListener implements OnPreferenceClickListener
    {
		@Override
		public boolean onPreferenceClick(final Preference preference)
		{
			//Confirm removal
	    	AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
	    	builder.setTitle(R.string.settings_list_remove_confirm);
	    	builder.setMessage(preference.getTitle());
	    	builder.setPositiveButton(android.R.string.ok, new OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					removeUser(preference);				
				}
			});
	    	builder.setNegativeButton(android.R.string.cancel, null);
	    	builder.show();
            return true;
		}
		
		/**
		 * Actually remove tapped user
		 */
		private void removeUser(Preference preference)
		{
			@SuppressWarnings("deprecation")
			PreferenceScreen favListPref = (PreferenceScreen) findPreference(SCREEN_FAVLIST);
			favListPref.removePreference(preference);

			//Remove also from SharedPrefs
			int listSize = prefs.getInt(PREF_FAV_LIST_SIZE,0);
			String lastKey = FAV_LIST_PREFIX + (listSize-1);
			Editor editor = prefs.edit();
			//In case it's removed from the middle, put the last one in its place and trim the list
			if (!lastKey.equals(preference.getKey()))
			{
				String currentLast = prefs.getString(lastKey, "");
				editor.putString(preference.getKey(), currentLast);
			}
			editor.putInt(PREF_FAV_LIST_SIZE, --listSize);
			editor.commit();

			//if add button was disabled, there's room for new users now
			toggleAddVisibility(true);
		}
    }

    
    /**
     * This listener will be used to invoke the "add user" screen 
     */
    private class AddUserOnPreferenceClickListener implements OnPreferenceClickListener
    {
		@Override
		public boolean onPreferenceClick(Preference preference)
		{
        	Intent iUserSelection = new Intent(SettingsActivity.this,UserSelectionActivity.class);
        	startActivityForResult(iUserSelection, UserSelectionActivity.ACTIVITY_REQUEST_CODE);
				return false;
		}
    }

    
	/**
	 * AsyncTask to connect and get user's details
	 */
	private class GetUserDetailsTask extends AsyncTask<Object,Void,HashMap<String, Object>>
	{
		String userName;
		SettingsActivity caller;
		protected HashMap<String, Object> doInBackground(Object... params)
		{
	        //Check if the user exists in Twitter
			userName = (String) params[0];
			caller = (SettingsActivity) params[1];
	        TwitterBridge twitter = TwitterBridge.getInstance(prefs);
	        HashMap<String, Object> userDetails = twitter.getUserDetails(userName);
	        return userDetails;
	        
		}
		
		protected void onPostExecute(final HashMap<String,Object> userDetails)
		{
			//Check results of username search, confirm with user
			super.onPostExecute(userDetails);

			if (userDetails == null)
	        {
	        	//Alert that user cannot be found
	        	AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
	        	builder.setMessage("User " + userName + " cannot be found in Twitter");
	        	builder.setPositiveButton(android.R.string.ok, null);
	        	AlertDialog dialog = builder.create();
	        	dialog.show();
	        }
	        else
	        {
	        	//Create a Dialog to confirm that we got the right one
	        	AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
	        	builder.setTitle(R.string.settings_list_add_confirm);
	        	LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.user_confirm_dialog,null);
	        	Drawable avatar = (Drawable) userDetails.get(TwitterBridge.AVATAR);
	        	((ImageView) layout.findViewById(R.id.avatar)).setImageDrawable(avatar);
	        	((TextView) layout.findViewById(R.id.screenname)).setText("@"+userDetails.get(TwitterBridge.SCREEN_NAME));
	        	((TextView) layout.findViewById(R.id.username)).setText((String)userDetails.get(TwitterBridge.USER_NAME));
	        	builder.setView(layout);
	        	//When confirmed, method in caller will be invoked to actually add the user
	        	builder.setPositiveButton(android.R.string.ok, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						caller.addUser((String)userDetails.get(TwitterBridge.SCREEN_NAME));
						caller.toggleAddVisibility(true);
					}
				});
	        	builder.setNegativeButton(android.R.string.cancel, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						caller.toggleAddVisibility(true);
					}
				});
	        		
	        	builder.setCancelable(false);
	        	AlertDialog dialog = builder.create();
	        	dialog.show();
	        }
			
		}
	}

	
	/**
	 * Utility method for reading an array stored in preferences
	 */
	public static String[] readSettingArray(SharedPreferences iPrefs)
	{
		int arrSize = iPrefs.getInt(PREF_FAV_LIST_SIZE, 0);
		String userArray[] = new String[arrSize];
		
		//Read all strings from settings, they are numbered from 0 to arrSize
		for (int i = 0; i < arrSize; i++)
		{
			String user = iPrefs.getString(FAV_LIST_PREFIX+i, "");
			userArray[i] = user;
		}
		return userArray;
	}

}
