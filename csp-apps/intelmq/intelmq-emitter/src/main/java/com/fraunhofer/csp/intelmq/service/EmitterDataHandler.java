package com.fraunhofer.csp.intelmq.service;

import org.springframework.http.ResponseEntity;

/**
 * Created by Majid Salehi on 20/04/2018
 */
public interface EmitterDataHandler {

	public ResponseEntity<String> handleIntelmqData(String intelmqData, String requestMethod);

}
