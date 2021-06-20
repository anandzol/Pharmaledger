package net.corda.pharmaledger.pharma;

import java.security.PublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
import net.corda.pharmaledger.medical.states.PatientState;
import net.corda.pharmaledger.pharma.contracts.TrialStateContract;
import net.corda.pharmaledger.pharma.states.TrialState;
import net.corda.pharmaledger.pharma.states.TrialTemplateState;

@InitiatingFlow
@StartableByRPC
public class SendTrial extends FlowLogic<String> {
    private int trialID;
    private String trialPatients;
    private String trialTemplateID;
    private String trialStatus;
    private String trialStartDate;
    private String trialEndDate;
    private String fromPharma;
    private String toMedical;


    public SendTrial(int trialID, String trialPatients, String trialTemplateID, String trialStatus, String trialStartDate, String trialEndDate, String fromPharma, String toMedical) {
        this.trialID = trialID;
        this.trialPatients = trialPatients;
        this.trialTemplateID = trialTemplateID;
        this.trialStatus = trialStatus;
        this.trialStartDate = trialStartDate;
        this.trialEndDate = trialEndDate;
        this.fromPharma = fromPharma;
        this.toMedical = toMedical;
    }
    

    @Override
    @Suspendable
    public String call() throws FlowException {
        List<StateAndRef<TrialTemplateState>> trialTemplateStateAndRefs = getServiceHub().getVaultService()
        .queryBy(TrialTemplateState.class).getStates();

        StateAndRef<TrialTemplateState> inputStateAndRef = trialTemplateStateAndRefs.stream().filter(trialTemplateStateAndRef -> {
            TrialTemplateState trialTemplatestate = trialTemplateStateAndRef.getState().getData();
            return trialTemplatestate.getTrialTemplateID().equals(trialTemplateID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Trial Template Not Found"));

        try {
            String[] patients = trialPatients.split(",");
            for (int counter =0; counter < patients.length; counter++) {
                int patientID = Integer.parseInt(patients[counter]);

                List<StateAndRef<PatientState>> patientStateAndRefs = getServiceHub().getVaultService()
                .queryBy(PatientState.class).getStates();
                StateAndRef<PatientState> inputPatientStateAndRef = patientStateAndRefs.stream().filter(patientStateAndRef -> {
                    PatientState patientState = patientStateAndRef.getState().getData();
                    return patientState.getPatientID() == patientID;
                }).findAny().orElseThrow(() -> new IllegalArgumentException("Patient data Not Found"));
        }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while checking patient presence: " + e);
        }

        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromPharma).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toMedical).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));

        Date startDateFormat = new Date();
        Date endDateFormat = new Date();
        try {
            startDateFormat = new SimpleDateFormat("dd/MM/yyyy").parse(trialStartDate);
            endDateFormat = new SimpleDateFormat("dd/MM/yyyy").parse(trialEndDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TrialState trial = new TrialState(trialID, trialPatients, trialTemplateID, trialStatus, startDateFormat, endDateFormat, new AnonymousParty(myKey), targetAcctAnonymousParty);
        
         final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addOutputState(trial)
                .addCommand(new TrialStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);
        
        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));

        return "Successfully sent trial data with ID: " + trialID;
    }
    
}

@InitiatedBy(SendTrial.class)
class SendTrialResponder extends FlowLogic<Void>{

    private FlowSession counterpartySession;

    public SendTrialResponder(FlowSession counterpartySession) {
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