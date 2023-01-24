## AutoCompChem
AutoCompChem (or ACC) is a collection of tools used to automatize computational chemistry tasks.

## Installation
1) Create a suitable environment with [conda](https://docs.conda.io/en/latest/)
   ```
   conda env create -f https://raw.githubusercontent.com/denoptim-project/AutoCompChem/main/environment.yml
   ```
   and then activate it with 
   ```
   conda activate acc_devel
   ```
   Alternatively, if you do not want to use conda, you can install [Maven](https://maven.apache.org/) and JAVA (version 11 or higher) by yerself. You endironment should allow to run the following commands without errors.
    ```
    mvn -version
    javac -version
    ```
2) Download the latest release from [the releases page](https://github.com/denoptim-project/AutoCompChem/releases) and unzip/untar-gz the resulting archive.

3) move inside the resulting folder, which we'll call `$ACC_HOME`
    ```
    cd $ACC_HOME
    ```
4) Build the project and run all tests.
    ```
    ./test/run_tests.sh
    ```
5) If the above command terminates successfully, you are done. AutoCompChem is ready to be used.

## Usage
Say you have installed AutoCompChem inside a folder that we'll refer to as the `$ACC_HOME` folder. This is how to launch any functionality of the tool by executing the Main class if the `$version` you have installed:

```
java -jar $ACC_HOME/target/autocompchem-${version}-jar-with-dependencies.jar
```
Executing the above command will print the usage instructions. In particular, use the '-h' option to get help.


## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support.
