package autocompchem.worker;

import java.util.ArrayList;
import java.util.Collections;

/*
 *   Copyright (C) 2020  Marco Foscato
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import autocompchem.run.Terminator;

/**
 * Class representing any task that a {@link Worker} can be asked to perform.
 * 
 * @author Marco Foscato
 */

public class Task implements Comparable<Task>
{
	/**
	 * This is the unique identifier of this task. It is used to ensure
	 * uniqueness of {@link Task}s.
	 */
	public final String ID;
	
	/**
	 * Case-enhanced version of the ID used to generate doc strings.
	 */
	public final String casedID;
	
	/**
	 * Flag identifying an instance as meant for tests and, therefore, ignorable
	 * in production tasks.
	 */
	public final boolean testOnly;
	
    /**
     * Set of known tasks as unique instances
     */
    private static Set<Task> uniqueInstances = new HashSet<Task>();
    
    /**
     * Synchronization lock. Used to guard alteration of the set of unique
     * instances of this class.
     */
    private final static Object uniqueTasksLock = new Object();
  	
//------------------------------------------------------------------------------
	
    /**
     * Constructor
     * @param ID the string used as unique identifier
     * @param casedID the string used to refer to this object in doc strings.
     */
	private Task(String ID, String casedID, boolean testOnly)
	{
		this.ID = ID;
		this.casedID = casedID;
		this.testOnly = testOnly;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Checks if an instance with the given identifier exists or has to be made.
	 * @param taskName the string used to make the case-insensitive identifier, 
	 * and used, for the very first creation, as a name of the task in 
	 * documentation.
	 * @param testOnly if <code>true</code> then this task is only for testing
	 * purposed and will not be displayed elsewhere.
	 * @return the unique instance of the task, whether new or taken from 
	 * previous makings.
	 */
	private static Task getUnique(String taskName, boolean testOnly)
	{
		return getExistingOrMake(taskName, testOnly, true, false);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Tries to search for an existing instance and returns it. If none is found
	 * then it returns <code>null</code>
	 * @param taskName the string to be used as base for making a task
	 * identifier string
	 * @return the requested instance
	 * or null.
	 */
	public static Task getExisting(String taskName) 
	{
		return getExistingOrMake(taskName, false, false, false);
	}
		
//------------------------------------------------------------------------------
	
	/**
	 * Tries to search for an existing instance and returns it. If none is found
	 * then it offers the possibility to terminate the master process,
	 * or to just ignore the fact and return <code>null</code>.
	 * @param taskName the string to be used as base for making a task
	 * identifier string
	 * @param terminateOnMissing if <code>true</code> terminates the master
	 * process in case
	 * no existing instance is found that matches the identifier AND we are
	 * not allowed to make a new one.
	 * @return the requested instance
	 * or null.
	 */
	public static Task getExisting(String taskName, boolean terminateOnMissing) 
	{
		return getExistingOrMake(taskName, false, false, terminateOnMissing);
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Tries to search for an existing instance and returns it. If none is found
	 * then it offers the possibility to make one, or to terminate the master
	 * process,
	 * or to just ignore the fact and return <code>null</code>.
	 * @param taskName the string to be used as base for making a task
	 * identifier string
	 * @param makeIt if <code>true</code> then a new instance will be made if no 
	 * existing instance matches the identifier.
	 * @param terminateOnMissing if <code>true</code> terminates the master
	 * process in case
	 * no existing instance is found that matches the identifier AND we are
	 * not allowed to make a new one.
	 * @return the requested instance, which may be an existing one, a new one,
	 * or null.
	 */
	public static Task getExistingOrMake(String taskName, boolean testOnly, 
			boolean makeIt, boolean terminateOnMissing)
	{
		Task target = null;
		String candidateID = standardazeID(taskName);
		synchronized (uniqueTasksLock) 
		{
            boolean found = false;
            for (Task existingTask : uniqueInstances)
            {
                if (existingTask.ID.equals(candidateID))
                {
                	target = existingTask;
                    found = true;
                    break;
                }
            }
            if (!found)
            {
            	if (makeIt)
            	{
            		target = new Task(candidateID, taskName, testOnly);
                    uniqueInstances.add(target);
            	} else {
            		if (terminateOnMissing)
            		{
            			Terminator.withMsgAndStatus("ERROR! String '" + taskName
            					+ "' does not match any registered Task! "
            					+ "Check your input.", -1);
            		}
            	}
            }
		}
		return target;
	}
	
//------------------------------------------------------------------------------
		
	/**
	 * Processes the string to return a string that should be used as identifier
	 * of the task.
	 * @param taskName the string to be formatted as an identifier
	 * @return the string that can be used a identifier.
	 */
	private static String standardazeID(String taskName)
	{
		return taskName.toUpperCase().trim().replaceAll("\\s+", "");
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Creates a task instance if no previously existing instance has the same 
	 * identifier.
	 * @param taskName the identifier of the task. While the case given here is 
	 * used to generate doc strings, the identifier is case.insensitive.
	 * @return a new or a previously existing instance of this class that if the
	 * only one with the given identifier.
	 */
	public static Task make(String taskName)
	{
		return getUnique(taskName, false);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Creates a task instance if no previously existing instance has the same 
	 * identifier.
	 * @param taskName the identifier of the task. While the case given here is 
	 * used to generate doc strings, the identifier is case.insensitive.
	 * @param testOnly if <code>true</code> then this task is only for testing
	 * purposed and will not be displayed elsewhere.
	 * @return a new or a previously existing instance of this class that if the
	 * only one with the given identifier.
	 */
	public static Task make(String taskName, boolean testOnly)
	{
		return getUnique(taskName, testOnly);
	}

//------------------------------------------------------------------------------

	/**
	 * Returns a sorted snapshot of the list of registered tasks.
	 * @return the sorted list.
	 */
	public static List<Task> getRegisteredTasks() 
	{
		List<Task> list = new ArrayList<Task>();
		synchronized (uniqueTasksLock) {
			list.addAll(uniqueInstances);
		}
		Collections.sort(list);
		return list;
	}	
	
//------------------------------------------------------------------------------
	
	@Override
	public String toString() 
	{
		return casedID;
	}

//------------------------------------------------------------------------------
	
	@Override
	public int compareTo(Task o) 
	{
		return this.ID.compareToIgnoreCase(o.ID);
	}
	
//------------------------------------------------------------------------------

}
