package com.terradue.ipft2.converters;

import static java.net.URI.create;

import java.net.URI;

import com.beust.jcommander.IStringConverter;

public final class UriConverter
    implements IStringConverter<URI>
{

    @Override
    public URI convert( String value )
    {
        return create( value );
    }

}
