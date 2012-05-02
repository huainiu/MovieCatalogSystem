package net.milanaleksic.mcs.infrastructure.gui.transformer;

import org.codehaus.jackson.JsonNode;

import java.lang.reflect.*;

import java.util.Map;

/**
 * User: Milan Aleksic
 * Date: 4/19/12
 * Time: 2:10 PM
 */
public interface Converter<T> {

    void invoke(Method method, Object targetObject, JsonNode value, Map<String, Object> mappedObjects, Class<T> argType) throws TransformerException;

    void setField(Field field, Object targetObject, JsonNode value, Map<String, Object> mappedObjects, Class<T> argType) throws TransformerException;

    void cleanUp() ;

}
