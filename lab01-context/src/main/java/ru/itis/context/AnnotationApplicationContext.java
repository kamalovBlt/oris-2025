package ru.itis.context;

import ru.itis.context.exception.NoFoundPublicConstructorException;
import ru.itis.context.exception.NoSuchBeanException;
import ru.itis.context.exception.PackageNotFoundException;

import java.io.File;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public class AnnotationApplicationContext implements ApplicationContext {

    private final Map<String, Object> beans = new HashMap<>(30);
    private final String packageName;

    public AnnotationApplicationContext(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public Object getBean(String name) {
        Object object = this.beans.get(name);
        if (object == null) {
            throw new NoSuchBeanException(name);
        }
        return object;
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        Object object = this.beans.get(name);
        if (object == null || !requiredType.isAssignableFrom(object.getClass())) {
            throw new NoSuchBeanException(name);
        }
        return requiredType.cast(object);
    }

    @Override
    public void run() {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        String path = this.packageName.replace('.', '/');
        URL resource = contextClassLoader.getResource(path);

        if (resource == null) {
            throw new PackageNotFoundException("Can't find resource " + path);
        }

        File directory = new File(resource.getFile());

        List<Class<?>> foundedClasses = new ArrayList<>(30);

        try {
            scanDirectory(this.packageName, directory, foundedClasses);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<Class<?>> components = checkFoundedClassesAndRemainOnlyComponentClasses(foundedClasses);

        for (Class<?> componentClass : components) {
            createBean(componentClass);
        }

    }

    @Override
    public void close() {
        this.beans.clear();
    }

    private List<Class<?>> checkFoundedClassesAndRemainOnlyComponentClasses(List<Class<?>> foundedClasses) {
        foundedClasses.removeIf(actualClass -> !actualClass.isAnnotationPresent(Component.class));
        return foundedClasses;
    }

    private Object createBean(Class<?> actualClass) {

        if (this.beans.containsKey(generateBeanName(actualClass))) {
            return this.beans.get(generateBeanName(actualClass));
        }

        try {

            Constructor<?>[] constructors = actualClass.getConstructors();
            Optional<Constructor<?>> constructorWithMaxParametersCount = Arrays.stream(constructors).max(Comparator.comparingInt(Constructor::getParameterCount));
            Constructor<?> constructor = constructorWithMaxParametersCount
                    .orElseThrow(() ->
                            new NoFoundPublicConstructorException("No found public constructor for " + actualClass.getName()
                            ));

            Parameter[] parameters = constructor.getParameters();
            List<Object> dependencies = new ArrayList<>(parameters.length);
            for (Parameter parameter : parameters) {
                String parameterName = parameter.getName();
                if (this.beans.containsKey(parameterName)) {
                    dependencies.add(this.beans.get(parameterName));
                } else {
                    Object dependency = createBean(parameter.getType());
                    dependencies.add(dependency);
                }
            }

            Object createdBean;
            if (!dependencies.isEmpty()) {
                createdBean = constructor.newInstance(dependencies.toArray());
            }
            else {
                createdBean = constructor.newInstance();
            }
            injectDependencies(createdBean);
            this.beans.put(generateBeanName(createdBean.getClass()), createdBean);
            return createdBean;

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    private String generateBeanName(Class<?> beanClass) {
        String simpleName = beanClass.getSimpleName();
        return simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
    }

    private void scanDirectory(String packageName, File directory, List<Class<?>> foundClasses) throws ClassNotFoundException {
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file, foundClasses);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                Class<?> actualClass = Class.forName(className);
                foundClasses.add(actualClass);
            }
        }
    }

    private void injectDependencies(Object bean) throws InvocationTargetException, IllegalAccessException {

        Class<?> actualClass = bean.getClass();
        Field[] fields = actualClass.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                Object foundedBean = this.beans.get(generateBeanName(field.getType()));
                if (foundedBean == null) {
                    Object createdBean = createBean(field.getType());
                    this.beans.put(generateBeanName(createdBean.getClass()), createdBean);
                }
            }
        }

        Method[] methods = actualClass.getMethods();

        for (Method method : methods) {
            if (method.getName().startsWith("set")) {
                String name = method.getName();
                String dependencyName = name.substring(3, 4).toLowerCase() + name.substring(4);
                method.invoke(bean, this.beans.get(dependencyName));
            }
        }

    }

}
