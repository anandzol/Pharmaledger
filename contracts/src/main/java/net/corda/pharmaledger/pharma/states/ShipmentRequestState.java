package net.corda.pharmaledger.pharma.states;

import java.util.ArrayList;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.pharma.contracts.ShipmentRequestStateContract;

@BelongsToContract(ShipmentRequestStateContract.class)
public class ShipmentRequestState implements ContractState {
    private String shipmentMappingID;
    private String packageID;
    private AnonymousParty fromPharma;
    private AnonymousParty toLogistics;
    private List<AbstractParty> participants;

    public ShipmentRequestState(String shipmentMappingID, String packageID, AnonymousParty fromPharma, AnonymousParty toLogistics) {
        this.shipmentMappingID = shipmentMappingID;
        this.packageID = packageID;
        this.fromPharma = fromPharma;
        this.toLogistics = toLogistics;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(fromPharma);
        this.participants.add(toLogistics);
    }

    public String getShipmentMappingID() {
        return this.shipmentMappingID;
    }

    public void setShipmentMappingID(String shipmentMappingID) {
        this.shipmentMappingID = shipmentMappingID;
    }

    public String getPackageID() {
        return this.packageID;
    }

    public void setPackageID(String packageID) {
        this.packageID = packageID;
    }

    public AnonymousParty getFromPharma() {
        return this.fromPharma;
    }

    public void setFromPharma(AnonymousParty fromPharma) {
        this.fromPharma = fromPharma;
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
