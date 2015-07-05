/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

/**
 * 
 * Simple test case for Dale & Reiter Algorithm.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */
public class DaleReiterAlgorithmTest extends TestCase {

    public static Test suite() {
        return new TestSuite(DaleReiterAlgorithmTest.class);
    }

    public void testResolve() throws Exception {
        Repository rep = new SailRepository(new MemoryStore());
        rep.initialize();

        ValueFactory f = rep.getValueFactory();

        List<URI> confusors = new ArrayList<URI>();
        URI confusor1 = f.createURI("http://alusivo/ballfar");
        URI confusor2 = f.createURI("http://alusivo/redballclose");
        confusors.add(confusor1);
        confusors.add(confusor2);
        URI referent = f.createURI("http://alusivo/redmiddle");

        RepositoryConnection conn = rep.getConnection();
        try {
            URI balltype = f.createURI("http://alusivo/ball");
            URI color = f.createURI("http://alusivo/color");
            URI distance = f.createURI("http://alusivo/distance");
            conn.add(new StatementImpl(referent, RDF.TYPE, balltype));
            conn.add(new StatementImpl(confusor1, RDF.TYPE, balltype));
            conn.add(new StatementImpl(confusor2, RDF.TYPE, balltype));
            conn.add(new StatementImpl(referent, color, f.createLiteral("red")));
            conn.add(new StatementImpl(confusor1, color, f.createLiteral("black")));
            conn.add(new StatementImpl(confusor2, color, f.createLiteral("red")));
            conn.add(new StatementImpl(referent, distance, f.createLiteral("middle")));
            conn.add(new StatementImpl(confusor1, distance, f.createLiteral("far")));
            conn.add(new StatementImpl(confusor2, distance, f.createLiteral("close")));

            Map<String, List<String>> priorities = new HashMap<String, List<String>>();
            priorities.put(balltype.toString(), Arrays.asList(new String[] { "type", "color", "distance" }));

            DaleReiterAlgorithm algorithm = new DaleReiterAlgorithm(priorities, null);
            ReferringExpression r = algorithm.resolve(referent, confusors, conn);
            //  System.out.println(r);
            assertFalse(r.hasNegatives());
            assertEquals(2, r.predicates().size());
            assertEquals(color, r.predicates().get(0).getPredicate());
            assertEquals(distance, r.predicates().get(1).getPredicate());
        } finally {
            conn.close();
        }

    }
}
