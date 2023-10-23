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
     * The tag assigned to a center/atom. It may or may not be related with 
     * the elements and/or the index of the center.
     */
    private String tag;
    
    /**
     * Center's (i.e., atom) index (0-based).
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
     * Constructor for a CenterBasisSet and define the ID of the center.
     * @param centerTag the tag identifying the center/s the basis set is meant 
     * for. Give null to signify that no tag is specified for the center.
     * @param idx the index of the center (0-based).
     * @param elSymb the elemental symbol.
     */

    public CenterBasisSet(String centerTag, Integer idx, String elSymb)
    {
        this.tag = centerTag;
        if (idx!=null)
        	this.id = Integer.valueOf(idx.toString());
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
     * @return the identifier of the centers this basis set applies to, or
     * null if such tag has not been defined.
     */
    public String getCenterTag() 
    {
		return tag;
	}

//------------------------------------------------------------------------------
    
    /**
     * Sets the identifier of the centers this basis set applies to.
     * @param tag the identifier of the centers this basis set applies to.
     */
	public void setCenterTag(String tag) 
	{
		this.tag = tag;
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
     * Sets the index of the center (0-based).
     */

    public void setCenterIndex(int id)
    {
        this.id = id;
    } 
    
//------------------------------------------------------------------------------

    /**
     * Returns the index of the center (0-based).
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
    	
    	if ((this.tag==null && other.tag!=null)
    			|| (this.tag!=null && other.tag==null))
    		return false;
    	if (this.tag!=null && other.tag!=null && !this.tag.equals(other.tag))
    		return false;
    	
    	if ((this.id==null && other.id!=null)
    			|| (this.id!=null && other.id==null))
    		return false;
    	if (this.id!=null && other.id!=null && this.id!=other.id)
    		return false;

    	if ((this.element==null && other.element!=null)
    			|| (this.element!=null && other.element==null))
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

}
