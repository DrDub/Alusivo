/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * 
 * Interface for Referring Expression algorithms. 
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 *
 */
public interface ReferringExpressionAlgorithm {

    public Result resolve(URI referent, List<URI> confusors,
	    RepositoryConnection repo) throws ReferringExpressionException,
	    RepositoryException;

    public static class Result implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<Statement> positives;
	private List<Statement> negatives;

	public Result(List<Statement> positives) {
	    this(positives, Collections.<Statement> emptyList());
	}

	public Result(List<Statement> positives, List<Statement> negatives) {
	    this.positives = positives;
	    this.negatives = negatives;
	}

	public List<Statement> getPositives() {
	    return positives;
	}

	public List<Statement> getNegatives() {
	    return negatives;
	}

	public boolean hasNegatives() {
	    return this.negatives != null && !this.negatives.isEmpty();
	}
    }

}
