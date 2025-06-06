# AutoCompChem

![Anaconda_Version](https://anaconda.org/denoptim-project/autocompchem/badges/version.svg) ![Anaconda_last](https://anaconda.org/denoptim-project/autocompchem/badges/latest_release_date.svg) ![Anaconda_Platforms](https://anaconda.org/denoptim-project/autocompchem/badges/platforms.svg) ![Anaconda_License](https://anaconda.org/denoptim-project/autocompchem/badges/license.svg) ![Anaconda_downloads](
https://anaconda.org/denoptim-project/autocompchem/badges/downloads.svg)

AutoCompChem (or ACC) is a collection of tools used to automatize computational chemistry tasks.

## Installation from Conda
From within any conda environment you can install AutoCompChem as follows
```
conda install -c denoptim-project autocompchem
```
Now the `autocompchem` command should be available. Try to run the following to start using it:
```
autocompchem -h
```

## Installation from Source
Download and extract the <a href="https://github.com/denoptim-project/AutoCompChem/releases/latest">latest release</a> to create a folder we'll call `ACC_HOME`. In the following, remember to replace `$ACC_HOME` with the pathname leading to the extracted distribution of AutoCompChem.

Make sure you have an environment that includes JAVA and Maven. Such environment, which we call `acc_devel`, can be created by manual installation of both JAVA and Maven, or it can be created using conda:
```
cd $ACC_HOME
conda env create -f environment.yml
conda activate acc_devel
```

Verify the requirements by running these two commands: Both should return a message declaring which version has been installed.
```
javac -version
mvn -version
```

Now, you can build AutoCompChem with
```
mvn package
```

Finally, you can call AutoCompChem using a command like the following (NB: replace `$ACC_HOME` and `${VERSION}` with the values that apply to the version you have installed):
On Linux/Mac terminals and Windows GitBash:
```
java -jar $ACC_HOME/target/autocompchem-${VERSION}-jar-with-dependencies.jar
```
Instead, on Windows Anaconda prompt (assuming you have created the environment with any version of conda):
```
java -jar $ACC_HOME\target\autocompchem-${VERSION}-jar-with-dependencies.jar
```
You should create an alias to the appropriate command, depending on your operating system. For example, on a Unix system running a BASH terminal the alias woudl look like this (NB: replace `$ACC_HOME` and `${VERSION}` with the values that apply to the version you have installed): 
```
autocompchem="java -jar $ACC_HOME/target/autocompchem-${VERSION}-jar-with-dependencies.jar"
```

## Testing
Many self-evaluating tests are available to verify the functionality of AutoCompChem. These tests are also examples of usage. See under the `test` folder or run them all to verify the functionality of your installation by running 
```
cd $ACC_HOME
./test/run_tests.sh
```

## Usage
AutoCompChem can perform a number of tasks, i.e., operations useful in various contexts of any computational chemistry project. Run the following "help" command to get the list of supported tasks:
```
autocompchem -h
```
From the message writted by running the "help" command, choose a task you are interested in, say `<chosen_task>`, and repeate the above "help" command adding `-t <chosen_task>` (NB: in this document we use `<...>` to indicate any unspecified string. For example, `<chosen_task>` could be replaced by `mutateAtoms` or any other task name):
```
autocompchem -h -t <chosen_task>
```
This will give the complete documentation on any available command line argument that may be used to define any settings when performing the `<chosen_task>`. Arguments may be specified in any order, and are used either as *keywords* or *keyword:value* pairs. The *keyword* is always case-insentitive, but the value is case sensitive. The syntax for specifying any argument is
```
--<keyword>
```
for any argument that does not require a value, and
```
--<keyword> <value>
```
for those requiring a value. Quotation marks should be used as usual, when the value may contain spaces or other characters that the command line would interprete in unintended ways. E.g., `--<keyword> "<value1> <value2> <value3>"`. 

The actual execution of a task by AutoCompChem is called a *job*. AutoCompChem can perform single jobs or multiple jobs in a single call, i.e., workflows, where AutoCompChem performs a series of possibly nested tasks in sequence or in parallel. The settings of any job can be defined in three ways: as command line arguments, or in a [parameters' file](#parameters-file), or in a [JSON job details file](#job-details-file). The choice between these three ways to control AutoCompChem depends on the complexity of the job: for getting help or running simple jobs, the command line interface is the most effective, but becomes impractical if many complex arguments have to be used. In the latter case, the [parameters' file](#parameters-file) becomes more suitable than the command line interface, but if the complexity increases by nesting multiple jobs into a workflow, then a [JSON job details file](#job-details-file) allows to exploit the functionality of any good text editor to easily navigate the details of each part of the workflow.

### Single Job
#### Parameters File
Any list of command line arguments needed to perform a task can also be written into a text file, which is internally referred as a *parameters' file*. The following syntax appies:
```
TASK: <chosen_task>
<keywordA>
<keywordB>: <value1> <value2> <value3>
...
```
The order of the lines is irrelevant. Each line is meant to hold a single *keyword* or a single *keyword:value* pair, unless the value constains newline characters, in which case the `$START` and `$END` strings should be used to identify a multiline block of text that will be interpreted as a single line when parsing the perameters file. For example:
```
TASK: <chosen_task>
$START<keywordC>: <value1>
<value2>
<value3>
$END
<keywordD>: <valueD> 
...
```

> [!NOTE]
> While command line processing can exploit all command line functionality (e.g., use environmental variables and wildcards in pathnames), this cannot be done in parameters' files.

To use a parameters' file, call AutoCompChem and give it the pathname to the parameters' file as value of the `-p` (`--params`) argument:
```
autocompchem -p <path_to_parameters_file>
```

##### Example
The parameters file [examples/single_job.params](examples/single_job.params) defines a simple task meant to change the identity of any Cl atom into Br in a given SDF file:
```
TASK: mutateAtoms
INFILE: mol.sdf
SMARTS: [Cl] element Br
OUTFILE: mol_atomEdited.sdf
```
To perform this task you do the following:
```
cd $ACC_HOME/examples
autocompchem -p single_job.params
```
The result is saved into file `mol_atomEdited.sdf`, which you can visualize with any molecular visualization software package (e.g., [Jmol](https://jmol.sourceforge.net/)).
Many examples of parameters files are available under the [test folder](test), where they are named `*.params`.

#### Job Details File
Parameters may also be provided by defining jobs in [JSON format](https://en.wikipedia.org/wiki/JSON). This format is slightly more verbose than the parameters' file format, but is a standard format that can be manipulated by other software. For instance, it allows text editors to efficiently navigate the nested structure of the data stored in a JSON format. To define an AutoCompChem job (i.e., an *ACCJob*) in a JSON file use the following syntax:
```
{
  "jobType": "ACCJob",
  "params": [
    {
      "reference": "task",
      "value": "<chosen_task>"
    },
    {
      "reference": "<keywordA>"
    },
    {
      "reference": "<keywordB>",
      "value": "<value1> <value2> <value3>"
    },
    {
      "reference": "<keywordC>",
      "value": "<value1>\n<value2>\n<value3>"
    }
  ]
}
```
Note that any command line argument is defined in terms of a `reference`, i.e., the string identifying the argument, and `value`, which is the actual value of that argument, if any. 
To use a job details file, call AutoCompChem and give it the pathname to the job details file as value of the `-j` (`--job`) argument:
```
autocompchem -j <path_to_job_details_file>
```

The job details file [examples/single_job.json](examples/single_job.json) defines the same job performed in the previour example, i.e., a job meant to change the identity of any Cl atom into Br in a given SDF file. 
Many other examples are available under the [test folder](test). However, note that the JSON format can be used to define many kinds on objects, including jobs that are not meant to be performed by AutoCompChem, e.g., any molecular modeling job. Therefore, not all ´.json´ files in the test folder define ACCJobs.

JSON job details files can be conveniently generated from parameter's file (and vice cersa) by the `convertJobDefinition` task.

### Multiple Jobs
AutoCompChem can also perform multiple tasks, hence *jobs*, whether in a sequence (i.e., a workflow), or in parallel (i.e., a batch). Either way, the list of jobs to perform, whether steps of a workflow or independent jobs to be performed in parallel, are defined within a job that acts as a container. Such container may itself be contained in a parent job resulting in a recursive structure. 
The distinction between serial and parallel execution is controlled by the jobs' container: if the container defines the `PARALLELIZE: <threads>` key-value pair, then the contained jobs will be executed in parallel using a number of asynchronous threads equal to the value specified by `<threads>`.

Here are the two ways to define AutoCompChem jobs meant to contain sub-jobs, whether serial or parallel, in parameter's file format or job details file format:

* **Parameters files**: the `JOBSTART` and `JOBEND` keyword are used to surround the settings and content of a single job (NB: empty lines are only used to increase readability, but they are not needed):
  ```
  JOBSTART
  TASK: <task1>
  ...
  JOBEND

  JOBSTART
  TASK: <task2>
  ...
  JOBEND
  ```
  This parameters file defines a two-step *sequential* job. Notably, the container job can be effectively omitted from this syntax unless it needs to be altered in some way. For instance, you may alter the container job by requesting to run its subjobs in parallel:
  ```
  JOBSTART
  PARALLELIZE: 2
  
  JOBSTART
  TASK: <task1>
  ...
  JOBEND

  JOBSTART
  TASK: <task2>
  ...
  JOBEND
  
  JOBEND
  ```
  Now, the two tasks will be executed in parallel within the parent job that acts as a container.

  For example, files [examples/sequential.params](examples/sequential.params) and [examples/parallel.params](examples/parallel.params) can be used to perform the same tasks either sequentially or in parallel under the [examples](examples) folder.
  
* **Job details files**:
  The jobs contained in a job container are listed under the ´steps´ section, irrespectively on whether the execution is meant to be serial or parallel.
  ```
  {
    "jobType": "ACCJob",
    "steps": [
      {
        "jobType": "ACCJob",
        "params": [
          {
            "reference": "task",
            "value": "<task1>"
          },
          ...
        ]
      },
      {
        "jobType": "ACCJob",
        "params": [
          {
            "reference": "task",
            "value": "<task2>"
          },
          ...
        ]
      }
    ]
  }
  ```
  Adding the JSON-formatted version of the `PARALLELIZE: <threads>` parameter to the job container will make the contained jobs run in parallel using the given number of threads:
  ```
  {
    "jobType": "ACCJob",
    "params": [
      {
        "reference": "parallelize",
        "value": 2
      }
    ], 
    "steps": [
      ...
    ]
  }
  ```
  
  For example, see files [examples/sequential.json](examples/sequential.json) and [examples/parallel.json](examples/parallel.json) can be used to perform the same tasks either sequentially or in parallel under the [examples](examples) folder.

## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support.
