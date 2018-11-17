# Comment-Analyzer
Performs simple word-count analysis on bulk reservation commentary data, provided a .CSV-formatted list of comments.

**How to Use**
This is a standalone program and requires no setup aside from modifying its configuration file.
1. Place the executable .JAR file in a directory where it can read and write files.

:warning: Caution: The program creates or overwrites a file called `config.cfg` in its working directory. If you have any data in a file by this name in the same directory where you put the .JAR it will be overwritten.

:warning: Caution: The program creates or overwrites a file called `results.txt` in its working directory. If you have any data in a file by this name in the same directory where you put the .JAR it will be overwritten.

2. Execute the .JAR. This will spawn a file called `config.cfg` which is human-readable.

3. Open and edit the configuration file. The auto-generated file contains detailed instructions on how to use its various directives. At minimum your config file must contain a `#SOURCE` directive and at least one `#TYPE` directive. You may include other directive and notes as you see fit.

4. Execute the .JAR. During computation the program will spawn a file called `results.txt`. Beware that this file is written progressively as the input data is processed. Allow sufficient time for the program to finish execution before opening the results file. Each time the program is executed this file is overwritten.

5. After completing execution the results file contains all of the program output.

**Troubleshooting**
If the program encounters a problem during execution it will attempt to write an error message to the results file. If for some reason the error file is empty or will not materialize then try running the program in a console environment. Error messages are printed to the system console as well as to file, and if there are issues writing the results file a complete stack trace is printed to the system console.
