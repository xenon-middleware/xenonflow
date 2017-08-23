# Build with:
#
# docker build -t nlesc/xenon-slurm:2.6.5 .
#
# Run with:
#
# docker run -d --name=xenon-slurm nlesc/xenon-slurm
#
# Test with:
#
# ssh xenon@<ip of xenon-slurm container>

FROM phusion/baseimage:0.9.22
MAINTAINER Berend Weel "b.weel@esciencecenter.nl"

RUN rm -f /etc/service/sshd/down
RUN /etc/my_init.d/00_regen_ssh_host_keys.sh

# Upgrade OS
RUN apt-get update && \
  apt-get install -y slurm-llnl python3 git python-pip dos2unix

# Create a test user
RUN /usr/sbin/useradd -p $(openssl passwd javagat) -d /home/xenon -m --shell /bin/bash xenon

# Add it to sudoers list
RUN usermod -aG sudo xenon
WORKDIR /home/xenon

COPY munge.key /etc/munge/munge.key
COPY service /etc/service
RUN dos2unix /etc/service/**/run
# Key was generated with openssl genrsa -out slurm.key 1024 && openssl rsa -in slurm.key -pubout -out slurm.cert
COPY etc /etc/slurm-llnl

RUN mkdir /var/run/munge && \
  mkdir -p /var/spool/slurmctld/state /var/spool/slurmd.node-0 /var/spool/slurmd.node-1 && \
  chmod 600 /etc/munge/munge.key && \
  chown root.root /var/lib/munge /etc/munge && \
  touch /var/spool/slurmctld/accounting.txt /var/spool/slurmctld/job_completions.txt /var/spool/slurmctld/state/recv_state && \
  chown slurm -R /var/spool/slurmctld /etc/slurm-llnl/slurm.key

# Clean up install
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

CMD ["/sbin/my_init"]
