package autocompchem.modeling.forcefield;

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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.google.common.math.StatsAccumulator;

import autocompchem.run.Terminator;


/**
 * Utility to perform on the fly statistical analysis of force field parameters.
 *
 * @author Marco Foscato
 */

public class ForceFieldParameterStats
{
    /**
     * Collector of statistical accumulators per force field parameter
     */
    private Map<String,Map<String,ArrayList<StatsAccumulator>>> statsFFPars = 
                  new HashMap<String,Map<String,ArrayList<StatsAccumulator>>>();

    /**
     * Collector of reference force field parameters (i.e., the first imported
     * parameter for a given reference name)
     */
    private Map<String,ForceFieldParameter> refFFPars =
                                      new HashMap<String,ForceFieldParameter>();

    /**
     * Key for force constants
     */
    private static final String FKKEY = "f";

    /**
     * Key for equilibrium values
     */
    private static final String EQKEY = "e";


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ForceFieldParameterStats
     */

    public ForceFieldParameterStats() 
    {
    }

//------------------------------------------------------------------------------

    /**
     * Add all force field parameter from a list into the current statistics
     * @param ffParList the list of force field parameter to be added
     */

    public void addAllFFParams(ArrayList<ForceFieldParameter> ffParList)
    {
        for (ForceFieldParameter ffPar : ffParList)
        {
            addFFPar(ffPar);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add a force field parameter to the current statistics
     * @param ffPar the force field parameter to be added
     */

    public void addFFPar(ForceFieldParameter ffPar)
    {
        if (statsFFPars.containsKey(ffPar.getName()))
        {
            // check consistency with first imported
            if (!ffPar.isAnalogueTo(refFFPars.get(ffPar.getName())))
            {
                Terminator.withMsgAndStatus("ERROR! Attempt to compare two "
                        + "force field parameters that are not analogues: "
                        + System.getProperty("line.separator") + "NEW FFPar.: "
                        + System.getProperty("line.separator")
                        + ffPar.toString() 
                        + System.getProperty("line.separator") + "REF. FFPar.: "
                        + System.getProperty("line.separator")
                        + refFFPars.get(ffPar.getName()).toString(),-1);
            }

            // recover storage
            Map<String,ArrayList<StatsAccumulator>> innerMap = 
                                               statsFFPars.get(ffPar.getName());
 
            // force constants
            ArrayList<StatsAccumulator> saK = innerMap.get(FKKEY);
            ArrayList<ForceConstant> fkList = ffPar.getForceConstants();
            for (int i=0; i<fkList.size(); i++)
            {
                saK.get(i).add(fkList.get(i).getValue());
            }
            // equilibrium values
            ArrayList<StatsAccumulator> saE = innerMap.get(EQKEY);
            ArrayList<EquilibriumValue> eqList = ffPar.getEqValues();
            for (int i=0; i<eqList.size(); i++)
            {
                saE.get(i).add(eqList.get(i).getValue());
            }
        }
        else
        {
            // force constants
            ArrayList<StatsAccumulator> saK = new ArrayList<StatsAccumulator>();
            for (ForceConstant k : ffPar.getForceConstants())
            {
                StatsAccumulator sa = new StatsAccumulator();
                sa.add(k.getValue());
                saK.add(sa);
            }
            // equilibrium values
            ArrayList<StatsAccumulator> saE = new ArrayList<StatsAccumulator>();
            for (EquilibriumValue e : ffPar.getEqValues())
            {
                StatsAccumulator sa = new StatsAccumulator();
                sa.add(e.getValue());
                saE.add(sa);
            }
            // store new
            Map<String,ArrayList<StatsAccumulator>> innerMap = 
                              new HashMap<String,ArrayList<StatsAccumulator>>();
            innerMap.put(FKKEY,saK);
            innerMap.put(EQKEY,saE);
            statsFFPars.put(ffPar.getName(),innerMap);
            refFFPars.put(ffPar.getName(),ffPar);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Generate and return the set of mean force field parameters. 
     * @return the set of parameters obtained by calculating the mean force
     * constants and the mean equilibrium values among the imported
     * force field parameters
     */

    public Set<ForceFieldParameter> getMeanFFPar()
    {
        Set<ForceFieldParameter> lst = new HashSet<ForceFieldParameter>();
        for (String rName : statsFFPars.keySet())
        {
	    
            ForceFieldParameter mFFPar = new ForceFieldParameter();
	    try
	    {
		mFFPar = refFFPars.get(rName).deepCopy();
	    }
	    catch (Throwable t)
	    {
		t.printStackTrace();
		Terminator.withMsgAndStatus("ERROR! Failed deep-copy of force "
		  + "field parameter. Plese report this bug to the author.",-1);
	    }
            for (int i=0; i<mFFPar.getEqValues().size(); i++)
            {
                StatsAccumulator saK = statsFFPars.get(rName).get(FKKEY).get(i);
                mFFPar.getForceConstants().get(i).setValue(saK.mean());
            }
            for (int i=0; i<mFFPar.getEqValues().size(); i++)
            {
                StatsAccumulator saE = statsFFPars.get(rName).get(EQKEY).get(i);
                mFFPar.getEqValues().get(i).setValue(saE.mean());
            }
            lst.add(mFFPar);
        }
        return lst;
    }

//------------------------------------------------------------------------------

}
