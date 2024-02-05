package autocompchem.worker;

import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;

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
 * Collection of all registered tasks. Each one may or may not have a dedicated 
 * {@link Worker} that declares its capability to perform such task.
 * 
 * <p><b>WARNING:</b> although {@link TaskID}s can be created from string,
 * you should always check for equality using their 
 * <code>.equals()</code> method, not by string representation as the latter
 * is altered to remove spaces and underscores, so you may not be comparing
 * the strings you think to be comparing.</p>
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
    // generate atom-specific information
    ADDATOMSPECIFICKEYWORDS,
    
    /**
	 * Task about adding a filename that is dependent 
	 * on the specifics of the molecular system calculated. This string can be 
	 * followed (space separated by a string to be appended to the pathname 
	 * root.
	 */
    ADDFILENAME,
    
	/**
	 * Task about reporting a geometry. Defined where and how and what geometry 
	 * (i.e., what actual chemical system to consider). This is used inside
	 * the {@link DirectiveData} object that will later contain the actual 
	 * coordinates.
	 * This string can be followed (space separated a specification of the
	 * {@link CoordsType}.
	 */
    ADDGEOMETRY,
    
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
	 * Converts a plain string into a {@link TaskID} even if the plain string
	 * contains spaces of "_" characters (NB: spaces and "_" are removed from 
	 * the string), or stops with error message.
	 * @param task the string to be converted. May contain spaces or "_".
	 * @return the TaskID corresponding to the given string or exits with an 
	 * error.
	 */
	
	public static TaskID getFromString(String task)
	{
    	TaskID taskID = TaskID.UNSET;
    	boolean found = false;
    	task = task.trim().replaceAll("\\s+", "").replaceAll("_", "");
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
