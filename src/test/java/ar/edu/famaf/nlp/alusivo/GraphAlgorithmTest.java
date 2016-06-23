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
 * Simple test case for the Graph Algorithm.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */
public class GraphAlgorithmTest extends TestCase {

    public static Test suite() {
        return new TestSuite(GraphAlgorithmTest.class);
    }

    public void testChihuahuas() throws Exception {
        // http://www.mitpressjournals.org/doi/pdfplus/10.1162/089120103321337430, Fig-3

        Repository rep = new SailRepository(new MemoryStore());
        rep.initialize();

        ValueFactory f = rep.getValueFactory();

        List<URI> confusors = new ArrayList<URI>();
        URI referent = null;
        URI[] d = new URI[4];

        for (int i = 1; i <= 4; i++) {
            d[i - 1] = f.createURI("http://alusivo/d" + i);
            if (i == 1)
                referent = d[i - 1];
            else
                confusors.add(d[i - 1]);
        }

        RepositoryConnection conn = rep.getConnection();
        try {
            URI dog = f.createURI("http://alusivo/dog");
            URI chihuahua = f.createURI("http://alusivo/chihuahua");
            URI doghouse = f.createURI("http://alusivo/doghouse");
            URI small = f.createURI("http://alusivo/small");
            URI large = f.createURI("http://alusivo/large");
            URI brown = f.createURI("http://alusivo/brown");
            URI white= f.createURI("http://alusivo/white");
            URI leftOf = f.createURI("http://alusivo/left_of");
            URI rightOf = f.createURI("http://alusivo/right_of");
            URI nextTo = f.createURI("http://alusivo/next_to");
            URI contains = f.createURI("http://alusivo/contains");
            URI in = f.createURI("http://alusivo/in");
            
            conn.add(new StatementImpl(d[0], dog, d[0]));
            conn.add(new StatementImpl(d[0], small, d[0]));
            conn.add(new StatementImpl(d[0], brown, d[0]));
            conn.add(new StatementImpl(d[0], chihuahua, d[0]));
            
            conn.add(new StatementImpl(d[1], dog, d[1]));
            conn.add(new StatementImpl(d[1], small, d[1]));
            conn.add(new StatementImpl(d[1], brown, d[1]));
            conn.add(new StatementImpl(d[1], chihuahua, d[1]));
            
            conn.add(new StatementImpl(d[2], doghouse, d[2]));
            conn.add(new StatementImpl(d[2], white, d[2]));
            conn.add(new StatementImpl(d[2], large, d[2]));

            conn.add(new StatementImpl(d[3], doghouse, d[3]));
            conn.add(new StatementImpl(d[3], white, d[3]));
            conn.add(new StatementImpl(d[3], large, d[3]));
            
            // d1->d2
            conn.add(new StatementImpl(d[0], nextTo, d[1]));
            conn.add(new StatementImpl(d[0], leftOf, d[1]));
            
            // d2->d1
            conn.add(new StatementImpl(d[1], nextTo, d[0]));
            conn.add(new StatementImpl(d[1], rightOf, d[0]));
            
            // d1->d3
            conn.add(new StatementImpl(d[0], in, d[2]));
            
            // d3->d1
            conn.add(new StatementImpl(d[2], contains, d[0]));
            
            // d2->d4
            conn.add(new StatementImpl(d[1], nextTo, d[3]));
            conn.add(new StatementImpl(d[1], leftOf, d[3]));
            
            // d4->d2
            conn.add(new StatementImpl(d[3], nextTo, d[1]));
            conn.add(new StatementImpl(d[3], rightOf, d[1]));
            
            // d3->d4
            conn.add(new StatementImpl(d[2], nextTo, d[3]));
            conn.add(new StatementImpl(d[2], leftOf, d[3]));
            
            // d4->d3
            conn.add(new StatementImpl(d[3], nextTo, d[2]));
            conn.add(new StatementImpl(d[3], rightOf, d[2]));

            Map<String, List<String>> priorities = new HashMap<String, List<String>>();
            String[] prio = new String[] { "dog","small","large","brown","white","left_of",
                    "right_of","next_to","contains","in"};
            priorities.put(dog.toString(), Arrays.asList(prio));
            priorities.put(doghouse.toString(), Arrays.asList(prio));

            GraphAlgorithm algorithm = new GraphAlgorithm(priorities, null);
            ReferringExpression r = algorithm.resolve(referent, confusors, conn);
            System.out.println(r);
            assertFalse(r.hasNegatives());
            assertEquals(1, r.predicates().size());
            assertEquals(in, r.predicates().get(0).getPredicate());
        } finally {
            conn.close();
        }

    }
}
