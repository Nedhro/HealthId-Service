FROM centos:6.6

COPY healthId-api/build/distributions/healthId-*.noarch.rpm /tmp/healthId.rpm
RUN yum install -y java /tmp/healthId.rpm && rm -f /tmp/healthId.rpm && yum clean all
COPY env/docker_healthid /etc/default/healthid
ENTRYPOINT . /etc/default/healthid && java -jar /opt/healthid/lib/healthId-schema-*.jar && java -Dserver.port=$HEALTH_ID_SERVICE_PORT -DHID_LOG_LEVEL=$HID_LOG_LEVEL -jar  /opt/healthid/lib/healthId-api.war

