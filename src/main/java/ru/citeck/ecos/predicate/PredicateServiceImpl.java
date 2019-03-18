package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ru.citeck.ecos.predicate.json.JsonConverter;
import ru.citeck.ecos.predicate.json.std.StdJsonConverter;
import ru.citeck.ecos.predicate.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PredicateServiceImpl implements PredicateService {

    private volatile JsonConverter jsonConverter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Predicate readJson(JsonNode predicateNode) {

        if (predicateNode == null) {
            return null;
        }

        JsonNode objNode = predicateNode;

        if (objNode.isTextual()) {
            try {
                objNode = objectMapper.readTree(objNode.asText());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (objNode instanceof ObjectNode) {
            return getJsonConverter().fromJson((ObjectNode) objNode);
        }

        return null;
    }

    @Override
    public Predicate readJson(String predicateJson) {
        return readJson(TextNode.valueOf(predicateJson));
    }

    @Override
    public ObjectNode writeJson(Predicate predicate) {
        return getJsonConverter().toJson(optimize(predicate));
    }

    private Predicate optimize(Predicate predicate) {

        if (predicate instanceof ComposedPredicate) {

            ComposedPredicate comp = (ComposedPredicate) predicate;

            if ((comp instanceof AndPredicate
                    || comp instanceof OrPredicate) && comp.getPredicates().size() == 1) {

                predicate = optimize(comp.getPredicates().get(0));

            } else {

                List<Predicate> toAdd = null;
                List<Predicate> toRemove = null;

                for (Predicate child : comp.getPredicates()) {

                    Predicate optimized = optimize(child);

                    if (child != optimized) {
                        if (toAdd == null) {
                            toAdd = new ArrayList<>();
                            toRemove = new ArrayList<>();
                        }
                        toAdd.add(optimized);
                        toRemove.add(child);
                    }
                }

                if (toAdd != null) {
                    List<Predicate> predicates = new ArrayList<>(comp.getPredicates());
                    predicates.removeAll(toRemove);
                    predicates.addAll(toAdd);
                    comp.setPredicates(predicates);
                }
            }
        } else if (predicate instanceof NotPredicate) {

            NotPredicate not = (NotPredicate) predicate;
            not.setPredicate(optimize(not.getPredicate()));
        }

        return predicate;
    }

    private JsonConverter getJsonConverter() {
        if (jsonConverter == null) {
            synchronized (this) {
                if (jsonConverter == null) {
                    jsonConverter = new StdJsonConverter();
                }
            }
        }
        return jsonConverter;
    }

    public void setJsonConverter(JsonConverter jsonConverter) {
        synchronized (this) {
            this.jsonConverter = jsonConverter;
        }
    }
}
