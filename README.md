16onions
===============

Module: Onion

Team number: 16

Team name: 16onions

Team members: Charlie Groh, Josef Stark

The tests for this module are in com/voidphone/testing. The Unit-Test (Testing.java) tests multiple artificial situations. All of them should pass. The Real-World-Test (Test.java) starts multiple Peers and tries to transmit data through multiple tunnels at the same time. At the end, it prints how many transmissions were successful. Unfortunately, it often fails.

It may be necessary to hard code the keystore path (the variable is called propertiesPath in SecurityHelper.java) in order to avoid ugly Exceptions.