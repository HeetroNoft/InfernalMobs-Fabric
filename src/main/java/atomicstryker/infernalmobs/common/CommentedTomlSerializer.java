package atomicstryker.infernalmobs.common;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * AutoConfig serializer writing a TOML file in the Forge/NeoForge layout: every value is preceded by its
 * {@link Comment} and its default. AutoConfig's own Toml4jConfigSerializer writes TOML without any comment.
 * Supports the field types used by {@link atomicstryker.infernalmobs.common.Config}: booleans, numbers,
 * strings, lists of strings and maps of strings to booleans or numbers.
 */
public class CommentedTomlSerializer<T extends ConfigData> implements ConfigSerializer<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Config definition;
    private final Class<T> configClass;

    public CommentedTomlSerializer(Config definition, Class<T> configClass) {
        this.definition = definition;
        this.configClass = configClass;
    }

    private Path getConfigPath() {
        return Utils.getConfigFolder().resolve(definition.name() + ".toml");
    }

    @Override
    public void serialize(T config) throws SerializationException {
        T defaults = createDefault();
        CommentedConfig toml = TomlFormat.newConfig(LinkedHashMap::new);
        for (Field field : getFields()) {
            // put into the value map directly, set() would split keys containing dots into sub tables
            toml.valueMap().put(field.getName(), toToml(Utils.getUnsafely(field, config)));
            String comment = buildComment(field, Utils.getUnsafely(field, defaults));
            if (comment != null) {
                toml.setComment(List.of(field.getName()), comment);
            }
        }
        try {
            Files.createDirectories(getConfigPath().getParent());
            TomlFormat.instance().createWriter().write(toml, getConfigPath(), WritingMode.REPLACE);
        } catch (IOException | RuntimeException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return createDefault();
        }
        CommentedConfig toml;
        try (Reader reader = Files.newBufferedReader(path)) {
            toml = TomlFormat.instance().createParser().parse(reader);
        } catch (IOException | ParsingException e) {
            throw new SerializationException(e);
        }

        // values missing from the file or of the wrong type keep their default
        T config = createDefault();
        for (Field field : getFields()) {
            Object value = toml.valueMap().get(field.getName());
            if (value == null) {
                continue;
            }
            Object converted = fromToml(field, value);
            if (converted == null) {
                LOGGER.warn("Config value {} = {} in {} is invalid, using the default", field.getName(), value, path.getFileName());
            } else {
                Utils.setUnsafely(field, config, converted);
            }
        }
        return config;
    }

    @Override
    public T createDefault() {
        return Utils.constructUnsafely(configClass);
    }

    private List<Field> getFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : configClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Forge layout: description lines start right after the #, then "# Default: x" and "# Range: ..."
     * when the comment ends with a range line
     */
    private static String buildComment(Field field, Object defaultValue) {
        Comment comment = field.getAnnotation(Comment.class);
        String text = comment == null ? "" : comment.value();
        if (defaultValue instanceof Boolean || defaultValue instanceof Number) {
            String defaultLine = " Default: " + defaultValue;
            int rangeStart = text.indexOf("\n Range:");
            if (rangeStart >= 0) {
                text = text.substring(0, rangeStart) + "\n" + defaultLine + text.substring(rangeStart);
            } else {
                text = text.isEmpty() ? defaultLine : text + "\n" + defaultLine;
            }
        }
        return text.isEmpty() ? null : text;
    }

    private static Object toToml(Object value) {
        if (value instanceof Map<?, ?> map) {
            // the runtime filled tables are sorted for a stable, readable file
            CommentedConfig table = TomlFormat.newConfig(LinkedHashMap::new);
            new TreeMap<>(map).forEach((key, entry) -> table.valueMap().put(String.valueOf(key), toToml(entry)));
            return table;
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Float number) {
            return number.doubleValue();
        }
        return value;
    }

    /**
     * @return the value converted to the field type, or null if it does not fit
     */
    private static Object fromToml(Field field, Object value) {
        Class<?> type = field.getType();
        if (type == boolean.class || type == Boolean.class) {
            return value instanceof Boolean ? value : null;
        }
        if (type == String.class) {
            return value instanceof String ? value : null;
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return value instanceof Number number ? toNumber(number, type) : null;
        }
        if (List.class.isAssignableFrom(type)) {
            if (!(value instanceof List<?> list)) {
                return null;
            }
            List<String> strings = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof String string) {
                    strings.add(string);
                }
            }
            return strings;
        }
        if (Map.class.isAssignableFrom(type)) {
            if (!(value instanceof UnmodifiableConfig table)) {
                return null;
            }
            Class<?> valueType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
            Map<String, Object> map = new LinkedHashMap<>();
            table.valueMap().forEach((key, entry) -> {
                Object converted = valueType == Boolean.class ? (entry instanceof Boolean ? entry : null)
                        : entry instanceof Number number ? toNumber(number, valueType) : null;
                if (converted != null) {
                    map.put(key, converted);
                }
            });
            return map;
        }
        return null;
    }

    /**
     * any number type is accepted, so writing 2 instead of 2.0 in the file works
     */
    private static Object toNumber(Number number, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return number.intValue();
        }
        if (type == long.class || type == Long.class) {
            return number.longValue();
        }
        if (type == float.class || type == Float.class) {
            return number.floatValue();
        }
        return number.doubleValue();
    }
}
