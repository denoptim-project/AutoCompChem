package autocompchem.worker;

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
import java.util.Set;

/**
 * Class representing any task that a {@link Worker} can be asked to perform.
 * 
 * @author Marco Foscato
 */

public class Task 
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
		Task newTask = null;
		String candidateID = taskName.toUpperCase().trim();
		synchronized (uniqueTasksLock) {
            boolean found = false;
            for (Task existingTask : uniqueInstances)
            {
                if (existingTask.ID.equals(candidateID))
                {
                	newTask = existingTask;
                    found = true;
                    break;
                }
            }
            if (!found)
            {
            	newTask = new Task(candidateID, taskName, testOnly);
                uniqueInstances.add(newTask);
            }
		}
		return newTask;
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
	
	@Override
	public String toString() 
	{
		return casedID;
	}
	
//------------------------------------------------------------------------------

}
