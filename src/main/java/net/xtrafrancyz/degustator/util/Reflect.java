package net.xtrafrancyz.degustator.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс предоставляет API для Java Reflection с кеширующими возможностями
 *
 * @author xtrafrancyz
 */
@SuppressWarnings({"unchecked", "unused"})
public class Reflect {
    private static final Map<String, ClassData> cache = new HashMap<>();
    
    private Reflect() {}
    
    /**
     * Создаёт объект класса clazz, при этом вызывая конструктор с аргументами args.
     *
     * @param clazz класс объекта
     * @param args  аргументы конструктора
     * @return новый объект
     */
    public static <E> E construct(Class<E> clazz, Object... args) {
        try {
            return getClass(clazz).construct(args);
        } catch (Exception e) {
            error(e, "Constructor error");
            return null;
        }
    }
    
    /**
     * Возвращает значение статического поля класса
     *
     * @param clazz класс
     * @param field имя поля
     * @return значение
     */
    public static <E> E get(Class clazz, String field) {
        try {
            return (E) getClass(clazz).get(null, field);
        } catch (Exception e) {
            error(e, "Get static field error");
            return null;
        }
    }
    
    /**
     * Возвращает значение поля объекта
     *
     * @param instance объект
     * @param field    имя поля
     * @return значение
     */
    public static <R> R get(Object instance, String field) {
        try {
            return (R) getClass(instance.getClass()).get(instance, field);
        } catch (Exception e) {
            error(e, "Get field error");
            return null;
        }
    }
    
    /**
     * Возвращает значение поля объекта
     *
     * @param clazz    класс, в котором есть это поле
     * @param instance объект
     * @param field    имя поля
     * @return значение
     */
    public static <T, E> E get(Class<T> clazz, T instance, String field) {
        try {
            return (E) getClass(clazz).get(instance, field);
        } catch (Exception e) {
            error(e, "Get field error");
            return null;
        }
    }
    
    /**
     * Устанавливает новое значение статическому полю класса
     *
     * @param clazz класс
     * @param field имя поля
     * @param value новое значение
     */
    public static void set(Class clazz, String field, Object value) {
        try {
            getClass(clazz).set(null, field, value);
        } catch (Exception e) {
            error(e, "Set static field error");
        }
    }
    
    /**
     * Устанавливает новое значение полю объекта
     *
     * @param instance объект
     * @param field    имя поля
     * @param value    новое значение
     */
    public static void set(Object instance, String field, Object value) {
        try {
            getClass(instance.getClass()).set(instance, field, value);
        } catch (Exception e) {
            error(e, "Set field error");
        }
    }
    
    /**
     * Устанавливает новое значение полю объекта
     *
     * @param clazz    класс, в котором есть это поле
     * @param instance объект
     * @param field    имя поля
     * @param value    новое значение
     */
    public static <T> void set(Class<T> clazz, T instance, String field, Object value) {
        try {
            getClass(clazz).set(instance, field, value);
        } catch (Exception e) {
            error(e, "Set field error");
        }
    }
    
    /**
     * Устанавливает новое значение статическому финальному полю класса
     *
     * @param clazz класс
     * @param field имя поля
     * @param value новое значение
     */
    public static void setFinal(Class clazz, String field, Object value) {
        try {
            getClass(clazz).setFinal(null, field, value);
        } catch (Exception e) {
            error(e, "Set static final field error");
        }
    }
    
    /**
     * Устанавливает новое значение финальному полю объекта
     *
     * @param instance объект
     * @param field    имя поля
     * @param value    новое значение
     */
    public static void setFinal(Object instance, String field, Object value) {
        try {
            getClass(instance.getClass()).setFinal(instance, field, value);
        } catch (Exception e) {
            error(e, "Set final field error");
        }
    }
    
    /**
     * Устанавливает новое значение финальному полю объекта
     *
     * @param clazz    класс, в котором есть это поле
     * @param instance объект
     * @param field    имя поля
     * @param value    новое значение
     */
    public static <T> void setFinal(Class<T> clazz, T instance, String field, Object value) {
        try {
            getClass(clazz).setFinal(instance, field, value);
        } catch (Exception e) {
            error(e, "Set final field error");
        }
    }
    
    /**
     * Вызывает статический метод у класса
     *
     * @param clazz  класс
     * @param method имя метода
     * @param args   аргументы метода
     * @return результат выполнения
     */
    public static <E> E invoke(Class clazz, String method, Object... args) {
        try {
            return (E) getClass(clazz).invoke(null, method, args);
        } catch (Exception e) {
            error(e, "Invoke static error");
            return null;
        }
    }
    
