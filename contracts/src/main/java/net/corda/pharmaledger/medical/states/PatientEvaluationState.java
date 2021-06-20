package net.corda.pharmaledger.medical.states;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.medical.contracts.PatientEvaluationStateContract;

@BelongsToContract(PatientEvaluationStateContract.class)
public class PatientEvaluationState implements ContractState {
    private int patientID;
    private String symptoms;
    private Date evaluationDate;
    private String  evaluationResult;
    private AnonymousParty fromMedical;
    private AnonymousParty toPharma;
    private List<AbstractParty> participants;

    public PatientEvaluationState(int patientID, String symptoms, Date evaluationDate, String evaluationResult, AnonymousParty fromMedical, AnonymousParty toPharma) {
        this.patientID = patientID;
        this.symptoms = symptoms;
        this.evaluationDate = evaluationDate;
        this.evaluationResult = evaluationResult;
        this.fromMedical = fromMedical;
        this.toPharma = toPharma;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(fromMedical);
        participants.add(toPharma);
    }

    public int getPatientID() {
        return this.patientID;
    }

    public void setPatientID(int patientID) {
        this.patientID = patientID;
    }

    public String getSymptoms() {
        return this.symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public Date getEvaluationDate() {
        return this.evaluationDate;
    }

    public void setEvaluationDate(Date evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public String getEvaluationResult() {
        return this.evaluationResult;
    }

    public void setEvaluationResult(String evaluationResult) {
        this.evaluationResult = evaluationResult;
    }

    public AnonymousParty getFromMedical() {
        return this.fromMedical;
    }

    public void setFromMedical(AnonymousParty fromMedical) {
        this.fromMedical = fromMedical;
    }

    public AnonymousParty getToPharma() {
        return this.toPharma;
    }

    public void setToPharma(AnonymousParty toPharma) {
        this.toPharma = toPharma;
    }
    public void setParticipants(List<AbstractParty> participants) {
        this.participants = participants;
    }




    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
    

}
