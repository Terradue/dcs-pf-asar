package com.terradue.ipft2;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;
import static java.util.TimeZone.getTimeZone;
import static org.apache.cocoon.sax.builder.SAXPipelineBuilder.newNonCachingPipeline;
import static org.apache.commons.beanutils.ConvertUtils.convert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.cocoon.sax.component.XSLTTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.terradue.ipft2.converters.UriConverter;
import com.terradue.ipft2.downloaders.CiopCopyDownloader;
import com.terradue.ipft2.handlers.IpfDownloadHandler;
import com.terradue.ipft2.xml.ProcessingParametersTransformer;
import com.terradue.ipft2.xml.TaskTableAlternativesFilter;
import com.terradue.jcatalogue.client.CatalogueClient;
import com.terradue.jcatalogue.client.DataSet;
import com.terradue.jcatalogue.client.Parameter;

@Parameters( commandDescription = "TastTable to JobOrder processor" )
public final class IpfT2
{

    @com.beust.jcommander.Parameter( names = { "-h", "--help" }, description = "Display help information." )
    public boolean printHelp;

    @com.beust.jcommander.Parameter( names = { "-a", "--aux" }, description = "Aux files catalogue URL.", converter = UriConverter.class  )
    public URI auxCat;
    
    @com.beust.jcommander.Parameter( names = { "-v", "--version" }, description = "Display version information." )
    public boolean showVersion;

    @com.beust.jcommander.Parameter( names = { "-t", "--tasktable" }, description = "The tasktable XML file.", converter = FileConverter.class )
    public File taskTable = new File( new File( System.getProperty( "user.dir" ) ), "tasktable.xml" );

    @com.beust.jcommander.Parameter( names = { "-p", "--product" }, description = "The level 0 product.", converter = UriConverter.class )
    public URI levelZeroProduct;

    @com.beust.jcommander.Parameter( names = { "-o", "--output" }, description = "The output dir.", converter = FileConverter.class )
    public File output = new File( System.getProperty( "user.dir" ) );

    @com.beust.jcommander.Parameter( names = { "-X", "--debug" }, description = "Produce execution debug output." )
    public boolean debug;

    @com.beust.jcommander.Parameter( names = { "--key" }, description = "Private SSL key.", converter = FileConverter.class )
    public File key;

    @com.beust.jcommander.Parameter( names = { "--cert" }, description = "Public SSL certificate.", converter = FileConverter.class )
    public File cert;

    @com.beust.jcommander.Parameter( names = { "--password" }, description = "PEM pass phrase." )
    public String pemPassword = "";

    @com.beust.jcommander.Parameter( names = { "--um-sso-username" }, description = "UM-SSO username." )
    public String umSsoUsername = "";

    @com.beust.jcommander.Parameter( names = { "--um-sso-password" }, description = "UM-SSO password." )
    public String umSsoPassword = "";

    @DynamicParameter( names = "-P", description = "use value for given property" )
    public Map<String, String> dynaProperties = new HashMap<String, String>();

