package autocompchem.datacollections;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Storage of predefined constants related to parameters
 *
 * @author Marco Foscato
 */

public class ParameterConstants
{
    /**
     * Label defining a commented-out line
     */
    public final static String COMMENTLINE = "#";

    /**
     * Separator between key and value of a parameter
     */
    public final static String SEPARATOR = ":";

    /**
     * Label defining the beginning of a multiline block
     */
    public final static String STARTMULTILINE = "$START";

    /**
     * Label defining the end of a multiline block
     */
    public final static String ENDMULTILINE = "$END";

    /**
     * String defining the verbosity of a job
     */
    public final static String VERBOSITY = "VERBOSITY";

    /**
     * String defining the beginning of a job.
     */
    public final static String STARTJOB = "JOBSTART";

    /**
     * String defining the end of a job.
     */
    public final static String ENDJOB = "JOBEND";
    
	/**
	 * Keyword defining the application meant to do a job 
	 */
	public final static String RUNNABLEAPPIDKEY = "APP";

	/**
	 * Keyword of parameter requesting parallelization of sub jobs 
	 * and specifying the number of threads.
	 */
	public static final String PARALLELIZE = "PARALLELIZE";
	
	/**
	 * Keyword of parameter defining input files as source of information, i.e.,
	 * info channels. With 'input' we mean one or more input files of the job
	 * that is being evaluated.
	 */
	public static final String INFOSRCINPUTFILES = "IC-INPUT";
	
	/**
	 * Keyword of parameter defining output files as source of information, 
	 * i.e., info channels. With 'output' we mean one or more output file 
	 * produced by the job being evaluated.
	 */
	public static final String INFOSRCOUTPUTFILES = "IC-OUTPUT";
	
	/**
	 * Keyword of parameter defining log files as source of information, i.e.,
	 * info channels. With 'log file' we mean one or more log file produced by 
	 * the job being evaluated.
	 */
	public static final String INFOSRCLOGFILES = "IC-LOGS";
	
	/**
	 * Keyword of parameter defining a job details file as source of
	 * information, i.e., info channel. With 'job' we mean the job being 
	 * evaluated.
	 */
	public static final String INFOSRCJOBDETAILS = "IC-JOBDETAILS";
	
	/**
	 * Keyword defining the parameter containing the database of information
	 * channels.
	 */
	public static final String INFOCHANNELSDB = "INFOCHANNELSDB";

	/**
	 * Keyword defining the parameter containing the database of known 
	 * situations.
	 */
	public static final String SITUATIONSDB = "SITUATIONSDB";
	
	/**
	 * Keyword defining the pathname to the root folder that collects all known
	 * situations, i.e., the database of known situations.
	 */
	public static final String SITUATIONSDBROOT = "SITUATIONSDBROOT";

	/**
	 * Keyword of parameter defining where, i.e., in which file, to find the 
	 * definition of a job.
	 */
	public static final String JOBDEF = "JOBDEFINITION";

	/**
	 * Keyword defining the parameter containing the job to evaluate.
	 */
	public static final String JOBTOEVALUATE = "JOBTOEVALUATE";
	
	/**
	 * Keyword defining the parameter defining the container of the job to evaluate.
	 */
	public static final String JOBTOEVALPARENT = "JOBTOEVALPARENT";
	
	/**
	 * Keyword of parameter defining a situation
	 */
	public static final String SITUATION = "SITUATION";

	/**
	 * Placeholder for a string defined via command line argument. This
	 * placeholder string is replaced with the string given in CLI upon
	 * importing parameters from a file.
	 */
	public static final String STRINGFROMCLI = "STRINGFROMCLI";

	/**
	 * Keyword of parameter defining tolerance towards info channels that are
	 * defined by not readable.
	 */
	public static final String TOLERATEMISSINGIC = "TOLERATEMISSINGIC";
	
}
