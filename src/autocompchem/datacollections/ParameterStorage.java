package autocompchem.datacollections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import autocompchem.constants.ACCConstants;
import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlock;

/**
 * Storage of {@link Parameter}s. 
 * This class has also the capability of importing string-based parameters 
 * directly from a formatted text file.
 * The recognised format is as follows:
 * <ul>
 * <li> lines beginning with 
 * {@value  autocompchem.datacollections.ParameterConstants#COMMENTLINE} 
 * are ignored as comments</li>
 * <li> lines beginning with 
 * {@value autocompchem.datacollections.ParameterConstants#STARTMULTILINE} are
 * considered part of a multi line block, together with all the lines that
 * follow until a line beginning with 
 * {@value autocompchem.datacollections.ParameterConstants#ENDMULTILINE}
 * is found. All lines of a multi line block are interpreted as pertaining to a 
 * single {@link Parameter}. The text in between 
 * {@value autocompchem.datacollections.ParameterConstants#STARTMULTILINE} and 
 * the 
 * {@value autocompchem.datacollections.ParameterConstants#ENDMULTILINE}, 
 * apart from containing one or more
 * new line characters, follows the same syntax defined below for the single
 * line definition of a {@link Parameter}.</li>
 * <li> all other lines define each one a single {@link Parameter}.
 * All text before the separator (i.e., the first 
 * {@value autocompchem.datacollections.ParameterConstants#SEPARATOR} 
 * character) is interpreted as the reference name of the {@link Parameter},
 * while the rest as its value/content.</li>
 * </ul>
 *
 * @author Marco Foscato
 */

public class ParameterStorage extends NamedDataCollector
{

	/**
	 * Container of parameters
	 */
	private Map<String,Parameter> allPars = new HashMap<String,Parameter>();
	
//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ParameterStorage
     */

    public ParameterStorage()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor from a filled map of parameters
     * @param allData the map of parameters
     */
    
    public ParameterStorage(Map<String,Parameter> allData)
    {
        this.allData.putAll(allData);
    }

//------------------------------------------------------------------------------

    /**
     * Return the parameter corresponding to the given reference name.
     * @param ref reference name of the parameter.
     * @return the parameter with the given reference string.
     */

    public Parameter getParameterOrNull(String ref)
    {
        if (!this.contains(ref))
        {
            return null;
        }
        return (Parameter) allData.get(ref);
    }


//------------------------------------------------------------------------------

    /**
     * Return the parameter required, which is expected to exist. Kills process
     * with an error if the parameter does not exist.
     * @param ref reference name of the parameter.
     * @return the parameter with the given reference string.
     */

    public Parameter getParameter(String ref)
    {
        if (!this.contains(ref))
        {
            Terminator.withMsgAndStatus("ERROR! Key '" + ref + "' not found in "
                        + "ParameterStorage!",-1);
        }
        return (Parameter) allData.get(ref);
    }

//------------------------------------------------------------------------------

    /**
     * Return the parameter required or the given alternative.
     * @param refName the reference name of the parameter
     * @param defKind the parameter type to use for the default parameter
     * @param defValue the fully qualified name of the class from which
     * the default value is to be taken
     * @return the user defined value or the default
     */

    public Parameter getParameterOrDefault(String refName, 
    		Parameter.NamedDataType defKind, 
    		Object defValue)
    {
        Parameter p = new Parameter();
        if (this.contains(refName))
        {
             p = (Parameter) allData.get(refName);
        }
        else
        {
        	p = new Parameter(refName, defKind, defValue);
        }
        return p;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Updates the subset of NamedData as to contain only those that are
     * instances of the Parameter class.
     */
    
    private void updateParamMap()
    {
    	allPars.clear();
    	for (Entry<String,NamedData> e : allData.entrySet())
    	{    		
    		if (e.getValue() instanceof Parameter)
    		{
    			allPars.put(e.getKey(),(Parameter) e.getValue());
    		}
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Return all the parameters stored here.
     * @return the map with all parameters.
     */

    public Map<String,Parameter> getAllParameters()
    {
    	updateParamMap();
        return allPars;
    }

//------------------------------------------------------------------------------

    /**
     * Store a parameter with the given reference name. If the parameter already
     * exists, it will be overwritten.
     * @param ref the reference name of the parameter.
     * @param par the new parameter to be stores.
     */

    public void setParameter(String ref, Parameter par)
    {
        allData.put(ref,par); 
    }

//------------------------------------------------------------------------------

    /**
     * Set the default parameters.
     */

    public void setDefault()
    {
        //Set default parameter
        Parameter vl = new Parameter(ACCConstants.VERBOSITYPAR,
        		Parameter.NamedDataType.INTEGER,"0");
        allData.put(ACCConstants.VERBOSITYPAR,vl);
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted text file and import all parameters.
     * Meant only for single-job parameter files. Cannot handle parameter files
     * including more than one job nor nested jobs.
     * @param paramFile name of the text file to read.
     */

    public void importParameters(String paramFile) 
    {
        //Get filled form
        ArrayList<ArrayList<String>> form = IOtools.readFormattedText(paramFile,
                                                   ParameterConstants.SEPARATOR,
                                                 ParameterConstants.COMMENTLINE,
                                              ParameterConstants.STARTMULTILINE,
                                               ParameterConstants.ENDMULTILINE);

        //Make the ParameterStorage object
        importParameterBlocks(form);
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted block of text and import all parameters. 
     * Ignored nested blocks.
     * @param tb the block of lines to read
     */

    public void importParameters(TextBlock tb)
    {
        importParameterBlocks(TextAnalyzer.readKeyValue(tb.getText(),
                                                  ParameterConstants.SEPARATOR,
                                                ParameterConstants.COMMENTLINE,
                                             ParameterConstants.STARTMULTILINE,
                                              ParameterConstants.ENDMULTILINE));
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted text and import all parameters
     * @param filename pseudo file name used only for reporting errors, does
     * not need to be an existing file.
     * @param lines the block of lines to read
     */

    public void importParametersFromLines(String filename, 
                                                        ArrayList<String> lines)
    {
        //Get filled form
        ArrayList<ArrayList<String>> form = IOtools.readFormattedText(filename,
                                                                          lines,
                                                   ParameterConstants.SEPARATOR,
                                                 ParameterConstants.COMMENTLINE,
                                              ParameterConstants.STARTMULTILINE,
                                               ParameterConstants.ENDMULTILINE);

        //Make the ParameterStorage object
        importParameterBlocks(form);   
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted blocks and import parameters
     * @param blocks the block of text to read
     */

    public void importParameterBlocks(ArrayList<ArrayList<String>> blocks)
    {
        for (int i=0; i<blocks.size(); i++)
        {
            ArrayList<String> signleBlock = blocks.get(i);
            String key = signleBlock.get(0);
            String value = signleBlock.get(1);

            //All params read from text file are seen as Strings for now
            Parameter prm = new Parameter(key,NamedData.NamedDataType.STRING,
            		value);
            setParameter(key,prm);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a block of lines with the parameters stored in here formatted for
     * a jobdetails file.
     * @return the list of lines.
     */
    
    public ArrayList<String> toLinesJobDetails()
    {
    	//Collections.sort(directives, new JobDirectiveComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (Entry<String, Parameter> par : getAllParameters().entrySet())
        {
        	String parStr = par.getKey() + ParameterConstants.SEPARATOR 
        			+ par.getValue().getValueAsString();
            lines.add(parStr);
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * @return the string representation of this parameter storage
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[ParameterStorage [");
        for (String k : allData.keySet())
        {
            sb.append(k).append("=").append(allData.get(k)).append(", ");
        }
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
