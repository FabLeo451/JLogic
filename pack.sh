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

for f in target/$jarfile target/$jarfile.original target/bp2java
do
  src=$f
  dst=${target_dir}/bin
  
  echo "Copying $src to $dst ..."
  cp -p $src $dst
done

#mv ${target_dir}/bin/$name-0.0.1.jar ${target_dir}/bin/$name

cp -rp data/blueprint ${target_dir}/data
cp -p data/asset/*.json ${target_dir}/data/asset/
cp -rp data/asset/nodes/Standard ${target_dir}/data/asset/nodes
cp -p env.sh ${target_dir}/bin

echo "# GLobal properties" > ${target_dir}/data/global.properties

#--------Begin here document-----------#
(
cat << EOF
#!/bin/bash

source \`dirname \$0\`/env.sh

cd \$JL_BIN

java -jar $jarfile
EOF
) > ${target_dir}/bin/$name
#----------End here document-----------#

chmod +x ${target_dir}/bin/$name
