#!/usr/bin/env python3

# JLogic command line utility
# Written by Fabio Leone

version = "1.0.0"

import http.client
from http.client import responses
import ssl
import json
import getopt, sys, os
from base64 import b64encode
from prettytable import PrettyTable
from prettytable import PLAIN_COLUMNS

host = 'localhost';
port = 8443;
user = "" if os.getenv('JLOGIC_USER') is None else os.getenv('JLOGIC_USER')
password = "" if os.getenv('JLOGIC_PASSWORD') is None else os.getenv('JLOGIC_PASSWORD')

# Usage
def usage():
    print(os.path.basename(sys.argv[0]) + " " + version)
    print("Usage: " + sys.argv[0] + " [options]")
    print("  -u, --user     USER          : Set HTTP user")
    print("  -p, --password PASSWORD      : Set HTTP password")
    print("  -l, --list                   : List programs")
    print("  -c, --compile  ID            : Compile program with the given ID")
    print("  -v, --version                : Show version")

def getConnection():
    # print("Connecting as "+user)
    connection = http.client.HTTPSConnection(host, port, timeout=10, context = ssl._create_unverified_context())
    #connection = http.client.HTTPConnection(host, 1234)
    return(connection)

def error(response):
    try:
        jo = json.loads(response.read().decode('utf-8'))
        message = jo["message"]
    except ValueError as e:
        message = "Error " + str(response.status) + ": "+ responses[response.status]

    print(message)
    sys.exit(1)

# List programs
def list():
    connection = getConnection();
    userAndPass = b64encode(str.encode(user+":"+password)).decode("ascii")
    headers = { 'Authorization' : 'Basic %s' %  userAndPass }
    try:
        connection.request("GET", "/programs", headers=headers)
        response = connection.getresponse()

        if response.status == 200:
            jo = json.loads(response.read().decode('utf-8'))

            table = PrettyTable()
            table.field_names = ["ID", "NAME", "VERSION", "STATUS"]
            table.set_style(PLAIN_COLUMNS)
            table.align["ID"] = "l"
            table.align["NAME"] = "l"
            table.align["VERSION"] = "l"
            table.align["STATUS"] = "l"

            for plugin in jo:
                id = plugin['id']
                name = plugin["name"]
                version = plugin["version"]
                status = plugin["status"]
                table.add_row([id, name, version, status])

            print(table)
        else:
            error(response)

    except ConnectionRefusedError:
        print("Failed: Unable to connect");
        sys.exit(1)

    finally:
        connection.close()

# Compile a program
def compile(id):
    print("Compiling "+id+" ...")

    connection = getConnection();
    userAndPass = b64encode(str.encode(user+":"+password)).decode("ascii")
    headers = { 'Authorization' : 'Basic %s' %  userAndPass, 'Content-type': 'application/json' }

    try:
        connection.request("POST", "/program/"+id+"/compile", headers=headers)
        response = connection.getresponse()

        if response.status == 200:
            print('Program successfully compiled')
        else:
            error(response)

    except ConnectionRefusedError:
        print("Failed: Unable to connect");
        sys.exit(1)

    finally:
        connection.close()

# Main
def main():
    global user, password

    if len(sys.argv) == 1:
        usage()
        sys.exit

    try:
        opts, args = getopt.getopt(sys.argv[1:], "hu:p:c:vl", ["help", "user=", "password=", "compile=", "version", "list"])
    except getopt.GetoptError as err:
        # print help information and exit:
        print(err)  # will print something like "option -a not recognized"
        usage()
        sys.exit(2)
    output = None
    verbose = False
    for o, a in opts:
        if o in ("-h", "--help"):
            usage()
            sys.exit()
        elif o in ("-v", "--version"):
            print(version)
            sys.exit()
        elif o in ("-u", "--user"):
            user = a
        elif o in ("-p", "--password"):
            password = a
        elif o in ("-c", "--compile"):
            compile(a)
        elif o in ("-l", "--list"):
            list()
        else:
            assert False, "unhandled option"
    # ...

if __name__ == "__main__":
    main()
