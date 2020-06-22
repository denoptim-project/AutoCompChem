#!/bin/bash
#------------------------------------------------------------------------------
#
#                   Script for the compilation of AutoCompChem
#                                by Marco Foscato
#                              Bergen, 11/June/2020
#
#------------------------------------------------------------------------------
#
# Run with '-h' or '--help' to print the usage instructions and exit.
#
###############################################################################






#TODO dearch for JAVA
javaDir="/usr/bin"

#
# Function printing the usage instructions
#
function printUsage () {
cat <<EOF

  Run this script with no argument to just build AutoCompChem.

  Usage:

  ./build.sh [options]

  Options:

  -h      prints this help message.

  --help  prints this help message.

  -u      runs unit tests after building AutoCompChem.. If followed by one or more strings (i.e., class names), then runs only the unit testing found in the given classes.

  -f      runs functionality tests after building AutoCompChem. If followed by one or more integer numbers, then runs only the corresponding functionality tests.

  -l      prints the log of any functionality test that is run

  -n      excludes building step. Use it to run only tests.

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
        if [[ "${args[$i+1]}" == "-"* ]]
        then
            argument="none"
        else
            argument=${args[$i+1]}
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

    # Clean traces of old jar
    rm -f AutoCompChem.jar
    
    # Check version of Java compiler
    javaVersion=$("$javaDir/javac" -version 2>&1 | awk '{print $2}')
    if [[ "$javaVersion" < "1.7" ]]; then
        echo " "
        echo "UiBKvant requires JAVAC 1.7 or above. Found $javaVersion"
        echo " "
        exit 1
    fi
    echo "Using JAVAC version $javaVersion"
    
    # Compile
    find "$ACCHome/src" -name "*.java" > sourcefiles.txt
    jarsColumnSeparated=$(ls -1 $ACCHome/lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/:/g' | sed 's/\:$//g')
    
    $javaDir/javac -cp "$jarsColumnSeparated" @sourcefiles.txt -d . -Xlint:unchecked 
    exitStatus=$?
    # And check
    if [ $exitStatus != 0 ]
    then
        echo " "
        echo "JAVAC returned non zero exit status!"
        echo " "
        exit 1
    fi
    
    # Copy text files
    lst=$(find "$ACCHome/src" -name "*.txt")
    txtFilesList=""
    for f in $lst
    do
        tmp=$(echo $f | sed "s|$ACCHome\/src\/||" )
        cp $f $tmp
        txtFilesList=$txtFilesList" $tmp"
    done
    
    # list jars in lib
    jars=$(ls -1 $ACCHome/lib/*.jar | while read l ; do echo $l"@@" ; done | tr -d "\n" | sed 's/@@/ /g')
    
    # Create manifest
    echo "Txt files added to classpath: $txtFilesList" 
    echo "Main-Class: autocompchem.ui.ACCMain" > manifest.mf
    echo "Class-Path:$(echo $jars $txtFilesList | fold -w58 | awk '{print " "$0}')" >> manifest.mf
    echo >> manifest.mf
    
    # Make archive
    find ./ -name "*.class" > compiled.txt
    $javaDir/jar cvfm AutoCompChem.jar manifest.mf autocompchem
    # And check
    if [ ! -f "AutoCompChem.jar" ]; then
        echo "###########################################"
        echo "Cannot make AutoCompChem.jar"
        echo "###########################################"
        exit 1
    else
        echo "BUILDING DONE!"
        buildingDone=true
    fi

    #Clean tmp files
    rm -rf sourcefiles.txt manifest.mf compiled.txt sourcefiles.txt autocompchem
}    

#
# Function running unit tests
#
function unitTesting() {
    echo "Starting unit testing"

    if [ ${#chosenUnitTests[@]} -gt 0 ]
    then
        for class in ${chosenUnitTests[@]}
        do
            java -jar junit/junit-platform-console-standalone-1.5.1.jar -cp .:AutoCompChem.jar:"$jarsColumnSeparated" -c "$class"
        done
    else
        #Run all JUnit tests
        java -jar junit/junit-platform-console-standalone-1.5.1.jar -cp .:AutoCompChem.jar:"$jarsColumnSeparated" --scan-classpath=:AutoCompChem.jar --details=tree
    fi
    
    echo " "
    echo "UNIT TESTING DONE!"
    echo " "
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

    # We run all tests aunless a specific list of test IDs was given in  cli args
    if [ ${#chosenTests[@]} -eq 0 ]
    then 
        for i in $(seq 1 $(ls ../*.params | wc -l | awk '{print $1}'))
        do
            chosenTests+=($i)
        done
    fi
    for i in ${chosenTests[@]}
    do 
        parFile="../t$i.params"
        if [ ! -f "$parFile" ]
        then
            echo "Test t$i not found. Jumping to next test ID."
            continue
        fi
        echo " "
        echo "Running test $i/${#chosenTests[@]}"
        log=t$i.log
        $javaDir/java -jar $ACCHome/AutoCompChem.jar  ../t$i.params > $log 2>&1

        if $printFuncTestLog
        then
            echo "Echoing test log:"
            cat t$i.log
        fi
    
        ../t$i.check
    
        if [ $? != 0 ]
        then
            echo " "
            echo "Output of test $i differ from expected output!"
            echo " "
            exit 1
        fi
    done
    
    echo " "
    echo "FUNCTIONALITY TESTING DONE! "
    echo " "
}

###############################################################################
#
# Main
#
###############################################################################

# Now, parse command line options
args=($@)
runBuild=true
buildingDone=false
runUnitTests=false
runFunctionalityTests=false
printFuncTestLog=false
chosenUnitTests=()
chosenTests=()
for ((i=0; i<$#; i++))
do
    arg=${args[$i]}
    case "$arg" in
        "-h") printUsage; exit 1;;
        "--help") printUsage; exit 1;;
        "-u") runUnitTests=true
              getArg "$i" "$#"
              if [[ "none" != "$argument" ]]
              then
                chosenUnitTests+=($argument)
              fi;;
        "-f") runFunctionalityTests=true
	      getArg "$i" "$#" 
	      if [[ "none" != "$argument" ]]
	      then
	        chosenTests+=($argument)
              fi;;
	"-l") printFuncTestLog=true;;
        "-n") runBuild=false;;
        -[a-z,A-Z,0-9]) echo "ERROR! Unrecognized option '$arg'";
              printUsage; exit 1;;
        *);;
    esac
done

if ! $runBuild && ! $runUnitTests && ! $runFunctionalityTests
then
    echo "Nothing to do. '-n' requires no building, but neither '-u' nor '-f' was given. So there is nothing I can do and I exit."
    exit 0
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

if $runBuild
then
    build
    if ! $buildingDone
    then
        exit 1
    fi
fi

if $runUnitTests
then
    unitTesting
fi

if $runFunctionalityTests
then
    functionalityTesting
fi

echo "All done!"
exit 0
