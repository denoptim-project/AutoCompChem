package autocompchem.chemsoftware;

/*   
 *   Copyright (C) 2023  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import autocompchem.chemsoftware.gaussian.GaussianOutputReader;
import autocompchem.chemsoftware.nwchem.NWChemOutputReader;
import autocompchem.chemsoftware.orca.OrcaOutputReader;
import autocompchem.chemsoftware.xtb.XTBOutputReader;
import autocompchem.files.FileFingerprint;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;

/**
 * A factory to create software-specific instances of 
 * {@link ChemSoftOutputReader} and {@link ChemSoftInputWriter}.
 * The specific software is defined by the context, e.g., by a log file
 * that should be read, or a declaration of which output reader one wants, or
 * by a declaration of which software an input should be made for.
 * This is a singleton and ensures synchronized management of the registry of 
 * known software output readers and input writers.
 * 
 * @author Marco Foscato
 */

public final class ChemSoftReaderWriterFactory
{
	
	/**
	 * The collection of known output readers. Note that since each reader 
	 * declares an input writer implementation (via the 
	 * {@link ChemSoftOutputReader#getChemSoftInputWriter()}), 
	 * this is effectively also a registry of input writers.
	 */
	private static Map<String, ChemSoftOutputReader> knownOutputReaders = 
			new HashMap<String, ChemSoftOutputReader>();

	/**
	 * Singleton instance of this class
	 */
	private static ChemSoftReaderWriterFactory INSTANCE;


//------------------------------------------------------------------------------

	/**
	 * Constructs a builder that can be configured by registering any analyzer 
	 * with the {@link #registerAnalyzer(String, Object)} method.
	 */
	private ChemSoftReaderWriterFactory()
	{
		this.registerOutputReader(new NWChemOutputReader());
		this.registerOutputReader(new GaussianOutputReader());
		this.registerOutputReader(new OrcaOutputReader());
		this.registerOutputReader(new OrcaOutputReader());
		this.registerOutputReader(new XTBOutputReader());
	}

//-----------------------------------------------------------------------------

	/**
	 * Returns the singleton instance of this class, i.e., the sole factory of
	 * {@link ChemSoftOutputReader}s and {@link ChemSoftInputWriter}
	 * that can be configured and used.
	 * @return the singleton instance.
	 */
	public synchronized static ChemSoftReaderWriterFactory getInstance()
	{
		if (INSTANCE==null)
			INSTANCE = new ChemSoftReaderWriterFactory();
		return INSTANCE;
	}

//------------------------------------------------------------------------------

	/**
	 * Creates an instance that is able to read the given file or files 
	 * contained in the given file.
	 * @param file the file or folder to call "the output" to read.
	 * @return the instance of the read, or <code>null</code> if the 
	 * instance could not be made.
	 * @throws FileNotFoundException if the file is not found.
	 */
	 
