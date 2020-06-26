package autocompchem.parameters;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import autocompchem.chemsoftware.nwchem.NWChemDirective;
import autocompchem.chemsoftware.nwchem.NWChemDirectiveComparator;
import autocompchem.constants.ACCConstants;
import autocompchem.io.IOtools;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlock;

/**
 * Storage and management of {@link Parameter}s. 
 * This class can also import parameters directly from a formatted text file
 * The recognized format is as follows:
 * <ul>
 * <li> lines beginning with 
 * {@value  autocompchem.parameters.ParameterConstants#COMMENTLINE} 
 * are ignored as comments</li>
 * <li> lines beginning with 
 * {@value autocompchem.parameters.ParameterConstants#STARTMULTILINE} are
 * considered part of a multi line block, together with all the lines that
 * follow until a line beginning with 
 * {@value autocompchem.parameters.ParameterConstants#ENDMULTILINE}
 * is found. All lines of a multi line block are interpreted as pertaining to a 
 * single {@link Parameter}. The text in between 
 * {@value autocompchem.parameters.ParameterConstants#STARTMULTILINE} and 
 * the 
 * {@value autocompchem.parameters.ParameterConstants#ENDMULTILINE}, 
 * apart from containing one or more
 * new line characters, follows the same syntax defined below for the single
 * line definition of a {@link Parameter}.</li>
 * <li> all other lines define each one a single {@link Parameter}.
 * All text before the separator (i.e., the first 
 * {@value autocompchem.parameters.ParameterConstants#SEPARATOR} 
 * character) is interpreted as the reference name of the {@link Parameter},
 * while the rest as its value/content.</li>
 * </ul>
 *
 * @author Marco Foscato
 */

public class ParameterStorage 
{

    //Map of parameters
    private Map<String,Parameter> allParams;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ParameterStorage
     */

    public ParameterStorage()
    {
        allParams = new HashMap<String,Parameter>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor from a filled map of parameters
     * @param allParams the map of parameters
     */
    
    public ParameterStorage(Map<String,Parameter> allParams)
    {
        this.allParams = allParams;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>false</code> if this ParameterStorage contains parameters
     */

    public boolean isEmpty()
    {
        return allParams.isEmpty();
    }

//------------------------------------------------------------------------------

    /**
     * Return the parameter required or null
     * @param ref reference name of the parameter
     * @return the parameter with the given reference string
     */

    public Parameter getParameterOrNull(String ref)
    {
        if (!this.contains(ref))
        {
            return null;
        }
        return allParams.get(ref);
    }


//------------------------------------------------------------------------------

    /**
     * Return the parameter required
     * @param ref reference name of the parameter
     * @return the parameter with the given reference string
     */

    public Parameter getParameter(String ref)
    {
        if (!this.contains(ref))
        {
            Terminator.withMsgAndStatus("ERROR! Key '" + ref + "' not found in "
                        + "ParameterStorage!",-1);
        }
        return allParams.get(ref);
    }

//------------------------------------------------------------------------------

    /**
     * Return the parameter required or the default.
     * If a specific parameter is defined in this
     * ParameterStorage, returns such parameter, otherwise returns its default
     * as defined in a specific class. For this to work the reference name
     * of the parameter must be the same of the field used to define the
     * default.
     * @param refName the reference name of the parameter
     * @param constClsName the fully qualified name of the class from which
     * the default value is to be taken
     * @return the user defined value or the default
     */

    public Parameter getParameterOrDefault(String refName, String constClsName)
    {
        Parameter p = new Parameter();
        if (this.contains(refName))
        {
             p = allParams.get(refName);
        }
        else
        {
            try
            {
                Class constCls = Class.forName(constClsName);
                Object constClsObj = (Object) constCls.newInstance();
                Field constField = constCls.getField(refName.toUpperCase());
                Class constType = constField.getType();
                p = new Parameter(refName,constField.getType().getName(),
                                                   constField.get(constClsObj));
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                Terminator.withMsgAndStatus("ERROR! No costant value for "
                    + refName + "' in '" + constClsName + "'.",-1);
            }
        }
        return p;
    }

//------------------------------------------------------------------------------

    /**
     * Return all the parameters stored
     * @return the map with all parameters
     */

    public Map<String,Parameter> getAllParameters()
    {
        return allParams;
    }

//------------------------------------------------------------------------------

    /**
     * Store a parameter with the given reference name. If the parameter already
     * exists, it will be overwritten
     * @param ref the reference name of the parameter
     * @param par the new parameter to be stores
     */

    public void setParameter(String ref, Parameter par)
    {
        allParams.put(ref,par); 
    }

//------------------------------------------------------------------------------

    /**
     * Search for a reference name
     * @param ref the reference name to be searched
     * @return <code>true</code> if this ParameterStorage contains a parameter
     * with the given reference name 
     */

    public boolean contains(String ref)
    {
        if (allParams.keySet().contains(ref))
            return true;
        else
            return false;
    }

//------------------------------------------------------------------------------

    /**
     * Set the default parameters
     */

    public void setDefault()
    {
        //Set default parameter
        Parameter vl = new Parameter(ACCConstants.VERBOSITYPAR, "integer",
                                                                           "0");
        allParams.put(ACCConstants.VERBOSITYPAR,vl);
    }

//------------------------------------------------------------------------------

    /**
     * Read a formatted text file and import all parameters.
     * Meant only for single-job parameter files. Cannot handle parameter files
     * including more than one job nor nested jobs.
     * @param paramFile name of the text file to read
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
        //Make the ParameterStorage object
        for (int i=0; i<blocks.size(); i++)
        {
            ArrayList<String> signleBlock = blocks.get(i);
            String key = signleBlock.get(0);
            String value = signleBlock.get(1);

            //All params are seen as Strings
            Parameter prm = new Parameter(key,"string",value);
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
        for (Entry<String, Parameter> par : allParams.entrySet())
        {
        	String parStr = par.getKey() + ParameterConstants.SEPARATOR 
        			+ par.getValue();
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
        for (String k : allParams.keySet())
        {
            sb.append(k).append("=").append(allParams.get(k)).append(", ");
        }
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
