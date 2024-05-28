package com.zenith.generator;

import com.squareup.javapoet.*;
import com.zenith.DataGenerator;
import com.zenith.util.Registry;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

public abstract class RegistryGenerator<T> implements Generator {
    protected final Class<T> dataType;
    protected final String classPackage;
    protected final String registryClassName;

    public RegistryGenerator(Class<T> dataType, String classPackage, String registryClassName) {
        this.dataType = dataType;
        this.classPackage = classPackage;
        this.registryClassName = registryClassName;
    }

    public abstract List<T> buildDataList();
    public abstract String dataNameMapper(T data);
    public abstract int idMapper(T data);
    public abstract CodeBlock dataInitializer(T data);

    @Override
    public void generate() {
        List<T> dataList = buildDataList();

        var registryField = FieldSpec
            .builder(ParameterizedTypeName.get(Registry.class, dataType), "REGISTRY")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new Registry<>(" + dataList.size() + ")")
            .build();

        var registerMethod = MethodSpec.methodBuilder("register")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(dataType, "value")
            .returns(dataType)
            .addStatement("$N.put(value.id(), value)", registryField)
            .addStatement("return value")
            .build();

        var dataInstanceFields = dataList.stream()
            .map(dataInstance -> FieldSpec.builder(dataType, dataNameMapper(dataInstance).toUpperCase())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(CodeBlock.builder()
                                 .add("$N($L)", registerMethod, dataInitializer(dataInstance))
                                 .build())
                .build())
            .toList();

        var typeSpec = TypeSpec.classBuilder(registryClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(registryField)
            .addMethod(registerMethod)
            .addFields(dataInstanceFields)
            .build();
        var javaFile = JavaFile.builder(classPackage, typeSpec).indent("    ").build();
        try {
            javaFile.writeTo(DataGenerator.dataDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("{} generated with {} instances!", registryClassName, dataList.size());
    }

}
