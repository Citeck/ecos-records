package ru.citeck.ecos.records3.test.local;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

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

        List<EntityRef> fromQuery = new ArrayList<>(typeInfo.parents);
        List<EntityRef> expected = new ArrayList<>(Arrays.asList(TypesDao.type1Ref, TypesDao.type0Ref));
        assertEquals(expected, fromQuery);
    }

    @Data
    public static class TypeParents {
        private List<EntityRef> parents;
    }

    public static class TypesDao extends AbstractRecordsDao implements RecordsAttsDao {

        public static final String ID = "emodel/type";

        public static final String type0Id = "type0";
        public static final EntityRef type0Ref = EntityRef.valueOf(ID + "@" + type0Id);
        public static final String type1Id = "type1";
        public static final EntityRef type1Ref = EntityRef.valueOf(ID + "@" + type1Id);
        public static final String type2Id = "type2";
        public static final EntityRef type2Ref = EntityRef.valueOf(ID + "@" + type2Id);

        private Map<String, Record> records = new HashMap<>();

        @NotNull
        @Override
        public String getId() {
            return ID;
        }

        public TypesDao() {
            records.put(type2Id, new Record(type2Id, Arrays.asList(
                EntityRef.valueOf(ID + "@" + type1Id),
                EntityRef.valueOf(ID + "@" + type0Id)
            )));
            records.put(type1Id, new Record(type1Id, Collections.singletonList(
                EntityRef.valueOf(ID + "@" + type0Id)))
            );
            records.put(type0Id, new Record(type0Id, Collections.emptyList()));
        }

        @Override
        public List<?> getRecordsAtts(List<String> records) {
            return records.stream().map(r -> this.records.get(r)).collect(Collectors.toList());
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Record {
            private String id;
            private List<EntityRef> parents = new ArrayList<>();
        }
    }
}
