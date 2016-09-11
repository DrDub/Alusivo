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
 * Simple test case for Gardent Algorithm.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */
public class GardentAlgorithmTest extends TestCase {

    public static Test suite() {
        return new TestSuite(GardentAlgorithmTest.class);
    }

    public void testResolveMembers() throws Exception {
        // https://hal.inria.fr/inria-00099410/document, Fig-4

        Repository rep = new SailRepository(new MemoryStore());
        rep.initialize();

        ValueFactory f = rep.getValueFactory();

        List<URI> confusors = new ArrayList<URI>();
        URI referent = null;
        URI[] x = new URI[6];

        for (int i = 1; i <= 6; i++) {
            x[i - 1] = f.createURI("http://alusivo/x" + i);
            if (i == 6)
                referent = x[i - 1];
            else
                confusors.add(x[i - 1]);
        }

        RepositoryConnection conn = rep.getConnection();
        try {
            URI person = f.createURI("http://alusivo/person");
            URI member = f.createURI("http://alusivo/member");
            URI boardmember = f.createURI("http://alusivo/boardmember");
            URI secretary = f.createURI("http://alusivo/secretary");
            URI president = f.createURI("http://alusivo/president");
            URI treasurer = f.createURI("http://alusivo/treasurer");
            for (int i = 0; i < 6; i++) {
                conn.add(new StatementImpl(x[i], RDF.TYPE, person));
                conn.add(new StatementImpl(x[i], RDF.TYPE, member));
                if (i != 5)
                    conn.add(new StatementImpl(x[i], RDF.TYPE, boardmember));
            }
            conn.add(new StatementImpl(x[0], RDF.TYPE, president));
            conn.add(new StatementImpl(x[1], RDF.TYPE, secretary));
            conn.add(new StatementImpl(x[2], RDF.TYPE, treasurer));

            Map<String, List<String>> priorities = new HashMap<String, List<String>>();
            priorities.put(person.toString(), Arrays.asList(new String[] { "type" }));

            GardentAlgorithm algorithm = new GardentAlgorithm(priorities, null);
            ReferringExpression r = algorithm.resolve(referent, confusors, conn);
            // System.out.println(r);
            assertTrue(r.hasNegatives());
            assertEquals(1, r.predicates().size());
            assertTrue(r.predicates().get(0).isNegative());
            assertEquals(boardmember, r.predicates().get(0).getObject());
        } finally {
            conn.close();
        }

    }
}
