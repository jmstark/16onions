#!/usr/bin/env python3
import subprocess as proc
import sys
import time
import os
import signal

JAVA_PATH = "java"

MODULES = ["gossip.Main",
           "mockups.nse.Main",
           "mockups.rps.Main",
           "mockups.auth.Main",
           "mockups.onion.Main"]
PROCESSES = []

#Flag used for signalling termination
TERMINATE = False

def start_module(name, args):
    split_index = -2
    java_args = args[:split_index]
    java_args.append(name)
    java_args.extend(args[split_index:])
    java_args.insert(0, "java")
    print(java_args)
    return proc.Popen(args=java_args,
                      executable=JAVA_PATH,
                      cwd=os.getcwd(),
                      stderr = proc.STDOUT)

def start_all():
    global MODULES
    global PROCESSES
    for module in MODULES:
        PROCESSES.append(start_module(module,args))
        time.sleep(1)

def terminate_all():
    for process in PROCESSES:
        process.terminate()
    for process in PROCESSES:
        process.wait()
    ret = 0;
    for process in PROCESSES:
        if 0 != process.returncode:
            ret = process.returncode
    return ret

def signal_cb_sigchld(signum, frame):
    global TERMINATE
    signal.signal(signal.SIGCHLD, signal.SIG_IGN)
    TERMINATE = True

def signal_cb_sigterm(signum, frame):
    global TERMINATE
    TERMINATE = True

def setup_signals():
    signal.signal(signal.SIGCHLD, signal_cb_sigchld)
    signal.signal(signal.SIGTERM, signal_cb_sigterm)
    signal.signal(signal.SIGINT, signal_cb_sigterm)

def await():
    global TERMINATE
    while not TERMINATE:
        signal.pause()

if __name__ == "__main__":
    args = sys.argv[1:]
    setup_signals()
    start_all()
    await()
    exit(terminate_all())

    


