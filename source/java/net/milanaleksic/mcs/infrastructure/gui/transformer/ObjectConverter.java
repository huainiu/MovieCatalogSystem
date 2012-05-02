package net.milanaleksic.mcs.infrastructure.gui.transformer;

import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.*;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.Map;
import java.util.regex.*;

/**
 * User: Milan Aleksic
 * Date: 4/19/12
 * Time: 3:03 PM
 */
public class ObjectConverter implements Converter<Object>, ApplicationContextAware {

    private static final Pattern magicConstantsValue = Pattern.compile("\\((.*)\\)");

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static Map<String, Class<?>> knownClasses = ImmutableMap
            .<String, Class<?>>builder()
            .put("showImageComposite", net.milanaleksic.mcs.application.gui.helper.ShowImageComposite.class)
            .put("dynamicSelectorText", net.milanaleksic.mcs.application.gui.helper.DynamicSelectorText.class)

            .put("gridData", org.eclipse.swt.layout.GridData.class)
            .put("gridLayout", org.eclipse.swt.layout.GridLayout.class)

            .put("shell", org.eclipse.swt.widgets.Shell.class)
            .put("button", org.eclipse.swt.widgets.Button.class)
            .put("canvas", org.eclipse.swt.widgets.Canvas.class)
            .put("composite", org.eclipse.swt.widgets.Composite.class)
            .put("group", org.eclipse.swt.widgets.Group.class)
            .put("label", org.eclipse.swt.widgets.Label.class)
            .put("tabFolder", org.eclipse.swt.widgets.TabFolder.class)
            .put("tabItem", org.eclipse.swt.widgets.TabItem.class)
            .put("table", org.eclipse.swt.widgets.Table.class)
            .put("tableColumn", org.eclipse.swt.widgets.TableColumn.class)
            .put("link", org.eclipse.swt.widgets.Link.class)
            .put("list", org.eclipse.swt.widgets.List.class)
            .put("text", org.eclipse.swt.widgets.Text.class)
            .put("combo", org.eclipse.swt.widgets.Combo.class)
            .put("toolBar", org.eclipse.swt.widgets.ToolBar.class)
            .put("toolItem", org.eclipse.swt.widgets.ToolItem.class)
            .put("menu", org.eclipse.swt.widgets.Menu.class)
            .put("menuItem", org.eclipse.swt.widgets.MenuItem.class)

            .put("scrolledComposite", org.eclipse.swt.custom.ScrolledComposite.class)
            .build();


    @Inject
    private Transformer transformer;

    private ApplicationContext applicationContext;

    @Override
    public final void invoke(Method method, Object targetObject, JsonNode value, Map<String, Object> mappedObjects, Class<Object> argType) throws TransformerException {
        try {
            method.invoke(targetObject, getValueFromJson(value, mappedObjects, argType));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new TransformerException("Wrapped invoke failed: ", e);
        }
    }

    @Override
    public final void setField(Field field, Object targetObject, JsonNode value, Map<String, Object> mappedObjects, Class<Object> argType) throws TransformerException {
        try {
            field.set(targetObject, getValueFromJson(value, mappedObjects, argType));
        } catch (IllegalAccessException e) {
            throw new TransformerException("Wrapped setField failed: ", e);
        }
    }

    @Override
    public void cleanUp() {}

    protected Object getValueFromJson(JsonNode node, Map<String, Object> mappedObjects, Class<?> argType) throws TransformerException {
        if (node.isTextual()) {
            String originalValue = node.asText();
            Matcher matcher = magicConstantsValue.matcher(originalValue);
            if (matcher.matches()) {
                Object mappedObject = mappedObjects.get(matcher.group(1));
                if (mappedObject == null) {
                    try {
                        mappedObject = applicationContext.getBean(matcher.group(1));
                    } catch (NoSuchBeanDefinitionException e) {
                        throw new TransformerException("Object does not exist - " + node.asText());
                    }
                }
                return mappedObject;
            } else
                throw new TransformerException("Invalid syntax for magical value - " + originalValue);
        }
        return createWidgetFromResource(argType, node, mappedObjects);
    }

    public Object createWidgetFromResource(Class<?> widgetClass, JsonNode value, Map<String, Object> mappedObjects) throws TransformerException {
        try {
            Object widget;
            Class<?> deducedClass = deduceClassFromNode(value);
            if (deducedClass != null)
                widgetClass = deducedClass;
            widget = widgetClass.newInstance();
            transformer.transformNodeToProperties(value, widget, mappedObjects);
            return widget;
        } catch (InstantiationException | TransformerException | IllegalAccessException e) {
            throw new TransformerException("Widget creation of class " + widgetClass.getName() + " failed", e);
        }
    }

    public static Class<?> deduceClassFromNode(JsonNode value) throws TransformerException {
        if (value.has(Transformer.KEY_SPECIAL_TYPE)) {
            String classIdentifier = value.get(Transformer.KEY_SPECIAL_TYPE).asText();
            Class<?> aClass = knownClasses.get(classIdentifier);
            if (aClass != null)
                return aClass;
            else
                try {
                    return Class.forName(classIdentifier);
                } catch (ClassNotFoundException e) {
                    throw new TransformerException("Class was not found: " + classIdentifier, e);
                }
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
