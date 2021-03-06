package net.milanaleksic.mcs.application.gui.helper;

import com.google.common.collect.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.util.List;

/**
 * User: Milan Aleksic
 * Date: 4/24/12
 * Time: 2:26 PM
 */
public class DynamicSelectorText extends Composite implements PaintListener {

    private static final int PADDING_IN_ITEM = 3;
    private static final int PADDING_IN_ITEM_LEFT = 3;
    private static final int CLOSER_DIMENSION = 6;
    private static final int PADDING_BETWEEN_ITEMS = 5;
    private static final int BORDER_SUM_BETWEEN_ITEMS = 2;
    private static final int SCROLL_BAR_MULTIPLIER = 10;

    private SortedSet<String> selectedItems = Sets.newTreeSet();

    private Map<String, Object> dataItems = Maps.newHashMap();

    private ControlEditor editor;

    private List<Rectangle> closingButtons;

    private Color selectedItemBackgroundColor;

    private Color selectedItemForegroundColor;

    private Color closerColor;

    private ResourceBundle bundle;

    public DynamicSelectorText(Composite parent, int style) {
        super(parent, style | SWT.NO_BACKGROUND);
        prepareComponent();
        addListeners();
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    private void prepareComponent() {
        if (isModifiable())
            prepareComboEditor();
        final ScrollBar verticalScrollBar = getVerticalBar();
        if (verticalScrollBar != null)
            verticalScrollBar.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (verticalScrollBar.getSelection() != lastPosition) {
                        lastPosition = verticalScrollBar.getSelection();
                        redraw();
                    }
                }
            });
        closingButtons = new LinkedList<>();
        selectedItemBackgroundColor = getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        selectedItemForegroundColor = getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        closerColor = getDisplay().getSystemColor(SWT.COLOR_GRAY);
    }

    private void prepareComboEditor() {
        editor = new ControlEditor(this);
        final Combo chooser = new Combo(this, SWT.DROP_DOWN);
        chooser.setBackground(getBackground());
        editor.minimumHeight = 20;
        editor.minimumWidth = 100;
        editor.setEditor(chooser);
        chooser.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                switch (e.detail) {
                    case SWT.TRAVERSE_RETURN:
                        final String itemText = chooser.getText();
                        if (Arrays.asList(chooser.getItems()).contains(itemText))
                            safeAddSelectItem(itemText);
                        else
                            chooser.select(0);
                        break;
                    case SWT.TRAVERSE_ESCAPE:
                        chooser.select(0);
                        break;
                }
            }
        });
        chooser.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                final int selectionIndex = chooser.getSelectionIndex();
                if (selectionIndex <= 0 || selectionIndex > chooser.getItemCount())
                    return;
                final String item = chooser.getItem(selectionIndex);
                safeAddSelectItem(item);
            }
        });
    }

    private void safeAddSelectItem(String item) {
        if (selectedItems.contains(item))
            return;
        selectedItems.add(item);
        getComboEditor().select(0);
        redraw();
    }

    private void addListeners() {
        if (isModifiable()) {
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    Iterator<String> iterator = selectedItems.iterator();
                    if (!iterator.hasNext())
                        return;
                    for (Rectangle closingButton : closingButtons) {
                        iterator.next();
                        if (e.x < closingButton.x || e.x > closingButton.x + closingButton.width)
                            continue;
                        if (e.y < closingButton.y || e.y > closingButton.y + closingButton.height)
                            continue;
                        iterator.remove();
                        redraw();
                        return;
                    }
                }
            });
        }
        this.addPaintListener(this);
    }

    @Override
    public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        gc.setAdvanced(true);
        gc.setTextAntialias(SWT.ON);
        closingButtons.clear();
        gc.fillRectangle(e.x, e.y, e.width, e.height);

        int verticalBuffer = getVerticalBar() != null ? getVerticalBar().getSelection() * SCROLL_BAR_MULTIPLIER : 0;
        int verticalBarWidth = getVerticalBar() != null ? getVerticalBar().getSize().y : 0;

        int xIter = PADDING_IN_ITEM, yIter = PADDING_IN_ITEM - verticalBuffer;
        Point textExtentGlobal = gc.textExtent("Wq");
        for (String itemToPaint : selectedItems) {
            final Point textExtent = gc.textExtent(itemToPaint);

            if (getVerticalBar() == null
                    && !isModifiable()
                    && xIter + verticalBarWidth + getItemWidthBeforeCloser(textExtent)
                    + getCloserWidth() + getItemWidthAfterCloser()
                    + PADDING_IN_ITEM + gc.textExtent("...").x > getBounds().width) {
                gc.drawText("...", xIter + PADDING_IN_ITEM, yIter + PADDING_IN_ITEM);
                break;
            }

            drawItemRectangle(gc, xIter, yIter, itemToPaint, textExtent);
            xIter += getItemWidthBeforeCloser(textExtent);

            if (isModifiable())
                drawItemCloser(gc, xIter, yIter, textExtent);

            xIter += getCloserWidth() + getItemWidthAfterCloser();

            if ((isModifiable() && xIter + editor.minimumWidth + verticalBarWidth > getBounds().width)
                    || (!isModifiable() && xIter + verticalBarWidth > getBounds().width)) {
                xIter = PADDING_IN_ITEM;
                yIter += textExtent.y + 2 * PADDING_IN_ITEM + PADDING_BETWEEN_ITEMS;
            }
        }
        if (isModifiable()) {
            Point location = editor.getEditor().getLocation();
            // if check to avoid redraw recursion
            if (location.x != xIter || location.y != yIter)
                editor.getEditor().setLocation(xIter, yIter);
        }
        if (getVerticalBar() != null) {
            int max = (yIter + textExtentGlobal.y + 2 * PADDING_IN_ITEM + verticalBuffer) / SCROLL_BAR_MULTIPLIER;
            getVerticalBar().setValues(lastPosition, 0, max, 2, 1, 2);
        }
    }

    private int getCloserWidth() {
        return isModifiable() ? CLOSER_DIMENSION + PADDING_IN_ITEM : 0;
    }

    private int getItemWidthAfterCloser() {
        return PADDING_BETWEEN_ITEMS + BORDER_SUM_BETWEEN_ITEMS;
    }

    private int getItemWidthBeforeCloser(Point textExtent) {
        return PADDING_IN_ITEM + PADDING_IN_ITEM_LEFT + textExtent.x + PADDING_IN_ITEM;
    }

    private int lastPosition = -1;

    private Combo getComboEditor() {
        return ((Combo) editor.getEditor());
    }

    private void drawItemCloser(GC gc, int xIter, int yIter, Point textExtent) {
        final Color prevForeground = gc.getForeground();
        gc.setForeground(closerColor);
        gc.setLineWidth(3);
        Rectangle closingButton = new Rectangle(xIter, yIter + textExtent.y / 2,
                CLOSER_DIMENSION, CLOSER_DIMENSION);
        closingButtons.add(closingButton);
        gc.drawLine(closingButton.x, closingButton.y,
                closingButton.x + closingButton.width, closingButton.y + closingButton.height);
        gc.drawLine(closingButton.x + closingButton.width,
                closingButton.y, closingButton.x, closingButton.y + closingButton.height);
        gc.setLineWidth(1);
        gc.setForeground(prevForeground);
    }

    private void drawItemRectangle(GC gc, int xIter, int yIter, String itemToPaint, Point textExtent) {
        final Color previousBackground = gc.getBackground();
        final Color previousForeground = gc.getForeground();

        gc.setBackground(selectedItemBackgroundColor);
        gc.setForeground(selectedItemForegroundColor);
        Rectangle itemRectangle = new Rectangle(xIter, yIter,
                getItemWidthBeforeCloser(textExtent) + getCloserWidth() + PADDING_IN_ITEM, textExtent.y + PADDING_IN_ITEM * 2);
        gc.fillRoundRectangle(itemRectangle.x, itemRectangle.y, itemRectangle.width, itemRectangle.height, 4, 4);
        gc.drawRoundRectangle(itemRectangle.x, itemRectangle.y, itemRectangle.width, itemRectangle.height, 4, 4);

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        gc.drawText(itemToPaint, xIter + PADDING_IN_ITEM + PADDING_IN_ITEM_LEFT, PADDING_IN_ITEM + yIter);

        gc.setBackground(previousBackground);
        gc.setForeground(previousForeground);
    }

    private boolean isModifiable() {
        return (getStyle() & SWT.READ_ONLY) == 0;
    }

    public void setItems(Iterable<String> items) {
        if (isModifiable()) {
            final Combo combo = getComboEditor();
            combo.setItems(Iterables.toArray(items, String.class));
            combo.add(bundle.getString("global.chooseFromList"), 0);
            combo.select(0);

            HashSet<String> newItems = Sets.newHashSet(items);
            List<String> itemsToBeDeleted = Lists.newLinkedList();
            for (String item : this.selectedItems) {
                if (!newItems.contains(item))
                    itemsToBeDeleted.add(item);
            }
            for (String item : itemsToBeDeleted) {
                removeItem(item);
            }
        }
        redraw();
    }

    private void removeItem(String itemToBeDeleted) {
        Iterator<String> iterator = selectedItems.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (item.equals(itemToBeDeleted)) {
                iterator.remove();
                return;
            }
        }
    }

    public void setSelectedItems(Iterable<String> selectedItems) {
        this.selectedItems = Sets.newTreeSet(selectedItems);
        redraw();
    }

    public Iterable<String> getSelectedItems() {
        return Iterables.unmodifiableIterable(selectedItems);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public void setData(String key, Object data) {
        this.dataItems.put(key, data);
    }

    public Object getData(String key) {
        return this.dataItems.get(key);
    }

    public int getItemCount() {
        if (!isModifiable()) {
            return 0;
        }
        return getComboEditor().getItemCount();
    }

    public Color getSelectedItemBackgroundColor() {
        return selectedItemBackgroundColor;
    }

    public void setSelectedItemBackgroundColor(Color selectedItemBackgroundColor) {
        this.selectedItemBackgroundColor = selectedItemBackgroundColor;
    }

    public Color getSelectedItemForegroundColor() {
        return selectedItemForegroundColor;
    }

    public void setSelectedItemForegroundColor(Color selectedItemForegroundColor) {
        this.selectedItemForegroundColor = selectedItemForegroundColor;
    }

    public Color getCloserColor() {
        return closerColor;
    }

    public void setCloserColor(Color closerColor) {
        this.closerColor = closerColor;
    }
}
