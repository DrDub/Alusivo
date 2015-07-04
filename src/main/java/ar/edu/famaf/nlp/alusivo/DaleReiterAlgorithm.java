/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import info.aduna.iteration.Iterations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Dale & Reiter Algorithm.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */

// Reference:

// @inproceedings{Reiter:1992:FAG:992066.992105,
// author = {Reiter, Ehud and Dale, Robert},
// title = {A Fast Algorithm for the Generation of Referring Expressions},
// booktitle = {Proceedings of the 14th Conference on Computational Linguistics
// - Volume 1},
// series = {COLING '92},
// year = {1992},
// location = {Nantes, France},
// pages = {232--238},
// numpages = {7},
// url = {http://dx.doi.org/10.3115/992066.992105},
// doi = {10.3115/992066.992105},
// acmid = {992105},
// publisher = {Association for Computational Linguistics},
// address = {Stroudsburg, PA, USA},
// }

public class DaleReiterAlgorithm implements ReferringExpressionAlgorithm {

    static final Logger logger = LoggerFactory.getLogger(DaleReiterAlgorithm.class);

    private Map<String, List<String>> priorities;
    private Map<String, List<String>> ignored;

    public DaleReiterAlgorithm(Map<String, List<String>> priorities, Map<String, List<String>> ignored) {
        this.priorities = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> e : priorities.entrySet()) {
            this.priorities.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
        this.ignored = new HashMap<String, List<String>>();
        if (ignored != null)
            for (Map.Entry<String, List<String>> e : ignored.entrySet()) {
                this.ignored.put(e.getKey(), new ArrayList<String>(e.getValue()));
            }
    }

    public ReferringExpression resolve(URI referent, List<URI> confusors, RepositoryConnection repo)
            throws ReferringExpressionException, RepositoryException {
        RepositoryResult<Statement> types = repo.getStatements(referent, RDF.TYPE, null, true);
        if (!types.hasNext())
            throw new ReferringExpressionException("Unknwon type for referent '" + referent + "'");
        List<String> priorities = null;
        Set<String> ignored = new HashSet<String>();
        StringBuilder typeNames = new StringBuilder();
        String type = null;
        while (types.hasNext()) {
            Statement typeStmt = types.next();
            type = typeStmt.getObject().stringValue();
            typeNames.append(' ').append(type);
            priorities = this.priorities.get(type);
            if (priorities != null) {
                if (this.ignored.containsKey(type))
                    ignored.addAll(this.ignored.get(type));
                break;
            }
        }

        if (priorities == null)
            throw new ReferringExpressionException("No priorities for referent with types [" + typeNames + " ]");
        logger.debug("Using priorities " + priorities + " (type '" + type + "') for referent '" + referent);

        List<Statement> worldStmts = new ArrayList<Statement>();
        Iterations.addAll(repo.getStatements(referent, null, null, true), worldStmts);
        Iterations.addAll(repo.getStatements(null, null, referent, true), worldStmts);
        List<Statement> referentStmts = new ArrayList<Statement>(worldStmts);
        for (URI confusor : confusors) {
            boolean empty = true;
            RepositoryResult<Statement> confusorStmts1 = repo.getStatements(confusor, null, null, true);
            if (confusorStmts1.hasNext())
                empty = false;
            Iterations.addAll(confusorStmts1, worldStmts);
            RepositoryResult<Statement> confusorStmts2 = repo.getStatements(null, null, confusor, true);
            if (confusorStmts2.hasNext())
                empty = false;
            Iterations.addAll(confusorStmts2, worldStmts);
            if (empty)
                throw new ReferringExpressionException("No information available for confusor " + confusor);

        }

        // check we know about all predicates for this type
        Set<String> unknownPredicates = new HashSet<String>();
        for (Statement stmt : worldStmts)
            unknownPredicates.add(stmt.getPredicate().getLocalName());
        unknownPredicates.removeAll(priorities);
        unknownPredicates.removeAll(ignored);
        if (!unknownPredicates.isEmpty())
            logger.warn("For type '" + type + "' missing properties: " + unknownPredicates + ", referent " + referent);

        ReferringExpression result = new ReferringExpression(referent);
        Set<Statement> added = new HashSet<Statement>();

        List<URI> remainingConfusors = new ArrayList<URI>(confusors);
        for (String predicate : priorities) {
            for (Statement stmt : referentStmts)
                if (!added.contains(stmt) && stmt.getPredicate().getLocalName().equals(predicate)) {
                    List<URI> removed = rulesOut(remainingConfusors, stmt, worldStmts);
                    added.add(stmt);
                    result.addPositive(stmt);
                    remainingConfusors.removeAll(removed);
                    if (remainingConfusors.isEmpty())
                        break;
                }
            if (remainingConfusors.isEmpty())
                break;
        }

        if (!remainingConfusors.isEmpty()) {
            throw new ReferringExpressionException("Confusors left: " + remainingConfusors);
        }

        return result;
    }

    /**
     * Check which confusors will get ruled out by adding a given statement.
     */
    public static List<URI> rulesOut(List<URI> confusors, Statement stmtToAdd, List<Statement> worldStmts) {
        logger.debug("Checking statement " + stmtToAdd + " to rule out " + confusors);
        List<URI> result = new ArrayList<URI>();
        for (URI confusor : confusors) {
            if (result.contains(confusor))
                // in case repeated of confusors
                continue;

            // see how the new statement will look for the confusor
            Statement newStmt = new StatementImpl(confusor, stmtToAdd.getPredicate(), stmtToAdd.getObject());
            if (worldStmts.contains(newStmt)) {
                // it holds, this confusor is not ruled out
                logger.debug("Already present" + newStmt);
                continue;
            }
            // check if there's another statement about this confusor
            // invalidated
            for (Statement stmt : worldStmts) {
                if (!stmt.getSubject().equals(confusor))
                    continue;
                if (!stmt.getPredicate().equals(stmtToAdd.getPredicate()))
                    continue;

                result.add(confusor);
                logger.debug("Statement '" + stmtToAdd + "' rules out confusor " + confusor);
            }
        }
        return result;
    }

    // dbPedia priorities, from Pacheco et al. (2012)

}
