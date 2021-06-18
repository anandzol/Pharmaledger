package net.corda.pharmaledger.logistics;

import java.security.PublicKey;
import java.util.List;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.accountUtilities.NewKeyForAccount;
import net.corda.pharmaledger.medical.states.PatientAddressState;

@InitiatingFlow
@StartableByRPC
public class CreateShipment extends FlowLogic<String>{

    private String shipmentMappingID;
    private String status;
    private String fromLogistics;
    private String toPharma;

    public CreateShipment(String shipmentMappingID, String status, String fromLogistics, String toPharma) {
        this.shipmentMappingID = shipmentMappingID;
        this.status = status;
        this.fromLogistics = fromLogistics;
        this.toPharma = toPharma;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        List<StateAndRef<PatientAddressState>> patientAddressStateAndRefs = getServiceHub().getVaultService()
        .queryBy(PatientAddressState.class).getStates();

        StateAndRef<PatientAddressState> inputStateAndRef = patientAddressStateAndRefs.stream().filter(patientAddressStateAndRef -> {
            PatientAddressState patientAddressState = patientAddressStateAndRef.getState().getData();
            return patientAddressState.getShipmentMappingID().equals(shipmentMappingID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Patient Address Not Found"));

        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromLogistics).get(0).getState().getData();

        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toPharma).get(0).getState().getData();

        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));

        return null;
    }


    public String getShipmentMappingID() {
        return this.shipmentMappingID;
    }

    public void setShipmentMappingID(String shipmentMappingID) {
        this.shipmentMappingID = shipmentMappingID;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFromLogistics() {
        return this.fromLogistics;
    }

    public void setFromLogistics(String fromLogistics) {
        this.fromLogistics = fromLogistics;
    }

    public String getToPharma() {
        return this.toPharma;
    }

    public void setToPharma(String toPharma) {
        this.toPharma = toPharma;
    }
    
}
