# BEAM

[![Build Status](https://travis-ci.org/LBNL-UCB-STI/beam.svg?branch=master)](https://travis-ci.org/LBNL-UCB-STI/beam) [all branches](BuildStatus.md)

The Framework for Modeling Behavior, Energy, Autonomy, and Mobility in Transportation Systems

BEAM extends the [Multi-Agent Transportation Simulation Framework](https://github.com/matsim-org/matsim) (MATSim)
to enable powerful and scalable analysis of urban transportation systems.

## Build
BEAM is a gradle project. To build the project, you are required to run the following command:
```
gradle build
```
It will generate a shadow jar with all dependencies inside build/libs directory.

## Run
Once the `beam.jar` is ready. You can run beam by executing the following command.
```
java -Xmx2g -jar build/libs/beam.jar --config test/input/beamville/beam.conf
```

> You need to set an environment variable named `PWD` to BEAM home.


## Deploy
To run BEAM simulation on amazon ec2, use following command with some optional parameters.
```
gradle deploy
```
 It can take some parameters from command line, use `-P` to specify the parameter.
 
 - `beamBranch`: To specify the branch for simulation, master is default branch.
 - `beamCommit`: The commit SHA to run simulation. use `HEAD` if you want to run with latest commit.
 - `beamConfigs`: A comma `,` separated list of `beam.conf` file names. It should be relative path under the project home.
 - `shutdownWait`: As simulation ends, ec2 instance would automatically terminate. In case you want to use the instance, please specify the wait in minutes, default wait is 30 min. 
 
 To access the ec2 instance, a proper certificate from admin and DNS is required. DNS of ec2 instance can found in the output log of the command.
 
 To run batch simulation you can specify the conf files using parameter like:
 ```
 gradle deploy -PbeamConfigs=production/application-sfbay/base.conf,production/application-sfbay/beamB.conf,production/application-sfbay/beamC.conf,production/application-sfbay/beamD.conf
 ```
 It will start an ec2 instance, using provided configurations and run all simulations in serial. At the end of each simulation it uploads the results to s3.
 
> gradle.properties contains default values for all the parameters.

## Documentation
BEAM is documented on [readthedocs](http://beam.readthedocs.io/en/akka/)

## Project website: 
http://beam.lbl.gov/

