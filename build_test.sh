#!/bin/bash
#------------------------------------------------------------------------------
#
#                Script for the compilation of "AutoCompChem"
#                             by Marco Foscato
#                            Bergen, 2/Dec/2013
#
#------------------------------------------------------------------------------

# Default settings
ACCHome=$(pwd)
recompUibKvT="Yes"
UIBKvantHOME="$ACCHome/../UiBKvant"
uivkvantVersion="1.0"
javaDir="/usr/bin"

#------------------------------------------------------------------------------
# No need to change default paths/variables below this line
#------------------------------------------------------------------------------

src=$ACCHome/src
jar=$ACCHome/jar
lib=$ACCHome/lib
tst=$ACCHome/test

#welcome
echo " "
echo "####################################################################"
echo "          Welcome to the building script for AutoCompChem"
echo "####################################################################"
echo " "
echo " Please, check the default settings:"
echo " -> AutoCompChem home directory:     $ACCHome"
echo " -> path to JAVA (javac, java, jar): $javaDir"
echo " "
echo " -> Recompile uibkvant library:      $recompUibKvT"
echo " -> uibkvant home directory:         $UIBKvantHOME"
echo " "

#ask for changes
while true; do
    echo " Do you accept all the default settings?"
    echo " Type 'N/no' to make changes or Y/yes/RETURN' to accept all"
    read yn
    case $yn in
        [Nn]* )
		echo " "
                echo " Specify the AutoCompChem home directory (RETURN to skip):"
                read nt
                if [ ! -z $nt ]
                then
                        ACCHome=$nt
                fi
		echo " "
                echo " Specify the path to JAVA tools (RETURN to skip)"
                read nt
                if [ ! -z $nt ]
                then
                        javaDir=$nt
                fi
		echo " "
                echo " Do you want to recompile UiBkvant library from source? [Y/N]"
                    read yn2
                    case $yn2 in
                        [Yy]* )
			    recompUibKvT="Yes"
			    echo " "
                            echo " Specify the UiBkvant home directory (RETURN to skip)"
                            read nt
                            if [ ! -z $nt ]
                            then
                                UIBKvantHOME=$nt
                            fi
                            break;;
                        [Nn]* )
			    recompUibKvT="No"
			    UIBKvantHOME=""
                            break;;
	        	* ) echo " Please answer yes/y or no/n.";;
		    esac
                break;;
        [Yy]* )
                break;;
        * ) 
        	if [ -z $yn ]
		then
		    break
		else
		    echo " Please answer yes/y or no/n."
		fi
        esac
done

#remember launching folder
old=$(pwd)

# Clean traces of old jar
if [ -f "$jar/AutoCompChem.jar" ]; then
    rm $jar/AutoCompChem.jar
fi
if [ ! -d "$jar" ]; then
    mkdir "$jar"
fi

# Check version of Java compiler
javaVersion=$("$javaDir/javac" -version 2>&1 | awk '{print $2}')
if [[ "$javaVersion" < "1.7" ]]; then
    echo " "
    echo " AutoCompChem requires JAVAC 1.7 or above. Found $javaVersion"
    echo " "
    exit 1
fi
echo "JAVAC version $javaVersion"

# Recompile library uibkvant
if [ "$recompUibKvT" == "Yes" ]
then
    cd $UIBKvantHOME
    echo "Building UiBkvant library..."
    ./build.sh
    if [ ! -f "uibkvant-$uivkvantVersion.jar" ]; then
        exit 1
    else
        echo " "
        echo " Library uibkvant-$uivkvantVersion built."
        echo " "
    fi

    cd $ACCHome
    cp $UIBKvantHOME/uibkvant-$uivkvantVersion.jar lib/
    cp $UIBKvantHOME/lib/cdk-1.4.14.jar lib/
    cp $UIBKvantHOME/lib/guava-20.0.jar lib/
fi

# Compile
cd $ACCHome
echo "Building AutoCompChem..."
find ./ -name "*.java" > sourcefiles.txt
$javaDir/javac -cp $lib/uibkvant-$uivkvantVersion.jar:$lib/cdk-1.4.14.jar:$lib/guava-20.0.jar @sourcefiles.txt -d . -Xlint:unchecked 
exitStatus=$?
if [ $exitStatus != 0 ]
then
    echo " "
    echo "ERROR while compiling: JAVAC returned non zero exit status!"
    echo " "
    exit 1
fi

# Create manifest 
echo "Main-Class: AutoCompChem" > manifest.mf
echo "Class-Path: $lib/uibkvant-$uivkvantVersion.jar $lib/cdk-1.4.14.jar $lib/guava-20.0.jar" >> manifest.mf
echo >> manifest.mf

# Make archive
find ./ -name "*.class" > compiled.txt
$javaDir/jar cvfm AutoCompChem.jar manifest.mf *.class $lib/uibkvant-$uivkvantVersion.jar $lib/cdk-1.4.14.jar $lib/guava-20.0.jar
if [ ! -f "AutoCompChem.jar" ]; then
    echo " "
    echo "ERROR while making jar file"
    echo " "
fi

#Clean tmp files
rm -rf *.class manifest.mf compiled.txt sourcefiles.txt
mv AutoCompChem.jar $jar

# Run tests
echo " "
echo "BUILDING DONE!"
echo " "
echo "Testing AutoCompChem..."
if [ ! -d $tst/results ]
then
  mkdir $tst/results
fi
cd $tst/results
rm -rf $tst/results/*

#hard coded flag used for development
runAll=true
#runAll=false

totTests="112"
if $runAll == true 
then
    for i in $(seq 1 $totTests)
    do 
        echo "##################"
        echo "Running test $i/$totTests"
        log=t$i.log
        $javaDir/java -jar $jar/AutoCompChem.jar  ../t$i.params > $log 2>&1

        ../t$i.check
        
        if [ $? != 0 ]
        then
            echo " "
            echo " Output of test $i differ from expected output!"
            echo " "
            exit 
        fi
    done
else
    n=109
    echo "###############"
    echo "Running test $n"
    rm -f $tst/results/*
#    $javaDir/java -jar $jar/AutoCompChem.jar ../t$n.params 
    $javaDir/java -jar $jar/AutoCompChem.jar ../t$n.params > t$n.log 2>&1
    ../t$n.check

    cat t$n.log 
fi

echo " "
echo " TESTING DONE! "
echo " "

exit 0
