package kermit11.tweettimer.intf;

/**
 * Callback interface to be passed to loader which will allow it to update UI with progress 
 */
public interface ProgressUpdater
{
	public void publishProgress(int current, int max);
}
