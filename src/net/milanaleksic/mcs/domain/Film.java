package net.milanaleksic.mcs.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class Film implements Serializable, Comparable<Film> {

	private int idfilm;
	private Zanr zanr;
	private String nazivfilma;
	private String prevodnazivafilma;
	private int godina;
	private String komentar;
	private BigDecimal imdbrejting;
	private Set<Medij> medijs = new HashSet<Medij>(0);

	public Film() {
	}

	public int getIdfilm() {
		return this.idfilm;
	}

	private void setIdfilm(int idfilm) {
		this.idfilm = idfilm;
	}

	public Zanr getZanr() {
		return this.zanr;
	}

	public void setZanr(Zanr zanr) {
		this.zanr = zanr;
	}

	public String getNazivfilma() {
		return this.nazivfilma;
	}

	public void setNazivfilma(String nazivfilma) {
		this.nazivfilma = nazivfilma;
	}

	public String getPrevodnazivafilma() {
		return this.prevodnazivafilma;
	}

	public void setPrevodnazivafilma(String prevodnazivafilma) {
		this.prevodnazivafilma = prevodnazivafilma;
	}

	public int getGodina() {
		return this.godina;
	}

	public void setGodina(int godina) {
		this.godina = godina;
	}

	public String getKomentar() {
		return this.komentar;
	}

	public void setKomentar(String komentar) {
		this.komentar = komentar;
	}

	public BigDecimal getImdbrejting() {
		return this.imdbrejting;
	}

	public void setImdbrejting(BigDecimal imdbrejting) {
		this.imdbrejting = imdbrejting;
	}

	public Set<Medij> getMedijs() {
		return this.medijs;
	}

	public void setMedijs(Set<Medij> medijs) {
		this.medijs = medijs;
	}
	
	public String toString() {
		return getNazivfilma();
	}
	
	public void addMedij(Medij m) {
		medijs.add(m);
		m.getFilms().add(this);
	}
	
	public String getFilmLocation() {
		// priprema informacija za narednu obradu (polje "prisutan")
		int brojNeprisutnih = 0;
		for (Medij medij : getMedijs()) {
			if (!medij.getPozicija().getPozicija().equals("присутан"))
				brojNeprisutnih++;
		}
		
		// obrada polja "prisutan"
		String prisutan;
		if (brojNeprisutnih==0)
			prisutan = "присутан";
		else {
            StringBuilder builder = new StringBuilder();
			for (Medij medij : getMedijs()) {
                builder.append(medij.toString()).append("-").append(medij.getPozicija().toString()).append("; ");
			}
			prisutan = builder.substring(0, builder.length()-2);
		}						
		for (Medij medij : getMedijs()) {
			if (!medij.getPozicija().getPozicija().equals("присутан"))
				prisutan = medij.getPozicija().toString();
		}
		return prisutan;
	}

    public String getMedijListAsString() {
        StringBuilder medijInfo = new StringBuilder();
        Object[] mediji = getMedijs().toArray();
        Arrays.sort(mediji);
        for (Object medij : mediji)
            medijInfo.append(medij.toString()).append(' ');
        return medijInfo.toString();
    }

    @Override
    public int compareTo(Film o) {
		return getMedijListAsString().compareTo(o.getMedijListAsString());
    }
}