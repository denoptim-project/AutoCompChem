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
import java.util.Locale;

import autocompchem.run.Terminator;


/**
 * Object representing an ensemble of shells used as basis set for a center 
 * (i.e., atom or pseudo-atom). The ensemble may be a portion of the entire 
 * basis set for the specific center. ECP may be included in this basis set.
 *
 * @author Marco Foscato
 */

public class CenterBasisSet
{
    /**
     * Center's (i.e., atom) reference name.
     */
    private String atmId = "noAtomId";
    
    /**
     * Center's (i.e., atom) index.
     */
    private Integer id;

    /**
     * Center's (i.e., atom) elemental symbol.
     */
    private String element;   
    
    /**
     * Basis set components by name. 
     * Names are independent from the list of shells and ECP components.
     */
    private List<String> namedComponents = new ArrayList<String>();

    /**
     * List of shells 
     */
    private List<Shell> shells = new ArrayList<Shell>();

    /**
     * List of ECP shells
     */
    private List<ECPShell> ecps = new ArrayList<ECPShell>();

    /**
     * Type of ECP
     */
    private String ecpType = "noECPType";

    /**
     * Maximum angular momentum for ECP potential
     */
    private int maxl = 0;

    /**
     * Number of electrons in ECP
     */
    private int ne = 0;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty CenterBasisSet that does not even specify which 
     * center it applies to.
     */
    public CenterBasisSet() 
    {}

//------------------------------------------------------------------------------
    
    /**
     * Constructor for a CenterBasisSet and define the elemental symbol of the
     * ceter. This is usually used for element-specific basis sets, so the
     * "center" is intended to be abstract.
     * @param atmId the index of the center.
     */

    public CenterBasisSet(String elSymb)
    {
        this.atmId = elSymb;
        this.element = elSymb;
    }
    
//------------------------------------------------------------------------------

	/**
     * Constructor for a CenterBasisSet and define the ID of the center
     * @param atmId the index of the center.
     * @param elSymb the elemental symbol.
     */

    public CenterBasisSet(int atmId, String elSymb)
    {
        this.atmId = elSymb+atmId;
        this.id = atmId;
        this.element = elSymb;
    }

//------------------------------------------------------------------------------

	/**
     * Add a named components, that is, a basis set that is widely recognized
     * by its name. Note that the name is not interpreted, but is treated as
     * a string.
     * @param compName the name of the component to be added
     */

    public void addNamedComponent(String compName)
    {
        namedComponents.add(compName);
    }

//------------------------------------------------------------------------------

    /**
     * Add a shell
     * @param s the shell to be added
     */

    public void addShell(Shell s)
    {
        shells.add(s);
    }

//------------------------------------------------------------------------------

    /**
     * Add an ECP shell
     * @param s the ECP shell to be added
     */

    public void addECPShell(ECPShell s)
    {
        ecps.add(s);
    }

//------------------------------------------------------------------------------

    /**
     * Takes the component of another CenterBasisSet and appends them to this
     * object. No checking for duplicate components is done. The appended 
     * objects are clones of the originals. Should <code>other</code> have 
     * ECP shells, all settings of the ECP (i.e., max. ang.
     * momentum, and number of core electons) are imported from 
     * <code>other</code>.
     * @param other the CenterBasisSet from which components ar copyed.
     */

