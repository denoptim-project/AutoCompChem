package autocompchem.worker;

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
    BasisSetGenerator,
    ChelateAnalyzer,
    ConnectivityGenerator,
    DummyObjectsHandler,
    ForceFieldEditor,
	GaussianInputWriter,
    GaussianOutputHandler,
    GaussianReStarter,
    GenericToolOutputHandler,
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
    QMMMInputWriter,
    SpartanInputWriter,
    SpartanOutputHandler,
    VibModuleOutputHandler,
    ZMatrixHandler;
    
//-----------------------------------------------------------------------------

}
