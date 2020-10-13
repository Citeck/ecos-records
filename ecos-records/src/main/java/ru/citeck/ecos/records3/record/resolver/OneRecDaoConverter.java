package ru.citeck.ecos.records3.record.resolver;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.delete.DelStatus;
import ru.citeck.ecos.records3.record.op.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.op.delete.RecordsDeleteDao;
import ru.citeck.ecos.records3.record.op.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.op.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.op.mutate.RecordMutateDao;
import ru.citeck.ecos.records3.record.op.mutate.RecordsMutateDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public class OneRecDaoConverter {

    public RecordsDao convertOneToMultiDao(RecordsDao dao) {
        if (dao instanceof RecordAttsDao) {
            return mapToMultiDao((RecordAttsDao) dao);
        }
        if (dao instanceof RecordDeleteDao) {
            return mapToMultiDao((RecordDeleteDao) dao);
        }
        if (dao instanceof RecordMutateDao) {
            return mapToMultiDao((RecordMutateDao) dao);
        }
        return dao;
    }

    private RecordsAttsDao mapToMultiDao(RecordAttsDao dao) {
        return new RecordsAttsDao() {
            @Override
            public List<?> getRecordsAtts(@NotNull List<String> records) {
                return mapElements(
                    records,
                    dao::getRecordAtts,
                    v -> EmptyAttValue.INSTANCE,
                    (v, e) -> ObjectData.create()
                );
            }

            @Override
            public String getId() {
                return dao.getId();
            }
        };
    }

    public RecordsDeleteDao mapToMultiDao(RecordDeleteDao dao) {
        return new RecordsDeleteDao() {

            @Override
            public List<DelStatus> delete(@NotNull List<String> records) {
                return mapElements(
                    records,
                    dao::delete,
                    v -> DelStatus.OK,
                    (v, e) -> {
                        ExceptionUtils.throwException(e);
                        throw new RuntimeException("Unreachable code");
                    }
                );
            }

            @Override
            public String getId() {
                return dao.getId();
            }
        };
    }

    public RecordsMutateDao mapToMultiDao(RecordMutateDao dao) {
        return new RecordsMutateDao() {
            @Override
            public List<RecordRef> mutate(@NotNull List<RecordAtts> records) {
                return mapElements(
                    records,
                    dao::mutate,
                    RecordAtts::getId,
                    (v, e) -> {
                        ExceptionUtils.throwException(e);
                        throw new RuntimeException("Unreachable code");
                    }
                );
            }

            @Override
            public String getId() {
                return dao.getId();
            }
        };
    }

    private <T, R> List<R> mapElements(List<T> input,
                                       Function<T, R> mapFunc,
                                       Function<T, R> onEmpty,
                                       BiFunction<T, Exception, R> onError) {

        List<R> result = new ArrayList<>();
        for (T value : input) {
            try {
                R res = mapFunc.apply(value);
                if (res == null) {
                    res = onEmpty.apply(value);
                }
                result.add(res);
            } catch (Exception e) {
                log.error("Mapping failed", e);
                RequestContext.getCurrentNotNull().addMsg(MsgLevel.ERROR,
                    () -> ErrorUtils.convertException(e));
                result.add(onError.apply(value, e));
            }
        }
        return result;
    }
}
