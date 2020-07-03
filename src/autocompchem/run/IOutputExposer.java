package autocompchem.run;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;

/**
 * The contract to be implemented by any class that intends to make data
 * reachable from outside itself, i.e., typically from outside the specific 
 * job which initiated that class. 
 * 
 * @author Marco Foscato
 */
public interface IOutputExposer 
{
	/**
	 * Sets the reference to the data structure where the exposed data
	 * is to be collected.
	 * @param collector the collector data structure
	 */
	public void setDataCollector(NamedDataCollector collector);
	
	/**
	 * Adds some data the the collection of exposed data.
	 * @param data the data to expose
	 */
	public void exposeData(NamedData data);
	
}
