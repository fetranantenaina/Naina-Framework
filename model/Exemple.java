package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;


public class Exemple {
	int id;
	int entier;
	double doubleenina;
	double reel;
	String chaine;
	String texte;
	boolean booleen;
	LocalDate date_col;
	LocalDateTime timestamp_col;
	Duration intervalle;


	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getEntiEr() {
		return this.entier;
	}

	public void setEntiEr(int entier) {
		this.entier = entier;
	}

	public double getDoubleenina() {
		return this.doubleenina;
	}

	public void setDoubleenina(double doubleenina) {
		this.doubleenina = doubleenina;
	}

	public double getReel() {
		return this.reel;
	}

	public void setReel(double reel) {
		this.reel = reel;
	}

	public String getChaine() {
		return this.chaine;
	}

	public void setChaine(String chaine) {
		this.chaine = chaine;
	}

	public String getTexTe() {
		return this.texte;
	}

	public void setTexTe(String texte) {
		this.texte = texte;
	}

	public boolean getBooleen() {
		return this.booleen;
	}

	public void setBooleen(boolean booleen) {
		this.booleen = booleen;
	}

	public LocalDate getDate_col() {
		return this.date_col;
	}

	public void setDate_col(LocalDate date_col) {
		this.date_col = date_col;
	}

	public LocalDateTime getTimesTamp_col() {
		return this.timestamp_col;
	}

	public void setTimesTamp_col(LocalDateTime timestamp_col) {
		this.timestamp_col = timestamp_col;
	}

	public Duration getIntervalle() {
		return this.intervalle;
	}

	public void setIntervalle(Duration intervalle) {
		this.intervalle = intervalle;
	}


    
}
