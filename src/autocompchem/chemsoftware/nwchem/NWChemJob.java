package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;

import autocompchem.io.IOtools;
import autocompchem.modeling.basisset.BasisSetConstants;
import autocompchem.run.Terminator;

/**
 * Object representing a complete NWChem job and may include one or more tasks 
 * (i.e., steps).
 * An <code>NWChemJob</code> can be constructed from
 * a text file (the so-called JOBDETAILS file) that contains the denifition of
 * one or more {@link NWChemTask}s, i.e., the steps in a multi-step job. 
 * The format used to specify the details of each task is defined  in the 
 * {@link NWChemTask}'s documentation, and 
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#TASKSEPARATORJD}
 * (without double quote) is used as task separator.
 * <br>
 * <code>
 * ----- beginning of text file -----
 * ...<br>
 * ... definition of the first {@link NWChemTask} ...<br>
 * ...<br>
 * [separator]<br>
 * ...<br>
 * ... definition of the second {@link NWChemTask} ...<br>
 * ...<br>
 * [separator]<br>
 * ...<br>
 * ... definition of the third and last {@link NWChemTask} ...<br>
 * ...<br>
 * ----- end of text file ----- <br>
 * </code>
 *
 * @author Marco Foscato
 */

public class NWChemJob
{
    /**
     * List of tasks
     */
    private ArrayList<NWChemTask> steps = new ArrayList<NWChemTask>();

    /** 
     * Flag cotrolling use of tags for every atom
     */
    private boolean useTags = false;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty NWChem job
     */

    public NWChemJob()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Construct a NWChem Job object from a formatted file (JobDetails) with
     * instructions and parameters for the whole calculation. In the job details
     * single or multi step calculation can be specified, but Molecular 
     * specification section is not provided in the job details' file.
     * Molecular specification's section, i.e., the geometry,
     * can be defined/edited acting on the 
     * {@link NWChemTask} of a {@link NWChemJob} after the
     * construction of the latter by means of the method
     * <code>setMolSpecification</code>.
     * @param inFile formatted JobDetails' file to be read
     */

    public NWChemJob(String inFile)
    {
        this(IOtools.readTXT(inFile));
    }

//------------------------------------------------------------------------------

    /**
     * Construct a NWChem Job object from a formatted text divided
     * in lines. Molecular specification section is not read by this method.
     * Molecular specification's section can be defined/edited acting on the 
     * {@link NWChemTask} of a {@link NWChemJob} after the
     * construction of the latter by means of the method
     * <code>setMolSpecification</code>.
     * @param lines array of lines to be read
     */

    public NWChemJob(ArrayList<String> lines)
    {
        ArrayList<String> linesOfAStep = new ArrayList<String>();
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i).trim();

            // Look for any of the keywords that require use of atom tags
            if (!useTags 
                && line.toUpperCase().contains(BasisSetConstants.ATMSPECBS))
            {
                useTags = true;
            }

            // Split and interprete each step/Task
            if (line.toUpperCase().equals(NWChemConstants.TASKSEPARATORJD))
            {
                NWChemTask step = new NWChemTask(linesOfAStep);
                addStep(step);
                linesOfAStep.clear();
            } else {
                linesOfAStep.add(line);
            }
        }

        //Deal with the last step that doesn't have a separator at the end
        NWChemTask step = new NWChemTask(linesOfAStep);
        addStep(step);
    }

//------------------------------------------------------------------------------

    /**
     * Add a single step (i.e., task) to this NWChem job. 
     * The new step is appended after
     * all previously existing steps.
     * @param step the new step (i.e., task) to be added
     */

    public void addStep(NWChemTask step)
    {
        steps.add(step);
    }

//------------------------------------------------------------------------------

    /**
     * Get a specific step (i.e., task) from this NWChem Job
     * @param i the index of the step (0 to n-1)
     * @return the given step
     */

    public NWChemTask getStep(int i)
    {
        if (i > steps.size())
        {
            Terminator.withMsgAndStatus("ERROR! Trying to get step number " + i
                           + " in a NWChem job that has only " + steps.size()
                                                                + " steps.",-1);
        }
        return steps.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of steps (i.e., tasks) of this NWChem Job
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
            getStep(i).setCharge(newCharge);
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
           getStep(i).setSpinMultiplicity(newSpinMult);
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
           getStep(i).setTitle(title);
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
           getStep(i).setPrefix(prefix);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Checks is this NWChemJob requires the use of atom tags.
     * @return <code>true</code> is tags must be added to all atom.
     */

    public boolean requiresAtomTags()
    {
        return useTags;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the flag controlling the requirement of atom tags on all atoms.
     * @param value the boolean to be set
     */
 
    protected void setImpositionOfAtomTags(boolean value)
    {
        useTags = value;
    }

//------------------------------------------------------------------------------

    /**
     * Produced the text representation of this object as NWChem input file.
     * The text is returned as a list of strings, which are the
     * lines of the NWChem input file.
     * @return the list of lines ready to print an NWChem input file
     */

    public ArrayList<String> toLinesInput()
    {
        ArrayList<String> lines = new ArrayList<String>();

        for (int step = 0; step<steps.size(); step++)
        {
            if (step != 0)
            {
                lines.add(NWChemConstants.TASKSEPARATORNW);
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
     * not part of the constructor of NWChemJob. 
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
                lines.add(NWChemConstants.TASKSEPARATORJD);
            }

            lines.addAll(getStep(step).toLinesJobDetails());
        }
        return lines;
    }

//------------------------------------------------------------------------------

}
