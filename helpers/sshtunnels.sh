#!/bin/bash

ssh \
  -L 127.0.0.2:8443:nedspen-blue-ap.i.x-road.rocks:8443 \
  -L 127.0.0.3:8443:nedspen-red-ap.i.x-road.rocks:8443 \
  -L 127.0.0.4:8443:nedspen-blue-smp.i.x-road.rocks:8443 \
  -L 127.0.0.5:8443:nedspen-red-smp.i.x-road.rocks:8443 \
  -L 127.0.0.6:8080:nedspen-sml.i.x-road.rocks:8080 \
  bastion.x-road.rocks

