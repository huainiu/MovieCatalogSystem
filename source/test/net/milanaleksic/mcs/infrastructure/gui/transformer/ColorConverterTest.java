package net.milanaleksic.mcs.infrastructure.gui.transformer;

import com.google.common.collect.Maps;
import org.codehaus.jackson.node.TextNode;
import org.eclipse.swt.graphics.Color;
import org.junit.*;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

/**
 * User: Milan Aleksic
 * Date: 4/20/12
 * Time: 10:04 AM
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class ColorConverterTest {

    private HashMap<String, Object> mappedObjects;

    @Before
    public void create_dependencies() {
        this.mappedObjects = Maps.newHashMap();
    }

    @Test
    public void convert_simple_value() {
        ColorConverter colorConverter = new ColorConverter();
        try {
            Color color = colorConverter.getValueFromJson(new TextNode("#ff1001"), mappedObjects);
            assertThat(color.getRed(), equalTo(255));
            assertThat(color.getGreen(), equalTo(16));
            assertThat(color.getBlue(), equalTo(1));
            Color color2 = colorConverter.getValueFromJson(new TextNode("#00AA66"), mappedObjects);
            assertThat(color2.getRed(), equalTo(0));
            assertThat(color2.getGreen(), equalTo(170));
            assertThat(color2.getBlue(), equalTo(102));
        } catch (TransformerException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
