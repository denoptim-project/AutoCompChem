package autocompchem.wiro;

import autocompchem.run.Job;

/*   
 *   Copyright (C) 2014  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Storage of predefined constants related to
 * writing input and reading output (WIRO).
 *
 * @author Marco Foscato
 */

public class WIROConstants
{
	/**
	 * Name of data containing an identifier of a software, i.e., the
	 * computational chemistry software that created some output data.
	 */
	public static final String SOFTWAREID = "SOFTWAREID";
	
	/**
	 * Key for parameter defining the pathname to the file defining the details
	 * of the computational chemistry job.
	 */
	public static final String PARJOBDETAILSFILE = "JOBDETAILSFILE";

	/**
	 * Key for parameter defining the details
	 * of a job as a {@link Job} instance.
	 */
	public static final String PARJOBDETAILSOBJ = "JOBDETAILSOBJ";
	
	/**
	 * Key for parameter requesting to ignore the processing of input atom 
	 * containers.
	 */
	public static final String PARIGNOREINPUTIAC = "IGNOREINPUTIAC";

	/**
	 * Key for parameter defining the pathname's root for any output file (i.e.,
	 * the input for the computational chemistry software),
	 * i.e., a pathname without
	 * extension that can be used a root pathname for generating pathnames
	 * that are meant to be related. E.g., the string
	 * <code>dir/filename</code> is pathname's root of files such 
	 * <code>dir/filename.inp</code>, <code>dir/filename.log</code>,
	 * <code>dir/filename_tmp1.out</code>, etc.
	 */
	public static final String PAROUTFILEROOT = "ROOTPATHNAMEOUTPUT";

	/**
	 * Key for parameter defining the pathname of the main output file (i.e.,
	 * the input for the computational chemistry software).
	 */
	public static final String PAROUTFILE = "PATHNAMEOUTPUT";

	/**
	 * Key for parameter requiring to skip generation of the json file with 
	 * the definition of a mol/s-specific job.
	 */
	public static final String PARNOJSONOUTPUT = "NOJSONOUTPUT";
	/**
	 * Extension for job details file in JSON format
	 */
	public static final String JSONJDEXTENSION = ".jd.json";

	/**
	 * Key for parameter defining the pathname to an output from a comp.
	 * chem. job.
	 */
	public static final String PARJOBOUTPUTFILE = "JOBOUTPUTFILE";

	/**
	 * Name of data storing any data from a job. This is the entire
	 * data structure produced upon analyzing a job output.
	 */
	public static final String JOBOUTPUTDATA = "JOBOUTPUTDATA";

	/**
	 * Key for parameter requiring to terminate ACC with an error in case it
	 * detects abnormal termination in the output being analyzed by ACC.
	 */
	public static final String REQUIRENORMALTERM = "REQUIRENORMALTERMINATION";
}