    public void execute()
    {
        if ( showVersion )
        {
            printVersionInfo();
        }

        if ( !output.exists() )
        {
            if ( !output.mkdirs() )
            {
                printAndExit( "Impossible to create output dir %s - check the current user has enough permissions%n",
                              taskTable );
            }
        }

        checkFile( taskTable, false, "TaskTable" );
        checkFile( key, true, "Private key" );
        checkFile( cert, true, "Public certificate" );

        final Logger logger = LoggerFactory.getLogger( getClass() );

        logger.info( "" );
        logger.info( "------------------------------------------------------------------------" );
        logger.info( "IPF-T2" );
        logger.info( "------------------------------------------------------------------------" );
        logger.info( "" );

        CatalogueClient.Configuration conf = new CatalogueClient.Configuration();
        logger.info("Initializing downloader");
        CatalogueClient catalogueClient = new CatalogueClient( conf.registerDownloader( new CiopCopyDownloader() ) );
        long start = currentTimeMillis();
        int exit = 0;

        Throwable error = null;
        try
        {
            DataSet levelZeroDataSet = catalogueClient.getDataSet( levelZeroProduct );
            String fileLocation = levelZeroDataSet.download( output, new IpfDownloadHandler() );
            if ( fileLocation == null )
            {
                throw new RuntimeException( "Impossible to download the input product level 0 media data" );
            }
            // date format used in the joborder

        //    final TimeZone gmtTimeZone = getTimeZone( "GMT" );
            SimpleDateFormat targetDateFormat = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS'000'" );
        //    targetDateFormat.setTimeZone( gmtTimeZone );
            // XSL parameters
            Map<String, Object> xslParameters = new HashMap<String, Object>();            
            SimpleDateFormat originDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
            
            Date beginPosition = null;
            Date endPosition = null;
            Parameter startDate = null;
            Parameter endDate = null;
            logger.info("testtest" + levelZeroDataSet.getBeginPosition());

            if (levelZeroDataSet.getBeginPosition() == null) { //case of dc:date
            	beginPosition = originDateFormat.parse(levelZeroDataSet.getDatePosition().split("/")[0]);
            	endPosition = originDateFormat.parse(levelZeroDataSet.getDatePosition().split("/")[1]);            
	            if (beginPosition != null)
	            	xslParameters.put( "level0.start", targetDateFormat.format(beginPosition) );
	            if(endPosition != null)
	            	xslParameters.put( "level0.stop", targetDateFormat.format(endPosition) );
	            startDate = new Parameter( "start", originDateFormat.format(beginPosition) );
	            endDate = new Parameter( "stop", originDateFormat.format(endPosition) );	            
            }else{ //it's still gml:validTime            
            	final TimeZone gmtTimeZone = getTimeZone( "GMT" );
                targetDateFormat.setTimeZone( gmtTimeZone );
	            startDate = new Parameter( "start", (String) convert( levelZeroDataSet.getBeginPosition(), String.class ) );
	            endDate = new Parameter( "stop", (String) convert( levelZeroDataSet.getEndPosition(), String.class ) );
	            xslParameters.put( "level0.start", targetDateFormat.format( levelZeroDataSet.getBeginPosition() ) );
	            xslParameters.put( "level0.stop", targetDateFormat.format( levelZeroDataSet.getEndPosition() ) );
            }
            xslParameters.put( "output.dir", output.getAbsolutePath() );
            XSLTTransformer task2joborder = new XSLTTransformer( Main.class.getResource( "xml/task2joborder.xsl" ) );
            task2joborder.setParameters( xslParameters );
            // Catalogue query date format

      /*      SimpleDateFormat queryDateFormat = new SimpleDateFormat( "yyyyMMdd_HHmmssSSS" );
            queryDateFormat.setTimeZone( gmtTimeZone );*/
            
           /* Parameter startDate = new Parameter( "start", originDateFormat.format(beginPosition) );
            Parameter endDate = new Parameter( "stop", originDateFormat.format(endPosition) );*/
            // run!

            newNonCachingPipeline()
                .setFileGenerator( taskTable )
                .addComponent( new ProcessingParametersTransformer( dynaProperties ) )
                .addComponent( new TaskTableAlternativesFilter( startDate,
                                                                endDate,                                                               
                                                                catalogueClient,
                                                                auxCat,
                                                                levelZeroProduct,
                                                                output,
                                                                targetDateFormat,
                                                                debug ) )
                .addComponent( task2joborder )
                .addSerializer()
                .setConfiguration( dynaProperties )
                .setup( new FileOutputStream( new File( output, "joborder.xml" ) ) )
                .execute();
            logger.info( "10" );
        }
        catch ( Throwable t )
        {
            exit = -1;
            logger.info( "ERROR IS : " + t.getMessage());
            if ( null == t.getCause() )
            {
                error = t;
            }
            else
            {
                error = t.getCause();
            }
        }
        finally
        {
            logger.info( "" );
            logger.info( "------------------------------------------------------------------------" );
            logger.info( "IPF-T2 {}", ( exit < 0 ) ? "FAILURE" : "SUCCESS" );

            if ( exit < 0 )
            {
                logger.info( "" );

                if ( debug )
                {
                    logger.error( "Execution terminated with errors", error );
                }
                else
                {
                    logger.error( "Execution terminated with errors: {}", error.getMessage() );
                }

                logger.info( "" );
            }

            logger.info( "Total time: {}s", ( ( currentTimeMillis() - start ) / 1000 ) );
            logger.info( "Finished at: {}", new Date() );

            final Runtime runtime = Runtime.getRuntime();
            final int megaUnit = 1024 * 1024;
            logger.info( "Final Memory: {}M/{}M", ( runtime.totalMemory() - runtime.freeMemory() ) / megaUnit,
                         runtime.totalMemory() / megaUnit );

            logger.info( "------------------------------------------------------------------------" );

            exit( exit );
        }
    }

