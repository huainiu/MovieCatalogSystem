package net.milanaleksic.mcs.application.gui;

import com.google.common.base.*;
import net.milanaleksic.mcs.application.ApplicationManager;
import net.milanaleksic.mcs.application.config.ProgramArgsService;
import net.milanaleksic.mcs.application.gui.helper.*;
import net.milanaleksic.mcs.application.gui.helper.event.*;
import net.milanaleksic.mcs.domain.model.*;
import net.milanaleksic.mcs.infrastructure.config.ApplicationConfiguration;
import net.milanaleksic.mcs.infrastructure.export.*;
import net.milanaleksic.mcs.infrastructure.image.ImageRepository;
import net.milanaleksic.mcs.infrastructure.thumbnail.ThumbnailManager;
import net.milanaleksic.mcs.infrastructure.util.*;
import net.milanaleksic.mcs.infrastructure.worker.WorkerManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

// do not allow java.awt.* to be added to import list because SWT's FileDialog
// will not work in some cases(https://bugs.eclipse.org/bugs/show_bug.cgi?id=349387)

public class MainForm extends Observable {

    private static final Logger log = Logger.getLogger(MainForm.class);

    @Inject
    private NewOrEditMovieDialogForm newOrEditMovieDialogForm;

    @Inject
    private SettingsDialogForm settingsDialogForm;

    @Inject
    private AboutDialogForm aboutDialogForm;

    @Inject
    private ApplicationManager applicationManager;

    @Inject
    private ZanrRepository zanrRepository;

    @Inject
    private TipMedijaRepository tipMedijaRepository;

    @Inject
    private PozicijaRepository pozicijaRepository;

    @Inject
    private DeleteMovieDialogForm deleteMovieDialogForm;

    @Inject
    private FilmRepository filmRepository;

    @Inject
    private ProgramArgsService programArgsService;

    @Inject
    private UnusedMediumsDialogForm unusedMediumsDialogForm;

    @Inject
    private UnmatchedMoviesDialogForm unmatchedMoviesDialogForm;

    @Inject
    private ThumbnailManager thumbnailManager;

    @Inject
    private WorkerManager workerManager;

    @Inject
    private ImageRepository imageRepository;

    private ResourceBundle bundle;

    private final static String titleConst = "Movie Catalog System (C) by Milan.Aleksic@gmail.com"; //NON-NLS

    private Shell shell;
    private Combo comboZanr;
    private Combo comboTipMedija;
    private Combo comboPozicija;
    private Label labelFilter;
    private Label labelFilterDesc;
    private Label labelCurrent;
    private Canvas toolTicker;
    private Menu settingsPopupMenu;
    private Composite statusBar;

    private CurrentViewState currentViewState = new CurrentViewState();

    private CoolMovieComposite mainTable;

    private MovieDetailsPanel movieDetailsPanel;

    // private classes

    private static class CurrentViewState {

        private volatile Long activePage = 0L;
        private volatile Long showableCount = 0L;
        private volatile String filterText = "";
        private volatile int maxItemsPerPage;
        private SingularAttribute<Film, String> singularAttribute = Film_.medijListAsString;
        private boolean ascending = true;

        public boolean isAscending() {
            return ascending;
        }

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public String getFilterText() {
            return filterText;
        }

        public void setFilterText(String filterText) {
            this.filterText = filterText;
            activePage = 0L;
        }

        public Long getActivePage() {
            return activePage;
        }

        public void setActivePage(Long activePage) {
            this.activePage = activePage;
        }

        public Long getShowableCount() {
            return showableCount;
        }

        public void setShowableCount(Long showableCount) {
            this.showableCount = showableCount;
        }

        public void setMaxItemsPerPage(int maxItemsPerPage) {
            this.maxItemsPerPage = maxItemsPerPage;
        }

        public int getMaxItemsPerPage() {
            return maxItemsPerPage;
        }

        public void setCurrentSortOn(SingularAttribute<Film, String> singularAttribute) {
            this.singularAttribute = singularAttribute;
        }

        public SingularAttribute<Film, String> getSingularAttribute() {
            return singularAttribute;
        }
    }

