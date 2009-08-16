package net.milanaleksic.mcs.db;

// Generated 07.11.2007. 23.32.08 by Hibernate Tools 3.2.0.b9

import java.util.HashSet;
import java.util.Set;

/**
 * TipMedija generated by hbm2java
 */
public class TipMedija implements java.io.Serializable {

	private int idtip;
	private String naziv;
	private Set<Medij> medijs = new HashSet<Medij>(0);

	public TipMedija() {
	}

	public TipMedija(int idtip, String naziv) {
		this.idtip = idtip;
		this.naziv = naziv;
	}

	public TipMedija(int idtip, String naziv, Set<Medij> medijs) {
		this.idtip = idtip;
		this.naziv = naziv;
		this.medijs = medijs;
	}

	public int getIdtip() {
		return this.idtip;
	}

	public void setIdtip(int idtip) {
		this.idtip = idtip;
	}

	public String getNaziv() {
		return this.naziv;
	}

	public void setNaziv(String naziv) {
		this.naziv = naziv;
	}

	public Set<Medij> getMedijs() {
		return this.medijs;
	}

	public void setMedijs(Set<Medij> medijs) {
		this.medijs = medijs;
	}
	
	public String toString() {
		return getNaziv();
	}
	
	public void addMedij(Medij m) {
		medijs.add(m);
		m.setTipMedija(this);
	}


}
