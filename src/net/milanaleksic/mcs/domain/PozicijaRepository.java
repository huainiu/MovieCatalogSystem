package net.milanaleksic.mcs.domain;

import java.util.List;

/**
 * User: Milan Aleksic
 * Date: 8/18/11
 * Time: 10:37 PM
 */
public interface PozicijaRepository {

    List<Pozicija> getPozicijas();

    void addPozicija(String newPozicija);
}
