package kermit11.tweettimer.util;

import java.util.ArrayList;

import kermit11.tweettimer.R;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class StringListAdapter extends ArrayAdapter<String>
{
	private ArrayList<String> entries;
	private Activity activity;

	public StringListAdapter(Activity a, int textViewResourceId, ArrayList<String> entries) 
	{
		super(a, textViewResourceId, entries);
		this.entries = entries;
		this.activity = a;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View v = (View)convertView;
		if (v == null) 
		{
			LayoutInflater vi = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = (View)vi.inflate(R.layout.list_item_main, null);
		}
		TextView tv = (TextView) v.findViewById(R.id.username);
		String val = entries.get(position);
		tv.setText(val);
		return v;
	}

}

