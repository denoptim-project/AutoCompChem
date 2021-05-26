package autocompchem.text;

/*
 *   Copyright (C) 2014  Marco Foscato
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
import java.util.Collection;

/**
 * Class representing a block of text in the form of a sorted collection of lines.
 * 
 * @author Marco Foscato
 */

public class TextBlock extends ArrayList<String>
{

//------------------------------------------------------------------------------

    /**
	 * Version UID
	 */
	private static final long serialVersionUID = 4368062182512848954L;

	/**
     * Construct an empty TextBlock
     */

    public TextBlock()
    {
    }

//------------------------------------------------------------------------------

      /**
       * Construct an empty TextBlock
       */

      public TextBlock(Collection<String> c)
      {
    	  super(c);
      }
      
//------------------------------------------------------------------------------

    /**
     * Append a text to this block after the last existing text.
     * @param line the text to append
     */

    public void appendText(String line)
    {
        super.add(line);
    }

//------------------------------------------------------------------------------

}
