# Installation from Source
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

## Testing
Many self-evaluating tests are available to verify the functionality of AutoCompChem. These tests are also examples of usage. See under the `test` folder or run them all to verify the functionality of your installation by running
```
cd $ACC_HOME
./test/run_tests.sh
```

## Which JAR to Use?
The building process produces two JAR archives meant for different purposes:

1. `autocompchem-${VERSION}.jar` (Spring Boot JAR)
- **Size**: ~73 MB
- **Structure**: Spring Boot executable JAR with nested dependencies.
- **Use case**: Recommended for server deployments and development.
- **Features**:
  - Optimized for Spring Boot applications.
  - Better memory management for web applications.
  - Standard Spring Boot deployment practices.

2. `autocompchem-${VERSION}-jar-with-dependencies.jar` (Flat JAR)
- **Size**: ~72 MB  
- **Structure**: Traditional flat JAR with all dependencies at root level.
- **Use case**: Recommended for CLI usage and standalone deployments.
- **Features**:
  - Faster startup for command-line tasks.
  - Traditional JAR structure.
  - No Spring Boot overhead for simple CLI operations.

**Both JARs work for both modes**, but the above recommendations provide optimal performance.

Finally, you can use either of the JAR files with a command like the following (NB: replace `$ACC_HOME` and `${VERSION}` with the values that apply to the version you have installed):
On Linux/Mac terminals and Windows GitBash:
```
java -jar $ACC_HOME/target/autocompchem-${VERSION}-jar-with-dependencies.jar
```
Instead, on Windows Anaconda prompt (assuming you have created the environment with any version of conda):
```
java -jar $ACC_HOME\target\autocompchem-${VERSION}-jar-with-dependencies.jar
```

You should create an alias to the appropriate command, depending on your operating system. For example, on a Unix system running a BASH terminal the alias would look like this (NB: replace `$ACC_HOME` and `${VERSION}` with the values that apply to the version you have installed):
```
autocompchem="java -jar $ACC_HOME/target/autocompchem-${VERSION}-jar-with-dependencies.jar"
```
