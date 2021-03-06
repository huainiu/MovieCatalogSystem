package net.milanaleksic.mcs.infrastructure.persistence.jpa;

import com.google.common.base.Optional;
import net.milanaleksic.mcs.domain.model.*;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * User: Milan Aleksic
 * Date: 8/23/11
 * Time: 8:50 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-beans.xml"})
public class JpaFilmRepositoryIntegrationTest {

    @Inject
    private FilmRepository filmRepository;

    @Inject
    private ZanrRepository zanrRepository;

    @Inject
    private TagRepository tagRepository;

    @Before
    public void prepare() {
        BasicConfigurator.configure();
    }

    @Test
    public void get_films() {
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 10, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films are empty", filmsWithCount.films.size(), not(0));
        assertThat("Size is not as expected!", filmsWithCount.films.size(), equalTo(10));
    }

    @Test
    public void get_films_second_page() {
        FilmRepository.FilmsWithCount filmsWithCountFirstPage = filmRepository.getFilmByCriteria(0, 50, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films are empty", filmsWithCountFirstPage.films.size(), not(0));
        assertThat("Size is not as expected!", filmsWithCountFirstPage.films.size(), equalTo(50));

        FilmRepository.FilmsWithCount filmsWithCountSecondPage = filmRepository.getFilmByCriteria(50, 50, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films are empty", filmsWithCountSecondPage.films.size(), not(0));
        assertThat("Size is not as expected!", filmsWithCountSecondPage.films.size(), equalTo(50));

        for(Film filmFromFirstPage : filmsWithCountFirstPage.films) {
            assertFalse("second page has at least one element from first page", filmsWithCountSecondPage.films.contains(filmFromFirstPage));
        }
    }

    @Test
    public void get_films_no_limit() {
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 0, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        assertThat("Films are empty", filmsWithCount.films.size(), not(0));
        assertThat("Films count is not identical to the collection size", 0L+filmsWithCount.films.size(), equalTo(filmsWithCount.count));
    }

    @Test
    public void get_films_with_zanr() {
        Zanr zanr = zanrRepository.getZanrByName("акција");
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 23, Optional.of(zanr), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        for (Film film : filmsWithCount.films) {
            assertThat("Zanr is not correct", zanr.getIdzanr(), equalTo(film.getZanr().getIdzanr()));
        }
        assertThat("Size is not as expected!", filmsWithCount.films.size(), equalTo(23));
    }

    @Test
    public void get_films_with_zanr_no_limit() {
        Zanr zanr = zanrRepository.getZanrByName("акција");
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 0, Optional.of(zanr), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.<Tag>absent(), Optional.<String>absent(), Film_.medijListAsString, true);
        Assert.assertTrue("Films are empty", filmsWithCount.films.size() != 0);
        for (Film film : filmsWithCount.films) {
            assertThat("Zanr is not correct", zanr.getIdzanr(), equalTo(film.getZanr().getIdzanr()));
            Assert.assertEquals(zanr.getIdzanr(), film.getZanr().getIdzanr());
        }
        assertThat("Films count is not identical to the collection size!", 0L+filmsWithCount.films.size(), equalTo(filmsWithCount.count));
    }

    @Test
    public void get_films_with_tag() {
        Tag tag = tagRepository.getTagByName("неодгледан");
        FilmRepository.FilmsWithCount filmsWithCount = filmRepository.getFilmByCriteria(0, 23, Optional.<Zanr>absent(), Optional.<TipMedija>absent(), Optional.<Pozicija>absent(), Optional.of(tag), Optional.<String>absent(), Film_.medijListAsString, true);
        for (Film film : filmsWithCount.films) {
            film = filmRepository.getCompleteFilm(film);
            assertThat("Tag is not correct", film.getTags(), hasItem(tag));
        }
        assertThat("Size is not as expected!", filmsWithCount.films.size(), equalTo(23));
    }

}
