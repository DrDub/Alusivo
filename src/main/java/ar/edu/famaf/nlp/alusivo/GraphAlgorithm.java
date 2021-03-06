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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.graph.DirectedPseudograph;
import org.openrdf.model.Resource;
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

    private static long MAX_TIME = 60 * 1000L; // 1 min

    private Map<String, List<String>> priorities;
    private Map<String, List<String>> ignored;
    private long maxTime = 60 * 1000L; // 1 min

    public GraphAlgorithm(Map<String, List<String>> priorities, Map<String, List<String>> ignored) {
        this(priorities, ignored, MAX_TIME);
    }

    public GraphAlgorithm(Map<String, List<String>> priorities, Map<String, List<String>> ignored, long maxTime) {
        this.priorities = null;
        this.ignored = null;

        if(priorities != null) {
            this.priorities = new HashMap<String, List<String>>();
            for (Map.Entry<String, List<String>> e : priorities.entrySet()) {
                this.priorities.put(e.getKey(), new ArrayList<String>(e.getValue()));
            }
        }
        if (ignored != null){
            this.ignored = new HashMap<String, List<String>>();
            for (Map.Entry<String, List<String>> e : ignored.entrySet()) {
                this.ignored.put(e.getKey(), new ArrayList<String>(e.getValue()));
            }
        }
        this.maxTime = maxTime;
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
            if (this.isRelation != other.isRelation)
                return false;
            if (!this.uri.equals(other.uri))
                return false;
            if (this.isRelation)
                return true;
            return this.value.equals(other.value);
        }

        @Override
        public String toString() {
            return "Edge(" + (isRelation ? "R:" + uri.getLocalName() :
                              uri.getLocalName() + " " + value) + ")";
        }
    }

    public ReferringExpression resolve(URI referent, List<URI> confusors, RepositoryConnection repo)
            throws ReferringExpressionException, RepositoryException {

        Map<String, Integer> mappedOrder = new HashMap<String, Integer>();
        Set<String> ignored = new HashSet<String>();
        if(this.priorities != null){
            List<String> priorities = null;
            RepositoryResult<Statement> types = repo.getStatements(referent, RDF.TYPE, null, true);

            if (!types.hasNext())
                throw new ReferringExpressionException("Unknwon type for referent '" + referent + "'");
        
            
            StringBuilder typeNames = new StringBuilder();
            String type = null;
            while (types.hasNext()) {
                Statement typeStmt = types.next();
                type = typeStmt.getObject().stringValue();
                typeNames.append(' ').append(type);
                priorities = this.priorities.get(type);
                if (priorities != null) {
                    if (this.ignored != null && this.ignored.containsKey(type))
                        ignored.addAll(this.ignored.get(type));
                    break;
                }
            }

            if (priorities == null)
                throw new ReferringExpressionException("No priorities for referent with types [" + typeNames + " ]");

            mappedOrder = new HashMap<String, Integer>();
            for (int i = 0; i < priorities.size(); i++)
                mappedOrder.put(priorities.get(i), i);
        }

        URI[] uris = new URI[confusors.size() + 1];
        confusors.toArray(uris);
        uris[uris.length - 1] = referent;
        DirectedPseudograph<Resource, Edge> graph = buildGraph(repo, uris, ignored);
        DirectedPseudograph<Resource, Edge> bestGraph = null;
        DirectedPseudograph<Resource, Edge> candidate = new DirectedPseudograph<Resource, Edge>(Edge.class);
        candidate.addVertex(referent);

        DirectedPseudograph<Resource, Edge> finalGraph =
            findGraph(referent, graph, bestGraph, Double.NaN, candidate,
                      System.currentTimeMillis(), mappedOrder).getLeft();

        if (finalGraph == null)
            throw new ReferringExpressionException("No graph found");

        // read out properties from final graph
        ReferringExpression result = new ReferringExpression(referent);
        for (Edge e : finalGraph.edgeSet())
            result.addPositive(finalGraph.getEdgeSource(e), e.getURI(), e.isRelation() ? finalGraph.getEdgeTarget(e)
                    : e.getValue());

        return result;
    }

    private DirectedPseudograph<Resource, Edge> buildGraph(RepositoryConnection repo, URI[] uris, Set<String>ignored)
            throws RepositoryException {
        DirectedPseudograph<Resource, Edge> result = new DirectedPseudograph<Resource, Edge>(Edge.class);

        Set<Statement> consumed = new HashSet<Statement>();

        for (URI uri : uris) {
            buildGraphAddAll(result, repo.getStatements(uri, null, null, true), consumed, ignored);
            buildGraphAddAll(result, repo.getStatements(null, null, uri, true), consumed, ignored);
        }

        return result;
    }

    private void buildGraphAddAll(DirectedPseudograph<Resource, Edge> graph, RepositoryResult<Statement> stmts,
            Set<Statement> consumed, Set<String>ignored) throws RepositoryException {

        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            if (consumed.contains(stmt))
                continue;
            if(ignored.contains(stmt.getPredicate().getLocalName()))
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

    private Pair<DirectedPseudograph<Resource, Edge>, Double>
        findGraph(URI referent,
                  DirectedPseudograph<Resource, Edge> fullGraph, DirectedPseudograph<Resource, Edge> bestGraph,
                  double bestGraphCost, DirectedPseudograph<Resource, Edge> candidate, long startTime,
                  final Map<String, Integer>mappedOrder)
            throws ReferringExpressionException {
        double candidateCost = cost(candidate);
        if (bestGraph != null && bestGraphCost <= candidateCost)
            return Pair.of(bestGraph, bestGraphCost);

        List<Resource> distractors = new ArrayList<Resource>(fullGraph.vertexSet().size());
        for (Resource v : fullGraph.vertexSet()) {
            if (System.currentTimeMillis() - startTime > maxTime)
                throw new ReferringExpressionException("Time-out");
            if (v != referent && matchGraphs(referent, candidate, v, fullGraph, startTime))
                distractors.add(v);
        }

        if (distractors.isEmpty())
            return Pair.of(candidate, candidateCost);

        Collection<Edge>neighbors = neighbors(candidate, fullGraph);
        if(mappedOrder != null){
            List<Edge>toSort = new ArrayList<Edge>(neighbors);

            Collections.sort(toSort, new Comparator<Edge>() {
                    public int compare(Edge e1, Edge e2) {
                        Integer m1 = mappedOrder.get(e1.getURI().getLocalName());
                        Integer m2 = mappedOrder.get(e2.getURI().getLocalName());
                        if (m1 != null && m2 != null)
                            return m1.compareTo(m2);
                        if (m1 != null)
                            return -1;
                        if (m2 != null)
                            return 1;
                        return e1.getURI().toString().compareTo(e2.getURI().toString());
                    }
                });
            neighbors = toSort;
        }
        for (Edge e : neighbors) {
            if (System.currentTimeMillis() - startTime > maxTime)
                throw new ReferringExpressionException("Time-out");

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
            Pair<DirectedPseudograph<Resource, Edge>, Double> p =
                findGraph(referent, fullGraph, bestGraph,
                          bestGraphCost, newCandidate, startTime, mappedOrder);
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
        for (Edge e : graph.incomingEdgesOf(node))
            result.add(graph.getEdgeSource(e));
        return result;
    }

    private boolean containedPropertiesInSubgraph(Resource node1, DirectedPseudograph<Resource, Edge> subGraph,
            Resource node2, DirectedPseudograph<Resource, Edge> fullGraph) {
        Set<Edge> basicProperties = subGraph.getAllEdges(node1, node1);
        if (!fullGraph.getAllEdges(node2, node2).containsAll(basicProperties)) {
            return false;
        }
        return true;
    }

    private boolean matchGraphs(Resource referent, DirectedPseudograph<Resource, Edge> candidate, Resource other,
            DirectedPseudograph<Resource, Edge> fullGraph, long startTime) throws ReferringExpressionException {

        if (!containedPropertiesInSubgraph(referent, candidate, other, fullGraph))
            return false;

        if (System.currentTimeMillis() - startTime > maxTime)
            throw new ReferringExpressionException("Time-out");

        Map<Resource, Resource> bijection = new HashMap<>();
        bijection.put(referent, other);

        return matchHelper(bijection, neighbors(candidate, referent), fullGraph, candidate, startTime);
    }

    private boolean matchHelper(Map<Resource, Resource> bijection, // pi
            Set<Resource> neighbors, // Y
            DirectedPseudograph<Resource, Edge> fullGraph, // G
            DirectedPseudograph<Resource, Edge> candidate, // H
            long startTime) throws ReferringExpressionException {

        if (bijection.keySet().size() == candidate.vertexSet().size()) {
            return true;
        }
        if (neighbors.isEmpty()){
            return false;
        }

        if (System.currentTimeMillis() - startTime > maxTime)
            throw new ReferringExpressionException("Time-out");

        for (Resource toMap : neighbors) { // y
            if (bijection.containsKey(toMap))
                continue;
            // find valid matches Z
            for (Resource extension : fullGraph.vertexSet()) { // z
                if (bijection.values().contains(extension) || bijection.keySet().contains(extension))
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
                bijectionRec.put(toMap, extension);
                Set<Resource> neighborsRec = new HashSet<Resource>(neighbors);
                neighborsRec.remove(toMap); // not in Figure 8
                if (matchHelper(bijectionRec, neighborsRec, fullGraph, candidate, startTime))
                    return true;
            }

        }
        return false;
    }

    private double cost(DirectedPseudograph<Resource, Edge> candidate) {
        return candidate.vertexSet().size() + candidate.edgeSet().size();
    }
}
