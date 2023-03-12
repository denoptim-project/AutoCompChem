package autocompchem.chemsoftware.gaussian;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.io.IOtools;
import autocompchem.run.Terminator;

/**
 * Object representing a Gaussian job being it a single or multi step
 * job. The object <code>GaussianJob</code> can be defined directly from
 * a text file (the JOBDETAILS file) which may contain information to
 * define one or more {@link GaussianStep}. To separate the steps
 * a separator (
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#STEPSEPARATOR}
 *  - without double quote signs
 * ) is used.
 * The format used to define each step is defined in the documentation of 
 * the {@link GaussianStep}.
 * <br>
 * <br>
 * <code> 
 * ----- beginning of text file -----
 * ...<br>
 * ... definition of the first {@link GaussianStep} ...<br>
 * ...<br>
 * [separator]<br>
 * ...<br>
 * ... definition of the second {@link GaussianStep} ...<br>
 * ...<br>
 * [separator]<br>
 * ...<br>
 * ... definition of the third and last {@link GaussianStep} ...<br>
 * ...<br>
 * ----- end of text file ----- <br>
 * </code>
 * 
 * @author Marco Foscato
 */

public class GaussianJob
{
    /**
     * The list of steps in this job
     */
    private List<GaussianStep> steps = new ArrayList<GaussianStep>();

    /**
     * the total number of steps
     */
    private int numSteps;


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty compound Gaussian job with
     */

    public GaussianJob()
    {
        numSteps = 0;
    }

//------------------------------------------------------------------------------

    /**
     * Construct a Gaussian Job object from a formatted file (JobDetails) with
     * instructions and parameters for the whole calculation. In the job details
     * single or multi step calculation can be specified, but Molecular 
     * specification section is not provided in the such job details' file.
     * Molecular specification's section can be defined/edited acting on the 
     * {@link GaussianStep} of a {@link GaussianJob} after the
     * construction of the latter by means of the method
     * <code>setMolSpecification</code>.
     * @param inFile formatted JobDetails' file to be read
     */

    public GaussianJob(String inFile)
    {
        this(IOtools.readTXT(inFile,true));
    }

//------------------------------------------------------------------------------

    /**
     * Construct a Gaussian Job/Input object from a formatted text divided
     * in lines. Molecular specification section is not read by this method.
     * Molecular specification's section can be defined/edited acting on the 
     * {@link GaussianStep} of a {@link GaussianJob} after the
     * construction of the latter by means of the method
     * <code>setMolSpecification</code>.
     * @param lines array of lines to be read
     */

    public GaussianJob(List<String> lines)
    {
        numSteps = 0;
        ArrayList<String> linesOfAStep = new ArrayList<String>();
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);

            if (line.toUpperCase().contains(ParameterConstants.STARTMULTILINE))
            {
                boolean goon = true;
                while (goon && i<lines.size())
                {
                    i++;
                    String inLine = lines.get(i);
                    goon = !inLine.toUpperCase().contains(
                                               ParameterConstants.ENDMULTILINE);
                    line = line + System.getProperty("line.separator") + inLine;
                }
                if (goon)
                {
                    String msg = "ERROR! A multiline block was opened but no "
                                 + "closed. Check the input, in particular the "
                                 + "following block of lines: " + line;
                    Terminator.withMsgAndStatus(msg,-1);
                }
            }

            if (line.toUpperCase().equals(
                                GaussianConstants.STEPSEPARATOR.toUpperCase()))
            {
                GaussianStep step = new GaussianStep(linesOfAStep);
                addStep(step);
                linesOfAStep.clear();
            } else {
                linesOfAStep.add(line);
            }
        }

        //Deal with the last step that doesn't have a separator at the end
        GaussianStep step = new GaussianStep(linesOfAStep);
        addStep(step);

    }

