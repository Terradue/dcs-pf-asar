package com.terradue.ipft2.xml;

import static com.terradue.ipft2.xml.SAXEventsBuilder.attribute;
import static com.terradue.ipft2.xml.SAXEventsBuilder.wrap;
import static java.lang.String.format;
import static org.apache.commons.beanutils.ConvertUtils.convert;
import static org.apache.commons.digester3.binder.DigesterLoader.newLoader;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cocoon.sax.AbstractSAXTransformer;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.terradue.ipft2.domain.Alternative;
import com.terradue.ipft2.domain.AlternativeList;
import com.terradue.ipft2.handlers.IpfDownloadHandler;
import com.terradue.jcatalogue.client.CatalogueClient;
import com.terradue.jcatalogue.client.DataSet;
import com.terradue.jcatalogue.client.Parameter;
import com.terradue.jcatalogue.client.Series;

public final class TaskTableAlternativesFilter
    extends AbstractSAXTransformer
{

    private static final String INPUT = "Input";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Parameter startDate;

    private final Parameter endDate;

    private final CatalogueClient catalogueClient;

    private final URI levelZeroProduct;

    private final URI auxCat;
    
    private final File targetDir;

    private final SimpleDateFormat dateFormat;

    private final boolean debug;

    private final Digester digester = newLoader( new FromAnnotationsRuleModule()
    {

        @Override
        protected void configureRules()
        {
            bindRulesFrom( AlternativeList.class );
        }

    } ).newDigester();

    // memento

    private boolean forwarding = true;

    public TaskTableAlternativesFilter( Parameter startDate,
                                        Parameter endDate,
                                        CatalogueClient catalogueClient,
                                        URI auxCat,
                                        URI levelZeroProduct,
                                        File targetDir,
                                        SimpleDateFormat dateFormat,
                                        boolean debug )
    {    	
        this.startDate = startDate;
        this.endDate = endDate;
        this.catalogueClient = catalogueClient;
        this.auxCat = auxCat;
        this.levelZeroProduct = levelZeroProduct;
        this.targetDir = targetDir;
        this.dateFormat = dateFormat;
        this.debug = debug;
    }

    @Override
    public void startElement( String uri, String localName, String name, Attributes atts )
        throws SAXException
    {
        if ( INPUT.equals( localName ) )
        {
            if ( logger.isInfoEnabled() )
            {
                logger.info( "%---------- Evaluating new alternatives ----------%" );
            }

            forwarding = false;
            digester.startDocument();
        }

        if ( forwarding )
        {
            super.startElement( uri, localName, name, atts );
        }
        else
        {
            digester.startElement( uri, localName, name, atts );
        }
    }

    @Override
    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        if ( forwarding )
        {
            super.characters( ch, start, length );
        }
        else
        {
            digester.characters( ch, start, length );
        }
    }

    @Override
    public void endElement( String uri, String localName, String name )
        throws SAXException
    {
        if ( forwarding )
        {
            super.endElement( uri, localName, name );
        }
        else
        {
            digester.endElement( uri, localName, name );
        }

        if ( INPUT.equals( localName ) )
        {
            digester.endDocument();

            AlternativeList alternatives = digester.getRoot();

            digester.clear();

            boolean found = false;
            int index = 1;
            int size = alternatives.size();
            logger.info( "NUMOF ALT " + size);
            while ( !alternatives.isEmpty() && !found )
            {
            	
                Alternative alternative = alternatives.nextAlternative();
                logger.info( "INPUTSOURCE= " + alternative.isInput_source_data() + " levelZeroProduct " + levelZeroProduct);
                URI fileTypeUri = null;
                if (alternative.isInput_source_data()) {
                	String fileTypePath = format( "../../%s/atom", alternative.getFileType() );
                    fileTypeUri = levelZeroProduct.resolve( fileTypePath );
                }else{
	                
	                if ( logger.isInfoEnabled() )
	                {
	                    logger.info( "Processing alternative {}/{}: {}", new Object[] { index, size, alternative } );
	                }
	
	                String fileTypePath = format( "%s/atom", alternative.getFileType() );
	                fileTypeUri = auxCat.resolve( fileTypePath );
	                logger.info( "Aux URLs = " + fileTypeUri.toString());
                }
                try
                {                	
                	Parameter sort = new Parameter("sort", "eop:processingDate,,descending");
                	Parameter count = new Parameter("count", "1");
                	
                    Series serie = catalogueClient.getSeries( fileTypeUri, startDate, endDate, sort, count);
                    if ( serie.getTotalResults() > 0 )
                    {
                        DataSet dataSet = serie.iterator().next();
                        SimpleDateFormat originDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
                        
                        
                        Date beginPosition = null;
                        Date endPosition = null;
                       
                        if (dataSet.getBeginPosition() == null) { //case of dc:date
                        	logger.info( "IT'S NOT GML");
                        	beginPosition = originDateFormat.parse(dataSet.getDatePosition().split("/")[0]);
                        	endPosition = originDateFormat.parse(dataSet.getDatePosition().split("/")[1]); 	            
                        }else{ //it's still gml:validTime            
                        	logger.info( "IT'S GML");
                        	beginPosition = dataSet.getBeginPosition();
                        	endPosition = dataSet.getEndPosition();
                        }
                        
                        String fileLocation = dataSet.download( targetDir, new IpfDownloadHandler() );

                        if ( fileLocation != null )
                        {
                            found = true;
                            logger.debug( "DATASET " + dataSet.getId());
                            logger.debug( "BEGIN = " + beginPosition);
                            fileLocation = fileLocation.replace(".gz", ""); //this is because ciop-copy unzip it
                            logger.info("FILE " + fileLocation);
                            wrap( getSAXConsumer() )
                            .start( INPUT )
                                .start( "List_of_Alternatives" )
                                    .start( "Alternative" )
                                      .start( "File_Type" ).body( alternative.getFileType() ).end()
                                      .start( "File_Name_Type" ).body( alternative.getFileNameType() ).end()
                                      .start( "Input_Source_Data" ).body( alternative.isInput_source_data() ).end()

                                      .start( "List_of_File_Names", attribute( "count", 1 ) )
                                          .start( "File_Name" ).body( fileLocation ).end()
                                      .end()

                                      .start( "List_of_Time_Intervals", attribute( "count", 1 ) )
                                          .start( "Time_Interval" )
                                              .start( "Start" ).body( dateFormat.format(beginPosition)).end()
                                              .start( "Stop" ).body( dateFormat.format(endPosition)).end()
                                              .start( "File_Name" ).body( fileLocation ).end()
                                          .end()
                                      .end()
                                    .end()
                                .end()
                            .end();
                        }
                    }
                }
                catch ( Throwable t )
                {
                    found = false;

                    if ( debug )
                    {
                        logger.warn( "Impossible to process the Alternative list [" + index + "]", t );
                    }
                    else
                    {
                        logger.warn( "Impossible to process the Alternative list [{}]: {}", index, t.getMessage() );
                    }
                }

                index++;
            }

            if ( !found )
            {
                if ( alternatives.isMandatory() )
                {
                    throw new RuntimeException( "No one of the alternatives contain level 1 products that cover the given input" );
                }
                else if ( logger.isInfoEnabled() )
                {
                    logger.info( "%-------- No one of Alternatives found (but not mandatory) -------%" );
                }
            }
            else if ( logger.isInfoEnabled() )
            {
                logger.info( "%-------- Alternative list evaluation is successfully over -------%" );
            }

            forwarding = true;
        }
    }

}