    private class MainTableKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            try {
                String filterText = currentViewState.getFilterText();
                switch (e.keyCode) {
                    case SWT.PAGE_UP:
                        previousPage();
                        return;
                    case SWT.PAGE_DOWN:
                        nextPage();
                        return;
                    case SWT.ESC:
                        if (allFiltersAreCleared())
                            shell.close();
                        else
                            clearAllFilters();
                        return;
                    case SWT.BS:
                        if (!filterText.isEmpty()) {
                            currentViewState.setFilterText(filterText.substring(0, filterText.length() - 1));
                            doFillMainTable();
                        }
                        return;
                }
                if (!Character.isLetterOrDigit(e.character) && e.keyCode != java.awt.event.KeyEvent.VK_SPACE)
                    return;
                currentViewState.setFilterText(filterText + e.character);
                doFillMainTable();
            } finally {
                if (!shell.isDisposed()) {
                    setChanged();
                    MainForm.super.notifyObservers();
                }
            }
        }

        private void clearAllFilters() {
            comboPozicija.select(0);
            comboTipMedija.select(0);
            comboZanr.select(0);
            currentViewState.setFilterText("");
            doFillMainTable();
        }

        private boolean allFiltersAreCleared() {
            String filterText = currentViewState.getFilterText();
            return filterText.isEmpty()
                    && comboPozicija.getSelectionIndex() == 0
                    && comboTipMedija.getSelectionIndex() == 0
                    && comboZanr.getSelectionIndex() == 0;
        }

    }

    private class NextPageSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            nextPage();
            mainTable.setFocus();
        }

    }

    private class PreviousPageSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            previousPage();
            mainTable.setFocus();
        }

    }

    private class MainFormShellListener extends ShellAdapter {

        private boolean activated;

        @Override
        public void shellClosed(ShellEvent e) {
            ApplicationConfiguration.InterfaceConfiguration interfaceConfiguration = applicationManager.getApplicationConfiguration().getInterfaceConfiguration();
            interfaceConfiguration.setLastApplicationLocation(new net.milanaleksic.mcs.infrastructure.config.Rectangle(shell.getBounds()));
            interfaceConfiguration.setMaximized(shell.getMaximized());
        }

        @Override
        public void shellActivated(ShellEvent e) {
            if (activated)
                return;
            activated = true;
            ApplicationConfiguration.InterfaceConfiguration interfaceConfiguration = applicationManager.getApplicationConfiguration().getInterfaceConfiguration();
            net.milanaleksic.mcs.infrastructure.config.Rectangle lastApplicationLocation = interfaceConfiguration.getLastApplicationLocation();
            if (lastApplicationLocation == null)
                shell.setBounds(20, 20, 800, Display.getCurrent().getPrimaryMonitor().getBounds().height - 80);
            else
                shell.setBounds(lastApplicationLocation.toSWTRectangle());
            shell.setMaximized(interfaceConfiguration.isMaximized());
            if (programArgsService.getProgramArgs().isNoInitialMovieListLoading())
                return;
            doFillMainTable();
            executeAdditionalLowPriorityPreparation();
        }
    }

    private class ComboRefreshAdapter extends SelectionAdapter {

        public void widgetSelected(SelectionEvent e) {
            Combo combo = (Combo) e.widget;
            if (combo.getSelectionIndex() == 1)
                combo.select(0);
            currentViewState.setActivePage(0L);
            doFillMainTable();
            mainTable.setFocus();
        }

    }

    private class SortingComboSelectionListener extends SelectionAdapter {

        @SuppressWarnings({"unchecked"})
        public void widgetSelected(SelectionEvent e) {
            Combo combo = (Combo) e.widget;
            int selectionIndex = combo.getSelectionIndex();
            SingularAttribute singularAttribute = ((SingularAttribute[]) combo.getData())[selectionIndex];
            currentViewState.setCurrentSortOn((SingularAttribute<Film, String>) singularAttribute);
            doFillMainTable();
            mainTable.setFocus();
        }

    }

    private class SortingCheckBoxSelectionListener extends SelectionAdapter {

        public void widgetSelected(SelectionEvent e) {
            Button ascending = (Button) e.widget;
            currentViewState.setAscending(ascending.getSelection());
            doFillMainTable();
            mainTable.setFocus();
        }

    }

    private class ToolExportSelectionAdapter extends SelectionAdapter {

        private String[] columnNames;

        public ToolExportSelectionAdapter() {
            columnNames = new String[]{
                    bundle.getString("main.medium"),
                    bundle.getString("main.movieTitle"),
                    bundle.getString("main.movieTitleTranslation"),
                    bundle.getString("main.genre"),
                    bundle.getString("main.location"),
                    bundle.getString("main.comment"),
            };
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            FileDialog dlg = new FileDialog(shell, SWT.SAVE);
            dlg.setFilterNames(new String[]{bundle.getString("main.export.html")});
            dlg.setFilterExtensions(new String[]{"*.htm"}); //NON-NLS
            final String targetFileForExport = dlg.open();
            if (targetFileForExport == null)
                return;
            String ext = targetFileForExport.substring(targetFileForExport.lastIndexOf('.') + 1);
            if (log.isDebugEnabled())
                log.debug("Exporting to file \"" + targetFileForExport + "\""); //NON-NLS
            final Optional<Exporter> exporter = ExporterFactory.getInstance().getExporter(ext);
            if (!exporter.isPresent()) {
                log.error("Exporting to the selected format is not supported"); //NON-NLS
                return;
            }
            getAllFilms(0, new Function<List<Film>, Void>() {
                @Override
                public Void apply(List<Film> filmList) {
                    checkNotNull(filmList);
                    final Film[] allFilms = filmList.toArray(new Film[filmList.size()]);
                    exporter.get().export(new ExporterSource() {

                        @Override
                        public String getTargetFile() {
                            return targetFileForExport;
                        }

                        @Override
                        public int getItemCount() {
                            return allFilms.length;
                        }

                        @Override
                        public int getColumnCount() {
                            return 5;
                        }

                        @Override
                        public String getData(int row, int column) {
                            if (row == -1)
                                return columnNames[column];
                            switch (column) {
                                case 0:
                                    return allFilms[row].getMedijListAsString();
                                case 1:
                                    return allFilms[row].getNazivfilma();
                                case 2:
                                    return allFilms[row].getPrevodnazivafilma();
                                case 3:
                                    return allFilms[row].getZanr().getZanr();
                                case 4:
                                    return allFilms[row].getPozicija();
                                default:
                                    return "";
                            }
                        }

                    });
                    return null;
                }
            });
        }

    }

    private class ToolEraseSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Optional<Film> selectedMovie = mainTable.getSelectedItem();
            if (!selectedMovie.isPresent())
                return;
            deleteMovieDialogForm.open(shell, selectedMovie.get(),
                    new Runnable() {

                        @Override
                        public void run() {
                            doFillMainTable();
                        }

                    });
        }

    }

    private class ToolSettingsSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.detail == SWT.ARROW) {
                ToolItem toolItem = (ToolItem) e.widget;
                ToolBar toolBar = toolItem.getParent();
                Rectangle rect = toolItem.getBounds();
                Point pt = new Point(rect.x, rect.y + rect.height);
                pt = toolBar.toDisplay(pt);
                settingsPopupMenu.setLocation(pt.x, pt.y);
                settingsPopupMenu.setVisible(true);
            } else {
                settingsDialogForm.open(shell, new Runnable() {
                    @Override
                    public void run() {
                        resetPozicije();
                        resetZanrovi();
                        resetMedija();
                        doFillMainTable();
                    }
                });
            }
        }

    }

    private class ToolExitSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            shell.close();
        }

    }

    private class ToolAboutSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            aboutDialogForm.open(shell);
        }

    }

    private class ToolNewSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            newOrEditMovieDialogForm.open(shell, Optional.<Film>absent(), new Runnable() {
                @Override
                public void run() {
                    doFillMainTable();
                }
            });
        }

    }

    private class ShowUnusedMediumsForm extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent selectionEvent) {
            unusedMediumsDialogForm.open(shell, new Runnable() {

                @Override
                public void run() {
                    doFillMainTable();
                }

            });
        }
    }

    private class ShowUnmatchedMoviesForm extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent selectionEvent) {
            unmatchedMoviesDialogForm.open(shell, new Runnable() {

                @Override
                public void run() {
                    doFillMainTable();
                }

            });
        }
    }


    // DESIGN

    public MainForm() {
        this.currentViewState = new CurrentViewState();
        this.addObserver(new Observer() {

            @Override
            public void update(Observable obs, Object arg) {
                if (currentViewState.getMaxItemsPerPage() > 0) {
                    long lowerBound = currentViewState.getActivePage() * currentViewState.getMaxItemsPerPage() + 1;
                    if (currentViewState.getShowableCount() == 0)
                        lowerBound = 0;
                    long upperBound = (currentViewState.getActivePage() + 1) * currentViewState.getMaxItemsPerPage();
                    if (upperBound > currentViewState.getShowableCount())
                        upperBound = currentViewState.getShowableCount();
                    labelCurrent.setText(lowerBound + "-" + upperBound + " (" + currentViewState.getShowableCount().toString() + ")");
                } else
                    labelCurrent.setText(currentViewState.getShowableCount().toString());
                String filter = currentViewState.getFilterText();
                labelFilter.setText(filter);
                labelFilterDesc.setVisible(!filter.isEmpty());
                statusBar.layout();
            }

        });
    }

    public void open() {
        bundle = applicationManager.getMessagesBundle();
        checkCreated();
        shell.open();
        mainTable.setFocus();
    }

    private void checkCreated() {
        if (shell != null)
            return;
        createShell();
        shell.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/database-64.png")); //NON-NLS
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    public boolean isDisposed() {
        return shell.isDisposed();
    }

    private void createShell() {
        shell = new Shell();
        shell.setText(titleConst);
        shell.setMaximized(false);
        shell.setLayout(new GridLayout(1, false));

        createHeader();
        createCenterComposite();
        createStatusBar();

        createSettingsPopupMenu();
        shell.addShellListener(new MainFormShellListener());
    }

    private void createHeader() {
        Composite header = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        header.setLayout(layout);
        header.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        createToolBar(header);
        createToolTicker(header);
    }

    private void createToolTicker(Composite header) {
        GridData tickerGridData = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        tickerGridData.widthHint = 24;
        tickerGridData.heightHint = 24;
        toolTicker = new Canvas(header, SWT.NONE);
        toolTicker.setLayoutData(tickerGridData);
        SWTUtil.addImagePaintListener(toolTicker, "/net/milanaleksic/mcs/application/res/db_find.png"); //NON-NLS
    }

    private void createToolBar(Composite header) {
        final ToolBar toolBar = new ToolBar(header, SWT.FLAT | SWT.WRAP);
        toolBar.setBounds(new Rectangle(11, 50, 4, 50));
        toolBar.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
        ToolItem toolNew = new ToolItem(toolBar, SWT.PUSH);
        toolNew.setText(bundle.getString("global.newMovie"));
        toolNew.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/media.png")); //NON-NLS
        ToolItem toolErase = new ToolItem(toolBar, SWT.PUSH);
        toolErase.setText(bundle.getString("global.deleteMovie"));
        toolErase.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/alert.png")); //NON-NLS
        ToolItem toolExport = new ToolItem(toolBar, SWT.PUSH);
        toolExport.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/folder_outbox.png")); //NON-NLS
        toolExport.setText(bundle.getString("main.export"));
        final ToolItem toolSettings = new ToolItem(toolBar, SWT.DROP_DOWN);
        toolSettings.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/advancedsettings.png")); //NON-NLS
        toolSettings.setWidth(90);
        toolSettings.setText(bundle.getString("main.settings"));
        new ToolItem(toolBar, SWT.SEPARATOR);
        ToolItem toolAbout = new ToolItem(toolBar, SWT.PUSH);
        toolAbout.setText(bundle.getString("global.aboutProgram"));
        toolAbout.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/jabber_protocol.png")); //NON-NLS
        new ToolItem(toolBar, SWT.SEPARATOR);
        ToolItem toolExit = new ToolItem(toolBar, SWT.PUSH);
        toolExit.setText(bundle.getString("main.exit"));
        toolExit.setImage(imageRepository.getResourceImage("/net/milanaleksic/mcs/application/res/shutdown.png")); //NON-NLS

        toolNew.addSelectionListener(new ToolNewSelectionAdapter());
        toolErase.addSelectionListener(new ToolEraseSelectionAdapter());
        toolExport.addSelectionListener(new ToolExportSelectionAdapter());
        toolSettings.addSelectionListener(new ToolSettingsSelectionAdapter());
        toolAbout.addSelectionListener(new ToolAboutSelectionAdapter());
        toolExit.addSelectionListener(new ToolExitSelectionAdapter());
    }

    private void createCenterComposite() {
        Composite centerComposite = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        centerComposite.setLayout(layout);
        centerComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        ScrolledComposite scrolledComposite = new ScrolledComposite(centerComposite, SWT.V_SCROLL | SWT.NO);
        mainTable = new CoolMovieComposite(scrolledComposite, SWT.NONE, thumbnailManager);
        mainTable.addMovieSelectionListener(new MovieSelectionListener() {

            @Override
            public void movieSelected(MovieSelectionEvent e) {
                movieDetailsPanel.showDataForMovie(e.film);
            }

            @Override
            public void movieDetailsSelected(MovieSelectionEvent e) {
                newOrEditMovieDialogForm.open(shell, Optional.of(filmRepository.getCompleteFilm(e.film.get())),
                        new Runnable() {
                            @Override
                            public void run() {
                                doFillMainTable();
                            }
                        });
            }

        });
        mainTable.addKeyListener(new MainTableKeyAdapter());
        scrolledComposite.setContent(mainTable);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.getVerticalBar().setIncrement(10);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolledComposite.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_GRAY));

        movieDetailsPanel = new MovieDetailsPanel(centerComposite, SWT.NONE, bundle, thumbnailManager);
        GridData layoutData = new GridData(SWT.FILL, SWT.END, true, false);
        layoutData.heightHint = 150;
        movieDetailsPanel.setLayoutData(layoutData);
    }

    private void createComboZanr(Composite panCombos) {
        GridData gridData5 = new GridData();
        gridData5.widthHint = 80;
        comboZanr = new Combo(panCombos, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        comboZanr.setLayoutData(gridData5);
        comboZanr.setVisibleItemCount(16);
        comboZanr.addSelectionListener(new ComboRefreshAdapter());
        resetZanrovi();
    }

    private void createComboTipMedija(Composite panCombos) {
        GridData gridData1 = new GridData();
        gridData1.widthHint = 80;
        comboTipMedija = new Combo(panCombos, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        comboTipMedija.setLayoutData(gridData1);
        comboTipMedija.setVisibleItemCount(8);
        comboTipMedija.addSelectionListener(new ComboRefreshAdapter());
        resetMedija();
    }

    private void createComboPozicija(Composite panCombos) {
        GridData gridData3 = new GridData();
        gridData3.widthHint = 80;
        comboPozicija = new Combo(panCombos, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        comboPozicija.setLayoutData(gridData3);
        comboPozicija.setVisibleItemCount(8);
        comboPozicija.addSelectionListener(new ComboRefreshAdapter());
        resetPozicije();
    }

    private void resetZanrovi() {
        comboZanr.setItems(new String[]{});
        comboZanr.add(bundle.getString("main.allGenres"));
        comboZanr.add("-----------");
        workerManager.submitLongTaskWithResultProcessingInSWTThread(
                new Callable<List<Zanr>>() {
                    @Override
                    public List<Zanr> call() throws Exception {
                        return zanrRepository.getZanrs();
                    }
                },
                new Function<List<Zanr>, Void>() {
                    @Override
                    public Void apply(List<Zanr> zanrs) {
                        checkNotNull(zanrs);
                        int iter = 2; // each item except first 2 will have Zanr object as data
                        for (Zanr zanr : zanrs) {
                            comboZanr.setData(Integer.toString(iter++), zanr);
                            comboZanr.add(zanr.toString());
                        }
                        comboZanr.select(0);
                        return null;
                    }
                }
        );
    }

    private void resetPozicije() {
        comboPozicija.setItems(new String[]{});
        comboPozicija.add(bundle.getString("main.anyLocation"));
        comboPozicija.add("-----------");
        workerManager.submitLongTaskWithResultProcessingInSWTThread(new Callable<List<Pozicija>>() {
                    @Override
                    public List<Pozicija> call() throws Exception {
                        return pozicijaRepository.getPozicijas();
                    }
                },
                new Function<List<Pozicija>, Void>() {
                    @Override
                    public Void apply(List<Pozicija> pozicijas) {
                        checkNotNull(pozicijas);
                        for (Pozicija pozicija : pozicijas) {
                            comboPozicija.setData(Integer.toString(comboPozicija.getItemCount()), pozicija);
                            comboPozicija.add(pozicija.toString());
                        }
                        comboPozicija.select(0);
                        return null;
                    }
                }
        );
    }

    private void resetMedija() {
        comboTipMedija.setItems(new String[]{});
        comboTipMedija.add(bundle.getString("main.allMediums"));
        comboTipMedija.add("-----------");
        workerManager.submitLongTaskWithResultProcessingInSWTThread(new Callable<List<TipMedija>>() {
                    @Override
                    public List<TipMedija> call() throws Exception {
                        return tipMedijaRepository.getTipMedijas();
                    }
                },
                new Function<List<TipMedija>, Void>() {
                    @Override
                    public Void apply(List<TipMedija> tipMedijas) {
                        checkNotNull(tipMedijas);
                        for (TipMedija tip : tipMedijas) {
                            comboTipMedija.setData(Integer.toString(comboTipMedija.getItemCount()), tip);
                            comboTipMedija.add(tip.toString());
                        }
                        comboTipMedija.select(0);
                        return null;
                    }
                }
        );
    }

    private void createStatusBar() {
        statusBar = new Composite(shell, SWT.BORDER);
        statusBar.setLayoutData(new GridData(GridData.FILL, GridData.END, true, false));
        statusBar.setLayout(new GridLayout(4, false));
        addStatusPagingCell();
        addStatusEntityFilteringCell();
        addStatusTextSearchFilterCell();
        addStatusSortCell();
    }

    private void addStatusPagingCell() {
        Composite pagingComposite = createNoMarginStatusBarCell(3, SWT.BEGINNING);
        Button btnPrevPage = new Button(pagingComposite, SWT.PUSH);
        btnPrevPage.setText("<<");
        btnPrevPage.addSelectionListener(new PreviousPageSelectionAdapter());

        labelCurrent = new Label(pagingComposite, SWT.NONE);
        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false);
        layoutData.widthHint = 90;
        labelCurrent.setLayoutData(layoutData);
        labelCurrent.setAlignment(SWT.CENTER);
        labelCurrent.setText("0");

        Button btnNextPage = new Button(pagingComposite, SWT.PUSH);
        btnNextPage.setText(">>");
        btnNextPage.addSelectionListener(new NextPageSelectionAdapter());
    }

    private void addStatusEntityFilteringCell() {
        Composite entityFilteringComposite = createNoMarginStatusBarCell(3, SWT.BEGINNING);
        createComboTipMedija(entityFilteringComposite);
        createComboPozicija(entityFilteringComposite);
        createComboZanr(entityFilteringComposite);
    }

    private void addStatusTextSearchFilterCell() {
        Composite filterComposite = createNoMarginStatusBarCell(2, SWT.BEGINNING);
        labelFilterDesc = new Label(filterComposite, SWT.NONE);
        labelFilterDesc.setText(bundle.getString("main.activeFilter"));
        labelFilterDesc.setVisible(false);
        labelFilter = new Label(filterComposite, SWT.NONE);
        FontData systemFontData = SWTUtil.getSystemFontData();
        Font systemFont = new Font(shell.getDisplay(), systemFontData.getName(), systemFontData.getHeight(), SWT.BOLD);
        labelFilter.setFont(systemFont);
        labelFilter.setText("");
    }

    private void addStatusSortCell() {
        Composite sortComposite = createNoMarginStatusBarCell(3, SWT.END);
        Label sortLabel = new Label(sortComposite, SWT.NONE);
        sortLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sortLabel.setText(bundle.getString("main.sortOn"));
        sortLabel.setAlignment(SWT.RIGHT);
        Combo combo = new Combo(sortComposite, SWT.READ_ONLY);
        combo.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        combo.setItems(new String[]{
                bundle.getString("main.medium"),
                bundle.getString("main.movieTitle"),
                bundle.getString("main.movieTitleTranslation"),
        });
        combo.setData(new SingularAttribute[]{
                Film_.medijListAsString,
                Film_.nazivfilma,
                Film_.prevodnazivafilma
        });
        combo.select(0);
        combo.addSelectionListener(new SortingComboSelectionListener());

        Button cbAscending = new Button(sortComposite, SWT.CHECK);
        cbAscending.setText(bundle.getString("main.ascending"));
        cbAscending.setSelection(true);
        cbAscending.addSelectionListener(new SortingCheckBoxSelectionListener());
    }

    private Composite createNoMarginStatusBarCell(int numColumns, int horizontalAlignment) {
        Composite pagingComposite = new Composite(statusBar, SWT.NONE);
        GridLayout layout = new GridLayout(numColumns, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        pagingComposite.setLayout(layout);
        pagingComposite.setLayoutData(new GridData(horizontalAlignment, SWT.CENTER, horizontalAlignment == SWT.END, false));
        return pagingComposite;
    }

    private void createSettingsPopupMenu() {
        settingsPopupMenu = new Menu(shell, SWT.POP_UP);
        MenuItem settingsMenuItem = new MenuItem(settingsPopupMenu, SWT.PUSH);
        settingsMenuItem.setText(bundle.getString("main.settings"));
        settingsMenuItem.addSelectionListener(new ToolSettingsSelectionAdapter());
        settingsPopupMenu.setDefaultItem(settingsMenuItem);
        MenuItem findUnusedMediums = new MenuItem(settingsPopupMenu, SWT.PUSH);
        findUnusedMediums.setText(bundle.getString("main.findUnusedMediums"));
        findUnusedMediums.addSelectionListener(new ShowUnusedMediumsForm());
        MenuItem findUmatchedImdbMovies = new MenuItem(settingsPopupMenu, SWT.PUSH);
        findUmatchedImdbMovies.setText(bundle.getString("main.findUmatchedImdbMovies"));
        findUmatchedImdbMovies.addSelectionListener(new ShowUnmatchedMoviesForm());
    }


    // LOGIC


    private void doFillMainTable() {
        toolTicker.setVisible(true);
        toolTicker.update();
        getAllFilms(applicationManager.getUserConfiguration().getElementsPerPage(), new Function<List<Film>, Void>() {
            @Override
            public Void apply(@Nullable List<Film> films) {
                mainTable.setMovies(Optional.fromNullable(films));

                MainForm.this.setChanged();
                MainForm.super.notifyObservers();

                toolTicker.setVisible(false);

                return null;
            }
        });
    }

    private void nextPage() {
        if (currentViewState.getMaxItemsPerPage() > 0)
            if (currentViewState.getMaxItemsPerPage() * (currentViewState.getActivePage() + 1) > currentViewState.getShowableCount())
                return;
        currentViewState.setActivePage(currentViewState.getActivePage() + 1);
        doFillMainTable();
    }

    private void previousPage() {
        if (currentViewState.getActivePage() == 0)
            return;
        currentViewState.setActivePage(currentViewState.getActivePage() - 1);
        doFillMainTable();
    }

    public void getAllFilms(final int maxItems, Function<List<Film>, Void> whatToDoWithFilms) {
        final Zanr zanrFilter = (Zanr) comboZanr.getData(Integer.toString(comboZanr.getSelectionIndex()));
        final TipMedija tipMedijaFilter = (TipMedija) comboTipMedija.getData(Integer.toString(comboTipMedija.getSelectionIndex()));
        final Pozicija pozicijaFilter = (Pozicija) comboPozicija.getData(Integer.toString(comboPozicija.getSelectionIndex()));
        final String filterText = currentViewState.getFilterText().isEmpty() ?
                null :
                '%' + currentViewState.getFilterText() + '%';
        final int startFrom = currentViewState.getActivePage().intValue() * maxItems;

        workerManager.submitLongTaskWithResultProcessingInSWTThread(
                new Callable<List<Film>>() {
                    @Override
                    @MethodTiming(name = "getAllFilms")
                    public List<Film> call() throws Exception {
                        currentViewState.setMaxItemsPerPage(maxItems);
                        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(startFrom, maxItems,
                                Optional.fromNullable(zanrFilter),
                                Optional.fromNullable(tipMedijaFilter),
                                Optional.fromNullable(pozicijaFilter),
                                Optional.fromNullable(filterText),
                                currentViewState.getSingularAttribute(),
                                currentViewState.isAscending());
                        currentViewState.setShowableCount(filmsWithCount.count);
                        return filmsWithCount.films;
                    }
                }, whatToDoWithFilms);
    }

    private void executeAdditionalLowPriorityPreparation() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                doExecuteAdditionalLowPriorityPreparation();
            }
        }.start();
    }

    @MethodTiming
    private void doExecuteAdditionalLowPriorityPreparation() {
        thumbnailManager.preCacheThumbnails();
    }

}