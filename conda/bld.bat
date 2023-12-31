cd "%SRC_DIR%"

cmd.exe /c mvn --batch-mode clean || echo ""
cmd.exe /c mvn --batch-mode package || echo ""

copy "%SRC_DIR%\target\autocompchem-%PKG_VERSION%-jar-with-dependencies.jar" "%LIBRARY_LIB%\"

md "%SCRIPTS%\"

echo @echo off > "%SCRIPTS%\autocompchem.cmd"
echo java -jar "%LIBRARY_LIB%\autocompchem-%PKG_VERSION%-jar-with-dependencies.jar" %%* >> "%SCRIPTS%\autocompchem.cmd"
echo IF %%ERRORLEVEL%% NEQ 0 EXIT /B %%ERRORLEVEL%% >> "%SCRIPTS%\autocompchem.cmd"

echo #!/bin/bash > "%SCRIPTS%\autocompchem"
echo java -jar "%LIBRARY_LIB%\autocompchem-%PKG_VERSION%-jar-with-dependencies.jar" $@ >> "%SCRIPTS%\autocompchem"

