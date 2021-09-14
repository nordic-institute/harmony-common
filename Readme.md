# Harmony common repository

This repository contains tools to help build and package NIIS Harmony eDelivery packages and to set up development and
testing environments.

## Setting up environments using Ansible scripts

Ansible scripts for setting up development and testing environments are in `ansible` folder. Example commands below
assume that this is current folder.

AWS environment is set up using `hosts/aws_neds_test.txt` host file. Local LXD environment can be set up using
`/hosts/local_lxd.txt` file.

Command to set up local LXC environment is shown below.

    ansible-playbook --ask-become-pass -i hosts/local_lxd.txt install_all.yml

`--ask-become-pass` switch is needed so created container ip-addresses can be
automatically added to `/etc/hosts` file. If you dont want that to happen this switch can be omitted. This switch has no effect
in AWS environment.

Ansible scripts install and configure needed dependencies (Java runtime, MySQL, Tomcat). Initialise MySQL database and
load initial configuration into it. Finally application war is deployed in Tomcat container. On local LXD environment 
MySQL is listening on all interfaces so access to database is possible without any special arrangements.

## Setting up AWS testing environment

AWS Cloudformation stack for eDelivery testing is at `cloudformation/neds.yaml`. With this file Cloudformation stack
can be created using AWS Console. Make sure that you have access to SSH key used to create instances and that instance
hostnames match the ones configured in `hosts/aws_neds_test.txt`. Adjust as necessary. For creating or recreating
initial test environment default values should be suitable. When setting up AWS environment user user ubuntu ie add `-u ubuntu`
to ansible commandline.

## SMP users and credentials

Initially SMP is configured with three users - system, smp and user. Initial password for all users is 123456. Different
user roles have different permissions. Note that system user can not do smp user actions and vice versa.