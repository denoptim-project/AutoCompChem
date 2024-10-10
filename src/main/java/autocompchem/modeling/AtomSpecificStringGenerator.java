package autocompchem.modeling;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
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
	public void processOneAtomContainer(IAtomContainer iac, int i) 
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
	
	        if (exposedOutputCollector != null)
	    	{
	    		int jj = 0;
	    		for (String one : atomStringsForThisMol)
	    		{
	    			jj++;
	  		        exposeOutputData(new NamedData(
	  		        		GETATOMSPECIFICSTRINGTASK.ID + "_mol-" + i 
	  		        			+ "_hit-" + jj, 
	  		        		NamedDataType.STRING, one));
	    		}
	    	}
		} else {
			dealWithTaskMismatch();
	    }
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
