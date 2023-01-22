package autocompchem.perception;

import java.util.ArrayList;
import java.util.List;

import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.situation.Situation;

/**
 * Utility class representing a text query that is associated with one or
 * more pairs of situation:circumstance/s each represented by a {@link SCPair}.
 */

public class TxtQuery
{
	/**
     * The text query.
     */
    public String query = "";

    /**
     * List of sources as
     * pairs of Situation and Circumstance that require matching
     * this very same text.
     */
    public List<SCPair> sources = new ArrayList<SCPair>();

//------------------------------------------------------------------------------

    /**
     * Constructor with arguments
     * @param query the actual text query 
     * @param s the source situation that includes the circumstance 
     * that include the query.
     * @param c the circumstance that requires the query
     */

    public TxtQuery(String query, Situation s, ICircumstance c)
    {
		this.query = query;
        this.sources.add(new SCPair(s,c));
    }

//------------------------------------------------------------------------------

    /**
     * Add a pair of references
     * @param s the source situation that includes the circumstance
     * that include the query.
     * @param c the circumstance that requires the query
     */

    public void addReference(Situation s, ICircumstance c)
    {
        this.sources.add(new SCPair(s,c));
    }

//------------------------------------------------------------------------------
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[TxtQuery '"+query+"' in sources:").append(
        		System.getProperty("line.separator"));
        for (SCPair nc : sources)
        {
            sb.append("   -> "+nc.toString());
        }
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
}