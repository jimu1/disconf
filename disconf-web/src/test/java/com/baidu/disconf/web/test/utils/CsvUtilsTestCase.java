package com.baidu.disconf.web.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.baidu.disconf.web.service.secexport.utils.CsvUtils;

/**
 * CsvUtils 单元测试：通过公开的 buildCsv 覆盖 CSV 公式注入防护与特殊字符转义
 *
 * @author secexport
 */
public class CsvUtilsTestCase {

    private String singleCell(String cell) {

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[]{cell});
        String csv = CsvUtils.buildCsv(new String[]{"H"}, rows);
        // 去掉表头行与行尾 CRLF，仅保留数据单元格的输出
        return csv.substring(csv.indexOf("\r\n") + 2, csv.length() - 2);
    }

    //
    // 公式注入防护：以 = + - @ \t \r 开头的单元格必须前置单引号
    //

    @Test
    public void formulaPrefixShouldBeNeutralized() {
        assertEquals("\"'=1+1\"", singleCell("=1+1"));
    }

    @Test
    public void plusPrefixShouldBeNeutralized() {
        assertEquals("\"'+cmd\"", singleCell("+cmd"));
    }

    @Test
    public void minusPrefixShouldBeNeutralized() {
        assertEquals("\"'-1\"", singleCell("-1"));
    }

    @Test
    public void atPrefixShouldBeNeutralized() {
        assertEquals("\"'@sum(a1:a2)\"", singleCell("@sum(a1:a2)"));
    }

    @Test
    public void tabPrefixShouldBeNeutralized() {
        assertEquals("\"'\t9\"", singleCell("\t9"));
    }

    //
    // 特殊字符转义：逗号 / 双引号 / 换行
    //

    @Test
    public void commaShouldBeQuoted() {
        assertEquals("\"a,b\"", singleCell("a,b"));
    }

    @Test
    public void doubleQuoteShouldBeEscaped() {
        assertEquals("\"say\"\"hi\"\"\"", singleCell("say\"hi\""));
    }

    @Test
    public void newlineShouldBeQuoted() {
        assertEquals("\"a\r\nb\"", singleCell("a\r\nb"));
    }

    //
    // 普通值与空值
    //

    @Test
    public void plainValueShouldKeepUnchanged() {
        assertEquals("10.10.1.11", singleCell("10.10.1.11"));
    }

    @Test
    public void nullCellShouldBeEmpty() {
        assertEquals("", singleCell(null));
    }

    //
    // buildCsv：整体拼接（RFC 4180，CRLF 换行）
    //

    @Test
    public void buildCsvShouldJoinHeaderAndRowsWithCrlf() {

        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[]{"1", "demoApp", "10.10.1.11"});
        rows.add(new String[]{"2", "=HYPERLINK(\"http://evil\")", "10.10.1.12"});

        String csv = CsvUtils.buildCsv(new String[]{"ID", "应用", "主机"}, rows);

        assertEquals("ID,应用,主机\r\n"
                + "1,demoApp,10.10.1.11\r\n"
                + "2,\"'=HYPERLINK(\"\"http://evil\"\")\",10.10.1.12\r\n", csv);
    }

    @Test
    public void buildCsvShouldHandleNullRows() {

        String csv = CsvUtils.buildCsv(new String[]{"A", "B"}, null);
        assertEquals("A,B\r\n", csv);
    }

    @Test
    public void utf8BomShouldBeExpectedBytes() {

        assertTrue(Arrays.equals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, CsvUtils.UTF8_BOM));
    }
}
