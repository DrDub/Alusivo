/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

/**
 * 
 * Custom exception. 
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 *
 */
public class ReferringExpressionException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public ReferringExpressionException() {
    }

    public ReferringExpressionException(String message) {
	super(message);
    }

    public ReferringExpressionException(Throwable cause) {
	super(cause);
    }

    public ReferringExpressionException(String message, Throwable cause) {
	super(message, cause);
    }

    public ReferringExpressionException(String message, Throwable cause,
	    boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

}
