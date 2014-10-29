package com.terradue.ipft2.xml;

import org.apache.cocoon.sax.AbstractSAXTransformer;
import org.apache.cocoon.xml.sax.AttributesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

abstract class AbstractGeneratorProcessor
    extends AbstractSAXTransformer
{

    private static final String EMPTY_NS = "";

    private final AttributesImpl emptyAttributes = new AttributesImpl();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected final void startElement( String name, Attribute...attributes )
        throws SAXException
    {
        for ( Attribute attribute : attributes )
        {
            emptyAttributes.addAttribute( EMPTY_NS, attribute.getName(), attribute.getName(), "CDATA", attribute.getValue() );
        }

        super.startElement( EMPTY_NS, name, name, emptyAttributes );

        emptyAttributes.clear();
    }

    protected final void element( String name, String value )
        throws SAXException
    {
        startElement( name );
        super.characters( value.toCharArray(), 0, value.length() );
        endElement( name );
    }

    protected final void endElement( String name )
        throws SAXException
    {
        super.endElement( EMPTY_NS, name, name );
    }

    public Logger getLogger()
    {
        return logger;
    }

    protected static final class Attribute
    {

        private final String name;

        private final String value;

        public Attribute( String name, String value )
        {
            this.name = name;
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }

    }

}
