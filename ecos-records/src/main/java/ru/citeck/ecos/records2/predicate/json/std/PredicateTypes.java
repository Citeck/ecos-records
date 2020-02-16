package ru.citeck.ecos.records2.predicate.json.std;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.predicate.model.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PredicateTypes {

    private Map<String, Class<? extends Predicate>> predicateTypes = new ConcurrentHashMap<>();

    public PredicateTypes() {
        register(OrPredicate.class);
        register(AndPredicate.class);
        register(NotPredicate.class);
        register(ValuePredicate.class);
        register(EmptyPredicate.class);
    }

    public Class<? extends Predicate> get(String type) {
        return predicateTypes.get(type);
    }

    public List<Class<? extends Predicate>> getTypes() {
        return new ArrayList<>(predicateTypes.values());
    }

    public void register(Class<? extends Predicate> type) {

        Method getTypes = null;

        try {
            getTypes = type.getMethod("getTypes");
        } catch (NoSuchMethodException e) {
            log.error("Method getTypes not found in " + type, e);
        }

        if (getTypes == null) {
            throw new RuntimeException("Predicate type should have static method getTypes");
        }

        try {

            @SuppressWarnings("unchecked")
            Collection<String> types = (Collection<String>) getTypes.invoke(null);

            types.forEach(n ->
                predicateTypes.put(n, type)
            );

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
