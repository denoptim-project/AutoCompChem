package autocompchem.chemsoftware.orca;

import java.util.ArrayList;

import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;

/**
 * Object representing an Orca job that may include one or more 
 * sub jobs/tasks/steps.
 *
 * @author Marco Foscato
 */

public class OrcaJob
{
    /**
     * List of sub jobs/tasks/steps
     */
    private ArrayList<OrcaJob> steps = new ArrayList<OrcaJob>();

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty Orca job
     */

    public OrcaJob()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Construct a Orca job from a formatted file (JobDetails) with
     * instructions and parameters that define the whore calculation.
     * @param inFile formatted JobDetails' file to be read
     */

    public OrcaJob(String inFile)
    {
        this(IOtools.readTXT(inFile));
    }

//------------------------------------------------------------------------------

    /**
     * Construct a Orca Job object from a formatted text divided
     * in lines. The format of these lines is expected to adhere to that of
     * JobDetails files.
     * @param lines array of lines to be read
     */

    public OrcaJob(ArrayList<String> lines)
    {
    	//TODO
    	//TextAnalyzer.readKeyValue(lines, ChemSoftConstants.KEYVALSEPARATOR, ChemSoftConstants.CcommentLab, start, end)
        /*
    	ArrayList<String> linesOfAStep = new ArrayList<String>();
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i).trim();

            // Split and interprete each step/Task
            if (line.toUpperCase().equals(ChemSoftConstants.TASKSEPARATORJD))
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
        */
    }

//------------------------------------------------------------------------------

    /**
     * Add a single step (i.e., task) to this Orca job. 
     * The new step is appended after
     * all previously existing steps.
     * @param step the new step (i.e., task) to be added
     */

    public void addStep(OrcaJob step)
    {
        steps.add(step);
    }

//------------------------------------------------------------------------------

    /**
     * Get a specific step (i.e., task) from this Orca Job
     * @param i the index of the step (0 to n-1)
     * @return the given step
     */

    public OrcaJob getStep(int i)
    {
        if (i > steps.size())
        {
            Terminator.withMsgAndStatus("ERROR! Trying to get step number " + i
                           + " in a Orca job that has only " + steps.size()
                                                                + " steps.",-1);
        }
        return steps.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of steps (i.e., tasks) of this Orca Job
     * @return the number of steps
     */

    public int getNumberOfSteps()
    {
        return steps.size();
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
     * Change the prefix in all steps
     * @param prefix string to be used as file prefix in all steps
     */

    public void setAllPrefix(String prefix)
    {
        for (int i=0; i<steps.size(); i++)
        {
           //TODO getStep(i).setPrefix(prefix);
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
            //Write job-separator
            if (step != 0)
            {
            	//TODO
            	//FIXME OrcaConstants.TASKSEPARATORJD or start end
                lines.add("_____");
            }

            lines.addAll(getStep(step).toLinesJobDetails());
        }
        return lines;
    }

//------------------------------------------------------------------------------

}
