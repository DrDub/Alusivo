/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.graph.DirectedPseudograph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Graph Algorithm.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */

// Reference:

// @article{krahmer2003graph,
// title={Graph-based generation of referring expressions},
// author={Krahmer, Emiel and Van Erk, Sebastiaan and Verleg, Andr{\'e}},
// journal={Computational Linguistics},
// volume={29},
// number={1},
// pages={53--72},
// year={2003},
// publisher={MIT Press}
// }

public class GraphAlgorithm implements ReferringExpressionAlgorithm {

    static final Logger logger = LoggerFactory.getLogger(GraphAlgorithm.class);

    private Map<String, List<String>> priorities;
    private Map<String, List<String>> ignored;

    public GraphAlgorithm(Map<String, List<String>> priorities, Map<String, List<String>> ignored) {
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

    protected static class Edge {

        private URI uri;
        private Value value;

        private boolean isRelation;

        public Edge(URI uri) {
            this.isRelation = true;
            this.uri = uri;
            this.value = null;
        }

        public Edge(URI uri, Value value) {
            this.isRelation = false;
            this.uri = uri;
            this.value = value;
        }

        public boolean isRelation() {
            return isRelation;
        }

        public URI getURI() {
            return uri;
        }

        public Value getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            if (isRelation)
                return uri.hashCode();
            else
                return uri.hashCode() * value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge))
                return false;
            Edge other = (Edge) obj;
            if (!(this.isRelation ^ other.isRelation))
                return false;
            if (!this.uri.equals(other.uri))
                return false;
            if (this.isRelation)
                return true;
            return this.value.equals(other.value);
        }
    }

    public ReferringExpression resolve(URI referent, List<URI> confusors, RepositoryConnection repo)
            throws ReferringExpressionException, RepositoryException {

        URI[] uris = new URI[confusors.size() + 1];
        confusors.toArray(uris);
        uris[uris.length - 1] = referent;
        DirectedPseudograph<Resource, Edge> graph = buildGraph(repo, uris);
        DirectedPseudograph<Resource, Edge> bestGraph = null;
        DirectedPseudograph<Resource, Edge> candidate = new DirectedPseudograph<Resource, Edge>(Edge.class);
        candidate.addVertex(referent);

        DirectedPseudograph<Resource, Edge> finalGraph = findGraph(referent, graph, bestGraph, Double.NaN, candidate)
                .getLeft();

        if (finalGraph == null)
            throw new ReferringExpressionException("No graph found");

        // read out properties from final graph
        ReferringExpression result = new ReferringExpression(referent);
        for (Edge e : finalGraph.edgeSet())
            result.addPositive(finalGraph.getEdgeSource(e), e.getURI(), e.isRelation() ? finalGraph.getEdgeTarget(e)
                    : e.getValue());

        return result;
    }

    private DirectedPseudograph<Resource, Edge> buildGraph(RepositoryConnection repo, URI[] uris)
            throws RepositoryException {
        DirectedPseudograph<Resource, Edge> result = new DirectedPseudograph<Resource, Edge>(Edge.class);

        Set<Statement> consumed = new HashSet<Statement>();

        for (URI uri : uris) {
            buildGraphAddAll(result, repo.getStatements(uri, null, null, true), consumed);
            buildGraphAddAll(result, repo.getStatements(null, null, uri, true), consumed);
        }

        return result;
    }

    private void buildGraphAddAll(DirectedPseudograph<Resource, Edge> graph, RepositoryResult<Statement> stmts,
            Set<Statement> consumed) throws RepositoryException {

        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            if (consumed.contains(stmt))
                continue;
            consumed.add(stmt);
            Resource source = stmt.getSubject();
            if (!graph.containsVertex(source))
                graph.addVertex(source);
            Value value = stmt.getObject();
            Resource target = source;
            Edge edge = null;
            if (value instanceof Resource) {
                target = (Resource) value;
                edge = new Edge(stmt.getPredicate());
            } else {
                edge = new Edge(stmt.getPredicate(), value);
            }
            if (!graph.containsVertex(target))
                graph.addVertex(target);
            graph.addEdge(source, target, edge);
        }
    }

    private Pair<DirectedPseudograph<Resource, Edge>, Double> findGraph(URI referent,
            DirectedPseudograph<Resource, Edge> fullGraph, DirectedPseudograph<Resource, Edge> bestGraph,
            double bestGraphCost, DirectedPseudograph<Resource, Edge> candidate) {
        double candidateCost = cost(candidate);
        if (bestGraph != null && bestGraphCost <= candidateCost)
            return Pair.of(bestGraph, bestGraphCost);

        List<Resource> distractors = new ArrayList<Resource>(fullGraph.vertexSet().size());
        for (Resource v : fullGraph.vertexSet())
            if (v != referent && matchGraphs(referent, candidate, v, fullGraph))
                distractors.add(v);

        if (distractors.isEmpty())
            return Pair.of(candidate, candidateCost);

        for (Edge e : neighbors(candidate, fullGraph)) {
            @SuppressWarnings("unchecked")
            DirectedPseudograph<Resource, Edge> newCandidate = (DirectedPseudograph<Resource, Edge>) candidate.clone();
            Resource source = fullGraph.getEdgeSource(e);
            Resource target = fullGraph.getEdgeTarget(e);
            if (!newCandidate.vertexSet().contains(source))
                // odd, this shouldn't always be the case?
                newCandidate.addVertex(source);
            if (!newCandidate.vertexSet().contains(target))
                newCandidate.addVertex(target);
            newCandidate.addEdge(fullGraph.getEdgeSource(e), fullGraph.getEdgeTarget(e), e);
            Pair<DirectedPseudograph<Resource, Edge>, Double> p = findGraph(referent, fullGraph, bestGraph,
                    bestGraphCost, newCandidate);
            if (bestGraph == null || p.getRight() <= bestGraphCost) {
                bestGraph = p.getLeft();
                bestGraphCost = p.getRight();
            }
        }

        return Pair.of(bestGraph, bestGraphCost);
    }

    private Set<Edge> neighbors(DirectedPseudograph<Resource, Edge> candidate,
            DirectedPseudograph<Resource, Edge> fullGraph) {
        Set<Edge> result = new HashSet<>();
        for (Resource node : candidate.vertexSet())
            for (Edge e : fullGraph.outgoingEdgesOf(node)) {
                if (candidate.containsEdge(e))
                    continue;
                result.add(e);
            }
        return result;
    }

    private Set<Resource> neighbors(DirectedPseudograph<Resource, Edge> graph, Resource node) {
        Set<Resource> result = new HashSet<Resource>();
        for (Edge e : graph.outgoingEdgesOf(node))
            result.add(graph.getEdgeTarget(e));
        return result;
    }

    private boolean containedPropertiesInSubgraph(Resource node1, DirectedPseudograph<Resource, Edge> subGraph,
            Resource node2, DirectedPseudograph<Resource, Edge> fullGraph) {
        Set<Edge> basicProperties = subGraph.getAllEdges(node1, node1);
        if (!fullGraph.getAllEdges(node2, node2).containsAll(basicProperties))
            return false;
        return true;
    }

    private boolean matchGraphs(Resource referent, DirectedPseudograph<Resource, Edge> candidate, Resource other,
            DirectedPseudograph<Resource, Edge> fullGraph) {

        if (!containedPropertiesInSubgraph(referent, candidate, other, fullGraph))
            return false;
        Map<Resource, Resource> bijection = new HashMap<>();
        bijection.put(referent, other);

        return matchHelper(bijection, neighbors(candidate, referent), fullGraph, candidate);
    }

    private boolean matchHelper(Map<Resource, Resource> bijection, // pi
            Set<Resource> neighborgs, // Y
            DirectedPseudograph<Resource, Edge> fullGraph, // G
            DirectedPseudograph<Resource, Edge> candidate) { // H
        if (bijection.keySet().size() == candidate.vertexSet().size())
            return true;
        if (neighborgs.isEmpty())
            return false;
        for (Resource toMap : neighborgs) { // y
            if (bijection.containsKey(toMap))
                continue;
            // find valid matches Z
            for (Resource extension : fullGraph.vertexSet()) { // z
                if (bijection.values().contains(extension))
                    continue;
                if (!containedPropertiesInSubgraph(toMap, candidate, extension, fullGraph))
                    continue;
                Set<Resource> toCheck = neighbors(candidate, toMap);
                toCheck.retainAll(bijection.keySet());
                boolean good = true;
                for (Resource other : toCheck) { // h
                    // (toMap -> other) y->h
                    Set<Edge> outgoing = candidate.getAllEdges(toMap, other);
                    if (!fullGraph.getAllEdges(extension, bijection.get(other)).containsAll(outgoing)) {
                        good = false;
                        break;
                    }
                    // (other -> toMap) y->h
                    Set<Edge> incoming = candidate.getAllEdges(other, toMap);
                    if (!fullGraph.getAllEdges(bijection.get(other), extension).containsAll(incoming)) {
                        good = false;
                        break;
                    }
                }
                if (!good)
                    continue;
                Map<Resource, Resource> bijectionRec = new HashMap<>();
                bijectionRec.putAll(bijection);
                bijection.put(toMap, extension);
                Set<Resource> neighborgsRec = new HashSet<Resource>(neighborgs);
                neighborgsRec.remove(toMap); // not in Figure 8
                if (matchHelper(bijectionRec, neighborgsRec, fullGraph, candidate))
                    return true;
            }

        }
        return false;
    }

    private double cost(DirectedPseudograph<Resource, Edge> candidate) {
        return candidate.vertexSet().size() + candidate.edgeSet().size();
    }

    // private matchGraphs

    // RepositoryResult<Statement> types = repo.getStatements(referent,
    // RDF.TYPE, null, true);
    // if (!types.hasNext())
    // throw new ReferringExpressionException(
    // "Unknwon type for referent '" + referent + "'");
    // List<String> priorities = null;
    // Set<String> ignored = new HashSet<String>();
    // StringBuilder typeNames = new StringBuilder();
    // String type = null;
    // while (types.hasNext()) {
    // Statement typeStmt = types.next();
    // type = typeStmt.getObject().stringValue();
    // typeNames.append(' ').append(type);
    // priorities = this.priorities.get(type);
    // if (priorities != null) {
    // if (this.ignored.containsKey(type))
    // ignored.addAll(this.ignored.get(type));
    // break;
    // }
    // }
    //
    // if (priorities == null)
    // throw new ReferringExpressionException(
    // "No priorities for referent with types [" + typeNames
    // + " ]");
    // logger.debug("Using priorities " + priorities + " (type '" + type
    // + "') for referent '" + referent);
    //
    // List<Statement> referentStmts = new ArrayList<Statement>();
    // Iterations.addAll(repo.getStatements(referent, null, null, true),
    // referentStmts);
    // Iterations.addAll(repo.getStatements(null, null, referent, true),
    // referentStmts);
    //
    // final Map<String, Integer> mappedOrder = new HashMap<String,
    // Integer>();
    // for (int i = 0; i < priorities.size(); i++)
    // mappedOrder.put(priorities.get(i), i);
    //
    // Set<Pair> allPairs = new HashSet<Pair>();
    //
    // // calculate allP+
    // Set<Pair> allPplus = new HashSet<Pair>();
    // for (Statement stmt : referentStmts)
    // allPplus.add(new Pair(stmt));
    // allPairs.addAll(allPplus);
    //
    // // calculate allP-
    // @SuppressWarnings("unchecked")
    // List<Statement> confusorsTrue[] = new List[confusors.size()];
    // for (int i = 0; i < confusors.size(); i++) {
    // confusorsTrue[i] = new ArrayList<Statement>();
    // URI confusor = confusors.get(i);
    // boolean empty = true;
    // RepositoryResult<Statement> confusorStmts1 = repo.getStatements(
    // confusor, null, null, true);
    // if (confusorStmts1.hasNext())
    // empty = false;
    // Iterations.addAll(confusorStmts1, confusorsTrue[i]);
    // RepositoryResult<Statement> confusorStmts2 = repo.getStatements(
    // null, null, confusor, true);
    // if (confusorStmts2.hasNext())
    // empty = false;
    // Iterations.addAll(confusorStmts2, confusorsTrue[i]);
    // if (empty)
    // throw new ReferringExpressionException(
    // "No information available for confusor " + confusor);
    // }
    //
    // // check we know about all predicates for this type
    // Set<String> unknownPredicates = new HashSet<String>();
    // for (List<Statement> l : confusorsTrue)
    // for (Statement stmt : l)
    // unknownPredicates.add(stmt.getPredicate().getLocalName());
    // unknownPredicates.removeAll(priorities);
    // unknownPredicates.removeAll(ignored);
    // if (!unknownPredicates.isEmpty())
    // logger.warn("For type '" + type + "' missing properties: "
    // + unknownPredicates + ", referent " + referent);
    //
    // Map<URI, Set<Value>> domains = new HashMap<URI, Set<Value>>();
    // for (List<Statement> l : confusorsTrue)
    // for (Statement stmt : l) {
    // if (!domains.containsKey(stmt.getPredicate()))
    // domains.put(stmt.getPredicate(), new HashSet<Value>());
    // domains.get(stmt.getPredicate()).add(stmt.getObject());
    // }
    // Set<Pair> allPminus = new HashSet<Pair>();
    // for (Map.Entry<URI, Set<Value>> e : domains.entrySet())
    // for (Value val : e.getValue()) {
    // Pair p = new Pair(e.getKey(), val);
    // allPairs.add(p);
    // if (!allPplus.contains(p))
    // allPminus.add(p);
    // }
    //
    // // map all pairs into integers
    // List<Pair> numberedPairs = new ArrayList<Pair>(allPairs);
    // Collections.sort(numberedPairs, new Comparator<Pair>() {
    //
    // public int compare(Pair p1, Pair p2) {
    // String prop1 = p1.getPredicateString();
    // String prop2 = p2.getPredicateString();
    // Integer m1 = mappedOrder.get(prop1);
    // Integer m2 = mappedOrder.get(prop2);
    // if (m1 != null && m2 != null)
    // return m1.compareTo(m2);
    // if (m1 != null)
    // return -1;
    // if (m2 != null)
    // return 1;
    // if (prop1 != prop2) // using unique strings
    // return prop1.compareTo(prop2);
    // return p1.getValueString().compareTo(p2.getValueString());
    // }
    //
    // });
    // Map<Pair, Integer> pairToInt = new HashMap<Pair, Integer>();
    // for (int i = 0; i < numberedPairs.size(); i++)
    // pairToInt.put(numberedPairs.get(i), i);
    // // System.out.println(pairToInt);
    //
    // // define the constraints
    // int[] allPplusArr = new int[allPplus.size()];
    // List<Pair> allPplusL = new ArrayList<Pair>(allPplus);
    // for (int i = 0; i < allPplus.size(); i++)
    // allPplusArr[i] = pairToInt.get(allPplusL.get(i));
    //
    // Arrays.sort(allPplusArr);
    //
    // int[] allPminusArr = new int[allPminus.size()];
    // List<Pair> allPminusL = new ArrayList<Pair>(allPminus);
    // for (int i = 0; i < allPminus.size(); i++)
    // allPminusArr[i] = pairToInt.get(allPminusL.get(i));
    // Arrays.sort(allPminusArr);
    //
    // Solver solver = new Solver();
    // SetVar pPlus = VF.set("Pplus", 0, numberedPairs.size(), solver);
    // SetVar pMinus = VF.set("Pminus", 0, numberedPairs.size(), solver);
    // SetVar allPplusVar = VF.set("allPplus", allPplusArr, allPplusArr,
    // solver); // constant
    // SetVar allPminusVar = VF.set("allPminus", allPminusArr, allPminusArr,
    // solver); // constant
    //
    // solver.post(SCF.subsetEq(new SetVar[] { pPlus, allPplusVar }));
    // solver.post(SCF.subsetEq(new SetVar[] { pMinus, allPminusVar }));
    // IntVar one = VF.fixed(1, solver);
    //
    // for (int i = 0; i < confusorsTrue.length; i++) {
    // // Pi+
    // Set<Pair> allPiPlus = new HashSet<Pair>();
    //
    // for (Statement stmt : confusorsTrue[i])
    // allPiPlus.add(new Pair(stmt));
    // List<Pair> allPiPlusL = new ArrayList<Pair>(allPiPlus);
    // int[] allPiPlusI = new int[allPiPlusL.size()];
    // for (int j = 0; j < allPiPlusI.length; j++) {
    // Integer ii = pairToInt.get(allPiPlusL.get(j));
    // if (ii == null)
    // System.out.println("Not found: " + allPiPlusL.get(j));
    // allPiPlusI[j] = ii;
    // }
    // Arrays.sort(allPiPlusI);
    //
    // // P/Pi+
    // Set<Pair> allPiPlusCompl = new HashSet<Pair>(allPairs);
    // allPiPlusCompl.removeAll(allPiPlus);
    // List<Pair> allPiPlusComplL = new ArrayList<Pair>(allPiPlusCompl);
    // int[] allPiPlusComplI = new int[allPiPlusComplL.size()];
    // for (int j = 0; j < allPiPlusComplI.length; j++)
    // allPiPlusComplI[j] = pairToInt.get(allPiPlusComplL.get(j));
    // Arrays.sort(allPiPlusComplI);
    //
    // SetVar pPlusSansPiPlus = VF.set("pPlusSansP" + i + "Plus", 0,
    // numberedPairs.size(), solver);
    // SetVar allPiPlusComplIVar = VF.set("allP" + i + "PlusComplI",
    // allPiPlusComplI, allPiPlusComplI, solver);
    // solver.post(SCF.intersection(new SetVar[] { pPlus,
    // allPiPlusComplIVar }, pPlusSansPiPlus));
    //
    // SetVar pMinusInterPiPlus = VF.set("pMinusInterP" + i + "Plus", 0,
    // numberedPairs.size(), solver);
    // SetVar allPiPlusIVar = VF.set("allP" + i + "PlusI", allPiPlusI,
    // allPiPlusI, solver);
    // solver.post(SCF.intersection(
    // new SetVar[] { pMinus, allPiPlusIVar }, pMinusInterPiPlus));
    //
    // SetVar bothCases = VF.set("bothCases_" + i, 0,
    // numberedPairs.size(), solver);
    // solver.post(SCF.union(new SetVar[] { pPlusSansPiPlus,
    // pMinusInterPiPlus }, bothCases));
    //
    // // now, for the actual constraint
    // solver.post(SCF.cardinality(bothCases, one));
    // }
    // SetVar pPlus_pMinus = VF.set("pPlus_pMinus", 0, numberedPairs.size(),
    // solver);
    // solver.post(SCF.union(new SetVar[] { pPlus, pMinus }, pPlus_pMinus));
    // int targetCard = 1;
    // while (targetCard < 10) {
    // Solver cardSolver = solver.duplicateModel();
    //
    // IntVar targetCardVar = VF.fixed(targetCard, cardSolver);
    // cardSolver.post(SCF.cardinality(setVar(cardSolver, pPlus_pMinus),
    // targetCardVar));
    //
    // boolean solved = cardSolver.findSolution();
    //
    // logger.debug(cardSolver.getMeasures().toString());
    // if (solved) {
    // logger.debug("P+: ");
    // List<Statement> plus = new ArrayList<Statement>();
    //
    // for (int i : setVar(cardSolver, pPlus).getValues()) {
    // logger.debug("\t" + numberedPairs.get(i));
    // Pair p = numberedPairs.get(i);
    // plus.add(repo.getStatements(referent, p.getPredicate(),
    // p.getValue(), true).next());
    // }
    // logger.debug("P-: ");
    // List<Statement> minus = new ArrayList<Statement>();
    // for (int i : setVar(cardSolver, pMinus).getValues()) {
    // logger.debug("\t" + numberedPairs.get(i));
    //
    // Pair p = numberedPairs.get(i);
    // repo.add(referent, p.getPredicate(), p.getValue());
    // minus.add(repo.getStatements(referent, p.getPredicate(),
    // p.getValue(), true).next());
    // }
    //
    // return new Result(plus, minus);
    // }
    // targetCard++;
    // }

}
