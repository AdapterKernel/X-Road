#!/bin/bash

# FIXME: error handling
DUMP_FILE=$1

PW=$(crudini --get /etc/xroad/db.properties '' serverconf.hibernate.connection.password)
USER=$(crudini --get /etc/xroad/db.properties '' serverconf.hibernate.connection.username)
PORT=$(crudini --get /etc/xroad/db.properties '' serverconf.hibernate.connection.url | cut -d '/' -f 3 | cut -d ':' -f2)
PGPASSWORD=${PW:-serverconf} pg_dump -F t -h 127.0.0.1 -p ${PORT:-5432} -U ${USER:-serverconf} -f ${DUMP_FILE} serverconf

