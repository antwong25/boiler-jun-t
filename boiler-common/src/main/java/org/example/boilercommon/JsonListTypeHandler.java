package org.example.boilercommon;

import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.example.boilerpojo.MessageItem;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * 自定义 MyBatis 类型处理器：实现 List<MessageItem> 与数据库 TEXT(JSON) 字段的双向转换。
 * 使用 FastJSON2 进行序列化与反序列化，兼容空值、空字符串、空 JSON 数组场景。
 */
@MappedTypes(List.class)
public class JsonListTypeHandler extends BaseTypeHandler<List<MessageItem>> {

    /**
     * 将 Java 端的 List<MessageItem> 序列化为 JSON 字符串，写入 PreparedStatement。
     * 空集合序列化为 "[]"，null 序列化为 "[]" 以确保数据库不为 NULL。
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    List<MessageItem> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setString(i, "[]");
        } else {
            ps.setString(i, JSON.toJSONString(parameter));
        }
    }

    /**
     * 从 ResultSet 按列名读取 JSON 字符串，反序列化为 List<MessageItem>。
     */
    @Override
    public List<MessageItem> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJsonToList(rs.getString(columnName));
    }

    /**
     * 从 ResultSet 按列索引读取 JSON 字符串，反序列化为 List<MessageItem>。
     */
    @Override
    public List<MessageItem> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJsonToList(rs.getString(columnIndex));
    }

    /**
     * 从 CallableStatement 按列索引读取 JSON 字符串，反序列化为 List<MessageItem>。
     */
    @Override
    public List<MessageItem> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJsonToList(cs.getString(columnIndex));
    }

    /**
     * 将 JSON 字符串安全解析为 List<MessageItem>。
     * 对 null、空字符串、空 JSON 数组均返回空集合，避免 NPE。
     */
    private List<MessageItem> parseJsonToList(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank() || "null".equalsIgnoreCase(jsonStr.trim())) {
            return Collections.emptyList();
        }
        try {
            List<MessageItem> result = JSON.parseArray(jsonStr, MessageItem.class);
            return result == null ? Collections.emptyList() : result;
        } catch (Exception e) {
            // 解析异常时记录日志并返回空集合，避免单条脏数据影响整体查询
            System.err.println("JsonListTypeHandler 解析 messageList JSON 失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
