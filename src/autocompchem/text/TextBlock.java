package autocompchem.text;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

/**
 * Class representing a block of text. Typically a sorted collection of lines 
 * that is extracted from a larger collection.
 * 
 * @author Marco Foscato
 */

public class TextBlock
{
    /**
     * The lines of text of this block
     */
    private ArrayList<String> lines;

    /**
     * The lines of bested blocks (first level only)
     */
    private ArrayList<TextBlock> nestedBlocks;

    /**
     * Extraction-dependent index A.
     * Index of this block in the overall sequence of matches.
     */
    private int idA = -1;

    /**
     * Extraction-dependent index B.
     * Index of this pattern/REGEX that defined this block 
     */
    private int idB = -1;

    /**
     * Extraction-dependent index C.
     * Index of this block among blocks of the same type
     */
    private int idC = -1;


//------------------------------------------------------------------------------

    /**
     * Construct an empty TextBlock
     */

    public TextBlock()
    {
	this.lines = new ArrayList<String>();
	this.nestedBlocks = new ArrayList<TextBlock>();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a TextBlock from its properties
     * @param idA Index of this block in the overall sequence of matches.
     * @param idB Index of this pattern/REGEX that defined this block.
     * @param idC Index of this block among blocks of the same type.
     */

    public TextBlock(int idA, int idB, int idC)
    {
	this.lines = new ArrayList<String>();
	this.nestedBlocks = new ArrayList<TextBlock>();
        this.idA = idA;
        this.idB = idB;
        this.idC = idC;
    }

//------------------------------------------------------------------------------

    /**
     * Construct a TextBlock from its content and properties
     * @param lines the lines of text in this block.
     * @param idA Index of this block in the overall sequence of matches.
     * @param idB Index of this pattern/REGEX that defined this block.
     * @param idC Index of this block among blocks of the same type.
     */

    public TextBlock(ArrayList<String> lines, int idA, int idB, int idC)
    {
	this.lines = new ArrayList<String>();
	this.nestedBlocks = new ArrayList<TextBlock>();
	this.idA = idA;
	this.idB = idB;
	this.idC = idC;
    }

//------------------------------------------------------------------------------

    /**
     * Append a text to this block after the last existing text.
     * @param line the text to append
     */

    public void appendText(String line)
    {
        if (lines == null)
        {
            lines = new ArrayList<String>();
        }
        lines.add(line);
    }

//------------------------------------------------------------------------------

    /**
     * Append a nested block to this block and after the last existing 
     * nested block
     * @param nb the nested bloc to append
     */

    public void appendNestedBlock(TextBlock nb)
    {
	if (nestedBlocks == null)
	{
	    nestedBlocks = new ArrayList<TextBlock>();
	}
        nestedBlocks.add(nb);
    }

//------------------------------------------------------------------------------

    /**
     * Get the lines of text
     */

    public ArrayList<String> getText()
    {
	return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Get nested blocks
     */

    public ArrayList<TextBlock> getNestedBlocks()
    {
        return nestedBlocks;
    }

//------------------------------------------------------------------------------

    /**
     * Get the index of this block in the overall sequence of matched blocks.
     * This is a property that depends on the extraction of this block from
     * its source.
     * @return the index
     */

    public int getIndexA()
    {
	return idA;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of this block in the overall sequence of matched blocks.
     * This is a property that depends on the extraction of this block from
     * its source.
     * @param id the index
     */

    public void setIndexA(int id)
    {
        this.idA = id;
    }

//------------------------------------------------------------------------------

    /**
     * Get the index of this pattern/REGEX that defined this block.
     * This is a property that depends on the extraction of this block from
     * its source.
     * @return the index
     */

    public int getIndexB()
    {
        return idB;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of this pattern/REGEX that defined this block.
     * This is a property that depends on the extraction of this block from
     * its source.
     * @param id the index
     */

    public void setIndexB(int id)
    {
        this.idB = id;
    }

//------------------------------------------------------------------------------

    /**
     * Get the index of this block among blocks of the same type.
     * This is a property that depends on the extraction of this block from
     * its source.
     * @return the index
     */

    public int getIndexC()
    {
        return idC;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of this block among blocks of the same type.
     * This is a property that depends on the extraction of this block from
     * its source.
     * @param id the index
     */

    public void setIndexC(int id)
    {
        this.idC = id;
    }

//------------------------------------------------------------------------------

    /**
     * Return only the indexes as a string
     * @return a string
     */

    public String toShortString()
    {
        return "[TextBlock "+idA+"_"+idB+"_"+idC+" ]";
    }

//------------------------------------------------------------------------------

    /**
     * Return a string representation
     * @return a string
     */

    public String toString()
    {
        String NL = System.getProperty("line.separator");
	StringBuilder sb = new StringBuilder();
	sb.append("[TextBlock "+idA+"_"+idB+"_"+idC+" [");
	if (lines != null && lines.size()>0)
	{
	    boolean first = true;
	    for (String l : lines)
	    {
	        if (first)
	        {
		    sb.append(l);
		    first = false;
	        }
	        else
	        {
	            sb.append(NL+l);
		}
	    }
	}
	sb.append("]");
	if (nestedBlocks == null || nestedBlocks.size() == 0)
	{
	    sb.append(" 0 nestedBlocks]");
	}
	else
	{
	    sb.append(" "+nestedBlocks.size()+" nested blocks:");
	    for(TextBlock tb : nestedBlocks)
	    {
		sb.append(NL+" -nested-> "+tb.toShortString());
	    }
	    sb.append("]");
	}

	return sb.toString();
    }

//------------------------------------------------------------------------------

}