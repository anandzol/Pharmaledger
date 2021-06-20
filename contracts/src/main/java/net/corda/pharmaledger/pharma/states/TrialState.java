package net.corda.pharmaledger.pharma.states;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.pharma.contracts.TrialStateContract;

@BelongsToContract(TrialStateContract.class)
public class TrialState implements ContractState{
    private int trialID;
    private String trialPatients; 
    private String trialTemplateID;
    private String status;
    private Date startDate;
    private Date endDate;
    private AnonymousParty fromPharma;
    private AnonymousParty toMedical;
    private List<AbstractParty> participants;


    public TrialState(int trialID, String trialPatients, String trialTemplateID, String status, Date startDate, Date endDate, AnonymousParty fromPharma, AnonymousParty toMedical) {
        this.trialID = trialID;
        this.trialPatients = trialPatients;
        this.trialTemplateID = trialTemplateID;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fromPharma = fromPharma;
        this.toMedical = toMedical;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(fromPharma);
        participants.add(toMedical);
    }
    

    public int getTrialID() {
        return this.trialID;
    }

    public void setTrialID(int trialID) {
        this.trialID = trialID;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrialPatients() {
        return this.trialPatients;
    }

    public void setTrialPatients(String trialPatients) {
        this.trialPatients = trialPatients;
    }

    public String getTrialTemplateID() {
        return this.trialTemplateID;
    }

    public void setTrialTemplateID(String trialTemplateID) {
        this.trialTemplateID = trialTemplateID;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public AnonymousParty getFromPharma() {
        return this.fromPharma;
    }

    public void setFromPharma(AnonymousParty fromPharma) {
        this.fromPharma = fromPharma;
    }

    public AnonymousParty getToMedical() {
        return this.toMedical;
    }

    public void setToMedical(AnonymousParty toMedical) {
        this.toMedical = toMedical;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
}
