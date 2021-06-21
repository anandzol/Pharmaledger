package net.corda.pharmaledger.pharma;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.CollectSignatureFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.pharmaledger.accountUtilities.NewKeyForAccount;
import net.corda.pharmaledger.medical.states.PatientAddressState;
import net.corda.pharmaledger.medical.states.PatientState;
import net.corda.pharmaledger.pharma.contracts.ShipmentRequestStateContract;
import net.corda.pharmaledger.pharma.states.ShipmentRequestState;

@InitiatingFlow
@StartableByRPC
public class SendShipmentRequest extends FlowLogic<String> {
    private String shipmentMappingID;
    private String packageID;
    private String fromPharma;
    private String toLogistics;


    public SendShipmentRequest(String shipmentMappingID, String packageID, String fromPharma, String toLogistics) {
        this.shipmentMappingID = shipmentMappingID;
        this.packageID = packageID;
        this.fromPharma = fromPharma;
        this.toLogistics = toLogistics;
    }


    @Override
    @Suspendable
    public String call() throws FlowException {

        List<StateAndRef<PatientState>> patientStateAndRefs = getServiceHub().getVaultService()
        .queryBy(PatientState.class).getStates();

        StateAndRef<PatientState> inputStateAndRef = patientStateAndRefs.stream().filter(patientStateAndRef -> {
            PatientState patientstate = patientStateAndRef.getState().getData();
            return patientstate.getShipmentMappingID().equals(shipmentMappingID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Shipment MappingID Not Found"));

        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromPharma).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toLogistics).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));

        ShipmentRequestState shipmentData = new ShipmentRequestState(shipmentMappingID, packageID, new AnonymousParty(myKey), targetAcctAnonymousParty);

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addOutputState(shipmentData)
                .addCommand(new ShipmentRequestStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);

        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));

        
        return "Sent Shipment Request for Mapping ID: " + shipmentMappingID;
    }
    
}

@InitiatedBy(SendShipmentRequest.class)
class SendShipmentRequestResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public SendShipmentRequestResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                ContractState output = stx.getTx().getOutputs().get(0).getData();
                ShipmentRequestState shipment = (ShipmentRequestState) output;
                List<StateAndRef<PatientAddressState>> patientAddressStateAndRefs = getServiceHub().getVaultService()
                .queryBy(PatientAddressState.class).getStates();

                StateAndRef<PatientAddressState> inputStateAndRef = patientAddressStateAndRefs.stream().filter(patientAddressStateAndRef -> {
                PatientAddressState patientAddressState = patientAddressStateAndRef.getState().getData();
                return patientAddressState.getShipmentMappingID().equals(shipment.getShipmentMappingID());
                }).findAny().orElseThrow(() -> new IllegalArgumentException("Patient Address Not Found"));
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession));
        return null;
    }
}