    private static void checkFile( File file, boolean allowNullables, String parameterName )
    {
        if ( file != null )
        {
            if ( !file.exists() )
            {
                printAndExit( "%s %s file not found, please verify it exists", file, parameterName );
            }

            if ( file.isDirectory() )
            {
                printAndExit( "%s %s file must be not a directory", file, parameterName );
            }
        }
        else if ( !allowNullables )
        {
            printAndExit( "%s is required but not specified", parameterName );
        }
    }

    private static void printAndExit( String pattern, Object...arguments )
    {
        System.out.printf( pattern, arguments );
        System.exit( -1 );
    }

    private static void printVersionInfo()
    {
        Properties properties = new Properties();
        InputStream input =
            Main.class.getClassLoader().getResourceAsStream( "META-INF/maven/com.terradue/ipf-t2/pom.properties" );

        if ( input != null )
        {
            try
            {
                properties.load( input );
            }
            catch ( IOException e )
            {
                // ignore, just don't load the properties
            }
            finally
            {
                try
                {
                    input.close();
                }
                catch ( IOException e )
                {
                    // close quietly
                }
            }
        }

        System.out.printf( "%s %s (%s)%n",
                           properties.getProperty( "name" ),
                           properties.getProperty( "version" ),
                           properties.getProperty( "build" ) );
        System.out.printf( "Java version: %s, vendor: %s%n",
                           System.getProperty( "java.version" ),
                           System.getProperty( "java.vendor" ) );
        System.out.printf( "Java home: %s%n", System.getProperty( "java.home" ) );
        System.out.printf( "Default locale: %s_%s, platform encoding: %s%n",
                           System.getProperty( "user.language" ),
                           System.getProperty( "user.country" ),
                           System.getProperty( "sun.jnu.encoding" ) );
        System.out.printf( "OS name: \"%s\", version: \"%s\", arch: \"%s\", family: \"%s\"%n",
                           System.getProperty( "os.name" ),
                           System.getProperty( "os.version" ),
                           System.getProperty( "os.arch" ),
                           getOsFamily() );
    }

    private static final String getOsFamily()
    {
        String osName = System.getProperty( "os.name" ).toLowerCase();
        String pathSep = System.getProperty( "path.separator" );

        if ( osName.indexOf( "windows" ) != -1 )
        {
            return "windows";
        }
        else if ( osName.indexOf( "os/2" ) != -1 )
        {
            return "os/2";
        }
        else if ( osName.indexOf( "z/os" ) != -1 || osName.indexOf( "os/390" ) != -1 )
        {
            return "z/os";
        }
        else if ( osName.indexOf( "os/400" ) != -1 )
        {
            return "os/400";
        }
        else if ( pathSep.equals( ";" ) )
        {
            return "dos";
        }
        else if ( osName.indexOf( "mac" ) != -1 )
        {
            if ( osName.endsWith( "x" ) )
            {
                return "mac"; // MACOSX
            }
            return "unix";
        }
        else if ( osName.indexOf( "nonstop_kernel" ) != -1 )
        {
            return "tandem";
        }
        else if ( osName.indexOf( "openvms" ) != -1 )
        {
            return "openvms";
        }
        else if ( pathSep.equals( ":" ) )
        {
            return "unix";
        }

        return "undefined";
    }

}
