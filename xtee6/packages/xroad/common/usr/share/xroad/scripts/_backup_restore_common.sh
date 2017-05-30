#/bin/bash
# Helper functions and common variables for the backup and restore scripts of X-Road.

# XXX Don't change this file name without a reason -- that will break backwards compatibilty
# with existing tarballs because the restore scripts expect to find a file with this
# name after unpacking the tarball.
umask 007
DATABASE_DUMP_FILENAME="/var/lib/xroad/dbdump.dat"

DATABASE_BACKUP_SCRIPT="/usr/share/xroad/scripts/backup_db.sh"
DATABASE_RESTORE_SCRIPT="/usr/share/xroad/scripts/restore_db.sh"
COMMON_BACKUP_SCRIPT="/usr/share/xroad/scripts/_backup_xroad.sh"

# This version number must be increased when we introduce changes that make
# earlier backup files incompatible with the current system.
XROAD_VERSION_LABEL="XROAD_6.8"

die () {
    echo >&2 "$@"
    exit 1
}

check_user () {
  if [ "$(id -nu )" != "xroad" ] ; then
    echo "This script (${THIS_FILE}) must be run by the xroad user"
    exit 2
  fi
}

check_instance_id () {
  if [[ -z ${FORCE_RESTORE} && -z "${INSTANCE_ID}" ]] ; then
    echo "Missing value of instance ID"
    usage
    exit 2
  fi
  if [[ $USE_BASE_64 = true ]] ; then
    INSTANCE_ID=$(echo $INSTANCE_ID | base64 --decode)
  fi
}

check_central_ha_node_name () {
  if [ -z ${FORCE_RESTORE} ] ; then
    # Look for BDR-patched Postgres 9.4 in order to detect HA support.
    dpkg -l | cut -d ' ' -f 3 | grep postgresql-bdr-9.4 2>&1 >/dev/null
    if [ $? -eq 0 ] ; then
      if [ -z "$CENTRAL_SERVER_HA_NODE_NAME" ] ; then
        echo "Missing value of HA node name but postgresql-bdr-9.4 is installed"
        usage
        exit 2
      fi
    else
      if [ -n "$CENTRAL_SERVER_HA_NODE_NAME" ] ; then
        echo "Not expecting HA node name if postgresql-bdr-9.4 is not installed"
        usage
        exit 2
      fi
    fi
    if [[ $USE_BASE_64 = true ]] ; then
      CENTRAL_SERVER_HA_NODE_NAME=$(echo $CENTRAL_SERVER_HA_NODE_NAME | base64 --decode)
    fi
  fi
}

check_security_server_id () {
  if [[ -z ${FORCE_RESTORE} && -z "${SECURITY_SERVER_ID}" ]] ; then
    echo "Missing value of security server ID"
    usage
    exit 2
  fi
  if [[ $USE_BASE_64 = true ]] ; then
    SECURITY_SERVER_ID=$(echo $SECURITY_SERVER_ID | base64 --decode)
  fi
}

check_backup_file_name () {
  if [ -z "${BACKUP_FILENAME}" ] ; then
    echo "Missing value of backup tar file name"
    usage
    exit 2
  fi
  if [[ $USE_BASE_64 = true ]] ; then
    BACKUP_FILENAME=$(echo $BACKUP_FILENAME | base64 --decode)
  fi
}

check_server_type () {
  # We don't support base64 in the type of server because the allowed values
  # are a predefined set anyway.
  case ${SERVER_TYPE} in
    security)
      if [[ -z ${FORCE_RESTORE} && -z ${SECURITY_SERVER_ID} ]] ; then
        die "Missing security server ID -- did you use the correct wrapper script?"
      fi
      ;;
    central)
      ;;
    *)
      die "Invalid server type -- did you use the correct wrapper script?"
      ;;
  esac
}

# XXX The tarball label is simply an underscore-separated list of the input
# parameters.
make_tarball_label () {
  case ${SERVER_TYPE} in
    security)
      TARBALL_LABEL="security_${XROAD_VERSION_LABEL}_${SECURITY_SERVER_ID}"
      ;;
    central)
      TARBALL_LABEL="central_${XROAD_VERSION_LABEL}_${INSTANCE_ID}"
      if [ -n "${CENTRAL_SERVER_HA_NODE_NAME}" ] ; then
        TARBALL_LABEL="${TARBALL_LABEL}_${CENTRAL_SERVER_HA_NODE_NAME}"
      fi
      ;;
    *)
      # The type of server has been checked already.
      ;;
  esac
}

has_command () {
    command -v $1 &>/dev/null
}

# vim: ts=2 sw=2 sts=2 et filetype=sh
