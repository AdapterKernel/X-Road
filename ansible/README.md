# X-Road installation instructions

## 1. Install Ansible

Install Ansible by following instructions at http://docs.ansible.com/ansible/intro_installation.html

## 2. Configuration options

The servers to be installed are written in the Ansible inventory (hosts-file). 
In the provided example command they are in `hosts/example_xroad_hosts.txt`. 
Host names in the file must be correct fully qualified host names because they are used 
in X-Road certificate generation and setting the hostname of the servers when installing. 
Do not use IP addresses.

You determine which servers are initialized by filling in the groups 
`cs-servers`, `ss-servers`, `cp-servers` and `ca-servers`. If you have no use for such servers,
you can leave that group empty.

Parameters such as the admin username and password, and CA server's distinguished name values, 
can be configured using files in `group_vars` directory. 
In the provided example the configuration file is `group_vars/example.yml`. 
The link between the inventory file and the variable definition file is based on (in this example) a group name. 
`example_xroad_hosts.txt` defines a host group
```
[example:children]
 cs-servers
 ss-servers
 cp-servers
 ca-server
```
containing all the hosts in the inventory. 
Ansible searches for variables from `group_vars/example.yml` when initializing hosts from "example" group.

## 3. Install X-Road using Ansible

```
ansible-playbook -i hosts/example_xroad_hosts.txt xroad_init.yml
```

## 4. Development using LXD

On Linux it is possible to use the LXD container hypervisor for fast testing and development.

First, make sure that you have LXD installed and configured as explained on https://linuxcontainers.org/lxd/getting-started-cli/. For example, on Ubuntu:

```
sudo apt-get install lxd

# Log in to the new group lxd (or logout/login)
newgrp lxd

# Configure lxd
sudo lxd init (use bridge network configuration and turn on nat)
```
Study the files hosts/lxd_hosts.txt and hosts/lxd_hosts_from_local.txt and change repository and host names if needed. The LXD hosts are first created with the name given. Under group lxd-servers is the host, normally localhost.

Install ansible to local LXD containers from public repository with:

```
ansible-playbook  -i hosts/lxd_hosts.txt xroad_init.yml
```

Compile your own X-Road, build packages and install X-Road from your own packages using playbook xroad_dev.yml.

First make sure that docker and docker-py are installed. Docker is used for building the deb packages on localhost.

```
ansible-playbook  -i hosts/lxd_hosts_from_local.txt xroad_dev.yml
```
### Partial compilation and deployment

For fast development, you can compile and install modules separately. The ansible playbook *xroad_dev_partial.yml* offers this. For example, if you make a change to a Java or Ruby file under proxy-ui, use the following command to compile the war and deploy it to the security servers.
```
ansible-playbook  -i hosts/lxd_hosts_from_local.txt   xroad_dev_partial.yml   -e selected_modules=proxy-ui
```

It is also possible to compile and install several modules (jars or wars). The following command compiles and installs common-util and signer jars and proxy-ui war to the correct servers.
```
ansible-playbook  -i hosts/lxd_hosts_from_local.txt   xroad_dev_partial.yml   -e selected_modules=common-util,proxy-ui,signer
```

The modules are listed in dicts common_modules.yml, cs_modules.yml, cp_modules.yml and ss_modules.yml.

Note that the playbook *xroad_dev_partial.yml* only copies jars and wars to the servers in correct locations. For the full installation, use *xroad_dev.yml*.


## 5. Test CA, TSA, and OCSP

More information about these [here.](TESTCA.md)
