package com.terradue.ipft2.domain;

import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;

@ObjectCreate( pattern = "Input" )
public final class AlternativeList
{

    private Queue<Alternative> alternatives = new PriorityQueue<Alternative>();

    @BeanPropertySetter( pattern = "Input/Mandatory" )
    private boolean mandatory = true;

    @SetNext
    public void addList( Alternative alternative )
    {
        alternatives.offer( alternative );
    }

    public boolean isEmpty()
    {
        return alternatives.isEmpty();
    }

    public int size()
    {
        return alternatives.size();
    }

    public Alternative nextAlternative()
    {
        return alternatives.remove();
    }

    public boolean isMandatory()
    {
        return mandatory;
    }

    public void setMandatory( boolean mandatory )
    {
        this.mandatory = mandatory;
    }

    @Override
    public String toString()
    {
        return alternatives.toString();
    }

}
