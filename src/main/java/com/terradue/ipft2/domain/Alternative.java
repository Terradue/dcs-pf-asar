package com.terradue.ipft2.domain;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

@ObjectCreate( pattern = "Input/List_of_Alternatives/Alternative" )
public final class Alternative
    implements Comparable<Alternative>
{

	@BeanPropertySetter( pattern = "Input/List_of_Alternatives/Alternative/Input_Source_Data" )
	private boolean input_source_data;
	
    public boolean isInput_source_data() {
		return input_source_data;
	}

	public void setInput_source_data(boolean input_source_data) {
		this.input_source_data = input_source_data;
	}

	@BeanPropertySetter( pattern = "Input/List_of_Alternatives/Alternative/Order" )
    private Integer order;

    @BeanPropertySetter( pattern = "Input/List_of_Alternatives/Alternative/Origin" )
    private String origin;

    @BeanPropertySetter( pattern = "Input/List_of_Alternatives/Alternative/Retrieval_Mode" )
    private String retrievalMode;

    @BeanPropertySetter( pattern = "Input/List_of_Alternatives/Alternative/File_Type" )
    private String fileType;

    @BeanPropertySetter( pattern = "Input/List_of_Alternatives/Alternative/File_Name_Type" )
    private String fileNameType;

    public int getOrder()
    {
        return order;
    }

    public void setOrder( int order )
    {
        this.order = order;
    }

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin( String origin )
    {
        this.origin = origin;
    }

    public String getRetrievalMode()
    {
        return retrievalMode;
    }

    public void setRetrievalMode( String retrievalMode )
    {
        this.retrievalMode = retrievalMode;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType( String fileType )
    {
        this.fileType = fileType;
    }

    public String getFileNameType()
    {
        return fileNameType;
    }

    public void setFileNameType( String fileNameType )
    {
        this.fileNameType = fileNameType;
    }

    @Override
    public int compareTo( Alternative o )
    {
        return order.compareTo( o.getOrder() );
    }

    @Override
    public String toString()
    {
        return "Alternative [order=" + order + ", origin=" + origin + ", retrievalMode=" + retrievalMode
            + ", fileType=" + fileType + ", fileNameType=" + fileNameType + "]";
    }

}