    /**
     * Вызывает метод у объекта
     *
     * @param instance объект класса
     * @param method   имя метода
     * @param args     аргументы метода
     * @return результат выполнения
     */
    public static <E> E invoke(Object instance, String method, Object... args) {
        try {
            return (E) getClass(instance.getClass()).invoke(instance, method, args);
        } catch (Exception e) {
            error(e, "Invoke error");
            return null;
        }
    }
    
    /**
     * Вызывает метод у объекта
     *
     * @param clazz    класс, к которому принадлежит метод
     * @param instance объект класса
     * @param method   имя метода
     * @param args     аргументы метода
     * @return результат выполнения
     */
    public static <T, E> E invoke(Class<T> clazz, T instance, String method, Object... args) {
        try {
            return (E) getClass(clazz).invoke(instance, method, args);
        } catch (Exception e) {
            error(e, "Invoke error");
            return null;
        }
    }
    
    /**
     * Проверяет, существует ли конструктор в классе
     *
     * @param clazz класс, в котором нужно искать конструктор
     * @param args  аргументы
     * @return true - если конструктор существует
     */
    public static <T> boolean isConstructorExist(Class<T> clazz, Class... args) {
        return findConstructor(clazz, args) != null;
    }
    
    /**
     * Проверяет, существует ли метод в классе
     *
     * @param clazz  класс, в котором нужно искать метод
     * @param method имя метода
     * @param args   аргументы
     * @return true - если метод существует
     */
    public static <T> boolean isMethodExist(Class<T> clazz, String method, Class... args) {
        return findMethod(clazz, method, args) != null;
    }
    
    /**
     * Проверяет, существует ли поле в классе
     *
     * @param clazz класс, в котором нужно искать метод
     * @param field имя поля
     * @return true - если поле существует
     */
    public static <T> boolean isFieldExist(Class<T> clazz, String field) {
        return findField(clazz, field) != null;
    }
    
