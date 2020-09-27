package ru.citeck.ecos.records.test.local;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForceLocalRecordsTest {

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        RecordsService recordsService = factory.getRecordsService();

        recordsService.register(new TypesDao());

        TypeParents typeInfo = recordsService.getAtts(TypesDao.type2Ref, TypeParents.class);

        List<RecordRef> fromQuery = new ArrayList<>(typeInfo.parents);
        List<RecordRef> expected = new ArrayList<>(Arrays.asList(TypesDao.type1Ref, TypesDao.type0Ref));
        assertEquals(expected, fromQuery);
    }

    @Data
    public static class TypeParents {
        private List<RecordRef> parents;
    }

    public static class TypesDao extends LocalRecordsDao implements LocalRecordsMetaDao {

        public static final String ID = "emodel/type";

        public static final String type0Id = "type0";
        public static final RecordRef type0Ref = RecordRef.valueOf(ID + "@" + type0Id);
        public static final String type1Id = "type1";
        public static final RecordRef type1Ref = RecordRef.valueOf(ID + "@" + type1Id);
        public static final String type2Id = "type2";
        public static final RecordRef type2Ref = RecordRef.valueOf(ID + "@" + type2Id);

        private Map<String, Record> records = new HashMap<>();

        public TypesDao() {
            setId(ID);

            records.put(type2Id, new Record(type2Id, Arrays.asList(
                RecordRef.valueOf(getId() + "@" + type1Id),
                RecordRef.valueOf(getId() + "@" + type0Id)
            )));
            records.put(type1Id, new Record(type1Id, Collections.singletonList(
                RecordRef.valueOf(getId() + "@" + type0Id)))
            );
            records.put(type0Id, new Record(type0Id, Collections.emptyList()));
        }

        @Override
        public List<Object> getLocalRecordsMeta(List<RecordRef> records) {
            return records.stream().map(r -> this.records.get(r.getId())).collect(Collectors.toList());
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Record {
            private String id;
            private List<RecordRef> parents = new ArrayList<>();
        }
    }
}
