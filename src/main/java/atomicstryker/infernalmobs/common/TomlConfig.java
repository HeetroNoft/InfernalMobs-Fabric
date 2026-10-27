package atomicstryker.infernalmobs.common;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stores config objects as TOML files. Gson still maps the object to a tree (so field names stay
 * the same as in the old json .cfg), night-config turns that tree into TOML and back.
 */
public class TomlConfig {

    private static final Gson gson = new Gson();

    public static <T> T loadConfigWithDefault(Class<T> clazz, File file, T defaultInstance) {
        try {
            if (!file.exists()) {
                saveConfig(defaultInstance, file);
                return defaultInstance;
            }
            String toml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Config parsed = TomlFormat.instance().createParser().parse(new StringReader(toml));
            return gson.fromJson(toJson(parsed), clazz);
        } catch (IOException | ParsingException e) {
            throw new UnsupportedOperationException("Config failed parsing toml file " + file.getAbsolutePath() + ", read logfile", e);
        }
    }

    public static void saveConfig(Object config, File file) {
        try {
            String tomlNew = toTomlString(config);
            if (file.exists()) {
                String tomlOld = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                if (tomlNew.equals(tomlOld)) {
                    // no change to config, dont torture the user SSD with pointless writes
                    return;
                }
            }
            Files.writeString(file.toPath(), tomlNew, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Config failed saving toml file somehow, read logfile", e);
        }
    }

    /**
     * Converts a pre-TOML json config file into the new TOML file, keeping the old one as a backup
     */
    public static <T> void migrateLegacyJson(Class<T> clazz, File legacyFile, File tomlFile) {
        if (tomlFile.exists() || !legacyFile.exists()) {
            return;
        }
        try {
            T legacy = gson.fromJson(Files.readString(legacyFile.toPath(), StandardCharsets.UTF_8), clazz);
            if (legacy != null) {
                saveConfig(legacy, tomlFile);
                Files.move(legacyFile.toPath(), legacyFile.toPath().resolveSibling(legacyFile.getName() + ".bak"));
            }
        } catch (Exception e) {
            InfernalMobsCore.LOGGER.error("Could not migrate legacy config {}, a default TOML config will be created", legacyFile.getAbsolutePath(), e);
        }
    }

    private static String toTomlString(Object config) {
        Config toml = fromJson(gson.toJsonTree(config).getAsJsonObject(), false);
        TomlWriter writer = TomlFormat.instance().createWriter();
        // item strings are json with lots of double quotes, single quoted literals keep them readable
        writer.setWriteStringLiteralPredicate(s -> s.indexOf('\'') < 0 && s.chars().noneMatch(c -> c < 0x20 || c == 0x7f));
        writer.setIndentArrayElementsPredicate(list -> !list.isEmpty());
        StringWriter out = new StringWriter();
        writer.write(toml, out);
        return out.toString();
    }

    private static Config fromJson(JsonObject json, boolean sortKeys) {
        Config toml = TomlFormat.newConfig(LinkedHashMap::new);
        Map<String, JsonElement> entries = sortKeys ? new TreeMap<>(json.asMap()) : json.asMap();
        // put into the value map directly, Config.set would split keys containing dots into sub tables
        entries.forEach((key, value) -> {
            if (!value.isJsonNull()) {
                toml.valueMap().put(key, fromJson(value));
            }
        });
        return toml;
    }

    private static Object fromJson(JsonElement json) {
        if (json.isJsonObject()) {
            // nested tables are entity/modifier maps backed by HashMaps, sort them for a stable file
            return fromJson(json.getAsJsonObject(), true);
        }
        if (json.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                list.add(fromJson(element));
            }
            return list;
        }
        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            String number = primitive.getAsNumber().toString();
            boolean decimal = number.contains(".") || number.contains("e") || number.contains("E") || number.contains("N") || number.contains("I");
            return decimal ? (Object) primitive.getAsDouble() : (Object) primitive.getAsLong();
        }
        return primitive.getAsString();
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof UnmodifiableConfig config) {
            JsonObject json = new JsonObject();
            config.valueMap().forEach((key, child) -> json.add(key, toJson(child)));
            return json;
        }
        if (value instanceof List<?> list) {
            JsonArray json = new JsonArray();
            list.forEach(child -> json.add(toJson(child)));
            return json;
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(value.toString());
    }
}
