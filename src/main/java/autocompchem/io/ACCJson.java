package autocompchem.io;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataDeserializer;
import autocompchem.datacollections.NamedData.NamedDataSerializer;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.ParameterStorage.ParameterStorageDeserializer;
import autocompchem.datacollections.ParameterStorage.ParameterStorageSerializer;
import autocompchem.io.jsonableatomcontainer.AtomContainerSetDeserializer;
import autocompchem.io.jsonableatomcontainer.AtomContainerSetSerializer;
import autocompchem.io.jsonableatomcontainer.IAtomContainerDeserializer;
import autocompchem.io.jsonableatomcontainer.IAtomContainerSerializer;
import autocompchem.perception.circumstance.CountTextMatches;
import autocompchem.perception.circumstance.CountTextMatches.CountTextMatchesDeserializer;
import autocompchem.perception.circumstance.CountTextMatches.CountTextMatchesSerializer;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.circumstance.ICircumstance.ICircumstanceDeserializer;
import autocompchem.perception.circumstance.MatchDirComponent;
import autocompchem.perception.circumstance.MatchDirComponent.MatchDirComponentDeserializer;
import autocompchem.perception.circumstance.MatchDirComponent.MatchDirComponentSerializer;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.perception.circumstance.MatchText.MatchTextDeserializer;
import autocompchem.perception.circumstance.MatchText.MatchTextSerializer;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.infochannel.InfoChannelType.InfoChannelTypeDeserializer;
import autocompchem.run.ACCJob;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.Job.JobDeserializer;
import autocompchem.run.Job.JobSerializer;
import autocompchem.run.MonitoringJob;
import autocompchem.run.ShellJob;
import autocompchem.run.ShellJob.ShellJobSerializer;
import autocompchem.run.jobediting.Action.ActionObject;
import autocompchem.run.jobediting.Action.ActionObjectDeserializer;
import autocompchem.run.jobediting.Action.ActionType;
import autocompchem.run.jobediting.Action.ActionTypeDeserializer;
import autocompchem.run.jobediting.AddDirectiveComponent;
import autocompchem.run.jobediting.AddDirectiveComponent.AddDirectiveComponentDeserializer;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskType;
import autocompchem.run.jobediting.DataArchivingRule.ArchivingTaskTypeDeserializer;
import autocompchem.run.jobediting.IJobEditingTask;
import autocompchem.run.jobediting.IJobEditingTask.IJobEditingTaskDeserializer;
import autocompchem.run.jobediting.IJobSettingsInheritTask;
import autocompchem.run.jobediting.IJobSettingsInheritTask.IJobSettingsInheritTaskDeserializer;
import autocompchem.run.jobediting.JobEditType;
import autocompchem.run.jobediting.JobEditType.JobEditTypeDeserializer;
import autocompchem.run.jobediting.SetDirectiveComponent;
import autocompchem.run.jobediting.SetDirectiveComponent.SetDirectiveComponentDeserializer;
import autocompchem.wiro.chem.CompChemJob;
import autocompchem.wiro.chem.DirComponentAddress;
import autocompchem.wiro.chem.Directive;
import autocompchem.wiro.chem.DirectiveData;
import autocompchem.wiro.chem.Keyword;
import autocompchem.wiro.chem.CompChemJob.CompChemJobSerializer;
import autocompchem.wiro.chem.DirComponentAddress.DirComponentAddressDeserializer;
import autocompchem.wiro.chem.DirComponentAddress.DirComponentAddressSerializer;
import autocompchem.wiro.chem.Directive.DirectiveSerializer;
import autocompchem.wiro.chem.DirectiveData.DirectiveDataDeserializer;
import autocompchem.wiro.chem.DirectiveData.DirectiveDataSerializer;
import autocompchem.wiro.chem.Keyword.KeywordDeserializer;
import autocompchem.wiro.chem.Keyword.KeywordSerializer;
import autocompchem.worker.ConfigItem;
import autocompchem.worker.ConfigItem.ConfigItemTypeDeserializer;

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
 * Reader/Writer for JSON files with AutoCompChem objects.
 */

public class ACCJson 
{
	
	private static ACCJson instance = null;
	
	Gson reader;

    Gson writer;
   
//------------------------------------------------------------------------------

