#!/bin/bash

name=jlogic
version=0.1.0-alpha-8
dist_dir=$name-$version
jarfile=$name-$version.jar
source_dir=$name
destination_dir=.

while getopts "hD:" opt; do
  case "$opt" in
    D) destination_dir=$OPTARG
       ;;
    h) echo "$0 [-D path]"
       exit 0
       ;;
    *) echo "Unknown option: $opt"
       exit 1
       ;;
  esac
done

shift $((OPTIND-1))

target_dir=${destination_dir}/${dist_dir}

echo "Target directory: ${target_dir}"

echo "Creating directories..."

mkdir -p ${target_dir}/bin
mkdir -p ${target_dir}/log
mkdir -p ${target_dir}/lib
mkdir -p ${target_dir}/plugin
mkdir -p ${target_dir}/temp
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

# lib

for f in lib/java-getopt-1.0.13.jar  lib/json-simple-1.1.1.jar  lib/log4j-1.2.12.jar  lib/Standard.jar
do
  src=$f
  dst=${target_dir}/lib

  echo "Copying $src to $dst ..."
  cp -p $src $dst
done

#mv ${target_dir}/bin/$name-0.0.1.jar ${target_dir}/bin/$name

cp -rp data/blueprint ${target_dir}/data

cp -p data/asset/*.json ${target_dir}/data/asset/
cp -rp data/asset/nodes/Standard ${target_dir}/data/asset/nodes
cp -rp data/asset/nodes/JSON ${target_dir}/data/asset/nodes
cp -rp data/asset/nodes/File ${target_dir}/data/asset/nodes

cp -p LICENSE ${target_dir}
cp -p env.sh ${target_dir}/bin

echo "# Global properties" > ${target_dir}/data/global.properties

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
