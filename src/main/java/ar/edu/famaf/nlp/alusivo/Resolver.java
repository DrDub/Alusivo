/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import ar.edu.famaf.nlp.alusivo.ReferringExpression.Predicate;

/**
 * 
 * This class takes a referring expression, a RDF triples repository and a set
 * of elements and returns the element(s) that are resolved by the expression.
 * Useful for test case.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */
public class Resolver {

    private Resolver() {
        // this class is not intended to be instantiated
    }

    public static List<URI> resolve(ReferringExpression refExp, List<URI> candidates, RepositoryConnection repo)
            throws RepositoryException {
        List<URI> result = new ArrayList<URI>(candidates);

        for (Predicate pred : refExp.predicates()) {
            int idx = 0;
            while (idx < result.size()) {
                if (pred.holds(result.get(idx), repo)) {
                    idx++;
                } else {
                    result.remove(idx);
                }
            }
            if (result.isEmpty())
                break;
        }

        return result;
    }

}
