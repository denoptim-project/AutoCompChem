package autocompchem.chemsoftware;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import autocompchem.datacollections.ParameterStorage;

/**
 * A class representing the request of a kind of analysis and some settings
 * associated with the analysis task.
 * 
 * @author Marco Foscato
 */
public class AnalysisTask 
{
    
	/**
	 * The kind of analysis to perform
	 */
	private AnalysisKind kind;
	
    /**
     * Possible kinds of analysis that can be asked for jobs/steps
     */
    public enum AnalysisKind {
    	SCFENERGY,
    	LASTGEOMETRY,
    	CRITICALPOINTKIND,
    	VIBMODE,
    	QHTHERMOCHEMISTRY, 
    	ALLGEOM,
    	BLVSCONNECTIVITY}
    
    /**
     * Settings that control how the analysis is performed
     */
    private ParameterStorage params = new ParameterStorage();
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor 
     * @param kind the kind of analysis to perform
     */
    public AnalysisTask(AnalysisKind kind)
    {
    	this.kind = kind;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor 
     * @param kind the kind of analysis to perform
     * @param params settings for the analysis
     */
    public AnalysisTask(AnalysisKind kind, ParameterStorage params)
    {
    	this.kind = kind;
    	this.params = params;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Set the parameters associated with this analysis task.
     * @param params settings for the analysis
     */
    public void setParams(ParameterStorage params)
    {
    	this.params = params;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return the kind of analysis represented by this object
     */
    public AnalysisKind getKind()
    {
    	return kind;
    }
     
//-----------------------------------------------------------------------------
    
    /**
     * Returns the parameters meant to control the analysis
     * @return the parameters meant to control the analysis
     */
    public ParameterStorage getParams()
    {
    	return params;
    }
    
//-----------------------------------------------------------------------------

    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("[AnalysisTask: ").append(kind).append(" ");
    	sb.append(params.toLinesJobDetails()).append("]");
    	return sb.toString();
    }
    
//-----------------------------------------------------------------------------
    
}
