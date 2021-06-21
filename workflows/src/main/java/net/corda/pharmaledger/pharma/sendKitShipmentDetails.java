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
import net.corda.pharmaledger.pharma.contracts.KitShipmentStateContract;
import net.corda.pharmaledger.pharma.states.KitShipmentState;
import net.corda.pharmaledger.pharma.states.ShipmentRequestState;
@InitiatingFlow
@StartableByRPC
public class sendKitShipmentDetails  extends FlowLogic<String>{

    private String packageID;
    private String shipmentMappingID;
    private String kitID;
    private String fromPharma;
    private String toMedical;

    public sendKitShipmentDetails(String packageID, String shipmentMappingID, String kitID, String fromPharma, String toMedical) {
        this.packageID = packageID;
        this.shipmentMappingID = shipmentMappingID;
        this.kitID = kitID;
        this.fromPharma = fromPharma;
        this.toMedical = toMedical;
    }
    
    @Suspendable
    @Override
    public String call() throws FlowException {

        List<StateAndRef<ShipmentRequestState>> shipmentRequestStateAndRefs = getServiceHub().getVaultService()
        .queryBy(ShipmentRequestState.class).getStates();

        StateAndRef<ShipmentRequestState> inputStateAndRef = shipmentRequestStateAndRefs.stream().filter(shipmentRequestStateAndRef -> {
            ShipmentRequestState shipmentRequeststate = shipmentRequestStateAndRef.getState().getData();
            return shipmentRequeststate.getPackageID().equals(packageID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Package ID Not Found"));

        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromPharma).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toMedical).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));
        KitShipmentState kitShipment = new KitShipmentState(packageID, shipmentMappingID, kitID, new AnonymousParty(myKey), targetAcctAnonymousParty);
        
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addOutputState(kitShipment)
                .addCommand(new KitShipmentStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);
        
        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));


        return "Successfully sent package details for kit with ID: " + kitID;
    }
    
}

@InitiatedBy(sendKitShipmentDetails.class)
class SendKitShipmentDetailsResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public SendKitShipmentDetailsResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession));
        return null;
    }
}

