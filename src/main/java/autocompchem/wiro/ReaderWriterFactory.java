package autocompchem.wiro;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autocompchem.files.FileFingerprint;
import autocompchem.run.SoftwareId;
import autocompchem.wiro.acc.ACCOutputReader;
import autocompchem.wiro.chem.ChemSoftInputWriter;
import autocompchem.wiro.chem.ChemSoftOutputReader;
import autocompchem.wiro.chem.gaussian.GaussianOutputReader;
import autocompchem.wiro.chem.nwchem.NWChemOutputReader;
import autocompchem.wiro.chem.orca.OrcaOutputReader;
import autocompchem.wiro.chem.spartan.SpartanOutputReader;
import autocompchem.wiro.chem.xtb.XTBOutputReader;
import autocompchem.worker.Worker;

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

public final class ReaderWriterFactory
{
	
	/**
	 * The collection of known output readers. Note that since each reader 
	 * declares an input writer implementation (via the 
	 * {@link OutputReader#getSoftInputWriter()}), 
	 * this is effectively also a registry of input writers.
	 */
	private static Map<SoftwareId, OutputReader> knownOutputReaders = 
			new HashMap<SoftwareId, OutputReader>();

	/**
	 * Singleton instance of this class
	 */
	private static ReaderWriterFactory INSTANCE;
	
	private static Logger logger = LogManager.getLogger(
			ReaderWriterFactory.class);


//------------------------------------------------------------------------------

	/**
	 * Constructs a builder that can be configured by registering any analyzer 
	 * with the {@link #registerAnalyzer(String, Object)} method.
	 */
	private ReaderWriterFactory()
	{
		this.registerOutputReader(new ACCOutputReader());
		this.registerOutputReader(new NWChemOutputReader());
		this.registerOutputReader(new GaussianOutputReader());
		this.registerOutputReader(new OrcaOutputReader());
		this.registerOutputReader(new OrcaOutputReader());
		this.registerOutputReader(new XTBOutputReader());
		this.registerOutputReader(new SpartanOutputReader());
	}

//-----------------------------------------------------------------------------

