package autocompchem.modeling.basisset;

/*   
 *   Copyright (C) 2016  Marco Foscato 
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

import java.util.List;
import java.util.ArrayList;

import autocompchem.run.Terminator;


/**
 * Object representing a basis set as a combination of center-specific basis 
 * sets.
 *
 * @author Marco Foscato
 */

public class BasisSet
{
    /**
     * List center-specific basis sets
     */
    public List<CenterBasisSet> centerBSs = new ArrayList<CenterBasisSet>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty object
     */

    public BasisSet()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Add a new center-specific basis set
     * @param cbs the center-specific basis set to append
     */

    public void addCenterSpecBSet(CenterBasisSet cbs)
    {
	centerBSs.add(cbs);
    }

//------------------------------------------------------------------------------

    /**
     * Checks if this bais set contains a center with the given reference name
     * @param centerId the reference name of the center (i.e., atom)
     * @return <code>true</code> if this basis set containg a center with the 
     * given reference string
     */

    public boolean hasCenter(String centerId)
    {
	for (CenterBasisSet cbs : centerBSs)
	{
	    if (cbs.getCenterId().equals(centerId))
	    {
		return true;
	    }
	}
	return false;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the basis set fo the specified center ID. If no center-specific
     * basis set is assigned to such center ID, then it creates a new one within
     * this basis set object.
     * @param centerId the ID of the center
     * @return the center-specific basis set associated with the given center ID
     * ID. If there is no such center ID in this BAsis set, then a new 
     * {@link autocompchem.modeling.basisset.CenterBasisSet} is created, added to
     * this basis set, and returned.
     */

    public CenterBasisSet getCenterBasisSetForCenter(String centerId)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterId().equals(centerId))
            {
                return cbs;
            }
        }
	CenterBasisSet newCbs = new CenterBasisSet(centerId);
	centerBSs.add(newCbs);
	return newCbs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the complete list of <code>CenterBasisSet</code>s
     * @return the list of center-specific basis set
     */

    public List<CenterBasisSet> getAllCenterBSs()
    {
	return centerBSs;
    }

//------------------------------------------------------------------------------

    /** 
     * Checks whether there is any ECP in this basis set
     * @return <code>true</code> if any of the centers has an ECP
     */

    public boolean hasECP()
    {
	boolean res = false;
	for (CenterBasisSet cbs : centerBSs)
	{
	    if (cbs.getECPShells().size() > 0)
	    {
		res = true;
		break;
	    }
	}
	return res;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string representation of this basis set that is limited to its 
     * basis functions section, i.e., exclude effective core potentials.
     * Known formats: "Gaussian", "NWChem".
     * @param format defined the software syntax to follow in the generation of
     * the string
     * @return a single string that contains all lines (with newline characters)
     */

    public String toInputFileStringBS(String format)
    {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringBS(format));
                }
		sb.append(nl);
                break;

            case "NWCHEM":
		sb.append("BASIS \"ao basis\" print").append(nl);
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringBS(format));
                }
		sb.append("END").append(nl);
                break;

            default:
                String msg = "ERROR! Format '" + format + "' is not a known "
                             + "format for reporting basis sets (in BasisSet-BS"
			     + "). Check your input.";
                Terminator.withMsgAndStatus(msg,-1);
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string representation of this basis set that is limited to its 
     * effective core potentials section, i.e., no basis functions section.
     * Known formats: "Gaussian", "NWChem".
     * @param format defined the software syntax to follow in the generation of
     * the string
     * @return a single string that contains all lines (with newline characters)
     */

    public String toInputFileStringECP(String format)
    {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringECP(format));
                }
                sb.append(nl);
                break;

            case "NWCHEM":
                sb.append("ECP").append(nl);
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringECP(format));
                }
                sb.append("END").append(nl);
                break;

            default:
                String msg = "ERROR! Format '" + format + "' is not a known "
                             + "format for reporting basis sets (in BasisSet-"
			     + "ECP). Check your input.";
                Terminator.withMsgAndStatus(msg,-1);
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string representation for deployment in the preparation of 
     * input files for a specific software package.
     * Known formats: "Gaussian", "NWChem".
     * @param format defined the software syntax to follow in the generation of
     * the string
     * @return a single string that contains all lines (newline charactrs)
     */

    public String toInputFileString(String format)
    {
        StringBuilder sb = new StringBuilder();
	sb.append(toInputFileStringBS(format));
//        sb.append(System.getProperty("line.separator"));
	sb.append(toInputFileStringECP(format));
/*
//KEEP: we might need this if we encounter a format that does not split the bs and ECP sections.

        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringBS(format));
                }
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringECP(format));
                }
		sb.append(nl);
                break;

            case "NWCHEM":
		sb.append("BASIS \"ao basis\" print").append(nl);
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringBS(format));
                }
		sb.append("END").append(nl);
                sb.append(nl);
		sb.append("ECP").append(nl);
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringECP(format));
                }
		sb.append("END").append(nl);
                break;

            default:
                String msg = "ERROR! Format '" + format + "' is not a known "
                             + "format for reporting basis sets (in BasisSet). "
			     + "Check your input.";
                Terminator.withMsgAndStatus(msg,-1);
        }
*/
        return sb.toString();
    }

//------------------------------------------------------------------------------

}