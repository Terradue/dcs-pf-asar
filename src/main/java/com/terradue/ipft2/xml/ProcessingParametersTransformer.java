package com.terradue.ipft2.xml;

import static com.terradue.ipft2.xml.SAXEventsBuilder.wrap;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.beanutils.ConvertUtils.register;
import static org.apache.commons.digester3.binder.DigesterLoader.newLoader;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cocoon.sax.AbstractSAXTransformer;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.StackAction;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.terradue.ipft2.converters.ParamTypeConverter;
import com.terradue.ipft2.domain.ParamType;
import com.terradue.ipft2.domain.ProcessingParameter;

public final class ProcessingParametersTransformer
    extends AbstractSAXTransformer
    implements StackAction
{

    private static final String PROCESSING_PARAMETERS = "Processing_Parameters";

    private static final String PROCESSING_PARAMETER = "Processing_Parameter";

    private static final String NAME = "Name";

    private static final String VALUE = "Value";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Pattern numberPattern = compile( "^[\\+|\\-]?[0-9]+[\\.]?[0-9]+$" );

    private final Map<String, String> dynaProperties;

    private final Digester digester = newLoader( new FromAnnotationsRuleModule()
    {

        @Override
        protected void configureRules()
        {
            bindRulesFrom( ProcessingParameter.class );
        }

    } ).setStackAction( this ).newDigester();

    private boolean forwarding = true;

    public ProcessingParametersTransformer( Map<String, String> dynaProperties )
    {
        this.dynaProperties = dynaProperties;
        register( new ParamTypeConverter(), ParamType.class );
    }

    @Override
    public void startElement( String uri, String localName, String name, Attributes atts )
        throws SAXException
    {
        if ( forwarding )
        {
            super.startElement( uri, localName, name, atts );
        }
        else
        {
            digester.startElement( uri, localName, name, atts );
        }

        if ( PROCESSING_PARAMETERS.equals( localName ) )
        {
            if ( logger.isInfoEnabled() )
            {
                logger.info( "%---------- Processing input parameters ----------%" );
            }

            forwarding = false;
            digester.startDocument();
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
        if ( PROCESSING_PARAMETERS.equals( localName ) )
        {
            if ( logger.isInfoEnabled() )
            {
                logger.info( "%------ Input parameters processing is over ------%" );
            }

            forwarding = true;
            digester.endElement( uri, localName, name );
            digester.endDocument();
            digester.clear();
        }

        if ( forwarding )
        {
            super.endElement( uri, localName, name );
        }
        else
        {
            digester.endElement( uri, localName, name );
        }
    }

    @Override
    public <T> T onPush( Digester d, String stackName, T o )
    {
        return o;
    }

    @Override
    public <T> T onPop( Digester d, String stackName, T o )
    {
        if ( o instanceof ProcessingParameter )
        {
            ProcessingParameter parameter = (ProcessingParameter) o;

            if ( logger.isInfoEnabled() )
            {
                logger.info( "Checking parameter {}: {}", parameter.getName(), parameter.getDescription() );
            }

            if ( parameter.isMandatory() )
            {
                if ( logger.isInfoEnabled() )
                {
                    logger.info( "Parameter {} is required", parameter.getName() );
                }

                boolean found = dynaProperties.containsKey( parameter.getName() );

                if ( ParamType.ENUMERATION == parameter.getParamType() && null != parameter.getDefaultValue() )
                {
                    found = true;
                }

                if ( !found )
                {
                    throw new RuntimeException( format( "Parameter '%s' is required but not found in the CLI",
                                                        parameter.getName() ) );
                }
            }

            String passedValue = dynaProperties.get( parameter.getName() );
            if ( null == passedValue )
            {
                passedValue = parameter.getDefaultValue();

                if ( null == passedValue )
                {
                    if ( logger.isInfoEnabled() )
                    {
                        logger.info( "Parameter {} not provided but it is optional", parameter.getName() );
                    }
                    return o; // not mandatory, ignore
                }
            }

            switch ( parameter.getParamType() )
            {
                case STRING:
                    // do nothing, OK
                    break;

                case NUMBER:
                    if ( logger.isInfoEnabled() )
                    {
                        logger.info( "Checkin if parameter {} value {} is a legal number",
                                          parameter.getName(),
                                          passedValue );
                    }

                    Matcher matcher = numberPattern.matcher( passedValue );
                    if ( !matcher.matches() )
                    {
                        throw new RuntimeException( format( "Passed value '%s' for parameter '%s' is not a valid number",
                                                            passedValue,
                                                            parameter.getName() ) );
                    }
                    break;

                case ENUMERATION:
                    if ( logger.isInfoEnabled() )
                    {
                        logger.info( "Checkin if parameter {} value {} is one of admitted values: {}",
                                          new Object[] {
                                              parameter.getName(),
                                              passedValue,
                                              parameter.getAdmittedValues()
                                          } );
                    }

                    if ( !parameter.isAdmitted( passedValue ) )
                    {
                        throw new RuntimeException( format( "Passed value '%s' for parameter '%s' not admitted, only one of %s",
                                                            passedValue,
                                                            parameter.getName(),
                                                            parameter.getAdmittedValues() ) );
                    }
                    break;

                default:
                    // not supported type blocked at parsing time
                    break;
            }

            if ( logger.isInfoEnabled() )
            {
                logger.info( "Parameter {} value OK!", parameter.getName() );
            }

            try
            {
                wrap( getSAXConsumer() )
                .start( PROCESSING_PARAMETER )
                    .start( NAME ).body( parameter.getName() ).end()
                    .start( VALUE ).body( passedValue ).end()
                .end();
            }
            catch ( Exception e )
            {
                // should not happen
            }
        }

        return o;
    }

}
