package autocompchem.molecule.intcoords.zmatrix;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;


/**
 * Object representing a single center (i.e., an atom) of which position is 
 * defined by means of internal coordinates. 
 * This object is a single line in a {@link ZMatrix}.
 *
 * @author Marco Foscato
 */ 

public class ZMatrixAtom implements Cloneable
{
    /**
     * Center name (i.e., atom symbol)
     */
    protected String name = "noname";

    /**
     * Index of the first reference center (i.e., directly bonded atom)
     */
    protected int idI = -1;

    /**
     * Index of the second reference center (i.e., used to define an angle)
     */
    protected int idJ = -1;

    /**
     * Index of the third reference center (i.e., used for torsion or angle)
     */
    protected int idK = -1;

    /**
     * First internal coordinate: always a distance
     */
    protected InternalCoord icI;

    /**
     * Second internal coordinate: always a bond angle
     */
    protected InternalCoord icJ;

    /**
     * Third internal coordinate: either a bond angle or a torsion angle
     */
    protected InternalCoord icK; 
    
    /**
     * Storage of properties for this center
     */
    private Map<Object, Object> properties = new HashMap<Object, Object>();


//------------------------------------------------------------------------------

    /**
     * Construct an empty ZMatrixAtom
     */

    public ZMatrixAtom()
    {}

//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixAtom from sting. If the string contains no spaces,
     * we defining only the name of the current the center. Otherwise,
     * we parse the internal coordinates.
     * @param string the string to parse
     * @param id a unique id for this ZMatrixAtom (i.e., position in list).
     * This is used to define the reference
     * names for the internal coordinates included in this line.
     * @param zeroBasedIds set to <code>true</code> to read zero-based atom IDs.
     * By default we expect 1-based atom IDs.
     */

