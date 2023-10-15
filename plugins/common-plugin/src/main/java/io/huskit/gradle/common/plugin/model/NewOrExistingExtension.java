package io.huskit.gradle.common.plugin.model;

import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;
import org.gradle.api.plugins.ExtensionContainer;

import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class NewOrExistingExtension {

    private final Log log;
    private final ExtensionContainer extensions;

    @SuppressWarnings("unchecked")
    public <T, V extends T> V getOrCreate(Class<T> publicType,
                                          Class<V> instanceType,
                                          String name,
                                          Supplier<List<Object>> argsSupplier) {
        V extension = (V) extensions.findByName(name);
        if (extension == null) {
            log.info("Extension [{}] not found, creating new instance", name);
            List<Object> args = argsSupplier.get();
            if (args.isEmpty()) {
                extension = (V) extensions.create(publicType, name, instanceType);
            } else {
                extension = (V) extensions.create(publicType, name, instanceType, args.toArray());
            }
        } else {
            log.info("Extension [{}] found, using existing instance", name);
        }
        return extension;
    }

    @SuppressWarnings("unchecked")
    public <T, V extends T> V getOrCreate(Class<T> publicType,
                                          Class<V> instanceType,
                                          String name) {
        return this.getOrCreate(publicType, instanceType, name, List::of);
    }
}
