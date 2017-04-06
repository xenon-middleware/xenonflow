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

FROM phusion/baseimage:0.9.17
MAINTAINER Stefan Verhoeven "s.verhoeven@esciencecenter.nl"

RUN rm -f /etc/service/sshd/down
RUN /etc/my_init.d/00_regen_ssh_host_keys.sh

# Upgrade OS
RUN apt-get update && \
  apt-get install -y slurm-llnl python3 git python-pip dos2unix && \
  pip install virtualenv

# Create a test user
RUN /usr/sbin/useradd -p $(openssl passwd javagat) -d /home/xenon -m --shell /bin/bash xenon

# Add it to sudoers list
RUN usermod -aG sudo xenon
WORKDIR /home/xenon
COPY config.ini /home/xenon/.simcity_client
RUN mkdir in out && chown -R xenon in out

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

USER xenon
RUN virtualenv --python=/usr/bin/python3 simcity && . simcity/bin/activate && \
  git clone https://github.com/indodutch/sim-city-client.git && \
  cd sim-city-client && pip install -U pip && pip install .

# Add the dependencies for your simulation here
USER root
RUN export DEBIAN_FRONTEND=noninteractive && apt-get install -y openjdk-7-jre

# Clean up install
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

COPY simulations/ /home/xenon/simulations/
RUN chown -R xenon /home/xenon/simulations

CMD ["/sbin/my_init"]
