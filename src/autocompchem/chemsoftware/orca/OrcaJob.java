package autocompchem.chemsoftware.orca;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveFactory;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.text.TextAnalyzer;

/**
 * Object representing an Orca job that may include one or more 
 * sub jobs/tasks/steps.
 *
 * @author Marco Foscato
 */

public class OrcaJob extends Job
{
	/**
	 * List of settings, data, and keywords for Orca
	 */
	private ArrayList<Directive> directives = new ArrayList<Directive>();

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty Orca job
     */

    public OrcaJob()
    {
    	super();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a Orca job from a formatted file (i.e., ACC's job details file)
     * containing instructions and parameters that define the calculation.
     * @param inFile formatted job details file to be read.
     */

    public OrcaJob(String inFile)
    {
        this(IOtools.readTXT(inFile));
    }

//------------------------------------------------------------------------------

    /**
     * Construct a OrcaJob object from a formatted text divided
     * in lines. The format of these lines is expected to adhere to that of
     * job details files.
     * @param lines array of lines to be read.
     */

    public OrcaJob(ArrayList<String> lines)
    {
    	super();
    	
    	if (lines.toString().contains(ChemSoftConstants.JDLABSTEPSEPARATOR))
    	{
	    	ArrayList<String> newLines = 
	    			TextAnalyzer.readTextWithMultilineBlocks(lines, 
	    			ChemSoftConstants.JDCOMMENT, 
	    			ChemSoftConstants.JDOPENBLOCK, 
	    			ChemSoftConstants.JDCLOSEBLOCK);
	
	    	ArrayList<String> linesOfAStep = new ArrayList<String>();
	        for (int i=0; i<newLines.size(); i++)
	        {
	            String line = lines.get(i).trim();
	
	            if (line.toUpperCase().equals(
	            		ChemSoftConstants.JDLABSTEPSEPARATOR))
	            {
	                OrcaJob step = new OrcaJob(linesOfAStep);
	                addStep(step);
	                linesOfAStep.clear();
	            } else {
	                linesOfAStep.add(line);
	            }
	        }
	        //Deal with the last step that doesn't have a separator at the end
	        OrcaJob step = new OrcaJob(linesOfAStep);
	        addStep(step);
    	} else {
    		directives = DirectiveFactory.buildAllFromJDText(lines);
    	}
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Looks into the existing directives for ACC tasks, and performs them.
     * Depending on the task, some directives may be changed as a result of the
     * ACC tasks. 
     * @param mol the molecular representation given to mol-dependent tasks.
     */
    
    public void processDirectives(IAtomContainer mol)
    {
    	for (Directive d : directives)
    	{
    		if (!d.hasACCTask())
    		{
    			continue;
    		}
    		d.performACCTasks(mol,this);
    	}
    }

//-----------------------------------------------------------------------------
    
    /**
     * @param name of the directive to return (case insensitive).
     * @return the directive or null if none is found with that name.
     */
    
    public Directive getDirective(String name)
    {
    	for (Directive d : directives)
    	{
    		if (d.getName().toUpperCase().equals(name.toUpperCase()))
    		{
    			return d;
    		}
    	}
    	return null;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Retrieves the wanted step.
     * @param i the index of the directive
     * @return the directive    
     */
    
    public Directive getDirective(int i)
    {
    	return directives.get(i);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Adds directive to this job or, if the a directive with such name already
     * exists, replaces the existing one with the given one.
     * @param d the given directive
     */
    
    public void setDirective(Directive d)
    {
    	if (directives.contains(d))
    	{
    		int id = directives.indexOf(d);
    		directives.set(id, d);
    	} else {
    	    directives.add(d);
    	}
    }

////------------------------------------------------------------------------------
//
//    /**
//     * Change the charge in all steps
//     * @param newCharge new value of the charge for all steps
//     */
//
//    public void setAllCharge(int newCharge)
//    {
//        for (int i=0; i<steps.size(); i++)
//        {
//        	//TODO
////            getStep(i).setCharge(newCharge);
//        }
//    }
//
////------------------------------------------------------------------------------
//
//    /**
//     * Change spin multiplicity in all steps
//     * @param newSpinMult new value of the spin multiplicity for all steps
//     */
//
//    public void setAllSpinMultiplicity(int newSpinMult)
//    {
//        for (int i=0; i<steps.size(); i++)
//        {
//           //TODO getStep(i).setSpinMultiplicity(newSpinMult);
//        }
//    }
//
////------------------------------------------------------------------------------
//
//    /**
//     * Change the title in all steps
//     * @param title string to be used as title in all steps
//     */
//
//    public void setAllTitle(String title)
//    {
//        for (int i=0; i<steps.size(); i++)
//        {
//           //TODO getStep(i).setTitle(title);
//        }
//    }

//------------------------------------------------------------------------------

    /**
     * Produced the text representation of this object as Orca input file.
     * The text is returned as a list of strings, which are the
     * lines of the Orca input file.
     * @return the list of lines ready to print an Orca input file
     */

    public ArrayList<String> toLinesInput()
    {
        ArrayList<String> lines = new ArrayList<String>();

        for (int step = 0; step<steps.size(); step++)
        {
            if (step != 0)
            {
            	//TODO
                //lines.add(ChemSoftConstants.TASKSEPARATORNW);
            }

            lines.addAll(getStep(step).toLinesInput());
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this object following the format of
     * autocompchem's JobDetail text file.
     * This method ignored molecular specification sections because they are
     * not part of the constructor of OrcaJob. 
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines= new ArrayList<String>();
        for (int step = 0; step<steps.size(); step++)
        {
            if (step != 0)
            {
                lines.add(ChemSoftConstants.JDLABSTEPSEPARATOR);
            }
            lines.addAll(getStep(step).toLinesJobDetails());
        }
        return lines;
    }

//------------------------------------------------------------------------------

}
