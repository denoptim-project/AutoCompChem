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

import java.io.BufferedReader;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.IDescriptor;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.$Gson$Preconditions;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;

import autocompchem.chemsoftware.AnalysisTask.AnalysisKind;
import autocompchem.chemsoftware.gaussian.GaussianOutputAnalyzer;
import autocompchem.chemsoftware.nwchem.NWChemOutputAnalyzer;
import autocompchem.chemsoftware.orca.OrcaOutputAnalyzer;
import autocompchem.chemsoftware.xtb.XTBOutputAnalyzer;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ListOfDoubles;
import autocompchem.datacollections.ListOfIntegers;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileFingerprint;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.perception.TxtQuery;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.SituationBase;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Worker;

/**
 * A factory to create instances of {@link ChemSoftOutputAnalyzer} that are
 * possibly specific for a software defined by the context (e.g., by a log file
 * that should be read).
 * 
 * @author Marco Foscato
 */

public final class ChemSoftOutputAnalyzerBuilder
{   
	public static enum CHEMSOFTWITHINTERNALANALYZER {Gaussian, NWChem, ORCA, 
		XTB};
	
	/**
	 * The collection of known analyzers.
	 */
	private final Map<String, ChemSoftOutputAnalyzer> knownAnalyzers = 
			new HashMap<String, ChemSoftOutputAnalyzer>();

//------------------------------------------------------------------------------

	/**
	 * Constructs a builder that can be configured by registering any analyzer 
	 * with the {@link #registerAnalyzer(String, Object)} method.
	 */
	public ChemSoftOutputAnalyzerBuilder()
	{
		this.registerAnalyzer(CHEMSOFTWITHINTERNALANALYZER.NWChem.toString(), 
				new NWChemOutputAnalyzer());
		this.registerAnalyzer(CHEMSOFTWITHINTERNALANALYZER.Gaussian.toString(),
				new GaussianOutputAnalyzer());
		this.registerAnalyzer(CHEMSOFTWITHINTERNALANALYZER.ORCA.toString(), 
				new OrcaOutputAnalyzer());
		this.registerAnalyzer(CHEMSOFTWITHINTERNALANALYZER.ORCA.toString(), 
				new OrcaOutputAnalyzer());
		this.registerAnalyzer(CHEMSOFTWITHINTERNALANALYZER.XTB.toString(), 
				new XTBOutputAnalyzer());
	}
	
//------------------------------------------------------------------------------

	/**
	 * Creates an instance that is able to analyze the given file or files 
	 * contained in it.
	 * @param file the file or folder to call "the output" to analyze.
	 * @return the instance of the analyzer.
	 * @throws FileNotFoundException if the file is not found.
	 */
	 
	public ChemSoftOutputAnalyzer makeInstance(File file) 
			throws FileNotFoundException
	{
		String detectedSoftwareName = detectOutputFormat(file);
		
		if (detectedSoftwareName!=null)
			return makeInstance(detectedSoftwareName);
		else
			return null;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Identifies the software that generated the file or file structure based
	 * on the list of registered {@link ChemSoftOutputAnalyzer}s.
	 * @param file the output to analyze. Note it can be a file or a folder.
	 * @return the name of the software or null if the identification was not
	 * successful.
	 */
	
	public String detectOutputFormat(File file)
	{
		for (String softwareName : knownAnalyzers.keySet())
		{
			for (FileFingerprint fp : 
				knownAnalyzers.get(softwareName).getOutputFingerprint())
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

	public ChemSoftOutputAnalyzer makeInstance(String softwareName)
	{
		if (!knownAnalyzers.containsKey(softwareName))
		{
			return null;
		}
		ChemSoftOutputAnalyzer target = knownAnalyzers.get(softwareName);
		String analyzerName = target.getClass().getName();
		ChemSoftOutputAnalyzer csoa = null;
		ClassLoader classLoader = getClass().getClassLoader();
		try {
            @SuppressWarnings("unchecked")
			Class<? extends ChemSoftOutputAnalyzer> c = 
            		(Class<? extends ChemSoftOutputAnalyzer>) classLoader
            			.loadClass(analyzerName);
            
            csoa = instantiate(c);
        } catch (NoClassDefFoundError | ClassNotFoundException error) {
			//TODO-gg: log warning
			System.err.println("Could not find analyzer class " + analyzerName);
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
	private ChemSoftOutputAnalyzer instantiate(
			Class<? extends ChemSoftOutputAnalyzer> c)
	{
		ChemSoftOutputAnalyzer csoa = null;
		try {
	        for (@SuppressWarnings("rawtypes") Constructor constructor : 
	        	c.getConstructors()) 
	        {
	        	csoa = (ChemSoftOutputAnalyzer) constructor.newInstance();
	        }
        } catch (InstantiationException 
        		| IllegalAccessException 
        		| IllegalArgumentException 
        		| InvocationTargetException  exception) {
	        throw new IllegalStateException("analyzer " + c.getSimpleName() 
	        	+ "could not ne instantiated");
        }
        return csoa;
    }
	
//------------------------------------------------------------------------------

	/**
	 * @return the list of software that can be analyzed by any of the 
	 * registered analyzers.
	 */
	public List<String> getAnalyzableSoftwareNames()
	{
		return new ArrayList<String>(knownAnalyzers.keySet());
	}
	
//-----------------------------------------------------------------------------

	public ChemSoftOutputAnalyzerBuilder registerAnalyzer(String chemSoftName,
			Object analyzer)
	{
		if (analyzer instanceof ChemSoftOutputAnalyzer)
		{
			knownAnalyzers.put(chemSoftName, (ChemSoftOutputAnalyzer) analyzer);
		} else {
			//TODO-gg: log warning
			System.err.println("Registration of analyzer has failed because "
					+ "the given analyzer is not an instance of "
					+ ChemSoftOutputAnalyzer.class.getSimpleName() 
					+ ". Ignoting analyser of software '" + chemSoftName + "'");
		}
	    return this;
	}
    
//------------------------------------------------------------------------------
	
}
