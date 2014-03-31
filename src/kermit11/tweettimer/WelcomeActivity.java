package kermit11.tweettimer;

import kermit11.tweettimer.util.ObjHolder;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
/**
 * This is a pre-main activity to introduce new users to the application and check if Twitter authorization is needed.
 * Based on example code from <a href="http://schwiz.net/blog/2011/using-scribe-with-android/">Schwiz Logcat</a>
 */
public class WelcomeActivity extends ActionBarActivity
{
	final static String CALLBACK = "oauth://twitter";
	
	final ObjHolder requestTokenHolder = new ObjHolder();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);

		ActionBar actionBar = getSupportActionBar();
    	actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(MainActivity.COLOR_ACTIONBAR)));

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);

		//In case activity was launched with logout request, simply remove user's access token
		Bundle extras = getIntent().getExtras();
    	if (extras != null) {
        	boolean isLogout = extras.getBoolean("LOGOUT",false);
    	    if (isLogout)
    	    {
    			Editor editor = prefs.edit();
    			editor.putString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN, null);
    			editor.commit();
    	    }
    	}
    	
		String savedToken = prefs.getString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN, null);
		if (savedToken == null)
		{
			//First time user - offer him a drink and authorize in Twitter (using Scribe)
			final  WebView webview = (WebView) findViewById(R.id.webview);

			//Get request token 
			final OAuthService service = new ServiceBuilder()
			  .provider(TwitterApi.SSL.class)
			  .apiKey(TwitterTasks.APIKEY)
			  .apiSecret(TwitterTasks.APISECRET)
			  .callback(CALLBACK)
			  .build();

			//Attach WebViewClient to intercept the callback url
			webview.setWebViewClient(new WebViewClient()
			{
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) 
				{
					//Check for our custom callback protocol otherwise use default behavior
					if(url.startsWith(CALLBACK))
					{
						//In case user/Twitter denied authorization, there's nothing much we can do
						if (url.indexOf("denied") > 0)
						{
							//TODO fix/rewrite dialogTheme 
//							AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(WelcomeActivity.this, R.style.DialogTheme));
							AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
							builder.setMessage(R.string.auth_error_desc);
				        	builder.setPositiveButton(android.R.string.ok, null);
				        	AlertDialog dialog = builder.create();
				        	dialog.show();
				        	return true;
						}
						
						//Authorization complete hide webview for now.
						webview.setVisibility(View.GONE);

						Uri uri = Uri.parse(url);
						String verifier = uri.getQueryParameter("oauth_verifier");
						Verifier v = new Verifier(verifier);

						//Save this token using a background thread. Continue to main activity from there.
						TokenHandler handlerThread = new TokenHandler();
						handlerThread.execute(service, requestTokenHolder.val, v);

						return true;
					}

					return super.shouldOverrideUrlLoading(view, url);
				}
			});


			//The welcome message
			//TODO fix/rewrite dialogTheme
			//AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.DialogTheme));
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle(R.string.welcome_message_title);
        	builder.setMessage(R.string.welcome_message_desc);
        	builder.setPositiveButton(android.R.string.ok, new OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					//Perform the actual connection on a background thread
					AuthorizationThread authThread = new AuthorizationThread();
					authThread.execute(service);
				}
			});
        	AlertDialog dialog = builder.create();
        	dialog.show();

		}
		else
		{
			//Returning user - go straight to the Main activity
			Intent i = new Intent(this,MainActivity.class);
			startActivity(i);
		}
	}
	
	
	/**
	 * AsyncTask to perform application's connection to Twitter's authorization service
	 */
	private class AuthorizationThread extends AsyncTask<OAuthService,Void,String>
	{
		protected String doInBackground(OAuthService... params)
		{
			//Get a URL for authorizing this app using the app's token
			OAuthService service = params[0];
			Token requestToken = service.getRequestToken();
			requestTokenHolder.val = requestToken;
			String authURL = service.getAuthorizationUrl(requestToken);
			return authURL;
		}
		
		protected void onPostExecute(String result)
		{
			//Loading is done, switch between views to send user to auth page
			super.onPostExecute(result);
			TextView tempTV = (TextView) findViewById(R.id.tempWelecomeTV);
			tempTV.setVisibility(View.INVISIBLE);
			WebView webview = (WebView) findViewById(R.id.webview);
			webview.setVisibility(View.VISIBLE);
			webview.loadUrl(result);
			
		}
	}

	
	/**
	 * AsyncTask to handle the user's authorization token after callback
	 */
	private class TokenHandler extends AsyncTask<Object,Void,Token>
	{
		protected Token doInBackground(Object... params)
		{
			//Fetch user's token
			OAuthService service = (OAuthService) params[0];
			Token requestToken = (Token) params[1];
			Verifier v = (Verifier) params[2];
			Token accessToken = service.getAccessToken(requestToken, v);
			return accessToken;
		}
		
		protected void onPostExecute(Token result)
		{
			super.onPostExecute(result);

			//Save token in user's preferences for next time
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);
			Editor editor = prefs.edit();
			editor.putString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN, result.getToken());
			editor.putString(SettingsActivity.PREF_TWITTER_ACCESS_TOKEN_SECRET, result.getSecret());
			editor.commit();

			//Launch main activity
			Intent i = new Intent(WelcomeActivity.this,MainActivity.class);
			startActivity(i);
		}
	}


}
