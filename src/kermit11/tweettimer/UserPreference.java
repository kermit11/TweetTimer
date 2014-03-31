package kermit11.tweettimer;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserPreference extends Preference
{

	public UserPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public UserPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public UserPreference(Context context)
	{
		super(context);
		setLayoutResource(R.layout.list_item_settings_users);
	}
	
	private int prefNumber = 1;
	public void setPrefNumber(int newVal)
	{
		prefNumber = newVal;
	}

	public View getView(View convertView, ViewGroup parent) 
	{
		View v = super.getView(convertView, parent);
		if (v.getId() == R.id.settingsUserPrefLayout)
		{
			LinearLayout layout = (LinearLayout) v;
			for (int i = 0; i < layout.getChildCount(); i++)
			{
				View child = layout.getChildAt(i);
				if (child.getId() == R.id.settingsUserPrefNumber)
				{
					TextView number = (TextView) child;
					number.setText(String.format("%02d", prefNumber));
				}
				if (child.getId() == R.id.settingsUserPrefInnerLayout)
				{
					LinearLayout innerLayout = (LinearLayout) child;
					for (int j = 0; j < innerLayout.getChildCount(); j++)
					{
						//Go ahead. Make a joke at the name of my variable. I dare you. I double dare you.
						View innerChild = innerLayout.getChildAt(j);
						if (innerChild.getId() == R.id.settingsUserPrefUsername)
						{
							TextView title = (TextView) innerChild;
							title.setText("@"+getTitle());
						}
						if (innerChild.getId() == R.id.settingsUserPrefHint)
						{
							TextView hint = (TextView) innerChild;
							hint.setText(getSummary());
						}
					}
				}
			}
		}
		return v;
	}

}
