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
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import jakarta.el.ExpressionFactory;


/**
 * Facility to generate constraints given a list of matching rules.
 * 
 * @author Marco Foscato
 */

public class ConstraintsGenerator extends AtomTupleGenerator
{
    /**
     * Key for parameter specifying the first given set of constraints
     */
    public static String KEYSETCONSTRAINTSA = "CONSTRAINTS-A";

    /**
     * The first set of constraints for building an interpolated set
     */
    private ConstraintsSet constraintsA;

    /**
     * Key for parameter specifying the second given set of constraints
     */
    public static String KEYSETCONSTRAINTSB = "CONSTRAINTS-B";

    /**
     * The second set of constraints for building an interpolated set
     */
    private ConstraintsSet constraintsB;

    /**
     * Key for parameter specifying the lambda coefficient for the conversion 
     * constraints A into constraints B. Lambda indicates the intermediate
     * position between A (lambda=0.0) and B (lambda=1.0)
     */
    public static String KEYINTERPOLATIONFACTOR = "INTERPOLATIONFACTOR";

    /**
     * The interpolation factor
     */
    private Double interpolationFactor;

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

    /**
     * String defining the task of generating constraints
     */
    public static final String INTERPOLATECONSTRAINTSTASKNAME = 
    		"interpolateConstraints";

    /**
     * Task about interpolating constraints
     */
    public static final Task INTERPOLATECONSTRAINTSTASK;
    static {
    	INTERPOLATECONSTRAINTSTASK = Task.make(INTERPOLATECONSTRAINTSTASKNAME);
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
             Arrays.asList(GENERATECONSTRAINTSTASK,
                INTERPOLATECONSTRAINTSTASK
             )));
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
     * {@link ConstraintDefinition}s.
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
     * Initialize the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        super.initialize();
        
        if (params.contains(KEYINTERPOLATIONFACTOR))
        {
            String value = params.getParameter(KEYINTERPOLATIONFACTOR)
                .getValueAsString();
            interpolationFactor = NumberUtils.parseValueOrExpression(value);
        }

        if (params.contains(KEYSETCONSTRAINTSA))
        {
            constraintsA = (ConstraintsSet) params.getParameter(
                KEYSETCONSTRAINTSA).getValue();
        }

        if (params.contains(KEYSETCONSTRAINTSB))
        {
            constraintsB = (ConstraintsSet) params.getParameter(
                KEYSETCONSTRAINTSB).getValue();
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
        if (task.equals(INTERPOLATECONSTRAINTSTASK))
        {
            ConstraintsSet cs = interpolateConstraints(constraintsA, 
                constraintsB, interpolationFactor);
        
            logger.debug(cs.toString());
            
            if (outFile!=null)
            {
                outFileAlreadyUsed = true;
                IOtools.writeTXTAppend(outFile, cs.toString(), true);
            }
            
            if (exposedOutputCollector != null)
            {
                exposeOutputData(new NamedData(INTERPOLATECONSTRAINTSTASK.ID, cs));
            }
        } else {
    	    processInput();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Create a new set of constraints by computing new constraints based on the
     * given input.
     * @param constraintsA
     * @param constraintsB
     * @param interpolationFactor
     * @return
     */
    public static ConstraintsSet interpolateConstraints(
        ConstraintsSet constraintsA, ConstraintsSet constraintsB, 
        Double interpolationFactor)
    {
        if (constraintsA == null || constraintsA.isEmpty())
        {
            Terminator.withMsgAndStatus("Missing or empty initial "
                + "set of constraints. "
                + "Please, provide a constraintsSet as value for " 
                + KEYSETCONSTRAINTSA + ".", -1); 
            return new ConstraintsSet();
        }
        if (constraintsB == null || constraintsB.isEmpty())
        {
            Terminator.withMsgAndStatus("Missing or empty final "
                + "set of constraints. "
                + "Please, provide a constraintsSet as value for " 
                + KEYSETCONSTRAINTSB + ".", -1); 
            return new ConstraintsSet();
        }
        if (interpolationFactor == null)
        {
            Terminator.withMsgAndStatus("Missing or empty final "
                + "set of constraints. "
                + "Please, provide a value for " 
                + KEYINTERPOLATIONFACTOR + ".", -1); 
            return new ConstraintsSet();
        }

        ConstraintsSet interpolatesCnstrs = new ConstraintsSet();

        // Find pair of corresponding constraints in constraintsA and constraintsB
        for (Constraint cA : constraintsA)
        {
            Constraint cB = null;
            List<Integer> idsA = cA.getAtomIDs();
            for (Constraint candidate : constraintsB.getConstrainsWithType(cA.getType()))
            {
                List<Integer> idsB = candidate.getAtomIDs();
                if (idsA.size() == idsB.size())
                {
                    boolean match = true;
                    for (int i = 0; i < idsA.size(); i++)
                    {
                        if (idsA.get(i) != idsB.get(i))
                        {
                            match = false;
                            break;
                        }
                    }
                    if (match)
                    {
                        cB = candidate;
                        break;
                    }
                }
            }
            
            if (cB == null)
            {
                Terminator.withMsgAndStatus("No matching constraint found for "
                    + cA.toString() + " in " + constraintsB.toString() + ".", -1); 
            }

            // NB: the set value takes priority over the current value
            Double valueA = cA.getValue();
            Double valueB = cB.getValue();
            if (cA.hasCurrentValue())
            {
                valueA = cA.getCurrentValue();
            }
            if (cB.hasCurrentValue())
            {
                valueB = cB.getCurrentValue();
            }

            // Clone constraint A to preserve all its features
            Constraint interpolated = cA.clone();
            
            // Calculate interpolated value: value = valueA + lambda * (valueB - valueA)
            double lambda = interpolationFactor.doubleValue();
            double interpolatedValue = valueA + lambda * (valueB - valueA);
            
            // Set the interpolated value
            interpolated.setValue(interpolatedValue);
            
            interpolatesCnstrs.add(interpolated);
        }
        
        return interpolatesCnstrs;
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
