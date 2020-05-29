#!/bin/bash

name=jlogic
version=0.0.1
target_dir=$name-$version
jarfile=$name-$version.jar
source_dir=$name

echo "Creating directories..."

mkdir -p ${target_dir}/bin
mkdir -p ${target_dir}/log
mkdir -p ${target_dir}/classes
mkdir -p ${target_dir}/data/program
mkdir -p ${target_dir}/data/asset/nodes

# bin

for f in target/$jarfile target/$jarfile.original target/bp2java
do
  src=$f
  dst=${target_dir}/bin
  
  echo "Copying $src to $dst ..."
  cp -p $src $dst
done

# classes

for f in classes/java-getopt-1.0.13.jar  classes/jsch-0.1.55.jar  classes/json-simple-1.1.1.jar  classes/log4j-1.2.12.jar  classes/Standard.jar
do
  src=$f
  dst=${target_dir}/classes
  
  echo "Copying $src to $dst ..."
  cp -p $src $dst
done

#mv ${target_dir}/bin/$name-0.0.1.jar ${target_dir}/bin/$name

cp -rp data/blueprint ${target_dir}/data

cp -p data/asset/*.json ${target_dir}/data/asset/
cp -rp data/asset/nodes/Standard ${target_dir}/data/asset/nodes
cp -rp data/asset/nodes/JSON ${target_dir}/data/asset/nodes
cp -rp data/asset/nodes/SFTP ${target_dir}/data/asset/nodes
cp -rp data/asset/nodes/File ${target_dir}/data/asset/nodes

cp -p env.sh ${target_dir}/bin

echo "# GLobal properties" > ${target_dir}/data/global.properties

#--------Begin here document-----------#
(
cat << EOF
#!/bin/bash

source \`dirname \$0\`/env.sh

java -jar \$JL_BIN/$jarfile

EOF
) > ${target_dir}/bin/$name
#----------End here document-----------#

chmod +x ${target_dir}/bin/$name
