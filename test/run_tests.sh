#!/bin/bash
#------------------------------------------------------------------------------
#
#                   Script for running functionality tests
#                                by Marco Foscato
#                              Bergen, 11/June/2022
#
#------------------------------------------------------------------------------
#
# Run with '-h' or '--help' to print the usage instructions and exit.
#
###############################################################################

#
# Function to get version from POM file
#
function getAccVersion () {
    accVersion=$(grep -m 1 "<version>" "$ACCHome/pom.xml" | awk -F'[>,<]' '{print $3}')
}

#
# Function to find java toolbox and set javaDir path accordingly
#
function findJava () {
    if [ -z "$javaDir" ]; then
        if type -p javac; then
            echo "Found javac in PATH."
            javaDir=$(dirname $(which javac))
        elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
            echo "Found javac executable in JAVA_HOME."
            javaDir="$JAVA_HOME/bin"
        else
            echo " "
            echo "Compiling AutoCompChem requires JDK 11 or above. None found."
            echo "Please, do any of the following:"
            echo " - add Java executables to the PATH, or "
            echo " - define the JAVA_HOME environmental variable, or "
            echo " - run this script with option -j <path_to_java_bin_folder> "
            echo "    In this case, you'll remember to run AutoCompChem using the same"
            echo "    executables in <path_to_java_bin_folder>."
            echo " "
            exit 1
        fi
    fi
    echo "Java tools from '$javaDir'."
    if [ ! -f "$javaDir/java" ]; then
        echo "Java executable not found at '$javaDir/java'!"
        exit 1
    elif [ ! -f "$javaDir/javac" ]; then
        echo "Javac executable not found at '$javaDir/javac'!"
        exit 1
    elif [ ! -f "$javaDir/jar" ]; then
        echo "Jar executable not found at '$javaDir/jar'!"
        exit 1
    fi
        
    javaVersion=$("$javaDir/javac" -version 2>&1 | awk '{print $2}' | awk -F_ '{print $1}')
    jvNum="$javaVersion"
    if [[ "$jvNum" == "1."* ]]; then
        jvNum="$(echo "$jvNum" | cut -c 3- )"
    fi
    numDots="$(echo $jvNum | awk -F"." '{print NF-1}')"
    if [ 0 -eq "$numDots" ]; then
        jvNum="$(echo "$jvNum" | awk '{printf("%d0000",$1)}')"
    elif [ 1 -eq "$numDots" ]; then
        jvNum="$(echo "$jvNum" | awk -F. '{printf("%d%04d",$1,$2)}')"
    else
        jvNum="$(echo "$jvNum" | awk -F. '{printf("%d%04d",$1,$2)}')"
    fi
    if [ "$jvNum" -lt "110000" ]; then
        echo " "
        echo "Compiling AutoCompChem requires JDK 11 or above. Found $javaVersion"
        echo " "
        exit 1
    fi
    echo "Using JAVAC version $javaVersion"  
}

#
# Function printing the usage instructions
#
function printUsage () {
cat <<EOF

  Use this script to run functionality tests.

  Usage:

  ./build.sh [options]

  Options:

  -h         prints this help message.

  --help     prints this help message.

  -b         request to rebuilt AutoCompChem.

  -c [args]  runs command line interface (CLI) testing. If followed by one 
             or more integer numbers, then runs only the corresponding CLI 
             tests.

  -f [args]  runs functionality tests after building AutoCompChem. If 
             followed by one or more integer numbers, then runs only the 
             corresponding functionality tests.

  -l          prints the log of any functionality test that is run.

  -j <path/bin>     specifies the pathname of a specific Java bin folder. 

EOF
}

#
# Function that reads the argument of a cli option, if present
#
function getArg() {
    ii="$1"
    tot="$2"
    ii=$((ii+1))
    if [ "$ii" -ge "$tot" ]
    then
        argument="none"
    else
        if [[ "${args[$ii]}" == "-"* ]]
        then
            argument="none"
        else
            argument=${args[$ii]}
        fi
    fi
}


#
# Cross-platform fuction to find out our actual path following symlinks
#
function getMyAbsoluteDirname() {
    myName="$(basename "$0")"
    cd "$(dirname "$0")"
    i=0
    while [ -L "$myName" ]
    do
        i=$((i+1))
        myName="$(readlink "$myName")"
        cd "$(dirname "$myName")"
        myName="$(basename "$myName")"
        if [ 50 -lt "$i" ]
        then
            echo "Possible cyclic symlink! Exiting, now!"
            exit 1
        fi
    done
    myDir="$(pwd -P)"
}

#
# Function for building
#
function build() {
    echo "Building AutoCompChem"

    if ! [ -x "$(command -v mvn)" ]; then
        echo " "
        echo "ERROR: cannot build software. Maven not found."
        echo "Are you sure the environment is properly set up?"
        echo " "
        exit 1
    fi
 
    mvn clean
    mvn package
    exitStatus=$?
    # And check
    if [ $exitStatus != 0 ]
    then
        echo " "
        echo "ERROR: Maven returned non zero exit status!"
        echo " "
        exit 1
    else
        echo "BUILDING DONE!"
    fi
}    


