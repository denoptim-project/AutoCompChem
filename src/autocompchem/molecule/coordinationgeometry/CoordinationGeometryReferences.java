package autocompchem.molecule.coordinationgeometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.Map;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;

import autocompchem.run.Terminator;

/**
 *   Collection of reference coordination geometries 
 */

public class CoordinationGeometryReferences
{

    private static volatile boolean isInitialized = false;
    private static volatile List<CoordinationGeometry> allCG;
    private static volatile Map<String, CoordinationGeometry> cgByName;


//------------------------------------------------------------------------------

    public CoordinationGeometryReferences()
    {
	if (isInitialized) return;

	allCG = new ArrayList<CoordinationGeometry>();
	cgByName = new HashMap<String, CoordinationGeometry>();

	String file = "autocompchem/molecule/coordinationgeometry/referenceCG.txt";
	InputStream ins = CoordinationGeometryReferences.class.getClassLoader().
			  			      getResourceAsStream(file);
	BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
	String line;
	try {
        while ((line = reader.readLine()) != null) 
	{
	    if (line.equals("") || line == null)
		continue;

	    if (line.startsWith("#"))
		continue;

	    String[] parts = line.split("\\s+");

	    //First field if the name of the standard geometry
	    String cgName = parts[0];
	    //Other fields atom coordinates
	    IAtom centralAtm = new Atom();
	    List<IAtom> ligands = new ArrayList<IAtom>();
	    int idCnt = 1; //this defines which field represents the center
	    for (int i=1; i<parts.length; i++)
	    {
		String[] partsOfAtm = parts[i].split(",");
		if (partsOfAtm.length != 4)
		{
		    Terminator.withMsgAndStatus("ERROR! Check reference "
			+ "geometry " + cgName + " in file " + file
			+ ". Wrong number of fields for atom " + i, -1);
		}
		String atmSymbol = partsOfAtm[0];
		double xCoord = Double.parseDouble(partsOfAtm[1]);
                double yCoord = Double.parseDouble(partsOfAtm[2]);
                double zCoord = Double.parseDouble(partsOfAtm[3]);

		Point3d p3d = new Point3d(xCoord,yCoord,zCoord);
		IAtom atm = new Atom(atmSymbol, p3d);
		//First of list is the central atom
		if (i == idCnt)
		{
		    centralAtm = atm;
		} else {
		    ligands.add(atm);
		}
	    }
	    CoordinationGeometry cg = new CoordinationGeometry(cgName,
							       centralAtm,
							       ligands);
            allCG.add(cg);
	    cgByName.put(cg.getName(), cg);
	}
	} catch (IOException e) {
	    e.printStackTrace();
	    Terminator.withMsgAndStatus("ERROR! Unable to read reference "
		+ "coordination geometries", -1);
	}

	isInitialized = true;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a given reference <code>CoordinationGeometry</code>
     * @param name the name of the required <code>CoordinationGeometry</code>
     * @return the geometry corresponding to the given name
     */

    public static CoordinationGeometry getReferenceGeometryByName(String name)
    {
	return cgByName.get(name);
    }

//------------------------------------------------------------------------------

    /**
     * Returns all the <code>CoordinationGeometry</code> that represent centers
     * with the given connection number
     * @param cn the required connection number 
     * @return the list of geometries available for the given coordinatio number
     */

    public static List<CoordinationGeometry> getReferenceGeometryForCN(int cn)
    {
	ArrayList<CoordinationGeometry> res = 
					  new ArrayList<CoordinationGeometry>();
	for (CoordinationGeometry cg : allCG)
	{
	    if (cg.getConnectionNumber() == cn)
		res.add(cg);
	}
        return res;
    }

//------------------------------------------------------------------------------
        
    /**
     * Returns all the reference <code>CoordinationGeometry</code>
     * @return the list with all reference geometries
     */ 
            
    public static List<CoordinationGeometry> getAllReferenceGeometries()
    {
        return allCG;
    }

//------------------------------------------------------------------------------

}
