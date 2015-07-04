/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package ar.edu.famaf.nlp.alusivo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * 
 * A referring expression, as set of predicates. Currently the predicates can be
 * positive or negative.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */
public class ReferringExpression implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected transient URI referent; // the referent is used during
                                      // construction

    public ReferringExpression() {
        // this constructor is used for de-serialization
        this.predicates = new ArrayList<Predicate>();
    }

    public ReferringExpression(URI referent) {
        this();
        this.referent = referent;
    }

    public class Predicate {

        // might be null to reference the referent

        private URI subject;

        private URI predicate;

        private Value object;

        private boolean negative;

        protected Predicate() {
            // for de-serialziation
        }

        protected Predicate(Statement stmt) {
            this(stmt, false);
        }

        protected Predicate(Resource subject, URI predicate, Value object) {
            this(subject, predicate, object, false);
        }

        protected Predicate(Statement stmt, boolean negative) {
            this(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(), negative);
        }

        protected Predicate(Resource subject, URI predicate, Value object, boolean negative) {
            assert referent != null;
            this.subject = subject == null || subject.equals(referent) ? null : (URI) subject;
            this.predicate = predicate;
            this.object = object == null || object.equals(referent) ? null : object;
            this.negative = negative;
        }

        public URI getSubject() {
            return subject;
        }

        public URI getPredicate() {
            return predicate;
        }

        public Value getObject() {
            return object;
        }

        public boolean isNegative() {
            return negative;
        }

        public String toString() {
            return subject.toString() + "\t" + (negative ? "NOT" : "") + "\t" + predicate.toString() + "\t"
                    + object.toString();
        }
    }

    private List<Predicate> predicates;

    public List<Predicate> predicates() {
        return Collections.unmodifiableList(predicates);
    }

    public void addPositive(Statement stmt) {
        this.predicates.add(new Predicate(stmt));
    }

    public void addPositive(Resource subject, URI predicate, Value object) {
        this.predicates.add(new Predicate(subject, predicate, object));
    }

    public void addNegative(Statement stmt) {
        this.predicates.add(new Predicate(stmt, true));
    }

    public void addNegative(Resource subject, URI predicate, Value object) {
        this.predicates.add(new Predicate(subject, predicate, object, true));
    }

    public boolean hasNegatives() {
        for (Predicate pred : predicates)
            if (pred.isNegative())
                return true;
        return false;
    }
}