#!/bin/bash

version=1.0.0
host=localhost
port=8443

help() {
    echo "$(basename $0) $version"
    echo "$0 -i jar-file : install a plugin from jar file"
}

pluginInstall() {
    curl --insecure -X POST \
         -u ${user}:${password} \
         "https://${host}:${port}/plugin/install" \
         -d "jar=${jarFile}"
}

unset user
unset password

while getopts "hu:p:i:" opt; do
  case "$opt" in
    u) user=$OPTARG
        ;;
    p) password=$OPTARG
        ;;
    i) request="install"
       jarFile=$(readlink -f $OPTARG)
        ;;
    h) help
       exit 0
       ;;
    *) echo "Unknown option: $opt"
       exit 1
       ;;
  esac
done

shift $((OPTIND-1))

if [[ -z "$user" || -z "$password" ]]; then
    echo "Missing access credentials"
    exit 1
fi

case "$request" in
    install)
        echo "Installing $jarFile"
        pluginInstall
        echo
        ;;
    *) echo "Unknown request: $request"
        ;;
esac
