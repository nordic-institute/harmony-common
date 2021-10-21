# Harmony common repository

This repository contains tools to help build and package NIIS Harmony eDelivery packages and to set up development and
testing environments.

## Setting up AWS testing environment

AWS Cloudformation stack for eDelivery testing is at `cloudformation/neds.yaml`. With this file Cloudformation stack
can be created using AWS Console. Make sure that you have access to SSH key used to create instances and that instance
hostnames match the ones configured in `hosts/aws_neds_test.txt`. Adjust as necessary. For creating or recreating
initial test environment default values should be suitable. When setting up AWS environment user user ubuntu ie add `-u ubuntu`
to ansible commandline.

## Setting up environments using Ansible scripts

Ansible scripts for setting up development and testing environments are in `ansible` folder. Example commands below
assume that this is current folder. There are separate scripts for individual component types.

AWS environment is set up using `hosts/aws_neds_test.txt` host file. Local LXD environment can be set up using
`/hosts/local_lxd.txt` file.

Command to set up local LXC environment:

    ansible-playbook --ask-become-pass -i hosts/local_lxd.txt install_all.yml

`--ask-become-pass` switch (or shortly -K) is needed so created container ip-addresses can be
automatically added to `/etc/hosts` file. If you dont want that to happen this switch can be omitted. This switch is not
needed in AWS environment.

Command to set up AWS environment:

    ansible-playbook -i hosts/aws_neds_test.txt install_all.yml

Ansible scripts for original CEF conponents install and configure needed dependencies (Java runtime, MySQL, Tomcat).
Initialise MySQL database and load initial configuration into it. Encryption keys and certificates are generated on the
ansible host and then corresponding keystores created on target host.
Finally application war is deployed in Tomcat container. 

Ansible scripts for Harmony conponents use deb files produced by packaging/build-deb.sh script. Ansible script do not
run this script, this has to be done manually. After deb installation scripts repleace keystores with generated ones.

## SMP users and credentials

CEF SMP is initialised with three users - "system", "smp" and "user". Initial password for all users is "123456". Different
user roles have different permissions. Note that system user can not do smp user actions and vice versa.

Harmony SMP is initialised with one user - "admin" with password "admin".

## AP users and credentials

Domibus AP is initialised with two users "admin" and "user", password for both is "123456"

Harmony AP is initialised with one user - "admin" with password "admin".
