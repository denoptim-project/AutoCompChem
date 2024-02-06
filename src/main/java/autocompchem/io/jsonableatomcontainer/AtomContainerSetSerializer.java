/*
 *   This code is taken from the DENOPTIM project. See 
 *   https://github.com/denoptim-project/DENOPTIM for further details.
 *   
 *   The only modification is the use of this project's AtomUtils instead of the 
 *   corresponding MoleculeUtils package from DENOPTIM.
 *   
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
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

package autocompchem.io.jsonableatomcontainer;

import java.lang.reflect.Type;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


/**
 * Class to serialize CDK's {@link AtomContainerSet} in a simplified manner.
 * The simplification implies that all items are serialized as 
 * {@link JSONableIAtomContainer}. Therefore, you must not expect to find all 
 * attributes or properties of the full {@link IAtomContainer}.
 * The goal of this class is to make the most light-weight JSON 
 * representation of an {@link AtomContainerSet}.
 * 
 * @author Marco Foscato
 */
public class AtomContainerSetSerializer implements JsonSerializer<AtomContainerSet>
{

    @Override
    public JsonElement serialize(AtomContainerSet cSet, Type typeOfSrc,
            JsonSerializationContext context)
    {
    	JsonArray jsonArray = new JsonArray();
    	for (IAtomContainer iac : cSet.atomContainers())
    	{
    		jsonArray.add(context.serialize(iac));
    	}
        return jsonArray;
    }
}
