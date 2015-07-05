/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.sail.memory.MemoryStore;

import ch.qos.logback.classic.Level;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * 
 * Simple driver, it reads RDF as N-Triples and outputs the selected statements
 * to standard output.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */
public class Main {

    private static class Options {

        @Parameter(names = { "-log", "-verbose" }, description = "Level of verbosity")
        private boolean verbose = false;

        @Parameter(names = { "-rdf" }, description = "RDF statements in N-Triple format, for all entities", required = true)
        private String rdf;

        @Parameter(names = { "-referent" }, description = "URI for the referent", required = true)
        private String referent;

        @Parameter(names = { "-confusors" }, description = "URIs for the confusors", variableArity = true, required = true)
        private List<String> confusors = new ArrayList<String>();

        @Parameter(names = { "-algorithm" }, description = "Full class name of the algorithm to execute", required = true)
        private String algorithm = DaleReiterAlgorithm.class.getName();

        @Parameter(names = { "-type" }, description = "Type for the referent, if needed")
        private String type = null;
    }

    public static void main(String[] args) throws RDFParseException, UnsupportedRDFormatException, IOException,
            RepositoryException, ReferringExpressionException, RDFHandlerException {

        Options options = new Options();
        new JCommander(options, args);

        FileInputStream in = new FileInputStream(options.rdf);

        Model m = Rio.parse(in, "http://localhost/", RDFFormat.NTRIPLES);

        ReferringExpressionAlgorithm algorithm = null;
        if (options.algorithm.equals(DaleReiterAlgorithm.class.getName())) {
            algorithm = new DaleReiterAlgorithm(TypePriorities.dbPediaPriorities, TypePriorities.dbPediaIgnored);
            if (options.verbose)
                ((ch.qos.logback.classic.Logger) DaleReiterAlgorithm.logger).setLevel(Level.DEBUG);
        } else if (options.algorithm.equals(GardentAlgorithm.class.getName())) {
            algorithm = new GardentAlgorithm(TypePriorities.dbPediaPriorities, TypePriorities.dbPediaIgnored);
            if (options.verbose)
                ((ch.qos.logback.classic.Logger) GardentAlgorithm.logger).setLevel(Level.DEBUG);
        } else if (options.algorithm.equals(GraphAlgorithm.class.getName())) {
            algorithm = new GraphAlgorithm(TypePriorities.dbPediaPriorities, TypePriorities.dbPediaIgnored);
            if (options.verbose)
                ((ch.qos.logback.classic.Logger) GraphAlgorithm.logger).setLevel(Level.DEBUG);
        } else {
            System.err.println("Unknown algorithm '" + options.algorithm + "'");
            System.exit(-1);
        }

        Repository rep = new SailRepository(new MemoryStore());
        rep.initialize();
        ValueFactory f = rep.getValueFactory();

        List<URI> confusors = new ArrayList<URI>(options.confusors.size());
        for (String confusor : options.confusors)
            confusors.add(f.createURI(confusor));

        RepositoryConnection conn = rep.getConnection();
        try {
            conn.add(m);

            URI referent = f.createURI(options.referent);

            if (options.type != null)
                conn.add(referent, RDF.TYPE, f.createURI(options.type));

            ReferringExpression r = algorithm.resolve(referent, confusors, conn);
            System.out.println(r);
        } finally {
            conn.close();
        }

    }
}
