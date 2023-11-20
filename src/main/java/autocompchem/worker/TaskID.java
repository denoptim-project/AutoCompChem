package autocompchem.worker;

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

import autocompchem.run.Terminator;

/**
 * Collection of all registered tasks.
 * 
 * @author Marco Foscato
 */
public enum TaskID 
{
	UNSET,
	DUMMYTASK,
	DUMMYTASK2,
    ADDBONDSFORSINGLEELEMENT,
    ADDDUMMYATOMS,           
    ALIGNATOMLISTS,          
    ANALYSISCHELATES,        
    ANALYZEVDWCLASHES,       
    ASSIGNATOMTYPES,         
    COMPARETWOCONNECTIVITIES,
    COMPARETWOGEOMETRIES,    
    COMPARETWOMOLECULES,     
    CONVERTZMATRIXTOSDF,
    CONVERTJOBDETAILS,
    // analyze data in the output/log
    ANALYSEOUTPUT,
    ANALYSEGAUSSIANOUTPUT, 
    ANALYSEORCAOUTPUT,
    ANALYSEXTBOUTPUT,
    ANALYSENWCHEMOUTPUT,
    // check for errors using also data taken from the analysis of the output
    EVALUATEGAUSSIANOUTPUT, //TODO-gg to to use generic EVALUATEJOB with detection of type
    EVALUATENWCHEMOUTPUT, //TODO-gg to to use generic EVALUATEJOB with detection of type
    EVALUATEGENERICOUTPUT, //TODO to del
    EVALUATEJOB, 
    // Analyze and detect problems, if any, try to solve
    CUREGAUSSIANJOB,
    CURENWCHEMJOB,
    //TODO-gg make agnostic CUREJOB task
    // TODO-gg minor tasks to be made agnostic
    EXTRACTGEOMETRIESFROMSPARTANTREE,   //TODO-gg remove: use general purpose job analysis
    EXTRACTLASTGEOMETRYFROMNWCHEMOUTPUT, //TODO-gg remove: use general purpose job analysis
    EXTRACTLASTGEOMETRYFROMSPARTANTREE, //TODO-gg remove: use general purpose job analysis
    EXTRACTOPTGEOMSFROMNWCHEMOUTPUT,    //TODO-gg remove: use general purpose job analysis
    EXTRACTTRAJECTORYFROMNWCHEMOUTPUT,  //TODO-gg remove: use general purpose job analysis
    EXTRACTVIBMODULEFORCECONSTANTS,     
    FIXANDRESTARTGAUSSIAN,   //TODO removve
    FIXANDRESTARTNWCHEM,  //TODO to CURENWCHEMJOB
    GENERATEATOMLABELS,
    GENERATEATOMTUPLES,
    GENERATEBASISSET,   
    GENERATECONSTRAINTS,
    GENERATECONFORMATIONALSPACE,
    MEASUREGEOMDESCRIPTORS,  
    MODIFYGEOMETRY,          
    MUTATEATOMS,             
    PARAMETRIZEFORCEFIELD,
    // Prepare the input file /files of third parties software package
    PREPAREINPUT,
    PREPAREINPUTGAUSSIAN,    
    PREPAREINPUTNWCHEM,
    PREPAREINPUTORCA,
    PREPAREINPUTXTB,        
    PREPAREINPUTSPARTAN,  
    //
    PRINTZMATRIX,            
    PRUNEMOLECULES,           
    REMOVEDUMMYATOMS,         
    REORDERATOMLIST,          
    RICALCULATECONNECTIVITY,  
    SORTSDFMOLECULES,         
    SUBTRACTZMATRICES, 
    IMPOSECONNECTIONTABLE,
    CHECKBONDLENGTHS;
	
//-----------------------------------------------------------------------------
	
	/**
	 * Converts a plain string into a TaskID, or stops with error message.
	 * @param task the string to be converted
	 * @return the TaskID corresponding to the given string or exits with an 
	 * error.
	 */
	
	public static TaskID getFromString(String task)
	{
    	TaskID taskID = TaskID.UNSET;
    	boolean found = false;
    	for (TaskID knownTaskID : TaskID.values())
    	{
    		if (knownTaskID.toString().toUpperCase().equals(task.toUpperCase()))
    		{
    			taskID = knownTaskID;
    			found = true;
    			break;
    		}
    	}
    	
    	if (!found)
    	{
    		 Terminator.withMsgAndStatus("ERROR! Task '" + task  + "'"
                     + " is not registered! Check your input.",-1);
    	}
    	
    	return taskID;
	}
	
//-----------------------------------------------------------------------------

}
