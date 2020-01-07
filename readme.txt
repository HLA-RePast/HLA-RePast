### HLA_RePast ###

# Installation #

HLA_RePast in its current form you was developed on 1.3 NG rti. This cversion i snow discontinued but there are other RTI nersions available.

In addition you need the Java bindings to the RTI, these are supplied in the lib folder and *are not* covered by the licence, 
unlike HLA_RePast itself.

A document describing the installation and usage of this RTI and its bindings can be found in the rti-setup-howto.txt file 
(although this is 100% specific to the original system setup at the School of Computer Science at the University of Birmingham. 
In short: your mileage may vary).

To compile HLA_RePast, ensure the jar files in the lib directory are in your classpath and compile from the src directory.


# Example Code #

Although this distribution contains a fairly large number of 'example' classes the only one that is currently in a state useable for learning from is the 
testrti.BasicTest class. This can be executed (after setting up the necessary java runtime variables as described in the rti-setup-howto.txt file) by executing 
launch-scripts/runbasictest.sh <number_of_federates> from the root of your where your classes have compiled to.

Each federate can then be controlled via the standard RePast VCR-style interface. A 'stop' (square) or 'exit' (cross) command will attempt to shut down the given 
federate gracefully.


# Modifying and Debugging #

Please see the code_overview.txt for a fairly complete overview of the entire codebase and where to find the bit of functionality you might be looking for.


# End Note #



