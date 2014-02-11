package kermit11.tweettimer;

import java.util.ArrayList;
import java.util.Iterator;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class HelpActivity extends ActionBarActivity
{
	ArrayList<Integer> faqTitles;
	ArrayList<Integer> faqAnswers;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		ActionBar actionBar = getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);
    	actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(MainActivity.COLOR_ACTIONBAR)));
    	
    	faqTitles = loadResourceIDs("helpFAQtitle", "id");
    	faqAnswers = loadResourceIDs("helpFAQanswer", "id");
		final ArrayList<TextView> allAnswerTVs = new ArrayList<TextView>();

    	//Set listeners for all FAQs
    	for (Iterator<Integer> iterTitles = faqTitles.iterator(), iterAnswers = faqAnswers.iterator(); iterTitles.hasNext();)
		{
    		int titleResID = ((Integer)iterTitles.next()).intValue();
    		int answerResID = ((Integer)iterAnswers.next()).intValue();
			
    		TextView titleTV = (TextView) findViewById(titleResID);
    		final TextView answerTV = (TextView) findViewById(answerResID); 
    		allAnswerTVs.add(answerTV);
    		titleTV.setOnClickListener(new OnClickListener()
    		{
    			@Override
    			public void onClick(View v)
    			{
    				int currVisibility = answerTV.getVisibility();
    				//Hide the open one
    				for (Iterator<TextView> iter = allAnswerTVs.iterator(); iter.hasNext();)
					{
						TextView textView = (TextView) iter.next();
						textView.setVisibility(View.GONE);
					}
    				//And show current
    		    	if (currVisibility == View.GONE)
    		    	{
    		    		answerTV.setVisibility(View.VISIBLE);
    		    	}
    			}
    		});
    	}
	}
	
	public ArrayList<Integer> loadResourceIDs(String baseName, String type)
	{
		ArrayList<Integer> result = new ArrayList<Integer>();
		int resID = getResources().getIdentifier(baseName+0, type, getPackageName());
		for (int i = 1; resID!= 0; i++)
		{
			result.add(Integer.valueOf(resID));
			resID = getResources().getIdentifier(baseName+i, type, getPackageName());
		}
		return result;
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
