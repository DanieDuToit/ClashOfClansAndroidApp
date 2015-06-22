package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Application;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

public class GlobalState extends Application {

	private Integer ourParticipantID;
	private Integer warID;
	private Integer rank;
	private Integer theirRank;
	private String warName;
	private String gameName;
	private Integer numberOfParticipants = 0;

	public Integer getOurParticipantID() {
		return ourParticipantID;
	}
	public void setOurParticipantID(Integer participantID) {
		this.ourParticipantID = participantID;
	}

	public Integer getNumberOfParticipants() {
		return numberOfParticipants;
	}
	public void setNumberOfParticipants(Integer numberOfParticipants) {
		this.numberOfParticipants = numberOfParticipants;
	}

	public Integer getRank() {
		return rank;
	}
	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public Integer getTheirRank() {
		return theirRank;
	}
	public void setTheirRank(Integer theirRank) {
		this.theirRank = theirRank;
	}

	public Integer getWarID() {
		return warID;
	}
	public void setWarID(Integer warid) {
		this.warID = warid;
	}

	public String getGameName() {
		return gameName;
	}
	public void setGameName(String gamename) {
		this.gameName = gamename;
	}

	public String getWarName() {
		return warName;
	}
	public void setWarName(String warname) {
		this.warName = warname;
	}

	public String getInternetURL() {
		return "http://daniedutoit.no-ip.biz/ClashOfClans/";
	}
}
