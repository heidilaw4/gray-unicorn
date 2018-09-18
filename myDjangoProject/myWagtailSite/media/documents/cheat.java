public abstract class FlowStep_SendEmail extends FlowStep {

    @testVisible
    private abstract String getEmailTemplate();

    private virtual List<String> getBccAddress() {
        return new List<String>();
    }

    private virtual String getSubject() {
        return null;
    }

    @testVisible
    private virtual Util_Email.OrgWideEmail getFromEmailAddress() {
        return Util_Email.KLANTENSERVICE_ADDRESS;
    }

    private String getRelatedObjectId() {
        return input.customData.get(FlowStepInput.PARAM_ACTIVITY_CASE_ID);
    }

    public override Boolean requiresPersistedStateBeforeExecution() {
        return true;
    }

    @testVisible
    private virtual String getStepMessage() {
        return 'The confirmation email has been sent';
    }

    public virtual override FlowStepResult execute() {
        Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), getFromEmailAddress(), getSubject());
        return new FlowStepResult(FlowStep.Outcome.SUCCESS, getStepMessage());
    }

    // Should Send to Producer
    public class ChangePaymentDetails extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send change payment details confirmation email';
        }
        private override String getEmailTemplate() {
            return 'ChangePaymentDetailsEmailTemplate';
        }
        private Boolean shouldForwardEmailToBilling() {
            return input.customData.get(Flow_ChangePaymentDetails.OLD_BANK_ACCOUNT_NUMBER) != input.state.debtor.BankAccountNumber__c;
        }
        private override List<String> getBccAddress() {
            List<String> bccAddresses = new List<String>();
            if (shouldForwardEmailToBilling()) {
                if (Configuration_Email.EMAIL_BCC_ADDRESSES_FOR_BILLLING_OFFICE.size() == 0)
                    throw new InconsistentDataException('Can\'t notify billing office because no email addresses found for them, check the custom settings');
                bccAddresses.addAll(Configuration_Email.EMAIL_BCC_ADDRESSES_FOR_BILLLING_OFFICE);
            }
            return bccAddresses;
        }

        public override FlowStepResult execute() {
            Util_Email.sendToWithAdditionalBCC(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), getBccAddress());
            return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
        }
    }

    public class ChangeCustomerDetailsOldAddress extends FlowStep_SendEmail {

        // at this stage the account email address is already changed to the new one,
        // but the contact email address is still the old one.
        public override String getDisplayName() {
            return 'Send change customer details confirmation email to old address';
        }
        @testVisible
        private override String getEmailTemplate() {
            return 'ChangeCustomerDetailsEmailForOldAddress';
        }
        @testVisible
        private override String getStepMessage() {
            return 'The change confirmation has been sent to the old address: ' + input.state.primaryContact.Email;
        }
        public override Verdict shouldExecute() {
            if (input.state.account.Email__c == input.state.primaryContact.Email) {
                return new Verdict(false, 'the email has not been changed.');
            }
            return new Verdict(true);
        }
    }

    public class ChangeCustomerDetails extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send change customer details confirmation email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'ChangeCustomerDetailsEmailTemplate';
        }

        @testVisible
        private override String getStepMessage() {
            return 'The change confirmation has been sent to ' + input.state.primaryContact.Email;
        }
    }

    public class ChangeRateTariff extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send change rate tariff confirmation email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'ChangeRateTariffEmailTemplate';
        }

        public override FlowStepResult execute() {
            String connectionId = input.customData.get(Flow_ChangeRateTariff.PARAM_CONNECTION_ECEDO_ID);
            Connection__c connection = (Connection__c) Util_Collection.find(input.state.connections, 'EcedoId__c', connectionId);
            Contract__c contract = Flow_ChangeRateTariff.getContractForConnection(input.state, connection);
            MeeliftContract__c meeliftContract = Util_FlowState.getMeeliftContractForContract(input.state.meeliftContracts, contract);
            List<ContractPricePeriod__c> cpps = Util_ContractPricePeriods.getCPPsByContract(input.state.account.EcedoId__c, contract, meeliftContract);

            Util_Tariff.GovernmentCharges charges = Util_Tariff.getGovernmentChargesFromElectricityContract(contract, new EcedoService());
            Decimal customerPremium = Util_ContractPricePeriods.getCustomerPremium(cpps);

            List<ViewModel_TariffElectricityLevering> tariffs = new List<ViewModel_TariffElectricityLevering>{
                    new ViewModel_TariffElectricityLevering(Util_ContractPricePeriods.getCppByTariffName(cpps, 'Hoog'), charges, customerPremium),
                    new ViewModel_TariffElectricityLevering(Util_ContractPricePeriods.getCppByTariffName(cpps, 'Laag'), charges, customerPremium),
                    new ViewModel_TariffElectricityLevering(Util_ContractPricePeriods.getCppByTariffName(cpps, 'Enkel'), charges, customerPremium)
            };

            String postBody = JSON.serializePretty(tariffs);
            Util_Attachment.insertJSONAttachmentUnderParent(
                    Util_EventHandler.INBOUND_POST_BODY_TAG,
                    postBody,
                    input.customData.get(FlowStepInput.PARAM_ACTIVITY_CASE_ID)
            );

            Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), Util_Email.NOREPLY_ADDRESS);
            return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
        }
    }

    public class FarewellEmail extends FlowStep_SendEmail {
        public override Verdict shouldExecute() {
            Boolean shouldExecute = input.customData.find(FlowStepInput.PARAM_SHOULD_SEND_EMAIL) != null;
            if (!shouldExecute)
                return new Verdict(false, 'No actions have been done in the flow');
            return new Verdict(true);
        }
        public override String getDisplayName() {
            return 'Send farewell email.';
        }
        @testVisible
        private override String getEmailTemplate() {
            // what about add shipping address customer for referral ?
            SignupRequest__c signupRequest = (SignupRequest__c) Util_Database.getSObjectById(input.state.account.SignupRequest__c);
            if (Util_Signup.isReferral(signupRequest)) {
                return 'FarewellEmailReferral';
            } else {
                return 'FarewellEmail';
            }
        }
        @testVisible
        private override String getStepMessage() {
            return 'Farewell email sent';
        }
    }

    public class SendPeriodicCost extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send periodic cost email';
        }

        public override Verdict shouldExecute() {
            if (input.state.account.IsProducer__c == TRUE) {
                return new Verdict(false, 'This Email should not be sent to Producers.');
            }
            return new Verdict(true);
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'PeriodicCostEmail';
        }

        @testVisible
        private override Util_Email.OrgWideEmail getFromEmailAddress() {
            return Util_Email.NOREPLY_ADDRESS;
        }
    }

    public class MoveCluster extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send move email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'MoveClusterEmail';
        }

        public override FlowStepResult execute() {
            if (Flow_MoveCluster.isAddingNewConnections(input.customData)) {
                Contract__c theContract = Util_Contract.getSelectedContract(input);
                List<Attachment> attachments = FlowStep_Contract_SendContractEmail.getAttachmentsForContract(theContract, input);
                Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), attachments, getFromEmailAddress());
            } else {
                Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), getFromEmailAddress());
            }

            return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
        }
    }

    public class CustomerCESSurvey extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send CES Survey email to Customer.';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'SendCESSurveyEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'CES Survey email sent';
        }
    }

    public class CustomerExitSurvey extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send Exit Survey email to Customer.';
        }

        public override Verdict shouldExecute() {
            if (input.state.account.Email__c == input.state.primaryContact.Email) {
                return new Verdict(false, 'The email address has not been changed.');
            }
            return new Verdict(true);
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'ExitSurveyEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'Exit Survey email sent';
        }
    }

    public class CompensationDiscount extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send compensation discount email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'CompensationDiscountEmail';
        }

        public override FlowStepResult execute() {
            return getTermsAndConditions(input);
        }

    }

    public class RequestChargeCards extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send welcome or confirmation email';
        }

        private override String getEmailTemplate() {
            Boolean alreadyMSPCustomer = Util_String.safeToBoolean(input.customData.get(Flow_RequestChargeCards.PARAM_ALREADY_MSP_CUSTOMER));
            return alreadyMSPCustomer ? 'MSPExtraCardsRequestedEmail' : 'MSPWelcomeEmail';
        }

        public override FlowStepResult execute() {
            List<Attachment> attachments = new List<Attachment>();

            Attachment contractAttachment = (Attachment) Util_Database.getSObjectById(input.customData.get(FlowStepInput.PARAM_CONTRACT_ATTACHMENT_ID));

            attachments.add(contractAttachment);

            attachments.add(Util_Attachment.staticResourceToAttachment(Util_Database.getStaticResource('Herroepings_formulier_MSP').Id, 'Herroepings_formulier.pdf'));
            attachments.add(Util_Attachment.staticResourceToAttachment(Util_Database.getStaticResource('TermsAndConditions_MSP').Id, 'Algemene_voorwaarden_laadpas.pdf'));

            Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), attachments, getFromEmailAddress());
            return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
        }
    }

    public class ActivateMSPCard extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send charge card activation confirmation email';
        }


        @testVisible
        private override String getEmailTemplate() {
            return 'MSPCardConfirmActivationEmail';
        }
    }

    public class SendGenericEmail extends FlowStep_SendEmail {
        public override String getDisplayName() {
            if (Test.isRunningTest()) {
                return 'Generic send email';
            }
            return 'Send email ' + input.customData.get(FlowStepInput.PARAM_TEMPLATE_NAME);
        }

        @testVisible
        private override String getEmailTemplate() {
            return input.customData.get(FlowStepInput.PARAM_TEMPLATE_NAME);
        }
    }

    public class LoginEmail extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send login email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'LoginEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'Login email sent';
        }

        @testVisible
        private override Util_Email.OrgWideEmail getFromEmailAddress() {
            return Util_Email.NOREPLY_ADDRESS;
        }

        public override Verdict shouldExecute() {
            SignupRequest__c signup = (SignupRequest__c) Util_Database.getSObjectById(
                    input.customData.get(FlowStepInput.SIGNUP_REQUEST_ID)
            );
            Boolean isAddShippingAddress = signup.Context__c == Util_Signup.SignupContext.addShippingAddress.name();

            return ( isAddShippingAddress) ?
                    new Verdict(false, 'Login email not necessary') : new Verdict(true);
        }
    }

    public class EVReplaceCard extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send EV Flow Block / Replace card email';
        }


        @TestVisible
        private override String getEmailTemplate() {
            return 'EV_Block_ReplaceCard';
        }

        @TestVisible
        private override String getSubject() {
            Boolean originalCardIsCard = Util_String.safeToBoolean(input.customData.get(Flow_EVReplaceCard.PARAM_SELECTED_CARD_TYPE_IS_CARD));
            Boolean newObjectIsCard = Util_String.safeToBoolean(input.customData.get(Flow_EVReplaceCard.PARAM_NEW_CARD_TYPE_IS_CARD));
            Boolean createNewCard = Util_String.safeToBoolean(input.customData.get(Flow_EVReplaceCard.PARAM_SHOULD_CREATE_NEW_CARD));

            String originalObject = originalCardIsCard ? Label.EVCard_Card : Label.EVCard_Keychain;
            String lastPart = '';
            if (createNewCard) {
                String newObject = newObjectIsCard ? Label.EVCard_Card : Label.EVCard_Keychain;
                lastPart = ' ' + String.format(Label.EVReplaceCardEmailSubject_Replace, new List<String>{
                        newObject
                });
            }

            return String.format(Label.EVReplaceCardEmailSubject_Main, new List<String>{
                    originalObject, lastPart
            });
        }

        @testVisible
        private override String getStepMessage() {
            return 'EV Replace card email sent';
        }
    }

    public class SendEVInvoice extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send EV invoice email';
        }


        @testVisible
        private override String getEmailTemplate() {
            return 'EVInvoiceEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'EV invoice email sent';
        }

        public override FlowStepResult execute() {
            List<Attachment> attachments = new List<Attachment>();
            Attachment invoiceAttachment = (Attachment) Util_Database.getSObjectById(input.customData.get(Flow_SendEVInvoice.PARAM_INVOICE_ATTACHMENT_ID));
            attachments.add(invoiceAttachment);

            Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), attachments, getFromEmailAddress());
            return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
        }
    }

    public class SendStartSupply extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send the start supply email.';
        }


        @testVisible
        private override String getEmailTemplate() {
            return 'StartSupplyEmail';
        }

        public override Verdict shouldExecute() {
            if (input.state.account.IsProducer__c == TRUE) {
                return new Verdict(false, 'This Email should not be sent to Producers.');
            }
            return new Verdict(true);
        }

        @testVisible
        private override String getStepMessage() {
            return 'Start supply email sent';
        }
    }

    public class Producer extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send the producer email';
        }

        public override Verdict shouldExecute() {
            if (input.state.account.IsProducer__c == TRUE) {
                return new Verdict(false, 'This Email should not be sent to Producers.');
            }
            return new Verdict(true);
        }


        @testVisible
        private override String getEmailTemplate() {
            return 'ProducerEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'Producer Email sent';
        }
    }

    public class TooYoungEmail extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send the too young email';
        }


        @testVisible
        private override String getEmailTemplate() {
            return 'TooYoung';
        }

        @testVisible
        private override String getStepMessage() {
            return 'Too Young Email sent';
        }
    }

    public class ReferralDiscount extends FlowStep_SendEmail {

        private override Util_Email.OrgWideEmail getFromEmailAddress() {
            return Util_Email.NOREPLY_ADDRESS;
        }

        public override String getDisplayName() {
            return 'Send the referral discount email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'ReferralDiscountEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'Referral discount Email sent';
        }

        public override FlowStepResult execute() {
            return getTermsAndConditions(input);
        }
    }

    public class MoveOut extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send the MoveOut email';
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'MoveOutEmail';
        }

        @testVisible
        private override String getStepMessage() {
            return 'Moveout Email sent';
        }

        @testVisible
        private override Util_Email.OrgWideEmail getFromEmailAddress() {
            Case activityCase = (Case) Util_Database.getSObjectById(input.customData.get(FlowStepInput.PARAM_ACTIVITY_CASE_ID));
            if (activityCase.Reason__c == Util_Case.REASON_DECEASED) {
                return Util_Email.SPECIALCARE_ADDRESS;
            }
            return Util_Email.NOREPLY_ADDRESS;
        }

        @TestVisible
        private override String getSubject() {
            Case activityCase = (Case) Util_Database.getSObjectById(input.customData.get(FlowStepInput.PARAM_ACTIVITY_CASE_ID));
            if (activityCase.Reason__c == Util_Case.REASON_DECEASED) {
                return Label.MoveOutEmail_Subject_Deceased;
            }
            return Label.MoveOutEmail_Subject;
        }
    }

    public class ActivateSmartCharging extends FlowStep_SendEmail {

        public override String getDisplayName() {
            return 'Send smart charging activation confirmation email';
        }

        public override Verdict shouldExecute() {
            if (input.state.account.Email__c == input.state.primaryContact.Email) {
                return new Verdict(false, 'The email address has not been changed.');
            }
            return new Verdict(true);
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'SmartChargingActivationConfirmationEmail';
        }

        public override FlowStepResult execute() {
            List<Attachment> attachments = new List<Attachment>();
            attachments.add(Util_Attachment.staticResourceToAttachment(Configuration.SMART_CHARGING_TERMS_AND_CONDITIONS_ID, 'Slim Laden applicatie Privacyverklaring'));

            Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), attachments, getFromEmailAddress());
            return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
        }
    }

    public class SendVariableTariffs extends FlowStep_SendEmail {
        public override String getDisplayName() {
            return 'Send Variable tariff email';
        }

        public override Verdict shouldExecute() {
            if (input.state.account.IsProducer__c == TRUE) {
                return new Verdict(false, 'This Email should not be sent to Producers.');
            }
            return new Verdict(true);
        }

        @testVisible
        private override String getEmailTemplate() {
            return 'ChangeVariableTariffsEmail';
        }

        @testVisible
        private override Util_Email.OrgWideEmail getFromEmailAddress() {
            return Util_Email.NOREPLY_ADDRESS;
        }
    }
