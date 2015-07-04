/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import info.aduna.iteration.Iterations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.set.SCF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.variables.Variable;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Gardent Algorithm.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */

// Reference:

// @inproceedings{gardent2002generating,
// title={Generating minimal definite descriptions},
// author={Gardent, Claire},
// booktitle={Proceedings of the 40th Annual Meeting on Association for
// Computational Linguistics},
// pages={96--103},
// year={2002},
// organization={Association for Computational Linguistics}
// }

public class GardentAlgorithm implements ReferringExpressionAlgorithm {

    static final Logger logger = LoggerFactory.getLogger(GardentAlgorithm.class);

    private static class Pair {
        private URI predicate;
        private Value value;

        public Pair(Statement stmt) {
            this.predicate = stmt.getPredicate();
            this.value = stmt.getObject();
        }

        public Pair(URI property, Value value) {
            this.predicate = property;
            this.value = value;
        }

        public String getPredicateString() {
            return predicate.getLocalName();
        }

        public String getValueString() {
            return value.stringValue();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair))
                return false;
            Pair other = (Pair) o;
            return this.predicate.equals(other.predicate) && this.value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return this.predicate.hashCode() * 13 & this.value.hashCode();
        }

        public String toString() {
            return "Pair[" + this.getPredicateString() + "->" + this.getValueString() + "]";
        }

        public URI getPredicate() {
            // TODO Auto-generated method stub
            return null;
        }

        public Value getValue() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private Map<String, List<String>> priorities;
    private Map<String, List<String>> ignored;

    public GardentAlgorithm(Map<String, List<String>> priorities, Map<String, List<String>> ignored) {
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

        List<Statement> referentStmts = new ArrayList<Statement>();
        Iterations.addAll(repo.getStatements(referent, null, null, true), referentStmts);
        Iterations.addAll(repo.getStatements(null, null, referent, true), referentStmts);

        final Map<String, Integer> mappedOrder = new HashMap<String, Integer>();
        for (int i = 0; i < priorities.size(); i++)
            mappedOrder.put(priorities.get(i), i);

        Set<Pair> allPairs = new HashSet<Pair>();

        // calculate allP+
        Set<Pair> allPplus = new HashSet<Pair>();
        for (Statement stmt : referentStmts)
            allPplus.add(new Pair(stmt));
        allPairs.addAll(allPplus);

        // calculate allP-
        @SuppressWarnings("unchecked")
        List<Statement> confusorsTrue[] = new List[confusors.size()];
        for (int i = 0; i < confusors.size(); i++) {
            confusorsTrue[i] = new ArrayList<Statement>();
            URI confusor = confusors.get(i);
            boolean empty = true;
            RepositoryResult<Statement> confusorStmts1 = repo.getStatements(confusor, null, null, true);
            if (confusorStmts1.hasNext())
                empty = false;
            Iterations.addAll(confusorStmts1, confusorsTrue[i]);
            RepositoryResult<Statement> confusorStmts2 = repo.getStatements(null, null, confusor, true);
            if (confusorStmts2.hasNext())
                empty = false;
            Iterations.addAll(confusorStmts2, confusorsTrue[i]);
            if (empty)
                throw new ReferringExpressionException("No information available for confusor " + confusor);
        }

        // check we know about all predicates for this type
        Set<String> unknownPredicates = new HashSet<String>();
        for (List<Statement> l : confusorsTrue)
            for (Statement stmt : l)
                unknownPredicates.add(stmt.getPredicate().getLocalName());
        unknownPredicates.removeAll(priorities);
        unknownPredicates.removeAll(ignored);
        if (!unknownPredicates.isEmpty())
            logger.warn("For type '" + type + "' missing properties: " + unknownPredicates + ", referent " + referent);

        Map<URI, Set<Value>> domains = new HashMap<URI, Set<Value>>();
        for (List<Statement> l : confusorsTrue)
            for (Statement stmt : l) {
                if (!domains.containsKey(stmt.getPredicate()))
                    domains.put(stmt.getPredicate(), new HashSet<Value>());
                domains.get(stmt.getPredicate()).add(stmt.getObject());
            }
        Set<Pair> allPminus = new HashSet<Pair>();
        for (Map.Entry<URI, Set<Value>> e : domains.entrySet())
            for (Value val : e.getValue()) {
                Pair p = new Pair(e.getKey(), val);
                allPairs.add(p);
                if (!allPplus.contains(p))
                    allPminus.add(p);
            }

        // map all pairs into integers
        List<Pair> numberedPairs = new ArrayList<Pair>(allPairs);
        Collections.sort(numberedPairs, new Comparator<Pair>() {

            public int compare(Pair p1, Pair p2) {
                String prop1 = p1.getPredicateString();
                String prop2 = p2.getPredicateString();
                Integer m1 = mappedOrder.get(prop1);
                Integer m2 = mappedOrder.get(prop2);
                if (m1 != null && m2 != null)
                    return m1.compareTo(m2);
                if (m1 != null)
                    return -1;
                if (m2 != null)
                    return 1;
                if (prop1 != prop2) // using unique strings
                    return prop1.compareTo(prop2);
                return p1.getValueString().compareTo(p2.getValueString());
            }

        });
        Map<Pair, Integer> pairToInt = new HashMap<Pair, Integer>();
        for (int i = 0; i < numberedPairs.size(); i++)
            pairToInt.put(numberedPairs.get(i), i);
        // System.out.println(pairToInt);

        // define the constraints
        int[] allPplusArr = new int[allPplus.size()];
        List<Pair> allPplusL = new ArrayList<Pair>(allPplus);
        for (int i = 0; i < allPplus.size(); i++)
            allPplusArr[i] = pairToInt.get(allPplusL.get(i));

        Arrays.sort(allPplusArr);

        int[] allPminusArr = new int[allPminus.size()];
        List<Pair> allPminusL = new ArrayList<Pair>(allPminus);
        for (int i = 0; i < allPminus.size(); i++)
            allPminusArr[i] = pairToInt.get(allPminusL.get(i));
        Arrays.sort(allPminusArr);

        Solver solver = new Solver();
        SetVar pPlus = VF.set("Pplus", 0, numberedPairs.size(), solver);
        SetVar pMinus = VF.set("Pminus", 0, numberedPairs.size(), solver);
        SetVar allPplusVar = VF.set("allPplus", allPplusArr, allPplusArr, solver); // constant
        SetVar allPminusVar = VF.set("allPminus", allPminusArr, allPminusArr, solver); // constant

        solver.post(SCF.subsetEq(new SetVar[] { pPlus, allPplusVar }));
        solver.post(SCF.subsetEq(new SetVar[] { pMinus, allPminusVar }));
        IntVar one = VF.fixed(1, solver);

        for (int i = 0; i < confusorsTrue.length; i++) {
            // Pi+
            Set<Pair> allPiPlus = new HashSet<Pair>();

            for (Statement stmt : confusorsTrue[i])
                allPiPlus.add(new Pair(stmt));
            List<Pair> allPiPlusL = new ArrayList<Pair>(allPiPlus);
            int[] allPiPlusI = new int[allPiPlusL.size()];
            for (int j = 0; j < allPiPlusI.length; j++) {
                Integer ii = pairToInt.get(allPiPlusL.get(j));
                if (ii == null)
                    System.out.println("Not found: " + allPiPlusL.get(j));
                allPiPlusI[j] = ii;
            }
            Arrays.sort(allPiPlusI);

            // P/Pi+
            Set<Pair> allPiPlusCompl = new HashSet<Pair>(allPairs);
            allPiPlusCompl.removeAll(allPiPlus);
            List<Pair> allPiPlusComplL = new ArrayList<Pair>(allPiPlusCompl);
            int[] allPiPlusComplI = new int[allPiPlusComplL.size()];
            for (int j = 0; j < allPiPlusComplI.length; j++)
                allPiPlusComplI[j] = pairToInt.get(allPiPlusComplL.get(j));
            Arrays.sort(allPiPlusComplI);

            SetVar pPlusSansPiPlus = VF.set("pPlusSansP" + i + "Plus", 0, numberedPairs.size(), solver);
            SetVar allPiPlusComplIVar = VF.set("allP" + i + "PlusComplI", allPiPlusComplI, allPiPlusComplI, solver);
            solver.post(SCF.intersection(new SetVar[] { pPlus, allPiPlusComplIVar }, pPlusSansPiPlus));

            SetVar pMinusInterPiPlus = VF.set("pMinusInterP" + i + "Plus", 0, numberedPairs.size(), solver);
            SetVar allPiPlusIVar = VF.set("allP" + i + "PlusI", allPiPlusI, allPiPlusI, solver);
            solver.post(SCF.intersection(new SetVar[] { pMinus, allPiPlusIVar }, pMinusInterPiPlus));

            SetVar bothCases = VF.set("bothCases_" + i, 0, numberedPairs.size(), solver);
            solver.post(SCF.union(new SetVar[] { pPlusSansPiPlus, pMinusInterPiPlus }, bothCases));

            // now, for the actual constraint
            solver.post(SCF.cardinality(bothCases, one));
        }
        SetVar pPlus_pMinus = VF.set("pPlus_pMinus", 0, numberedPairs.size(), solver);
        solver.post(SCF.union(new SetVar[] { pPlus, pMinus }, pPlus_pMinus));
        int targetCard = 1;
        while (targetCard < 10) {
            Solver cardSolver = solver.duplicateModel();

            IntVar targetCardVar = VF.fixed(targetCard, cardSolver);
            cardSolver.post(SCF.cardinality(setVar(cardSolver, pPlus_pMinus), targetCardVar));

            boolean solved = cardSolver.findSolution();

            logger.debug(cardSolver.getMeasures().toString());
            if (solved) {
                logger.debug("P+: ");
                ReferringExpression result = new ReferringExpression(referent);

                for (int i : setVar(cardSolver, pPlus).getValues()) {
                    logger.debug("\t" + numberedPairs.get(i));
                    Pair p = numberedPairs.get(i);
                    result.addPositive(null, p.getPredicate(), p.getValue());
                }
                logger.debug("P-: ");
                for (int i : setVar(cardSolver, pMinus).getValues()) {
                    logger.debug("\t" + numberedPairs.get(i));

                    Pair p = numberedPairs.get(i);
                    result.addNegative(null, p.getPredicate(), p.getValue());
                }

                return result;
            }
            targetCard++;
        }

        throw new ReferringExpressionException("No constraints found");
    }

    private SetVar setVar(Solver solver, SetVar v) {
        String name = v.getName();
        for (Variable var : solver.getVars()) {
            if (var.getName().equals(name))
                return (SetVar) var;
        }
        return null;
    }
}
