package ru.citeck.ecos.records.test.schema;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.record.op.meta.schema.GqlKeyUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GqlKeyUtilsTest {

    @Test
    public void test() {
        assertEsc("_u002E_disp", ".disp");
        assertEsc("_u002E__u002E_disp", "..disp");
        assertEsc("_u002E__u002E_di_u002E_sp", "..di.sp");
        assertEsc("_u002E__u002E_d_u002D__u002D_i_u002E_s_p", "..d--i.s_p");
    }

    private void assertEsc(String expected, String source) {
        String res = GqlKeyUtils.escape(source);
        assertEquals(expected, res);
        assertEquals(source, GqlKeyUtils.unescape(res));
    }
}
