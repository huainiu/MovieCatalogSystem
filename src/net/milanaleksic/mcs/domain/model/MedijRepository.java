package net.milanaleksic.mcs.domain.model;

import java.util.List;

/**
 * User: Milan Aleksic
 * Date: 8/18/11
 * Time: 10:37 PM
 */
public interface MedijRepository {

    int getNextMedijIndeks(String s) ;

    void saveMedij(int index, TipMedija tipMedija);

    List<Medij> getMedijs();
}
