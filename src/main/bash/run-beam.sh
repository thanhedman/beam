#!/bin/bash

java -cp . -Xmx30g -XX:+HeapDumpOnOutOfMemoryError -jar build/libs/beam.jar "$BEAM_SHARED_INPUTS/../beam-core/" "model-inputs/calibration-v2/config.xml" "$BEAM_OUTPUTS" 
