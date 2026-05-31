package tv.cinepilot.core.protocol;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

final class JsonPayload {
    private JsonPayload() {
    }

    static String object(Map<String, ?> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            appendValue(builder, entry.getValue());
            first = false;
        }
        return builder.append('}').toString();
    }

    private static void appendValue(StringBuilder builder, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(builder, map);
        } else if (value instanceof Iterable<?> iterable) {
            appendIterable(builder, iterable);
        } else if (value.getClass().isArray()) {
            appendArray(builder, value);
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void appendMap(StringBuilder builder, Map<?, ?> values) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
            appendValue(builder, entry.getValue());
            first = false;
        }
        builder.append('}');
    }

    private static void appendIterable(StringBuilder builder, Iterable<?> values) {
        builder.append('[');
        Iterator<?> iterator = values.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (!first) {
                builder.append(',');
            }
            appendValue(builder, value);
            first = false;
        }
        builder.append(']');
    }

    private static void appendArray(StringBuilder builder, Object values) {
        builder.append('[');
        int length = Array.getLength(values);
        for (int index = 0; index < length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            appendValue(builder, Array.get(values, index));
        }
        builder.append(']');
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
