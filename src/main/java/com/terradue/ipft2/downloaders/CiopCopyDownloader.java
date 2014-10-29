package com.terradue.ipft2.downloaders;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.terradue.jcatalogue.client.download.DownloadHandler;
import com.terradue.jcatalogue.client.download.Downloader;
import com.terradue.jcatalogue.client.download.Protocol;

@Protocol( { "ftp", "scp", "http", "https", "gridftp", "gsiftp", "file", "nfs", "s3" } )
public final class CiopCopyDownloader
    implements Downloader
{
	
	public CiopCopyDownloader () {
		logger.info("Initializing Ciop-copy");
	}
	
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    
    @Override
    public <T> T download( final File targetDir, final URI location, final DownloadHandler<T> handler )
    {
        final CommandLine ciopcopy = new CommandLine( "ciop-copy" );
        ciopcopy.addArgument( "-s" );
        ciopcopy.addArgument( "-c" );
        ciopcopy.addArgument( "-o" );       
        ciopcopy.addArgument( targetDir.getAbsolutePath() );
        ciopcopy.addArgument( location.toString() );

        logger.info( "Invoking `{}` to download the product", ciopcopy.toString() );
        logger.info( "with user " + System.getProperty("user.name"));
        logger.info( "and home " + System.getProperty("user.home"));

        try
        {
            int exitValue = new DefaultExecutor().execute( ciopcopy );
            logger.info( "exit status " + exitValue);
            if ( exitValue == 0)
            {
                String fileName = location.getPath().substring( location.getPath().lastIndexOf( '/' ) + 1 );
                File targetFile = new File( targetDir, fileName );

                return handler.onCompleted( targetFile );
            }
            else // fail
            {
                handler.onError( format( "ciop-copy command %s failed, exited with code: %s",
                                         ciopcopy.getExecutable(), exitValue ) );
            }
        }
        catch ( ExecuteException e )
        {
            handler.onError( format( "Impossible to execute ciopcopy command %s: %s",
                                     ciopcopy.getExecutable(), e.getMessage() ) );
        }
        catch ( IOException e )
        {
            handler.onError( format( "An error occurred while downloading %s: %s", location, e.getMessage() ) );
        }

        return null;
    }

}
