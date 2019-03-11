package ru.citeck.ecos.predicate.type;

public class NotPredicate implements Predicate {

    private Predicate predicate;

    public NotPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public NotPredicate() {
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }
}
