package autocompchem.modeling.atomtuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.molecule.MolecularUtils;
import autocompchem.utils.NumberUtils;

/*
 * This class represents a geometry condition that is applied on a define a tuple of atoms.
 * It is used to filter out tuples that do not satisfy the condition.
 */
class AtomTupleGeomCondition
{
    public enum GeomConditionType {DISTANCE, ANGLE, DIHEDRAL};
    public GeomConditionType type;
    public Double value;
	public enum GeomConditionOperator {
		CLOSE_TO, LESS_THAN, MORE_THAN, MAX, MIN};
	public GeomConditionOperator operator;
	private static final List<GeomConditionOperator> VALUELESS_OPERATORS = Arrays.asList(
		GeomConditionOperator.MIN, GeomConditionOperator.MAX);
	public List<Integer> atomIndexes;

//------------------------------------------------------------------------------

    /**
     * Constructs a geometry condition from the given type, atom indexes, operator, and value.
     * @param type the type of the condition.
     * @param atomIndexes the list of atom indexes.
     * @param operator the operator of the condition.
     * @param value the value of the condition.
     */
	public AtomTupleGeomCondition(GeomConditionType type, 
		List<Integer> atomIndexes, GeomConditionOperator operator, double value)
	{
		this.type = type;
		this.atomIndexes = atomIndexes;
		this.operator = operator;
		this.value = Double.valueOf(value);
	}

//------------------------------------------------------------------------------

    /**
     * Constructs a geometry condition from the given text representation.
     * @param text the text representation of the condition.
     */
	public AtomTupleGeomCondition(String text)
	{
		String[] parts = text.trim().split("\\s+");
		if (parts.length < 4) // Minimum: type atmId0 atmId1 operator [value]
		{
			throw new IllegalArgumentException(
				"Invalid definition of geometric condition: " + text);
		}

		// Type of the condition
		String typeStr = parts[0].toUpperCase();
		try {
			this.type = GeomConditionType.valueOf(typeStr);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
				"Invalid type of geometric condition: " + typeStr);
		}

		// Atom indexes of the condition
		this.atomIndexes = new ArrayList<Integer>();
		int nextIdx = 1;
		for (int i = 1; i < parts.length; i++)
		{
			if (!NumberUtils.isParsableToInt(parts[i]))
			{
				break;
			}
			this.atomIndexes.add(Integer.parseInt(parts[i]));
			nextIdx = i + 1;
		}

		if (nextIdx+1 > parts.length) // Minimum: type atmId0 atmId1... operator [value]
		{
			throw new IllegalArgumentException(
				"Invalid definition of geometric condition "
				+ "(missing operator and value): " + text);
		}
		
		// Operator
		String operatorStr = parts[nextIdx].toUpperCase();
		operatorStr = operatorStr.replaceAll("<", 
            GeomConditionOperator.LESS_THAN.toString());
		operatorStr = operatorStr.replaceAll(">", 
            GeomConditionOperator.MORE_THAN.toString());
		operatorStr = operatorStr.replaceAll("=", 
            GeomConditionOperator.CLOSE_TO.toString());
		try {
			this.operator = GeomConditionOperator.valueOf(operatorStr);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
				"Invalid operator of geometric condition: " + operatorStr);
		}
		nextIdx++;

		if (VALUELESS_OPERATORS.contains(operator))
		{
			if (nextIdx > parts.length)
			{
				throw new IllegalArgumentException(
					"Invalid definition of geometric condition "
					+ "(unused value): " + text);
			}
		} else {
			if (nextIdx+1 > parts.length)
			{
				throw new IllegalArgumentException(
					"Invalid definition of geometric condition "
					+ "(missing value): " + text);
			}
			this.value = Double.valueOf(parts[nextIdx]);
			nextIdx++;
		}
		if (nextIdx < parts.length)
		{
			throw new IllegalArgumentException(
				"Invalid definition of geometric condition "
				+ "(unused parts): " + text);
		}
	}

//------------------------------------------------------------------------------

	/**
	 * Checks if the geometric condition is independent, i.e., it applies to a single tuple of atoms.
	 * @return true if the condition is independent, false otherwise.
	 */
	public boolean isIndependent()
	{
		if (operator!=null && (
			operator == GeomConditionOperator.CLOSE_TO ||
			operator == GeomConditionOperator.LESS_THAN ||
			operator == GeomConditionOperator.MORE_THAN))
		{
			return true;
		}
		return false;
	}

