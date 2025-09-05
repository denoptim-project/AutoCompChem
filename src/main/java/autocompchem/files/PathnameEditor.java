package autocompchem.files;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.NamedData;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.wiro.WIROConstants;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Tool to edit pathnames.
 * 
 * @author Marco Foscato
 */


public class PathnameEditor extends Worker
{
    /**
     * The input pathname to work with
     */
	private File input;
	
	/**
	 * String meant to be pre-pended (i.e., added before, no separator)
	 * to the processed pathname
	 */
	private String prefix = "";
	
	/**
	 * String meant to be appended (i.e., no separator)
	 * to the processed pathname
	 */
	private String suffix = "";
	
	/**
	 * String meant to be added both before and after 
	 * to the processed pathname, like quotation marks.
	 */
	private String quotationMark = "";
	
    /**
     * String defining the task of getting and possibly editing a pathname.
     */
    public static final String GETPATHNAMETASKNAME = "getPathName";

    /**
     * Task about getting and possibly editing a pathname.
     */
    public static final Task GETPATHNAMETASK;
    static {
    	GETPATHNAMETASK = Task.make(GETPATHNAMETASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public PathnameEditor()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GETPATHNAMETASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/PathnameEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new PathnameEditor();
    }
    
//-----------------------------------------------------------------------------

	public void initialize() 
	{
    	super.initialize();
    	
        // Multiple ways to define the input pathname are here processes from
        // the lowest priority to the highest. Thus, here we implicitly define
        // the priority order.
        if (params.contains(WIROConstants.PAROUTFILEROOT))
        {
            this.input = getNewFile(
            		params.getParameter(WIROConstants.PAROUTFILEROOT)
            		.getValueAsString());
        }

        if (params.contains("PATHNAME"))
        {
            this.input = getNewFile(
            		params.getParameter("PATHNAME").getValueAsString());
        }

        if (params.contains("PREFIX"))
        {
            this.prefix = params.getParameter("PREFIX").getValueAsString();
        }
        
        if (params.contains("SUFFIX"))
        {
            this.suffix = params.getParameter("SUFFIX").getValueAsString();
        }
        
        if (params.contains("QUOTATION"))
        {
            this.quotationMark = 
            		params.getParameter("QUOTATION").getValueAsString();
        }
	}
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialized.
     */

    @Override
    public void performTask()
    {
    	if (task.equals(GETPATHNAMETASK))
    	{
    		getPathName();
    	} else {
    		dealWithTaskMismatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Processes the input given upon initialization to produce a processed
     * pathname.
     */

    public String getPathName()
    {
        if (input==null)
        {
            Terminator.withMsgAndStatus("ERROR! Missing input pathname. "
                + "Nothing to do",-1);
        }
        
        String processedPathname = processPathname(input.getPath());
        
        String result = quotationMark
        		+ prefix 
        		+ processedPathname 
        		+ suffix
        		+ quotationMark;
        
        if (exposedOutputCollector != null)
    	{
	        exposeOutputData(new NamedData(GETPATHNAMETASK.ID, result));
    	}
        
        return result;
    }

//-----------------------------------------------------------------------------

    /**
     * NB: this method is a placeholder of pathname processing functionality,
     * which is, however, not yet available.
     * 
     * @param original
     * @return the processed string
     */
    public String processPathname(String original)
    {
    	// NB: no processing is currently implemented
    	return original;
    }
    
//-----------------------------------------------------------------------------

}
