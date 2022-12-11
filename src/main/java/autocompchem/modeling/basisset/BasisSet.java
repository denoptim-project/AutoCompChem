package autocompchem.modeling.basisset;

import java.util.ArrayList;

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

import java.util.List;

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
    {}

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
     * Checks if this basis set contains an element-specific basis set.
     * @param elSymbol the elemental symbol (case insensitive).
     * @return <code>true</code> if this basis set contains for the specified
     * elemental symbol.
     */

    public boolean hasElement(String elSymbol)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex()==null 
            		&& cbs.getElement().toUpperCase().equals(
            				elSymbol.toUpperCase()))
            {
                return true;
            }
        }
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if this basis set contains a center with the given reference name
     * @param centerId the reference name of the center (i.e., atom)
     * @param elSymbol the elemental symbol (case insensitive).
     * @return <code>true</code> if this basis set contains a center with the 
     * given reference string.
     */

    public boolean hasCenter(int id, String elSymbol)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex()!=null && cbs.getCenterIndex()==id 
            		&& cbs.getElement().toUpperCase().equals(
            				elSymbol.toUpperCase()))
            {
                return true;
            }
        }
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the basis set for the specified element. If no center-specific
     * basis set is assigned to such element, then it creates a new one within
     * this object.
     * @param elSymbol the elemental symbol to search for.
     * @return the center-specific basis set associated with the given elemental
     * symbol. If none is found a new 
     * {@link autocompchem.modeling.basisset.CenterBasisSet} is created, added to
     * this basis set, and returned.
     */
    public CenterBasisSet getCenterBasisSetForElement(String elSymbol)
    {
    	for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getCenterIndex()==null && cbs.getElement().equals(elSymbol))
            {
                return cbs;
            }
        }
        CenterBasisSet newCbs = new CenterBasisSet(elSymbol);
        centerBSs.add(newCbs);
        return newCbs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the basis set for the specified center ID. If no center-specific
     * basis set is assigned to such center ID, then it creates a new one within
     * this object.
     * @param id the index of the center.
     * @param elSymb the elemental symbol of the center.
     * @return the center-specific basis set associated with the given center ID
     * ID. If there is no such center ID in this Basis set, then a new 
     * {@link autocompchem.modeling.basisset.CenterBasisSet} is created, added to
     * this basis set, and returned.
     */

    public CenterBasisSet getCenterBasisSetForCenter(int id, String elSymb)
    {
        for (CenterBasisSet cbs : centerBSs)
        {
            if (cbs.getElement().equals(elSymb) && cbs.getCenterIndex()!=null
            		&& cbs.getCenterIndex()==id)
            {
                return cbs;
            }
        }
        CenterBasisSet newCbs = new CenterBasisSet(id, elSymb);
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
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringBS(format));
                }
                break;

            case "NWCHEM":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringBS(format));
                }
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
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringECP(format));
                }
                break;

            case "NWCHEM":
                for (CenterBasisSet cbs : centerBSs)
                {
                    sb.append(cbs.toInputFileStringECP(format));
                }
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