//------------------------------------------------------------------------------

	/**
	 * Returns the value of the geometric condition for the given atom container.
	 * @param iac the atom container to get the value from.
	 * @return the value of the geometric condition.
	 */
	public double getValue(AnnotatedAtomTuple tuple, IAtomContainer iac)
	{
		return getValue(tuple, iac, null);
	}
//------------------------------------------------------------------------------

	/**
	 * Returns the value of the geometric condition for the given atom container.
	 * @param iac the atom container to get the value from.
	 * @return the value of the geometric condition.
	 */
	public double getValue(AnnotatedAtomTuple tuple, IAtomContainer iac, 
		Logger logger)
	{
        if (tuple.getNumberOfIDs() < atomIndexes.size())
        {
            throw new IllegalArgumentException(
                "The tuple has " + tuple.getNumberOfIDs() + " atoms, "
                + "but the geometric condition specified " + atomIndexes.size() 
				+ " indexes.");
        }
		IAtom[] atoms = new IAtom[atomIndexes.size()];
		for (int i = 0; i < atomIndexes.size(); i++)
		{
            if (i >= tuple.getNumberOfIDs())
            {
                throw new IllegalArgumentException(
                    "The tuple has " + tuple.getNumberOfIDs() + " atoms, "
                    + "but the geometric condition specified atom index " 
                    + i + " (Out of range).");
            }
			atoms[i] = iac.getAtom(tuple.getAtomIDs().get(atomIndexes.get(i)));
		}

		if (logger != null)
		{
			String msg = "Atoms used for geometry condition " 
				+ this.type + " " + this.atomIndexes + ": ";
			for (int i = 0; i < atoms.length; i++)
			{
				msg += MolecularUtils.getAtomRef(atoms[i], iac) + " ";
			}
			logger.trace(msg);
		}

		double valueInIAC = 0.0;
		switch (type)
		{
			case DISTANCE: 
			    valueInIAC = MolecularUtils.calculateInteratomicDistance(
					atoms[0], atoms[1]);
				break;
			case ANGLE: 
			    valueInIAC = MolecularUtils.calculateBondAngle(
				    atoms[0], atoms[1], atoms[2]);
				break;
			case DIHEDRAL: 
			    valueInIAC = MolecularUtils.calculateTorsionAngle(
				    atoms[0], atoms[1], atoms[2], atoms[3]);
				break;
		}
		return valueInIAC;
	}

//------------------------------------------------------------------------------

	/**
	 * For independent constraints (i.e., those that apply to a single 
	 * tuple of atoms), checks if the constraint is satisfied by the 
	 * given atom container.
	 * @param iac the atom container to check the constraint against.
	 * @return true if the constraint is satisfied, false otherwise.
	 * @throws IllegalArgumentException if the constraint is dependent, 
	 * so this method should not be called.
	 */
	public boolean isSatisfied(AnnotatedAtomTuple tuple, IAtomContainer iac)
	{
		return isSatisfied(tuple, iac, null);
	}

//------------------------------------------------------------------------------

	/**
	 * For independent constraints (i.e., those that apply to a single 
	 * tuple of atoms), checks if the constraint is satisfied by the 
	 * given atom container.
	 * @param iac the atom container to check the constraint against.
	 * @return true if the constraint is satisfied, false otherwise.
	 * @throws IllegalArgumentException if the constraint is dependent, 
	 * so this method should not be called.
	 */
	public boolean isSatisfied(AnnotatedAtomTuple tuple, IAtomContainer iac,
		Logger logger)
	{
		double valueInIAC = getValue(tuple, iac, logger);
		
		if (logger != null)
		{
			logger.debug("Value of geometry condition " 
				+ this.type + " " + this.atomIndexes + " for tuple " 
				+ tuple.getAtomIDs() + " is " + valueInIAC);
		}

		switch (operator)
		{
			case CLOSE_TO: return NumberUtils.closeEnough(
				value, valueInIAC, 0.001);
			case LESS_THAN: return valueInIAC < value;
			case MORE_THAN: return valueInIAC > value;
			case MIN: case MAX: 
			    throw new IllegalArgumentException("List-wise geometric conditions "
					+ "cannot be used for independent geometric condition evaluation.");
			default:
			    throw new IllegalArgumentException("Invalid operator: " + operator 
                + " for geometric condition type " + type + ".");
		}
	}

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "AtomTupleGeomCondition [type=" + type + ", atomIndexes=" 
            + atomIndexes + ", operator=" + operator + ", value=" + value + "]";
    }

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AtomTupleGeomCondition other = (AtomTupleGeomCondition) obj;
        return type == other.type && atomIndexes.equals(other.atomIndexes) 
            && operator == other.operator 
            && NumberUtils.closeEnough(value, other.value, 0.001);
    }
}