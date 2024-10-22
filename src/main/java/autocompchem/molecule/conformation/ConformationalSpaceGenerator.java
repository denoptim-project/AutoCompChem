package autocompchem.molecule.conformation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.modeling.constraints.ConstraintDefinition;
import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Tool to generate the definition of a conformational spaces based on a given 
 * set rules meant to define {@link ConformationalCoordinate}s.
 * 
 * @author Marco Foscato
 */

public class ConformationalSpaceGenerator extends AtomTupleGenerator
{
    /**
     * String defining the task of generating a definition of conformational 
     * space
     */
    public static final String GENERATECONFORMATIONALSPACETASKNAME = 
    		"generateConformationalSpace";

    /**
     * Task about generating a definition of conformational 
     * space
     */
    public static final Task GENERATECONFORMATIONALSPACETASK;
    static {
    	GENERATECONFORMATIONALSPACETASK = 
    			Task.make(GENERATECONFORMATIONALSPACETASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public ConformationalSpaceGenerator()
    {
    	// NB: this workers inherits all from the super class
    	ruleRoot = ConformationalCoordDefinition.BASENAME;
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATECONFORMATIONALSPACETASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ConformationalSpaceGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ConformationalSpaceGenerator();
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link ConstraintDefinition} and adds
     * the resulting rules to this instance of atom tuple generator.
     * @param lines the lines of text to be parsed into 
     * {@link AtomTupleMatchingRule}s.
     */

    @Override
    public void parseAtomTupleMatchingRules(List<String> lines)
    {
        for (String line : lines)
        {
        	rules.add(new ConformationalCoordDefinition(line, 
        			ruleID.getAndIncrement()));
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
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(GENERATECONFORMATIONALSPACETASK))
    	{   
    		ConformationalSpace cs = createConformationalSpace(iac);
            
    		cs.printAll(logger);
            
            if (exposedOutputCollector != null)
        	{
    			String molID = "mol-"+i;
    	        exposeOutputData(new NamedData(
    	        		GENERATECONFORMATIONALSPACETASK.ID + "-" + molID, cs));
        	}
    	} else {
    		dealWithTaskMismatch();
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Define conformational space in a given molecule using the currently  
     * loaded set of rules for defining {@link ConformationalCoordinate}s.
     * @param mol the molecular system we create a conformational space for.
     * @return the resulting conformational space.
     */

    public ConformationalSpace createConformationalSpace(IAtomContainer mol)
    {
    	return createConformationalSpace(mol, rules, params);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define conformational space in a given molecule and using the a given set
     * rules for defining {@link ConformationalCoordinate}s.
     * @param mol the molecular system we create a conformational space for.
     * @param rules the rules for constructing the 
     * {@link ConformationalCoordinate}s.
     * @param params container of general purpose parameters. These may contain 
     * settings controlling the nuances of the process. E.g., how to generate 
     * atom labels that define each {@link ConformationalCoordinate}.
     * @return the resulting conformational space.
     */

    public static ConformationalSpace createConformationalSpace(
    		IAtomContainer mol, 
    		List<AtomTupleMatchingRule> rules, ParameterStorage params)
    {

        // If needed, generate atom labels
    	List<String> labels = null;
        for (AtomTupleMatchingRule r : rules)
        {
    		if (r.hasValuelessAttribute(AtomTupleConstants.KEYGETATOMLABELS))
    		{
    	    	ParameterStorage labMakerParams = params.clone();
    			labels = generateAtomLabels(mol, labMakerParams);
    			break;
    		}
        }
        
    	ConformationalSpace confSpace = new ConformationalSpace();
    	List<AnnotatedAtomTuple> tuples = createTuples(mol, rules, labels);
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		confSpace.add(new ConformationalCoordinate(tuple));
    	}
        return confSpace;
    }

//-----------------------------------------------------------------------------

}