	public ChemSoftOutputReader makeOutputReaderInstance(File file) 
			throws FileNotFoundException
	{
		String detectedSoftwareName = detectOutputFormat(file);
		
		if (detectedSoftwareName!=null)
			return makeOutputReaderInstance(detectedSoftwareName);
		else
			return null;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Identifies the software that generated the file or file structure based
	 * on the list of registered {@link ChemSoftOutputReader}s.
	 * @param file the output to read. Note it can be a file or a folder.
	 * @return the name of the software or null if the identification was not
	 * successful.
	 * @throws FileNotFoundException  if the output does not exist.
	 */
	
	public static String detectOutputFormat(File file) 
			throws FileNotFoundException
	{
    	if (INSTANCE==null)
    		getInstance();
    	
		if (!file.exists())
			throw new FileNotFoundException("File '" + file + "' not found.");
		
		for (String softwareName : knownOutputReaders.keySet())
		{
			for (FileFingerprint fp : 
				knownOutputReaders.get(softwareName).getOutputFingerprint())
			{
				if (fp.matchedBy(file))
				{
					return softwareName;
				}
			}
		}
		return null;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a new instance of the {@link ChemSoftOutputReader} dedicated
	 * to the given software identifier.
	 * @param softwareName
	 * @return the software-specific instance, or <code>null</code> if 
	 * no software-specific implementation is registered for the given 
	 * identifier.
	 */
	public ChemSoftOutputReader makeOutputReaderInstance(String softwareName)
	{
		if (!knownOutputReaders.containsKey(softwareName))
		{
			return null;
		}
		ChemSoftOutputReader target = knownOutputReaders.get(softwareName);
		String readerName = target.getClass().getName();
		ChemSoftOutputReader csoa = null;
		ClassLoader classLoader = getClass().getClassLoader();
		try {
            @SuppressWarnings("unchecked")
			Class<? extends ChemSoftOutputReader> c = 
            		(Class<? extends ChemSoftOutputReader>) classLoader
            			.loadClass(readerName);
            
            csoa = instantiateOutputReader(c);
        } catch (NoClassDefFoundError | ClassNotFoundException error) {
			//TODO-gg: log warning
			System.err.println("Could not find analyzer class " + readerName);
        }
		return csoa;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Calls the constructor to instantiate an instance of the given class.
	 * For now no arguments are expected so the class to 
	 * instantiate must have a constructor with no arguments.
	 * @param c the class of the object to instantiate.
	 * @return the instance of the given class.
	 */
	private ChemSoftOutputReader instantiateOutputReader(
			Class<? extends ChemSoftOutputReader> c)
	{
		ChemSoftOutputReader csoa = null;
		try {
	        for (@SuppressWarnings("rawtypes") Constructor constructor : 
	        	c.getConstructors()) 
	        {
	        	csoa = (ChemSoftOutputReader) constructor.newInstance();
	        }
        } catch (InstantiationException 
        		| IllegalAccessException 
        		| IllegalArgumentException 
        		| InvocationTargetException  exception) {
	        throw new IllegalStateException("reader " + c.getSimpleName() 
	        	+ "could not be instantiated");
        }
        return csoa;
    }
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a new instance of the {@link ChemSoftInputWriter} dedicated
	 * to the given software identifier.
	 * @param softwareName the string identifying the software for which the
	 * {@link ChemSoftInputWriter} is able to generate an input.
	 * @return the software-specific instance, or <code>null</code> if 
	 * no software-specific implementation is registered for the given 
	 * identifier.
	 */
	public ChemSoftInputWriter makeInstanceInputWriter(String softwareName)
	{
		if (!knownOutputReaders.containsKey(softwareName))
		{
			return null;
		}
		ChemSoftInputWriter target = 
				knownOutputReaders.get(softwareName).getChemSoftInputWriter();

		ChemSoftInputWriter result = null;
		if (target==null)
		{
			//TODO-gg: log warning
			System.err.println("WARNING: Class " 
					+ knownOutputReaders.get(softwareName).getClass().getSimpleName()
					+ " declares no linked implementation of " 
					+ ChemSoftInputWriter.class.getSimpleName() 
					+ ". Cannot find a suitable " 
					+ Worker.class.getSimpleName() + " to prepare input for " 
					+ "software '" + softwareName + "'.");
			return result;
		}
		String writerName = target.getClass().getName();
		ClassLoader classLoader = getClass().getClassLoader();
		try {
            @SuppressWarnings("unchecked")
			Class<? extends ChemSoftInputWriter> c = 
            		(Class<? extends ChemSoftInputWriter>) classLoader
            			.loadClass(writerName);
            
            result = instantiateInputWriter(c);
        } catch (NoClassDefFoundError | ClassNotFoundException error) {
			//TODO-gg: log warning
			System.err.println("Could not find class " + writerName 
					+ " that extends " 
					+ ChemSoftInputWriter.class.getSimpleName());
        }
		return result;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Calls the constructor to instantiate an instance of the given class.
	 * For now no arguments are expected so the class to 
	 * instantiate must have a constructor with no arguments.
	 * @param c the class of the object to instantiate.
	 * @return the instance of the given class.
	 */
	private ChemSoftInputWriter instantiateInputWriter(
			Class<? extends ChemSoftInputWriter> c)
	{
		ChemSoftInputWriter csip = null;
		try {
	        for (@SuppressWarnings("rawtypes") Constructor constructor : 
	        	c.getConstructors()) 
	        {
	        	csip = (ChemSoftInputWriter) constructor.newInstance();
	        }
        } catch (InstantiationException 
        		| IllegalAccessException 
        		| IllegalArgumentException 
        		| InvocationTargetException  exception) {
	        throw new IllegalStateException("writer " + c.getSimpleName() 
	        	+ "could not be instantiated");
        }
        return csip;
    }
	
//-----------------------------------------------------------------------------

	/**
	 * Adds to the registry of known output readers. Note that 
	 * {@link ChemSoftOutputReader#getChemSoftInputWriter()} define a precise 
	 * relation between a software-specific implementation of 
	 * {@link ChemSoftOutputReader} and the corresponding software-specific
	 * implementation of {@link ChemSoftInputWriter}. Thus, by registering the 
	 * {@link ChemSoftOutputReader} for a software, you are effectively 
	 * registering also the {@link ChemSoftInputWriter} for that same software.
	 * @param exampleReader the worker to register.
	 */
	public synchronized void registerOutputReader(Object exampleReader)
	{
		if (exampleReader instanceof ChemSoftOutputReader)
		{
			ChemSoftOutputReader csoa = (ChemSoftOutputReader) exampleReader;
			knownOutputReaders.put(csoa.getSoftwareID(), csoa);
		} else {
			//TODO-gg: log warning
			System.err.println("Registration of output reader has failed "
					+ "because the given object is not an instance of "
					+ ChemSoftOutputReader.class.getSimpleName() 
					+ ". Ignoting reader '" + exampleReader.toString() + "'");
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Removes from the registry.
	 * @param reader the implementation to remove from the registry, if found.
	 */
	//TODO-gg change type of arg to template
	public synchronized void deregisterOutputReader(Object reader)
	{
		if (reader instanceof ChemSoftOutputReader)
		{
			ChemSoftOutputReader csoa = (ChemSoftOutputReader) reader;
			knownOutputReaders.remove(csoa.getSoftwareID());
		}
	}
	 
//------------------------------------------------------------------------------
		
	/**
	 * @return the list of  identifiers of known software for which there is a
	 * registered reader.
	 */
	public synchronized static Set<String> getRegisteredSoftwareIDs() 
	{
    	if (INSTANCE==null)
    		getInstance();
		return knownOutputReaders.keySet();
	}
    
//------------------------------------------------------------------------------
	
}
