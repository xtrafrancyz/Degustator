package net.xtrafrancyz.degustator.storage;

/**
 * @author xtrafrancyz
 */
public interface IStorage {
    /**
     * Загрузка инфы
     */
    void load();
    
    /**
     * Сохранение инфы
     */
    void save();
    
    /**
     * Устанавливает новое значение по ключу
     *
     * @param key   ключ
     * @param value новое значение
     * @return старое значение или null
     */
    String set(String key, String value);
    
    /**
     * Возвращает значение по ключу
     *
     * @param key ключ
     * @return текущее значение или null
     */
    String get(String key);
    
    /**
     * Удаляет значение по ключу
     *
     * @param key ключ
     * @return старое значение или null
     */
    String remove(String key);
}
