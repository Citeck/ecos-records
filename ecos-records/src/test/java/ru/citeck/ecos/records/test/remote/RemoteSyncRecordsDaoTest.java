package ru.citeck.ecos.records.test.remote;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RemoteSyncRecordsDaoTest {

    private static final int TOTAL_RECS = 200;

    private RecordsService recordsService;
    private RecordsWithMetaSource recordsWithMetaSource;
    private RemoteSyncRecordsDAO<ValueDto> remoteSyncRecordsDAO;

    @BeforeAll
    void setup() {

        RecordsServiceFactory factory = new RecordsServiceFactory() {
            @Override
            protected RemoteRecordsResolver createRemoteRecordsResolver() {
            return new RemoteRecordsResolver(this, new RemoteRecordsRestApi() {
                @Override
                public <T> T jsonPost(String url, Object request, Class<T> respType) {
                    @SuppressWarnings("unchecked")
                    T res = (T) getRestHandler().queryRecords(
                        Objects.requireNonNull(Json.getMapper().convert(request, QueryBody.class))
                    );
                    return Json.getMapper().convert(res, respType);
                }
            });
            }
        };

        this.recordsService = factory.getRecordsService();
        recordsWithMetaSource = new RecordsWithMetaSource();
        this.recordsService.register(recordsWithMetaSource);
        remoteSyncRecordsDAO = new RemoteSyncRecordsDAO<>("remote/remote-source", ValueDto.class);
        this.recordsService.register(remoteSyncRecordsDAO);

        factory.init();
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId("remote/remote-source");
        query.setQuery(VoidPredicate.INSTANCE);
        query.setLanguage(PredicateService.LANGUAGE_PREDICATE);

        RecordsQueryResult<RecordRef> result = recordsService.queryRecords(query);
        assertEquals(TOTAL_RECS, result.getTotalCount());

        assertEquals(new HashSet<>(recordsWithMetaSource.values), new HashSet<>(remoteSyncRecordsDAO.getRecords()));

        ValueDto dto = recordsService.getMeta(RecordRef.create("remote", "remote-source", "id-100"), ValueDto.class);
        ValueDto origDto = recordsWithMetaSource.values.stream().filter(v -> v.getId().equals("id-100")).findFirst().orElse(null);

        assertEquals(origDto, dto);

        Predicate predicate = Predicates.eq("attributes.attKey?str", origDto.attributes.get("attKey").asText());
        RecordsQuery query1 = new RecordsQuery();
        query1.setLanguage(PredicateService.LANGUAGE_PREDICATE);
        query1.setSourceId("remote/remote-source");
        query1.setQuery(predicate);

        RecordsQueryResult<RecordRef> recs = recordsService.queryRecords(query1);
        assertEquals(0, recs.getErrors().size());
        assertEquals(1, recs.getRecords().size());
        assertEquals(RecordRef.valueOf("remote/remote-source@id-100"), recs.getRecords().get(0));

        RecordsQueryResult<ValueDto> resultWithMeta = recordsService.queryRecords(query1, ValueDto.class);
        assertEquals(0, resultWithMeta.getErrors().size());
        assertEquals(1, resultWithMeta.getRecords().size());
        assertEquals(origDto, resultWithMeta.getRecords().get(0));
    }

    static class RecordsWithMetaSource extends LocalRecordsDAO
        implements LocalRecordsQueryWithMetaDAO<ValueDto> {

        static final String ID = "remote-source";

        @Getter
        private final List<ValueDto> values = new ArrayList<>();

        RecordsWithMetaSource() {
            setId(ID);

            Random random = new Random();

            for (int i = 0; i < TOTAL_RECS; i++) {

                ValueDto dto = new ValueDto();
                dto.setAttributes(ObjectData.create("{\"attKey\":\"" + UUID.randomUUID() + "\"}"));
                dto.setId("id-" + i);

                MLText mlText = new MLText();
                mlText.set(Locale.ENGLISH, "en_value_" + i);
                mlText.set(new Locale("ru"), "ру_знач_" + i);
                dto.setName(mlText);

                dto.setModified(Instant.now().plus((long) (random.nextFloat() * 60_000_000), ChronoUnit.MILLIS));

                Inner inner = new Inner();

                inner.setBoolField(random.nextBoolean());
                inner.setField0(UUID.randomUUID().toString());
                inner.setField1(UUID.randomUUID().toString());
                dto.setInner(inner);

                values.add(dto);
            }
        }

        @Override
        public RecordsQueryResult<ValueDto> queryLocalRecords(RecordsQuery query, MetaField field) {

            if (!query.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {
                throw new IllegalArgumentException("Language is not supported! " + query.getLanguage());
            }

            if (query.getSortBy().size() != 1
                    || !query.getSortBy().get(0).getAttribute().equals(RecordConstants.ATT_MODIFIED)) {
                throw new IllegalArgumentException("Expected 1 SortBy with "
                    + RecordConstants.ATT_MODIFIED + " field. " + query);
            }

            log.info("Query for " + query.getMaxItems() + " records");

            Predicate predicate = query.getQuery(Predicate.class);
            if (!(predicate instanceof ValuePredicate)) {
                throw new IllegalArgumentException("Expected value predicate");
            }
            assertEquals(((ValuePredicate) predicate).getType(), ValuePredicate.Type.GT);
            assertEquals(((ValuePredicate) predicate).getAttribute(), RecordConstants.ATT_MODIFIED);

            Instant fromModified = Objects.requireNonNull(
                Json.getMapper().convert(((ValuePredicate) predicate).getValue(), Instant.class)
            );

            List<ValueDto> sortedValues = new ArrayList<>(values);
            sortedValues.sort(Comparator.comparing(ValueDto::getModified));

            List<ValueDto> result = new ArrayList<>();
            int idx = 0;
            while (idx < sortedValues.size() && result.size() < query.getMaxItems()) {
                ValueDto value = sortedValues.get(idx);
                if (value.getModified().isAfter(fromModified)) {
                    result.add(value);
                }
                idx++;
            }

            RecordsQueryResult<ValueDto> queryResult = new RecordsQueryResult<>(result);
            queryResult.setTotalCount(result.size() + sortedValues.size() - idx);
            return queryResult;
        }

        @Override
        public String getId() {
            return ID;
        }
    }

    @Data
    public static class ValueDto implements Comparable<ValueDto> {
        @MetaAtt("_modified")
        private Instant modified;
        private String id;
        private MLText name;
        private ObjectData attributes;
        private Inner inner;

        @Override
        public int compareTo(@NotNull ValueDto o) {
            return id.compareTo(o.getId());
        }
    }

    @Data
    public static class Inner {
        private String field0;
        private String field1;
        private boolean boolField;
    }
}
