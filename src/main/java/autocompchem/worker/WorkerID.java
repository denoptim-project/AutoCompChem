package autocompchem.worker;

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
 * Collection of all registered workers. Only class names registered here
 * can be used to make new instances of workers.
 * 
 * @author Marco Foscato
 */
public enum WorkerID 
{
	DummyWorker,
    AtomClashAnalyzer,
    AtomTypeMatcher,
    AtomLabelsGenerator,
    BasisSetGenerator,
    ChelateAnalyzer,
    ConformationalSpaceGenerator,
    ConnectivityGenerator,
    ConstraintsGenerator,
    DummyObjectsHandler,
    ForceFieldEditor,
	GaussianInputWriter,
    GaussianOutputHandler,
    GaussianReStarter,
    //GenericToolOutputHandler,
    JobEvaluator,
    MolecularComparator,
    MolecularGeometryEditor,
    MolecularMeter,
    MolecularMutator,
    MolecularPruner,
    MolecularReorderer,
    MolecularSorter,
    NWChemInputWriter,
    NWChemOutputHandler,
    NWChemReStarter,
    OrcaInputWriter,
    OrcaOutputHandler,
    XTBInputWriter,
    XTBOutputHandler,
    QMMMInputWriter,
    SpartanInputWriter,
    SpartanInputWriter2, //TODO-gg keep only this and rename
    SpartanOutputHandler,
    VibModuleOutputHandler,
    ZMatrixHandler,
    JobDetailsConverter;
    
//-----------------------------------------------------------------------------

}
