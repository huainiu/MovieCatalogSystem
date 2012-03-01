package net.milanaleksic.mcs.application.gui;

import com.google.common.base.Function;
import net.milanaleksic.mcs.application.gui.helper.*;
import net.milanaleksic.mcs.application.util.ApplicationException;
import net.milanaleksic.mcs.domain.model.Film;
import net.milanaleksic.mcs.domain.service.FilmService;
import net.milanaleksic.mcs.infrastructure.network.HttpClientFactoryService;
import net.milanaleksic.mcs.infrastructure.network.PersistentHttpContext;
import net.milanaleksic.mcs.infrastructure.tmdb.TmdbException;
import net.milanaleksic.mcs.infrastructure.tmdb.TmdbService;
import net.milanaleksic.mcs.infrastructure.tmdb.bean.ImageInfo;
import net.milanaleksic.mcs.infrastructure.tmdb.bean.Movie;
import net.milanaleksic.mcs.infrastructure.util.IMDBUtil;
import net.milanaleksic.mcs.infrastructure.util.SWTUtil;
import net.milanaleksic.mcs.infrastructure.worker.WorkerManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class UnmatchedMoviesDialogForm extends AbstractDialogForm {

    @Inject
    private FilmService filmService;

    @Inject
    private WorkerManager workerManager;

    @Inject
    private TmdbService tmdbService;

    @Inject
    private HttpClientFactoryService httpClientFactoryService;

    private Table unmatchedMoviesTable;

    private Table possibleMatchesTable;

    private PersistentHttpContext persistentHttpContext;

    private LinkedList<Future<?>> listOfQueuedWorkers;
    private Map<Film, Integer> failureCountMap;
    private Map<Film, Movie[]> movieMatchesMap;

    private ShowImageComposite matchImage;
    private Text matchDescription;

    private HandledSelectionAdapter unmatchedMovieTableItemSelected = new HandledSelectionAdapter(shell, bundle) {
        @Override
        public void handledSelected(SelectionEvent event) throws ApplicationException {
            possibleMatchesTable.removeAll();
            int selectionIndex = unmatchedMoviesTable.getSelectionIndex();
            if (selectionIndex < 0)
                return;
            TableItem item = unmatchedMoviesTable.getItem(selectionIndex);
            Film film = (Film) item.getData();
            if (!movieMatchesMap.containsKey(film))
                return;
            Movie[] movies = movieMatchesMap.get(film);
            for (final Movie movie : movies) {
                createItemForMovieMatch(movie);
            }
        }

        private void createItemForMovieMatch(final Movie movie) {
            TableItem matchItem = new TableItem(possibleMatchesTable, SWT.NONE);
            matchItem.setText(new String[]{
                    movie.getName(),
                    movie.getReleasedYear(),
                    ""}
            );
            matchItem.setData(movie);
            TableEditor editor = new TableEditor(possibleMatchesTable);
            final Link link = new Link(possibleMatchesTable, SWT.NONE);
            link.setText("<A>" + bundle.getString("unmatchedMoviesTable.matches.url") + "</A>");
            link.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            link.addSelectionListener(new HandledSelectionAdapter(shell, bundle) {
                @Override
                public void handledSelected(SelectionEvent event) throws ApplicationException {
                    try {
                        Desktop.getDesktop().browse(IMDBUtil.createUriBasedOnId(movie.getImdbId()));
                    } catch (IOException e) {
                        throw new ApplicationException("Unexpected IO exception when trying to open URL based on received IMDB link");
                    }
                }
            });
            link.pack();
            editor.minimumWidth = link.getSize().x;
            editor.horizontalAlignment = SWT.LEFT;
            editor.setEditor(link, matchItem, 2);
            matchItem.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent disposeEvent) {
                    link.dispose();
                }
            });
        }
    };

    private HandledSelectionAdapter possibleMatchesTableItemSelected = new HandledSelectionAdapter(shell, bundle) {
        @Override
        public void handledSelected(SelectionEvent event) throws ApplicationException {
            int selectionIndex = possibleMatchesTable.getSelectionIndex();
            if (selectionIndex < 0)
                return;
            TableItem item = possibleMatchesTable.getItem(selectionIndex);
            Movie movie = (Movie) item.getData();
            String appropriateImageUrl = getAppropriateImageUrl(movie);
            matchDescription.setText(movie.getOverview());
            if (appropriateImageUrl == null)
                setStatusAndImage("unmatchedMoviesTable.noImageFound", null);
            else
                schedulePosterDownload(appropriateImageUrl);
        }

        private void schedulePosterDownload(final String appropriateImageUrl) {
            listOfQueuedWorkers.add(workerManager.submitWorker(new HandledRunnable(shell, bundle) {
                @Override
                public void handledRun() {
                    setStatusAndImage("unmatchedMoviesTable.downloadingImage", null);
                    SWTUtil.createImageFromUrl(URI.create(appropriateImageUrl),
                            persistentHttpContext,
                            new Function<Image, Void>() {
                                @Override
                                public Void apply(@Nullable final Image image) {
                                    setStatusAndImage(null, image);
                                    return null;
                                }
                            }
                    );
                }
            }));
        }

        private String getAppropriateImageUrl(Movie movie) {
            for (ImageInfo imageInfo : movie.getPosters()) {
                if (imageInfo.getImage().getWidth() == 185)
                    return imageInfo.getImage().getUrl();
            }
            return null;
        }

        private void setStatusAndImage(@Nullable final String resourceId, @Nullable final Image image) {
            shell.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (resourceId != null)
                        matchImage.setStatus(bundle.getString(resourceId));
                    matchImage.setImage(image);
                }
            });
        }
    };

    @Override
    protected void onShellCreated() {
        GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.verticalSpacing = 10;
        shell.setText(bundle.getString("global.unusedMediums"));
        shell.setLayout(gridLayout);
        createContent();
    }

    @Override
    protected void onShellReady() {
        persistentHttpContext = httpClientFactoryService.createPersistentHttpContext();
        readData();
    }

    private void readData() {
        java.util.List<Film> filmovi = filmService.getListOfUnmatchedMovies();
        unmatchedMoviesTable.removeAll();
        for (Film film : filmovi) {
            TableItem tableItem = new TableItem(unmatchedMoviesTable, SWT.NONE);
            tableItem.setText(new String[]{film.toString(), bundle.getString("unmatchedMoviesTable.status.awaiting")});
            tableItem.setData(film);
        }
        movieMatchesMap = new ConcurrentHashMap<>();
        listOfQueuedWorkers = new LinkedList<>();
        failureCountMap = new ConcurrentHashMap<>();
    }

    private void createContent() {
        final int ELEMENTS_IN_FIRST_ROW = 4;
        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(ELEMENTS_IN_FIRST_ROW, false));

        // first row
        createUnmatchedMoviesTable(composite);
        createBtnStartProcess(composite);
        createPossibleMatchesTable(composite);
        createMatchImageContainer(composite);

        //second row
        Composite compositeFooter = new Composite(composite, SWT.NONE);
        compositeFooter.setLayout(new GridLayout(1, false));
        compositeFooter.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, ELEMENTS_IN_FIRST_ROW, 1));
        Button btnClose = new Button(compositeFooter, SWT.NONE);
        GridData btnCloseGridData = new GridData(GridData.CENTER, GridData.CENTER, true, false);
        btnCloseGridData.widthHint = 150;
        btnClose.setLayoutData(btnCloseGridData);
        btnClose.setText(bundle.getString("global.close"));
        btnClose.addSelectionListener(new HandledSelectionAdapter(shell, bundle) {
            @Override
            public void handledSelected(SelectionEvent event) throws ApplicationException {
                shell.close();
            }
        });
    }

    private void createBtnStartProcess(Composite composite) {
        final Button btnStartMatching = new Button(composite, SWT.NONE);
        btnStartMatching.setText(bundle.getString("unmatchedMoviesTable.start"));
        btnStartMatching.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        btnStartMatching.addSelectionListener(new HandledSelectionAdapter(shell, bundle) {
            @Override
            public void handledSelected(SelectionEvent event) throws ApplicationException {
                btnStartMatching.setEnabled(false);
                startProcess();
            }
        });
    }

    private void createMatchImageContainer(Composite composite) {
        Composite matcherPanel = new Composite(composite, SWT.NONE);
        matcherPanel.setLayout(new GridLayout(1, false));
        matcherPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));


        TabFolder folder = new TabFolder(matcherPanel, SWT.NONE);
        GridData folderGroupData = new GridData(GridData.FILL, GridData.FILL, true, true);
        folderGroupData.widthHint = 200;
        folder.setLayoutData(folderGroupData);
        matchImage = new ShowImageComposite(folder, SWT.NONE);
        matchImage.setBundle(bundle);
        matchImage.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        matchImage.setLayout(new FillLayout());
        matchDescription = new Text(folder, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        matchDescription.setText(bundle.getString("unmatchedMoviesTable.pickAMatch"));

        TabItem tabImage = new TabItem(folder, SWT.NONE);
        tabImage.setText(bundle.getString("unmatchedMoviesTable.tab.image"));
        tabImage.setControl(matchImage);
        TabItem tabDescription = new TabItem(folder, SWT.NONE);
        tabDescription.setText(bundle.getString("unmatchedMoviesTable.tab.description"));
        tabDescription.setControl(matchDescription);

        final Button btnAcceptThisMatch = new Button(matcherPanel, SWT.NONE);
        btnAcceptThisMatch.setText(bundle.getString("unmatchedMoviesTable.acceptMatch"));
        btnAcceptThisMatch.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        btnAcceptThisMatch.addSelectionListener(new HandledSelectionAdapter(shell, bundle) {
            @Override
            public void handledSelected(SelectionEvent event) throws ApplicationException {
                //TODO: finalize the matching process
                MessageBox messageBox = new MessageBox(shell, SWT.NONE);
                messageBox.setMessage("NYI");
                messageBox.open();
            }
        });
    }

    private void createPossibleMatchesTable(Composite composite) {
        possibleMatchesTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        possibleMatchesTable.setHeaderVisible(true);
        GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 2);
        gridData.heightHint = 200;
        possibleMatchesTable.setLayoutData(gridData);
        possibleMatchesTable.addSelectionListener(possibleMatchesTableItemSelected);
        TableColumn firstColumn = new TableColumn(possibleMatchesTable, SWT.LEFT | SWT.FLAT);
        firstColumn.setText(bundle.getString("unmatchedMoviesTable.matches.nameColumn"));
        firstColumn.setWidth(300);
        TableColumn yearColumn = new TableColumn(possibleMatchesTable, SWT.LEFT | SWT.FLAT);
        yearColumn.setText(bundle.getString("unmatchedMoviesTable.matches.yearColumn"));
        yearColumn.setWidth(50);
        TableColumn linkColumn = new TableColumn(possibleMatchesTable, SWT.LEFT | SWT.FLAT);
        linkColumn.setText(bundle.getString("unmatchedMoviesTable.matches.imdbLink"));
        linkColumn.setWidth(50);
    }

    private void createUnmatchedMoviesTable(Composite parent) {
        unmatchedMoviesTable = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        unmatchedMoviesTable.setHeaderVisible(true);
        GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
        gridData.heightHint = 350;
        unmatchedMoviesTable.setLayoutData(gridData);
        TableColumn firstColumn = new TableColumn(unmatchedMoviesTable, SWT.LEFT | SWT.FLAT);
        firstColumn.setText(bundle.getString("unmatchedMoviesTable.firstColumnName"));
        firstColumn.setWidth(300);
        TableColumn processingColumn = new TableColumn(unmatchedMoviesTable, SWT.LEFT | SWT.FLAT);
        processingColumn.setText(bundle.getString("unmatchedMoviesTable.processingColumnName"));
        processingColumn.setWidth(120);
        unmatchedMoviesTable.addSelectionListener(unmatchedMovieTableItemSelected);
    }

    private synchronized void startProcess() {
        //TODO: remove boundaries
        int limitedMaxNumber = unmatchedMoviesTable.getItemCount();
        if (limitedMaxNumber > 10)
            limitedMaxNumber = 10;
        for (int i = 0; i < limitedMaxNumber; i++) {
            final TableItem item = unmatchedMoviesTable.getItem(i);
            item.setText(1, bundle.getString("unmatchedMoviesTable.status.enqueued"));
            addProcessingWorker(item, (Film) item.getData());
        }
    }

    private void addProcessingWorker(final TableItem item, final Film film) {
        listOfQueuedWorkers.add(workerManager.submitWorker(new HandledRunnable(shell, bundle) {
            @Override
            public void handledRun() {
                try {
                    Integer failureCount = failureCountMap.get(film);
                    if (failureCount != null && failureCount >= 3)
                        setStatusOnItem(item, bundle.getString("unmatchedMoviesTable.status.gaveUp"));
                    setStatusOnItem(item, bundle.getString("unmatchedMoviesTable.status.processing"));
                    Movie[] movies = tmdbService.searchForMovies(film.getNazivfilma());
                    if (movieMatchesMap == null)
                        return;
                    movieMatchesMap.put(film, movies);
                    setStatusOnItem(item, bundle.getString("unmatchedMoviesTable.status.processed")
                            + " (" + (movies == null ? 0 : movies.length) + ")");
                } catch (TmdbException e) {
                    logger.error("Application error while processing movie: " + film.getNazivfilma(), e);
                    retryForMovie(item, film);
                }
            }

            private synchronized void retryForMovie(TableItem item, Film film) {
                Integer failureCount = failureCountMap.get(film);
                failureCountMap.put(film, failureCount == null ? 1 : failureCount + 1);
                addProcessingWorker(item, film);
            }
        }));
    }

    private void setStatusOnItem(final TableItem item, final String status) {
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                item.setText(1, status);
            }
        });
    }

    @Override
    protected synchronized boolean onShouldShellClose() {
        if (thereAreUnfinishedWorkers()) {
            MessageBox messageBox = new MessageBox(shell, SWT.APPLICATION_MODAL | SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setMessage(bundle.getString("unmatchedMoviesTable.processNotFinishedConfirm"));
            if (messageBox.open() != SWT.YES)
                return false;
            logger.info("Killing all unfinished workers");
            killAllUnfinishedWorkers();
        }
        clearProcessingData();
        return true;
    }

    private void clearProcessingData() {
        if (listOfQueuedWorkers != null) {
            listOfQueuedWorkers.clear();
            listOfQueuedWorkers = null;
        }
        if (movieMatchesMap != null) {
            movieMatchesMap.clear();
            movieMatchesMap = null;
        }
        if (failureCountMap != null) {
            failureCountMap.clear();
            failureCountMap = null;
        }
    }

    private void killAllUnfinishedWorkers() {
        for (Future<?> future : listOfQueuedWorkers) {
            if (future != null && !future.isCancelled() && !future.isDone())
                future.cancel(true);
        }
    }

    private boolean thereAreUnfinishedWorkers() {
        if (listOfQueuedWorkers == null || listOfQueuedWorkers.size() == 0)
            return false;
        for (Future<?> future : listOfQueuedWorkers) {
            if (future != null && !future.isCancelled() && !future.isDone())
                return true;
        }
        return false;
    }
}
