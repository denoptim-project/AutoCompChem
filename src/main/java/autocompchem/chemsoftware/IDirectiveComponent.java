package autocompchem.chemsoftware;

/*
 *   Copyright (C) 2020  Marco Foscato
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

/**
 * 
 * @author Marco Foscato
 */

public interface IDirectiveComponent {

//-----------------------------------------------------------------------------
	
    /**
     * @return the name of this directive component.
     */

	public String getName();
	
//-----------------------------------------------------------------------------
	
    /**
     * @return the kind of directive component this is.
     */

	public DirectiveComponentType getComponentType();
	
//-----------------------------------------------------------------------------
	
    /**
     * Checks if there is any ACC task definition within this component.
     * @return <code>true</code> if there is at least one ACC task definition.
     */
    
    public boolean hasACCTask();
    
//-----------------------------------------------------------------------------
    
    /**
     * 
     */
}
