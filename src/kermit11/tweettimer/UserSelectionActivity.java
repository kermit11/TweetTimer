package kermit11.tweettimer;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class UserSelectionActivity extends ListActivity 
{
	public static final int ACTIVITY_REQUEST_CODE = 50;
	public static final String ACTIVITY_RESULT_SELECTED_VAL = "ACTIVITY_RESULT_SELECTED_VAL";
	private EditText filterText = null;
	private ArrayAdapter<String> adapter = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_userselection);

		filterText = (EditText) findViewById(R.building_list.search_box);
		filterText.addTextChangedListener(filterTextWatcher);
		
		Button button = (Button) findViewById(R.id.userSelectButtonOK);
		button.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String typedVal = filterText.getText().toString();
				Intent iResult = new Intent();
				iResult.putExtra(ACTIVITY_RESULT_SELECTED_VAL, typedVal);
				setResult(Activity.RESULT_OK, iResult);
		        finish();
			}
		});
	}

	
	@Override
	protected void onResume()
	{
		super.onResume();

		//Load the adapter with followers
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String[] followrsList = TwitterBridge.getInstance(prefs).getFollowersCached();
		if (followrsList != null)
		{		
			adapter = new ArrayAdapter<String>(this,
					R.layout.list_item_userselection, 
					followrsList);
			setListAdapter(adapter);
		}
		else
		{
			//Followers are still being loaded, so show the progress animation and update once we receive the broadcast message
			final LinearLayout linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
		    linlaHeaderProgress.setVisibility(View.VISIBLE);

		    LocalBroadcastManager.getInstance(this).registerReceiver(loadFollowersFinishedReceiver, new IntentFilter(TwitterBridge.INTENT_FOLLOWERS_CACHED));
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		//Clean up
		LocalBroadcastManager.getInstance(this).unregisterReceiver(loadFollowersFinishedReceiver);
	}

	//This receiver will catch the broadcast for when followers have been loaded 
	private BroadcastReceiver loadFollowersFinishedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String[] receivedFollowersList = (String[]) intent.getExtras().get(TwitterBridge.INTENT_EXTRA_FOLLOWERS);
			adapter = new ArrayAdapter<String>(UserSelectionActivity.this,
					R.layout.list_item_userselection, 
					receivedFollowersList);
			setListAdapter(adapter);
			LinearLayout linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
		    linlaHeaderProgress.setVisibility(View.GONE);
		}
	};

	/**
	 * Handle keystrokes to filter list
	 */
	private TextWatcher filterTextWatcher = new TextWatcher() {

	    public void afterTextChanged(Editable s) {
	    }

	    public void beforeTextChanged(CharSequence s, int start, int count,
	            int after) {
	    }

	    public void onTextChanged(CharSequence s, int start, int before,
	            int count) {
	        adapter.getFilter().filter(s);
	    }

	};

	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		CharSequence selectedVal = ((TextView)v).getText();
		Intent iResult = new Intent();
		iResult.putExtra(ACTIVITY_RESULT_SELECTED_VAL, (String)selectedVal);
		setResult(Activity.RESULT_OK, iResult);
        finish();
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    filterText.removeTextChangedListener(filterTextWatcher);
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

}