#
# Function running functionality tests, i.e., real usage case tests.
#
function functionalityTesting() {
    echo "Starting tests of full functionality with real case examples."
    tst=$ACCHome/test
    if [ ! -d $tst/results ]
    then
      mkdir $tst/results
    fi
    cd $tst/results
    rm -rf $tst/results/*

    # We run all tests unless a specific list of test IDs was given
    if [ ${#chosenTests[@]} -eq 0 ]
    then 
        for i in $(seq 1 $(ls ../t*.params | sed 's/\.\.\/t//' | sed 's/.params//' | sort -n | tail -n 1))
        do
            chosenTests+=($i)
        done
    fi
    for i in ${chosenTests[@]}
    do 
        parFile="../t$i.params"
        if [ ! -f "$parFile" ]
        then
            #echo "Test t$i not found. Jumping to next test ID."
            continue
        fi
        echo " "
        echo "Running test $i/${#chosenTests[@]}"
        log=t$i.log
        if $interactiveRun
        then
            "$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar"  "../t$i.params"
        else
            "$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar"  "../t$i.params" > "$log" 2>&1
        fi

        if $printFuncTestLog
        then
            echo "Echoing test log:"
            cat "$log"
        fi
    
        ../t$i.check
    
        if [ $? != 0 ]
        then
            echo " "
            echo "Output of test $i differs from expected output!"
            echo " "
            exit 1
        fi
    done
    
    echo " "
    echo "FUNCTIONALITY TESTING DONE! "
    echo " "
}

#
# Function running functionality tests with command line arguments
#
function cliTesting() {
    echo "Starting tests of command line interface."
    tst=$ACCHome/test
    if [ ! -d $tst/results ]
    then
      mkdir $tst/results
    fi
    cd $tst/results
    rm -rf $tst/results/cli*

    # We run all tests unless a specific list of test IDs was given
    if [ ${#chosenCliTests[@]} -eq 0 ]
    then
        for i in $(seq 1 $(ls ../cli*.sh | wc -l | awk '{print $1}'))
        do
            chosenCliTests+=($i)
        done
    fi
    for i in ${chosenCliTests[@]}
    do
        argsFile="../cli$i.sh"
        if [ ! -f "$argsFile" ]
        then
            echo "Test CLI $i not found. Jumping to next test ID."
            continue
        fi

        echo " "
        echo "Running test CLI $i/${#chosenCliTests[@]}"
        log=cli$i.log

        . ../cli$i.sh > "$log" 2>&1

        if $printFuncTestLog
        then
            echo "Echoing test log:"
            cat "$log"
        fi

        ../cli$i.check

        if [ $? != 0 ]
        then
            echo " "
            echo "Output of CLI test $i differs from expected output!"
            echo " "
            exit 1
        fi
    done
    
    echo " "
    echo "CLI TESTING DONE! "
    echo " "   
}

###############################################################################
#
# Main
#
###############################################################################

# Now, parse command line options
args=($@)
runBuild=false
runCliTesting=false
runFunctionalityTests=false
printFuncTestLog=false
interactiveRun=false
chosenUnitTests=()
chosenCliTests=()
chosenTests=()
for ((i=0; i<$#; i++))
do
    arg=${args[$i]}
    case "$arg" in
        "-h") printUsage; exit 1;;
        "--help") printUsage; exit 1;;
        "-b") runBuild=true;;
        "-f") runFunctionalityTests=true
              for ((j=$i; j<$#; j++))
              do
                  getArg "$j" "$#"
                  if [[ "none" != "$argument" ]]
                  then
                      chosenTests+=($argument)
                  else
                      break
                  fi
              done;;
        "-c") runCliTesting=true
              for ((j=$i; j<$#; j++))
              do
                  getArg "$j" "$#"
                  if [[ "none" != "$argument" ]]
                  then
                      chosenCliTests+=($argument)
                  else
                      break
                  fi
              done;;
	"-l") printFuncTestLog=true;;
        "-i") interactiveRun=true;;
        "-j") getArg "$i" "$#"
              javaDir="$argument";;
        -[a-z,A-Z,0-9]) echo "ERROR! Unrecognized option '$arg'";
              printUsage; exit 1;;
        *);;
    esac
done

if ! $runCliTesting && ! $runFunctionalityTests
then
    runFunctionalityTests=true
    runCliTesting=true
fi

# Find where I am, and cd to the ACCHome folder
old=$(pwd)
#NB: here we find out the source folder of this very script
#NB: and we possibly follow synlinks to this file
getMyAbsoluteDirname
# We must assume the intended folder structure.
# i.e., this script is under a subfolder of the ACCHome
ACCHome="$(cd "$myDir/.." ; pwd -P)"
cd "$ACCHome"

# This script looks for the version specified in the POM file
getAccVersion
echo "Using ACC version $accVersion"

# Here we make sure that java tools are available
findJava

if [ ! -f "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" ] || $runBuild
then
    build
fi

if $runCliTesting
then
    cliTesting
fi

if $runFunctionalityTests
then
    functionalityTesting
fi

echo "All done!"
exit 0
