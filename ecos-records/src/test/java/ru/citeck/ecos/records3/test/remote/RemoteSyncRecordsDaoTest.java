package ru.citeck.ecos.records3.test.remote;

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
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RemoteSyncRecordsDaoTest {

    private static final String REMOTE_SOURCE_ID = "remote/remote-source";
    private static final int TOTAL_RECS = 200;

    private RecordsServiceFactory recordsServiceFactory;
    private RecordsService recordsService;
    private RecordsWithMetaSource recordsWithMetaSource;
    private RemoteSyncRecordsDao<ValueDto> remoteSyncRecordsDao;

    @BeforeAll
    void setup() {

        RecordsServiceFactory remoteFactory = new RecordsServiceFactory();

        RecordsServiceFactory localFactory = new RecordsServiceFactory() {
            @Override
            protected RemoteRecordsResolver createRemoteRecordsResolver() {
            return new RemoteRecordsResolver(this, new RemoteRecordsRestApi() {
                @Override
                public <T> T jsonPost(String url, Object request, Class<T> respType) {
                    @SuppressWarnings("unchecked")
                    T res = (T) remoteFactory.getRestHandlerAdapter().queryRecords(request);
                    return Json.getMapper().convert(res, respType);
                }
            });
            }
        };
        recordsServiceFactory = localFactory;

        this.recordsService = localFactory.getRecordsServiceV1();

        recordsWithMetaSource = new RecordsWithMetaSource();
        remoteFactory.getRecordsServiceV1().register(recordsWithMetaSource);

        remoteSyncRecordsDao = new RemoteSyncRecordsDao<>(REMOTE_SOURCE_ID, ValueDto.class);
        this.recordsService.register(remoteSyncRecordsDao);
    }

    @Test
    void test() {

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(REMOTE_SOURCE_ID)
            .withQuery(VoidPredicate.INSTANCE)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .build();

        RecsQueryRes<RecordRef> result = recordsService.query(query);
        assertEquals(TOTAL_RECS, result.getTotalCount());

        assertEquals(new HashSet<>(recordsWithMetaSource.values), new HashSet<>(remoteSyncRecordsDao.getRecords().values()));

        ValueDto dto = recordsService.getAtts(RecordRef.create("remote", "remote-source", "id-100"), ValueDto.class);
        ValueDto origDto = recordsWithMetaSource.values.stream().filter(v -> v.getId().equals("id-100")).findFirst().orElse(null);

        assertEquals(origDto, dto);

        Predicate predicate = Predicates.eq("attributes.attKey?str", origDto.attributes.get("attKey").asText());
        RecordsQuery query1 = RecordsQuery.create()
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withSourceId(REMOTE_SOURCE_ID)
            .withQuery(predicate)
            .build();
        RecsQueryRes<RecordRef> recs = RequestContext.doWithCtx(recordsServiceFactory, ctx -> {
            try {
                return recordsService.query(query1);
            } finally {
                assertEquals(0, ctx.getErrors().size());
            }
        });
        assertEquals(1, recs.getRecords().size());
        assertEquals(RecordRef.valueOf(REMOTE_SOURCE_ID + "@id-100"), recs.getRecords().get(0));

        RecsQueryRes<ValueDto> resultWithMeta = RequestContext.doWithCtx(recordsServiceFactory, ctx -> {
            try {
                return recordsService.query(query1, ValueDto.class);
            } finally {
                assertEquals(0, ctx.getErrors().size());
            }
        });
        assertEquals(1, resultWithMeta.getRecords().size());
        assertEquals(origDto, resultWithMeta.getRecords().get(0));
    }

    static class RecordsWithMetaSource extends AbstractRecordsDao
        implements RecordsQueryDao {

        static final String ID = "remote-source";

        @Getter
        private final List<ValueDto> values = new ArrayList<>();

        RecordsWithMetaSource() {

            Random random = new Random();

            for (int i = 0; i < TOTAL_RECS; i++) {

                ValueDto dto = new ValueDto();
                dto.setAttributes(ObjectData.create("{\"attKey\":\"" + UUID.randomUUID() + "\"}"));
                dto.setId("id-" + i);

                MLText mlText = new MLText()
                    .withValue(Locale.ENGLISH, "en_value_" + i)
                    .withValue(new Locale("ru"), "ру_знач_" + i);
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
        public RecsQueryRes<ValueDto> queryRecords(RecordsQuery query) {

            if (!query.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {
                throw new IllegalArgumentException("Language is not supported! " + query.getLanguage());
            }

            if (query.getSortBy().size() != 1
                    || !query.getSortBy().get(0).getAttribute().equals(RecordConstants.ATT_MODIFIED)) {
                throw new IllegalArgumentException("Expected 1 SortBy with "
                    + RecordConstants.ATT_MODIFIED + " field. " + query);
            }

            log.info("Query for " + query.getPage().getMaxItems() + " records");

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
            while (idx < sortedValues.size() && result.size() < query.getPage().getMaxItems()) {
                ValueDto value = sortedValues.get(idx);
                if (value.getModified().isAfter(fromModified)) {
                    result.add(value);
                }
                idx++;
            }

            RecsQueryRes<ValueDto> queryResult = new RecsQueryRes<>(result);
            queryResult.setTotalCount(result.size() + sortedValues.size() - idx);

            log.info("Result: " + queryResult);
            return queryResult;
        }

        @Override
        public String getId() {
            return ID;
        }
    }

    @Data
    public static class ValueDto implements Comparable<ValueDto> {
        @AttName("_modified")
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
