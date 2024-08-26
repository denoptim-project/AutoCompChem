#!/bin/bash -euo

# Build
cd "$SRC_DIR"
mkdir -p "$PREFIX/lib" "$PREFIX/bin"

mvn clean package 
cp "$SRC_DIR/target/autocompchem-$PKG_VERSION-jar-with-dependencies.jar" "$PREFIX/lib"

echo '#!/bin/bash' > "$PREFIX/bin/autocompchem"
echo 'java -jar "'$PREFIX'/lib/autocompchem-'$PKG_VERSION'-jar-with-dependencies.jar" "$@"' >> "$PREFIX/bin/autocompchem"

chmod +x "${PREFIX}/bin/autocompchem"
