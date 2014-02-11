package kermit11.tweettimer.util;

import java.io.InputStream;
import java.net.URL;

import kermit11.tweettimer.MainActivity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class Utils
{

	public static Drawable getDrawableFromURL(String url)
	{
		try
		{
			BitmapDrawable ret = (BitmapDrawable) Drawable.createFromStream(((InputStream)new URL(url).getContent()), "src");
			Bitmap bm = ret.getBitmap();
			bm = Bitmap.createScaledBitmap(bm, 64, 64, false);
			return new BitmapDrawable(MainActivity.getContext().getResources(), bm);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

}
