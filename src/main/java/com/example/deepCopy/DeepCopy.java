package com.example.deepCopy;

import com.example.deepCopy.model.Man;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class DeepCopy {

    public static <T> T deepCopy(T original) {
        if (original == null) {
            return null;
        }

        Map<Object, Object> visited = new IdentityHashMap<>();
        return deepCopyRecursive(original, visited);
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepCopyRecursive(T original, Map<Object, Object> visited) {
        if (visited.containsKey(original)) {
            return (T) visited.get(original);
        }

        Class<?> clazz = original.getClass();

        if (isPrimitiveOrImmutable(clazz) || clazz.isArray()) {
            return original;
        }

        if (original instanceof Collection) {
            Collection<?> originalCollection = (Collection<?>) original;
            Collection<Object> newCollection = originalCollection.stream()
                    .map(item -> deepCopyRecursive(item, visited))
                    .collect(Collectors.toCollection(() -> instantiateCollection(originalCollection)));
            visited.put(original, newCollection);
            return (T) newCollection;
        }

        if (original instanceof Map) {
            Map<?, ?> originalMap = (Map<?, ?>) original;
            Map<Object, Object> newMap = originalMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> deepCopyRecursive(entry.getKey(), visited),
                            entry -> deepCopyRecursive(entry.getValue(), visited),
                            (oldValue, newValue) -> oldValue, // Merge function (retain old value)
                            LinkedHashMap::new // Preserve order
                    ));
            visited.put(original, newMap);
            return (T) newMap;
        }

        T newInstance = (T) instantiateClass(clazz);
        visited.put(original, newInstance);

        for (Field field : getAllFields(clazz)) {
            try {
                field.setAccessible(true);
                Object value = field.get(original);
                if (value != null) {
                    field.set(newInstance, deepCopyRecursive(value, visited));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return newInstance;
    }

    private static <T> boolean isPrimitiveOrImmutable(Class<T> clazz) {
        return clazz.isPrimitive() || clazz == String.class || Number.class.isAssignableFrom(clazz) ||
                Boolean.class == clazz || Character.class == clazz || clazz.isEnum();
    }

    private static <T> Collection<Object> instantiateCollection(Collection<T> originalCollection) {
        if (originalCollection instanceof List) {
            return new ArrayList<>();
        } else if (originalCollection instanceof Set) {
            return new HashSet<>();
        } else {
            throw new UnsupportedOperationException("Unsupported collection type: " + originalCollection.getClass());
        }
    }

    private static <T> T instantiateClass(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate class: " + clazz.getSimpleName(), e);
        }
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    public static void main(String[] args) {
        // Test the deep copy utility
        List<String> favoriteBooks = new ArrayList<>(Arrays
                .asList("Paulo Coelho the alchemist", " Richard Bach Jonathan Livingston"));
        Man originalMan = new Man("John Doe", 30, favoriteBooks);

        Man copiedMan = DeepCopy.deepCopy(originalMan);

        // Verify the deep copy
        System.out.println("Original Man: " + originalMan);
        System.out.println("Copied Man: " + copiedMan);

        // Modify the original and check if the copy remains unchanged
        originalMan.setName("Jane Doe");
        originalMan.getFavoriteBooks().add("Fahrenheit 451");

        System.out.println("Modified Original Man: " + originalMan);
        System.out.println("Unmodified Copied Man: " + copiedMan);
    }
}
