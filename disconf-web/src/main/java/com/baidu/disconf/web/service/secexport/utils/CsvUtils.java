package com.baidu.disconf.web.service.secexport.utils;

import java.util.List;

/**
 * CSV 构建工具
 * <p/>
 * 安全要求：
 * 1. 单元格内容做 CSV 公式注入防护（= + - @ \t \r 开头的单元格前置单引号），防止导出文件在 Excel 中执行恶意公式；
 * 2. 输出统一 UTF-8 并携带 BOM，保证 Excel 正确识别中文；
 * 3. 仅由服务端生成的表头与数据参与拼接，不拼接任何用户输入的文件名/路径。
 *
 * @author secexport
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    /** UTF-8 BOM，使 Excel 以 UTF-8 打开 */
    public static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * 构建 CSV 全文（含表头）
     *
     * @param headers 表头（服务端定义）
     * @param rows    数据行（服务端数据源）
     */
    public static String buildCsv(String[] headers, List<String[]> rows) {

        StringBuilder sb = new StringBuilder();

        // 表头
        appendRow(sb, headers);

        // 数据行
        if (rows != null) {
            for (String[] row : rows) {
                appendRow(sb, row);
            }
        }

        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCell(cells[i]));
        }
        sb.append("\r\n");
    }

    /**
     * 单元格转义：
     * - 双引号转义为两个双引号并整体加引号；
     * - 包含逗号/引号/换行的单元格整体加引号；
     * - 以 = + - @ \t \r 开头的单元格前置单引号，防 CSV 公式注入。
     */
    static String escapeCell(String value) {

        if (value == null) {
            return "";
        }

        String v = value;
        boolean needQuote = false;

        // 公式注入防护
        if (v.length() > 0) {
            char c = v.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
                v = "'" + v;
                needQuote = true;
            }
        }

        if (v.indexOf('"') >= 0) {
            v = v.replace("\"", "\"\"");
            needQuote = true;
        }
        if (v.indexOf(',') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            needQuote = true;
        }

        if (needQuote) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
