package net.corda.pharmaledger.pharma.states;

import java.util.ArrayList;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.pharma.contracts.KitShipmentStateContract;


@BelongsToContract(KitShipmentStateContract.class)
public class KitShipmentState implements ContractState{
    private String packageID;
    private String shipmentMappingID;
    private String kitID;
    private AnonymousParty fromPharma;
    private AnonymousParty toMedical;
    private List<AbstractParty> participants;
    

    public KitShipmentState(String packageID, String shipmentMappingID, String kitID, AnonymousParty fromPharma, AnonymousParty toMedical) {
        this.packageID = packageID;
        this.shipmentMappingID = shipmentMappingID;
        this.kitID = kitID;
        this.fromPharma=fromPharma;
        this.toMedical = toMedical;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(fromPharma);
        participants.add(toMedical);
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

    public void setParticipants(List<AbstractParty> participants) {
        this.participants = participants;
    }

    public String getPackageID() {
        return this.packageID;
    }

    public void setPackageID(String packageID) {
        this.packageID = packageID;
    }

    public String getShipmentMappingID() {
        return this.shipmentMappingID;
    }

    public void setShipmentMappingID(String shipmentMappingID) {
        this.shipmentMappingID = shipmentMappingID;
    }

    public String getKitID() {
        return this.kitID;
    }

    public void setKitID(String kitID) {
        this.kitID = kitID;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

}