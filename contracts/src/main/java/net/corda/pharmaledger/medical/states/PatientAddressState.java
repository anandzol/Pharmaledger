package net.corda.pharmaledger.medical.states;

import java.util.ArrayList;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.medical.contracts.PatientAddressStateContract;

@BelongsToContract(PatientAddressStateContract.class)
public class PatientAddressState implements ContractState {
    private String shipmentMappingID;
    private String patientAddress;
    private String patientMailID;
    private AnonymousParty fromMedical;
    private AnonymousParty toLogistics;
    private List<AbstractParty> participants;


    public PatientAddressState(String shipmentMappingID, String patientAddress, String patientMailID, AnonymousParty fromMedical, AnonymousParty toLogistics) {
        this.shipmentMappingID = shipmentMappingID;
        this.patientAddress = patientAddress;
        this.patientMailID = patientMailID;
        this.fromMedical = fromMedical;
        this.toLogistics = toLogistics;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(fromMedical);
        this.participants.add(toLogistics);
    }

    public String getShipmentMappingID() {
        return this.shipmentMappingID;
    }

    public void setShipmentMappingID(String shipmentMappingID) {
        this.shipmentMappingID = shipmentMappingID;
    }

    public String getPatientAddress() {
        return this.patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public String getPatientMailID() {
        return this.patientMailID;
    }

    public void setPatientMailID(String patientMailID) {
        this.patientMailID = patientMailID;
    }

    public AnonymousParty getFromMedical() {
        return this.fromMedical;
    }

    public void setFromMedical(AnonymousParty fromMedical) {
        this.fromMedical = fromMedical;
    }

    public AnonymousParty getToLogistics() {
        return this.toLogistics;
    }

    public void setToLogistics(AnonymousParty toLogistics) {
        this.toLogistics = toLogistics;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
    
}