//------------------------------------------------------------------------------

    /**
     * Add a single step to this Gaussian job. The new step is appended after
     * all previously existing steps.
     * @param step the new step to be added
     */

    public void addStep(GaussianStep step)
    {
        steps.add(step);
        numSteps++;
    }

//------------------------------------------------------------------------------

    /**
     * Get a specific step from the list of steps in this Gaussian Job
     * @param i the index of the step (0 to n-1)
     * @return the required step
     */

    public GaussianStep getStep(int i)
    {
        if (i > numSteps)
        {
            Terminator.withMsgAndStatus("ERROR! Trying to get step number " + i
               + " in a Gaussian job that has only " + numSteps + " steps.",-1);
        }
        return steps.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of steps of this Gaussian Job
     * @return the number of steps
     */

    public int getNumberOfSteps()
    {
        return numSteps;
    }

//------------------------------------------------------------------------------

    /**
     * Change the value of field <code>key</code> in all the Link 0 Sections
     * @param key is the key of the Link Section's field to be modified
     * @param value is the new value
     */

    public void setAllLinkSections(String key, String value)
    {
        for (int i=0; i<numSteps; i++)
        {
            getStep(i).getLinkCommand().setValue(key, value);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Change the charge in all steps
     * @param newCharge new value of the charge for all steps
     */

    public void setAllCharge(int newCharge)
    {
        for (int i=0; i<numSteps; i++)
        {
            getStep(i).getMolSpec().setCharge(newCharge);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Change spin multiplicity in all steps
     * @param newSpinMult new value of the spin multiplicity for all steps
     */

    public void setAllSpinMultiplicity(int newSpinMult)
    {
        for (int i=0; i<numSteps; i++)
        {
           getStep(i).getMolSpec().setSpinMultiplicity(newSpinMult);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Change comment in all steps
     * @param comment string to be used as comment in all steps
     */

    public void setAllComments(String comment)
    {
        for (int i=0; i<numSteps; i++)
        {
           getStep(i).setComment(comment);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of lines with all the information contained in this object
     * The format of each line of text corresponds to Gaussian03/09 input files.
     * @return the list of lines ready to print the input file for Gaussian
     */

    public ArrayList<String> toLinesInp()
    {
        ArrayList<String> lines = new ArrayList<String>();

        for (int step = 0; step<numSteps; step++)
        {
            //Write job-separator
            if (step != 0)
            {
                lines.add(GaussianConstants.STEPSEPARATOR);
            }

            lines.addAll(getStep(step).toLinesInp());
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of lines with all the information contained in this 
     * object.
     * The format corresponds to the format of autocompchem's JobDetail object and
     * can be used as input in the constructor of this object.
     * This method ignored molecular specification sections because they are
     * not part of the constructor of GaussianJob. 
     * @return the list of lines of text ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJob()
    {
        ArrayList<String> lines= new ArrayList<String>();
        for (int step = 0; step<numSteps; step++)
        {
            //Write job-separator
            if (step != 0)
            {
                lines.add(GaussianConstants.STEPSEPARATOR);
            }

            lines.addAll(getStep(step).toLinesJob());
        }
        return lines;
    }

//------------------------------------------------------------------------------

	public CompChemJob convertToCompChemJob() 
	{
		CompChemJob ccj = new CompChemJob();
		for (GaussianStep step : steps)
		{
			CompChemJob ccjStep = new CompChemJob();
			
			// Link0
			Directive lnk0Dir = new Directive(GaussianConstants.DIRECTIVELINK0);
			GaussianLinkCommandsSection lnk0 = step.getLinkCommand();
			for (String link0Key : lnk0.keySet())
			{
				Keyword k = new Keyword(link0Key, true, lnk0.getValue(link0Key));
				if (k.hasACCTask())
					k.removeValue();
				lnk0Dir.addKeyword(k);
			}
			ccjStep.setDirective(lnk0Dir);
			
			
			// Route
			Directive routeDir = new Directive(GaussianConstants.DIRECTIVEROUTE);
			GaussianRouteSection route = step.getRouteSection();
			int iFree = 0;
			for (String rKey : route.keySet())
			{
				String uKey = rKey.toUpperCase();
				if (uKey.startsWith(GaussianConstants.LABMUTEKEY))
				{
					routeDir.addKeyword(new Keyword(rKey.substring(3), false, 
							route.getValue(rKey)));
				} else if (uKey.startsWith(GaussianConstants.LABLOUDKEY))
				{
					if (uKey.contains("_$"))
					{
						String subDirName = rKey.substring(3, rKey.indexOf("_$"));
						String subkeyWLabel = rKey.substring(
								rKey.indexOf("_$") + 1);
	                    Directive outerDir = routeDir.getFirstDirective(subDirName);
	                    boolean existed = true;
	                    if (outerDir==null)
	                    {
	                    	existed = false;
	                    	outerDir = new Directive(subDirName);
	                    }
	                    
	                    if (subkeyWLabel.toUpperCase().startsWith(
	                    		GaussianConstants.LABLOUDKEY))
	                    {
	                    	Keyword k = new Keyword(subkeyWLabel.substring(3),
	                    			true, route.getValue(rKey));
	                    	if (k.hasACCTask())
	        					k.removeValue();
	                        outerDir.addKeyword(k);
	                    } 
	                    else if (subkeyWLabel.toUpperCase().startsWith(
	                    		GaussianConstants.LABMUTEKEY)) 
	                    {
	                    	Keyword k = new Keyword(subkeyWLabel.substring(3), 
	                        		false, route.getValue(rKey));
	                    	if (k.hasACCTask())
	        					k.removeValue();
	                        outerDir.addKeyword(k);
	                    }
	                    if (!existed)
	                    {
	                    	routeDir.addSubDirective(outerDir);
	                    }
					} else {
						Keyword k = new Keyword(rKey.substring(3), true, 
								route.getValue(rKey));
                    	if (k.hasACCTask())
        					k.removeValue();
						routeDir.addKeyword(k);
					}
				} else if (uKey.startsWith(GaussianConstants.LABFREE))
				{
					iFree++;
					routeDir.addKeyword(new Keyword("Free"+iFree, false, 
							route.getValue(rKey)));
				}
			}
			ccjStep.setDirective(routeDir);
			
			// Title
			Directive titleDir = new Directive(GaussianConstants.DIRECTIVETITLE);
			titleDir.addKeyword(new Keyword("title", false, step.getComment()));
			ccjStep.setDirective(titleDir);
			
			// MolSpec
			Directive molSpecDir = new Directive(GaussianConstants.DIRECTIVEMOLSPEC);
			GaussianMolSpecification molSpec = step.getMolSpec();
			molSpecDir.addKeyword(new Keyword(ChemSoftConstants.PARSPINMULT,
					false, molSpec.getSpinMultiplicity()+""));
			molSpecDir.addKeyword(new Keyword(ChemSoftConstants.PARCHARGE,
					false, molSpec.getCharge()+""));
			// WARNING: it is highly unlikely that we need to translate any other 
			// component of the molSpec data, but this is where it should be!
			ccjStep.setDirective(molSpecDir);
			
			//Opts
			Directive optDir = new Directive(GaussianConstants.DIRECTIVEOPTS);
			GaussianOptionsSection optSec = step.getOptionSection();
			for (String key : optSec.getRefNames())
			{
				String ddName = key.substring(3);
				if (ddName.toUpperCase().equals("BASIS"))
					ddName = "BASISSET";
				DirectiveData dd = new DirectiveData(ddName,
						new ArrayList<String>(Arrays.asList(
								optSec.getValue(key).split(
										System.getProperty("line.separator")))));
				if (dd.hasACCTask())
					dd.removeValue();
				optDir.addDirectiveData(dd);
			}
			ccjStep.setDirective(optDir);
			
			ccj.addStep(ccjStep);
		}
		return ccj;
	}

//------------------------------------------------------------------------------

}
