package com.codingchili.zapperflyasm;

import com.codingchili.core.context.StorageContext;
import com.codingchili.core.storage.JsonMap;

/**
 * @author Robin Duda
 *
 * Simple mock for the storage context.
 */
public class StorageContextMock<T> extends StorageContext<T> {

    /**
     * @param aClass the class the mocked context applies to.
     */
    public StorageContextMock(Class<T> aClass) {
        setCollection("test");
        setClass(aClass);
        setDatabase(aClass.getSimpleName());
        setPlugin(JsonMap.class);
    }

}
