package com.intrasoft.csp.anon.commons.exceptions;

import com.intrasoft.csp.libraries.restclient.exceptions.CspBusinessException;

public class InvalidDataTypeException extends CspBusinessException {


    private static final long serialVersionUID = 688618983639049736L;

    public InvalidDataTypeException(String message) {
        super(message);
    }

    public InvalidDataTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
