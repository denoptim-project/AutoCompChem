package autocompchem.wiro.chem;

import java.util.HashSet;
import java.util.Set;

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

/**
 * Collection of all registered types of {@link Directive}'s components.
 * 
 * @author Marco Foscato
 */
public enum DirectiveComponentType 
{
	ANY("*"),
	KEYWORD("Key"),
	DIRECTIVEDATA("Dat"),
	DIRECTIVE("Dir");
	
	/**
	 * A string to use as a short identification of this component type.
	 */
	public final String shortString;
	
	/**
	 * Storage of possible short forms. All in Uppercase!
	 */
	private static final Set<String> possibleShortForms;
	
	static {
		Set<String> collector = new HashSet<String>();
		for (DirectiveComponentType dct : values())
		{
			collector.add(dct.shortString.toUpperCase());
		}
		possibleShortForms = new HashSet<String>(collector);
	}
	
//------------------------------------------------------------------------------

	private DirectiveComponentType(String shortString) {
        this.shortString = shortString;
    }

//------------------------------------------------------------------------------
	
	/**
	 * Returns the enum starting from its short string form.
	 * @param shortForm the short form (case insensitive).
	 * @return the enum or null if no enum is found with the given short form.
	 */
	public static DirectiveComponentType getEnum(String shortForm)
	{
		for (DirectiveComponentType typ : values())
		{
			if (typ.shortString.toUpperCase().equals(shortForm.toUpperCase()))
				return typ;
		}
		return null;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Lists the possible short forms to define the value of this enum.
	 * @return the possible short forms.
	 */
	public static Set<String> getShortForms()
	{
		return possibleShortForms;
	}
	
//------------------------------------------------------------------------------

}
