/*
 *   This code is taken from the DENOPTIM project. See 
 *   https://github.com/denoptim-project/DENOPTIM for further details.
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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


/**
 * Deserialisation of {@link AtomContainerSet} in which each entry is 
 * manipulated as {@link JSONableIAtomContainer}.
 * 
 * @author Marco Foscato
 */

public class AtomContainerSetDeserializer implements JsonDeserializer<AtomContainerSet>
{

    @Override
    public AtomContainerSet deserialize(JsonElement jsonEl, Type type,
            JsonDeserializationContext context) throws JsonParseException
    {
        if (!jsonEl.isJsonArray())
            return null;

        JsonArray jArray = jsonEl.getAsJsonArray();
        AtomContainerSet cSet = new AtomContainerSet();
        for (JsonElement nestedJsonElement : jArray)
        {
            IAtomContainer iac = context.deserialize(nestedJsonElement,
            		IAtomContainer.class);
            cSet.addAtomContainer(iac);
        }
        return cSet;
    }
}
