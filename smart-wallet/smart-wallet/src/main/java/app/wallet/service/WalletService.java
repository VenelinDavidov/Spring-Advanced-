package app.wallet.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionType;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import app.wallet.repository.WalletRepository;
import app.web.dto.TransferRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class WalletService {

    private static final String SMART_WALLET_LTD = "Smart Wallet LTD";

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;


    //Constructor
    @Autowired
    public WalletService(WalletRepository walletRepository,
                         TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
    }





    public void unlockNewWallet(User user) {

        List <Wallet> allUserWallets = walletRepository.findAllByOwnerUsername (user.getUsername ()); // get all user wallets
        Subscription activeSubscription = user.getSubscriptions ().get (0);  // get the first subscription who ever is active

        boolean isDefaultPlanAndMaxWalletUnlocked = activeSubscription.getType () == SubscriptionType.DEFAULT  && allUserWallets.size () == 1;
        boolean isPremiumPlanAndMaxWalletUnlocked = activeSubscription.getType () == SubscriptionType.PREMIUM  && allUserWallets.size () == 2;
        boolean isUltimatePlanAndMaxWalletUnlocked = activeSubscription.getType () == SubscriptionType.ULTIMATE  && allUserWallets.size () == 3;

        if (isDefaultPlanAndMaxWalletUnlocked || isPremiumPlanAndMaxWalletUnlocked || isUltimatePlanAndMaxWalletUnlocked) {
            throw new DomainException ("Max wallet count reached for user with id [%s] and subscription type [%s]."
                    .formatted (user.getId (), activeSubscription.getType ()), HttpStatus.BAD_REQUEST);
        }

        // build new wallet for user plan
        Wallet newWallet = Wallet.builder ()
                .owner (user)
                .status (WalletStatus.ACTIVE)
                .balance (new BigDecimal ("0"))
                .currency (Currency.getInstance ("EUR"))
                .createdOn (LocalDateTime.now ())
                .updatedOn (LocalDateTime.now ())
                .build ();

        walletRepository.save (newWallet);  // save repo
    }



    public Wallet initializeFirstWallet(User user) {

        List <Wallet> allUsersWallet = walletRepository.findAllByOwnerUsername (user.getUsername ());

        if (!allUsersWallet.isEmpty ()) {
            throw new DomainException ("User with username [%s] with id [%s] already has a wallet! First wallet cannot be initialized."
                    .formatted (user.getUsername (), user.getId ()), HttpStatus.BAD_REQUEST);
        }

        Wallet wallet = walletRepository.save (initializeNewWallet (user));

        log.info ("Successfully created new wallet with id [%s] and balance [%.2f]."
                .formatted (wallet.getId (), wallet.getBalance ()));

        return wallet;
    }




    //Method change
    @Transactional
    public Transaction topUp(UUID walletId, BigDecimal amount) {

        Wallet wallet = getWalletById (walletId);


        if (wallet.getStatus () == WalletStatus.INACTIVE) {

            return transactionService.createNewTransaction (
                    wallet.getOwner (),
                    SMART_WALLET_LTD,
                    walletId.toString (),
                    amount,
                    wallet.getBalance (),
                    wallet.getCurrency (),
                    TransactionType.DEPOSIT,
                    TransactionStatus.FAILED,
                    "Top Up %.2f".formatted (amount.doubleValue ()),
                    "Inactive wallet");
        }

        wallet.setBalance (wallet.getBalance ().add (amount));
        wallet.setUpdatedOn (LocalDateTime.now ());

        walletRepository.save (wallet);


        return transactionService.createNewTransaction (
                wallet.getOwner (),
                SMART_WALLET_LTD,
                wallet.getId ().toString (),
                amount,
                wallet.getBalance (),
                wallet.getCurrency (),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                "Top Up %.2f".formatted (amount.doubleValue ()),
                null);
    }



   // Transfer Funds
    public Transaction transferFunds (User sender,  TransferRequest transferRequest){

        Wallet senderWallet = getWalletById (transferRequest.getFromWalledId ());

        Optional <Wallet> receiverWalletOptional = walletRepository.findAllByOwnerUsername (transferRequest.getToUsername ())
                .stream ()
                .filter (w -> w.getStatus () == WalletStatus.ACTIVE)
                .findFirst ();

        String transferDescription = "Transfer from %s to %s, for %.2f"
                .formatted (sender.getUsername (), transferRequest.getToUsername (), transferRequest.getAmount ());


        if (receiverWalletOptional.isEmpty ()){

            return transactionService.createNewTransaction (
                    sender,
                    senderWallet.getId ().toString (),
                    transferRequest.getToUsername (),
                    transferRequest.getAmount (),
                    senderWallet.getBalance (),
                    senderWallet.getCurrency (),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    transferDescription,
                    "Invalid criteria transfer!"
            );
        }

        Transaction withdrawal = charge (sender, senderWallet.getId (), transferRequest.getAmount (), transferDescription);

        if (withdrawal.getStatus () == TransactionStatus.FAILED){
            return withdrawal;
        }

        Wallet receiverWallet = receiverWalletOptional.get ();
        receiverWallet.setBalance (receiverWallet.getBalance ().add (transferRequest.getAmount ()));
        receiverWallet.setUpdatedOn (LocalDateTime.now ());

        walletRepository.save (receiverWallet);

        transactionService.createNewTransaction (receiverWallet.getOwner (),
                senderWallet.getId ().toString (),
                receiverWallet.getId ().toString (),
                transferRequest.getAmount (),
                receiverWallet.getBalance (),
                receiverWallet.getCurrency (),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                transferDescription,
                null);

        return withdrawal;
    }




    //Charge method for transaction
    @Transactional
    public Transaction charge (User user, UUID walletId, BigDecimal amount, String description){

        Wallet wallet = getWalletById (walletId);

        //Когато статуса ни е Inactive връщаме една транзакция и не променяме баланса на wallet!!!!
        //Validate Wallet
        String failureReason = null;
        boolean isFailedTransaction = false;

        if (wallet.getStatus () == WalletStatus.INACTIVE){

            isFailedTransaction = true;
            failureReason = "Inactive wallet status!";

        }
        // Когато Balance ни е < Amount не променяме баланса на wallet!!!!
        //Validate Balance
        if (wallet.getBalance ().compareTo (amount) < 0){

            isFailedTransaction = true;
            failureReason = "Insufficient funds!";
        }

        // It's a True!!!
        if (isFailedTransaction){
            return transactionService.createNewTransaction (
                    user,
                    wallet.getId ().toString (),
                    SMART_WALLET_LTD,
                    amount,
                    wallet.getBalance (),
                    wallet.getCurrency (),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    description,
                    failureReason
            );
        }

        //Връщаме успешното плащане -Charge
        //Deduct the amount and create the transaction record
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance (newBalance);
        wallet.setUpdatedOn (LocalDateTime.now ());

        walletRepository.save (wallet);

        //Return transaction
        return transactionService.createNewTransaction (
                user,
                wallet.getId ().toString (),
                SMART_WALLET_LTD,
                amount,
                newBalance,
                wallet.getCurrency (),
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCEEDED,
                description,
                null
        );
    }






    private Wallet getWalletById(UUID walletId) {
        return walletRepository.findById (walletId)
                .orElseThrow (() -> new DomainException ("Wallet with id [%s] does not exist."
                        .formatted (walletId), HttpStatus.BAD_REQUEST));
    }





    //method
    private Wallet initializeNewWallet(User user) {

        return Wallet.builder ()
                .owner (user)
                .status (WalletStatus.ACTIVE)
                .balance (new BigDecimal ("20.00"))
                .currency (Currency.getInstance ("EUR"))
                .createdOn (LocalDateTime.now ())
                .updatedOn (LocalDateTime.now ())
                .build ();
    }


    public Map <UUID, List <Transaction>> getLastFourTransactions(List <Wallet> wallets) {

        Map <UUID, List <Transaction>> transactionsByWalletsId = new LinkedHashMap <> ();

        for (Wallet wallet : wallets) {

        List<Transaction> lastFourTransactions  = transactionService.getLastFourTransactionsByWalletId (wallet);
        transactionsByWalletsId.put (wallet.getId (), lastFourTransactions);
        }

        return transactionsByWalletsId;
    }





    public void switchWalletStatus(UUID walletId, UUID ownerId) {

        Optional <Wallet> optionalWallet = walletRepository.findByIdAndOwnerId (walletId, ownerId);

        if (optionalWallet.isEmpty ()) {
            throw new DomainException ("Wallet with id [%s] does not exist, to user with id [%s]!"
                    .formatted (walletId, ownerId), HttpStatus.BAD_REQUEST);
        }

        Wallet wallet = optionalWallet.get ();

        if (wallet.getStatus () == WalletStatus.ACTIVE) {
            wallet.setStatus (WalletStatus.INACTIVE);
        } else {
            wallet.setStatus (WalletStatus.ACTIVE);
        }
        walletRepository.save (wallet);
    }
}