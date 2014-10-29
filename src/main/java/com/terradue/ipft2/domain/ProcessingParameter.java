package com.terradue.ipft2.domain;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;

@ObjectCreate( pattern = "Processing_Parameters/Processing_Parameter" )
public final class ProcessingParameter
{

    private final Set<String> admittedValues = new HashSet<String>();

    @SetProperty( pattern = "Processing_Parameters/Processing_Parameter", attributeName = "mandatory" )
    private boolean mandatory;

    @BeanPropertySetter( pattern = "Processing_Parameters/Processing_Parameter/Param_Name" )
    private String name;

    @BeanPropertySetter( pattern = "Processing_Parameters/Processing_Parameter/Param_Description" )
    private String description;

    @BeanPropertySetter( pattern = "Processing_Parameters/Processing_Parameter/Param_Type" )
    private ParamType paramType;

    @BeanPropertySetter( pattern = "Processing_Parameters/Processing_Parameter/Param_Default" )
    private String defaultValue;

    @CallMethod( pattern = "Processing_Parameters/Processing_Parameter/Param_Valid", usingElementBodyAsArgument = true )
    public void addAdmittedValue( String value )
    {
        admittedValues.add( value );
    }

    public boolean isAdmitted( String value )
    {
        return admittedValues.contains( value );
    }

    public boolean isMandatory()
    {
        return mandatory;
    }

    public void setMandatory( boolean mandatory )
    {
        this.mandatory = mandatory;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public ParamType getParamType()
    {
        return paramType;
    }

    public void setParamType( ParamType paramType )
    {
        this.paramType = paramType;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue( String defaultValue )
    {
        this.defaultValue = defaultValue;
    }

    public Set<String> getAdmittedValues()
    {
        return admittedValues;
    }

    @Override
    public String toString()
    {
        return "ProcessingParameter [admittedValues=" + admittedValues + ", mandatory=" + mandatory + ", name=" + name
            + ", description=" + description + ", paramType=" + paramType + ", defaultValue=" + defaultValue + "]";
    }

}
