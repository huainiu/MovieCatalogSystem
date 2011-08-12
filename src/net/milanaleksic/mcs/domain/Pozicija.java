package net.milanaleksic.mcs.domain;

import java.util.HashSet;
import java.util.Set;

public class Pozicija implements java.io.Serializable {

	private int idpozicija;
	private String pozicija;
	private Set<Medij> medijs = new HashSet<Medij>(0);

	public Pozicija() {
	}

	public int getIdpozicija() {
		return this.idpozicija;
	}

	private void setIdpozicija(int idpozicija) {
		this.idpozicija = idpozicija;
	}

	public String getPozicija() {
		return this.pozicija;
	}

	public void setPozicija(String pozicija) {
		this.pozicija = pozicija;
	}

	public Set<Medij> getMedijs() {
		return this.medijs;
	}

	public void setMedijs(Set<Medij> medijs) {
		this.medijs = medijs;
	}
	
	public String toString() {
		return getPozicija();
	}
	
	public void addMedij(Medij m) {
		medijs.add(m);
		m.setPozicija(this);
	}


}