    public void appendComponents(CenterBasisSet other)
    {
        // Append named basis set components
        for (String s : other.getNamedComponents())
        {
            namedComponents.add(s);
        }

        // Append explicit basis set component (i.e., shells)
        for (Shell shell : other.getShells())
        {
            shells.add(shell.clone());
        }

        // Append ECP components
        for (ECPShell ecp : other.getECPShells())
        {
            ecps.add(ecp.clone());
        }

        // Import ECP settings
        if (other.getECPShells().size() > 0)
        {
            this.setElement(other.getElement());
            this.setECPType(other.getECPType());
            this.setECPMaxAngMom(other.getECPMaxAngMom());
            this.setElectronsInECP(other.getElectronsInECP());
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the identifier of the centers this basis set applies to.
     */
    public String getAtmId() 
    {
		return atmId;
	}

//------------------------------------------------------------------------------
    
    /**
     * Sets the identifier of the centers this basis set applies to.
     * @param atmId the identifier of the centers this basis set applies to.
     */
	public void setAtmId(String atmId) 
	{
		this.atmId = atmId;
	}

//------------------------------------------------------------------------------

    /**
     * Set the elemental symbol
     * @param el the elemental symbol
     */
    public void setElement(String el)
    {
        this.element = el;
    }   

//------------------------------------------------------------------------------

    /**
     * Returns the elemental symbol
     * @return the elemental symbol
     */

    public String getElement()
    {
        return element;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the index of the center.
     * @return the index of the center or null if not defined.
     */

    public void setCenterIndex(int id)
    {
        this.id = id;
    } 
    
//------------------------------------------------------------------------------

    /**
     * Returns the index of the center.
     * @return the index of the center or null if not defined.
     */

    public Integer getCenterIndex()
    {
        return id;
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the type of the ECP
     * @param type the new type of ECP
     */

    public void setECPType(String type)
    {
        this.ecpType = type;
    }
    

//------------------------------------------------------------------------------

    /**
     * Returns the type of ECP
     * @return the type of ECP
     */

    public String getECPType()
    {
        return ecpType;
    }

//------------------------------------------------------------------------------

    /**
     * Set the maximum angular momentum for ECP 
     * @param maxl the maximum angular momentum
     */

    public void setECPMaxAngMom(int maxl)
    {
        this.maxl = maxl;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the maximum angular momentun for ECP
     * @return the maximum angular momentum in the ECP
     */

    public int getECPMaxAngMom()
    {
        return maxl;
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the number of core electrons replaced by the potential
     * @param ne the number of core electrons
     */

    public void setElectronsInECP(int ne)
    {
        this.ne = ne;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the number of core electrons replaced by the effective core 
     * potential
     * @return the number of core electrons includen in the ECP
     */

    public int getElectronsInECP()
    {
        return ne;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of named basis set components. Named components
     * have no explicit definition of the shells, but the reference name is
     * understood by quantum chemistry software packages.
     * @return the list of named components
     */

    public List<String> getNamedComponents()
    {
        return namedComponents;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of basis set components, i.e., the shells. 
     * @return the list of shells
     */

    public List<Shell> getShells()
    {
        return shells;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of ECP components.
     * @return the list of ECP shells
     */

    public List<ECPShell> getECPShells()
    {
        return ecps;
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o)
    {
    	if (!(o instanceof CenterBasisSet))
    		return false;
    	CenterBasisSet other = (CenterBasisSet) o;
    	
    	if (!this.atmId.equals(other.atmId))
    		return false;
    	if (this.id!=null && other.id!=null && this.id!=other.id)
    		return false;
    	if (this.element!=null && other.element!=null 
    			&& !this.element.equals(other.element))
    		return false;
    	if (!this.ecpType.equals(other.ecpType))
    		return false;
    	if (this.maxl!=other.maxl)
    		return false;
    	if (this.ne!=other.ne)
    		return false;
    	
    	if (this.namedComponents.size()!=other.namedComponents.size())
    		return false;
    	for (int i=0; i<this.namedComponents.size(); i++)
    	{
    		if (!this.namedComponents.get(i).equals(
    				other.namedComponents.get(i)))
    			return false;
    	}
    	
    	if (this.shells.size()!=other.shells.size())
    		return false;
    	for (int i=0; i<this.shells.size(); i++)
    	{
    		if (!this.shells.get(i).equals(other.shells.get(i)))
    			return false;
    	}

    	if (this.ecps.size()!=other.ecps.size())
    		return false;
    	for (int i=0; i<this.ecps.size(); i++)
    	{
    		if (!this.ecps.get(i).equals(other.ecps.get(i)))
    			return false;
    	}
    	return true;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string representation for deployment in the preparation of 
     * input files for a specific software package.
     * Known formats include: "Gaussian" and "NWChem".
     * @param format defined the software syntax to follow in the generation of
     * the string
     * @return a single string that contains all lines (newline characters). 
     * Note it contains a newline character also at the end.
     */

    public String toInputFileStringBS(String format)
    {
        StringBuilder sb = new StringBuilder();
        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                if (namedComponents.size() > 0)
                {
                    for (String n : namedComponents)
                    {
                        sb.append(String.format(Locale.ENGLISH,"%-6s 0", atmId))
                        	.append(nl);
                        sb.append(n).append(nl);
                        sb.append("****").append(nl);
                    } 
                }
                if (shells.size() > 0)
                {
                    sb.append(String.format(Locale.ENGLISH,"%-6s 0",atmId))
                    	.append(nl);
                    for (Shell s : shells)
                    {
                        sb.append(s.toInputFileString(format,"notUsed"));
                    }
                    sb.append("****").append(nl);
                }
                break;

            case "NWCHEM":
                String atmStr = "";
                if (id!=null)
                {
                	atmStr = Character.toUpperCase(element.charAt(0))
                        	+ element.toLowerCase().substring(1) + (id+1);
                } else {
                	atmStr = Character.toUpperCase(atmId.charAt(0))
                	+ atmId.toLowerCase().substring(1);
                }
                boolean first = true;
                for (String n : namedComponents)
                {
                	if (first)
                		first = false;
                	else
                		sb.append(nl);
                	
                    if (n.contains(" "))
                    {
                        sb.append(String.format(Locale.ENGLISH,
                        		"  %s library \"%s\"", atmStr, n));
                    }
                    else
                    {
                        sb.append(String.format(Locale.ENGLISH, 
                        		"  %s library %s", atmStr, n));
                    }
                }
                first = true;
                for (Shell s : shells)
                {
                	if (first)
                		first = false;
                	else
                		sb.append(nl);
                    sb.append(s.toInputFileString(format, atmStr));
                }
                break;

            default:
                String msg = "ERROR! Format '" + format + "' is not a known "
                         + "format for reporting basis sets in CenterBasisSet. "
                         + "Check your input.";
                Terminator.withMsgAndStatus(msg,-1);
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------


    /**
     * Returns a string representation of the effective core potential 
     * for deployment in the preparation of
     * input files for a specific software package.
     * Known formats include: "Gaussian".
     * @param format defined the software syntax to follow in the generation of
     * the string
     * @return a single string that contains all lines (newline charactrs)
     */

    public String toInputFileStringECP(String format)
    {
        StringBuilder sb = new StringBuilder();
        if (ecps.size() == 0)
        {
            return "";
        }
        String nl = System.getProperty("line.separator");
        switch (format.toUpperCase())
        {
            case "GAUSSIAN":
                sb.append(String.format(Locale.ENGLISH,
                		"%-6s 0",atmId)).append(nl);
                sb.append(String.format(Locale.ENGLISH,
                		"%s %2d %3d",ecpType,maxl,ne));
                sb.append(nl);
                for (ECPShell s : ecps)
                {
                    sb.append(s.toInputFileString(format));
                }
                break;

            case "NWCHEM":
                String atmStr = "";
                if (id!=null)
                {
                	atmStr = Character.toUpperCase(element.charAt(0))
                        	+ element.toLowerCase().substring(1) + (id+1);
                } else {
                	atmStr = Character.toUpperCase(atmId.charAt(0))
                	+ atmId.toLowerCase().substring(1);
                }
                sb.append(String.format(Locale.ENGLISH,
                		"  %s nelec %s",atmStr,ne)).append(nl);
                boolean first = true;
                for (ECPShell s : ecps)
                {
                    String ecpsType = s.getType();
                    if (first && ecpsType.substring(0,1).toUpperCase().equals(
                                BasisSetConstants.ANGMOMINTTOSTR.get(maxl)))
                    {
                        ecpsType = "ul";
                    } else {
                        String[] parts = ecpsType.split("-");
                        ecpsType = parts[0]; 
                    }
                    if (first)
                    	first = false;
                    else
                        sb.append(nl);
                    sb.append(String.format(Locale.ENGLISH, "  %s %s", atmStr, 
                    		ecpsType));
                    sb.append(s.toInputFileString(format));
                }
                break;

            default:
                String msg = "ERROR! Format '" + format + "' is not a known "
                             + "format for reporting ECPs in CenterBasisSet. "
                             + "Check your input.";
                Terminator.withMsgAndStatus(msg,-1);
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
