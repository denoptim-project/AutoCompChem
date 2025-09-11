package autocompchem.molecule.sorting;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to sorting molecules.
 * 
 * @author Marco Foscato
 */

public class MolecularSorter extends AtomContainerInputProcessor
{

    /**
     * Name of the property used to sort
     */
    private String propertyName;
    
    /**
     * String defining the task of sorting molecules
     */
    public static final String SORTMOLECULESTASKNAME = "sortMolecules";

    /**
     * Task about sorting molecules
     */
    public static final Task SORTMOLECULESTASK;
    static {
    	SORTMOLECULESTASK = Task.make(SORTMOLECULESTASKNAME);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public MolecularSorter()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(SORTMOLECULESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularSorter.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularSorter();
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();

        this.propertyName = params.getParameter("SDFPROPERTY")
        		.getValueAsString();
    }

//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @Override
    public void performTask()
    {
    	multiGeomMode = MultiGeomMode.ALLINONEJOB;
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
		throw new IllegalStateException(this.getClass().getSimpleName() 
				+ " should not call this method.");
    }
    
//------------------------------------------------------------------------------

	@Override
	public List<IAtomContainer> processAllAtomContainer(List<IAtomContainer> iacs) 
	{
        return sort(iacs, propertyName);
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Sort the atom container by the value returned by 
	 * {@link IAtomContainer#getProperties()}.
	 * @param iacs the containers to sort.
	 * @param propertyName the identifier of the property to use for sorting.
	 * @return the sorted list of atom containers.
	 */
	public static List<IAtomContainer> sort(List<IAtomContainer> iacs, 
			String propertyName)
	{
		List<SortableMolecule> smols = new ArrayList<SortableMolecule>();
        for (IAtomContainer mol : iacs)
        {
            if (!MolecularUtils.hasProperty(mol, propertyName))
            {
                String err = "Molecule " + MolecularUtils.getNameOrID(mol)
                             + " has no field named '" + propertyName + "'.";
                Terminator.withMsgAndStatus("ERROR! " + err, -1);
            }

            SortableMolecule sm = new SortableMolecule(mol, 
            		mol.getProperty(propertyName));
            smols.add(sm);
        }

        Collections.sort(smols, new SortableMoleculeComparator());

        List<IAtomContainer> result = new ArrayList<IAtomContainer>();
        for (SortableMolecule smol : smols)
    	{
        	result.add(smol.getIAtomContainer());
    	}
        return result;
	}

//-----------------------------------------------------------------------------

}
