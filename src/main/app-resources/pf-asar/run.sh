#!/bin/env bash

# ! / b i n / bash

# source the CIOP tools
source ${ciop_job_include}

# LD override
#export LD_LIBRARY_PATH=$_CIOP_APPLICATION_PATH/share/asarRT/lib:$LD_LIBRARY_PATH

# temporary settings for testing purposes
#TMPDIR=/tmp/1 #$_JOB_ID

mkdir -p $TMPDIR

SUCCESS=0
ERR_NOPARAMS=9
ERR_NOTASAIM=15
ERR_RUN=30
ERR_XMLLINT=31
ERR_TT=32
ERR_JAVAHOME=33
ERR_NOJAVA=34
ERR_RUN=35
ERR_CATJAR=36
ERR_NORT=37
ERR_NOJO=38
ERR_LIST=39
ERR_RPT=40
ERR_NOPROD=41
ERR_TGZ=42
DEBUG_EXIT=66

runrescode=0;
runmessage='';

function cleanExit ()
{
   local retval=$?
   local msg=""
   case "$retval" in
     $SUCCESS)      msg="Processing successfully concluded";;
     $ERR_NOPARAMS) msg="Could not find $DIR/params";;
     $ERR_XMLLINT)  msg="xmllint was not found in path";;
     $ERR_TT)       msg="TaskTable.xml is missing or invalid";;
     $ERR_JAVAHOME) msg="\$JAVA_HOME not defined in environment";;
     $ERR_NOJAVA)   msg="java binary not found in path";;
     $ERR_RUN)      msg="ipf-t2-0.1-SNAPSHOT.jar returned an error ($runrescode $runmessage)";;
     $ERR_CATJAR)   msg="jcatalogue-client[ver].jar or directory containing jars was not found under $DIR";;
     $ERR_NORT)     msg="$_CIOP_APPLICATION_PATH/share/asarRT not found or not successfully packed out";;
     $ERR_NOJO)     msg="ipf-t2 failed to generate $EOGRID_TMP/joborder.sh";;
     $ERR_LIST)     msg="AsarProducts.LIST not generated";;
     $ERR_RPT)      msg="ProductReport.rpt not generated";;
     $ERR_NOPROD)   msg="Level 0 product not found in joborder, tasktable-to-series mismatch?";;
     $ERR_TGZ)      msg="Could not create tgz of products, joborder, and report";;
     $ERR_NOTASAIM) msg="The product provided is not an ASA_IM__0P";;
     $DEBUG_EXIT)   msg="Breaking on debug exit";;
     *)             msg="Unknown error";;
   esac
   [ "$retval" != "0" ] && ciop-log "ERROR" "Error $retval - $msg, processing aborted" "pf-asar" || ciop-log "ERROR" "$msg" "pf-asar"
   exit $retval
}
trap cleanExit EXIT

# get the params for the job
task_table=`ciop-getparam task_table`.xml
[ $? != 0 ] && exit $ERR_MISSING_TT_PARAM

Product_Counter=`ciop-getparam product_counter`
[ $? != 0 ] && exit $ERR_MISSING_PC_PARAM

Processing_Stage_Flag=`ciop-getparam processing_stage_flag`
[ $? != 0 ] && exit $ERR_MISSING_PSF_PARAM

originator_ID=`ciop-getparam originator_id`
[ $? != 0 ] && exit $ERR_MISSING_OI_PARAM

aux_catalogue="`ciop-getparam aux_catalogue`"
[ $? != 0 ] && exit $ERR_MISSINGAUXCAT

#[ -e params ] && . params || exit $ERR_NOPARAMS
LINTBIN=`which xmllint`
[[ -x "$LINTBIN" ]] || exit $ERR_XMLLINT

# temporary location
EOGRID_LOG=$TMPDIR

# output location
OUTPUTDIR=$TMPDIR/output
mkdir -p $OUTPUTDIR

ciop-log "INFO" "task table: $task_table"
#set -x
#cat "/application/pf-asar/etc/$task_table" | $LINTBIN --format - > "$TMPDIR/TaskTable.xml" 2>"$EOGRID_LOG/TaskTable.validation.errs"
#[[ -s "$EOGRID_LOG/TaskTable.validation.errs" ]] && exit $ERR_TT

# switch to the validated tasktable
#task_table=$TMPDIR/TaskTable.xml
#[[ -s "$task_table" ]] || exit $ERR_TT


# environment for asarRT
export MDAC=$_CIOP_APPLICATION_PATH/share/asarRT
[[ -d "$MDAC" ]] || exit $ERR_NORT
export MDA_CONFIGURE=$MDAC
export PATH=$MDAC/bin:$MDAC/tools:$PATH
export LD_LIBRARY_PATH=$MDAC/lib:$LD_LIBRARY_PATH

#ipf-t2-0.1-SNAPSHOT/bin not needed
# sanity checks
[[ -z "$JAVA_HOME" ]] && exit $ERR_JAVAHOME
JAVABIN=`which java`
[[ -x "$JAVABIN" ]] || exit $ERR_NOJAVA