s    // todo:
    // 1. get rid of this getTermsAndConditions
    // 2. get a static resource, like in RequestChargeCards. Name of static resource: Actievoorwaardenvriendenkorting

    public FlowStepResult getTermsAndConditions(FlowStepInput input) {

        // input.customData does not have BillingItemGroupId
        Attachment attachment = Util_BillingItem.getAttachmentForBillingItemGroupId(input.customData.get(FlowStep_RegisterBillingItem.BILLING_ITEM_GROUP_ID_PARAM));
        List<Attachment> attachments = new List<Attachment>();
        if (attachment != null)
            attachments.add(attachment);

        Cluster__c targetCluster = Util_Flowstate.getClusterByReference(input.state, input.customData.get(FlowStepInput.PARAM_CLUSTER_REFERENCE));
        Connection__c targetConnection = Util_Connection.getElectricityOrElseGas(
                Util_FlowState.getConnectionsForCluster(input.state.connections, targetCluster)
        );
        Contract__c currentContract = Util_Flowstate.getActiveContractForConnection(input.state.contracts, targetConnection);
        attachments.add(Util_BillingItem.getCompensationAttachmentForContract(currentContract));

        Util_Email.sendTo(input.state.primaryContact, getRelatedObjectId(), getEmailTemplate(), attachments, getFromEmailAddress());
        return new FlowStepResult(Flowstep.Outcome.SUCCESS, getStepMessage());
    }
}