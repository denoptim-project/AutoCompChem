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

import autocompchem.parameters.Parameter;

/**
 * Interface for a job's step characterized by a set of parameters, a status.
 *
 * @author Marco Foscato
 */

public interface IStep
{

//------------------------------------------------------------------------------

    /**
     * Add a single parameter to this step.
     * @param p the parameter to be added
     */

    public void addParameter(Parameter p);

//------------------------------------------------------------------------------

    /**
     * Checks if this step contains given parameter.
     * @param parKey the parameter to be searched
     */

    public boolean containsParameter(String parKey);

//------------------------------------------------------------------------------

    /**
     * Get a specific parameter from this step
     * @param key the parameter identifier
     * @return the required parameter
     */

    public Parameter getParameter(String key);

//------------------------------------------------------------------------------

    /**
     * Produced the text representation of this step as in an input file for a
     * given software/tool. 
     * @return the list of lines ready to print an input file
     */

    public ArrayList<String> toLinesInput();

//------------------------------------------------------------------------------

    /**
     * Produced a formatted text representation of this step according to
     * autocompchem's JobDetail text file representation.
     * @return the list of lines ready to print a jobDetails file
     */

    public ArrayList<String> toLinesJobDetails();

//------------------------------------------------------------------------------

}
