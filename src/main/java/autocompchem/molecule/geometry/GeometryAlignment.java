package autocompchem.molecule.geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Mapping;

import autocompchem.atom.AtomUtils;
import autocompchem.io.jsonableatomcontainer.JSONableIAtomContainer;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.utils.NumberUtils;

/**
 * An alignment (superposition in space) of two {@link AtomContainer}s.
 */
public class GeometryAlignment 
{
	/**
	 * Atom by atom mapping
	 */
	private Map<IAtom, IAtom> atomMapping;
	
	/**
	 * Aligned first structure
	 */
	private JSONableIAtomContainer firstIAC;
	
	/**
	 * Aligned second structure
	 */
	private JSONableIAtomContainer secondIAC;
	
	/**
	 * The RMSD
	 */
	private double rmsd = Double.NaN;
	
	/**
	 * The RMS deviation of intermolecular distances.
	 */
	private double rmsdint = Double.NaN;
	
//------------------------------------------------------------------------------

	/**
	 * Constructor for a snapshot of the state of the two atom containers. 
	 * We take a clone of each container and store such clone, not the original 
	 * instance.
	 * @param iac1
	 * @param iac2
	 * @param atomMapping
	 * @throws CloneNotSupportedException 
	 */
	public GeometryAlignment(IAtomContainer iac1, IAtomContainer iac2, 
			Map<IAtom, IAtom> atomMapping) throws CloneNotSupportedException
	{
		this.firstIAC = new JSONableIAtomContainer(iac1.clone());
		this.secondIAC = new JSONableIAtomContainer(iac2.clone());
		this.atomMapping = new HashMap<IAtom, IAtom>();
		for (Entry<IAtom, IAtom> e : atomMapping.entrySet())
		{
			this.atomMapping.put(
					this.firstIAC.getAtom(iac1.indexOf(e.getKey())),
					this.secondIAC.getAtom(iac2.indexOf(e.getValue())));
		}
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Sets the RMSD
	 */
	public void setRMSD(double rmsd)
	{
		this.rmsd = rmsd;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Gets the RMSD
	 */
	public double getRMSD()
	{
		return this.rmsd;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Sets the RMS RMS deviation of intermolecular distances
	 */
	public void setRMSDIM(double rmsdint)
	{
		this.rmsdint = rmsdint;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Gets the RMS deviation of intermolecular distances
	 */
	public double getRMSDIM()
	{
		return this.rmsdint;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Returns the atom container with the first geometry aligned
	 * @return the atom container with the first geometry aligned
	 */
	public JSONableIAtomContainer getFirstIAC() {
		return firstIAC;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Returns the atom container with the second geometry aligned
	 * @return the atom container with the second geometry aligned
	 */
	public JSONableIAtomContainer getSecondIAC() {
		return secondIAC;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Gets the atom mapping defined in by the indexed of the atoms in the 
	 * respective containers.
	 * @return the mapping expressed by atom indexes.
	 */
	public Map<Integer, Integer> getMappingIndexes()
	{
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (Entry<IAtom, IAtom> e : atomMapping.entrySet())
		{
			map.put(this.firstIAC.iac.indexOf(e.getKey()),
					this.secondIAC.iac.indexOf(e.getValue()));
		}
		return map;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Returns a string describing the atom-atom mapping
	 * @return a string that can be printed to describe the mapping
	 */
	public String getMappingDefinition() {
		StringBuilder sb = new StringBuilder();
		for(Entry<IAtom, IAtom> entry : atomMapping.entrySet())
		{
			sb.append(MolecularUtils.getAtomRef(entry.getKey(), firstIAC.iac))
				.append(" - ")
				.append(MolecularUtils.getAtomRef(entry.getValue(), secondIAC.iac))
				.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}  
	
//------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o)
    {
    	if (!(o instanceof GeometryAlignment))
    		return false;
    	GeometryAlignment other = (GeometryAlignment) o;
    	
    	if (!NumberUtils.closeEnough(this.rmsd, other.rmsd))
			 return false;

    	if (!NumberUtils.closeEnough(this.rmsdint, other.rmsdint))
			 return false;
    	
    	return this.firstIAC.equals(other.firstIAC) 
   			 && this.secondIAC.equals(other.secondIAC);
    }
	
//------------------------------------------------------------------------------
		
}