JLIBDIR=`find $_CIOP_APPLICATION_PATH/pf-asar/lib -type f -a -name "jcatalogue-client-*.jar"`
[[ -f "$JLIBDIR" ]] || exit $ERR_CATJAR
JLIBDIR=${JLIBDIR%/*.jar}
[[ -d "$JLIBDIR" ]] || exit $ERR_CATJAR

CLASSPATH="$JLIBDIR"/jcatalogue-client-0.9-SNAPSHOT.jar:"$JLIBDIR"/async-http-client-1.7.2.jar:"$JLIBDIR"/netty-3.3.1.Final.jar:"$JLIBDIR"/not-yet-commons-ssl-0.3.11.jar:"$JLIBDIR"/slf4j-api-1.6.4.jar:"$JLIBDIR"/jcommander-1.23.jar:"$JLIBDIR"/logback-classic-0.9.28.jar:"$JLIBDIR"/logback-core-0.9.28.jar:"$JLIBDIR"/cocoon-pipeline-3.0.0-alpha-3.jar:"$JLIBDIR"/commons-logging-1.1.1.jar:"$JLIBDIR"/cocoon-sax-3.0.0-alpha-3.jar:"$JLIBDIR"/cocoon-xml-2.0.2.jar:"$JLIBDIR"/commons-digester3-3.1.jar:"$JLIBDIR"/commons-beanutils-1.8.3.jar:"$JLIBDIR"/xalan-2.7.1.jar:"$JLIBDIR"/serializer-2.7.1.jar:"$JLIBDIR"/dcs-pf-asar-0.1-SNAPSHOT-ipf-t2.jar:"$JLIBDIR"/aalto-xml-0.9.8.jar:"$JLIBDIR"/wstx-asl-3.2.6.jar:"$JLIBDIR"/stax2-api-3.0.3.jar:"$JLIBDIR"/commons-exec-1.1.jar
EXTRA_JVM_ARGUMENTS="-DIPF-T2_HOME=$MDAC -Xms500m -Xmx500m -XX:PermSize=128m -XX:-UseGCOverheadLimit"

while read product
do
  # output location
  OUTPUTDIR=$TMPDIR/output
  mkdir -p $OUTPUTDIR
  cat "/application/pf-asar/etc/$task_table" | $LINTBIN --format - > "$TMPDIR/TaskTable.xml" 2>"$EOGRID_LOG/TaskTable.validation.errs"
  [[ -s "$EOGRID_LOG/TaskTable.validation.errs" ]] && exit $ERR_TT

  # switch to the validated tasktable
  local_task_table=$TMPDIR/TaskTable.xml
  [[ -s "$local_task_table" ]] || exit $ERR_TT
  # check if it's an ASA_IM__0P product
  ciop-log "INFO" "Product $product"
  rdfproduct=${product/%atom/rdf}
  prefix="`ciop-casmeta -f "dc:identifier" $rdfproduct | cut -c 1-9`"
  [ "$prefix" != "ASA_IM__0" ] && exit $ERR_NOTASAIM

  product=${product/%rdf/atom}

  ciop-log "INFO" "Generating joborder with ipf-t2 for product $product"

  # ipf-t2 is a java class that queries the catalogue and produces the joborder.sh
  $JAVABIN $JAVA_OPTS \
      $EXTRA_JVM_ARGUMENTS \
      -classpath "$CLASSPATH" \
      -Dapp.name="ipf-t2" \
      -Dapp.pid="$$" \
      -Dapp.repo="$JLIBDIR" \
      -Dbasedir="$BASEDIR" \
      com.terradue.ipft2.Main \
      -t $local_task_table -p $product \
      -PProduct_Counter="$Product_Counter" \
      -PProcessing_Stage_Flag="$Processing_Stage_Flag" \
      -Poriginator_ID="$originator_ID" -o $TMPDIR  \
      -X \
	--aux "$aux_catalogue" 1>&2
  #    --aux "http://10.16.10.51/catalogue/sandbox/description" 1>&2

  runrescode="$?"
  [[ -s "$EOGRID_LOG/genjo.ipf-t2.log" ]] && runmessage=`cat $EOGRID_LOG/genjo.ipf-t2.log`
  [[ "$runrescode" == "0" ]] || exit $ERR_RUN

  [[ -s "$TMPDIR/joborder.sh" ]] || exit $ERR_NOJO

   # run the joborder.sh with $EOGRID_TMP as current working directory
   cd $TMPDIR
   ciop-log "INFO" "Running asarRT v6.00 - $TMPDIR/joborder.sh "
   shortname=${product%/*} ;  shortname=${shortname##*/}

  ciop-log "DEBUG" "shortname: $shortname"
  # sanity check on joborder:
  prodinjo=`grep $shortname $TMPDIR/joborder.xml`
  [[ "X$prodinjo" == "X" ]] && exit $ERR_NOPROD
  chmod 775 $TMPDIR/joborder.sh

  ciop-log "INFO" "Process job order"

  $TMPDIR/joborder.sh
  [[ "$?" == "0" ]] || exit $ERR_JOFAIL
  [ ! -e "$TMPDIR/AsarProducts.LIST" ] && exit $ERR_LIST
  [ ! -e "$TMPDIR/ProductReport.rpt" ] && exit $ERR_RPT

  cp -f  $TMPDIR/joborder.xml $shortname.xml
  cp -f  ProductReport.rpt $shortname.rpt

  ciop-log "DEBUG" "AsarProducts `cat AsarProducts.LIST`"

  prodfiles=`cat AsarProducts.LIST`

  archive=`cat AsarProducts.LIST`
  tar cvfz $archive.tgz  $prodfiles $shortname.xml $shortname.rpt 1>&2
  [[ "$?" == "0" ]] || exit $ERR_TGZ
  [[ -s "$archive.tgz" ]] || exit $ERR_TGZ
  mv $TMPDIR/$archive.tgz $OUTPUTDIR/
  [[ -s "$OUTPUTDIR/$archive.tgz" ]] || exit $ERR_TGZ

  ciop-publish -m $OUTPUTDIR/$archive.tgz
  cd - 1>&2
  ##### end run of joborder.sh

   rm -fr $TMPDIR/*

done
# rm -fr $TMPDIR/*
