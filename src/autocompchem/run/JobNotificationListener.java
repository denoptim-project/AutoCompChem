package autocompchem.run;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	
	/**
	 * Sends a notification that signals the completion of the job
	 * which might weak up any waiting process.
	 */
	public void notifyTermination(Job sender);
	
}
