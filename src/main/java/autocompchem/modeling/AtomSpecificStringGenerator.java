package autocompchem.modeling;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AnnotatedAtomTupleList;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.run.Job;
import autocompchem.text.TextBlock;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Tool to generate strings that are specific atoms found in a system. 
 * Effectively it provides the capability to search for one or more atoms,
 * make an ordered list (i.e., {@link AnnotatedAtomTuple})
 * with their identifiers or labels (as from {@link AtomLabelsGenerator}), 
 * combine the resulting strings into an overall string 
 * that can be decorated with associated prefix/suffix strings.
 * 
 * @author Marco Foscato
 */

public class AtomSpecificStringGenerator extends AtomContainerInputProcessor
{
    
    /**
     * Separator used to report the identifiers in the tuple
     */
    private String idSeparator = "";
    
    /**
     * Separator between prefix/suffix and items
     */
    private String fieldSeparator = "";

    /**
     * String defining the task of generating tuples of atoms
     */
    public static final String GETATOMSPECIFICSTRINGTASKNAME = 
    		"getAtomSpecificString";

    /**
     * Task about generating tuples of atoms
     */
    public static final Task GETATOMSPECIFICSTRINGTASK;
    static {
    	GETATOMSPECIFICSTRINGTASK = Task.make(GETATOMSPECIFICSTRINGTASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public AtomSpecificStringGenerator()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GETATOMSPECIFICSTRINGTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomSpecificStringGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new AtomSpecificStringGenerator();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

	@Override
    public void initialize()
    {   
		super.initialize();
		
        if (params.contains("IDSEPARATOR"))
        {
        	idSeparator = params.getParameter("IDSEPARATOR")
        		.getValueAsString();
        }
        
        if (params.contains("FIELDSEPARATOR"))
        {
        	fieldSeparator = params.getParameter("FIELDSEPARATOR")
        		.getValueAsString();
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @Override
    public void performTask()
    {
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(GETATOMSPECIFICSTRINGTASK))
    	{
	    	// Adjust parameters to configure AtomTupleGenerator
	        ParameterStorage tupleGenParams = params.clone();
	        tupleGenParams.setParameter(WorkerConstants.PARTASK,
	        		AtomTupleGenerator.GENERATEATOMTUPLESTASK.ID);
	        
	        // Run tuple generator
	        Worker embeddedWorker = null;
			try {
				embeddedWorker = WorkerFactory.createWorker(tupleGenParams, 
						myJob);
			} catch (ClassNotFoundException e) {
				// Cannot happen... unless there is a bug
				e.printStackTrace();
			}
	    	NamedDataCollector outputOfEmbedded = new NamedDataCollector();
	    	embeddedWorker.setDataCollector(outputOfEmbedded);
	    	embeddedWorker.performTask();
	    	
	    	TextBlock atomStringsForThisMol = new TextBlock();;
	        
	    	// Get atom-specific strings (i.e., annotated atom tuples)
	    	for (String key : outputOfEmbedded.getAllNamedData().keySet())
	    	{
	    		// As safety measure, ignore unexpected output, but there should
	    		// be only one named data matching.
	    		if (key.startsWith(AtomTupleGenerator.GENERATEATOMTUPLESTASK.ID))
	    		{
	    			AnnotatedAtomTupleList tuples = (AnnotatedAtomTupleList) 
	    					outputOfEmbedded.getNamedData(key).getValue();
	    			// NB: we are we not using the AnnotatedAtomTupleList because
	    			// it would embed multiple items into a single one that
	    			// would be what we deal with in an downstream processing.
	    			for (AnnotatedAtomTuple tuple : tuples)
	    			{
	    				atomStringsForThisMol.add(convertTupleToAtomSpecString(
	    						tuple));
	    			}
	    		}
	    	}
	    	
            if (outFile!=null)
            {
            	outFileAlreadyUsed = true;
            	StringBuilder sb = new StringBuilder();
	    		int jj = 0;
	    		for (String one : atomStringsForThisMol)
	    		{
	    			jj++;
	    			sb.append("mol-").append(i).append("_hit-").append(jj)
	    				.append(": ").append(one)
	    				.append(System.getProperty("line.separator"));
	    		}
            	IOtools.writeTXTAppend(outFile, sb.toString(), true);
            }
	
	        if (exposedOutputCollector != null)
	    	{
	    		int jj = 0;
	    		for (String one : atomStringsForThisMol)
	    		{
	    			jj++;
	  		        exposeOutputData(new NamedData(
	  		        		GETATOMSPECIFICSTRINGTASK.ID + "_mol-" + i 
	  		        			+ "_hit-" + jj, one));
	    		}
	    	}
		} else {
			dealWithTaskMismatch();
	    }
    	return iac;
    }
	
//------------------------------------------------------------------------------
    
    /**
     * Generated the string representation of the tuple using the separators 
     * configured in this instance.
     * @param tuple the atom tuple to process
     * @return the resulting string.
     */
    public String convertTupleToAtomSpecString(AnnotatedAtomTuple tuple)
    {
    	return convertTupleToAtomSpecString(tuple, idSeparator, fieldSeparator);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Generated the string representation of the tuple.
     * @param tuple the atom tuple to process
     * @param idSeparator string used to separate item identifiers
     * @param fieldSeparator string used to separate prefix/suffix and item 
     * identifiers.
     * @return the resulting string.
     */
    public static String convertTupleToAtomSpecString(AnnotatedAtomTuple tuple,
    		String idSeparator, String fieldSeparator)
    {
    	String ids = null;
    	if (tuple.getAtmLabels()!=null)
    	{
    		ids = StringUtils.mergeListToString(tuple.getAtmLabels(), 
    				idSeparator, true);
    	} else {
    		ids = StringUtils.mergeListToString(tuple.getAtomIDs(), 
    				idSeparator, true);
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append(tuple.getPrefix());
    	sb.append(fieldSeparator);
		sb.append(ids);
		sb.append(fieldSeparator);
		sb.append(tuple.getSuffix());
    	
    	return sb.toString();
    }
    
//-----------------------------------------------------------------------------

}
