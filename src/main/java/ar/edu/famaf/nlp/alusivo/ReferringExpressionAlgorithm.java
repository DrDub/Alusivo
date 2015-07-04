/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.util.List;

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

    public ReferringExpression resolve(URI referent, List<URI> confusors, RepositoryConnection repo)
            throws ReferringExpressionException, RepositoryException;

}
