package autocompchem.workflow;

/*
 *   Copyright (C) 2017  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

/**
 * Interface for a job that may correspond to a sequence of steps.
 *
 * @author Marco Foscato
 */

public interface IJob
{

//------------------------------------------------------------------------------

    /**
     * Add a single step to this job. 
     * The new step is appended after
     * all previously existing steps.
     * @param step the new step to be added
     */

    public void addStep(IStep step);

//------------------------------------------------------------------------------

    /**
     * Remove a single step from this job.
     * @param step the step to be removed
     */

    public void removeStep(IStep step);

//------------------------------------------------------------------------------

    /**
     * Get a specific step from this job
     * @param i the index of the step (0 to n-1)
     * @return the required step
     */

    public IStep getStep(int i);

//------------------------------------------------------------------------------

    /**
     * Return the number of steps of this job
     * @return the number of steps
     */

    public int getNumberOfSteps();

//------------------------------------------------------------------------------

    /**
     * Produced the text representation of this job as the input file for a
     * given software/tool. 
     * @return the list of lines ready to print an input file
     */

    public ArrayList<String> toLinesInput();

//------------------------------------------------------------------------------

    /**
     * Produced a formatted text representation of this object according to
     * autocompchem's JobDetail text file representation.
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJobDetails();

//------------------------------------------------------------------------------

}
