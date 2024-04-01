package org.example;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

public class BuilderProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<?  extends Element> elements = roundEnv.getElementsAnnotatedWith(OurMock.class);
        Set<Element> testClasses = new HashSet<>();
        Map<Element, Set<Element>> testClassToFieldsMap = new HashMap<>();
        Map<Element, String> fieldToConstructorMap = new HashMap<>();
        List<String> methodUUIDs = new ArrayList<>();
        for(Element element : elements) {

            Element enclosingTestClass = element.getEnclosingElement();
            testClasses.add(enclosingTestClass);
            if(!testClassToFieldsMap.containsKey(enclosingTestClass)) {
                testClassToFieldsMap.put(enclosingTestClass, new HashSet<>());
            }
            testClassToFieldsMap.get(enclosingTestClass).add(element);

            DeclaredType declaredFieldType = (DeclaredType) element.asType();
            TypeElement fieldTypeElement = (TypeElement) declaredFieldType.asElement();

            TypeSpec.Builder builder = TypeSpec.classBuilder(fieldTypeElement.getSimpleName()+"Mock")
                    .addSuperinterface(Mock.class)
                    .superclass(TypeName.get(element.asType()));

            for (Element enclosed: fieldTypeElement.getEnclosedElements()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, enclosed.getSimpleName());
                if (enclosed.getKind() == ElementKind.METHOD) {

                    String methodUUID = UUID.randomUUID().toString();
                    methodUUIDs.add(methodUUID);
                    ExecutableElement executableElement = (ExecutableElement) enclosed;
                    TypeName returnType = TypeName.get(executableElement.getReturnType());
                    boolean hasParams = !executableElement.getParameters().isEmpty();
                    String params = hasParams ? "methodParams" : "null";
                    MethodSpec.Builder methodBuilder = MethodSpec
                            .methodBuilder(executableElement.getSimpleName().toString());
                    methodBuilder.addCode("String uuid = \"" + methodUUID+"\";\n");
                    methodBuilder.addCode("OurMockito.setInvocation(this, uuid);\n");
                    CodeBlock.Builder verificationBlockBuilder = CodeBlock.builder()
                            .beginControlFlow("if(verificationMode)")
                            .addStatement("verificationMode = false;")
                            .addStatement("OurMockito.verifyCurrentMethodCalled();");

                    if(!returnType.equals(TypeName.VOID)) {
                        verificationBlockBuilder.addStatement("return null");
                    } else {
                        verificationBlockBuilder.addStatement("return");
                    }

                    verificationBlockBuilder.endControlFlow();
                    methodBuilder.addCode(verificationBlockBuilder.build());
                    if (hasParams) {
                        ParameterizedTypeName typeArrayList = ParameterizedTypeName.get(ArrayList.class, Object.class);
                        methodBuilder.addCode(CodeBlock.builder().addStatement("$T methodParams = new $T()", typeArrayList,
                                typeArrayList).build());
                    }
                    for (VariableElement variableElement: executableElement.getParameters()) {
                        methodBuilder.addParameter(ParameterSpec.get(variableElement));
                        if(hasParams) {
                            methodBuilder.addCode("methodParams.add("+variableElement.getSimpleName()+");\n");
                        }
                    }

                    methodBuilder.addCode("invocations.get(uuid).add("+params+");\n");
                    methodBuilder.addModifiers(Modifier.PUBLIC);
                    methodBuilder.addAnnotation(Override.class);
                    if(!returnType.equals(TypeName.VOID)) {
                        methodBuilder.returns(returnType);
                        methodBuilder.addCode("return OurMockito.<"+returnType+">getReturnVal(\""+methodUUID+"\","+params+");");
                    }
                    builder.addMethod(methodBuilder.build());
                }
            }

            ClassName hashmapName = ClassName.get(HashMap.class);
            ClassName listName = ClassName.get(ArrayList.class);
            ClassName stringName = ClassName.get(String.class);
            ParameterizedTypeName paramList = ParameterizedTypeName.get(ArrayList.class, Object.class);
            ParameterizedTypeName invocations = ParameterizedTypeName.get(listName, paramList);
            ParameterizedTypeName paramListType = ParameterizedTypeName.get(hashmapName,
                    stringName,
                    invocations);
            FieldSpec paramsListSpec = FieldSpec.builder(paramListType, "invocations")
                    .addModifiers(Modifier.PRIVATE)
                    .build();
            builder.addField(paramsListSpec);

            FieldSpec verificationFlag = FieldSpec.builder(TypeName.BOOLEAN, "verificationMode")
                    .addModifiers(Modifier.PRIVATE)
                    .build();

            builder.addField(verificationFlag);

            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
                    .addCode("invocations = new HashMap<>();\n");

            MethodSpec returnInvocationsMethod = MethodSpec.methodBuilder("getInvocations")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(paramListType)
                    .addCode("return invocations;")
                    .build();

            MethodSpec enableVerificationModeMethod = MethodSpec.methodBuilder("enableVerificationMode")
                    .addAnnotation(Override.class)
                    .addParameter(TypeName.BOOLEAN,"mode")
                    .addModifiers(Modifier.PUBLIC)
                    .addCode("verificationMode = mode;")
                    .build();

            MethodSpec clearInvocationsMethod = MethodSpec.methodBuilder("clearInvocations")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addCode(CodeBlock.builder().beginControlFlow("for(String uuid: invocations.keySet())")
                            .addStatement("invocations.get(uuid).clear();")
                            .endControlFlow().build())
                    .build();

            builder.addMethod(clearInvocationsMethod);
            builder.addMethod(returnInvocationsMethod);
            builder.addMethod(enableVerificationModeMethod);

            fieldToConstructorMap.put(element, fieldTypeElement.getSimpleName()+"Mock");

            for(String methodUUID : methodUUIDs) {
                constructorBuilder.addCode("invocations.put(\""+methodUUID+"\", new ArrayList<>());\n");
            }

            builder.addMethod(constructorBuilder.build());

            JavaFile javaFile = JavaFile.builder("org.example",
                    builder.build()).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        TypeSpec.Builder mockitoAnnotationsBuilder = TypeSpec.classBuilder("OurMockitoAnnotations")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC);
        ParameterizedTypeName initializerMapType
                = ParameterizedTypeName.get(HashMap.class, String.class, TestInitializer.class);
        FieldSpec initializerMapField = FieldSpec.builder(initializerMapType,"sInitMap", Modifier.STATIC, Modifier.PRIVATE)
                .build();
        mockitoAnnotationsBuilder.addField(initializerMapField);
        CodeBlock.Builder staticBlockBuilder = CodeBlock.builder()
                .add("sInitMap = new HashMap<>();\n");

        for (Element testElement: testClasses) {
            TypeName name = TypeName.get(testElement.asType());
            String initializerName = testElement.getSimpleName()+"Initializer";
            TypeSpec.Builder testInitializer = TypeSpec.classBuilder(initializerName)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addSuperinterface(TestInitializer.class);
            MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder("initialize")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.OBJECT, "testClass")
                    .addCode(testElement.getSimpleName()+ " testObj = ("+
                            testElement.getSimpleName()+")testClass;");
            for (Element fieldToInit: testClassToFieldsMap.get(testElement)) {
                initMethodBuilder.addCode("\n"+"testObj."+fieldToInit.getSimpleName()+" = new "
                + fieldToConstructorMap.get(fieldToInit)+ "();");
            }
            testInitializer.addMethod(initMethodBuilder.build());

            JavaFile initializerFile = JavaFile.builder("org.example",
                    testInitializer.build()).build();
            try {
                initializerFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            staticBlockBuilder.add("sInitMap.put(\"" + name.toString()+"\", new "+initializerName+"());\n");
        }
        mockitoAnnotationsBuilder.addStaticBlock(staticBlockBuilder.build());
        MethodSpec initMocksMethod = MethodSpec.methodBuilder("initMocks")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.OBJECT, "testClass")
                .addCode("sInitMap.get(testClass.getClass().getCanonicalName()).initialize(testClass);")
                .build();
        mockitoAnnotationsBuilder.addMethod(initMocksMethod);
        JavaFile myMockitoFile = JavaFile.builder("org.example",
                mockitoAnnotationsBuilder.build()).build();
        try {
            myMockitoFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        return true;
    }



    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(OurMock.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return super.getSupportedSourceVersion();
    }
}
