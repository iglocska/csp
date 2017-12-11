package com.intrasoft.csp.misp.commons.models.generated;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Generated;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseAll{

	@JsonProperty("response")
	private List<ResponseItem> response;

	public void setResponse(List<ResponseItem> response){
		this.response = response;
	}

	public List<ResponseItem> getResponse(){
		return response;
	}

	@Override
 	public String toString(){
		return 
			"ResponseAll{" + 
			"response = '" + response + '\'' + 
			"}";
		}
}