    public ZMatrixAtom(String string, int id, boolean zeroBasedIds)
    {
        int phase = 1;
        if (zeroBasedIds)
        {
            phase = 0;
        }
        String[] parts = string.trim().split("\\s+");
        switch (parts.length)
        {
            case 1:
                this.name = parts[0];
                break;

            case 3:
                this.name = parts[0];
                this.idI = Integer.parseInt(parts[1]) - phase;
                buildIcI(parts[2],id);
                break;

            case 4:
                break;

            case 5:
                this.name = parts[0];
                this.idI = Integer.parseInt(parts[1]) - phase;
                this.idJ = Integer.parseInt(parts[3]) - phase;
                buildIcI(parts[2],id);
                buildIcJ(parts[4],id);
                break;

            case 7:
                this.name = parts[0];
                this.idI = Integer.parseInt(parts[1]) - phase;
                this.idJ = Integer.parseInt(parts[3]) - phase;
                this.idK = Integer.parseInt(parts[5]) - phase;
                buildIcI(parts[2],id);
                buildIcJ(parts[4],id);
                buildIcK(parts[6],id,"notype");
                break;

            case 8:
                this.name = parts[0];
                this.idI = Integer.parseInt(parts[1]) - phase;
                this.idJ = Integer.parseInt(parts[3]) - phase;
                this.idK = Integer.parseInt(parts[5]) - phase;
                buildIcI(parts[2],id);
                buildIcJ(parts[4],id);
                buildIcK(parts[6],id,parts[7]);
                break;

            default:
                Terminator.withMsgAndStatus("Cannot construct a ZMatrixAtom "
                        + "from string '" + string + "'.",-1);
                break;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Build the first internal coordinate.
     */

    private void buildIcI(String s, int id)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ids.add(id);
        ids.add(idI);
        if (NumberUtils.isNumber(s))
        {
            this.icI = new InternalCoord(id+"_ic_1",Double.parseDouble(s),ids);
        }
        else
        {
            this.icI = new InternalCoord(s,0.0,ids);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Build the second internal coordinate.
     */

    private void buildIcJ(String s, int id)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ids.add(id);
        ids.add(idI);
        ids.add(idJ);
        if (NumberUtils.isNumber(s))
        {
            this.icJ = new InternalCoord(id+"_ic_2",Double.parseDouble(s),ids);
        }
        else
        {
            this.icJ = new InternalCoord(s,0.0,ids);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Build the third internal coordinate.
     */

    private void buildIcK(String s, int id, String type)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ids.add(id);
        ids.add(idI);
        ids.add(idJ);
        ids.add(idK);
        if (NumberUtils.isNumber(s))
        {
            if ("notype" == type)
            {
                this.icK = new InternalCoord(id+"_ic_3",Double.parseDouble(s),
                                                                           ids);

            }
            else
            {
                this.icK = new InternalCoord(id+"_ic_3",Double.parseDouble(s),
                                                                      ids,type);
            }
        }
        else
        {
            if ("notype" == type)
            {
                this.icK = new InternalCoord(s,0.0,ids);
            }
            else
            {
                this.icK = new InternalCoord(s,0.0,ids,type);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixAtom defining only the name of the current the center.
     * @param name the name of the ZMatrixAtom to create
     */

    public ZMatrixAtom(String name)
    {
        this.name = name;
    }

//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixAtom defining only the name of the current the center 
     * and the first internal coordinate
     * @param name the name of the ZMatrixAtom to create
     * @param idI the index of the previously defined ZMatrixAtom used to define
     * the first internal coordinate of this ZMatrixAtom
     * @param icI the first internal coordinate wich defines the position of
     * this ZMatrixAtom with respect to the ZMatrixAtom with id=idI
     */

    public ZMatrixAtom(String name, int idI, InternalCoord icI)
    {
        this.name = name;
        this.idI = idI;
        this.icI = icI;
    }

//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixAtom defining only the name of the current the center
     * the first two internal coordinates
     * @param name the name of the ZMatrixAtom to create
     * @param idI the index of the previously defined ZMatrixAtom used to define
     * the first internal coordinate of this ZMatrixAtom
     * @param idJ the index of the previously defined ZMatrixAtom used to define
     * the second internal coordinate of this ZMatrixAtom
     * @param icI the first internal coordinate which defines the position of
     * this ZMatrixAtom with respect to the ZMatrixAtom with id=idI
     * @param icJ the second internal coordinate which defines the position of
     * this ZMatrixAtom with respect to the ZMatrixAtoms idI and idJ
     */

    public ZMatrixAtom(String name, int idI, int idJ, InternalCoord icI, 
                                                               InternalCoord icJ)
    {
        this.name = name;
        this.idI = idI;
        this.idJ = idJ;
        this.icI = icI;
        this.icJ = icJ;
    }

//------------------------------------------------------------------------------

    /**
     * Construct a ZMatrixAtom defining all its internal coordinates
     * @param name the name of the ZMatrixAtom to create
     * @param idI the index of the previously defined ZMatrixAtom used to define
     * the first internal coordinate of this ZMatrixAtom
     * @param idJ the index of the previously defined ZMatrixAtom used to define
     * the second internal coordinate of this ZMatrixAtom
     * @param idK the index of the previously defined ZMatrixAtom used to define
     * the third internal coordinate of this ZMatrixAtom
     * @param icI the first internal coordinate which defines the position of
     * this ZMatrixAtom with respect to the ZMatrixAtom with id=idI
     * @param icJ the second internal coordinate which defines the position of
     * this ZMatrixAtom with respect to the ZMatrixAtoms idI and idA
     * @param icK the third internal coordinate which defines the position of
     * this ZMatrixAtom
     */

    public ZMatrixAtom(String name, int idI, int idJ, int idK,
                        InternalCoord icI, InternalCoord icJ, InternalCoord icK)
    {
        this.name = name;
        this.idI = idI;
        this.idJ = idJ;
        this.idK = idK;
        this.icI = icI;
        this.icJ = icJ;
        this.icK = icK;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the name of the center (i.e., atom) that is represented in this 
     * object.
     * @return the name of the center/atom
     */

    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the index of a selected reference center (i.e., reference atom).
     * @param l the index of the reference: 0, 1, or 2 respectively for the
     * first, second, and third reference atom
     * @return the index, which is -1 if not set.
     */

    public int getIdRef(int l)
    {
        int id = -1;
        switch (l)
        {
            case (0):
                id = idI;
                break;

            case (1):
                id = idJ;
                break;

            case (2):
                id = idK;
                break;
        }
        return id;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a selected internal coordinate.
     * @param l the index of the internal coordinate: 0, 1, or 2 
     * respectively for the first, second, and third coordinate.
     * @return the internal coordinate or null
     */

    public InternalCoord getIC(int l)
    {
        InternalCoord ic = null;
        switch (l)
        {
            case (0):
                ic = icI;
                break;

            case (1):
                ic = icJ;
                break;

            case (2):
                ic = icK;
                break;
        }
        return ic;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if any of the internal coordinates of this atom
     * is flagged to be a constant.
     */
  	public boolean hasConstantIC() 
  	{
  		if (icI!=null)
  		{
  			if (icI.isConstant())
  				return true;
  			if (icJ!=null)
  	  		{
  	  			if (icJ.isConstant())
  	  				return true;
	  	  		if (icK!=null)
	  	  		{
	  	  			if (icK.isConstant())
	  	  				return true;
	  	  		}
  	  		}
  		}
  		return false;
  	}

//------------------------------------------------------------------------------

    /**
     * Checks if this atom's internal coordinates include the proper torsion
     * along the bond/axes defined by the two parameters.
     * @param i the index of the first atom in the pair defining the direction 
     * of the intended torsion
     * @param j the index of the atom atom in the pair defining the direction
     * of the intended torsion
     * @return <code>true</code> if the torsion is used in the definition of 
     * this ZMatrixAtom
     */

    public boolean usesTorsion(int i, int j)
    {
        boolean res = false;
        if (idI == i && idJ == j && idK > -1)
        {
            if(icK.getType().equals("0"))
            {
                res = true;
            }
        }
        return res;
    }

//----------------------------------------------------------------------------

    /**
     * Return the number of internal coordinated defining this ZMatrixAtom
     * @return the number of internal coordinates contained in this object
     */

    public int getICsCount()
    {
        int nICs = 0;
        if (idI!=-1 && null!=icI)
        {
            nICs++;
            if (idJ!=-1 && null!=icJ)
            {
                nICs++;
                if (idK!=-1 && null!=icK)
                {
                    nICs++;
                }
            }
        }
        return nICs;
    }

//----------------------------------------------------------------------------

    /**
     * Checks if the reference IDs in this ZMatrixAtom are the same as
     * a given other ZMatrixAtom.
     * @param other the other ZMatrixAtom
     * @return <code>true</code> is the set of IDs is the same
     */

    public boolean sameIDsAs(ZMatrixAtom other)
    {
        boolean res = false;
        if (this.getICsCount() == other.getICsCount())
        {
            switch (this.getICsCount())
            {
                case 0:
                    res = true;
                    break;

                case 1:
                    res = this.getIC(0).compareIDs(other.getIC(0).getIDs());
                    break;

                case 2:
                    res = this.getIC(1).compareIDs(other.getIC(1).getIDs());
                    break;

                case 3:
                    res = this.getIC(2).compareIDs(other.getIC(2).getIDs());
                    break;
            }
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Produces a string to print this atom as part of a ZMatrix.
     * @param useReferences if <code>true</code> makes the method write variable
     * names instead of their value
     * @param zeroBasedIds set to <code>true</code> to require zero-based
     * numbering
     * @return a string that can be used to print a ZMatrix in human readable
     * text
     */

    public String toZMatrixLine(boolean useReferences, boolean zeroBasedIds)
    {
        StringBuilder sb = new StringBuilder();
        int phase = 1;
        if (zeroBasedIds)
        {
            phase = 0;
        }
        sb.append(name).append(" ");
        if (idI != -1)
        {
            sb.append(idI + phase).append(" ");
            sb.append(icI.getZMatrixString(useReferences)).append(" ");
            if (idJ != -1)
            {
                sb.append(idJ + phase).append(" ");
                sb.append(icJ.getZMatrixString(useReferences)).append(" ");
                if (idK != -1)
                {
                    sb.append(idK + phase).append(" ");
                    sb.append(icK.getZMatrixString(useReferences)).append(" ");
                }
            }
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a deep copy of this ZMatrixAtom
     */
    
    @Override
    public ZMatrixAtom clone()
    {
    	ZMatrixAtom zma = new ZMatrixAtom(name, idI, idJ, idK, icI.clone(), 
    			icJ.clone(), icK.clone());
    	return zma;
    }
    
//------------------------------------------------------------------------------

  	@Override
  	public boolean equals(Object o)
  	{
  		if (!(o instanceof ZMatrixAtom))
  			return false;
  		ZMatrixAtom other = (ZMatrixAtom) o;
  		
  		if (this.name!=null && other.name!=null
  				&& !this.name.equals(other.name))
  			return false;
  		if (this.name==null && other.name!=null)
  			return false;
  		if (this.name!=null && other.name==null)
  			return false;
  		
  		if (this.idI != other.idI)
  			return false;
  		
  		if (this.idJ != other.idJ)
  			return false;
  		
  		if (this.idK != other.idK)
  			return false;

  		if (this.icI!=null && other.icI!=null && !this.icI.equals(other.icI))
	   		 return false;
  		if (this.icI!=null && other.icI==null)
	   		 return false;
  		if (this.icI==null && other.icI!=null)
	   		 return false;

  		if (this.icJ!=null && other.icK!=null && !this.icJ.equals(other.icJ))
	   		 return false;
  		if (this.icJ!=null && other.icJ==null)
	   		 return false;
 		if (this.icJ==null && other.icJ!=null)
	   		 return false;

  		if (this.icK!=null && other.icK!=null && !this.icK.equals(other.icK))
	   		 return false;
  		if (this.icK!=null && other.icK==null)
	   		 return false;
 		if (this.icK==null && other.icK!=null)
	   		 return false;
  		
  	   	return true;
  	}
  	
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(name, idI, idJ, idK, icI, idJ, icK);
    }

//------------------------------------------------------------------------------

	public Map<Object, Object> getProperties() {
		return properties;
	}

//------------------------------------------------------------------------------

	public Object getProperty(Object key) {
		return properties.get(key);
	}

//------------------------------------------------------------------------------

	public void setProperties(Map<Object, Object> properties) {
		this.properties = properties;
	}

//------------------------------------------------------------------------------

	public void setProperty(Object key, Object value) {
		this.properties.put(key,  value);
	}
  	
//------------------------------------------------------------------------------

}
