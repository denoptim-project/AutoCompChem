package autocompchem.chemsoftware.orca;

import java.util.Comparator;

import autocompchem.chemsoftware.Directive;

/**
 * Comparator meant to sort directives according to the expected order of 
 * the Orca input file.
 * 
 * @author Marco Foscato
 */
public class OrcaDirectiveComparator implements Comparator<Directive> 
{

	@Override
	public int compare(Directive dA, Directive dB) 
	{
		String nA = dA.getName().replace("%", "");
		String nB = dB.getName().replace("%", "");
		
		int iA = 0;
		int iB = 0;
		
		if (nA.startsWith("!"))
			iA = -10;
		if (nB.startsWith("!"))
			iB = -10;
		
		if (nA.startsWith("#"))
			iA = -9;
		if (nB.startsWith("#"))
			iB = -9;
		
		if (nA.startsWith("*"))
			iA = 10;
		if (nB.startsWith("*"))
			iB = 10;
		
		if (nA.startsWith("coords"))
			iA = 10;
		if (nB.startsWith("coords"))
			iB = 10;
		
		return Integer.compare(iA, iB);
	}

}
