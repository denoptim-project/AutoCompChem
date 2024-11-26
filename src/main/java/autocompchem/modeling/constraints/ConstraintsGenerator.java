package autocompchem.modeling.constraints;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Facility to generate constraints given a list of matching rules.
 * 
 * @author Marco Foscato
 */


public class ConstraintsGenerator extends AtomTupleGenerator
{   
    /**
     * String defining the task of generating constraints
     */
    public static final String GENERATECONSTRAINTSTASKNAME = 
    		"generateConstraints";

    /**
     * Task about generating constraints
     */
    public static final Task GENERATECONSTRAINTSTASK;
    static {
    	GENERATECONSTRAINTSTASK = Task.make(GENERATECONSTRAINTSTASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public ConstraintsGenerator()
    {
    	ruleRoot = ConstraintDefinition.BASENAME;
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATECONSTRAINTSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ConstraintsGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ConstraintsGenerator();
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
        	rules.add(new ConstraintDefinition(line, ruleID.getAndIncrement()));
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
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(GENERATECONSTRAINTSTASK))
    	{
	    	ConstraintsSet cs = createConstraints(iac);
	        
	        logger.debug(cs.toString());
	        
	        if (outFile!=null)
            {
            	outFileAlreadyUsed = true;
            	IOtools.writeTXTAppend(outFile, cs.toString(), true);
            }
	        
	        if (exposedOutputCollector != null)
	    	{
    			String molID = "mol-"+i;
  		        exposeOutputData(new NamedData(
  		        		GENERATECONSTRAINTSTASK.ID + "_" + molID, cs));
	    	}
    	} else {
    		dealWithTaskMismatch();
        }
    	return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Define constraints in a given molecule and using the currently loaded 
     * atom tuple defining rules.
     * @param mol the molecular system we create constraints for.
     * @return the set of constraints.
     */

    public ConstraintsSet createConstraints(IAtomContainer mol)
    {
    	return createConstraints(mol, rules);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define constraints in a given molecule and using the a given set of
     * atom tuple defining rules.
     * @param mol the molecular system we create constraints for.
     * @return the set of constraints.
     */

    public static ConstraintsSet createConstraints(IAtomContainer mol, 
    		List<AtomTupleMatchingRule> rules)
    {
    	ConstraintsSet cLst = new ConstraintsSet();
    	
    	List<AnnotatedAtomTuple> tuples = createTuples(mol, rules);
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		cLst.add(new Constraint(tuple));
    	}
        return cLst;
    }

//-----------------------------------------------------------------------------

}
