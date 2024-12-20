## AutoCompChem
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

Verify the requirements by running the two commands: Both should return a message declaring which version has been installed.
```
javac -version
mvn -version
```

Now, you can build AutoCompChem with
```
mvn package
```

Finally, you can call AutoCompChem using a command like the following (NB: replace `$ACC_HOME` and `${VERSION}` as with the values that apply to version you have installed):
On Linux/Mac terminals and Windows GitBash:
```
java -jar $ACC_HOME/target/autocompchem-${VERSION}-jar-with-dependencies.jar
```
Instead, on Windows Anaconda prompt:
```
java -jar $ACC_HOME\target\autocompchem-${VERSION}-jar-with-dependencies.jar
```
You should create an alias so the appropriate command, depending on your operating system. For example, on a BASH 
```
autocompchem="java -jar $ACC_HOME/target/autocompchem-${VERSION}-jar-with-dependencies.jar"
```

## Testing
Many self-evaluating functionality tests are available and can also be used as examples of usage. See under the `test` folder or run them all to verify the functionality of your installation by running 
```
cd $ACC_HOME
./test/run_tests.sh
```

## Usage
Run the following to get the silt of supported tasks
```
autocompchem -h
```
appending any `-t <task>` will give the complete documentation on any available options and argument that are relevant for the specific `<task>`. 
Options and arguments can be specified in any order. Notably, a list of command line arguments can also be written into a text file using exactly the same syntax. Such text file is internally referred as a *parameters' file*. Many examples of such files are available under the [test folder](test) folder. Note however that while command line processing can exploit all command line functionality (e.g., use environmental variables and wildcards in pathnames), while this cannot be done in *parameters' files*.


## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support.
