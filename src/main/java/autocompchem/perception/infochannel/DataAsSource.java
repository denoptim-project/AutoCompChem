package autocompchem.perception.infochannel;

/*
 *   Copyright (C) 2026  Marco Foscato
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

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import autocompchem.datacollections.DataFetchingException;
import autocompchem.datacollections.NamedDataCollector;

/**
 * Class representing a processable data source.
 */

public class DataAsSource extends InfoChannel
{
    /**
     * Path to fetch data from the source {@link NamedDataCollector}.
     */
    public final String dataPath;

    /**
     * The data identified by the given name. This is not null
     * only upon making a context-specific copy of this info channel, i.e., by
     * parsing the data exposed by an output reader..
     */
    public Object data = null;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty RawDataAsSource
     */

    public DataAsSource(String dataPath)
    {
        super();
        this.dataPath = dataPath;
        this.setType(InfoChannelType.DATA);
    }

//------------------------------------------------------------------------------

    /**
     * Return the path to the data value to use for percption.
     * @return the data path.
     */
    public String getDataPath()
    {
        return dataPath;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the data.
     * @return the data.
     */
    public Object getData()
    {
        return data;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a new instance containing the data taken from the given source 
     * {@link NamedDataCollector}.
     * @param dataSource the source {@link NamedDataCollector}.
     * @return a new instance containing the data taken from the given source 
     * {@link NamedDataCollector}.
     */
    public DataAsSource makeSpecificCopy(NamedDataCollector dataSource)
    {
        DataAsSource das = new DataAsSource(dataPath);
        das.fetchDataFromSource(dataSource);
        return das;
    }

//------------------------------------------------------------------------------

    /**
     * Fetches the data from the given source {@link NamedDataCollector}.
     * @param dataSource the source {@link NamedDataCollector}.
     */
    public void fetchDataFromSource(NamedDataCollector dataSource)
    {
        String[] path= dataPath.split(",");
        try {
            this.data = dataSource.getNestedDataValue(path);
        } catch (DataFetchingException e) {
            // Do nothing: lack of data must be allowed.
        }
    }
    
//------------------------------------------------------------------------------

    public static class DataAsSourceSerializer 
    implements JsonSerializer<DataAsSource>
    {
        @Override
        public JsonElement serialize(DataAsSource das, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONICIMPLEMENTATION, 
            		InfoChannelImplementation.valueOf(
            				das.getClass().getSimpleName().toUpperCase())
            		.toString());
            jsonObject.addProperty(JSONINFOCHANNELTYPE, 
            		das.getType().toString());
            
            jsonObject.add("dataPath", context.serialize(das.dataPath));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class DataAsSourceDeserializer 
    implements JsonDeserializer<DataAsSource>
    {
        @Override
        public DataAsSource deserialize(JsonElement json, Type typeOfT,
              JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has(JSONICIMPLEMENTATION))
            {
                String msg = "Missing '" + JSONICIMPLEMENTATION + "': found a "
                        + "JSON string that cannot be converted into any "
                        + "InfoChannel subclass.";
                throw new JsonParseException(msg);
            }   
            
            InfoChannelImplementation impl = context.deserialize(
            		jsonObject.get(JSONICIMPLEMENTATION),
            		InfoChannelImplementation.class);
            if (this.getClass().getSimpleName().toUpperCase().equals(
            		impl.toString()))
            {
                String msg = "Cannot to deserialize '" + impl + "' into "
            			+ this.getClass().getSimpleName() + ".";
                throw new JsonParseException(msg);
            }

            String dataPath = context.deserialize(
            		jsonObject.get("dataPath"), String.class);

            InfoChannelType type = context.deserialize(
            		jsonObject.get(JSONINFOCHANNELTYPE), InfoChannelType.class);

            DataAsSource das = new DataAsSource(dataPath);
            das.setType(type);

            return das;
        }
    }

//------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "DataAsSource [dataPath=" + dataPath + ", data=" + data + "]";
    }

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataAsSource other = (DataAsSource) obj;
        if (data != null && other.data != null) {
            return Objects.equals(dataPath, other.dataPath) && Objects.equals(data, other.data);
        }
        return Objects.equals(dataPath, other.dataPath);
    }

//------------------------------------------------------------------------------

    @Override
    public int hashCode() {
        if (data != null) {
            return Objects.hash(dataPath, data);
        }
        return Objects.hash(dataPath);
    }

//------------------------------------------------------------------------------
}