	/**
	 * Returns the singleton instance of this class, i.e., the sole factory of
	 * {@link ChemSoftOutputReader}s and {@link ChemSoftInputWriter}
	 * that can be configured and used.
	 * @return the singleton instance.
	 */
	public synchronized static ReaderWriterFactory getInstance()
	{
		if (INSTANCE==null)
			INSTANCE = new ReaderWriterFactory();
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
	 
	public OutputReader makeOutputReaderInstance(File file) 
			throws FileNotFoundException
	{
		SoftwareId detectedSoftwareName = detectOutputFormat(file);
		
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
	
	public static SoftwareId detectOutputFormat(File file) 
			throws FileNotFoundException
	{
    	if (INSTANCE==null)
    		getInstance();
    	
		if (!file.exists())
			throw new FileNotFoundException("File '" + file + "' not found.");
		
		for (SoftwareId softwareName : knownOutputReaders.keySet())
		{
			// Map of results of the matches by pathname. Conditions acting on 
			// the same pathname MUST be matched simultaneously, while those
			// acting on different pathnames are independent.
			Map<String,Boolean> logicalAND= new HashMap<String,Boolean>();
			
			for (FileFingerprint fp : 
				knownOutputReaders.get(softwareName).getOutputFingerprint())
			{	
				boolean fpMatched = fp.matchedBy(file);

				if (!logicalAND.containsKey(fp.PATHNAME))
				{
					logicalAND.put(fp.PATHNAME, fpMatched);
				} else {
					boolean previous = logicalAND.get(fp.PATHNAME);
					logicalAND.put(fp.PATHNAME, previous && fpMatched);
					// Fast-failing
					if (!(previous && fpMatched))
						break;
				}
			}
			boolean matchesAny = false;
			for (Boolean combined : logicalAND.values())
				if (combined)
				{
					matchesAny = true;
					break;
				}
				
			if (matchesAny)
				return softwareName;
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
	public OutputReader makeOutputReaderInstance(SoftwareId softwareName)
	{
		if (!knownOutputReaders.containsKey(softwareName))
		{
			return null;
		}
		OutputReader target = knownOutputReaders.get(softwareName);
		String readerName = target.getClass().getName();
		OutputReader csoa = null;
		ClassLoader classLoader = getClass().getClassLoader();
		try {
            @SuppressWarnings("unchecked")
			Class<? extends OutputReader> c = 
            		(Class<? extends OutputReader>) classLoader
            			.loadClass(readerName);
            
            csoa = instantiateOutputReader(c);
        } catch (NoClassDefFoundError | ClassNotFoundException error) {
			logger.warn("Could not find analyzer class " + readerName);
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
	private OutputReader instantiateOutputReader(
			Class<? extends OutputReader> c)
	{
		OutputReader csoa = null;
		try {
	        for (@SuppressWarnings("rawtypes") Constructor constructor : 
	        	c.getConstructors()) 
	        {
	        	csoa = (OutputReader) constructor.newInstance();
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
	 * @param softwareID the string identifying the software for which the
	 * {@link ChemSoftInputWriter} is able to generate an input.
	 * @return the software-specific instance, or <code>null</code> if 
	 * no software-specific implementation is registered for the given 
	 * identifier.
	 */
	public Worker makeInstanceInputWriter(SoftwareId softwareID)
	{
		if (!knownOutputReaders.containsKey(softwareID))
		{
			return null;
		}
		ITextualInputWriter target = 
				knownOutputReaders.get(softwareID).getSoftInputWriter();

		Worker result = null;
		if (target==null)
		{
			logger.warn("WARNING: Class " 
					+ knownOutputReaders.get(softwareID).getClass().getSimpleName()
					+ " declares no linked implementation of " 
					+ ITextualInputWriter.class.getSimpleName() 
					+ ". Cannot find a suitable " 
					+ Worker.class.getSimpleName() + " to prepare input for " 
					+ "software '" + softwareID + "'.");
			return result;
		}
		String writerName = target.getClass().getName();
		ClassLoader classLoader = getClass().getClassLoader();
		try {
			Class<?> c = classLoader.loadClass(writerName);
            result = instantiateInputWriter(c);
        } catch (NoClassDefFoundError | ClassNotFoundException error) {
			logger.warn("Could not find class " + writerName 
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
	private Worker instantiateInputWriter(Class<?> c)
	{
		Worker writer = null;
		try {
	        for (@SuppressWarnings("rawtypes") Constructor constructor : 
	        	c.getConstructors()) 
	        {
	        	writer = (Worker) constructor.newInstance();
	        }
        } catch (InstantiationException 
        		| IllegalAccessException 
        		| IllegalArgumentException 
        		| InvocationTargetException  exception) {
	        throw new IllegalStateException("writer " + c.getSimpleName() 
	        	+ "could not be instantiated");
        }
        return writer;
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
		if (exampleReader instanceof OutputReader)
		{
			OutputReader sor = (OutputReader) exampleReader;
			knownOutputReaders.put(sor.getSoftwareID(), sor);
		} else {
			logger.warn("Registration of output reader has failed "
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
	public synchronized void deregisterOutputReader(Object reader)
	{
		if (reader instanceof OutputReader)
		{
			OutputReader sor = (OutputReader) reader;
			knownOutputReaders.remove(sor.getSoftwareID());
		}
	}
	 
//------------------------------------------------------------------------------
		
	/**
	 * @return the list of  identifiers of known software for which there is a
	 * registered reader.
	 */
	public synchronized static Set<SoftwareId> getRegisteredSoftwareIDs() 
	{
    	if (INSTANCE==null)
    		getInstance();
		return knownOutputReaders.keySet();
	}
    
//------------------------------------------------------------------------------
	
}
