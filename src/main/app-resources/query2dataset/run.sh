#!/bin/bash

# source the ciop functions (e.g. ciop-log)
source ${ciop_job_include}

SUCCESS=0
ERR_PUBLISH=2

function cleanExit ()
{
   local retval=$?
   local msg=""
   case "$retval" in
     $SUCCESS)      msg="Processing successfully concluded";;
     $ERR_PUBLISH)  msg="Could not publish find $DIR/params";;
     *)             msg="Unknown error";;
   esac
   [ "$retval" != "0" ] && ciop-log "ERROR" "Error $retval - $msg, processing aborted" "pf-asar" || ciop-log "ERROR" "$msg"
   exit $retval
}
trap cleanExit EXIT

while read query
do
    ciop-log "INFO" "Query: $query"
	for dataset in `ciop-casmeta $query -f rdf:about`
	do
		ciop-log "INFO" "Dataset $dataset"
		echo "$dataset" | ciop-publish -s
		[ "$?" != "0" ] && $ERR_PUBLISH
	done 
done

exit 0
