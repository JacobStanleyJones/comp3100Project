# COMP3100 Project | Jacob Stanley-Jones (45902240)
This repository contains my Client for the COMP3100 project.  
It is designed to connect to ds-server which you can find here:  
https://github.com/distsys-MQ/ds-sim  

---
## How to run
1. Run server from ds-sim `$ ds-server -n`
2. Run client `$ java MyClient [LRR | FC | FA]`

### Largest Round Robin (LRR)  
Schedules all jobs to the largest (most cores) server in a round robin fashion.  

### First Capable (FC)  
Schedules a job to the first server in the response to GETS Capable regardless of how many running and waiting jobs there are.  

### First Available (FA)  
Schedules a job to the first available server. If there aren't any available servers, it schedules to the first capable server.  
This algorithm is optimised to reduce the turnaround time.
