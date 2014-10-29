package com.terradue.ipft2;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.beust.jcommander.JCommander;

/*
 * Project:       GeoTIFF Uploader
 * Author:        $Author: stripodi $ (Terradue Srl)
 * Last update:   $Date: 2012-02-09 16:42:05 +0100 (Thu, 09 Feb 2012) $
 * Element:       ify web portal
 * Context:       services/gtu
 * Name:          service.js
 * Description:   Specific Javascript for gtu. It makes the _size optional
 *
 * This document is the property of Terradue and contains information directly
 * resulting from knowledge and experience of Terradue.
 * Any changes to this code is forbidden without written consent from Terradue Srl
 *
 * Contact: info@terradue.com
 */

public final class Main
{

    /**
     * @param args
     */
    public static void main( String[] args )
        throws Exception
    {
        IpfT2 ipfT2 = new IpfT2();

        JCommander commander = new JCommander( ipfT2 );
        commander.setProgramName( System.getProperty( "app.name" ) );
        commander.parse( args );

        if ( ipfT2.printHelp )
        {
            commander.usage();
            System.exit( -1 );
        }

        if ( ipfT2.debug )
        {
            System.setProperty( "logging.level", "DEBUG" );
        }
        else
        {
            System.setProperty( "logging.level", "INFO" );
        }

        // assume SLF4J is bound to logback in the current environment
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        try
        {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext( lc );
            // the context was probably already configured by default configuration
            // rules
            lc.reset();
            configurator.doConfigure( Main.class.getClassLoader().getResourceAsStream( "logback-config.xml" ) );
        }
        catch ( JoranException je )
        {
            // StatusPrinter should handle this
        }

        ipfT2.execute();
    }

}
