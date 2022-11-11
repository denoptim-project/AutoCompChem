## AutoCompChem
AutoCompChem (or ACC) is a colelction of tools used to automatize computational chemistry tasks.

## Installation
1) Make sure you have [Maven](https://maven.apache.org/), which is used to build AutoCompChem. The following command should return the version of Maven
```
mvn -version
```
2) Make sure you have a version of JAVA that is &ge;11. To this end try the following command
``` 
javac -version
```
3) Download the latest release and unzip/untar-gs the resulting archive. Alternatively, clone this github repository.

4) move inside the resulting folder, which we'll call `$ACC_HOME`
```
cd AutoCompChem
```
5) Build the project and run all tests
```
./test/run_tests.sh
```
6) If the above command terminates successfully, you are done. AutoCompChem is ready to be used.

## Usage
Say you have installed AutoCompChem inside a folder that we'll refer to as the `$ACC_HOME` folder. This is how to launch any functionality of the tool by executing the Main class if the `$version` you have installed:

    java -jar $ACC_HOME/target/autocompchem-${version}-jar-with-dependencies.jar

Executing the above command will print the usage instructions.


## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support.