    /**
     * Construct the singleton instance. This is run only once.
     */
    private ACCJson()
    {
    	writer = new GsonBuilder()
    			.setPrettyPrinting()
    	        .registerTypeAdapter(Job.class, new JobSerializer())
    	        .registerTypeAdapter(ACCJob.class, new JobSerializer())
    	        .registerTypeAdapter(MonitoringJob.class, new JobSerializer())
    	        .registerTypeAdapter(EvaluationJob.class, new JobSerializer())
    	        .registerTypeAdapter(ShellJob.class, new ShellJobSerializer())
    	        .registerTypeAdapter(CompChemJob.class, 
    	        		new CompChemJobSerializer())
    	        .registerTypeAdapter(DirectiveData.class, 
    	        		new DirectiveDataSerializer())
    	        .registerTypeAdapter(NamedData.class, 
    	        		new NamedDataSerializer())
    	        .registerTypeAdapter(Keyword.class, 
    	        		new KeywordSerializer())
    	        .registerTypeAdapter(ParameterStorage.class, 
    	        		new ParameterStorageSerializer())
    	        .registerTypeAdapter(Directive.class, new DirectiveSerializer())
    	        .registerTypeAdapter(DirComponentAddress.class, 
    	        		new DirComponentAddressSerializer())
    	        .registerTypeAdapter(MatchText.class, 
    	        		new MatchTextSerializer())
    	        .registerTypeAdapter(MatchDirComponent.class, 
    	        		new MatchDirComponentSerializer())
    	        .registerTypeAdapter(CountTextMatches.class, 
    	        		new CountTextMatchesSerializer())
    	        .registerTypeHierarchyAdapter(IAtomContainer.class, 
    	        		new IAtomContainerSerializer())
    	        .registerTypeHierarchyAdapter(AtomContainerSet.class, 
    	        		new AtomContainerSetSerializer())
    			.create();
    	
    	reader = new GsonBuilder()
    			.setPrettyPrinting()
    	        .registerTypeAdapter(Job.class, new JobDeserializer())
    	        .registerTypeAdapter(DirectiveData.class, 
    	        		new DirectiveDataDeserializer())
    	        .registerTypeAdapter(NamedData.class, 
    	        		new NamedDataDeserializer())
    	        .registerTypeAdapter(Keyword.class, 
    	        		new KeywordDeserializer())
    	        .registerTypeAdapter(DirComponentAddress.class, 
    	        		new DirComponentAddressDeserializer())
    	        .registerTypeAdapter(ParameterStorage.class, 
    	        		new ParameterStorageDeserializer())
    	        .registerTypeAdapter(IJobEditingTask.class, 
    	        		new IJobEditingTaskDeserializer())
    	        .registerTypeAdapter(IJobSettingsInheritTask.class, 
    	        		new IJobSettingsInheritTaskDeserializer())
    	        .registerTypeAdapter(ICircumstance.class, 
    	        		new ICircumstanceDeserializer())
    	        .registerTypeAdapter(MatchText.class, 
    	        		new MatchTextDeserializer())
    	        .registerTypeAdapter(CountTextMatches.class,
    	        		new CountTextMatchesDeserializer())
    	        .registerTypeAdapter(MatchDirComponent.class, 
    	        		new MatchDirComponentDeserializer())
    	        .registerTypeAdapter(AddDirectiveComponent.class, 
    	        		new AddDirectiveComponentDeserializer())
    	        .registerTypeAdapter(SetDirectiveComponent.class, 
    	        		new SetDirectiveComponentDeserializer())
    	        .registerTypeHierarchyAdapter(IAtomContainer.class, 
    	        		new IAtomContainerDeserializer())
    	        .registerTypeHierarchyAdapter(AtomContainerSet.class, 
    	        		new AtomContainerSetDeserializer())
    	        // JSON is case-specific but we want to allow case-insentitivity
    	        // for strings representing enums. These deseriualizers
    	        // are for enums that we want to be case-insentitive.
    	        .registerTypeHierarchyAdapter(ActionObject.class, 
    	        		new ActionObjectDeserializer())
    	        .registerTypeHierarchyAdapter(ActionType.class, 
    	        		new ActionTypeDeserializer())
    	        .registerTypeHierarchyAdapter(JobEditType.class, 
    	        		new JobEditTypeDeserializer())
    	        .registerTypeHierarchyAdapter(InfoChannelType.class, 
    	        		new InfoChannelTypeDeserializer())
    	        .registerTypeHierarchyAdapter(ArchivingTaskType.class, 
    	        		new ArchivingTaskTypeDeserializer())
    	        .registerTypeHierarchyAdapter(ConfigItem.class, 
    	        		new ConfigItemTypeDeserializer())
    			.create();
    }

//------------------------------------------------------------------------------

    /**
     * Gets the only implementation of this class.
     * @return the singleton instance
     */
    private static ACCJson getInstance()
    {
        if (instance == null)
            instance = new ACCJson();
        return instance;
    }

//------------------------------------------------------------------------------

    public static Gson getReader() 
    {
        return getInstance().reader;
    }

//------------------------------------------------------------------------------

    public static Gson getWriter() 
    {
        return getInstance().writer;
    }

//------------------------------------------------------------------------------

}
