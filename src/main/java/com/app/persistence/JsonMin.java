package com.app.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analizador JSON mínimo (objetos, arrays, string, boolean, null, números) sin dependencias externas.
 * Suficiente para el formato de archivo de tareas de la aplicación.
 */
final class JsonMin {

    private final String src;
    private int pos;

    JsonMin(String src) {
        this.src = src;
        this.pos = 0;
    }

    static Object parse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON vacío");
        }
        JsonMin p = new JsonMin(json);
        Object v = p.parseValue();
        p.skipWs();
        if (p.pos < p.src.length()) {
            throw new IllegalArgumentException("JSON con contenido sobrante en posición " + p.pos);
        }
        return v;
    }

    private Object parseValue() {
        skipWs();
        if (pos >= src.length()) {
            throw new IllegalArgumentException("JSON incompleto");
        }
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> {
                expectLiteral("true");
                yield Boolean.TRUE;
            }
            case 'f' -> {
                expectLiteral("false");
                yield Boolean.FALSE;
            }
            case 'n' -> {
                expectLiteral("null");
                yield null;
            }
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
            default -> throw new IllegalArgumentException("Token inesperado en " + pos + ": '" + c + "'");
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        skipWs();
        Map<String, Object> map = new LinkedHashMap<>();
        if (peek('}')) {
            pos++;
            return map;
        }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            expect(':');
            Object val = parseValue();
            map.put(key, val);
            skipWs();
            if (peek('}')) {
                pos++;
                break;
            }
            expect(',');
        }
        return map;
    }

    private List<Object> parseArray() {
        expect('[');
        skipWs();
        List<Object> list = new ArrayList<>();
        if (peek(']')) {
            pos++;
            return list;
        }
        while (true) {
            list.add(parseValue());
            skipWs();
            if (peek(']')) {
                pos++;
                break;
            }
            expect(',');
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new IllegalArgumentException("Escape incompleto");
                }
                char e = src.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw new IllegalArgumentException("\\u incompleto");
                        }
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalArgumentException("Escape desconocido: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("String sin cerrar");
    }

    private Number parseNumber() {
        int start = pos;
        if (peek('-')) {
            pos++;
        }
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
            pos++;
        }
        if (peek('.')) {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if (peek('e') || peek('E')) {
            pos++;
            if (peek('-') || peek('+')) {
                pos++;
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        String num = src.substring(start, pos);
        try {
            if (num.contains(".") || num.toLowerCase().contains("e")) {
                return Double.parseDouble(num);
            }
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: " + num, e);
        }
    }

    private void expectLiteral(String lit) {
        if (!src.startsWith(lit, pos)) {
            throw new IllegalArgumentException("Literal esperado: " + lit);
        }
        pos += lit.length();
    }

    private void expect(char ch) {
        skipWs();
        if (pos >= src.length() || src.charAt(pos) != ch) {
            throw new IllegalArgumentException("Se esperaba '" + ch + "' en posición " + pos);
        }
        pos++;
    }

    private void skipWs() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                pos++;
            } else {
                break;
            }
        }
    }

    private boolean peek(char ch) {
        skipWs();
        return pos < src.length() && src.charAt(pos) == ch;
    }
}
