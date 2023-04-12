package autocompchem.chemsoftware.tinker;

import java.io.File;

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

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.io.IOtools;
import autocompchem.molecule.connectivity.NearestNeighborMap;
import autocompchem.run.Terminator;

/**
 * Reader for Tinker XYZ files. This class is only a quick and dirty fix of
 * the lack of a proper reader.
 * 
 * @author Marco Foscato
 */

public class TinkerXYZReader
{

//------------------------------------------------------------------------------

    /**
     * Read a Tinker XYZ file and produces an IAtomContainer
     * @param file the file to read
     * @return the CDK representation of the molecule in the input file
     */

    public static IAtomContainer readTinkerXYZFIle(File file)
    {
        IAtomContainer iac = new AtomContainer();
        NearestNeighborMap cTab = new NearestNeighborMap();

        // Add title and atoms
        int natoms = 0;
        int i = 0;
        for (String line : IOtools.readTXT(file))
        {
            if (line.trim().equals(""))
            {
                continue;
            }
            if (i == 0)
            {
                String parts[] = line.trim().split("\\s+",2);
                natoms = Integer.parseInt(parts[0]);
                if (parts.length > 1)
                {
                    iac.setProperty(CDKConstants.TITLE,parts[1]);
                }
            }
            else
            {
                String parts[] = line.trim().split("\\s+");
                if (parts.length < 6)
                {
                    Terminator.withMsgAndStatus("ERROR! Not enough fields in "
                        + "file '" + file + "'.",-1);
                }
                int atmId = Integer.parseInt(parts[0]);
                String elSym = parts[1];
                Point3d p3d = new Point3d(Double.parseDouble(parts[2]),
                                          Double.parseDouble(parts[3]),
                                          Double.parseDouble(parts[4]));
                int atmTyp = Integer.parseInt(parts[5]);

                IAtom atm = new Atom(elSym,p3d);
                atm.setProperty("NUMFFTYPE",atmTyp);
                atm.setAtomTypeName(elSym);
                iac.addAtom(atm);

                if (parts.length > 6)
                {
                    ArrayList<Integer> nbrs = new ArrayList<Integer>();
                    for (int j=6; j<parts.length; j++)
                    {
                        nbrs.add(Integer.parseInt(parts[j]));
                    }
                    cTab.addNeighborningRelation(atmId-1,nbrs,true);
                }
            }
            i++;
        }

        // Add bonds
        for (int k=0; k<natoms; k++)
        {
            IAtom atmK = iac.getAtom(k);
            List<Integer> nbrIds = cTab.getNbrsId(k,true);
            for (Integer j : nbrIds)
            {
                IAtom atmJ = iac.getAtom(j);
                if (!iac.getConnectedAtomsList(atmK).contains(atmJ))
                {
                    iac.addBond(k,j,IBond.Order.valueOf("SINGLE"));
                }
            }
        }

        return iac;        
    }

//------------------------------------------------------------------------------

}
