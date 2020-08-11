package autocompchem.chemsoftware.orca;

import java.util.ArrayList;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.Directive;
import autocompchem.io.IOtools;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
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
     * Construct a Orca job from a formatted file (JobDetails) with
     * instructions and parameters that define the whore calculation.
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
    		//TODO directives from text
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Change the charge in all steps
     * @param newCharge new value of the charge for all steps
     */

    public void setAllCharge(int newCharge)
    {
        for (int i=0; i<steps.size(); i++)
        {
        	//TODO
//            getStep(i).setCharge(newCharge);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Change spin multiplicity in all steps
     * @param newSpinMult new value of the spin multiplicity for all steps
     */

    public void setAllSpinMultiplicity(int newSpinMult)
    {
        for (int i=0; i<steps.size(); i++)
        {
           //TODO getStep(i).setSpinMultiplicity(newSpinMult);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Change the title in all steps
     * @param title string to be used as title in all steps
     */

    public void setAllTitle(String title)
    {
        for (int i=0; i<steps.size(); i++)
        {
           //TODO getStep(i).setTitle(title);
        }
    }

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
