package net.milanaleksic.mcs.infrastructure.persistence.jpa;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import net.milanaleksic.mcs.domain.model.*;
import net.milanaleksic.mcs.infrastructure.restore.RestorePointRestorer;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.*;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.inject.Inject;
import javax.persistence.*;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * User: Milan Aleksic
 * Date: 8/23/11
 * Time: 8:50 PM
 */
@SuppressWarnings({"HardCodedStringLiteral"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:spring-beans-testing.xml",
        "classpath:spring-beans.xml"
})
public class JpaFilmRepositoryIntegrationTest {

    @Inject
    private PlatformTransactionManager transactionManager;

    @PersistenceContext(name = "MovieCatalogSystemDB")
    private EntityManager entityManager;

    @Inject
    private FilmRepository filmRepository;

    @Inject
    private ZanrRepository zanrRepository;

    @Inject
    private TagRepository tagRepository;

    @Inject
    private RestorePointRestorer restorePointRestorer;

    @Inject
    private PozicijaRepository pozicijaRepository;

    @Inject
    private TipMedijaRepository tipMedijaRepository;

    @Inject
    private MedijRepository medijRepository;

    @Before
    public void prepare() {
        BasicConfigurator.configure();
        final TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        try {
            final Query nativeQuery = entityManager.createNativeQuery("drop schema if exists db2admin");
            nativeQuery.executeUpdate();
            transactionManager.commit(transaction);
        } catch(Exception e) {
            transactionManager.rollback(transaction);
            e.printStackTrace();
            fail("Unexpected integration test DB failure");
        }
        restorePointRestorer.restoreDatabaseIfNeeded();
    }

    @Test
    public void get_films() {
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 10, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films should be empty", filmsWithCount.films.size(), equalTo(0));
        addSomeMovies();
        filmsWithCount = filmRepository.getFilmByCriteria(0, 10, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films should not be empty", filmsWithCount.films.size(), not(0));
        assertThat("Size is not as expected!", filmsWithCount.films.size(), equalTo(10));
    }

    private void addSomeMovies() {
        final Pozicija atJob = pozicijaRepository.addPozicija(new Pozicija("at job", false));
        final TipMedija dvdType = tipMedijaRepository.addTipMedija("DVD");
        final List<Medij> medijs = Lists.asList(
                medijRepository.saveMedij(1, dvdType),
                new Medij[]{medijRepository.saveMedij(2, dvdType)});
        final Zanr actionGenre = zanrRepository.addZanr("action");
        final Zanr horrorGenre = zanrRepository.addZanr("horror");
        final Tag primeTag = tagRepository.addTag("prime");
        final Tag oddTag = tagRepository.addTag("odd");
        for (int i = 0; i < 11; i++) {
            final Film film = new Film();
            film.setNazivfilma("Test movie " + i);
            film.setPrevodnazivafilma("Translation of test movie");
            film.setGodina(2000);
            film.setImdbId("");
            film.setKomentar("Test komentar");
            final List<Tag> tags = Lists.newLinkedList();
            if (i % 2 == 1)
                tags.add(oddTag);
            if (i == 1 || i ==3 || i==5 || i==7)
                tags.add(primeTag);
            filmRepository.saveFilm(
                    film,
                    i >= 9 ? horrorGenre : actionGenre,
                    medijs,
                    atJob,
                    tags);
        }
    }

    @Test
    public void get_films_second_page() {
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 7, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films should be empty", filmsWithCount.films.size(), equalTo(0));
        addSomeMovies();

        FilmRepository.FilmsWithCount filmsWithCountFirstPage = filmRepository.getFilmByCriteria(0, 7, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films are empty", filmsWithCountFirstPage.films.size(), not(0));
        assertThat("Size is not as expected!", filmsWithCountFirstPage.films.size(), equalTo(7));

        FilmRepository.FilmsWithCount filmsWithCountSecondPage = filmRepository.getFilmByCriteria(7, 5, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films are empty", filmsWithCountSecondPage.films.size(), not(0));
        assertThat("Size is not as expected!", filmsWithCountSecondPage.films.size(), equalTo(4));

        for (Film filmFromFirstPage : filmsWithCountFirstPage.films) {
            assertFalse("second page has at least one element from first page", filmsWithCountSecondPage.films.contains(filmFromFirstPage));
        }
    }

    @Test
    public void get_films_no_limit() {
        addSomeMovies();
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 0, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("There should be 11 movies", filmsWithCount.films.size(), equalTo(11));
        assertThat("Films count is not identical to the collection size", 0L + filmsWithCount.films.size(), equalTo(filmsWithCount.count));
    }

    @Test
    public void get_films_with_zanr() {
        addSomeMovies();
        Zanr zanr = zanrRepository.getZanrByName("action");
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 9, Optional.of(zanr), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        for (Film film : filmsWithCount.films) {
            assertThat("Zanr is not correct", zanr.getIdzanr(), equalTo(film.getZanr().getIdzanr()));
        }
        assertThat("Size is not as expected!", filmsWithCount.films.size(), equalTo(9));
    }

    @Test
    public void get_films_with_zanr_no_limit() {
        addSomeMovies();
        Zanr zanr = zanrRepository.getZanrByName("action");
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 0, Optional.of(zanr), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        Assert.assertTrue("Films are empty", filmsWithCount.films.size() != 0);
        for (Film film : filmsWithCount.films) {
            assertThat("Zanr is not correct", zanr.getIdzanr(), equalTo(film.getZanr().getIdzanr()));
            Assert.assertEquals(zanr.getIdzanr(), film.getZanr().getIdzanr());
        }
        assertThat("Films count is not identical to the collection size!", 0L + filmsWithCount.films.size(), equalTo(filmsWithCount.count));
    }

    @Test
    public void get_films_with_tag() {
        addSomeMovies();
        Tag tag = tagRepository.getTagByName("prime");
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 0, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.of(tag), Optional.<String>absent(), Film_.medijListAsString, true);
        for (Film film : filmsWithCount.films) {
            film = filmRepository.getCompleteFilm(film);
            assertThat("Tag is not correct", film.getTags(), hasItem(tag));
        }
        assertThat("Number of prime items is not as expected!", filmsWithCount.films.size(), equalTo(4));

        tag = tagRepository.getTagByName("odd");
        filmsWithCount = filmRepository.getFilmByCriteria(0, 0, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.of(tag), Optional.<String>absent(), Film_.medijListAsString, true);
        for (Film film : filmsWithCount.films) {
            film = filmRepository.getCompleteFilm(film);
            assertThat("Tag is not correct", film.getTags(), hasItem(tag));
        }
        assertThat("Number of odd items is not as expected!", filmsWithCount.films.size(), equalTo(5));
    }

}
