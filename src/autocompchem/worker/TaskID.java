package autocompchem.worker;

import autocompchem.run.Terminator;

/**
 * Collection of all registered workers.
 * 
 * @author Marco Foscato
 */
public enum TaskID 
{
	UNSET,
	DummyTask,
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
    EVALUATEGAUSSIANOUTPUT,  //TODO to analysis
    EVALUATEGENERICOUTPUT, //TODO to del
    EVALUATEJOB, 
    EVALUATENWCHEMOUTPUT, //TODO to analysis
    ANALYSEORCAOUTPUT,
    EXTRACTGEOMETRIESFROMSPARTANTREE,   
    EXTRACTLASTGEOMETRYFROMNWCHEMOUTPUT,
    EXTRACTLASTGEOMETRYFROMSPARTANTREE, 
    EXTRACTOPTGEOMSFROMNWCHEMOUTPUT,    
    EXTRACTTRAJECTORYFROMNWCHEMOUTPUT,  
    EXTRACTVIBMODULEFORCECONSTANTS,     
    FIXANDRESTARTGAUSSIAN,   
    FIXANDRESTARTNWCHEM,     
    GENERATEBASISSET,   
    GENERATECONSTRAINTS,
    MEASUREGEOMDESCRIPTORS,  
    MODIFYGEOMETRY,          
    MUTATEATOMS,             
    PARAMETRIZEFORCEFIELD,
    PREPAREINPUTGAUSSIAN,    
    PREPAREINPUTNWCHEM,
    PREPAREINPUTORCA,
    PREPAREINPUTQMMM,        
    PREPAREINPUTSPARTAN,     
    PRINTZMATRIX,            
    PRUNEMOLECULES,           
    REMOVEDUMMYATOMS,         
    REORDERATOMLIST,          
    RICALCULATECONNECTIVITY,  
    SORTSDFMOLECULES,         
    SUBTRACTZMATRICES, 
    IMPOSECONNECTIONTABLE,
    FINDATOMSETS;   //TODO check: is it implemented?     
	
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