    /**
     * Ищет конструктор в классе
     *
     * @param clazz класс, содержащий метод
     * @param args  типы аргументов конструктора
     * @return метод, который сразу можно вызывать
     */
    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class... args) {
        try {
            return getClass(clazz).findConstructor0(args);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * Ищет метод в классе
     *
     * @param clazz  класс, содержащий метод
     * @param method имя метода
     * @param args   типы аргументов метода
     * @return метод, который сразу можно вызывать
     */
    public static <T> Method findMethod(Class<T> clazz, String method, Class... args) {
        try {
            return getClass(clazz).findMethod0(method, args);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * Ищет поле в классе
     *
     * @param clazz класс, содержащий поле
     * @param field имя поля
     * @return поле, из которого сразу же можно получить данные
     */
    public static <T> Field findField(Class<T> clazz, String field) {
        try {
            return getClass(clazz).findField(field);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * Ищет поле в классе. Сразу убирает с него флаг final.
     *
     * @param clazz класс, содержащий поле
     * @param field имя поля
     * @return поле с правами доступа и без final
     */
    public static <T> Field findFinalField(Class<T> clazz, String field) {
        try {
            return getClass(clazz).findFinalField(field);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * Ищет класс по указанному полному названию не бросая исключения.
     *
     * @param name имя искомого класса
     * @return класс, если найден, иначе - null
     */
    public static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
    
    /**
     * Если будут использоваться перегруженные методы с одинаковым именем и одинаковым количеством аргументов, то
     * необходимо установить значение true, иначе - false.<br/>
     * При установленном значении true кэшироваться будут не только количество аргументов, а еще и их типы<br/>
     * Замедляет вызов метода (0.002мс на каждый аргумент)
     *
     * @param clazz класс, в котором присутствуют перегруженные методы
     * @param flag  true - включить агрессивную перегрузку, false - выключить
     */
    public static void setAggressiveMethodsOverloading(Class clazz, boolean flag) {
        ClassData data = getClass(clazz);
        if (data.aggressiveOverloading != flag) {
            data.aggressiveOverloading = flag;
            data.methods.clear();
        }
    }
    
    private static <T> ClassData<T> getClass(Class<T> clazz) {
        ClassData<T> data = cache.get(clazz.getName());
        if (data == null) {
            data = new ClassData<>(clazz);
            cache.put(clazz.getName(), data);
        }
        return data;
    }
    
    //Для лёгкости переноса этого класса
    private static void error(Exception e, String message) {
        Logger.getLogger("Reflect").log(Level.SEVERE, message, e);
    }
    
    static class ClassData<K> {
        private static Field FIELD_MODIFIERS = null;
        
        static {
            try {
                FIELD_MODIFIERS = Field.class.getDeclaredField("modifiers");
                FIELD_MODIFIERS.setAccessible(true);
            } catch (Exception ex) {
                error(ex, "Field modifiers field not found");
            }
        }
        
        private final Class<K> clazz;
        private final Map<String, Field> fields = new HashMap<>();
        private final Map<String, Method> methods = new HashMap<>();
        private final Map<String, Constructor<K>> constructors = new HashMap<>();
        
        boolean aggressiveOverloading = false;
        
        public ClassData(Class<K> clazz) {
            this.clazz = clazz;
        }
        
        void set(Object instance, String field, Object value) throws Exception {
            this.findField(field).set(instance, value);
        }
        
        void setFinal(Object instance, String field, Object value) throws Exception {
            findFinalField(field).set(instance, value);
        }
        
        Object get(Object instance, String field) throws Exception {
            return this.findField(field).get(instance);
        }
        
        Object invoke(Object instance, String method, Object... args) throws Exception {
            return this.findMethod(method, args).invoke(instance, args);
        }
        
        K construct(Object... args) throws Exception {
            return this.findConstructor(args).newInstance(args);
        }
        
        /**
         * Поиск конструктора с заданными аргументами
         *
         * @param args аргументы
         * @return конструктор или null, если таковой не найден
         */
        Constructor<K> findConstructor(Object... args) {
            return findConstructor0(toTypes(args));
        }
        
        /**
         * Поиск конструктора с заданными типами аргументов
         *
         * @param types типы аргументов
         * @return конструктор или null, если таковой не найден
         */
        Constructor<K> findConstructor0(Class... types) {
            String mapped = classesToString(types);
            Constructor<K> con = constructors.get(mapped);
            if (con == null) {
                constructorsLoop:
                for (Constructor c : clazz.getDeclaredConstructors()) {
                    Class<?>[] ptypes = c.getParameterTypes();
                    if (ptypes.length != types.length)
                        continue;
                    
                    for (int i = 0; i < ptypes.length; i++)
                        if (ptypes[i] != types[i] && !ptypes[i].isAssignableFrom(types[i]))
                            continue constructorsLoop;
                    
                    con = c;
                    con.setAccessible(true);
                    constructors.put(mapped, con);
                    break;
                }
                
                if (con == null)
                    throw new UnableToFindConstructorException(clazz, types);
            }
            return con;
        }
        
        /**
         * Рекурсивный поиск метода в классе и всех суперклассам данного класса
         *
         * @param name имя метода
         * @param args аргументы метода
         * @return метод или null, если он не найден
         */
        Method findMethod(String name, Object... args) {
            Class[] types = null;
            String mapped;
            if (aggressiveOverloading) {
                types = toTypes(args);
                mapped = name + classesToString(types);
            } else {
                mapped = args.length + name;
            }
            
            Method method = methods.get(mapped);
            if (method == null) {
                if (types == null)
                    types = toTypes(args);
                
                method = fastFindMethod(name, types);
                
                if (method == null)
                    throw new UnableToFindMethodException(clazz, name, types);
                else
                    methods.put(mapped, method);
            }
            return method;
        }
        
        /**
         * Рекурсивный поиск метода в классе и всех суперклассам данного класса
         *
         * @param name  имя метода
         * @param types типы аргументов метода
         * @return метод или null, если он не найден
         */
        Method findMethod0(String name, Class... types) {
            String mapped;
            if (aggressiveOverloading) {
                mapped = name + classesToString(types);
            } else {
                mapped = types.length + name;
            }
            
            Method method = methods.get(mapped);
            if (method == null) {
                method = fastFindMethod(name, types);
                if (method == null)
                    throw new UnableToFindMethodException(clazz, name, types);
                else
                    methods.put(mapped, method);
            }
            return method;
        }
        
        /**
         * Выполняет рекурсивный поиск метода в классе и всех суперклассам данного класса.
         * Увеличение производительности обеспечивается за счет упрощенного сравнения аргументов
         * метода.
         *
         * @param name  имя метода
         * @param types типы аргументов, которые принимает данный метод
         * @return метод или null, если он не найден
         */
        @SuppressWarnings("StringEquality")
        private Method fastFindMethod(String name, Class... types) {
            Method method = null;
            name = name.intern();
            Class clazz0 = clazz;
            do {
                methodsLoop:
                for (Method m : clazz0.getDeclaredMethods()) {
                    if (name != m.getName())
                        continue;
                    
                    Class<?>[] ptypes = m.getParameterTypes();
                    if (ptypes.length != types.length)
                        continue;
                    
                    for (int i = 0; i < ptypes.length; i++)
                        if (ptypes[i] != types[i] && !ptypes[i].isAssignableFrom(types[i]))
                            continue methodsLoop;
                    
                    method = m;
                    break;
                }
                if (method != null) {
                    method.setAccessible(true);
                    break;
                }
                clazz0 = clazz0.getSuperclass();
            } while (clazz0 != null);
            return method;
        }
        
        /**
         * Рекурсивный поиск поля по классу и всем суперклассам данного класса<br/>
         * Если поле находится, то у него убирается флаг final
         *
         * @param name имя поля
         * @return поле или null, если оно не найдено
         */
        Field findFinalField(String name) throws Exception {
            Field field = findField(name);
            FIELD_MODIFIERS.set(field, field.getModifiers() & ~Modifier.FINAL);
            return field;
        }
        
        /**
         * Рекурсивный поиск поля по классу и всем суперклассам данного класса
         *
         * @param name имя поля
         * @return поле или null, если оно не найдено
         */
        Field findField(String name) {
            Field field = fields.get(name);
            if (field == null) {
                Class clazz0 = clazz;
                while (clazz0 != null) {
                    try {
                        field = clazz0.getDeclaredField(name);
                        field.setAccessible(true);
                        fields.put(name, field);
                        break;
                    } catch (Exception e) {
                        clazz0 = clazz0.getSuperclass();
                    }
                }
                if (field == null)
                    throw new UnableToFindFieldException(clazz, name);
            }
            return field;
        }
        
        /**
         * Преобразует объекты в их классы
         *
         * @param objects объекты
         * @return классы объектов
         */
        private Class[] toTypes(Object[] objects) {
            if (objects.length == 0)
                return new Class[0];
            
            Class[] types = new Class[objects.length];
            for (int i = 0; i < objects.length; i++) {
                Class type = objects[i].getClass();
                if (type == Integer.class)
                    type = Integer.TYPE;
                else if (type == Double.class)
                    type = Double.TYPE;
                else if (type == Boolean.class)
                    type = Boolean.TYPE;
                else if (type == Float.class)
                    type = Float.TYPE;
                else if (type == Long.class)
                    type = Long.TYPE;
                else if (type == Character.class)
                    type = Character.TYPE;
                else if (type == Byte.class)
                    type = Byte.TYPE;
                else if (type == Short.class)
                    type = Short.TYPE;
                types[i] = type;
            }
            return types;
        }
        
        /**
         * Преобразует массив классов в строку, почти как дескриптор в байткоде
         */
        public static String classesToString(Class[] classes) {
            int iMax = classes.length - 1;
            if (iMax == -1)
                return "()";
            
            StringBuilder b = new StringBuilder();
            b.append('(');
            for (int i = 0; ; i++) {
                b.append(classes[i].getName());
                if (i == iMax)
                    return b.append(')').toString();
                b.append(",");
            }
        }
    }
    
    static class UnableToFindFieldException extends RuntimeException {
        private String fieldName;
        private String className;
        
        public UnableToFindFieldException(Class clazz, String fieldName) {
            super();
            this.fieldName = fieldName;
            this.className = clazz.getName();
        }
        
        @Override
        public String getMessage() {
            return toString();
        }
        
        @Override
        public String toString() {
            return "Unable to find field '" + fieldName + "' in class '" + className + "'";
        }
    }
    
    static class UnableToFindMethodException extends RuntimeException {
        protected String methodName;
        protected String className;
        protected Class[] types;
        
        public UnableToFindMethodException(Class clazz, String methodName, Class[] types) {
            super();
            this.methodName = methodName;
            this.className = clazz.getName();
            this.types = types;
        }
        
        @Override
        public String getMessage() {
            return toString();
        }
        
        @Override
        public String toString() {
            return "Unable to find method '" + className + "." + methodName + ClassData.classesToString(types) + "'";
        }
    }
    
    static class UnableToFindConstructorException extends UnableToFindMethodException {
        public UnableToFindConstructorException(Class clazz, Class[] types) {
            super(clazz, null, types);
        }
        
        @Override
        public String toString() {
            return "Unable to find constructor '" + className + ".<init>" + ClassData.classesToString(types) + "'";
        }
    }
}
