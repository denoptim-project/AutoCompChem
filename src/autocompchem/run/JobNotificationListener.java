package autocompchem.run;

/**
 * Interface for anything that wants to listen to notifications from the job 
 * class.
 * 
 * @author Marco Foscato
 */

interface JobNotificationListener 
{
	/**
	 * Sends a notification with a request to perform a given action.
	 * @param action the requested action.
	 * @param sender the job that is placing the request.
	 */
	public void reactToRequestOfAction(Action action, Job sender);
}