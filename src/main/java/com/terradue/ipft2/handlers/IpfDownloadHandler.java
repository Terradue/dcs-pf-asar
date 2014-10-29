package com.terradue.ipft2.handlers;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.terradue.jcatalogue.client.download.DownloadHandler;

public final class IpfDownloadHandler
    implements DownloadHandler<String>
{

    private static final Logger logger = LoggerFactory.getLogger( IpfDownloadHandler.class );

    @Override
    public void onError( Throwable t )
    {
        logger.error( t.getMessage() );
    }

    @Override
    public void onError( String message )
    {
        logger.error( message );
    }

    @Override
    public void onWarning( String message )
    {
        logger.warn( message );
    }

    @Override
    public void onFatal( String message )
    {
        logger.error( message );
    }

    @Override
    public String onCompleted( File file )
    {
        logger.info( "File {} successfully downloaded in {}", file.getName(), file.getParent() );
        return file.getAbsolutePath();
    }

    @Override
    public void onContentDownloadProgress( long current, long total )
    {
        System.out.printf( "%s%%%n", ( current * 100 ) / total );
    }

}
