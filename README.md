Requirements
============

You need JDK version 7 or above.

Additionally, you need Apache Ant to be installed as that is used as a the build
system.


Building
========

Before you use the testing framework, you should build the code:

    ant compile

Usually the master branch should be stable.  To check if you didn't pull any
broken commit you should run the test suites which test the testing suite ---
how ironical!

    ant test

Note that the testcases create TCP clients and servers on ports which are not
chosen randomly, so if a testsuite fails please try to understand where it fails
in the code and see if it is due to another program occupying that port.  The
sockets they use are usually bound to localhost (127.0.0.1 by default); they can
also use IPv6 when available.  Additionally refer to the Testing Caveats section
below.

The build does not produce any JAR, which is actually useful, to do that:

    ant jar


Running
=======

The run the sample Gossip module of the bootstrap peer:
  
    java -cp dist/voip.jar gossip.Main --config config/bootstrap.conf

The framework is equipped with flexible logging mechanism.  For example to
increase the verbosity of Gossip module try:
  
    java -Djava.util.logging.config.file=logging.properties -cp dist/voip.jar \
      gossip.Main --config config/peer-2.conf

You can edit the logging.properties file to set the verbosity of different
components.

Depending on your setup you may have to set the classpath for java to find the
dependency libraries.  In this case extend the `-cp dist/voip.jar' argument as
following:
  
    -cp dist/voip.jar:libs/commons-cli-1.3.1.jar:libs/ini4j-0.5.4.jar:junit-4.12.jar


Debugging
=========

If you'd like to debug the code it is helpful to run the code and then attach a
debugger to it.

To start the debugger run the Java VM with options:
  
    -Xdebug -Xrunjdwp:transport=dt_socket,address=4001,server=y

Now you can attach JPDA debugger to the socket 4001 on localhost.


Testing Caveats
===============

In test cases for the server implementation, lot of clients try to open
connections to the server.  This works until the system runs out of ports and
the connect() syscall fails with address unavailable error.  Observing
experimentally, this seems to happen after ~24500 connections.

This behavior is due to TCP connections waiting in TCP_WAIT state.  TCP_WAIT is
configured by default to have a timeout of ~1 minute.

To fix this, you have to either shrink the TCP_WAIT timeout to 1 sec.  This is
not a problem for testing as the test cases use loopback interface.  However,
you should be careful to reset TCP_WAIT timeout to the default setting
afterwards.

An another way, on Linux, is to enable TCP_TW_REUSE.  This is done by writing
`1` to `/proc/sys/net/ipv4/tcp_tw_reuse`.  This causes new TCP connections to
reuse the ports occupied by TCP connections in TCP_WAIT state.


Protocol Issues
===============

The specified protocol has issues, mainly breaking the REST semantics.  These
are observed in OnionAuth: during session creation the client has to
synchronously wait for HS1, HS2 messages.  Futhermore, there is no reply to
incoming HS2 message.  This could be solved by issues a request ID to each of
those requests and having the reply contain the ID of its corresponding request.

Security
========

The testing module uses Java Cryptography APIs for handling keys and using
cryptographic functions.  As part of this, you have to create a java keystore as
following:

1. Populate a file with a password:

        pwgen 16 1 > ~/.keystore_passwd
        
2. Create the keystore (default at ~/.keystore; can be changed with -keystore)
   and a keypair with (alias) name `pair1` and the common name (CN) for the
   corresponding certificate as `key1`:
   
        keytool -alias "pair1" -dname CN=key1 -genkeypair -keyalg RSA \
            -keysize 4096 -storepass:file .keystore_passwd
            
3. We also need a second key:
   
        keytool -alias "pair2" -dname CN=key2 -genkeypair -keyalg RSA \
            -keysize 4096 -storepass:file .keystore_passwd
        
4. List the certificates:

        keytool -list -storepass:file ~/.keystore_passwd
